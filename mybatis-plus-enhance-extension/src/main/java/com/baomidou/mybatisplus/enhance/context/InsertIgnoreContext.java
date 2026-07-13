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

    /**
     * 当前执行链进入 {@code INSERT IGNORE} 作用域的嵌套深度。
     */
    private static final TransmittableThreadLocal<Integer> DEPTH = new TransmittableThreadLocal<>();

    /**
     * 工具类不允许实例化。
     */
    private InsertIgnoreContext() {
    }

    /**
     * 开启当前线程的 {@code INSERT IGNORE} 作用域。
     *
     * <p>作用域支持嵌套；关闭内层作用域时会恢复外层深度。调用方应优先使用
     * try-with-resources，避免异常路径遗留上下文。</p>
     *
     * @return 必须关闭的作用域句柄
     */
    public static Scope open() {
        Integer previousDepth = DEPTH.get();
        DEPTH.set(Objects.isNull(previousDepth) ? 1 : previousDepth + 1);
        return new Scope(previousDepth);
    }

    /**
     * 判断当前线程是否启用了 {@code INSERT IGNORE} 改写。
     *
     * @return 作用域深度大于零时返回 {@code true}
     */
    public static boolean isEnabled() {
        Integer depth = DEPTH.get();
        return Objects.nonNull(depth) && depth > 0;
    }

    /**
     * 强制清理当前线程的作用域状态。
     *
     * <p>该方法主要用于请求结束、线程复用和测试清理；常规业务代码应关闭
     * {@link Scope}，以便正确恢复嵌套状态。</p>
     */
    public static void clear() {
        DEPTH.remove();
    }

    /**
     * 可自动恢复的 {@code INSERT IGNORE} 作用域句柄。
     *
     * <p>重复调用 {@link #close()} 是安全的。</p>
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 打开当前作用域前的嵌套深度；未启用时为 {@code null}。
         */
        private final Integer previousDepth;

        /**
         * 是否已经完成恢复，防止重复关闭破坏外层作用域。
         */
        private boolean closed;

        /**
         * 创建作用域恢复句柄。
         *
         * @param previousDepth 打开作用域前的嵌套深度
         */
        private Scope(Integer previousDepth) {
            this.previousDepth = previousDepth;
        }

        /**
         * 关闭当前作用域并恢复进入前的嵌套深度。
         */
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
