package org.apache.mybatis;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class SqlSetTest {

    @Test
    public void testSqlSet() {
        String sqlSet = "name='1', age=2";
        String[] sqlSetArr = StringUtils.split(sqlSet, Constants.COMMA);
        Map<String, String> propMap = Arrays.stream(sqlSetArr).map(el -> el.split(Constants.EQUALS)).collect(Collectors.toMap(el -> el[0], el -> el[1]));
        Assert.assertEquals("'1'", propMap.get("name"));
        Assert.assertEquals("2", propMap.get(" age"));
    }

}
