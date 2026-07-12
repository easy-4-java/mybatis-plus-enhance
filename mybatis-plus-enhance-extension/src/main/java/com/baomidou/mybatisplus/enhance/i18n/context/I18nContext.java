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

    private static final TransmittableThreadLocal<Locale> CURRENT_LOCALE = new TransmittableThreadLocal<>();

    /**
     * @return 当前 Locale；未设置时返回 {@code null}
     */
    public Locale getLocale() {
        return CURRENT_LOCALE.get();
    }

    /**
     * 设置当前 Locale，传入空值时清理上下文。
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

        private final I18nContext context;
        private final Locale previous;
        private boolean closed;

        private Scope(I18nContext context, Locale previous) {
            this.context = context;
            this.previous = previous;
        }

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
