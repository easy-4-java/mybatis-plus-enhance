package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.digest.HmacAlgorithm;

/**
 * HMAC 签名算法类型。
 *
 * <p>本枚举封装标准 HMAC 算法，用于公共 API，隔离第三方密码库类型。</p>
 *
 * @since 2.0.0
 */
public enum HmacType {

    /** HMAC-MD5，仅用于兼容历史系统。 */
    HmacMD5(HmacAlgorithm.HmacMD5),

    /** HMAC-SHA1。 */
    HmacSHA1(HmacAlgorithm.HmacSHA1),

    /** HMAC-SHA256，通用推荐。 */
    HmacSHA256(HmacAlgorithm.HmacSHA256),

    /** HMAC-SHA384。 */
    HmacSHA384(HmacAlgorithm.HmacSHA384),

    /** HMAC-SHA512。 */
    HmacSHA512(HmacAlgorithm.HmacSHA512);

    private final HmacAlgorithm hutoolAlgorithm;

    HmacType(HmacAlgorithm hutoolAlgorithm) {
        this.hutoolAlgorithm = hutoolAlgorithm;
    }

    /**
     * 获取内部 Hutool HMAC 算法枚举，仅供框架内部实现使用。
     *
     * @return Hutool {@link HmacAlgorithm} 实例
     */
    public HmacAlgorithm toHutoolAlgorithm() {
        return hutoolAlgorithm;
    }
}
