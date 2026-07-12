package com.baomidou.mybatisplus.enhance.annotation.datascope;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mapper 数据权限声明。
 *
 * <p>可以标记 Mapper 类型或方法。类型级配置作用于该 Mapper 的全部语句；方法级配置只作用于
 * 对应的 MappedStatement。方法需要临时关闭权限时使用 {@code enabled = false}。</p>
 *
 * <p>该注解只描述权限元数据，实际 SQL 表达式由数据权限表达式提供者生成。</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataScopePlus {

    /**
     * 是否启用数据权限处理。
     *
     * @return 默认 {@code true}
     */
    boolean enabled() default true;

    /**
     * SQL 中业务表的别名。
     *
     * @return 表别名；空字符串表示直接使用表字段
     */
    String tableAlias() default "";

    /**
     * “仅本人数据”规则使用的创建人字段名。
     *
     * @return 创建人字段名，默认 {@code create_id}
     */
    String oneselfScopeName() default "create_id";
}
