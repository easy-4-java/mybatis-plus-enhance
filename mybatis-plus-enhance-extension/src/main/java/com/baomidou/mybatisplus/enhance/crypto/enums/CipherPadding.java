package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Padding;

/**
 * 对称加密填充方式。
 *
 * <p>本枚举封装标准分组密码填充方式，用于公共 API，隔离第三方密码库类型。</p>
 *
 * @since 2.0.0
 */
public enum CipherPadding {

    /** 不填充，要求明文长度必须为分组长度的整数倍。 */
    NoPadding(Padding.NoPadding),

    /** 零字节填充。 */
    ZeroPadding(Padding.ZeroPadding),

    /** ISO 10126 填充。 */
    ISO10126Padding(Padding.ISO10126Padding),

    /** Optimal Asymmetric Encryption Padding，用于 RSA。 */
    OAEPPadding(Padding.OAEPPadding),

    /** PKCS#1 填充，用于 RSA。 */
    PKCS1Padding(Padding.PKCS1Padding),

    /** PKCS#5 填充（等同于 PKCS#7 对于 8 字节分组），通用推荐。 */
    PKCS5Padding(Padding.PKCS5Padding),

    /** SSL3 填充。 */
    SSL3Padding(Padding.SSL3Padding);

    private final Padding hutoolPadding;

    CipherPadding(Padding hutoolPadding) {
        this.hutoolPadding = hutoolPadding;
    }

    /**
     * 获取内部 Hutool 填充枚举，仅供框架内部实现使用。
     *
     * @return Hutool {@link Padding} 实例
     */
    public Padding toHutoolPadding() {
        return hutoolPadding;
    }
}
