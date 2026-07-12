package com.baomidou.mybatisplus.enhance.i18n.interceptor;

import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link DataI18nInnerInterceptor} 查询后生命周期测试。
 */
public class DataI18nInnerInterceptorTest {

    @Test
    public void shouldInvokeHandlerWhenLocaleAndResultsExist() throws Exception {
        AtomicBoolean invoked = new AtomicBoolean();
        DataI18nInnerInterceptor interceptor = new DataI18nInnerInterceptor(
                () -> Locale.CHINA,
                (locale, mappedStatement, results) -> invoked.set(true));
        List<Object> results = new ArrayList<>();
        results.add(new Object());

        interceptor.afterQuery(null, mappedStatement(), null, null, null, null, results);

        Assert.assertTrue(invoked.get());
    }

    private MappedStatement mappedStatement() {
        Configuration configuration = new Configuration();
        return new MappedStatement.Builder(configuration, "Mapper.select",
                new StaticSqlSource(configuration, "SELECT 1"), SqlCommandType.SELECT).build();
    }
}
