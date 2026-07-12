package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectObjs;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 查询满足条件所有数据
 */
public class SelectIgnoreDecryptObjs extends SelectObjs {

    public SelectIgnoreDecryptObjs() {
        this(EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_OBJS.getMethod());
    }

    /**
     * @param name 方法名
     * @since 3.5.0
     */
    public SelectIgnoreDecryptObjs(String name) {
        super(name);
    }

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_OBJS;
        String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectObjsColumns(tableInfo),
                tableInfo.getTableName(), sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
        SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForOther(mapperClass, methodName, sqlSource, Object.class);
    }
}
