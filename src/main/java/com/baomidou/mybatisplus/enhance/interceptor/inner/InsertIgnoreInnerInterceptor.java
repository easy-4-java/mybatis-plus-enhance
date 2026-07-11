package com.baomidou.mybatisplus.enhance.interceptor.inner;

import com.baomidou.mybatisplus.enhance.context.InsertIgnoreContext;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.commons.lang3.StringUtils;

import java.sql.Connection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites an INSERT statement to MySQL INSERT IGNORE in an explicit scope.
 */
public class InsertIgnoreInnerInterceptor implements EnhanceInnerInterceptor {

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "^(\\s*)INSERT\\s+(?!IGNORE\\b)", Pattern.CASE_INSENSITIVE);

    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection, Integer transactionTimeout) {
        if (!InsertIgnoreContext.isEnabled()) {
            return;
        }
        MetaObject statementMeta = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement = (MappedStatement) statementMeta.getValue("delegate.mappedStatement");
        if (Objects.isNull(mappedStatement) || mappedStatement.getSqlCommandType() != SqlCommandType.INSERT) {
            return;
        }
        BoundSql boundSql = statementHandler.getBoundSql();
        if (Objects.isNull(boundSql)) {
            return;
        }
        MetaObject boundSqlMeta = SystemMetaObject.forObject(boundSql);
        boundSqlMeta.setValue("sql", rewriteSql(boundSql.getSql()));
    }

    static String rewriteSql(String sql) {
        if (StringUtils.isEmpty(sql)) {
            return sql;
        }
        Matcher matcher = INSERT_PATTERN.matcher(sql);
        if (!matcher.find()) {
            return sql;
        }
        return matcher.replaceFirst("$1INSERT IGNORE ");
    }

}
