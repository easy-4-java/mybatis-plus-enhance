package com.baomidou.mybatisplus.enhance.i18n.annotation;

import java.util.Locale;

/**
 * 内置语言环境枚举。
 *
 * <p>用于注解参数，避免直接在注解中构造 {@link Locale}。</p>
 */
public enum LocaleEnum {

    zh_CN(Locale.CHINA),

    en_US(Locale.US);

    private final Locale locale;

    private LocaleEnum(Locale locale) {
        this.locale = locale;
    }

    /**
     * 忽略大小写解析语言枚举名称。
     *
     * @param parameter 枚举名称
     * @return 匹配的语言枚举
     * @throws IllegalArgumentException 名称不存在时抛出
     */
    static LocaleEnum valueOfIgnoreCase(String parameter) {
        return valueOf(parameter.toUpperCase(Locale.ENGLISH).trim());
    }

    /**
     * 获取对应的 JDK 语言环境。
     *
     * @return Locale 实例
     */
    public Locale getLocale() {
        return locale;
    }


}
