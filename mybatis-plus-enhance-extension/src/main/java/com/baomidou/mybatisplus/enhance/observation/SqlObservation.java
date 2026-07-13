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

    /**
     * Mapper 方法对应的 MappedStatement 全限定标识。
     */
    String mappedStatementId;

    /**
     * MyBatis 生成的 SQL 文本；无法取得时为 {@code null}。
     */
    String sql;

    /**
     * Executor 实际执行耗时，单位纳秒。
     */
    long elapsedNanos;

    /**
     * SQL 执行或结果增强阶段的失败原因；成功时为 {@code null}。
     */
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
