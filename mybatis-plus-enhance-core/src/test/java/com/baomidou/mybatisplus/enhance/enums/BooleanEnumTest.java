package com.baomidou.mybatisplus.enhance.enums;

import org.junit.Assert;
import org.junit.Test;

public class BooleanEnumTest {

    @Test
    public void shouldExposeMybatisPlusPersistenceValues() {
        Assert.assertEquals(Integer.valueOf(0), BooleanEnum.IS_FALSE.getValue());
        Assert.assertEquals(Integer.valueOf(1), BooleanEnum.IS_TRUE.getValue());
    }

    @Test
    public void shouldResolveDatabaseValues() {
        Assert.assertSame(BooleanEnum.IS_FALSE, BooleanEnum.valueOf(0));
        Assert.assertSame(BooleanEnum.IS_TRUE, BooleanEnum.valueOf(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldRejectUnsupportedDatabaseValue() {
        BooleanEnum.valueOf(2);
    }

    @Test
    public void shouldRetainChineseDisplayName() {
        Assert.assertEquals("否", BooleanEnum.IS_FALSE.getNameCn());
        Assert.assertEquals("是", BooleanEnum.IS_TRUE.getNameCn());
    }
}
