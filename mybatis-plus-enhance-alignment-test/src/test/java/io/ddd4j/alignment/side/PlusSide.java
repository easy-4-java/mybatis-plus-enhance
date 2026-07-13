package io.ddd4j.alignment.side;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataSignatureHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultEncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import com.baomidou.mybatisplus.enhance.injector.EnhanceSqlInjector;
import com.baomidou.mybatisplus.enhance.plugins.MybatisPlusEnhanceInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataDecryptionInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataEncryptionInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataSignatureInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ddd4j.alignment.entity.SharedUserEntity;
import io.ddd4j.alignment.mapper.PlusUserMapper;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.sqlite.SQLiteDataSource;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Plus 端执行器：完整 MyBatis-Plus + Enhance 拦截器链 + SQLite。
 *
 * <p>公共 API（与 {@code EnhanceSide} 对齐）：</p>
 * <ul>
 *   <li>{@link #openSession()} — 打开一个新会话</li>
 *   <li>{@link #mapper(SqlSession)} — 获取 PlusUserMapper</li>
 *   <li>{@link #close()} — 释放资源</li>
 * </ul>
 */
public class PlusSide implements Closeable {

    private final SQLiteDataSource dataSource;
    private final SqlSessionFactory sqlSessionFactory;

    public PlusSide(byte[] key, byte[] auth) throws Exception {
        dataSource = new SQLiteDataSource();
        dataSource.setUrl("jdbc:sqlite:file::memory:?cache=shared");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS user_entity");
            stmt.execute("CREATE TABLE user_entity ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name VARCHAR(255), "
                    + "mobile VARCHAR(2048), "
                    + "email VARCHAR(255), "
                    + "hamc VARCHAR(2048))");
        }

        DefaultEncryptedFieldHandler fieldHandler = new DefaultEncryptedFieldHandler(
                new ObjectMapper(),
                SymmetricAlgorithmType.AES,
                HmacType.HmacSHA256,
                CipherMode.CBC,
                CipherPadding.PKCS5Padding,
                new StaticCryptoKeyProvider(new CryptoKeyMaterial("alignment-v1", key, auth)));

        DefaultDataEncryptionHandler encryptionHandler = new DefaultDataEncryptionHandler(fieldHandler);
        DefaultDataSignatureHandler signatureHandler = new DefaultDataSignatureHandler(fieldHandler);

        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
        interceptor.addInnerInterceptor(new DataEncryptionInnerInterceptor(encryptionHandler));
        interceptor.addInnerInterceptor(new DataSignatureInnerInterceptor(signatureHandler, true, true));
        interceptor.addInnerInterceptor(new DataDecryptionInnerInterceptor(encryptionHandler, true));

        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setEnvironment(new Environment("plus-side", new JdbcTransactionFactory(), dataSource));
        GlobalConfig globalConfig = new GlobalConfig();
        globalConfig.setDbConfig(new GlobalConfig.DbConfig());
        globalConfig.setSqlInjector(new EnhanceSqlInjector());
        GlobalConfigUtils.setGlobalConfig(configuration, globalConfig);
        configuration.addInterceptor(interceptor);
        configuration.addMapper(PlusUserMapper.class);

        sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    public SqlSession openSession() {
        return sqlSessionFactory.openSession(false);
    }

    public PlusUserMapper mapper(SqlSession session) {
        return session.getMapper(PlusUserMapper.class);
    }

    @Override
    public void close() throws IOException {
        // SQLiteDataSource 内部连接池
    }
}
