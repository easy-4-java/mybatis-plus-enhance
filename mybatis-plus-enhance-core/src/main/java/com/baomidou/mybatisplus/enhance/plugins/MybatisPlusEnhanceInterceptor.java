package com.baomidou.mybatisplus.enhance.plugins;

import com.baomidou.mybatisplus.enhance.plugins.inner.EnhanceInnerInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.EnhancePhase;
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
import java.util.ArrayList;
import java.util.Objects;

/**
 * Enhanced interceptor chain entry point for MyBatis-Plus.
 *
 * <p>Extends the official {@link MybatisPlusInterceptor} with post-query, post-update, and
 * post-execution lifecycle callbacks for {@link EnhanceInnerInterceptor} implementations.
 * Typical use cases include query result decryption, data signature verification,
 * SQL observation, and other sidecar enhancements. Standard {@link InnerInterceptor}
 * implementations continue to execute in registration order without modification.</p>
 *
 * <p>Only one instance of this interceptor should be registered per MyBatis Configuration.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
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

    /**
     * Registers an inner interceptor and validates the enhance phase ordering.
     *
     * @param innerInterceptor the interceptor to register; must not be {@code null}
     * @throws IllegalArgumentException if the interceptor violates enhance phase ordering
     */
    @Override
    public void addInnerInterceptor(InnerInterceptor innerInterceptor) {
        Objects.requireNonNull(innerInterceptor, "innerInterceptor must not be null");
        List<InnerInterceptor> candidate = new ArrayList<>(getInterceptors());
        candidate.add(innerInterceptor);
        validateEnhanceOrder(candidate);
        super.addInnerInterceptor(innerInterceptor);
    }

    /**
     * Bulk-sets inner interceptors after validating enhance phase ordering.
     *
     * @param interceptors the complete interceptor list; must not be {@code null}
     * @throws IllegalArgumentException if the list violates enhance phase ordering
     */
    @Override
    public void setInterceptors(List<InnerInterceptor> interceptors) {
        Objects.requireNonNull(interceptors, "interceptors must not be null");
        validateEnhanceOrder(interceptors);
        super.setInterceptors(interceptors);
    }

    /**
     * Validates that all {@link EnhanceInnerInterceptor} instances appear in non-decreasing
     * phase order within the given list.
     */
    private void validateEnhanceOrder(List<InnerInterceptor> interceptors) {
        EnhancePhase previousPhase = null;
        Class<?> previousType = null;
        for (InnerInterceptor interceptor : interceptors) {
            if (!(interceptor instanceof EnhanceInnerInterceptor)) {
                continue;
            }
            EnhancePhase phase = ((EnhanceInnerInterceptor) interceptor).phase();
            if (phase == EnhancePhase.UNSPECIFIED) {
                continue;
            }
            if (Objects.nonNull(previousPhase) && phase.getOrder() < previousPhase.getOrder()) {
                throw new IllegalArgumentException("Invalid enhance interceptor order: "
                        + interceptor.getClass().getName() + " [" + phase + "] must not run after "
                        + previousType.getName() + " [" + previousPhase + "]");
            }
            previousPhase = phase;
            previousType = interceptor.getClass();
        }
    }

    /**
     * Dispatches MyBatis Executor and StatementHandler lifecycle events.
     *
     * <p>Query and update operations are intercepted so that post-execution enhance callbacks
     * can be triggered. StatementHandler events delegate to the standard InnerInterceptor
     * SQL preparation hooks.</p>
     *
     * @param invocation MyBatis plugin invocation context
     * @return the SQL execution result
     * @throws Throwable propagated from SQL execution or enhance processing failures
     */
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        Object[] args = invocation.getArgs();
        if (target instanceof Executor) {
            Executor executor = (Executor) target;
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

    /**
     * Executes a query and triggers post-query enhance callbacks and execution-complete notifications.
     *
     * @return the MyBatis query result list
     * @throws Throwable propagated from query execution or result enhancement failures
     */
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
                    result = Objects.requireNonNull(
                            enhanceInterceptor.afterQuery(
                                    executor, ms, parameter, rowBounds, resultHandler, boundSql, result),
                            () -> enhanceInterceptor.getClass().getName() + " returned a null query result");
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

    /**
     * Executes an update and triggers post-update enhance callbacks and execution-complete notifications.
     *
     * @return the MyBatis update result, typically the affected row count
     * @throws Throwable propagated from update execution or result enhancement failures
     */
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

    /**
     * Broadcasts the execution-complete event to all enhance interceptors.
     *
     * <p>This is a sidecar notification phase; runtime exceptions from individual implementations
     * are isolated and logged without affecting the SQL main flow.</p>
     */
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

    /**
     * Returns a diagnostic text representation of the registered interceptor chain.
     *
     * @return interceptor chain description
     */
    @Override
    public String toString() {
        return "MybatisPlusEnhanceInterceptor{interceptors=" + getInterceptors() + "}";
    }

}
