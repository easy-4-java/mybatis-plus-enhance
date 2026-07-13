package com.baomidou.mybatisplus.enhance.i18n.handler;

import org.apache.ibatis.mapping.MappedStatement;

import java.util.List;
import java.util.Locale;

/**
 * MyBatis 查询结果国际化处理器。
 *
 * <p>拦截器负责在查询完成后提供当前 Locale、MappedStatement 和结果列表，
 * 具体字段选择、外部数据合并或翻译表查询策略由实现决定。</p>
 *
 * @since 2.0.0
 */
@FunctionalInterface
public interface DataI18nHandler {

    /**
     * 就地处理查询结果中需要国际化的字段。
     *
     * @param locale          当前语言区域
     * @param mappedStatement MyBatis 映射语句元数据
     * @param results         已完成结果映射的对象列表
     */
    void handle(Locale locale, MappedStatement mappedStatement, List<Object> results);
}
