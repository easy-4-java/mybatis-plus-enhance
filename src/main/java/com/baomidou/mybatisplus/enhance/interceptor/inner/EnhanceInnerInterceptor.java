package com.baomidou.mybatisplus.enhance.interceptor.inner;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;

public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     * 成功完成后的结果增强处理。
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     * @param rtList        查询结果
     */
    default void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // do nothing
    }

    /**
     * {@link Executor#update(MappedStatement, Object)} 成功完成后的增强处理。
     *
     * @param executor     Executor（可能是代理对象）
     * @param ms           MappedStatement
     * @param parameter    parameter
     * @param boundSql     BoundSql
     * @param affectedRows 影响行数
     */
    default void afterUpdate(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                             int affectedRows) throws SQLException {
        // do nothing
    }

    /**
     * SQL 执行及结果增强全部完成后的生命周期通知。
     * <p>
     * 该通知同时覆盖查询、插入、更新、删除以及异常路径，适用于监控、追踪等旁路能力。
     * 实现不得抛出异常影响 SQL 主流程。
     *
     * @param executor     Executor（可能是代理对象）
     * @param ms           MappedStatement
     * @param parameter    parameter
     * @param boundSql     BoundSql
     * @param result       执行结果，失败时可能为 null
     * @param failure      执行或结果增强异常，成功时为 null
     * @param elapsedNanos Executor 实际执行耗时（纳秒）
     */
    default void afterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                                Object result, Throwable failure, long elapsedNanos) {
        // do nothing
    }

}
