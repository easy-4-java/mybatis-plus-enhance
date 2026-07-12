package com.baomidou.mybatisplus.enhance.tenant;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.baomidou.mybatisplus.enhance.context.TenantContext;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TenantContextTest {

    private final TenantContext context = new TenantContext();

    @After
    public void tearDown() {
        context.clear();
    }

    @Test
    public void shouldTransmitTenantContextToThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            context.setCurrentTenantId(1001L);
            Future<Object> tenantId = executor.submit(context::getCurrentTenantId);

            Assert.assertEquals(Long.valueOf(1001L), tenantId.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldCaptureContextForEachThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            context.setCurrentTenantId(1001L);
            Future<Object> firstTenantId = executor.submit(context::getCurrentTenantId);

            context.setCurrentTenantId(2002L);
            Future<Object> secondTenantId = executor.submit(context::getCurrentTenantId);

            Assert.assertEquals(Long.valueOf(1001L), firstTenantId.get());
            Assert.assertEquals(Long.valueOf(2002L), secondTenantId.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void shouldRestoreTenantAfterNestedScope() {
        context.setCurrentTenantId(1001L);

        try (TenantContext.Scope ignored = context.open(2002L)) {
            Assert.assertEquals(Long.valueOf(2002L), context.getCurrentTenantId());
            try (TenantContext.Scope nested = context.open(3003L)) {
                Assert.assertEquals(Long.valueOf(3003L), context.getCurrentTenantId());
            }
            Assert.assertEquals(Long.valueOf(2002L), context.getCurrentTenantId());
        }

        Assert.assertEquals(Long.valueOf(1001L), context.getCurrentTenantId());
    }

    @Test
    public void shouldClearContextWhenTenantIsNull() {
        context.setCurrentTenantId(1001L);

        context.setCurrentTenantId(null);

        Assert.assertNull(context.getCurrentTenantId());
    }
}
