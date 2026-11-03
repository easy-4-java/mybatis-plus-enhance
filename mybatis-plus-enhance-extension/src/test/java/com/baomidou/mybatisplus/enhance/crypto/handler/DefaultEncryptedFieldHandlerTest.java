package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Base64;

import static org.junit.Assert.*;

/**
 * 版本化密文信封、密钥分离与轮换测试。
 */
public class DefaultEncryptedFieldHandlerTest {

    private static final ObjectMapper MAPPER = new JsonMapper();

    /** Generated with bcprov-jdk18on 1.81, Hutool crypto, JDK 21 and Jackson 2.
     * SM4/CBC/PKCS5Padding; keyId bc181; encryption key is 16 ASCII '1' bytes,
     * HMAC-SHA256 key is 32 ASCII 'a' bytes; IV is bytes 00 through 0f.
     * Plaintext is the Jackson JSON string for SM4历史数据.
     */
    private static final String BC181_SM4_CIPHERTEXT = "MPE1.YmMxODE.SM4.CBC.PKCS5Padding.AAECAwQFBgcICQoLDA0ODw.k1HiLlv7mftE7WRvyc2QDKq_6daF4xEby7RRakCKtI4.h8nD3t50HRaTAvqWmRUzqBanLiifInpaA1O8wlNevn4";

    @Test
    public void shouldRoundTripSm4Ciphertext() {
        DefaultEncryptedFieldHandler handler = createSm4Handler();
        String ciphertext = handler.encrypt("SM4敏感数据");

        assertTrue(ciphertext.startsWith("MPE1."));
        assertEquals("SM4敏感数据", handler.decrypt(ciphertext, String.class));
    }

    @Test
    public void shouldRejectTamperedSm4Ciphertext() {
        DefaultEncryptedFieldHandler handler = createSm4Handler();
        String[] parts = handler.encrypt("SM4敏感数据").split("\\.", -1);
        byte[] ciphertext = Base64.getUrlDecoder().decode(parts[6]);
        ciphertext[0] ^= 1;
        parts[6] = Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
        try {
            handler.decrypt(String.join(".", parts), String.class);
            fail("Expected SM4 envelope authentication to reject ciphertext tampering");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("decrypt failed"));
        }
    }

    @Test
    public void shouldDecryptBc181Sm4Ciphertext() {
        assertEquals("SM4历史数据", createSm4Handler().decrypt(BC181_SM4_CIPHERTEXT, String.class));
    }

    private DefaultEncryptedFieldHandler createSm4Handler() {
        return new DefaultEncryptedFieldHandler(
                MAPPER, SymmetricAlgorithmType.SM4, HmacType.HmacSHA256,
                CipherMode.CBC, CipherPadding.PKCS5Padding,
                new StaticCryptoKeyProvider(key("bc181", '1', 'a')));
    }


    @Test
    public void shouldRoundTripVersionedCiphertextWithRandomIv() {
        DefaultEncryptedFieldHandler handler = createHandler(key("v1", '1', 'a'));

        String first = handler.encrypt("敏感数据");
        String second = handler.encrypt("敏感数据");

        assertTrue(first.startsWith("MPE1."));
        assertNotEquals(first, second);
        assertEquals("敏感数据", handler.decrypt(first, String.class));
        assertEquals("敏感数据", handler.decrypt(second, String.class));
    }

    @Test
    public void shouldSerializeEncryptedObjectAndRestoreRequestedType() {
        DefaultEncryptedFieldHandler handler = createHandler(key("v1", '1', 'a'));
        SecretRecord original = new SecretRecord();
        original.name = "敏感字段";
        original.count = 42;

        String ciphertext = handler.encrypt(original);
        SecretRecord restored = handler.decrypt(ciphertext, SecretRecord.class);

        assertTrue(ciphertext.startsWith("MPE1."));
        assertEquals(original.name, restored.name);
        assertEquals(original.count, restored.count);
    }

    public static class SecretRecord {
        public String name;
        public int count;
    }

    @Test
    public void shouldRejectTamperedCiphertextBeforeDecrypt() {
        DefaultEncryptedFieldHandler handler = createHandler(key("v1", '1', 'a'));
        String encrypted = handler.encrypt("sensitive-data");
        String[] parts = encrypted.split("\\.", -1);
        byte[] mac = Base64.getUrlDecoder().decode(parts[parts.length - 1]);
        mac[0] ^= 1;
        parts[parts.length - 1] = Base64.getUrlEncoder().withoutPadding().encodeToString(mac);
        String tampered = String.join(".", parts);

        try {
            handler.decrypt(tampered, String.class);
            fail("Expected authenticated envelope verification to fail");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("decrypt failed"));
        }
    }

    @Test
    public void shouldDecryptAndVerifyHistoricalKeyAfterRotation() {
        CryptoKeyMaterial oldKey = key("v1", '1', 'a');
        CryptoKeyMaterial currentKey = key("v2", '2', 'b');
        DefaultEncryptedFieldHandler oldHandler = createHandler(oldKey);
        String ciphertext = oldHandler.encrypt("payload");
        String signature = oldHandler.hmac("payload");

        DefaultEncryptedFieldHandler rotatedHandler = createHandler(
                new StaticCryptoKeyProvider(currentKey, Collections.singletonList(oldKey)));

        assertEquals("payload", rotatedHandler.decrypt(ciphertext, String.class));
        assertTrue(rotatedHandler.verifyHmac("payload", signature));
        assertFalse(rotatedHandler.verifyHmac("changed", signature));
        assertTrue(rotatedHandler.hmac("payload").startsWith("MPEH1."));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectEncryptionAndAuthenticationKeyReuse() {
        byte[] sameKey = repeat('x', 32);
        new CryptoKeyMaterial("v1", sameKey, sameKey);
    }

    private DefaultEncryptedFieldHandler createHandler(CryptoKeyMaterial key) {
        return createHandler(new StaticCryptoKeyProvider(key));
    }

    private DefaultEncryptedFieldHandler createHandler(StaticCryptoKeyProvider provider) {
        return new DefaultEncryptedFieldHandler(
                new JsonMapper(), SymmetricAlgorithmType.AES, HmacType.HmacSHA256,
                CipherMode.CBC, CipherPadding.PKCS5Padding, provider);
    }

    private CryptoKeyMaterial key(String keyId, char encryptionByte, char authenticationByte) {
        return new CryptoKeyMaterial(keyId, repeat(encryptionByte, 16), repeat(authenticationByte, 32));
    }

    private byte[] repeat(char value, int count) {
        StringBuilder builder = new StringBuilder(count);
        for (int index = 0; index < count; index++) {
            builder.append(value);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }
}
