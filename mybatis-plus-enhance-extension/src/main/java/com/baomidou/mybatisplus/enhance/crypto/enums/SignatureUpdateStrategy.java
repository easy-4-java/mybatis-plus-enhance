package com.baomidou.mybatisplus.enhance.crypto.enums;

/**
 * 表签名更新策略。
 *
 * <p>整行签名必须基于更新后的完整持久化状态计算。策略用于区分普通部分更新、由 Service
 * 负责写后补签的更新、完整行更新以及只写签名列的内部更新。</p>
 */
public enum SignatureUpdateStrategy {

    /**
     * 默认安全策略：拒绝对签名表执行无法证明完整性的部分更新。
     */
    REJECT_PARTIAL,

    /**
     * 当前更新先写业务字段，随后由同一事务读取完整原始行并刷新签名。
     */
    DEFERRED_RESIGN,

    /**
     * 调用方保证参数包含更新后的完整行，可以在写入前直接计算签名。
     */
    FULL_ROW,

    /**
     * 框架内部只更新已经计算完成的签名存储列。
     */
    SIGNATURE_ONLY
}
