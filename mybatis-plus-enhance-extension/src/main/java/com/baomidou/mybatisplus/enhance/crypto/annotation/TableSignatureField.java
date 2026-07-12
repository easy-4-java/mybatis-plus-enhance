package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 标记参与签名计算或保存签名结果的实体字段。
 *
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface TableSignatureField {

    /**
     * 字段参与签名原文拼接的顺序。
     *
     * @return 顺序值；相同时按数据库列名稳定排序
     */
    int order() default 0;

    /**
     * 是否作为签名结果存储字段。
     *
     * @return {@code true} 表示该字段不参与原文拼接，只保存签名值
     */
    boolean stored() default false;

}
