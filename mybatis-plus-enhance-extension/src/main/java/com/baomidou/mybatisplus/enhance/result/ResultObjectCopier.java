package com.baomidou.mybatisplus.enhance.result;

/**
 * 查询结果对象复制端口。
 *
 * <p>查询后增强（解密、国际化）会修改结果对象的字段值。若直接修改 MyBatis 一级或二级缓存
 * 持有的原始对象，会导致：同一 SqlSession 再次查询返回已解密的值；{@code selectIgnoreDecryptById}
 * 等"跳过解密"方法返回的也是已解密值。</p>
 *
 * <p>默认的 {@link ReflectionResultObjectCopier} 通过浅拷贝隔离缓存对象。如果应用
 * 已禁用 MyBatis 缓存（或使用外部缓存如 Redis），可以通过 {@link #noCopy()} 跳过拷贝，
 * 直接就地解密以节省内存。</p>
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

    /**
     * 返回不做拷贝的单例实现。适用于已禁用 MyBatis 本地缓存、或确信不会在同一
     * SqlSession 中混合使用普通查询与"跳过解密"查询的场景。
     *
     * @return 直接返回原对象的 {@link ResultObjectCopier}
     */
    static ResultObjectCopier noCopy() {
        return NoCopyResultObjectCopier.INSTANCE;
    }
}
