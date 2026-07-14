package io.ddd4j.alignment;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ddd4j.alignment.entity.SharedUserEntity;
import io.ddd4j.alignment.mapper.PlusUserMapper;
import io.ddd4j.alignment.mapper.EnhanceUserMapper;
import io.ddd4j.alignment.side.EnhanceSide;
import io.ddd4j.alignment.side.PlusSide;
import org.apache.ibatis.session.SqlSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.*;

/**
 * 真实端到端加密测试：带注解实体 + SQLite + 拦截器链。
 *
 * <p>验证完整链路：</p>
 * <ol>
 *   <li>insert → 拦截器自动加密 mobile 字段 + 计算签名</li>
 *   <li>selectById → 拦截器自动解密 mobile → 读出明文</li>
 *   <li>selectIgnoreDecryptById → 不解密 → 读出密文</li>
 *   <li>DB 中 mobile 存储的是密文（不是明文）</li>
 * </ol>
 *
 * <p>两侧（Plus / non-Plus）独立验证，逐字段比对结果。</p>
 */
public class EndToEndEncryptionTest {

    private static final byte[] ENC_KEY = bytes('e', 32);
    private static final byte[] AUTH_KEY = bytes('a', 32);

    private PlusSide plusSide;
    private EnhanceSide enhanceSide;

    @Before
    public void setUp() throws Exception {
        plusSide = new PlusSide(ENC_KEY, AUTH_KEY);
        enhanceSide = new EnhanceSide(ENC_KEY, AUTH_KEY);
    }

    @After
    public void tearDown() throws Exception {
        if (plusSide != null) plusSide.close();
        if (enhanceSide != null) enhanceSide.close();
    }

    /**
     * 用例 1：Plus 端真实加密验证。
     *
     * insert 时拦截器自动加密 mobile → DB 存密文
     * selectById 时拦截器自动解密 → 读出明文
     * selectIgnoreDecryptById → 读出密文
     */
    @Test
    public void shouldPlusEncryptOnInsertAndDecryptOnSelect() {
        String plaintext = "13800138000";
        long id;
        try (SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            // 先清空
            mapper.deleteAll();
            SharedUserEntity entity = newEntity("Alice", plaintext, "alice@example.com");
            mapper.insert(entity);
            id = entity.getId();
            s.commit();
        }

        // selectById → 应解密
        SharedUserEntity decrypted;
        // selectIgnoreDecryptById → 应是密文
        SharedUserEntity raw;
        try (SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            decrypted = mapper.selectById(id);
            raw = mapper.selectIgnoreDecryptById(id);
        }

        // 验证解密后明文
        assertNotNull(decrypted);
        assertEquals("Plus selectById mobile 应解密为明文", plaintext, decrypted.getMobile());
        assertEquals("Plus name 应一致", "Alice", decrypted.getName());

        // 验证密文
        assertNotNull(raw);
        assertNotEquals("Plus selectIgnoreDecryptById mobile 应是密文", plaintext, raw.getMobile());
        assertTrue("Plus 密文长度应大于明文", raw.getMobile().length() > plaintext.length());
        assertNotNull("Plus hamc 应非空", raw.getHamc());

        System.out.println("[Plus E2E] plaintext=" + plaintext + " → cipher=" + raw.getMobile().substring(0, Math.min(30, raw.getMobile().length())) + "...");
    }

    /**
     * 用例 2：non-Plus 端真实加密验证。
     */
    @Test
    public void shouldEnhanceEncryptOnInsertAndDecryptOnSelect() {
        String plaintext = "13900139000";
        long id;
        try (SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            mapper.deleteAll();
            SharedUserEntity entity = newEntity("Bob", plaintext, "bob@example.com");
            mapper.insert(entity);
            id = entity.getId();
            s.commit();
        }

        SharedUserEntity decrypted;
        SharedUserEntity raw;
        try (SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            decrypted = mapper.selectById(id);
            raw = mapper.selectIgnoreDecryptById(id);
        }

        assertNotNull(decrypted);
        assertEquals("non-Plus selectById mobile 应解密为明文", plaintext, decrypted.getMobile());
        assertEquals("non-Plus name 应一致", "Bob", decrypted.getName());

        assertNotNull(raw);
        assertNotEquals("non-Plus selectIgnoreDecryptById mobile 应是密文", plaintext, raw.getMobile());
        assertTrue("non-Plus 密文长度应大于明文", raw.getMobile().length() > plaintext.length());

        System.out.println("[Enhance E2E] plaintext=" + plaintext + " → cipher=" + raw.getMobile().substring(0, Math.min(30, raw.getMobile().length())) + "...");
    }

    /**
     * 用例 3（金标准）：同入参 → 两端独立执行 → 比对解密后结果一致。
     */
    @Test
    public void shouldBothProduceIdenticalDecryptedResults() {
        String plaintext = "13700137000";

        // Plus 端
        long plusId;
        try (SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            mapper.deleteAll();
            SharedUserEntity e = newEntity("Carol", plaintext, "carol@example.com");
            mapper.insert(e);
            plusId = e.getId();
            s.commit();
        }
        SharedUserEntity plusDecrypted;
        try (SqlSession s = plusSide.openSession()) {
            plusDecrypted = plusSide.mapper(s).selectById(plusId);
        }

        // non-Plus 端
        long enhanceId;
        try (SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            mapper.deleteAll();
            SharedUserEntity e = newEntity("Carol", plaintext, "carol@example.com");
            mapper.insert(e);
            enhanceId = e.getId();
            s.commit();
        }
        SharedUserEntity enhanceDecrypted;
        try (SqlSession s = enhanceSide.openSession()) {
            enhanceDecrypted = enhanceSide.mapper(s).selectById(enhanceId);
        }

        // 逐字段比对
        assertNotNull(plusDecrypted);
        assertNotNull(enhanceDecrypted);
        assertEquals("name 应一致", plusDecrypted.getName(), enhanceDecrypted.getName());
        assertEquals("解密后 mobile 应一致", plusDecrypted.getMobile(), enhanceDecrypted.getMobile());
        assertEquals("email 应一致", plusDecrypted.getEmail(), enhanceDecrypted.getEmail());
        assertEquals("解密后 mobile 应是原始明文", plaintext, plusDecrypted.getMobile());
        assertEquals("解密后 mobile 应是原始明文", plaintext, enhanceDecrypted.getMobile());
    }

    /**
     * 用例 4：批量查询 → 加密解密一致性。
     */
    @Test
    public void shouldBatchQueryDecryptConsistently() {
        // Plus 端插入 2 条
        long pId1, pId2;
        try (SqlSession s = plusSide.openSession()) {
            PlusUserMapper mapper = plusSide.mapper(s);
            mapper.deleteAll();
            SharedUserEntity e1 = newEntity("D1", "13800001111", "d1@a.com");
            SharedUserEntity e2 = newEntity("D2", "13800002222", "d2@b.com");
            mapper.insert(e1);
            mapper.insert(e2);
            pId1 = e1.getId();
            pId2 = e2.getId();
            s.commit();
        }
        List<SharedUserEntity> pList;
        try (SqlSession s = plusSide.openSession()) {
            pList = plusSide.mapper(s).selectBatchIds(List.of(pId1, pId2));
        }

        // non-Plus 端插入 2 条
        long eId1, eId2;
        try (SqlSession s = enhanceSide.openSession()) {
            EnhanceUserMapper mapper = enhanceSide.mapper(s);
            mapper.deleteAll();
            SharedUserEntity e1 = newEntity("D1", "13800001111", "d1@a.com");
            SharedUserEntity e2 = newEntity("D2", "13800002222", "d2@b.com");
            mapper.insert(e1);
            mapper.insert(e2);
            eId1 = e1.getId();
            eId2 = e2.getId();
            s.commit();
        }
        List<SharedUserEntity> eList;
        try (SqlSession s = enhanceSide.openSession()) {
            eList = enhanceSide.mapper(s).selectBatchIds(List.of(eId1, eId2));
        }

        // 比对
        assertEquals("Plus 应有 2 条", 2, pList.size());
        assertEquals("non-Plus 应有 2 条", 2, eList.size());
        assertEquals("第 1 条 mobile 应一致（解密后）", pList.get(0).getMobile(), eList.get(0).getMobile());
        assertEquals("第 2 条 mobile 应一致（解密后）", pList.get(1).getMobile(), eList.get(1).getMobile());
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
