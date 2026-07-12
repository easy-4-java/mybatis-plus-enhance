package com.baomidou.mybatisplus.enhance.i18n.handler;

import org.apache.mybatis.enhance.annotation.i18n.I18nColumn;
import org.apache.mybatis.enhance.annotation.i18n.I18nLocale;
import org.apache.mybatis.enhance.annotation.i18n.I18nMapper;
import org.apache.mybatis.enhance.annotation.i18n.LocaleEnum;
import com.baomidou.mybatisplus.enhance.i18n.handler.def.DefaultDataI18nHandler;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

/**
 * {@link DefaultDataI18nHandler} 实体与 Map 映射测试。
 */
public class DefaultDataI18nHandlerTest {

    private final DefaultDataI18nHandler handler = new DefaultDataI18nHandler();

    @Test
    public void shouldMapAnnotatedEntityField() {
        ProductRow row = new ProductRow("default", "中文名称", "English name");
        List<Object> results = new ArrayList<>();
        results.add(row);

        handler.handle(Locale.US, mappedStatement(ProductMapper.class.getName() + ".selectEntity"), results);

        Assert.assertEquals("English name", row.name);
    }

    @Test
    public void shouldMapMethodDefinitionForMapResult() {
        Map<String, Object> row = new HashMap<>();
        row.put("name", "default");
        row.put("nameZh", "中文名称");
        row.put("nameEn", "English name");
        List<Object> results = new ArrayList<>();
        results.add(row);

        handler.handle(Locale.CHINA, mappedStatement(ProductMapper.class.getName() + ".selectMap"), results);

        Assert.assertEquals("中文名称", row.get("name"));
    }

    private MappedStatement mappedStatement(String id) {
        Configuration configuration = new Configuration();
        return new MappedStatement.Builder(configuration, id,
                new StaticSqlSource(configuration, "SELECT 1"), SqlCommandType.SELECT).build();
    }

    private interface ProductMapper {

        void selectEntity();

        @I18nMapper({
                @I18nColumn(column = "name", i18n = {
                        @I18nLocale(locale = LocaleEnum.zh_CN, column = "nameZh"),
                        @I18nLocale(locale = LocaleEnum.en_US, column = "nameEn")
                })
        })
        void selectMap();
    }

    private static class ProductRow {

        private final String nameZh;
        private final String nameEn;
        @I18nColumn(column = "name", i18n = {
                @I18nLocale(locale = LocaleEnum.zh_CN, column = "nameZh"),
                @I18nLocale(locale = LocaleEnum.en_US, column = "nameEn")
        })
        private String name;

        private ProductRow(String name, String nameZh, String nameEn) {
            this.name = name;
            this.nameZh = nameZh;
            this.nameEn = nameEn;
        }
    }
}
