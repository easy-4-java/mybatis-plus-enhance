package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 声明实体需要生成和校验数据完整性签名。
 *
 * <p>签名字段由 {@link TableSignatureField} 或 {@link #unionAll()} 决定。</p>
 *
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TableSignature {

    /**
     * 是否将除签名存储字段外的全部持久化字段纳入联合签名。
     *
     * @return {@code true} 表示自动选择全部可签名字段
     */
    boolean unionAll() default false;

}
