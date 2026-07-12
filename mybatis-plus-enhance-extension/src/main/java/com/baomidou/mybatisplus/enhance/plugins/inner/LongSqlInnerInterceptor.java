package com.baomidou.mybatisplus.enhance.plugins.inner;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import java.sql.Connection;
import java.util.Objects;

/**
 * MyBatis-Plus 超长 SQL 检测拦截器。
 *
 * <p>本拦截器按 SQL 字符长度进行保护，不执行数据库 {@code EXPLAIN}，
 * 也不负责基于真实执行耗时判断慢 SQL。
 */
@Slf4j
public class LongSqlInnerInterceptor implements EnhanceInnerInterceptor {

    @Getter
    @Setter
    private int longSqlThreshold = 2000;

    @Getter
    @Setter
    private LongSqlHandler longSqlHandler;

    /**
     * 使用默认阈值 2000 个字符创建拦截器。
     */
    public LongSqlInnerInterceptor() {
    }

    /**
     * 使用指定字符阈值创建拦截器。
     *
     * @param longSqlThreshold SQL 字符数阈值；小于等于零时关闭检测
     */
    public LongSqlInnerInterceptor(int longSqlThreshold) {
        this.longSqlThreshold = longSqlThreshold;
    }

    /**
     * 使用指定阈值和回调创建拦截器。
     *
     * @param longSqlThreshold SQL 字符数阈值
     * @param longSqlHandler   超长 SQL 回调；可为 {@code null}
     */
    public LongSqlInnerInterceptor(int longSqlThreshold, LongSqlHandler longSqlHandler) {
        this.longSqlThreshold = longSqlThreshold;
        this.longSqlHandler = longSqlHandler;
    }

    /**
     * 在 SQL 预编译前检查字符长度并触发日志及业务回调。
     *
     * @param statementHandler   MyBatis StatementHandler
     * @param connection         当前数据库连接
     * @param transactionTimeout 事务超时时间
     */
    @Override
    public void beforePrepare(StatementHandler statementHandler, Connection connection,
                              Integer transactionTimeout) {
        if (longSqlThreshold <= 0) {
            return;
        }
        MetaObject metaObject = SystemMetaObject.forObject(statementHandler);
        MappedStatement mappedStatement =
                (MappedStatement) metaObject.getValue("delegate.mappedStatement");
        if (Objects.isNull(mappedStatement)) {
            return;
        }

        BoundSql boundSql = statementHandler.getBoundSql();
        String sql = Objects.nonNull(boundSql) ? boundSql.getSql() : null;
        if (Objects.nonNull(sql) && sql.length() > longSqlThreshold) {
            log.warn("Long SQL detected [length={}, mapper={}]: {}", sql.length(),
                    mappedStatement.getId(), sql.substring(0, Math.min(200, sql.length())) + "...");
            if (Objects.nonNull(longSqlHandler)) {
                longSqlHandler.onLongSql(mappedStatement.getId(), sql);
            }
        }
    }

    /**
     * 超长 SQL 回调。
     *
     * <p>适合接入告警、审计或指标系统。回调运行在 SQL 准备线程中，实现应快速完成。</p>
     */
    @FunctionalInterface
    public interface LongSqlHandler {

        /**
         * 处理检测到的超长 SQL。
         *
         * @param mappedStatementId Mapper 方法的全限定标识
         * @param sql               完整 SQL 文本
         */
        void onLongSql(String mappedStatementId, String sql);
    }
}
