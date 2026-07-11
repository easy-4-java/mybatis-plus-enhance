package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.threadpool.TtlExecutors;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InsertIgnoreContextTest {

    @After
    public void cleanup() {
        InsertIgnoreContext.clear();
    }

    @Test
    public void shouldRestoreNestedScopes() {
        assertFalse(InsertIgnoreContext.isEnabled());
        try (InsertIgnoreContext.Scope outer = InsertIgnoreContext.open()) {
            assertTrue(InsertIgnoreContext.isEnabled());
            try (InsertIgnoreContext.Scope inner = InsertIgnoreContext.open()) {
                assertTrue(InsertIgnoreContext.isEnabled());
            }
            assertTrue(InsertIgnoreContext.isEnabled());
        }
        assertFalse(InsertIgnoreContext.isEnabled());
    }

    @Test
    public void shouldTransmitContextToThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            Future<Boolean> enabled;
            try (InsertIgnoreContext.Scope ignored = InsertIgnoreContext.open()) {
                enabled = executor.submit(InsertIgnoreContext::isEnabled);
            }

            assertTrue(enabled.get());
            assertFalse(InsertIgnoreContext.isEnabled());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldCaptureContextForEachThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            Future<Boolean> enabled;
            try (InsertIgnoreContext.Scope ignored = InsertIgnoreContext.open()) {
                enabled = executor.submit(InsertIgnoreContext::isEnabled);
            }
            Future<Boolean> disabled = executor.submit(InsertIgnoreContext::isEnabled);

            assertTrue(enabled.get());
            assertFalse(disabled.get());
        } finally {
            executor.shutdownNow();
        }
    }
}
