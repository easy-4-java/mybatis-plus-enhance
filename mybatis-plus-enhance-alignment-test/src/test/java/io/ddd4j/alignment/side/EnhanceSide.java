package io.ddd4j.alignment.side;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ddd4j.alignment.entity.SharedUserEntity;
import io.ddd4j.alignment.mapper.EnhanceUserMapper;
import org.apache.ibatis.datasource.pooled.PooledDataSource;
import org.apache.ibatis.enhance.crypto.enums.SymmetricAlgorithmType;
import org.apache.ibatis.enhance.crypto.handler.DefaultDataEncryptionHandler;
import org.apache.ibatis.enhance.crypto.handler.DefaultDataSignatureHandler;
import org.apache.ibatis.enhance.crypto.handler.DefaultEncryptedFieldHandler;
import org.apache.ibatis.enhance.plugins.MybatisEnhanceInterceptor;
import org.apache.ibatis.enhance.crypto.interceptor.DataDecryptionInnerInterceptor;
import org.apache.ibatis.enhance.crypto.interceptor.DataEncryptionInnerInterceptor;
import org.apache.ibatis.enhance.crypto.interceptor.DataSignatureInnerInterceptor;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;

/**
 * non-Plus 端执行器：标准 MyBatis + Enhance 拦截器链 + SQLite。
 */
public class EnhanceSide implements Closeable {

    private final PooledDataSource dataSource;
    private final SqlSessionFactory sqlSessionFactory;

    public EnhanceSide(byte[] key, byte[] auth) throws Exception {
        dataSource = new PooledDataSource();
        dataSource.setDriver("org.sqlite.JDBC");
        dataSource.setUrl("jdbc:sqlite:file::memory:?cache=shared");
        dataSource.setUsername("");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute("DROP TABLE IF EXISTS user_entity");
            stmt.execute("CREATE TABLE user_entity ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "name VARCHAR(255), "
                    + "mobile VARCHAR(2048), "
                    + "email VARCHAR(255), "
                    + "hamc VARCHAR(2048))");
        }

        String keyBase64 = java.util.Base64.getEncoder().encodeToString(key);
        String authBase64 = java.util.Base64.getEncoder().encodeToString(auth);
        String ivBase64 = java.util.Base64.getEncoder().encodeToString(new byte[16]);

        DefaultEncryptedFieldHandler fieldHandler = new DefaultEncryptedFieldHandler(
                new ObjectMapper(),
                SymmetricAlgorithmType.AES,
                HmacAlgorithm.HmacSHA256,
                Mode.CBC,
                Padding.PKCS5Padding,
                keyBase64,
                ivBase64);

        DefaultDataEncryptionHandler encryptionHandler = new DefaultDataEncryptionHandler(fieldHandler);
        DefaultDataSignatureHandler signatureHandler = new DefaultDataSignatureHandler(fieldHandler);

        MybatisEnhanceInterceptor interceptor = new MybatisEnhanceInterceptor();
        interceptor.addInterceptor(new DataEncryptionInnerInterceptor(encryptionHandler, true));
        interceptor.addInterceptor(new DataSignatureInnerInterceptor(signatureHandler, true, true));
        interceptor.addInterceptor(new DataDecryptionInnerInterceptor(encryptionHandler, true));

        Configuration configuration = new Configuration();
        configuration.setEnvironment(new Environment("enhance-side", new JdbcTransactionFactory(), dataSource));
        configuration.addInterceptor(interceptor);
        configuration.addMapper(EnhanceUserMapper.class);

        sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
    }

    public SqlSession openSession() {
        return sqlSessionFactory.openSession(false);
    }

    public EnhanceUserMapper mapper(SqlSession session) {
        return session.getMapper(EnhanceUserMapper.class);
    }

    @Override
    public void close() throws IOException {
        if (dataSource != null) {
            dataSource.forceCloseAll();
        }
    }
}
