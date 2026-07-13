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
 * MyBatis-Plus 增强拦截器链入口。
 *
 * <p>兼容官方 {@link MybatisPlusInterceptor} 的前置生命周期，并为
 * {@link EnhanceInnerInterceptor} 增加查询成功后、更新成功后和执行完成后的回调。</p>
 *
 * <p>典型用途包括查询结果解密、数据验签、SQL 观测和其他旁路增强。普通官方
 * {@link InnerInterceptor} 仍按注册顺序执行，不需要改造。</p>
 *
 * <p>同一 MyBatis Configuration 中只应注册一个外层拦截器链入口。</p>
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
     * 注册内部拦截器并立即校验框架增强阶段顺序。
     *
     * @param innerInterceptor 待注册拦截器
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
     * 批量设置内部拦截器，并在写入父类前验证阶段顺序。
     *
     * @param interceptors 完整拦截器列表
     */
    @Override
    public void setInterceptors(List<InnerInterceptor> interceptors) {
        Objects.requireNonNull(interceptors, "interceptors must not be null");
        validateEnhanceOrder(interceptors);
        super.setInterceptors(interceptors);
    }

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
     * 分派 MyBatis Executor 与 StatementHandler 生命周期。
     *
     * <p>查询和更新由本类负责执行，以便在真实执行完成后触发增强回调；StatementHandler
     * 仍调用官方 InnerInterceptor 的 SQL 准备钩子。</p>
     *
     * @param invocation MyBatis 插件调用上下文
     * @return SQL 执行结果
     * @throws Throwable SQL 执行或前置增强失败时透传
     */
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

    /**
     * 执行查询并依次触发查询后增强和执行完成通知。
     *
     * @return MyBatis 查询结果
     * @throws Throwable 查询或结果增强失败时透传
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
     * 执行更新并依次触发更新后增强和执行完成通知。
     *
     * @return MyBatis 更新结果，通常为影响行数
     * @throws Throwable 更新或结果增强失败时透传
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
     * 向全部增强拦截器广播执行完成事件。
     *
     * <p>该阶段属于旁路通知，单个实现的运行时异常会被隔离并记录。</p>
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
     * 返回当前已注册拦截器列表的诊断文本。
     *
     * @return 拦截器链描述
     */
    @Override
    public String toString() {
        return "MybatisPlusEnhanceInterceptor{interceptors=" + getInterceptors() + "}";
    }

}
