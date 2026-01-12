package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Mode;
import cn.hutool.crypto.Padding;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import com.baomidou.mybatisplus.enhance.util.SymmetricCryptoUtil;
import lombok.Getter;

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

    public SymmetricAlgorithmType getFor(String name) {
        for (SymmetricAlgorithmType type : SymmetricAlgorithmType.values()) {
            if (type.getName().equals(name)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 获取 SymmetricCrypto
     * @param key 密钥
     * @param iv 偏移向量，加盐
     * @return SymmetricCrypto
     */
    public SymmetricCrypto getSymmetricCrypto(String mode, String padding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), Mode.valueOf(mode), Padding.valueOf(padding), key, iv);
    }

    /**
     * 获取 SymmetricCrypto
     * @param key 密钥
     * @param iv 偏移向量，加盐
     * @return SymmetricCrypto
     */
    public SymmetricCrypto getSymmetricCrypto(Mode mode, Padding padding, String key, String iv) {
        return SymmetricCryptoUtil.getSymmetricCrypto(this.getName(), mode, padding, key, iv);
    }

}
