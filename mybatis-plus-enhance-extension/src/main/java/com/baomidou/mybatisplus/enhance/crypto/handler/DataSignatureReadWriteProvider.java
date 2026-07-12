package com.baomidou.mybatisplus.enhance.crypto.handler;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;

import java.util.Optional;

/**
 * 数据签名值读写端口。
 *
 * <p>将签名计算与签名字段存取解耦，支持实体字段、Map 或自定义外部存储。</p>
 */
public interface DataSignatureReadWriteProvider {

    /**
     * 从原始对象读取已持久化的签名值。
     *
     * @param rawObject 数据对象
     * @param tableInfo 对象表信息
     * @return 签名值
     */
    Optional<Object> readSignature(Object rawObject, TableInfo tableInfo);

    /**
     * 将新签名值写入实体或更新 Wrapper。
     *
     * @param rawObject     数据对象
     * @param tableInfo     对象表信息
     * @param updateWrapper Wrapper 更新场景的 SET 参数；实体场景可能为空
     * @param signValue     新签名值
     * @return 是否继续执行数据更新操作
     */
    boolean writeSignature(Object rawObject, TableInfo tableInfo, AbstractWrapper<?, ?, ?> updateWrapper, String signValue);

}
