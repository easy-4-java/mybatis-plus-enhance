package com.baomidou.mybatisplus.enhance.interceptor;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.SQLException;
import java.util.List;

/**
 * MyBatis-Plus 增强拦截器生命周期扩展。
 *
 * <p>在官方 {@link InnerInterceptor} 的前置钩子基础上，增加查询成功后、更新成功后以及
 * SQL 执行完成后的回调。实现类由 {@code MybatisPlusEnhanceInterceptor} 统一调度。</p>
 *
 * <p>结果增强方法可以修改返回对象；执行完成方法应保持旁路特性，不得影响 SQL 主流程。</p>
 */
public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)}
     * 成功完成后的结果增强处理。
     *
     * @param executor      MyBatis 执行器，可能是代理对象
     * @param ms            映射语句元数据
     * @param parameter     Mapper 调用参数
     * @param rowBounds     分页边界
     * @param resultHandler 结果处理器
     * @param boundSql      已绑定的 SQL
     * @param rtList        查询结果列表
     * @throws SQLException 查询结果增强失败时抛出
     */
    default void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // do nothing
    }

    /**
     * {@link Executor#update(MappedStatement, Object)} 成功完成后的增强处理。
     *
     * @param executor     MyBatis 执行器，可能是代理对象
     * @param ms           映射语句元数据
     * @param parameter    Mapper 调用参数
     * @param boundSql     已绑定的 SQL
     * @param affectedRows 影响行数
     * @throws SQLException 更新结果增强失败时抛出
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
     * @param executor     MyBatis 执行器，可能是代理对象
     * @param ms           映射语句元数据
     * @param parameter    Mapper 调用参数
     * @param boundSql     已绑定的 SQL
     * @param result       执行结果，失败时可能为 {@code null}
     * @param failure      执行或结果增强异常，成功时为 {@code null}
     * @param elapsedNanos Executor 实际执行耗时（纳秒）
     */
    default void afterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                                Object result, Throwable failure, long elapsedNanos) {
        // do nothing
    }

}
