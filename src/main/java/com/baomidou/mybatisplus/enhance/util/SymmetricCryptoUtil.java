package com.baomidou.mybatisplus.enhance.util;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.digest.HMac;
import cn.hutool.crypto.digest.HmacAlgorithm;
import cn.hutool.crypto.symmetric.AES;
import cn.hutool.crypto.symmetric.SM4;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对称加密工具类
 */
public class SymmetricCryptoUtil {

    private static final Map<String, SymmetricCrypto> SYMMETRIC_CRYPTO_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, HMac> HMAC_CACHE  = new ConcurrentHashMap<>();

    /**
     * 获取 SymmetricCrypto
     * @param key 密钥
     * @param iv 偏移向量，加盐
     * @return SymmetricCrypto
     */
    public static SymmetricCrypto getSymmetricCrypto(String algorithmType, Mode mode, Padding padding, String key, String iv) {
        // 构造对称加密器
        String keyStr = String.join("_", algorithmType, mode.name(), padding.name(), key, iv);
        return SYMMETRIC_CRYPTO_CACHE.computeIfAbsent(keyStr, join -> {
            String[] keyArr =  join.split("_");
            String algorithmTypeStr = Objects.toString(keyArr[0], SM4.ALGORITHM_NAME);
            String modeStr = Objects.toString(keyArr[1], Mode.ECB.name());
            String paddingStr = Objects.toString(keyArr[2], Padding.PKCS5Padding.name());
            byte[] keyBytes = StringUtils.isBlank(keyArr[3]) ? null : keyArr[3].getBytes(CharsetUtil.CHARSET_UTF_8);
            byte[] ivBytes = StringUtils.isBlank(keyArr[4]) ? null : keyArr[4].getBytes(CharsetUtil.CHARSET_UTF_8);
            // 构造SM4加密器
            if(SM4.ALGORITHM_NAME.equalsIgnoreCase(algorithmType)){
                return new SM4(modeStr, paddingStr, keyBytes, ivBytes);
            }
            // 构造AES加密器
            if(SymmetricAlgorithm.AES.name().equalsIgnoreCase(algorithmType)){
                return new AES(Mode.valueOf(modeStr), Padding.valueOf(paddingStr), keyBytes, ivBytes);
            }
            return new AES(Mode.valueOf(algorithmTypeStr), Padding.valueOf(keyArr[2]), keyBytes, ivBytes);
        });
    }

    /**
     * 获取 SymmetricCrypto
     * @param key 密钥
     * @param iv 偏移向量，加盐
     * @return SymmetricCrypto
     */
    public static SymmetricCrypto getSm4(Mode mode, Padding padding, String key, String iv) {
        return getSymmetricCrypto(SM4.ALGORITHM_NAME, mode, padding, key, iv);
    }

    /**
     * 获取aes
     * @param key 密钥，支持三种密钥长度：128、192、256位
     * @param iv 偏移向量，加盐
     * @return AES
     */
    public static SymmetricCrypto getAes(Mode mode, Padding padding, String key, String iv) {
        return getSymmetricCrypto(SymmetricAlgorithm.AES.name(), mode, padding, key, iv);
    }

    /**
     * 获取aes
     * @param hmacAlgorithm Hmac算法
     * @param key 密钥，支持三种密钥长度：128、192、256位
     * @return AES
     */
    public static HMac getHmac(HmacAlgorithm hmacAlgorithm, String key) {
        StringJoiner keyJoiner = new StringJoiner("_").add(hmacAlgorithm.getValue()).add(key);
        // 构造对称加密器
        return HMAC_CACHE.computeIfAbsent(keyJoiner.toString(), join -> {
            String[] keyArr =  join.split("_");
            String hmacAlgorithmStr = Objects.toString(keyArr[0], HmacAlgorithm.HmacSM3.getValue());
            byte[] keyBytes = keyArr[1].getBytes(CharsetUtil.CHARSET_UTF_8);
            return new HMac(hmacAlgorithmStr, keyBytes);
        });
    }


}
