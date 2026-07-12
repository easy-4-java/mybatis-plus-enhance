package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;

/**
 * 实体与 Wrapper 加解密处理器。
 *
 * <p>拦截器只负责编排调用，字段识别、值转换和异常策略由实现类负责。</p>
 */
public interface DataEncryptionHandler {

    /**
     * 对通过 {@code save}/{@code updateById} 等 API 提交的实体执行字段加密。
     *
     * @param entity 待处理实体
     * @param <T>    对象类型
     * @return 是否继续执行数据库操作
     */
    <T> boolean doEntityEncrypt(T entity);

    /**
     * 对 UpdateWrapper 或 LambdaUpdateWrapper 中的赋值参数执行字段加密。
     *
     * @param entityClass   MyBatis-Plus 实体类型
     * @param updateWrapper 更新条件及 SET 参数
     * @return 是否继续执行数据库操作
     */
    boolean doWrapperEncrypt(Class<?> entityClass, AbstractWrapper<?, ?, ?> updateWrapper);

    /**
     * 对查询结果中的单个对象执行字段解密。
     *
     * @param rawObject   单个查询结果，支持实体或 Map
     * @param entityClass 结果对应的实体类型
     * @param <T>         对象类型
     */
    <T> void doRawObjectDecrypt(Object rawObject, Class<T> entityClass);

}
