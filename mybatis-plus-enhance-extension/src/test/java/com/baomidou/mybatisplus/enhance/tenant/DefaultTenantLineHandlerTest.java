package com.baomidou.mybatisplus.enhance.tenant;

import com.baomidou.mybatisplus.enhance.context.TenantContext;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class DefaultTenantLineHandlerTest {

    private final TenantContext context = new TenantContext();

    @After
    public void tearDown() {
        context.clear();
    }

    @Test
    public void shouldCreateNumericTenantExpression() {
        context.setCurrentTenantId(1001L);
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(context);

        Assert.assertEquals(new LongValue(1001L), handler.getTenantId());
    }

    @Test
    public void shouldCreateStringTenantExpression() {
        context.setCurrentTenantId("tenant-a");
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(context);

        Assert.assertEquals(new StringValue("tenant-a"), handler.getTenantId());
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRejectMissingTenant() {
        new DefaultTenantLineHandler(context).getTenantId();
    }

    @Test
    public void shouldSupportTenantColumnAndIgnoredTablePolicy() {
        DefaultTenantLineHandler handler = new DefaultTenantLineHandler(
                context,
                "organization_id",
                tableName -> tableName.startsWith("sys_"));

        Assert.assertEquals("organization_id", handler.getTenantIdColumn());
        Assert.assertTrue(handler.ignoreTable("sys_tenant"));
        Assert.assertFalse(handler.ignoreTable("biz_order"));
    }
}
