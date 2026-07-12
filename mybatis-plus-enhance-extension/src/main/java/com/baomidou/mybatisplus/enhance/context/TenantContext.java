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
 * 可透传的租户上下文。
 *
 * <p>租户标识保存在 {@link TransmittableThreadLocal} 中，可配合 TTL 提供的线程池包装器
 * 透传到异步任务。推荐使用 {@link #open(Object)} 创建可自动恢复的租户作用域。</p>
 *
 * <p>该类只管理租户标识，不负责决定表名、租户字段或 SQL 注入规则。</p>
 */
public class TenantContext {

    private static final TransmittableThreadLocal<Object> CURRENT_TENANT_ID = new TransmittableThreadLocal<>();

    /**
     * 获取当前租户标识。
     *
     * @return 当前租户标识；未设置时返回 {@code null}
     */
    public Object getCurrentTenantId() {
        return CURRENT_TENANT_ID.get();
    }

    /**
     * 设置当前租户标识。
     *
     * @param tenantId 租户标识；为 {@code null} 时清理当前上下文
     */
    public void setCurrentTenantId(Object tenantId) {
        if (Objects.isNull(tenantId)) {
            clear();
            return;
        }
        CURRENT_TENANT_ID.set(tenantId);
    }

    /**
     * 清理当前线程保存的租户标识。
     */
    public void clear() {
        CURRENT_TENANT_ID.remove();
    }

    /**
     * 在当前线程中切换租户，并在作用域关闭时恢复先前租户。
     *
     * @param tenantId 当前作用域的租户 ID
     * @return 可自动恢复上下文的租户作用域
     */
    public Scope open(Object tenantId) {
        Object previousTenantId = getCurrentTenantId();
        setCurrentTenantId(tenantId);
        return new Scope(this, previousTenantId);
    }

    /**
     * 可自动恢复的租户作用域句柄。
     *
     * <p>关闭作用域时恢复进入前的租户；重复关闭不会产生副作用。</p>
     */
    public static final class Scope implements AutoCloseable {

        private final TenantContext context;
        private final Object previousTenantId;
        private boolean closed;

        private Scope(TenantContext context, Object previousTenantId) {
            this.context = context;
            this.previousTenantId = previousTenantId;
        }

        /**
         * 关闭租户作用域并恢复先前租户。
         */
        @Override
        public void close() {
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
