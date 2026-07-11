package com.baomidou.mybatisplus.enhance.interceptor.inner;

import com.baomidou.mybatisplus.enhance.observation.SqlObservation;
import com.baomidou.mybatisplus.enhance.observation.SqlObservationListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Publishes SQL execution observations from the unified MyBatis-Plus enhance interceptor chain.
 */
@Slf4j
public class SqlObservationInnerInterceptor implements EnhanceInnerInterceptor {

    private final List<SqlObservationListener> listeners = new CopyOnWriteArrayList<>();

    public SqlObservationInnerInterceptor() {
    }

    public SqlObservationInnerInterceptor(SqlObservationListener listener) {
        addListener(listener);
    }

    public void addListener(SqlObservationListener listener) {
        if (Objects.nonNull(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void afterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                               Object result, Throwable failure, long elapsedNanos) {
        SqlObservation observation = new SqlObservation(
                ms.getId(), Objects.isNull(boundSql) ? null : boundSql.getSql(), elapsedNanos, failure);
        for (SqlObservationListener listener : listeners) {
            try {
                listener.onCompleted(observation);
            } catch (RuntimeException exception) {
                log.warn("SQL observation listener failed: {}", listener.getClass().getName(), exception);
            }
        }
    }
}
