package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectList;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 根据 Wrapper 查询原始持久化实体列表的 SQL 注入方法。
 *
 * <p>保留 MyBatis-Plus 的动态列、逻辑删除、排序、SQL first 与 comment 能力，
 * 使用独立 Mapper 方法名表达“跳过查询后解密”的语义。</p>
 */
public class SelectIgnoreDecryptList extends SelectList {

    /**
     * 使用 {@link EnhanceSqlMethod#SELECT_IGNORE_DECRYPT_LIST} 定义的方法名创建注入器。
     */
    public SelectIgnoreDecryptList() {
        this(EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_LIST.getMethod());
    }

    /**
     * 使用自定义 Mapper 方法名创建注入器。
     *
     * @param name 方法名
     */
    public SelectIgnoreDecryptList(String name) {
        super(name);
    }

    /**
     * 按实体表元数据生成 Wrapper 列表查询 MappedStatement。
     *
     * @param mapperClass Mapper 接口类型
     * @param modelClass  实体类型
     * @param tableInfo   实体表元数据
     * @return 已注册的原始实体列表查询 MappedStatement
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_LIST;
        String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectColumns(tableInfo, true), tableInfo.getTableName(),
                sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
        SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForTable(mapperClass, methodName, sqlSource, tableInfo);
    }
}
