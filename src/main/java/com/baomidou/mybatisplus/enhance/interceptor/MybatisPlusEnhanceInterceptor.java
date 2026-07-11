package com.baomidou.mybatisplus.enhance.interceptor;

import com.baomidou.mybatisplus.enhance.interceptor.inner.EnhanceInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
 * 参考：
 * - <a href="https://blog.csdn.net/tianmaxingkonger/article/details/130986784">...</a>
 */
@Intercepts(
    {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
    }
)
@Slf4j
public class MybatisPlusEnhanceInterceptor extends MybatisPlusInterceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        if (target instanceof Executor) {
            final Executor executor = (Executor) target;
            Object parameter = args[1];
            boolean isUpdate = args.length == 2;
            MappedStatement ms = (MappedStatement) args[0];
            if (!isUpdate && ms.getSqlCommandType() == SqlCommandType.SELECT) {
                RowBounds rowBounds = (RowBounds) args[2];
                ResultHandler<?> resultHandler = (ResultHandler<?>) args[3];
                BoundSql boundSql;
                if (args.length == 4) {
                    boundSql = ms.getBoundSql(parameter);
                } else {
                    // 几乎不可能走进这里面,除非使用Executor的代理对象调用query[args[6]]
                    boundSql = (BoundSql) args[5];
                }
                for (InnerInterceptor interceptor : super.getInterceptors()) {
                    if (!interceptor.willDoQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql)) {
                        return Collections.emptyList();
                    }
                    interceptor.beforeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
                }
                return executeQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql);
            } else if (isUpdate) {
                for (InnerInterceptor update : super.getInterceptors()) {
                    if (!update.willDoUpdate(executor, ms, parameter)) {
                        return -1;
                    }
                    update.beforeUpdate(executor, ms, parameter);
                }
                BoundSql boundSql = ms.getBoundSql(parameter);
                return executeUpdate(invocation, executor, ms, parameter, boundSql);
            }
        } else {
            // StatementHandler
            final StatementHandler sh = (StatementHandler) target;
            // 目前只有StatementHandler.getBoundSql方法args才为null
            if (Objects.isNull(args)) {
                for (InnerInterceptor innerInterceptor : super.getInterceptors()) {
                    innerInterceptor.beforeGetBoundSql(sh);
                }
            } else {
                Connection connections = (Connection) args[0];
                Integer transactionTimeout = (Integer) args[1];
                for (InnerInterceptor innerInterceptor : super.getInterceptors()) {
                    innerInterceptor.beforePrepare(sh, connections, transactionTimeout);
                }
            }
        }
        return invocation.proceed();
    }

    private Object executeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                                ResultHandler<?> resultHandler, BoundSql boundSql) throws Throwable {
        CacheKey cacheKey = executor.createCacheKey(ms, parameter, rowBounds, boundSql);
        List<Object> result = null;
        Throwable failure = null;
        long startedAt = System.nanoTime();
        long elapsedNanos = 0L;
        try {
            result = executor.query(ms, parameter, rowBounds, resultHandler, cacheKey, boundSql);
            elapsedNanos = System.nanoTime() - startedAt;
            for (InnerInterceptor interceptor : super.getInterceptors()) {
                if (interceptor instanceof EnhanceInnerInterceptor) {
                    EnhanceInnerInterceptor enhanceInterceptor = (EnhanceInnerInterceptor) interceptor;
                    enhanceInterceptor.afterQuery(executor, ms, parameter, rowBounds, resultHandler, boundSql, result);
                }
            }
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            if (elapsedNanos == 0L) {
                elapsedNanos = System.nanoTime() - startedAt;
            }
            notifyAfterExecution(executor, ms, parameter, boundSql, result, failure, elapsedNanos);
        }
    }

    private Object executeUpdate(Invocation invocation, Executor executor, MappedStatement ms, Object parameter,
                                 BoundSql boundSql) throws Throwable {
        Object result = null;
        Throwable failure = null;
        long startedAt = System.nanoTime();
        long elapsedNanos = 0L;
        try {
            result = invocation.proceed();
            elapsedNanos = System.nanoTime() - startedAt;
            int affectedRows = result instanceof Number ? ((Number) result).intValue() : 0;
            for (InnerInterceptor interceptor : super.getInterceptors()) {
                if (interceptor instanceof EnhanceInnerInterceptor) {
                    EnhanceInnerInterceptor enhanceInterceptor = (EnhanceInnerInterceptor) interceptor;
                    enhanceInterceptor.afterUpdate(executor, ms, parameter, boundSql, affectedRows);
                }
            }
            return result;
        } catch (Throwable throwable) {
            failure = throwable;
            throw throwable;
        } finally {
            if (elapsedNanos == 0L) {
                elapsedNanos = System.nanoTime() - startedAt;
            }
            notifyAfterExecution(executor, ms, parameter, boundSql, result, failure, elapsedNanos);
        }
    }

    private void notifyAfterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                                      Object result, Throwable failure, long elapsedNanos) {
        for (InnerInterceptor interceptor : super.getInterceptors()) {
            if (!(interceptor instanceof EnhanceInnerInterceptor)) {
                continue;
            }
            EnhanceInnerInterceptor enhanceInterceptor = (EnhanceInnerInterceptor) interceptor;
            try {
                enhanceInterceptor.afterExecution(
                        executor, ms, parameter, boundSql, result, failure, elapsedNanos);
            } catch (RuntimeException exception) {
                log.warn("Enhance after-execution listener failed: {}",
                        enhanceInterceptor.getClass().getName(), exception);
            }
        }
    }

    @Override
    public String toString() {
        return "MybatisPlusEnhanceInterceptor{interceptors=" + getInterceptors() + "}";
    }

}
