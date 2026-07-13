package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Base64;

import static org.junit.Assert.*;

/**
 * 版本化密文信封、密钥分离与轮换测试。
 */
public class DefaultEncryptedFieldHandlerTest {

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
                new ObjectMapper(), SymmetricAlgorithmType.AES, HmacAlgorithm.HmacSHA256,
                Mode.CBC, Padding.PKCS5Padding, provider);
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
