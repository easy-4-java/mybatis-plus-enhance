package com.baomidou.mybatisplus.enhance.result;

/**
 * 不做拷贝的结果复制器，直接返回原对象。
 *
 * <p>适用于已禁用 MyBatis 本地缓存、或确信不会在同一 SqlSession 中混合使用普通查询
 * 与"跳过解密"查询的场景。使用此实现时，解密和国际化会就地修改缓存对象，节省内存但
 * 需要使用者自行保证缓存安全。</p>
 *
 * @since 2.0.0
 */
final class NoCopyResultObjectCopier implements ResultObjectCopier {

    static final NoCopyResultObjectCopier INSTANCE = new NoCopyResultObjectCopier();

    private NoCopyResultObjectCopier() {
    }

    @Override
    public Object copy(Object source) {
        return source;
    }
}
