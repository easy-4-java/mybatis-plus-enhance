package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 标记需要透明加密存储、查询后解密的实体字段。
 *
 * <p>仅在同时启用加密、解密拦截器并提供 {@code EncryptedFieldHandler} 时生效。
 * 不应标记主键、分片键或需要数据库范围查询的字段。</p>
 *
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface EncryptedField {

}
