package com.baomidou.mybatisplus.enhance.observation;

/**
 * SQL 执行观测结果接收器。
 *
 * <p>该接口是观测数据的消费端口。实现方可以将结果写入日志、指标系统、
 * 链路追踪系统或业务线程上下文，而无需修改 SQL 拦截器。</p>
 *
 * @since 2.0.0
 */
@FunctionalInterface
public interface SqlObservationSink {

    /**
     * 接收一次已完成的 SQL 执行观测。
     *
     * @param observation SQL 执行观测结果
     */
    void accept(SqlObservation observation);
}
