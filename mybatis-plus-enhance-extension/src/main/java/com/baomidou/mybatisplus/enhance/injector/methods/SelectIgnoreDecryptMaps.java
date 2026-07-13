package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectMaps;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

import java.util.Map;

/**
 * 根据 Wrapper 查询 Map 形式原始持久化数据的 SQL 注入方法。
 *
 * <p>查询列和动态条件与 MyBatis-Plus {@code selectMaps} 保持一致，
 * 返回类型注册为 {@link Map}，并通过独立 Mapper 方法保留数据库中的原始值。</p>
 */
public class SelectIgnoreDecryptMaps extends SelectMaps {

    /**
     * 使用 {@link EnhanceSqlMethod#SELECT_IGNORE_DECRYPT_MAPS} 定义的方法名创建注入器。
     */
    public SelectIgnoreDecryptMaps() {
        this(EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_MAPS.getMethod());
    }

    /**
     * 使用自定义 Mapper 方法名创建注入器。
     *
     * @param name 方法名
     */
    public SelectIgnoreDecryptMaps(String name) {
        super(name);
    }

    /**
     * 按实体表元数据生成 Wrapper Map 查询 MappedStatement。
     *
     * @param mapperClass Mapper 接口类型
     * @param modelClass  实体类型
     * @param tableInfo   实体表元数据
     * @return 已注册的原始 Map 列表查询 MappedStatement
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_MAPS;
        String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectColumns(tableInfo, true), tableInfo.getTableName(),
                sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
        SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForOther(mapperClass, methodName, sqlSource, Map.class);
    }
}
