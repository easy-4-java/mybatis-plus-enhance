package com.baomidou.mybatisplus.enhance.datascope.toolkit;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 数据权限 JSqlParser 表达式工具。
 * <p>
 * 用于自定义 {@code DataScopeExpressionProvider} 安全构造常见的等值、IN、
 * AND 和 OR 条件，避免业务实现直接拼接 SQL 字符串。
 */
public final class DataScopeExpressions {

    /**
     * 工具类不允许实例化。
     */
    private DataScopeExpressions() {
    }

    /**
     * 构造带可选表别名的列。
     *
     * @param tableAlias SQL 表别名
     * @param columnName 列名
     * @return JSqlParser 列表达式
     */
    public static Column column(String tableAlias, String columnName) {
        Objects.requireNonNull(columnName, "Column name must not be null");
        return new Column(StringUtils.isBlank(tableAlias)
                ? columnName : tableAlias + "." + columnName);
    }

    /**
     * 构造等值条件。
     *
     * @param tableAlias SQL 表别名
     * @param columnName 列名
     * @param value      常量值
     * @return 等值表达式
     */
    public static Expression eq(String tableAlias, String columnName, Object value) {
        return new EqualsTo(column(tableAlias, columnName), literal(value));
    }

    /**
     * 构造 IN 条件。
     *
     * @param tableAlias SQL 表别名
     * @param columnName 列名
     * @param values     不能为空的值集合
     * @return IN 表达式
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Expression in(String tableAlias, String columnName, Collection<?> values) {
        Objects.requireNonNull(values, "Values must not be null");
        ExpressionList expressions = new ExpressionList(values.stream()
                .map(DataScopeExpressions::literal)
                .collect(Collectors.toList()));
        return new InExpression(column(tableAlias, columnName),
                new ParenthesedExpressionList(expressions));
    }

    /**
     * 构造 AND 组合条件。
     *
     * @param left  左侧条件表达式
     * @param right 右侧条件表达式
     * @return 使用 AND 连接的组合表达式
     */
    public static Expression and(Expression left, Expression right) {
        return new AndExpression(left, right);
    }

    /**
     * 构造 OR 组合条件。
     *
     * @param left  左侧条件表达式
     * @param right 右侧条件表达式
     * @return 使用 OR 连接的组合表达式
     */
    public static Expression or(Expression left, Expression right) {
        return new OrExpression(left, right);
    }

    /**
     * 将 Java 常量转换为 JSqlParser 字面量表达式。
     *
     * <p>整数族映射为 {@link LongValue}，其他数值映射为 {@link DoubleValue}，
     * 其余对象按字符串处理，空值映射为 {@link NullValue}。</p>
     *
     * @param value Java 常量值
     * @return 对应的 SQL 字面量表达式
     */
    private static Expression literal(Object value) {
        if (Objects.isNull(value)) {
            return new NullValue();
        }
        if (value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long) {
            return new LongValue(value.toString());
        }
        if (value instanceof Number) {
            return new DoubleValue(value.toString());
        }
        return new StringValue(value.toString());
    }
}
