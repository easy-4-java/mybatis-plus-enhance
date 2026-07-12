package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.HexUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.enums.SymmetricAlgorithmType;
import com.baomidou.mybatisplus.enhance.util.SymmetricCryptoUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 基于 Hutool 对称密码和 HMAC 的默认字段密码处理器。
 *
 * <p>字段值通过 Jackson 序列化后加密，解密后再恢复目标类型。密钥和初始化向量必须由
 * 外部安全配置或密钥管理系统提供，禁止在生产代码中随机生成后打印。</p>
 */
@Slf4j
public class DefaultEncryptedFieldHandler implements EncryptedFieldHandler {

    @Getter
    private final ObjectMapper objectMapper;
    private final SymmetricAlgorithmType algorithmType;
    private final HmacAlgorithm hmacAlgorithm;
    private final Mode mode;
    private final Padding padding;
    private final byte[] key;
    private final byte[] iv;
    private final boolean plainIsEncode;

    /**
     * 使用无 IV 配置创建默认 Base64 输出的处理器。
     *
     * @param objectMapper  字段值 JSON 序列化器
     * @param algorithmType 对称加密算法
     * @param hmacAlgorithm HMAC 算法
     * @param mode          对称密码工作模式
     * @param padding       填充方式
     * @param key           Base64 编码的密钥
     */
    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper, SymmetricAlgorithmType algorithmType, HmacAlgorithm hmacAlgorithm, Mode mode, Padding padding, String key) {
        this(objectMapper, algorithmType, hmacAlgorithm, mode, padding, key, null, true);
    }

    /**
     * 创建默认 Base64 输出的处理器。
     *
     * @param objectMapper  字段值 JSON 序列化器
     * @param algorithmType 对称加密算法
     * @param hmacAlgorithm HMAC 算法
     * @param mode          对称密码工作模式
     * @param padding       填充方式
     * @param key           Base64 编码的密钥
     * @param iv            Base64 编码的初始化向量
     */
    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper, SymmetricAlgorithmType algorithmType, HmacAlgorithm hmacAlgorithm, Mode mode, Padding padding, String key, String iv) {
        this(objectMapper, algorithmType, hmacAlgorithm, mode, padding, key, iv, true);
    }

    /**
     * 创建完整可配置的字段密码处理器。
     *
     * @param objectMapper  字段值 JSON 序列化器
     * @param algorithmType 对称加密算法
     * @param hmacAlgorithm HMAC 算法
     * @param mode          对称密码工作模式
     * @param padding       填充方式
     * @param key           Base64 编码的密钥
     * @param iv            Base64 编码的初始化向量，可为 {@code null}
     * @param plainIsEncode {@code true} 输出 Base64，{@code false} 输出十六进制
     */
    public DefaultEncryptedFieldHandler(ObjectMapper objectMapper, SymmetricAlgorithmType algorithmType, HmacAlgorithm hmacAlgorithm, Mode mode, Padding padding, String key, String iv, boolean plainIsEncode) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
        this.algorithmType = Objects.requireNonNull(algorithmType, "algorithmType must not be null");
        this.hmacAlgorithm = Objects.requireNonNull(hmacAlgorithm, "hmacAlgorithm must not be null");
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.padding = Objects.requireNonNull(padding, "padding must not be null");
        this.key = Base64.decode(Objects.requireNonNull(key, "key must not be null"));
        this.iv = Objects.isNull(iv) ? null : Base64.decode(iv);
        this.plainIsEncode = plainIsEncode;
    }

    /**
     * 序列化并加密字段值。
     *
     * @param value 待加密值
     * @param <T>   值类型
     * @return Base64 或算法实现定义的密文字符串
     */
    @Override
    public <T> String encrypt(T value) {
        try {
            // 1、序列化Value
            String valueAsString = getObjectMapper().writeValueAsString(value);
            // 2、获取加密器
            SymmetricCrypto crypto = SymmetricCryptoUtil.getSymmetricCrypto(
                    algorithmType.getName(), mode, padding, key, iv);
            // 3、加密Value，如果 plainIsEncode =true 则对加密结果进行Base64
            if (plainIsEncode) {
                valueAsString = crypto.encryptBase64(valueAsString);
            } else {
                valueAsString = crypto.encryptHex(valueAsString);
            }
            return valueAsString;
        } catch (Exception ex) {
            log.error("{} Encrypt Error : {}", algorithmType.getName(), ex.getMessage());
            throw ExceptionUtils.mpe("{} Encrypt Error", ex, algorithmType.getName());
        }
    }

    /**
     * 解密字段并反序列化为目标类型。
     *
     * @param value  密文字符串
     * @param rtType 目标类型
     * @param <T>    目标泛型
     * @return 解密后的值
     */
    @Override
    public <T> T decrypt(String value, Class<T> rtType) {
        try {
            // 2、获取解密器
            SymmetricCrypto crypto = SymmetricCryptoUtil.getSymmetricCrypto(algorithmType.getName(), mode, padding, key, iv);
            // 3、解密请求体
            byte[] encryptedBytes = plainIsEncode ? Base64.decode(value) : HexUtil.decodeHex(value);
            String decryptStr = crypto.decryptStr(encryptedBytes, StandardCharsets.UTF_8);
            return getObjectMapper().readValue(decryptStr, rtType);
        } catch (Exception ex) {
            log.error("{} Decrypt Error : {}", algorithmType.getName(), ex.getMessage());
            throw ExceptionUtils.mpe("{} Decrypt Error", ex, algorithmType.getName());
        }
    }

    /**
     * 对字段值的序列化结果计算 HMAC。
     *
     * @param value 待签名值
     * @param <T>   值类型
     * @return HMAC 字符串
     */
    @Override
    public <T> String hmac(T value) {
        try {
            HMac hMac = SymmetricCryptoUtil.getHmac(hmacAlgorithm, key);
            String hmacValue;
            if (plainIsEncode) {
                hmacValue = hMac.digestBase64(getObjectMapper().writeValueAsString(value), StandardCharsets.UTF_8, Boolean.TRUE);
            } else {
                hmacValue = hMac.digestHex(getObjectMapper().writeValueAsString(value));
            }
            return hmacValue;
        } catch (Exception ex) {
            log.error("HMAC Digest Error : {}", ex.getMessage());
            throw ExceptionUtils.mpe("HMAC Digest Error", ex);
        }
    }

}
