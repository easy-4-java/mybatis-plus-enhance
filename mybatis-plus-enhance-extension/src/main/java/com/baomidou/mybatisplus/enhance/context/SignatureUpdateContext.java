package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;

import java.util.Objects;

/**
 * 表签名更新策略线程上下文。
 *
 * <p>作用域用于 Service 事务与拦截器之间传递明确的更新语义。调用方必须使用
 * try-with-resources 关闭作用域，嵌套调用会在关闭时恢复上一层策略。</p>
 */
public final class SignatureUpdateContext {

    private static final TransmittableThreadLocal<SignatureUpdateStrategy> STRATEGY =
            new TransmittableThreadLocal<>();

    private SignatureUpdateContext() {
    }

    /**
     * 打开指定签名更新策略作用域。
     *
     * @param strategy 当前更新策略
     * @return 必须关闭的作用域句柄
     */
    public static Scope open(SignatureUpdateStrategy strategy) {
        SignatureUpdateStrategy previous = STRATEGY.get();
        STRATEGY.set(Objects.requireNonNull(strategy, "strategy must not be null"));
        return new Scope(previous);
    }

    /**
     * 获取当前策略；未打开作用域时使用拒绝部分更新的安全默认值。
     *
     * @return 当前签名更新策略
     */
    public static SignatureUpdateStrategy current() {
        SignatureUpdateStrategy strategy = STRATEGY.get();
        return Objects.isNull(strategy) ? SignatureUpdateStrategy.REJECT_PARTIAL : strategy;
    }

    /**
     * 清理当前线程策略，主要供请求边界和测试使用。
     */
    public static void clear() {
        STRATEGY.remove();
    }

    /**
     * 自动恢复上一层签名策略的作用域。
     */
    public static final class Scope implements AutoCloseable {

        private final SignatureUpdateStrategy previous;
        private boolean closed;

        private Scope(SignatureUpdateStrategy previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Objects.isNull(previous)) {
                STRATEGY.remove();
            } else {
                STRATEGY.set(previous);
            }
            closed = true;
        }
    }
}
