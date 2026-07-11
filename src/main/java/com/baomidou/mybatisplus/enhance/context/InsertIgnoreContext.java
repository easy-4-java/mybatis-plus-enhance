package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

/**
 * {@code INSERT IGNORE} 线程上下文。
 *
 * <p>通过 {@link #open()} 创建作用域，并配合 try-with-resources 使用，确保嵌套调用和异常场景下
 * 都能恢复进入作用域前的状态。上下文使用 {@link TransmittableThreadLocal}，可配合 TTL 线程池包装器
 * 透传到异步任务。</p>
 */
public final class InsertIgnoreContext {

    private static final TransmittableThreadLocal<Integer> DEPTH = new TransmittableThreadLocal<>();

    private InsertIgnoreContext() {
    }

    public static Scope open() {
        Integer previousDepth = DEPTH.get();
        DEPTH.set(Objects.isNull(previousDepth) ? 1 : previousDepth + 1);
        return new Scope(previousDepth);
    }

    public static boolean isEnabled() {
        Integer depth = DEPTH.get();
        return Objects.nonNull(depth) && depth > 0;
    }

    public static void clear() {
        DEPTH.remove();
    }

    public static final class Scope implements AutoCloseable {

        private final Integer previousDepth;
        private boolean closed;

        private Scope(Integer previousDepth) {
            this.previousDepth = previousDepth;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Objects.isNull(previousDepth)) {
                DEPTH.remove();
            } else {
                DEPTH.set(previousDepth);
            }
            closed = true;
        }
    }
}
