package com.baomidou.mybatisplus.enhance.observation;

import lombok.Value;

import java.util.Objects;

/**
 * SQL 执行观测结果值对象。
 *
 * <p>记录 Mapper 标识、SQL 文本、实际执行耗时和失败原因。对象不可变，适合在线程边界外
 * 交给日志、指标或链路追踪组件消费。</p>
 */
@Value
public class SqlObservation {

    String mappedStatementId;
    String sql;
    long elapsedNanos;
    Throwable failure;

    /**
     * 将纳秒耗时转换为毫秒。
     *
     * @return 向下取整后的毫秒耗时
     */
    public long getElapsedMillis() {
        return elapsedNanos / 1_000_000L;
    }

    /**
     * 判断 SQL 执行是否成功。
     *
     * @return 未记录失败原因时返回 {@code true}
     */
    public boolean isSuccess() {
        return Objects.isNull(failure);
    }
}
