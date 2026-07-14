package io.ddd4j.alignment;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultEncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.enhance.crypto.handler.EnvelopeEncryptedFieldHandler;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 跨框架密文兼容性测试：A 加密 → B 解密 = 原文。
 *
 * <p>由于两个框架在 classpath 中有独立的 CryptoKeyMaterial 类（Plus 版和 non-Plus 版），
 * 这里通过 reflection 让 EnvelopeEncryptedFieldHandler（非 Plus 版）接受来自反射构造的对象。</p>
 */
public class EnvelopeCompatibilityTest {

    private static final byte[] ENC_KEY = bytes('e', 32);
    private static final byte[] AUTH_KEY = bytes('a', 32);

    private static EnvelopeEncryptedFieldHandler newEnhanceHandler(Object nonPlusMaterial) throws Exception {
        Class<?> symClass = Class.forName("org.apache.ibatis.enhance.crypto.enums.SymmetricAlgorithmType");
        Class<?> matClass = Class.forName("org.apache.ibatis.enhance.crypto.key.CryptoKeyMaterial");
        java.lang.reflect.Constructor<?> ctor = EnvelopeEncryptedFieldHandler.class.getConstructor(
                ObjectMapper.class, symClass, HmacAlgorithm.class, Mode.class, Padding.class, matClass);
        ctor.setAccessible(true);
        return (EnvelopeEncryptedFieldHandler) ctor.newInstance(
                new ObjectMapper(),
                symClass.getField("AES").get(null),
                HmacAlgorithm.HmacSHA256, Mode.CBC, Padding.PKCS5Padding,
                nonPlusMaterial);
    }

    private static Object newNonPlusMaterial() throws Exception {
        return Class.forName("org.apache.ibatis.enhance.crypto.key.CryptoKeyMaterial")
                .getConstructor(String.class, byte[].class, byte[].class)
                .newInstance("v1", ENC_KEY, AUTH_KEY);
    }

    @Test
    public void shouldPlusEncryptAndEnhanceDecrypt() {
        String plaintext = "13800138000";

        // Plus 端加密
        CryptoKeyMaterial plusMaterial = new CryptoKeyMaterial("v1", ENC_KEY, AUTH_KEY);
        DefaultEncryptedFieldHandler plusEnc = new DefaultEncryptedFieldHandler(
                new ObjectMapper(),
                com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType.AES,
                HmacAlgorithm.HmacSHA256,
                Mode.CBC, Padding.PKCS5Padding,
                new StaticCryptoKeyProvider(plusMaterial));
        String plusCipher = plusEnc.encrypt(plaintext);
        assertNotNull(plusCipher);
        assertTrue("Plus 密文应以 MPE1. 开头", plusCipher.startsWith("MPE1."));

        try {
            EnvelopeEncryptedFieldHandler enhanceDec = newEnhanceHandler(newNonPlusMaterial());
            String decrypted = enhanceDec.decrypt(plusCipher, String.class);
            assertEquals("Plus 加密 -> non-Plus 解密 = 原文", plaintext, decrypted);
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("non-Plus 端加载/解密失败: " + e + " | cause: " + cause);
        }
    }

    @Test
    public void shouldEnhanceEncryptAndPlusDecrypt() {
        String plaintext = "13800002222";

        try {
            EnvelopeEncryptedFieldHandler enhanceEnc = newEnhanceHandler(newNonPlusMaterial());
            String enhanceCipher = enhanceEnc.encrypt(plaintext);
            assertNotNull(enhanceCipher);
            assertTrue("non-Plus 密文应以 MPE1. 开头", enhanceCipher.startsWith("MPE1."));

            // Plus 端解密
            CryptoKeyMaterial plusMaterial = new CryptoKeyMaterial("v1", ENC_KEY, AUTH_KEY);
            DefaultEncryptedFieldHandler plusDec = new DefaultEncryptedFieldHandler(
                    new ObjectMapper(),
                    com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType.AES,
                    HmacAlgorithm.HmacSHA256,
                    Mode.CBC, Padding.PKCS5Padding,
                    new StaticCryptoKeyProvider(plusMaterial));
            String decrypted = plusDec.decrypt(enhanceCipher, String.class);
            assertEquals("non-Plus 加密 -> Plus 解密 = 原文", plaintext, decrypted);
        } catch (Exception e) {
            fail("non-Plus 端加载/加密失败: " + e);
        }
    }

    @Test
    public void shouldRandomIVProduceDifferentCiphertexts() {
        try {
            EnvelopeEncryptedFieldHandler handler = newEnhanceHandler(newNonPlusMaterial());
            String c1 = handler.encrypt("hello");
            String c2 = handler.encrypt("hello");
            assertNotEquals("随机 IV 应产生不同密文", c1, c2);
        } catch (Exception e) {
            fail("reflection 加载失败: " + e);
        }
    }

    private static byte[] bytes(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString().getBytes();
    }
}
