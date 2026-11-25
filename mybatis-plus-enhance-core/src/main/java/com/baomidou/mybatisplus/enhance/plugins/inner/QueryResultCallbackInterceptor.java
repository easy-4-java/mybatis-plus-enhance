package com.baomidou.mybatisplus.enhance.plugins.inner;

import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;

/**
 * 在成功查询后，将标准化的 Mapper 参数及结果交给调用方。
 * 同一 SqlSessionFactory 只选择增强链或原生 MyBatis 插件其中一种注册方式。
 */
@Intercepts({
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {
                MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
})
public final class QueryResultCallbackInterceptor implements EnhanceInnerInterceptor, Interceptor {

    private final BiConsumer<Collection<Object>, List<?>> callback;

    /**
     * 创建查询回调拦截器。
     * @param callback 消费 Mapper 参数对象和查询结果的回调
     */
    public QueryResultCallbackInterceptor(BiConsumer<Collection<Object>, List<?>> callback) {
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    @Override
    public EnhancePhase phase() {
        return EnhancePhase.RESULT_PROCESSING;
    }

    @Override
    public List<Object> afterQuery(Executor executor, MappedStatement statement, Object parameter,
                                   RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql,
                                   List<Object> results) {
        callback.accept(ParameterUtils.extractParameters(parameter), results);
        return results;
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object results = invocation.proceed();
        if (results instanceof List<?>) {
            callback.accept(ParameterUtils.extractParameters(invocation.getArgs()[1]), (List<?>) results);
        }
        return results;
    }

    @Override
    public void setProperties(Properties properties) {
        Interceptor.super.setProperties(properties);
    }
}
