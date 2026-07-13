package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

/**
 * 表签名验签控制上下文。
 *
 * <p>仅用于历史数据补签和延迟重签流程读取尚未刷新签名的原始行。普通业务查询不得打开
 * 此作用域，确保签名不匹配始终 fail-closed。</p>
 */
public final class SignatureVerificationContext {

    private static final TransmittableThreadLocal<Integer> IGNORE_DEPTH = new TransmittableThreadLocal<>();

    private SignatureVerificationContext() {
    }

    /** @return 开启临时忽略验签的维护作用域 */
    public static Scope openIgnored() {
        Integer previous = IGNORE_DEPTH.get();
        IGNORE_DEPTH.set(Objects.isNull(previous) ? 1 : previous + 1);
        return new Scope(previous);
    }

    /** @return 当前是否处于维护补签读取作用域 */
    public static boolean isIgnored() {
        Integer depth = IGNORE_DEPTH.get();
        return Objects.nonNull(depth) && depth > 0;
    }

    /** 清理当前线程状态，主要供请求边界和测试使用。 */
    public static void clear() {
        IGNORE_DEPTH.remove();
    }

    /** 自动恢复上一层状态的作用域句柄。 */
    public static final class Scope implements AutoCloseable {

        private final Integer previous;
        private boolean closed;

        private Scope(Integer previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Objects.isNull(previous)) {
                IGNORE_DEPTH.remove();
            } else {
                IGNORE_DEPTH.set(previous);
            }
            closed = true;
        }
    }
}
