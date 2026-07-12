package com.baomidou.mybatisplus.enhance.plugins.inner;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.RowBounds;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;

public class LongSqlInnerInterceptorTest {

    @Test
    public void shouldNotifyHandlerWhenSqlExceedsThreshold() {
        String sql = "SELECT * FROM orders WHERE status = 'PAID'";
        Configuration configuration = new Configuration();
        MappedStatement mappedStatement = new MappedStatement.Builder(configuration,
                "OrderMapper.select", new StaticSqlSource(configuration, sql), SqlCommandType.SELECT).build();
        StatementHandler statementHandler = configuration.newStatementHandler(
                null, mappedStatement, null, RowBounds.DEFAULT, null, null);

        AtomicReference<String> capturedSql = new AtomicReference<>();
        LongSqlInnerInterceptor interceptor = new LongSqlInnerInterceptor(10,
                (mappedStatementId, actualSql) -> capturedSql.set(actualSql));

        interceptor.beforePrepare(statementHandler, null, null);

        assertEquals(sql, capturedSql.get());
    }
}
