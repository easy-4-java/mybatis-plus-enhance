package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HmacAlgorithm;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.StaticCryptoKeyProvider;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** 跨 Jackson 版本的对象 HMAC 生成及历史签名兼容契约。 */
@RunWith(Parameterized.class)
public class HmacBranchCompatibilityTest {

    // 修复前分别由 feature/1.0.x、feature/3.0.x 生成，认证密钥为 32 个 ASCII m。
    private static final String DECLARATION_SIGNATURE =
            "MPEH1.cHJvYmU.zavdHN6vhti2zYql5fIWUK3gGragnP-LC5uwCYiFx2o";
    private static final String ALPHABETIC_SIGNATURE =
            "MPEH1.cHJvYmU.ebB0bbgxfe19B1RyDEGkoYer4rir2UvFawePLlROrIg";
    private static final String STRING_SIGNATURE =
            "MPEH1.cHJvYmU.KgFpBTB4qcMW0oaXJrWnJEUvsPMLxm9WtwxnJsuO2YI";
    // 独立按固定 JSON 原文计算，验证嵌套对象也遵循同一顺序。
    private static final String NESTED_DECLARATION_SIGNATURE =
            "MPEH1.cHJvYmU.eSaS5AajkO9Z1RUns9bybocM440eYmFK26gE-wu9S9I";
    private static final String NESTED_ALPHABETIC_SIGNATURE =
            "MPEH1.cHJvYmU.ELbe87nDG4n3pao-Uf1fCxF12x665zRrIKzuyf3-5yQ";

    private final boolean sorted;
    private final boolean legacyConstructor;

    public HmacBranchCompatibilityTest(boolean sorted, boolean legacyConstructor) {
        this.sorted = sorted;
        this.legacyConstructor = legacyConstructor;
    }

    @Parameterized.Parameters(name = "sorted={0}, legacyConstructor={1}")
    public static Collection<Object[]> parameters() {
        return Arrays.asList(new Object[][] {
                {false, false}, {true, false}, {false, true}, {true, true}
        });
    }

    @Test
    public void shouldGenerateDeclarationOrderSignatureAcrossVersions() {
        assertEquals(DECLARATION_SIGNATURE, handler(mapper(), provider()).hmac(new Payload()));
    }

    @Test
    public void shouldVerifyBothHistoricalPropertyOrders() {
        DefaultEncryptedFieldHandler handler = handler(mapper(), provider());
        assertTrue(handler.verifyHmac(new Payload(), DECLARATION_SIGNATURE));
        assertTrue(handler.verifyHmac(new Payload(), ALPHABETIC_SIGNATURE));
    }

    @Test
    public void shouldRejectChangedPayloadForBothHistoricalSignatures() {
        DefaultEncryptedFieldHandler handler = handler(mapper(), provider());
        Payload changed = new Payload();
        changed.alpha = 8;
        assertFalse(handler.verifyHmac(changed, DECLARATION_SIGNATURE));
        assertFalse(handler.verifyHmac(changed, ALPHABETIC_SIGNATURE));
    }

    @Test
    public void shouldPreserveCallerMapperAndCipherSerialization() throws Exception {
        ObjectMapper mapper = mapper();
        String before = mapper.writeValueAsString(new Payload());
        DefaultEncryptedFieldHandler handler = handler(mapper, provider());
        handler.hmac(new Payload());
        handler.verifyHmac(new Payload(), ALPHABETIC_SIGNATURE);
        assertSame(mapper, handler.getObjectMapper());
        assertEquals(sorted, mapper.isEnabled(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY));
        assertEquals(before, mapper.writeValueAsString(new Payload()));
        Payload restored = handler.decrypt(handler.encrypt(new Payload()), Payload.class);
        assertEquals("中文", restored.zeta);
        assertEquals(7, restored.alpha);
    }

    @Test
    public void shouldPreserveStringSignaturesUsedByRowSigning() {
        DefaultEncryptedFieldHandler handler = handler(mapper(), provider());
        assertEquals(STRING_SIGNATURE, handler.hmac("中文|7"));
        assertTrue(handler.verifyHmac("中文|7", STRING_SIGNATURE));
    }

    @Test
    public void shouldSupportNestedObjectsInBothHistoricalOrders() {
        DefaultEncryptedFieldHandler handler = handler(mapper(), provider());
        NestedPayload payload = new NestedPayload();
        assertEquals(NESTED_DECLARATION_SIGNATURE, handler.hmac(payload));
        assertTrue(handler.verifyHmac(payload, NESTED_DECLARATION_SIGNATURE));
        assertTrue(handler.verifyHmac(payload, NESTED_ALPHABETIC_SIGNATURE));
        payload.zeta.alpha = 8;
        assertFalse(handler.verifyHmac(payload, NESTED_ALPHABETIC_SIGNATURE));
    }

    @Test
    public void shouldVerifyHistoricalPojoAfterKeyRotation() {
        StaticCryptoKeyProvider provider = new StaticCryptoKeyProvider(
                new CryptoKeyMaterial("next", bytes('x', 16), bytes('y', 32)),
                Collections.singletonList(key()));
        DefaultEncryptedFieldHandler handler = handler(mapper(), provider);
        assertTrue(handler.verifyHmac(new Payload(), DECLARATION_SIGNATURE));
        assertTrue(handler.verifyHmac(new Payload(), ALPHABETIC_SIGNATURE));
    }

    private ObjectMapper mapper() {
        return JsonMapper.builder().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, sorted).build();
    }

    @SuppressWarnings("deprecation")
    private DefaultEncryptedFieldHandler handler(ObjectMapper mapper, StaticCryptoKeyProvider provider) {
        if (legacyConstructor) {
            return new DefaultEncryptedFieldHandler(mapper, SymmetricAlgorithmType.AES,
                    HmacAlgorithm.HmacSHA256, Mode.CBC, Padding.PKCS5Padding, provider);
        }
        return new DefaultEncryptedFieldHandler(mapper, SymmetricAlgorithmType.AES,
                HmacType.HmacSHA256, CipherMode.CBC, CipherPadding.PKCS5Padding, provider);
    }

    private StaticCryptoKeyProvider provider() {
        return new StaticCryptoKeyProvider(key());
    }

    private CryptoKeyMaterial key() {
        return new CryptoKeyMaterial("probe", bytes('e', 16), bytes('m', 32));
    }

    private byte[] bytes(char value, int count) {
        byte[] result = new byte[count];
        Arrays.fill(result, (byte) value);
        return result;
    }

    public static class Payload {
        public String zeta = "中文";
        public int alpha = 7;
    }

    public static class NestedPayload {
        public Payload zeta = new Payload();
        public int alpha = 1;
    }
}
