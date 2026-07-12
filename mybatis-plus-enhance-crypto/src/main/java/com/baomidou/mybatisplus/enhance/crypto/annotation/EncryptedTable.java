package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 声明实体参与透明字段加解密。
 *
 * <p>只有同时标记 {@link EncryptedField} 的字段会执行密码运算。</p>
 *
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EncryptedTable {

}
