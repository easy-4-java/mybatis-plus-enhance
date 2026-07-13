package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;

/**
 * 数据签名与验签处理器。
 *
 * <p>负责根据实体签名元数据生成完整性签名，并在查询后校验持久化签名。</p>
 *
 * @since 2.0.0
 */
public interface DataSignatureHandler {

    /**
     * 对通过实体 API 提交的数据生成并写入签名。
     *
     * @param entity 待签名实体
     * @param <T>    对象类型
     * @return 是否继续执行数据库操作
     */
    <T> boolean doEntitySignature(T entity);

    /**
     * 对 UpdateWrapper 或 LambdaUpdateWrapper 中的数据生成签名。
     *
     * @param entityClass   MyBatis-Plus 实体类型
     * @param updateWrapper 更新条件及 SET 参数
     * @return 是否继续执行数据库操作
     */
    boolean doWrapperSignature(Class<?> entityClass, AbstractWrapper<?, ?, ?> updateWrapper);

    /**
     * 对单个查询结果执行完整性验签。
     *
     * @param rawObject   单个查询结果，支持实体或 Map
     * @param entityClass 结果对应的实体类型
     * @param <T>         对象类型
     */
    <T> void doSignatureVerification(Object rawObject, Class<T> entityClass);

}
