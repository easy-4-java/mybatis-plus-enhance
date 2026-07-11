/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.tenant;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

public class TenantContext {

	private static final TransmittableThreadLocal<Object> CURRENT_TENANT_ID = new TransmittableThreadLocal<>();

	public void setCurrentTenantId(Object tenantId) {
		if (Objects.isNull(tenantId)) {
			clear();
			return;
		}
		CURRENT_TENANT_ID.set(tenantId);
	}

	public Object getCurrentTenantId() {
		return CURRENT_TENANT_ID.get();
	}

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

	public static final class Scope implements AutoCloseable {

		private final TenantContext context;
		private final Object previousTenantId;
		private boolean closed;

		private Scope(TenantContext context, Object previousTenantId) {
			this.context = context;
			this.previousTenantId = previousTenantId;
		}

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
