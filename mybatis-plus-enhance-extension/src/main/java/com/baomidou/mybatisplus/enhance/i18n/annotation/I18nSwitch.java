package com.baomidou.mybatisplus.enhance.i18n.annotation;

import java.lang.annotation.*;

/**
 * 为 Mapper 方法启用并配置国际化字段切换。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface I18nSwitch {

    /**
     * 当前方法需要切换的国际化字段。
     *
     * @return 字段映射定义
     */
    public abstract I18nColumn[] value() default {};

}
