package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectByIds;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 根据ID集合，批量查询数据，不解密
 */
public class SelectIgnoreDecryptBatchIds extends SelectByIds {

    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_BATCH_BY_IDS;
        SqlSource sqlSource = super.createSqlSource(configuration, String.format(sqlMethod.getSql(),
                sqlSelectColumns(tableInfo, false), tableInfo.getTableName(), tableInfo.getKeyColumn(),
                SqlScriptUtils.convertForeach("#{item}", COLL, null, "item", COMMA),
                tableInfo.getLogicDeleteSql(true, true)), Object.class);
        return addSelectMappedStatementForTable(mapperClass, methodName, sqlSource, tableInfo);
    }

}
