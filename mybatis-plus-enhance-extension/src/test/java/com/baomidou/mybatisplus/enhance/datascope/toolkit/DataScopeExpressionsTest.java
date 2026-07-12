package com.baomidou.mybatisplus.enhance.datascope.toolkit;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

/**
 * {@link DataScopeExpressions} 常用表达式构造测试。
 */
public class DataScopeExpressionsTest {

    @Test
    public void shouldBuildQualifiedExpressions() {
        Assert.assertEquals("o.creator_id = 7",
                DataScopeExpressions.eq("o", "creator_id", 7L).toString());
        Assert.assertEquals("o.dept_id IN (1, 2)",
                DataScopeExpressions.in("o", "dept_id", Arrays.asList(1L, 2L)).toString());
    }
}
