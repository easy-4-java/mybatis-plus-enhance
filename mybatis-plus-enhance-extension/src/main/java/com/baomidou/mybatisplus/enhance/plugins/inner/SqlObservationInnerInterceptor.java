package com.baomidou.mybatisplus.enhance.plugins.inner;

import com.baomidou.mybatisplus.enhance.observation.SqlObservation;
import com.baomidou.mybatisplus.enhance.observation.SqlObservationSink;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 从统一的 MyBatis-Plus 增强拦截链发布 SQL 执行观测结果。
 *
 * <p>默认通过 {@link ServiceLoader} 发现 {@link SqlObservationSink}，也支持通过
 * 构造方法或 {@link #addSink(SqlObservationSink)} 显式注册。
 */
@Slf4j
public class SqlObservationInnerInterceptor implements EnhanceInnerInterceptor {

    private final List<SqlObservationSink> sinks = new CopyOnWriteArrayList<>();

    /**
     * 创建拦截器并通过 {@link ServiceLoader} 自动发现观测接收器。
     */
    public SqlObservationInnerInterceptor() {
        ServiceLoader.load(SqlObservationSink.class).forEach(this::addSink);
    }

    /**
     * 创建拦截器，并在自动发现结果基础上追加指定接收器。
     *
     * @param sink SQL 观测接收器；为 {@code null} 时忽略
     */
    public SqlObservationInnerInterceptor(SqlObservationSink sink) {
        this();
        addSink(sink);
    }

    /**
     * 添加 SQL 观测接收器。
     *
     * <p>相同实例不会重复加入；接收器集合支持运行期并发读取。</p>
     *
     * @param sink 待添加的接收器
     */
    public void addSink(SqlObservationSink sink) {
        if (Objects.nonNull(sink) && !sinks.contains(sink)) {
            sinks.add(sink);
        }
    }

    /**
     * 将 SQL 执行结果转换为不可变观测对象并分发给全部接收器。
     *
     * <p>单个接收器抛出的运行时异常会被记录并隔离，不影响其他接收器。</p>
     *
     * @param executor     MyBatis 执行器
     * @param ms           映射语句元数据
     * @param parameter    Mapper 参数
     * @param boundSql     已绑定 SQL
     * @param result       执行结果
     * @param failure      执行异常，成功时为 {@code null}
     * @param elapsedNanos 实际执行耗时，单位纳秒
     */
    @Override
    public void afterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                               Object result, Throwable failure, long elapsedNanos) {
        SqlObservation observation = new SqlObservation(
                ms.getId(), Objects.isNull(boundSql) ? null : boundSql.getSql(), elapsedNanos, failure);
        for (SqlObservationSink sink : sinks) {
            try {
                sink.accept(observation);
            } catch (RuntimeException exception) {
                log.warn("SQL observation sink failed: {}", sink.getClass().getName(), exception);
            }
        }
    }
}
