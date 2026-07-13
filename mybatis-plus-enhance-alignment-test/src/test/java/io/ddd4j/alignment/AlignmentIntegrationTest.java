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
 * 对齐集成测试：同 SQLite schema + 同 {@link SharedUserEntity}（无注解）+ 同 Mapper 接口，
 * 在 mybatis-plus-enhance 与 mybatis-enhance 两侧独立运行完整链路，逐字段比对入参与返回。
 *
 * <p>不依赖任何框架 Service 基类，只通过 mapper + 拦截器链验证"完整链路一致性"。</p>
 *
 * @author <a href="https://github.com/hiwepy">wandl</a>
 * @since 3.0.x
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

        // 两侧共享同一内存库（file::memory:?cache=shared），只需在 Plus 端清空即可
        try (org.apache.ibatis.session.SqlSession s = plusSide.openSession()) {
            s.update("io.ddd4j.alignment.mapper.PlusUserMapper.deleteAll");
            s.commit();
        }
    }

    @After
    public void tearDown() throws Exception {
        if (plusSide != null) plusSide.close();
        if (enhanceSide != null) enhanceSide.close();
    }

    /**
     * 用例 1：完整链路对比 — insert + selectById + selectList + selectIgnoreDecryptById。
     */
    @Test
    public void shouldProduceIdenticalFullStackResults() {
        // === debug: 验证 deleteAll ===
        try (org.apache.ibatis.session.SqlSession s = plusSide.openSession()) {
            int d1 = s.update("io.ddd4j.alignment.mapper.PlusUserMapper.deleteAll");
            s.commit();
            System.out.println("DEBUG deleteAll (Plus mapper): " + d1);
        }
        try (org.apache.ibatis.session.SqlSession s = enhanceSide.openSession()) {
            int d2 = s.update("io.ddd4j.alignment.mapper.EnhanceUserMapper.deleteAll");
            s.commit();
            System.out.println("DEBUG deleteAll (Enhance mapper): " + d2);
        }
        // === debug: list tables ===
        try (org.apache.ibatis.session.SqlSession s = plusSide.openSession()) {
            // 验证 deleteAll 确实执行
            int deleted = s.update("io.ddd4j.alignment.mapper.PlusUserMapper.deleteAll");
            s.commit();
            System.out.println("deleteAll rows deleted: " + deleted);
        }

        // === debug: 两端共享验证 ===
        try (org.apache.ibatis.session.SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            SharedUserEntity input = newEntity("TestUser", "13800000000", "test@test.com");
            mapper.insert(input);
            s.commit();
            System.out.println("DEBUG Plus insert id: " + input.getId());

            // 在 Enhance 端查
            try (org.apache.ibatis.session.SqlSession s2 = enhanceSide.openSession()) {
                EnhanceUserMapper mapper2 = enhanceSide.mapper(s2);
                List<SharedUserEntity> list2 = mapper2.selectList();
                System.out.println("DEBUG Enhance selectList after Plus insert: " + list2.size());
            }
        }
        try (org.apache.ibatis.session.SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            // 清空后再插入
            s.update("io.ddd4j.alignment.mapper.EnhanceUserMapper.deleteAll");
            s.commit();
        }
        // === Plus 端 ===
        // 先清空表（两侧共享同一个内存库）
        try (SqlSession s = plusSide.openSession()) {
            plusSide.mapper(s).deleteAll();
            s.commit();
        }
        long plusId;
        SharedUserEntity plusById, plusListFirst, plusRaw;
        try (SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            SharedUserEntity input = newEntity("Alice", "13800138000", "alice@example.com");
            mapper.insert(input);
            plusId = input.getId();
            assertNotNull("Plus 端 id 应回填", plusId);
            s.commit();
            plusById = mapper.selectById(plusId);
            List<SharedUserEntity> list = mapper.selectList();
            assertEquals(1, list.size());
            plusListFirst = list.get(0);
            plusRaw = mapper.selectIgnoreDecryptById(plusId);
        }

        // === non-Plus 端 ===
        // 再次清空表（共享内存库）
        try (SqlSession s = enhanceSide.openSession()) {
            enhanceSide.mapper(s).deleteAll();
            s.commit();
        }
        long enhanceId;
        SharedUserEntity enhanceById, enhanceListFirst, enhanceRaw;
        try (SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            SharedUserEntity input = newEntity("Alice", "13800138000", "alice@example.com");
            mapper.insert(input);
            enhanceId = input.getId();
            assertNotNull("non-Plus 端 id 应回填", enhanceId);
            s.commit();
            enhanceById = mapper.selectById(enhanceId);
            List<SharedUserEntity> list = mapper.selectList();
            assertEquals(1, list.size());
            enhanceListFirst = list.get(0);
            enhanceRaw = mapper.selectIgnoreDecryptById(enhanceId);
        }

        // === 逐字段比对 ===
        assertNotNull(plusById);
        assertNotNull(enhanceById);
        assertEquals("name 应一致", plusById.getName(), enhanceById.getName());
        // mobile 是加密字段，解密后应等于明文（两个框架都能正确解密）
        assertEquals("解密后 mobile 应一致", plusById.getMobile(), enhanceById.getMobile());
        assertEquals("email 应一致", plusById.getEmail(), enhanceById.getEmail());

        assertEquals("selectList 第一个 name", plusListFirst.getName(), enhanceListFirst.getName());
        assertEquals("selectList 第一个 mobile", plusListFirst.getMobile(), enhanceListFirst.getMobile());

        // selectIgnoreDecryptById：两侧均返回记录，mobile 非空
        assertNotNull("Plus 端 mobile 应非空", plusRaw.getMobile());
        assertNotNull("non-Plus 端 mobile 应非空", enhanceRaw.getMobile());
        // hamc：实体无注解，拦截器不知道这是签名字段，所以 hamc 可能为 null
        // 加密/签名行为由各框架的 CryptoSignatureIntegrationTest 验证
        // 这里只验证 selectIgnoreDecryptById 链路本身正常工作
        System.out.println("Plus 端 hamc: " + plusRaw.getHamc());
        System.out.println("non-Plus 端 hamc: " + enhanceRaw.getHamc());
    }

    /**
     * 用例 2：批量查询对比。
     */
    @Test
    public void shouldProduceIdenticalBatchResults() {
        // Plus 端
        long pId1, pId2;
        List<SharedUserEntity> pList;
        try (SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            SharedUserEntity e1 = newEntity("Alice", "13800001111", "a@b.com");
            SharedUserEntity e2 = newEntity("Bob",   "13800002222", "b@c.com");
            mapper.insert(e1);
            mapper.insert(e2);
            pId1 = e1.getId();
            pId2 = e2.getId();
            s.commit();
            pList = mapper.selectBatchIds(List.of(pId1, pId2));
        }

        // non-Plus 端
        long eId1, eId2;
        List<SharedUserEntity> eList;
        try (SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            SharedUserEntity e1 = newEntity("Alice", "13800001111", "a@b.com");
            SharedUserEntity e2 = newEntity("Bob",   "13800002222", "b@c.com");
            mapper.insert(e1);
            mapper.insert(e2);
            eId1 = e1.getId();
            eId2 = e2.getId();
            s.commit();
            eList = mapper.selectBatchIds(List.of(eId1, eId2));
        }

        // 比对
        assertEquals("Plus 端应有 2 条", 2, pList.size());
        assertEquals("non-Plus 端应有 2 条", 2, eList.size());
        assertEquals("第 1 条 name", pList.get(0).getName(), eList.get(0).getName());
        assertEquals("第 1 条 mobile（解密后）", pList.get(0).getMobile(), eList.get(0).getMobile());
        assertEquals("第 2 条 name", pList.get(1).getName(), eList.get(1).getName());
        assertEquals("第 2 条 mobile（解密后）", pList.get(1).getMobile(), eList.get(1).getMobile());
    }

    /**
     * 用例 3：SPI 反射契约 — 两侧注解反射能力一致。
     */
    @Test
    public void shouldExposeConsistentFieldContract() {
        PlusSideConfigurator plusCfg = new PlusSideConfigurator(SharedUserEntity.class);
        EnhanceSideConfigurator enhanceCfg = new EnhanceSideConfigurator(SharedUserEntity.class);

        // SharedUserEntity 无注解 → 所有字段都返回 null / false
        assertNull(plusCfg.signatureOrder("mobile"));
        assertNull(enhanceCfg.signatureOrder("mobile"));
        assertFalse(plusCfg.isEncrypted("mobile"));
        assertFalse(enhanceCfg.isEncrypted("mobile"));
    }

    // ========================= 工具 =========================

    private static SharedUserEntity newEntity(String name, String mobile, String email) {
        SharedUserEntity e = new SharedUserEntity();
        e.setName(name);
        e.setMobile(mobile);
        e.setEmail(email);
        return e;
    }

    private static byte[] bytes(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
