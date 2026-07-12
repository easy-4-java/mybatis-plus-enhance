package com.baomidou.mybatisplus.enhance.i18n.provider;

import java.util.Locale;

/**
 * 当前请求 Locale 提供者。
 */
@FunctionalInterface
public interface LocaleProvider {

    /**
     * @return 当前 Locale；返回 {@code null} 表示跳过本次国际化处理
     */
    Locale getLocale();
}
