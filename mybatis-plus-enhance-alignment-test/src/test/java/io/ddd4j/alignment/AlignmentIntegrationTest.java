package io.ddd4j.alignment;

import io.ddd4j.alignment.entity.SharedUserEntity;
import io.ddd4j.alignment.mapper.EnhanceUserMapper;
import io.ddd4j.alignment.mapper.PlusUserMapper;
import io.ddd4j.alignment.side.EnhanceSide;
import io.ddd4j.alignment.side.PlusSide;
import io.ddd4j.alignment.spi.EnhanceSideConfigurator;
import io.ddd4j.alignment.spi.PlusSideConfigurator;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 对齐集成测试：同 SQLite schema + 同带注解实体 + 同 Mapper 接口，
 * 在 mybatis-plus-enhance 与 mybatis-enhance 两侧独立运行完整链路。
 *
 * @author <a href="https://github.com/hiwepy">wandl</a>
 */
public class AlignmentIntegrationTest {

    private static final byte[] KEY = bytes('e', 32);
    private static final byte[] AUTH = bytes('a', 32);

    private PlusSide plusSide;
    private EnhanceSide enhanceSide;

    @Before
    public void setUp() throws Exception {
        plusSide = new PlusSide(KEY, AUTH);
        enhanceSide = new EnhanceSide(KEY, AUTH);
    }

    @After
    public void tearDown() throws Exception {
        if (plusSide != null) plusSide.close();
        if (enhanceSide != null) enhanceSide.close();
    }

    /**
     * SPI 反射契约 — 两侧字段注解信息一致。
     */
    @Test
    public void shouldExposeConsistentFieldContract() {
        PlusSideConfigurator plusCfg = new PlusSideConfigurator(SharedUserEntity.class);
        EnhanceSideConfigurator enhanceCfg = new EnhanceSideConfigurator(SharedUserEntity.class);

        assertEquals("mobile signatureOrder 应一致", plusCfg.signatureOrder("mobile"), enhanceCfg.signatureOrder("mobile"));
        assertEquals("mobile isEncrypted 应一致", plusCfg.isEncrypted("mobile"), enhanceCfg.isEncrypted("mobile"));
        assertEquals("email signatureOrder 应一致", plusCfg.signatureOrder("email"), enhanceCfg.signatureOrder("email"));
        assertEquals("name signatureOrder 应一致", plusCfg.signatureOrder("name"), enhanceCfg.signatureOrder("name"));
        assertEquals("name isEncrypted 应一致", plusCfg.isEncrypted("name"), enhanceCfg.isEncrypted("name"));
    }

    private static byte[] bytes(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
