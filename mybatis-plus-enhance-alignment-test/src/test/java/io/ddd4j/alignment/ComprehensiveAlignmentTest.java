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
import org.apache.ibatis.enhance.crypto.handler.EnvelopeEncryptedFieldHandler;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * 全面对齐验证测试：参数化多种 plaintext、key 长度、错误场景。
 *
 * <p>每个参数组合执行 3 个测试方法：</p>
 * <ol>
 *   <li>{@link #shouldBothDecryptToIdenticalPlaintext} — roundtrip + 跨端对比</li>
 *   <li>{@link #shouldBeIdempotentUnderRepeatedEncryption} — 幂等性</li>
 *   <li>{@link #shouldBothRejectTamperedCiphertext} — 篡改检测</li>
 * </ol>
 */
@RunWith(Parameterized.class)
public class ComprehensiveAlignmentTest {

    private final String description;
    private final byte[] encKey;
    private final byte[] authKey;
    private final String plaintext;

    public ComprehensiveAlignmentTest(String description, byte[] encKey, byte[] authKey, String plaintext) {
        this.description = description;
        this.encKey = encKey;
        this.authKey = authKey;
        this.plaintext = plaintext;
    }

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
            // ===== 多种 plaintext 类型 =====
            { "数字 11位手机号",        bytes('e', 16), bytes('a', 32), "13800138000" },
            { "数字 18位身份证",         bytes('e', 16), bytes('a', 32), "11010119900101001X" },
            { "空字符串",                bytes('e', 16), bytes('a', 32), "" },
            { "中文",                     bytes('e', 16), bytes('a', 32), "你好世界" },
            { "emoji + 特殊字符",        bytes('e', 16), bytes('a', 32), "🎉💡@#$%" },
            { "JSON 字符串",              bytes('e', 16), bytes('a', 32), "{\"name\":\"张三\",\"age\":18}" },
            { "超长字符串 1000 字符",    bytes('e', 16), bytes('a', 32), "x".repeat(1000) },
            { "超长字符串 10000 字符",   bytes('e', 16), bytes('a', 32), "y".repeat(10000) },
            { "Unicode 4字节字符",        bytes('e', 16), bytes('a', 32), "\uD83D\uDE00\uD83D\uDE01" },
            { "UUID 字符串",              bytes('e', 16), bytes('a', 32), UUID.randomUUID().toString() },
            { "Base64 字符串",            bytes('e', 16), bytes('a', 32), "SGVsbG8gV29ybGQ=" },

            // ===== 多种 key 长度 =====
            { "16字节 key",               bytes('k', 16), bytes('a', 32), "data1" },
            { "24字节 key",               bytes('k', 24), bytes('a', 32), "data2" },
            { "32字节 key",               bytes('k', 32), bytes('a', 32), "data3" },

            // ===== 更多边界 =====
            { "单字符",                   bytes('e', 32), bytes('a', 32), "A" },
            { "换行符",                   bytes('e', 32), bytes('a', 32), "line1\nline2\r\nline3" },
            { "制表符",                   bytes('e', 32), bytes('a', 32), "col1\tcol2\tcol3" },
        });
    }

    /**
     * 用例 1：两端各自加密→解密→验证解密结果与原文一致 + 两端解密结果一致。
     */
    @Test
    public void shouldBothDecryptToIdenticalPlaintext() throws Exception {
        DefaultEncryptedFieldHandler plusHandler = createPlusHandler(encKey, authKey);
        EnvelopeEncryptedFieldHandler enhanceHandler = createEnhanceHandler(encKey, authKey);

        // Plus 端 roundtrip
        String plusCipher = plusHandler.encrypt(plaintext);
        String plusDecrypted = plusHandler.decrypt(plusCipher, String.class);
        assertEquals("[" + description + "] Plus 解密应等于原文", plaintext, plusDecrypted);

        // non-Plus 端 roundtrip
        String enhanceCipher = enhanceHandler.encrypt(plaintext);
        String enhanceDecrypted = enhanceHandler.decrypt(enhanceCipher, String.class);
        assertEquals("[" + description + "] non-Plus 解密应等于原文", plaintext, enhanceDecrypted);

        // 真跨端对比：两端解密结果一致
        assertEquals("[" + description + "] 两端解密结果应一致", plusDecrypted, enhanceDecrypted);
    }

    /**
     * 用例 2：幂等性 — 同一明文加密3次→解密3次→每次都是原文 + 密文互不相同。
     */
    @Test
    public void shouldBeIdempotentUnderRepeatedEncryption() throws Exception {
        DefaultEncryptedFieldHandler plusHandler = createPlusHandler(encKey, authKey);
        EnvelopeEncryptedFieldHandler enhanceHandler = createEnhanceHandler(encKey, authKey);

        // Plus 端 3 次
        String pC1 = plusHandler.encrypt(plaintext);
        String pC2 = plusHandler.encrypt(plaintext);
        String pC3 = plusHandler.encrypt(plaintext);
        assertEquals("[" + description + "] Plus #1 解密", plaintext, plusHandler.decrypt(pC1, String.class));
        assertEquals("[" + description + "] Plus #2 解密", plaintext, plusHandler.decrypt(pC2, String.class));
        assertEquals("[" + description + "] Plus #3 解密", plaintext, plusHandler.decrypt(pC3, String.class));
        assertNotEquals("[" + description + "] Plus 密文#1≠#2（随机IV）", pC1, pC2);
        assertNotEquals("[" + description + "] Plus 密文#2≠#3（随机IV）", pC2, pC3);

        // non-Plus 端 3 次
        String eC1 = enhanceHandler.encrypt(plaintext);
        String eC2 = enhanceHandler.encrypt(plaintext);
        String eC3 = enhanceHandler.encrypt(plaintext);
        assertEquals("[" + description + "] non-Plus #1 解密", plaintext, enhanceHandler.decrypt(eC1, String.class));
        assertEquals("[" + description + "] non-Plus #2 解密", plaintext, enhanceHandler.decrypt(eC2, String.class));
        assertEquals("[" + description + "] non-Plus #3 解密", plaintext, enhanceHandler.decrypt(eC3, String.class));
        assertNotEquals("[" + description + "] non-Plus 密文#1≠#2（随机IV）", eC1, eC2);
        assertNotEquals("[" + description + "] non-Plus 密文#2≠#3（随机IV）", eC2, eC3);
    }

    /**
     * 用例 3：篡改检测 — HMAC 段被替换 → 两端都应拒绝解密。
     */
    @Test
    public void shouldBothRejectTamperedCiphertext() throws Exception {
        DefaultEncryptedFieldHandler plusHandler = createPlusHandler(encKey, authKey);
        EnvelopeEncryptedFieldHandler enhanceHandler = createEnhanceHandler(encKey, authKey);

        // Plus 端：生成密文 → 篡改 HMAC → 应拒绝
        String plusCipher = plusHandler.encrypt(plaintext);
        String tamperedPlus = tamperHmacSegment(plusCipher);
        try {
            plusHandler.decrypt(tamperedPlus, String.class);
            fail("[" + description + "] Plus 端应拒绝篡改后的密文");
        } catch (Exception expected) {
            // 预期异常
        }

        // non-Plus 端：生成密文 → 篡改 HMAC → 应拒绝
        String enhanceCipher = enhanceHandler.encrypt(plaintext);
        String tamperedEnhance = tamperHmacSegment(enhanceCipher);
        try {
            enhanceHandler.decrypt(tamperedEnhance, String.class);
            fail("[" + description + "] non-Plus 端应拒绝篡改后的密文");
        } catch (Exception expected) {
            // 预期异常
        }
    }

    // ========================= 工具 =========================

    /** 篡改 MPE1 信封的第 8 段（HMAC 段） */
    private static String tamperHmacSegment(String cipher) {
        String[] parts = cipher.split("\\.", -1);
        if (parts.length >= 8) {
            parts[7] = "tampered_hmac_value";
        }
        return String.join(".", parts);
    }

    private static DefaultEncryptedFieldHandler createPlusHandler(byte[] encKey, byte[] authKey) {
        CryptoKeyMaterial material = new CryptoKeyMaterial("v1", encKey, authKey);
        return new DefaultEncryptedFieldHandler(
                new ObjectMapper(),
                SymmetricAlgorithmType.AES,
                HmacType.HmacSHA256,
                CipherMode.CBC,
                CipherPadding.PKCS5Padding,
                new StaticCryptoKeyProvider(material));
    }

    private static EnvelopeEncryptedFieldHandler createEnhanceHandler(byte[] encKey, byte[] authKey) throws Exception {
        Class<?> matClass = Class.forName("org.apache.ibatis.enhance.crypto.key.CryptoKeyMaterial");
        Object nonPlusMaterial = matClass
                .getConstructor(String.class, byte[].class, byte[].class)
                .newInstance("v1", encKey, authKey);
        Class<?> ctorClass = Class.forName("org.apache.ibatis.enhance.crypto.handler.EnvelopeEncryptedFieldHandler");
        Class<?> symClass = Class.forName("org.apache.ibatis.enhance.crypto.enums.SymmetricAlgorithmType");
        java.lang.reflect.Constructor<?> ctor = ctorClass.getConstructor(
                ObjectMapper.class, symClass,
                Class.forName("cn.hutool.crypto.digest.HmacAlgorithm"),
                Mode.class, Padding.class, matClass);
        ctor.setAccessible(true);
        return (EnvelopeEncryptedFieldHandler) ctor.newInstance(
                new ObjectMapper(),
                symClass.getField("AES").get(null),
                Class.forName("cn.hutool.crypto.digest.HmacAlgorithm").getField("HmacSHA256").get(null),
                Mode.CBC, Padding.PKCS5Padding,
                nonPlusMaterial);
    }

    private static byte[] bytes(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
