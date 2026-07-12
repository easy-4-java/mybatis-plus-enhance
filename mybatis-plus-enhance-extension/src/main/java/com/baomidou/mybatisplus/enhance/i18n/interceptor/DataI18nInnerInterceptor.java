package com.baomidou.mybatisplus.enhance.i18n.interceptor;

import com.baomidou.mybatisplus.enhance.i18n.context.I18nContext;
import com.baomidou.mybatisplus.enhance.i18n.handler.DataI18nHandler;
import com.baomidou.mybatisplus.enhance.i18n.provider.LocaleProvider;
import com.baomidou.mybatisplus.enhance.plugins.inner.EnhanceInnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * MyBatis 查询结果国际化内部拦截器。
 * <p>
 * 依赖 {@code MybatisPlusEnhanceInterceptor} 提供的查询后生命周期，不修改 SQL，
 * 只在 MyBatis 完成结果映射后委托 {@link DataI18nHandler} 处理。
 */
public class DataI18nInnerInterceptor implements EnhanceInnerInterceptor {

    private final LocaleProvider localeProvider;
    private final DataI18nHandler i18nHandler;

    /**
     * 使用 {@link I18nContext} 提供 Locale。
     */
    public DataI18nInnerInterceptor(I18nContext context, DataI18nHandler i18nHandler) {
        this(context::getLocale, i18nHandler);
    }

    /**
     * 使用自定义 Locale 提供者。
     */
    public DataI18nInnerInterceptor(LocaleProvider localeProvider, DataI18nHandler i18nHandler) {
        this.localeProvider = Objects.requireNonNull(localeProvider, "LocaleProvider must not be null");
        this.i18nHandler = Objects.requireNonNull(i18nHandler, "DataI18nHandler must not be null");
    }

    @Override
    public void afterQuery(Executor executor, MappedStatement ms, Object parameter,
                           RowBounds rowBounds, ResultHandler<?> resultHandler,
                           BoundSql boundSql, List<Object> results) throws SQLException {
        Locale locale = localeProvider.getLocale();
        if (Objects.isNull(locale) || Objects.isNull(results) || results.isEmpty()) {
            return;
        }
        i18nHandler.handle(locale, ms, results);
    }
}
