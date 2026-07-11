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

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * 默认 MyBatis-Plus 租户处理器。
 *
 * <p>租户值由 {@link TenantContext} 提供，租户字段和忽略表策略可由使用方配置。</p>
 */
public class DefaultTenantLineHandler implements TenantLineHandler {

	public static final String DEFAULT_TENANT_COLUMN = "tenant_id";

	private static final Predicate<String> NEVER_IGNORE = tableName -> false;

	private final TenantContext context;
	private final String tenantColumn;
	private final Predicate<String> ignoredTable;

	public DefaultTenantLineHandler(TenantContext context) {
		this(context, DEFAULT_TENANT_COLUMN, NEVER_IGNORE);
	}

	public DefaultTenantLineHandler(TenantContext context, String tenantColumn, Predicate<String> ignoredTable) {
		this.context = Objects.requireNonNull(context, "TenantContext must not be null");
		this.tenantColumn = Objects.requireNonNull(tenantColumn, "Tenant column must not be null");
		this.ignoredTable = Objects.requireNonNull(ignoredTable, "Ignored-table predicate must not be null");
	}

	@Override
	public Expression getTenantId() {
		Object tenantId = context.getCurrentTenantId();
		if (Objects.isNull(tenantId)) {
			throw new IllegalStateException("Tenant ID is missing from TenantContext");
		}
		if (tenantId instanceof Number) {
			return new LongValue(tenantId.toString());
		}
		return new StringValue(tenantId.toString());
	}

	@Override
	public String getTenantIdColumn() {
		return tenantColumn;
	}

	@Override
	public boolean ignoreTable(String tableName) {
		return ignoredTable.test(tableName);
	}
}
