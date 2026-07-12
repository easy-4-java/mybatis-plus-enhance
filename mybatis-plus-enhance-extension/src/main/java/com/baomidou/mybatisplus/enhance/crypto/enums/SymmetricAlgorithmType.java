package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.baomidou.mybatisplus.enhance.util.SymmetricCryptoUtil;
import lombok.Getter;

/**
 * 内置对称加密算法类型。
 * <p>
 * 枚举值只描述算法名称，实际安全性还取决于模式、填充、密钥长度、IV 生成和密钥管理。
 * 新系统应优先使用 AES 或 SM4 的安全模式，历史算法仅用于兼容已有密文。
 */
@Getter
public enum SymmetricAlgorithmType {

    AES(SymmetricAlgorithm.AES.name()),
    ARCFOUR(SymmetricAlgorithm.ARCFOUR.name()),
    Blowfish(SymmetricAlgorithm.Blowfish.name()),
    DES(SymmetricAlgorithm.DES.name()),
    DESede(SymmetricAlgorithm.DESede.name()),
    RC2(SymmetricAlgorithm.RC2.name()),
    PBEWithMD5AndDES(SymmetricAlgorithm.PBEWithMD5AndDES.name()),
    PBEWithSHA1AndDESede(SymmetricAlgorithm.PBEWithSHA1AndDESede.name()),
    PBEWithSHA1AndRC2_40(SymmetricAlgorithm.PBEWithSHA1AndRC2_40.name()),

    SM4("SM4");

    private final String name;

    SymmetricAlgorithmType(String name) {
        this.name = name;
    }

    /**
     * 按算法名查找枚举值。
     *
     * @param name Hutool/JCE 算法名称
     * @return 匹配的算法类型，未找到时返回 {@code null}
     */
    public SymmetricAlgorithmType getFor(String name) {
        for (SymmetricAlgorithmType type : SymmetricAlgorithmType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据字符串模式与填充方式创建对称加密器。
     *
     * @param mode    工作模式名称
     * @param padding 填充方式名称
     * @param key     密钥，不应写入日志或源码
     * @param iv      初始化向量，其长度必须符合算法要求
     * @return 已配置的对称加密器
     */
    public SymmetricCrypto getSymmetricCrypto(String mode, String padding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), Mode.valueOf(mode), Padding.valueOf(padding), key, iv);
    }

    /**
     * 根据强类型模式与填充方式创建对称加密器。
     *
     * @param mode    工作模式
     * @param padding 填充方式
     * @param key     密钥，不应写入日志或源码
     * @param iv      初始化向量，其长度必须符合算法要求
     * @return 已配置的对称加密器
     */
    public SymmetricCrypto getSymmetricCrypto(Mode mode, Padding padding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), mode, padding, key, iv);
    }

}
