package io.ddd4j.alignment;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultEncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.hutool.crypto.digest.HmacAlgorithm;
import org.apache.ibatis.enhance.crypto.handler.EnvelopeEncryptedFieldHandler;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * 对齐验证测试：mybatis-plus-enhance 与 mybatis-enhance 的加密行为一致性。
 *
 * <p><strong>真对比：两侧使用相同 key + 相同 plaintext，各自加密并解密，比对解密结果。</strong></p>
 *
 * <p>由于两端 IV 策略不同（Plus 端用 CryptoKeyMaterial 派生随机 IV，non-Plus 端可显式传入），
 * 密文本身不可直接对比（这由 EnvelopeCompatibilityTest 解决）。本测试对比的是：
 * <strong>两端各自加密相同 plaintext，都能解密回原文</strong>。</p>
 *
 * @author <a href="https://github.com/hiwepy">wandl</a>
 * @since 3.0.x
 */
public class AlignmentTest {

    private static final byte[] ENC_KEY = bytes('e', 32);
    private static final byte[] AUTH_KEY = bytes('a', 32);
    private static final byte[] FIXED_IV = bytes('i', 16);  // 16-byte IV

    private DefaultEncryptedFieldHandler plusHandler;
    private EnvelopeEncryptedFieldHandler enhanceHandler;

    @Before
    public void setUp() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        // Plus 版：使用 StaticCryptoKeyProvider 注入 CryptoKeyMaterial（用 Plus 版的 HmacType）
        CryptoKeyMaterial plusMaterial = new CryptoKeyMaterial("v1", ENC_KEY, AUTH_KEY);
        plusHandler = new DefaultEncryptedFieldHandler(
                objectMapper,
                SymmetricAlgorithmType.AES,
                HmacType.HmacSHA256,
                CipherMode.CBC,
                CipherPadding.PKCS5Padding,
                new StaticCryptoKeyProvider(plusMaterial));

        // non-Plus 版：使用 EnvelopeEncryptedFieldHandler（通过反射绕过 CryptoKeyMaterial 类型问题）
        Class<?> matClass = Class.forName("org.apache.ibatis.enhance.crypto.key.CryptoKeyMaterial");
        Object nonPlusMaterial = matClass
                .getConstructor(String.class, byte[].class, byte[].class)
                .newInstance("v1", ENC_KEY, AUTH_KEY);
        Class<?> ctorClass = Class.forName("org.apache.ibatis.enhance.crypto.handler.EnvelopeEncryptedFieldHandler");
        Class<?> symClass = Class.forName("org.apache.ibatis.enhance.crypto.enums.SymmetricAlgorithmType");
        java.lang.reflect.Constructor<?> ctor = ctorClass.getConstructor(
                ObjectMapper.class, symClass,
                HmacAlgorithm.class, Mode.class, Padding.class, matClass);
        ctor.setAccessible(true);
        enhanceHandler = (EnvelopeEncryptedFieldHandler) ctor.newInstance(
                objectMapper,
                symClass.getField("AES").get(null),
                HmacAlgorithm.HmacSHA256,
                Mode.CBC, Padding.PKCS5Padding,
                nonPlusMaterial);
    }

    /**
     * 用例 1：相同 plaintext → 两端各自加密 → 各自解密 → 两端解密结果一致。
     *
     * <p>真对比：两端独立执行完整加密/解密流程，比对解密后的明文是否一致。</p>
     */
    @Test
    public void shouldBothDecryptToIdenticalPlaintext() {
        String plaintext = "13800138000";

        // Plus 端
        String plusCipher = plusHandler.encrypt(plaintext);
        String plusDecrypted = plusHandler.decrypt(plusCipher, String.class);

        // non-Plus 端
        String enhanceCipher = enhanceHandler.encrypt(plaintext);
        String enhanceDecrypted = enhanceHandler.decrypt(enhanceCipher, String.class);

        // 各自能解密回原文
        assertEquals("Plus 端解密结果", plaintext, plusDecrypted);
        assertEquals("non-Plus 端解密结果", plaintext, enhanceDecrypted);

        // 真对比：两端解密结果应一致
        assertEquals("Plus vs non-Plus 解密结果应一致", plusDecrypted, enhanceDecrypted);
    }

    /**
     * 用例 2：真跨框架对比 — Plus 加密 → non-Plus 解密。
     */
    @Test
    public void shouldPlusEncryptAndEnhanceDecrypt() {
        String plaintext = "13800138000";
        String plusCipher = plusHandler.encrypt(plaintext);
        String enhanceDecrypted = enhanceHandler.decrypt(plusCipher, String.class);
        assertEquals("Plus 加密 → non-Plus 解密 = 原文", plaintext, enhanceDecrypted);
    }

    /**
     * 用例 3：真跨框架对比 — non-Plus 加密 → Plus 解密。
     */
    @Test
    public void shouldEnhanceEncryptAndPlusDecrypt() {
        String plaintext = "13800002222";
        String enhanceCipher = enhanceHandler.encrypt(plaintext);
        String plusDecrypted = plusHandler.decrypt(enhanceCipher, String.class);
        assertEquals("non-Plus 加密 → Plus 解密 = 原文", plaintext, plusDecrypted);
    }

    /**
     * 用例 4：密文格式检查 — 两端都以 MPE1. 信封格式输出。
     */
    @Test
    public void shouldBothProduceMPE1EnvelopeFormat() {
        String plaintext = "13800138000";

        String plusCipher = plusHandler.encrypt(plaintext);
        String enhanceCipher = enhanceHandler.encrypt(plaintext);

        assertTrue("Plus 密文应以 MPE1. 开头", plusCipher.startsWith("MPE1."));
        assertTrue("non-Plus 密文应以 MPE1. 开头", enhanceCipher.startsWith("MPE1."));

        // 密文段数：8 段
        assertEquals("Plus 密文应有 8 段", 8, plusCipher.split("\\.").length);
        assertEquals("non-Plus 密文应有 8 段", 8, enhanceCipher.split("\\.").length);
    }

    /**
     * 用例 5：HMAC 签名语义一致性 — 两端对同一明文签名，都能产生有效的 HMAC。
     *
     * <p>注：两端 HMAC 格式不同（Plus 端用 MPEH1 3 段信封，non-Plus 端用 1 段纯 Base64），
     * 是设计差异。本测试只验证语义正确性，不验证字节级一致。</p>
     */
    @Test
    public void shouldBothProduceValidHMACSignature() {
        String data = "13800138000|user@example.com";

        String plusHmac = plusHandler.hmac(data);
        String enhanceHmac = enhanceHandler.hmac(data);

        assertNotNull(plusHmac);
        assertNotNull(enhanceHmac);
        assertFalse(plusHmac.equals(data));
        assertFalse(enhanceHmac.equals(data));
        // 两端都使用 Base64 URL-safe 编码
        assertFalse("Plus HMAC 不应包含 + 或 /", plusHmac.contains("+") || plusHmac.contains("/"));
        assertFalse("non-Plus HMAC 不应包含 + 或 /", enhanceHmac.contains("+") || enhanceHmac.contains("/"));
    }

    // ========================= 工具 =========================

    private static byte[] bytes(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
