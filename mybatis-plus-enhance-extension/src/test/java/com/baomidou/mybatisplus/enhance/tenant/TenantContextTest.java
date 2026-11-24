package com.baomidou.mybatisplus.enhance.tenant;

import com.alibaba.ttl.threadpool.TtlExecutors;
import com.baomidou.mybatisplus.enhance.context.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TenantContext}.
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
@DisplayName("TenantContext")
class TenantContextTest {

    private final TenantContext context = new TenantContext();

    @AfterEach
    void tearDown() {
        context.clear();
    }

    @Test
    @DisplayName("should return null when no tenant is set")
    void shouldReturnNullWhenNotSet() {
        assertThat(context.getCurrentTenantId()).isNull();
    }

    @Test
    @DisplayName("should set and get tenant identifier")
    void shouldSetAndGetTenantId() {
        context.setCurrentTenantId(1001L);
        assertThat(context.getCurrentTenantId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("should clear context when tenant is set to null")
    void shouldClearContextWhenTenantIsNull() {
        context.setCurrentTenantId(1001L);
        context.setCurrentTenantId(null);
        assertThat(context.getCurrentTenantId()).isNull();
    }

    @Test
    @DisplayName("should restore tenant after nested scope")
    void shouldRestoreTenantAfterNestedScope() {
        context.setCurrentTenantId(1001L);

        try (TenantContext.Scope ignored = context.open(2002L)) {
            assertThat(context.getCurrentTenantId()).isEqualTo(2002L);
            try (TenantContext.Scope nested = context.open(3003L)) {
                assertThat(context.getCurrentTenantId()).isEqualTo(3003L);
            }
            assertThat(context.getCurrentTenantId()).isEqualTo(2002L);
        }

        assertThat(context.getCurrentTenantId()).isEqualTo(1001L);
    }

    @Test
    @DisplayName("should be safe to close scope multiple times")
    void shouldBeSafeToCloseMultipleTimes() {
        context.setCurrentTenantId(1001L);
        TenantContext.Scope scope = context.open(2002L);
        scope.close();
        assertThat(context.getCurrentTenantId()).isEqualTo(1001L);
        context.setCurrentTenantId(3003L);
        scope.close();
        assertThat(context.getCurrentTenantId()).isEqualTo(3003L);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"owner-original"})
    void shouldRejectForeignCloseWithoutChangingTenantsAndAllowOwnerToRestore(String originalTenant) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        context.setCurrentTenantId(originalTenant);
        try (TenantContext.Scope scope = context.open("owner-temporary")) {
            executor.submit(() -> {
                context.setCurrentTenantId("worker-tenant");
                try {
                    assertThatThrownBy(scope::close).isInstanceOf(IllegalStateException.class);
                    assertThat(context.getCurrentTenantId()).isEqualTo("worker-tenant");
                } finally {
                    context.clear();
                }
            }).get(10, TimeUnit.SECONDS);
            assertThat(context.getCurrentTenantId()).isEqualTo("owner-temporary");
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(context.getCurrentTenantId()).isEqualTo(originalTenant);
    }

    @Test
    void shouldRejectForeignCloseEvenAfterOwnerClosedScope() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        TenantContext.Scope scope = context.open("temporary");
        scope.close();
        try {
            executor.submit(() -> {
                context.setCurrentTenantId("worker-tenant");
                try {
                    assertThatThrownBy(scope::close).isInstanceOf(IllegalStateException.class);
                    assertThat(context.getCurrentTenantId()).isEqualTo("worker-tenant");
                } finally {
                    context.clear();
                }
            }).get(10, TimeUnit.SECONDS);
            assertThat(context.getCurrentTenantId()).isNull();
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    @DisplayName("should transmit tenant context to TTL thread pool task")
    void shouldTransmitTenantContextToThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            context.setCurrentTenantId(1001L);
            Future<Object> tenantId = executor.submit(context::getCurrentTenantId);
            assertThat(tenantId.get()).isEqualTo(1001L);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("should capture context for each thread pool task independently")
    void shouldCaptureContextForEachThreadPoolTask() throws Exception {
        ExecutorService executor = TtlExecutors.getTtlExecutorService(Executors.newSingleThreadExecutor());
        try {
            context.setCurrentTenantId(1001L);
            Future<Object> firstTenantId = executor.submit(context::getCurrentTenantId);

            context.setCurrentTenantId(2002L);
            Future<Object> secondTenantId = executor.submit(context::getCurrentTenantId);

            assertThat(firstTenantId.get()).isEqualTo(1001L);
            assertThat(secondTenantId.get()).isEqualTo(2002L);
        } finally {
            executor.shutdownNow();
        }
    }
}
