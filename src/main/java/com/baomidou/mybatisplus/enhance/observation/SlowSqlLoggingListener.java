package com.baomidou.mybatisplus.enhance.observation;

import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

/**
 * Logs SQL executions whose actual elapsed time exceeds a threshold.
 */
@Slf4j
public class SlowSqlLoggingListener implements SqlObservationListener {

    private final long thresholdMillis;
    private final int maxSqlLength;

    public SlowSqlLoggingListener(long thresholdMillis) {
        this(thresholdMillis, 500);
    }

    public SlowSqlLoggingListener(long thresholdMillis, int maxSqlLength) {
        if (thresholdMillis < 0) {
            throw new IllegalArgumentException("thresholdMillis must not be negative");
        }
        if (maxSqlLength < 1) {
            throw new IllegalArgumentException("maxSqlLength must be positive");
        }
        this.thresholdMillis = thresholdMillis;
        this.maxSqlLength = maxSqlLength;
    }

    @Override
    public void onCompleted(SqlObservation observation) {
        if (observation.getElapsedMillis() < thresholdMillis) {
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
