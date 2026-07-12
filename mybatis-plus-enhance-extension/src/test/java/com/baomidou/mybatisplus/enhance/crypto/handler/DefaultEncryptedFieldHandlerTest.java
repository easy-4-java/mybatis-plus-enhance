package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.*;

/**
 * {@link DefaultEncryptedFieldHandler} 的编码与密码操作测试。
 */
public class DefaultEncryptedFieldHandlerTest {

    private static final String KEY = Base64.encode("0123456789abcdef".getBytes(StandardCharsets.UTF_8));
    private static final String IV = Base64.encode("abcdef0123456789".getBytes(StandardCharsets.UTF_8));

    @Test
    public void shouldRoundTripBase64Ciphertext() {
        DefaultEncryptedFieldHandler handler = createHandler(true);

        String encrypted = handler.encrypt("敏感数据");

        assertNotEquals("敏感数据", encrypted);
        assertEquals("敏感数据", handler.decrypt(encrypted, String.class));
    }

    @Test
    public void shouldRoundTripHexCiphertext() {
        DefaultEncryptedFieldHandler handler = createHandler(false);

        String encrypted = handler.encrypt("sensitive-data");

        assertTrue(HexUtil.isHexNumber(encrypted));
        assertEquals("sensitive-data", handler.decrypt(encrypted, String.class));
    }

    @Test
    public void shouldProduceStableEncodedHmac() {
        DefaultEncryptedFieldHandler base64Handler = createHandler(true);
        DefaultEncryptedFieldHandler hexHandler = createHandler(false);

        String base64 = base64Handler.hmac("payload");
        String hex = hexHandler.hmac("payload");

        assertEquals(base64, base64Handler.hmac("payload"));
        assertEquals(hex, hexHandler.hmac("payload"));
        assertTrue(Base64.isBase64(base64));
        assertTrue(HexUtil.isHexNumber(hex));
    }

    private DefaultEncryptedFieldHandler createHandler(boolean base64Output) {
        return new DefaultEncryptedFieldHandler(
                new ObjectMapper(),
                SymmetricAlgorithmType.AES,
                HmacAlgorithm.HmacSHA256,
                Mode.CBC,
                Padding.PKCS5Padding,
                KEY,
                IV,
                base64Output);
    }
}
