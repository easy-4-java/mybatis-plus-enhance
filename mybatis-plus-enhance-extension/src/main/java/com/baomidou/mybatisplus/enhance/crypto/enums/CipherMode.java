package com.baomidou.mybatisplus.enhance.crypto.enums;

import cn.hutool.crypto.Mode;

/**
 * 对称加密工作模式。
 *
 * <p>本枚举封装标准分组密码工作模式，用于公共 API，隔离第三方密码库类型。
 * 新系统应优先使用 {@link #CBC}，不推荐 {@link #ECB}。</p>
 *
 * @since 2.0.0
 */
public enum CipherMode {

    /** 无模式。 */
    NONE(Mode.NONE),

    /** 密码分组链接模式，通用推荐。 */
    CBC(Mode.CBC),

    /** 密文反馈模式。 */
    CFB(Mode.CFB),

    /** 计数器模式。 */
    CTR(Mode.CTR),

    /** Cipher Text Stealing。 */
    CTS(Mode.CTS),

    /** 电子密码本模式，不推荐用于加密。 */
    ECB(Mode.ECB),

    /** 输出反馈模式。 */
    OFB(Mode.OFB),

    /** Propagating Cipher Block。 */
    PCBC(Mode.PCBC);

    private final Mode hutoolMode;

    CipherMode(Mode hutoolMode) {
        this.hutoolMode = hutoolMode;
    }

    /**
     * 获取内部 Hutool 模式枚举，仅供框架内部实现使用。
     *
     * @return Hutool {@link Mode} 实例
     */
    public Mode toHutoolMode() {
        return hutoolMode;
    }
}
