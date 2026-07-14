package com.baomidou.mybatisplus.enhance.integration;

import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.enhance.context.SignatureUpdateContext;
import com.baomidou.mybatisplus.enhance.context.SignatureVerificationContext;
import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataSignatureHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultEncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import com.baomidou.mybatisplus.enhance.injector.EnhanceSqlInjector;
import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import com.baomidou.mybatisplus.enhance.plugins.MybatisPlusEnhanceInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataDecryptionInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataEncryptionInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataSignatureInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.enhance.annotation.crypto.EncryptedField;
import org.apache.ibatis.enhance.annotation.crypto.EncryptedTable;
import org.apache.ibatis.enhance.annotation.crypto.TableSignature;
import org.apache.ibatis.enhance.annotation.crypto.TableSignatureField;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.*;

/**
 * 真实 MyBatis-Plus、H2、缓存和密码增强组合测试。
 */
public class CryptoSignatureIntegrationTest {

    private SqlSessionFactory sqlSessionFactory;
    private DefaultDataSignatureHandler signatureHandler;

    @Before
    public void setUp() throws Exception {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:enhance;MODE=MySQL;DB_CLOSE_DELAY=-1");
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS crypto_record");
            statement.execute("CREATE TABLE crypto_record ("
                    + "id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    + "mobile VARCHAR(2048), email VARCHAR(255), signature_value VARCHAR(2048))");
        }

        DefaultEncryptedFieldHandler encryptedFieldHandler = new DefaultEncryptedFieldHandler(
                new ObjectMapper(), SymmetricAlgorithmType.AES, HmacType.HmacSHA256,
                CipherMode.CBC, CipherPadding.PKCS5Padding,
                new StaticCryptoKeyProvider(new CryptoKeyMaterial(
                        "integration-v1", bytes('e', 16), bytes('m', 32))));
        DefaultDataEncryptionHandler encryptionHandler = new DefaultDataEncryptionHandler(encryptedFieldHandler);
        signatureHandler = new DefaultDataSignatureHandler(encryptedFieldHandler);

        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
        interceptor.addInnerInterceptor(new DataEncryptionInnerInterceptor(encryptionHandler));
        interceptor.addInnerInterceptor(new DataSignatureInnerInterceptor(signatureHandler, true, true));
        interceptor.addInnerInterceptor(new DataDecryptionInnerInterceptor(encryptionHandler, true));

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment(
                "integration", new JdbcTransactionFactory(), dataSource));
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setDbConfig(new GlobalConfig.DbConfig());
        globalConfig.setSqlInjector(new EnhanceSqlInjector());
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        configuration.addInterceptor(interceptor);
        configuration.addMapper(CryptoMapper.class);
        sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    @After
    public void tearDown() {
        SignatureUpdateContext.clear();
        SignatureVerificationContext.clear();
    }

    @Test
    public void shouldKeepCachedSnapshotEncryptedAcrossRepeatedQueries() {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            CryptoMapper mapper = session.getMapper(CryptoMapper.class);
            CryptoRecord record = new CryptoRecord();
            record.setMobile("13800138000");
            record.setEmail("user@example.com");
            assertEquals(1, mapper.insert(record));
            session.commit();
            session.clearCache();

            CryptoRecord first = mapper.selectById(record.getId());
            CryptoRecord second = mapper.selectById(record.getId());
            CryptoRecord raw = mapper.selectIgnoreDecryptById(record.getId());

            assertEquals("13800138000", first.getMobile());
            assertEquals("13800138000", second.getMobile());
            assertNotSame(first, second);
            assertTrue(raw.getMobile().startsWith("MPE1."));
            assertTrue(raw.getSignatureValue().startsWith("MPEH1."));
        }
    }

    @Test
    public void shouldRejectPartialUpdateAndAllowDeferredSignatureOnlyRefresh() {
        try (SqlSession session = sqlSessionFactory.openSession(false)) {
            CryptoMapper mapper = session.getMapper(CryptoMapper.class);
            CryptoRecord record = new CryptoRecord();
            record.setMobile("13800138000");
            record.setEmail("before@example.com");
            mapper.insert(record);
            session.commit();

            CryptoRecord partial = new CryptoRecord();
            partial.setId(record.getId());
            partial.setEmail("after@example.com");
            try {
                mapper.updateById(partial);
                fail("Expected direct partial update to be rejected");
            } catch (RuntimeException expected) {
                assertTrue(expected.getMessage().contains("DEFERRED_RESIGN"));
            }

            try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                    SignatureUpdateStrategy.DEFERRED_RESIGN)) {
                assertEquals(1, mapper.updateById(partial));
            }
            CryptoRecord raw;
            try (SignatureVerificationContext.Scope ignored = SignatureVerificationContext.openIgnored()) {
                raw = mapper.selectIgnoreDecryptById(record.getId());
            }
            assertTrue(signatureHandler.doEntitySignature(raw));
            try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                    SignatureUpdateStrategy.SIGNATURE_ONLY)) {
                assertEquals(1, mapper.updateSignatureById(raw));
            }
            session.commit();
            session.clearCache();

            CryptoRecord verified = mapper.selectById(record.getId());
            assertEquals("after@example.com", verified.getEmail());
            assertEquals("13800138000", verified.getMobile());
        }
    }

    private byte[] bytes(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    public interface CryptoMapper extends EnhanceBaseMapper<CryptoRecord> {
    }

    @EncryptedTable
    @TableSignature
    @TableName("crypto_record")
    public static class CryptoRecord {

        @TableId(type = IdType.AUTO)
        private Long id;

        @EncryptedField
        @TableSignatureField(order = 1)
        private String mobile;

        @TableSignatureField(order = 2)
        private String email;

        @TableSignatureField(stored = true)
        private String signatureValue;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getMobile() {
            return mobile;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSignatureValue() {
            return signatureValue;
        }

        public void setSignatureValue(String signatureValue) {
            this.signatureValue = signatureValue;
        }
    }
}
