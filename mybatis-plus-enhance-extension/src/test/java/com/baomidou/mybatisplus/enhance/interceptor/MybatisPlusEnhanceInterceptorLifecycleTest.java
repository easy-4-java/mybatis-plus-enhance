package com.baomidou.mybatisplus.enhance.plugins;

import com.baomidou.mybatisplus.enhance.observation.SqlObservation;
import com.baomidou.mybatisplus.enhance.plugins.inner.SqlObservationInnerInterceptor;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.*;

public class MybatisPlusEnhanceInterceptorLifecycleTest {

    @Test
    public void shouldPublishSuccessfulQueryThroughInnerChain() throws Throwable {
        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
        LifecycleRecorder recorder = new LifecycleRecorder();
        AtomicReference<SqlObservation> observation = new AtomicReference<>();
        interceptor.addInnerInterceptor(recorder);
        interceptor.addInnerInterceptor(new SqlObservationInnerInterceptor(observation::set));

        List<Object> expected = Collections.<Object>singletonList("result");
        Executor executor = executorReturning(expected, null);
        MappedStatement mappedStatement = mappedStatement("mapper.select", SqlCommandType.SELECT, "SELECT 1");

        Object result = interceptor.intercept(queryInvocation(executor, mappedStatement));

        assertSame(expected, result);
        assertEquals(1, recorder.afterQueryCount);
        assertEquals(1, recorder.afterExecutionCount);
        assertNull(recorder.failure);
        assertNotNull(observation.get());
        assertTrue(observation.get().isSuccess());
        assertEquals("SELECT 1", observation.get().getSql());
    }

    @Test
    public void shouldPublishQueryFailureThroughInnerChain() throws Throwable {
        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
        LifecycleRecorder recorder = new LifecycleRecorder();
        interceptor.addInnerInterceptor(recorder);
        IllegalStateException expected = new IllegalStateException("query failed");
        Executor executor = executorReturning(null, expected);
        MappedStatement mappedStatement = mappedStatement("mapper.select", SqlCommandType.SELECT, "SELECT 1");

        try {
            interceptor.intercept(queryInvocation(executor, mappedStatement));
            fail("Expected query failure");
        } catch (IllegalStateException actual) {
            assertSame(expected, actual);
        }

        assertEquals(0, recorder.afterQueryCount);
        assertEquals(1, recorder.afterExecutionCount);
        assertSame(expected, recorder.failure);
    }

    @Test
    public void shouldInvokeUpdateLifecycleThroughInnerChain() throws Throwable {
        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
        LifecycleRecorder recorder = new LifecycleRecorder();
        interceptor.addInnerInterceptor(recorder);
        Executor executor = executorReturning(Integer.valueOf(3), null);
        MappedStatement mappedStatement = mappedStatement("mapper.update", SqlCommandType.UPDATE,
                "UPDATE sample SET value = 1");
        Method method = Executor.class.getMethod("update", MappedStatement.class, Object.class);

        Object result = interceptor.intercept(new Invocation(executor, method, new Object[]{mappedStatement, null}));

        assertEquals(Integer.valueOf(3), result);
        assertEquals(1, recorder.afterUpdateCount);
        assertEquals(3, recorder.affectedRows);
        assertEquals(1, recorder.afterExecutionCount);
        assertNull(recorder.failure);
    }

    private Invocation queryInvocation(Executor executor, MappedStatement mappedStatement) throws Exception {
        Method method = Executor.class.getMethod("query", MappedStatement.class, Object.class,
                RowBounds.class, ResultHandler.class);
        return new Invocation(executor, method,
                new Object[]{mappedStatement, null, RowBounds.DEFAULT, null});
    }

    private MappedStatement mappedStatement(String id, SqlCommandType commandType, String sql) {
        Configuration configuration = new Configuration();
        return new MappedStatement.Builder(configuration, id, new StaticSqlSource(configuration, sql), commandType)
                .build();
    }

    private Executor executorReturning(Object result, RuntimeException failure) {
        return (Executor) Proxy.newProxyInstance(
                Executor.class.getClassLoader(),
                new Class<?>[]{Executor.class},
                (proxy, method, arguments) -> {
                    if ("createCacheKey".equals(method.getName())) {
                        return new CacheKey();
                    }
                    if ("query".equals(method.getName()) || "update".equals(method.getName())) {
                        if (failure != null) {
                            throw failure;
                        }
                        return result;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
                });
    }

    private static final class LifecycleRecorder implements EnhanceInnerInterceptor {

        private int afterQueryCount;
        private int afterUpdateCount;
        private int afterExecutionCount;
        private int affectedRows;
        private Throwable failure;

        @Override
        public void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                               ResultHandler<?> resultHandler, BoundSql boundSql, List<Object> result) {
            afterQueryCount++;
        }

        @Override
        public void afterUpdate(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                                int affectedRows) {
            afterUpdateCount++;
            this.affectedRows = affectedRows;
        }

        @Override
        public void afterExecution(Executor executor, MappedStatement ms, Object parameter, BoundSql boundSql,
                                   Object result, Throwable failure, long elapsedNanos) {
            afterExecutionCount++;
            this.failure = failure;
        }
    }
}
