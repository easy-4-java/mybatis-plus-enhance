package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectObjs;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 根据 Wrapper 查询首列原始持久化值的 SQL 注入方法。
 *
 * <p>列选择规则与 MyBatis-Plus {@code selectObjs} 保持一致，返回每行首列值，
 * 适用于签名运维或需要直接读取数据库原始列值的场景。</p>
 */
public class SelectIgnoreDecryptObjs extends SelectObjs {

    /**
     * 使用 {@link EnhanceSqlMethod#SELECT_IGNORE_DECRYPT_OBJS} 定义的方法名创建注入器。
     */
    public SelectIgnoreDecryptObjs() {
        this(EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_OBJS.getMethod());
    }

    /**
     * 使用自定义 Mapper 方法名创建注入器。
     *
     * @param name 方法名
     */
    public SelectIgnoreDecryptObjs(String name) {
        super(name);
    }

    /**
     * 按实体表元数据生成 Wrapper 首列值查询 MappedStatement。
     *
     * @param mapperClass Mapper 接口类型
     * @param modelClass  实体类型
     * @param tableInfo   实体表元数据
     * @return 已注册的原始首列值查询 MappedStatement
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_OBJS;
        String sql = String.format(sqlMethod.getSql(), sqlFirst(), sqlSelectObjsColumns(tableInfo),
                tableInfo.getTableName(), sqlWhereEntityWrapper(true, tableInfo), sqlOrderBy(tableInfo), sqlComment());
        SqlSource sqlSource = super.createSqlSource(configuration, sql, modelClass);
        return this.addSelectMappedStatementForOther(mapperClass, methodName, sqlSource, Object.class);
    }
}
