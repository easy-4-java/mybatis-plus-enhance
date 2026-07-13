package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.methods.SelectById;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 按主键查询单个原始持久化实体的 SQL 注入方法。
 *
 * <p>生成与 MyBatis-Plus {@code selectById} 等价的主键和逻辑删除条件，
 * 但使用独立方法名，使查询后解密拦截器可以通过 Mapper 注解保留密文。</p>
 */
public class SelectIgnoreDecryptById extends SelectById {

    /**
     * 使用 {@link EnhanceSqlMethod#SELECT_IGNORE_DECRYPT_BY_ID} 定义的方法名创建注入器。
     */
    public SelectIgnoreDecryptById() {
        this(EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_BY_ID.getMethod());
    }

    /**
     * 使用自定义 Mapper 方法名创建注入器。
     *
     * @param name 方法名
     */
    public SelectIgnoreDecryptById(String name) {
        super(name);
    }

    /**
     * 按实体表元数据生成单主键查询 MappedStatement。
     *
     * @param mapperClass Mapper 接口类型
     * @param modelClass  实体类型
     * @param tableInfo   实体表元数据
     * @return 已注册的原始数据单条查询 MappedStatement
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.SELECT_IGNORE_DECRYPT_BY_ID;
        SqlSource sqlSource = super.createSqlSource(configuration, String.format(sqlMethod.getSql(),
                sqlSelectColumns(tableInfo, false),
                tableInfo.getTableName(), tableInfo.getKeyColumn(), tableInfo.getKeyProperty(),
                tableInfo.getLogicDeleteSql(true, true)), Object.class);
        return this.addSelectMappedStatementForTable(mapperClass, methodName, sqlSource, tableInfo);
    }

}
