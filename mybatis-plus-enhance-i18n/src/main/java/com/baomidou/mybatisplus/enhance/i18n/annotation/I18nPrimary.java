package com.baomidou.mybatisplus.enhance.i18n.annotation;

import java.lang.annotation.*;

/**
 * 标记原始数据与国际化数据之间的关联主键。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface I18nPrimary {

    /**
     * 显式指定主键字段名。
     *
     * @return 主键字段名；空字符串表示从注解位置推断
     */
    public abstract String value() default "";

}
