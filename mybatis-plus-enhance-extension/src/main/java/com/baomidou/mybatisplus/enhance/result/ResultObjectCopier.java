package com.baomidou.mybatisplus.enhance.result;

/**
 * 查询结果对象复制端口。
 *
 * <p>查询后增强不得修改 MyBatis 一级或二级缓存持有的原始对象。实现负责为单条结果创建
 * 可安全修改的副本；业务实体无法由默认实现复制时，可以提供自定义实现。</p>
 */
@FunctionalInterface
public interface ResultObjectCopier {

    /**
     * 创建查询结果对象的独立副本。
     *
     * @param source 原始查询结果
     * @return 与原对象相互独立的副本
     */
    Object copy(Object source);
}
