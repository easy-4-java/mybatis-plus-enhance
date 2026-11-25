/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

/**
 * Propagatable tenant context.
 *
 * <p>Stores the tenant identifier in a {@link TransmittableThreadLocal}, enabling
 * propagation to async tasks when paired with TTL thread-pool wrappers. It is
 * recommended to use {@link #open(Object)} to create an auto-restoring tenant scope.</p>
 *
 * <p>This class only manages the tenant identifier; it does not determine table names,
 * tenant columns, or SQL injection rules.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public class TenantContext {

    /**
     * Tenant identifier bound to the current execution chain.
     */
    private static final TransmittableThreadLocal<Object> CURRENT_TENANT_ID = new TransmittableThreadLocal<>();

    /**
     * Returns the current tenant identifier.
     *
     * @return the current tenant identifier, or {@code null} if not set
     */
    public Object getCurrentTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    /**
     * Sets the current tenant identifier.
     *
     * @param tenantId the tenant identifier; passing {@code null} clears the context
     */
    public void setCurrentTenantId(Object tenantId) {
        if (Objects.isNull(tenantId)) {
            clear();
            return;
        }
        CURRENT_TENANT_ID.set(tenantId);
    }

    /**
     * Clears the tenant identifier stored for the current thread.
     */
    public void clear() {
        CURRENT_TENANT_ID.remove();
    }

    /**
     * Switches the tenant for the current thread and returns a scope that restores
     * the previous tenant on close.
     * <p>作用域必须在创建线程关闭；异步任务应在任务线程内创建自己的作用域。</p>
     *
     * @param tenantId the tenant ID for this scope
     * @return an auto-restoring tenant scope handle
     */
    public Scope open(Object tenantId) {
        Object previousTenantId = getCurrentTenantId();
        setCurrentTenantId(tenantId);
        return new Scope(this, previousTenantId);
    }

    /**
     * Auto-restoring scope handle for tenant context.
     *
     * <p>只能由创建线程关闭并恢复原租户；同线程重复关闭保持幂等。
     * 跨线程关闭始终抛出异常，不改变租户上下文或关闭状态。</p>
     */
    public static final class Scope implements AutoCloseable {

        /** 创建此作用域的线程，使用对象身份校验归属。 */
        private final Thread ownerThread;

        /**
         * The context instance responsible for restoring tenant state.
         */
        private final TenantContext context;

        /**
         * The tenant identifier that was bound before this scope was opened.
         */
        private final Object previousTenantId;

        /**
         * Whether this scope has been closed, ensuring idempotent restoration.
         */
        private boolean closed;

        /**
         * Creates a tenant scope restoration handle.
         *
         * @param context          the tenant context
         * @param previousTenantId the tenant identifier before this scope was opened
         */
        private Scope(TenantContext context, Object previousTenantId) {
            this.ownerThread = Thread.currentThread();
            this.context = context;
            this.previousTenantId = previousTenantId;
        }

        /**
         * 在创建线程关闭作用域并恢复原租户；同线程重复关闭不改变上下文。
         *
         * @throws IllegalStateException 由其他线程关闭，包括已经关闭的作用域
         */
        @Override
        public void close() {
            // 在读取关闭状态或恢复租户前拒绝其他线程，防止污染调用线程的上下文。
            if (Thread.currentThread() != ownerThread) {
                throw new IllegalStateException("Tenant scope must be closed by its creating thread");
            }
            if (closed) {
                return;
            }
            if (Objects.isNull(previousTenantId)) {
                context.clear();
            } else {
                context.setCurrentTenantId(previousTenantId);
            }
            closed = true;
        }
    }

}
