package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherMode;
import com.baomidou.mybatisplus.enhance.crypto.enums.CipherPadding;
import com.baomidou.mybatisplus.enhance.crypto.enums.HmacType;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyMaterial;
import com.baomidou.mybatisplus.enhance.crypto.key.CryptoKeyProvider;
import com.baomidou.mybatisplus.enhance.util.SymmetricCryptoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;

/**
 * 使用版本化密文信封的默认字段密码处理器。
 *
 * <p>每次加密生成独立随机 IV，并采用 Encrypt-then-MAC 保护信封完整性。密文携带协议版本、
 * keyId、算法、模式与填充信息，可在密钥轮换后解析历史数据。加密密钥与 HMAC 密钥由
 * {@link CryptoKeyProvider} 分别提供，禁止同钥复用。</p>
 */
@Slf4j
public class DefaultEncryptedFieldHandler implements EncryptedFieldHandler {

    private static final String CIPHER_VERSION = "MPE1";
    private static final String HMAC_VERSION = "MPEH1";
    private static final String SEPARATOR_REGEX = "\\.";
    private static final int ENVELOPE_PARTS = 8;
    private static final int HMAC_PARTS = 3;
    private static final int BLOCK_IV_BYTES = 16;

    @Getter
    private final ObjectMapper objectMapper;
    private final SymmetricAlgorithmType algorithmType;
    private final HmacAlgorithm hmacAlgorithm;
    private final Mode mode;
    private final Padding padding;
    private final CryptoKeyProvider keyProvider;
    private final SecureRandom secureRandom;

    /**
     * 创建具备密钥轮换和完整性保护能力的字段密码处理器。
     *
     * @param objectMapper  JSON 序列化器
     * @param algorithmType 对称算法，只允许 AES 或 SM4
     * @param hmacType      HMAC 算法
     * @param cipherMode    工作模式，不允许 ECB
     * @param cipherPadding 填充方式
     * @param keyProvider   当前及历史密钥提供者
     * @since 2.0.0
     */
    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper,
                                        SymmetricAlgorithmType algorithmType,
                                        HmacType hmacType,
                                        CipherMode cipherMode,
                                        CipherPadding cipherPadding,
                                        CryptoKeyProvider keyProvider) {
        this(objectMapper, algorithmType, hmacType, cipherMode, cipherPadding, keyProvider, new SecureRandom());
    }

    DefaultEncryptedFieldHandler(ObjectMapper objectMapper,
                                 SymmetricAlgorithmType algorithmType,
                                 HmacType hmacType,
                                 CipherMode cipherMode,
                                 CipherPadding cipherPadding,
                                 CryptoKeyProvider keyProvider,
                                 SecureRandom secureRandom) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.algorithmType = requireSafeAlgorithm(algorithmType);
        this.hmacAlgorithm = Objects.requireNonNull(hmacType, "hmacType must not be null").toHutoolAlgorithm();
        this.mode = Objects.requireNonNull(cipherMode, "cipherMode must not be null").toHutoolMode();
        if (cipherMode == CipherMode.ECB) {
            throw new IllegalArgumentException("ECB mode is not allowed");
        }
        this.padding = Objects.requireNonNull(cipherPadding, "cipherPadding must not be null").toHutoolPadding();
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider must not be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        validateKey(keyProvider.currentKey());
    }

    /**
     * 使用 Hutool 类型创建字段密码处理器。
     *
     * @param objectMapper  JSON 序列化器
     * @param algorithmType 对称算法
     * @param hmacAlgorithm Hutool HMAC 算法
     * @param mode          Hutool 工作模式
     * @param padding       Hutool 填充方式
     * @param keyProvider   当前及历史密钥提供者
     * @deprecated 使用 {@link #DefaultEncryptedFieldHandler(ObjectMapper, SymmetricAlgorithmType, HmacType, CipherMode, CipherPadding, CryptoKeyProvider)} 替代，
     *             避免公共 API 直接依赖 Hutool 类型。
     */
    @Deprecated
    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper,
                                        SymmetricAlgorithmType algorithmType,
                                        HmacAlgorithm hmacAlgorithm,
                                        Mode mode,
                                        Padding padding,
                                        CryptoKeyProvider keyProvider) {
        this(objectMapper, algorithmType, hmacAlgorithm, mode, padding, keyProvider, new SecureRandom());
    }

    @Deprecated
    DefaultEncryptedFieldHandler(ObjectMapper objectMapper,
                                 SymmetricAlgorithmType algorithmType,
                                 HmacAlgorithm hmacAlgorithm,
                                 Mode mode,
                                 Padding padding,
                                 CryptoKeyProvider keyProvider,
                                 SecureRandom secureRandom) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.algorithmType = requireSafeAlgorithm(algorithmType);
        this.hmacAlgorithm = Objects.requireNonNull(hmacAlgorithm, "hmacAlgorithm must not be null");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        if (mode == Mode.ECB) {
            throw new IllegalArgumentException("ECB mode is not allowed");
        }
        this.padding = Objects.requireNonNull(padding, "padding must not be null");
        this.keyProvider = Objects.requireNonNull(keyProvider, "keyProvider must not be null");
        this.secureRandom = Objects.requireNonNull(secureRandom, "secureRandom must not be null");
        validateKey(keyProvider.currentKey());
    }

    @Override
    public <T> String encrypt(T value) {
        try {
            CryptoKeyMaterial key = keyProvider.currentKey();
            validateKey(key);
            byte[] iv = new byte[BLOCK_IV_BYTES];
            secureRandom.nextBytes(iv);
            SymmetricCrypto crypto = SymmetricCryptoUtil.getSymmetricCrypto(
                    algorithmType.getName(), mode, padding, key.getEncryptionKey(), iv);
            byte[] ciphertext = crypto.encrypt(objectMapper.writeValueAsBytes(value));
            String header = String.join(".",
                    CIPHER_VERSION,
                    encode(key.getKeyId().getBytes(StandardCharsets.UTF_8)),
                    algorithmType.getName(), mode.name(), padding.name(),
                    encode(iv), encode(ciphertext));
            return header + '.' + encode(mac(key, header.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            log.error("{} encrypt failed", algorithmType.getName());
            throw ExceptionUtils.mpe("{} encrypt failed", exception, algorithmType.getName());
        }
    }

    @Override
    public <T> T decrypt(String value, Class<T> resultType) {
        try {
            String[] parts = value.split(SEPARATOR_REGEX, -1);
            if (parts.length != ENVELOPE_PARTS || !CIPHER_VERSION.equals(parts[0])) {
                throw new IllegalArgumentException("Unsupported ciphertext envelope");
            }
            String keyId = new String(decode(parts[1]), StandardCharsets.UTF_8);
            CryptoKeyMaterial key = keyProvider.findKey(keyId)
                    .orElseThrow(() -> new IllegalStateException("Unknown crypto keyId: " + keyId));
            validateKey(key);
            String header = String.join(".", parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6]);
            byte[] expectedMac = mac(key, header.getBytes(StandardCharsets.UTF_8));
            if (!MessageDigest.isEqual(expectedMac, decode(parts[7]))) {
                throw new IllegalStateException("Ciphertext envelope authentication failed");
            }
            SymmetricAlgorithmType envelopeAlgorithm = resolveAlgorithm(parts[2]);
            requireSafeAlgorithm(envelopeAlgorithm);
            Mode envelopeMode = Mode.valueOf(parts[3]);
            if (envelopeMode == Mode.ECB) {
                throw new IllegalArgumentException("ECB mode is not allowed");
            }
            Padding envelopePadding = Padding.valueOf(parts[4]);
            SymmetricCrypto crypto = SymmetricCryptoUtil.getSymmetricCrypto(
                    envelopeAlgorithm.getName(), envelopeMode, envelopePadding,
                    key.getEncryptionKey(), decode(parts[5]));
            byte[] plaintext = crypto.decrypt(decode(parts[6]));
            return objectMapper.readValue(plaintext, resultType);
        } catch (Exception exception) {
            log.error("Ciphertext decrypt failed");
            throw ExceptionUtils.mpe("Ciphertext decrypt failed", exception);
        }
    }

    @Override
    public <T> String hmac(T value) {
        try {
            CryptoKeyMaterial key = keyProvider.currentKey();
            validateKey(key);
            return HMAC_VERSION + '.' + encode(key.getKeyId().getBytes(StandardCharsets.UTF_8)) + '.'
                    + encode(mac(key, objectMapper.writeValueAsBytes(value)));
        } catch (Exception exception) {
            log.error("HMAC digest failed");
            throw ExceptionUtils.mpe("HMAC digest failed", exception);
        }
    }

    @Override
    public <T> boolean verifyHmac(T value, String signature) {
        try {
            if (Objects.isNull(signature)) {
                return false;
            }
            String[] parts = signature.split(SEPARATOR_REGEX, -1);
            if (parts.length != HMAC_PARTS || !HMAC_VERSION.equals(parts[0])) {
                return false;
            }
            String keyId = new String(decode(parts[1]), StandardCharsets.UTF_8);
            CryptoKeyMaterial key = keyProvider.findKey(keyId).orElse(null);
            if (Objects.isNull(key)) {
                return false;
            }
            return MessageDigest.isEqual(mac(key, objectMapper.writeValueAsBytes(value)), decode(parts[2]));
        } catch (Exception exception) {
            return false;
        }
    }

    private byte[] mac(CryptoKeyMaterial key, byte[] value) {
        HMac hMac = SymmetricCryptoUtil.getHmac(hmacAlgorithm, key.getAuthenticationKey());
        return hMac.digest(value);
    }

    private void validateKey(CryptoKeyMaterial key) {
        Objects.requireNonNull(key, "crypto key must not be null");
        int length = key.getEncryptionKey().length;
        if (algorithmType == SymmetricAlgorithmType.AES && length != 16 && length != 24 && length != 32) {
            throw new IllegalArgumentException("AES key must contain 16, 24 or 32 bytes");
        }
        if (algorithmType == SymmetricAlgorithmType.SM4 && length != 16) {
            throw new IllegalArgumentException("SM4 key must contain 16 bytes");
        }
    }

    private SymmetricAlgorithmType requireSafeAlgorithm(SymmetricAlgorithmType algorithm) {
        Objects.requireNonNull(algorithm, "algorithmType must not be null");
        if (algorithm != SymmetricAlgorithmType.AES && algorithm != SymmetricAlgorithmType.SM4) {
            throw new IllegalArgumentException("Only AES and SM4 are allowed for new ciphertext");
        }
        return algorithm;
    }

    private SymmetricAlgorithmType resolveAlgorithm(String name) {
        for (SymmetricAlgorithmType type : SymmetricAlgorithmType.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported ciphertext algorithm: " + name);
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }
}
