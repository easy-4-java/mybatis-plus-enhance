package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.enhance.annotation.datascope.DataScopePlus;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.schema.Table;
import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicReference;

/**
 * {@link DataScopeAnnotationHandler} 注解优先级与分页 COUNT 映射测试。
 */
public class DataScopeAnnotationHandlerTest {

    @Test
    public void shouldPreferMethodAnnotation() {
        AtomicReference<DataScopePlus> captured = new AtomicReference<>();
        DataScopeAnnotationHandler handler = handler(captured);

        handler.getSqlSegment(new Table("orders"), null,
                ScopedMapper.class.getName() + ".methodScoped");

        Assert.assertEquals("method", captured.get().tableAlias());
    }

    @Test
    public void shouldFallBackToTypeAnnotation() {
        AtomicReference<DataScopePlus> captured = new AtomicReference<>();
        DataScopeAnnotationHandler handler = handler(captured);

        handler.getSqlSegment(new Table("orders"), null,
                ScopedMapper.class.getName() + ".typeScoped");

        Assert.assertEquals("type", captured.get().tableAlias());
    }

    @Test
    public void shouldAllowMethodToDisableTypeScope() {
        AtomicReference<DataScopePlus> captured = new AtomicReference<>();
        DataScopeAnnotationHandler handler = handler(captured);

        Expression expression = handler.getSqlSegment(new Table("orders"), null,
                ScopedMapper.class.getName() + ".disabled");

        Assert.assertNull(expression);
        Assert.assertNull(captured.get());
    }

    @Test
    public void shouldResolvePaginationCountStatement() {
        AtomicReference<DataScopePlus> captured = new AtomicReference<>();
        DataScopeAnnotationHandler handler = handler(captured);

        handler.getSqlSegment(new Table("orders"), null,
                ScopedMapper.class.getName() + ".methodScoped_COUNT");

        Assert.assertEquals("method", captured.get().tableAlias());
    }

    private DataScopeAnnotationHandler handler(AtomicReference<DataScopePlus> captured) {
        return new DataScopeAnnotationHandler((table, where, statementId, annotation) -> {
            captured.set(annotation);
            return new LongValue(1);
        });
    }

    @DataScopePlus(tableAlias = "type")
    private interface ScopedMapper {

        @DataScopePlus(tableAlias = "method")
        void methodScoped();

        void typeScoped();

        @DataScopePlus(enabled = false)
        void disabled();
    }
}
