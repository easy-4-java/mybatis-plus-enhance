package com.baomidou.mybatisplus.enhance.interceptor;

import com.baomidou.mybatisplus.enhance.context.InsertIgnoreContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MySQL {@code INSERT IGNORE} SQL 改写拦截器。
 *
 * <p>只有在 {@link InsertIgnoreContext} 作用域开启且当前语句为 INSERT 时才进行改写。
 * 已包含 {@code IGNORE} 的语句保持不变；其他数据库方言不应注册该拦截器。</p>
 */
public class InsertIgnoreInnerInterceptor implements EnhanceInnerInterceptor {

    private static final Pattern INSERT_PATTERN = Pattern.compile(
            "^(\\s*)INSERT\\s+(?!IGNORE\\b)", Pattern.CASE_INSENSITIVE);

    /**
     * 将普通 INSERT 语句改写为 MySQL {@code INSERT IGNORE}。
     *
     * @param sql 原始 SQL
     * @return 改写后的 SQL；空值、非 INSERT 或已含 IGNORE 时原样返回
     */
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

    /**
     * 在 StatementHandler 准备 SQL 前按需插入 {@code IGNORE} 关键字。
     *
     * @param statementHandler   MyBatis StatementHandler
     * @param connection         当前数据库连接
     * @param transactionTimeout 事务超时时间，单位由 MyBatis 决定
     */
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

}
