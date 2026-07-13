package com.baomidou.mybatisplus.enhance.i18n.interceptor;

import com.baomidou.mybatisplus.enhance.i18n.context.I18nContext;
import com.baomidou.mybatisplus.enhance.i18n.handler.DataI18nHandler;
import com.baomidou.mybatisplus.enhance.i18n.provider.LocaleProvider;
import com.baomidou.mybatisplus.enhance.plugins.inner.EnhanceInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.EnhancePhase;
import com.baomidou.mybatisplus.enhance.result.ReflectionResultObjectCopier;
import com.baomidou.mybatisplus.enhance.result.ResultObjectCopier;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;

/**
 * MyBatis 查询结果国际化内部拦截器。
 * <p>
 * 依赖 {@code MybatisPlusEnhanceInterceptor} 提供的查询后生命周期，不修改 SQL，
 * 只在 MyBatis 完成结果映射后委托 {@link DataI18nHandler} 处理。
 */
public class DataI18nInnerInterceptor implements EnhanceInnerInterceptor {

    @Override
    public EnhancePhase phase() {
        return EnhancePhase.RESULT_I18N;
    }

    /**
     * 提供当前查询应使用的语言区域。
     */
    private final LocaleProvider localeProvider;

    /**
     * 对已映射查询结果执行国际化转换的处理器。
     */
    private final DataI18nHandler i18nHandler;

    /**
     * 在国际化处理前复制缓存结果对象的策略。
     */
    private final ResultObjectCopier resultObjectCopier;

    /**
     * 使用 {@link I18nContext} 提供 Locale。
     *
     * @param context     可透传的语言区域上下文
     * @param i18nHandler 查询结果国际化处理器
     */
    public DataI18nInnerInterceptor(I18nContext context, DataI18nHandler i18nHandler) {
        this(context::getLocale, i18nHandler);
    }

    /**
     * 使用自定义 Locale 提供者。
     *
     * @param localeProvider 当前语言区域提供者
     * @param i18nHandler    查询结果国际化处理器
     */
    public DataI18nInnerInterceptor(LocaleProvider localeProvider, DataI18nHandler i18nHandler) {
        this(localeProvider, i18nHandler, new ReflectionResultObjectCopier());
    }

    /**
     * 使用自定义结果复制策略创建国际化拦截器。
     *
     * @param localeProvider     当前语言区域提供者
     * @param i18nHandler        查询结果国际化处理器
     * @param resultObjectCopier 查询结果复制策略
     */
    public DataI18nInnerInterceptor(LocaleProvider localeProvider, DataI18nHandler i18nHandler,
                                    ResultObjectCopier resultObjectCopier) {
        this.localeProvider = Objects.requireNonNull(localeProvider, "LocaleProvider must not be null");
        this.i18nHandler = Objects.requireNonNull(i18nHandler, "DataI18nHandler must not be null");
        this.resultObjectCopier = Objects.requireNonNull(resultObjectCopier,
                "ResultObjectCopier must not be null");
    }

    /**
     * 在查询结果映射完成后，根据当前 Locale 委托国际化处理器转换结果。
     *
     * <p>未提供 Locale、结果为空或结果列表为空时直接跳过，不修改 SQL 和查询参数。</p>
     *
     * @param executor      MyBatis 执行器
     * @param ms            当前映射语句
     * @param parameter     Mapper 查询参数
     * @param rowBounds     分页边界
     * @param resultHandler 结果处理器
     * @param boundSql      已绑定 SQL
     * @param results       MyBatis 已映射结果列表
     * @throws SQLException 国际化处理器访问数据失败时抛出
     */
    @Override
    public List<Object> afterQuery(Executor executor, MappedStatement ms, Object parameter,
                                   RowBounds rowBounds, ResultHandler<?> resultHandler,
                                   BoundSql boundSql, List<Object> results) throws SQLException {
        Locale locale = localeProvider.getLocale();
        if (Objects.isNull(locale) || Objects.isNull(results) || results.isEmpty()) {
            return results;
        }
        List<Object> detachedResults = new ArrayList<>(results.size());
        for (Object result : results) {
            detachedResults.add(resultObjectCopier.copy(result));
        }
        i18nHandler.handle(locale, ms, detachedResults);
        return detachedResults;
    }
}
