package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.function.Function;

/** 将已有 SQL 条件策略适配到官方 MyBatis-Plus 数据权限插件。 */
public final class SqlDataScopeHandler implements MultiDataPermissionHandler {

    private final Function<String, String> conditionProvider;

    /**
     * 创建 SQL 条件适配器。
     * @param conditionProvider 根据 Mapper 语句 ID 返回可信的 SQL 条件，空白表示无需附加条件
     */
    public SqlDataScopeHandler(Function<String, String> conditionProvider) {
        this.conditionProvider = Objects.requireNonNull(conditionProvider, "conditionProvider");
    }

    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        String condition = conditionProvider.apply(mappedStatementId);
        if (StringUtils.isBlank(condition)) {
            return null;
        }
        try {
            Expression expression = CCJSqlParserUtil.parseCondExpression(condition, false);
            // 权限 OR 与已有业务条件相交时必须作为一个整体。
            if (expression instanceof OrExpression) {
                return new ParenthesedExpressionList<>(expression);
            }
            return expression;
        } catch (JSQLParserException exception) {
            throw new IllegalArgumentException("Invalid data scope for " + mappedStatementId, exception);
        }
    }
}
