package com.baomidou.mybatisplus.enhance.util;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SM4;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 对称加密与 HMAC 工厂。
 *
 * <p>密码对象通常封装可变的 JCE 状态，因此本类每次调用都创建独立实例，不缓存密钥、
 * 初始化向量或密码对象，避免敏感材料长期驻留以及并发复用带来的线程安全问题。</p>
 */
public final class SymmetricCryptoUtil {

    private SymmetricCryptoUtil() {
    }

    /**
     * 使用 UTF-8 文本密钥创建对称加密器。
     *
     * @param algorithmType 算法名称
     * @param mode          工作模式
     * @param padding       填充方式
     * @param key           文本密钥
     * @param iv            文本初始化向量，可为 {@code null}
     * @return 独立的对称加密器
     */
    public static SymmetricCrypto getSymmetricCrypto(String algorithmType, Mode mode, Padding padding,
                                                     String key, String iv) {
        byte[] keyBytes = StringUtils.isBlank(key) ? null : key.getBytes(StandardCharsets.UTF_8);
        byte[] ivBytes = StringUtils.isBlank(iv) ? null : iv.getBytes(StandardCharsets.UTF_8);
        return getSymmetricCrypto(algorithmType, mode, padding, keyBytes, ivBytes);
    }

    /**
     * 使用原始二进制密钥创建对称加密器。
     *
     * @param algorithmType 算法名称
     * @param mode          工作模式
     * @param padding       填充方式
     * @param key           原始密钥字节
     * @param iv            原始初始化向量字节，可为 {@code null}
     * @return 独立的对称加密器
     */
    public static SymmetricCrypto getSymmetricCrypto(String algorithmType, Mode mode, Padding padding,
                                                     byte[] key, byte[] iv) {
        Objects.requireNonNull(algorithmType, "algorithmType must not be null");
        Objects.requireNonNull(mode, "mode must not be null");
        Objects.requireNonNull(padding, "padding must not be null");
        if (SM4.ALGORITHM_NAME.equalsIgnoreCase(algorithmType)) {
            return new SM4(mode.name(), padding.name(), key, iv);
        }
        if (SymmetricAlgorithm.AES.name().equalsIgnoreCase(algorithmType)) {
            return new AES(mode, padding, key, iv);
        }
        String transformation = String.join("/", algorithmType, mode.name(), padding.name());
        SymmetricCrypto crypto = new SymmetricCrypto(transformation, key);
        if (Objects.nonNull(iv)) {
            crypto.setIv(iv);
        }
        return crypto;
    }

    /**
     * 创建 SM4 加密器。
     */
    public static SymmetricCrypto getSm4(Mode mode, Padding padding, String key, String iv) {
        return getSymmetricCrypto(SM4.ALGORITHM_NAME, mode, padding, key, iv);
    }

    /**
     * 创建 AES 加密器。
     */
    public static SymmetricCrypto getAes(Mode mode, Padding padding, String key, String iv) {
        return getSymmetricCrypto(SymmetricAlgorithm.AES.name(), mode, padding, key, iv);
    }

    /**
     * 使用 UTF-8 文本密钥创建 HMAC。
     */
    public static HMac getHmac(HmacAlgorithm hmacAlgorithm, String key) {
        Objects.requireNonNull(key, "key must not be null");
        return getHmac(hmacAlgorithm, key.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 使用原始二进制密钥创建 HMAC。
     *
     * @param hmacAlgorithm HMAC 算法
     * @param key           原始密钥字节
     * @return 独立的 HMAC 实例
     */
    public static HMac getHmac(HmacAlgorithm hmacAlgorithm, byte[] key) {
        Objects.requireNonNull(hmacAlgorithm, "hmacAlgorithm must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return new HMac(hmacAlgorithm, key);
    }
}
