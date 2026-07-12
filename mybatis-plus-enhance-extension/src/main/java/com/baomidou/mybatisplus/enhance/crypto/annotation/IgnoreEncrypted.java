package com.baomidou.mybatisplus.enhance.crypto.annotation;

import java.lang.annotation.*;

/**
 * 关闭指定 Mapper 方法的自动加密或解密处理。
 *
 * <p>主要用于返回原始密文、执行数据库侧迁移或绕开透明处理的专用查询。</p>
 *
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited
public @interface IgnoreEncrypted {

}
