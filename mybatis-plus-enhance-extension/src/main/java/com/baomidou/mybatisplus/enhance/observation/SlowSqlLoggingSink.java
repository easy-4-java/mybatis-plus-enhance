package com.baomidou.mybatisplus.enhance.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * 慢 SQL 日志接收器。
 *
 * <p>根据真实执行耗时筛选 SQL，并限制日志中 SQL 文本长度。该实现只记录日志，
 * 不改变执行结果，也不会重新执行 SQL。</p>
 */
@Slf4j
public class SlowSqlLoggingSink implements SqlObservationSink {

    private final long thresholdMillis;
    private final int maxSqlLength;

    /**
     * 使用默认最大 SQL 展示长度 500 创建接收器。
     *
     * @param thresholdMillis 慢 SQL 阈值，单位毫秒
     */
    public SlowSqlLoggingSink(long thresholdMillis) {
        this(thresholdMillis, 500);
    }

    /**
     * 创建可配置阈值和最大展示长度的接收器。
     *
     * @param thresholdMillis 慢 SQL 阈值，单位毫秒，不能为负数
     * @param maxSqlLength    日志中 SQL 最大字符数，必须大于零
     * @throws IllegalArgumentException 参数不满足约束时抛出
     */
    public SlowSqlLoggingSink(long thresholdMillis, int maxSqlLength) {
        if (thresholdMillis < 0) {
            throw new IllegalArgumentException("thresholdMillis must not be negative");
        }
        if (maxSqlLength < 1) {
            throw new IllegalArgumentException("maxSqlLength must be positive");
        }
        this.thresholdMillis = thresholdMillis;
        this.maxSqlLength = maxSqlLength;
    }

    /**
     * 当观测耗时达到阈值时写入 WARN 日志。
     *
     * @param observation SQL 执行观测结果；为 {@code null} 时忽略
     */
    @Override
    public void accept(SqlObservation observation) {
        if (Objects.isNull(observation) || observation.getElapsedMillis() < thresholdMillis) {
            return;
        }
        String sql = observation.getSql();
        String displayedSql = Objects.isNull(sql) || sql.length() <= maxSqlLength
                ? sql
                : sql.substring(0, maxSqlLength) + "...";
        log.warn("Slow SQL [elapsedMs={}, mapper={}, success={}]: {}",
                observation.getElapsedMillis(), observation.getMappedStatementId(),
                observation.isSuccess(), displayedSql);
    }
}
