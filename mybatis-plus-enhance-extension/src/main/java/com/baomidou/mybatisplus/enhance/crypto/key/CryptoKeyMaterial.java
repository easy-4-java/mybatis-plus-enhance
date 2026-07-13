package com.baomidou.mybatisplus.enhance.crypto.key;

import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Objects;

/**
 * 具备版本标识的密码密钥材料。
 *
 * <p>字段加密密钥与 HMAC 密钥必须相互独立。对象对输入和输出字节数组均执行复制，避免
 * 调用方意外修改框架持有的密钥内容。</p>
 */
public final class CryptoKeyMaterial {

    private final String keyId;
    private final byte[] encryptionKey;
    private final byte[] authenticationKey;

    /**
     * 创建密钥材料。
     *
     * @param keyId             非空密钥版本标识
     * @param encryptionKey     字段加密密钥
     * @param authenticationKey HMAC 认证密钥，至少 32 字节
     */
    public CryptoKeyMaterial(String keyId, byte[] encryptionKey, byte[] authenticationKey) {
        if (StringUtils.isBlank(keyId)) {
            throw new IllegalArgumentException("keyId must not be blank");
        }
        this.encryptionKey = Arrays.copyOf(
                Objects.requireNonNull(encryptionKey, "encryptionKey must not be null"), encryptionKey.length);
        this.authenticationKey = Arrays.copyOf(
                Objects.requireNonNull(authenticationKey, "authenticationKey must not be null"),
                authenticationKey.length);
        if (this.encryptionKey.length == 0) {
            throw new IllegalArgumentException("encryptionKey must not be empty");
        }
        if (this.authenticationKey.length < 32) {
            throw new IllegalArgumentException("authenticationKey must contain at least 32 bytes");
        }
        if (Arrays.equals(this.encryptionKey, this.authenticationKey)) {
            throw new IllegalArgumentException("encryptionKey and authenticationKey must be different");
        }
        this.keyId = keyId;
    }

    public String getKeyId() {
        return keyId;
    }

    public byte[] getEncryptionKey() {
        return Arrays.copyOf(encryptionKey, encryptionKey.length);
    }

    public byte[] getAuthenticationKey() {
        return Arrays.copyOf(authenticationKey, authenticationKey.length);
    }
}
