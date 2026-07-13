package com.baomidou.mybatisplus.enhance.i18n.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Locale;
import java.util.Objects;

/**
 * 可透传的语言区域上下文。
 * <p>
 * 推荐通过 {@link #open(Locale)} 与 try-with-resources 限定作用域，以便在嵌套调用和
 * 异常路径中恢复之前的 Locale。异步线程仍需使用 TTL 包装的执行器。
 */
public class I18nContext {

    /**
     * 当前执行链绑定的语言区域。
     */
    private static final TransmittableThreadLocal<Locale> CURRENT_LOCALE = new TransmittableThreadLocal<>();

    /**
     * @return 当前 Locale；未设置时返回 {@code null}
     */
    public Locale getLocale() {
        return CURRENT_LOCALE.get();
    }

    /**
     * 设置当前 Locale，传入空值时清理上下文。
     *
     * @param locale 当前语言区域；为 {@code null} 时清理上下文
     */
    public void setLocale(Locale locale) {
        if (Objects.isNull(locale)) {
            clear();
            return;
        }
        CURRENT_LOCALE.set(locale);
    }

    /**
     * 清理当前线程的 Locale。
     */
    public void clear() {
        CURRENT_LOCALE.remove();
    }

    /**
     * 打开临时 Locale 作用域。
     *
     * @param locale 作用域内使用的 Locale
     * @return 关闭时自动恢复旧值的句柄
     */
    public Scope open(Locale locale) {
        Locale previous = getLocale();
        setLocale(locale);
        return new Scope(this, previous);
    }

    /**
     * 可自动恢复的 Locale 作用域。
     */
    public static final class Scope implements AutoCloseable {

        /**
         * 负责恢复语言区域的上下文实例。
         */
        private final I18nContext context;

        /**
         * 打开作用域前绑定的语言区域。
         */
        private final Locale previous;

        /**
         * 是否已关闭，用于保证恢复操作幂等。
         */
        private boolean closed;

        /**
         * 创建语言区域作用域恢复句柄。
         *
         * @param context  语言区域上下文
         * @param previous 进入作用域前的语言区域
         */
        private Scope(I18nContext context, Locale previous) {
            this.context = context;
            this.previous = previous;
        }

        /**
         * 关闭当前作用域并恢复进入前的语言区域。
         */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            context.setLocale(previous);
            closed = true;
        }
    }
}
