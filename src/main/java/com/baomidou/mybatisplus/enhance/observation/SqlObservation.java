package com.baomidou.mybatisplus.enhance.observation;

import lombok.Data;

import java.util.Objects;

/**
 * Immutable SQL execution observation.
 */
@Data
public final class SqlObservation {

    private final String mappedStatementId;
    private final String sql;
    private final long elapsedNanos;
    private final Throwable failure;

    public long getElapsedMillis() {
        return elapsedNanos / 1_000_000L;
    }

    public boolean isSuccess() {
        return Objects.isNull(failure);
    }
}
