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
package com.baomidou.mybatisplus.enhance.plugins.handler;

import com.baomidou.mybatisplus.enhance.context.TenantContext;
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

    /**
     * 默认租户字段名。
     */
    public static final String DEFAULT_TENANT_COLUMN = "tenant_id";

    /**
     * 默认不忽略任何业务表的判断策略。
     */
    private static final Predicate<String> NEVER_IGNORE = tableName -> false;

    /**
     * 提供当前执行链租户标识的上下文。
     */
    private final TenantContext context;

    /**
     * 追加到租户 SQL 条件中的字段名。
     */
    private final String tenantColumn;

    /**
     * 判断指定表是否跳过租户条件的策略。
     */
    private final Predicate<String> ignoredTable;

    /**
     * 使用默认租户字段 {@value #DEFAULT_TENANT_COLUMN} 创建处理器。
     *
     * @param context 租户上下文
     */
    public DefaultTenantLineHandler(TenantContext context) {
        this(context, DEFAULT_TENANT_COLUMN, NEVER_IGNORE);
    }

    /**
     * 创建可配置租户字段和忽略表策略的处理器。
     *
     * @param context      租户上下文
     * @param tenantColumn 租户字段名
     * @param ignoredTable 返回 {@code true} 时跳过租户条件的表判断器
     */
    public DefaultTenantLineHandler(TenantContext context, String tenantColumn, Predicate<String> ignoredTable) {
        this.context = Objects.requireNonNull(context, "TenantContext must not be null");
        this.tenantColumn = Objects.requireNonNull(tenantColumn, "Tenant column must not be null");
        this.ignoredTable = Objects.requireNonNull(ignoredTable, "Ignored-table predicate must not be null");
    }

    /**
     * 将当前租户标识转换为 JSqlParser 表达式。
     *
     * @return 数字租户使用 {@link LongValue}，其他类型使用 {@link StringValue}
     * @throws IllegalStateException 当前上下文不存在租户标识时抛出
     */
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

    /**
     * 获取租户字段名。
     *
     * @return 租户字段名
     */
    @Override
    public String getTenantIdColumn() {
        return tenantColumn;
    }

    /**
     * 判断指定表是否跳过租户条件。
     *
     * @param tableName 数据库表名
     * @return 忽略时返回 {@code true}
     */
    @Override
    public boolean ignoreTable(String tableName) {
        return ignoredTable.test(tableName);
    }
}
