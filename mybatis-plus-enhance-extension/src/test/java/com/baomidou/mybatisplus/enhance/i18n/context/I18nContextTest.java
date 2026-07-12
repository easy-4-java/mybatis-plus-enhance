package com.baomidou.mybatisplus.enhance.i18n.context;

import org.junit.Assert;
import org.junit.Test;

import java.util.Locale;

/**
 * {@link I18nContext} 嵌套作用域测试。
 */
public class I18nContextTest {

    @Test
    public void shouldRestorePreviousLocale() {
        I18nContext context = new I18nContext();
        try (I18nContext.Scope ignored = context.open(Locale.CHINA)) {
            Assert.assertEquals(Locale.CHINA, context.getLocale());
            try (I18nContext.Scope nested = context.open(Locale.US)) {
                Assert.assertEquals(Locale.US, context.getLocale());
            }
            Assert.assertEquals(Locale.CHINA, context.getLocale());
        }
        Assert.assertNull(context.getLocale());
    }
}
