package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectByIds;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.sql.SqlScriptUtils;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 按主键集合查询原始持久化实体的 SQL 注入方法。
 *
 * <p>生成逻辑删除条件并复用 MyBatis-Plus 的批量主键参数协议；返回结果配合
 * {@code @IgnoreEncrypted} Mapper 契约跳过查询后解密。</p>
 */
public class SelectIgnoreDecryptBatchIds extends SelectByIds {

    /**
     * 使用增强批量原始数据查询方法名创建注入器。
     */
    public SelectIgnoreDecryptBatchIds() {
        super(EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_BATCH_BY_IDS.getMethod());
    }

    /**
     * 按实体表元数据生成批量主键查询 MappedStatement。
     *
     * @param mapperClass Mapper 接口类型
     * @param modelClass  实体类型
     * @param tableInfo   实体表元数据
     * @return 已注册的原始数据批量查询 MappedStatement
     */
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
