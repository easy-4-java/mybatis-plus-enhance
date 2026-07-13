package com.baomidou.mybatisplus.enhance.injector.methods;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.enums.EnhanceSqlMethod;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlSource;

/**
 * 按主键仅更新表签名存储列的 SQL 注入方法。
 *
 * <p>该方法用于历史数据补签和写后完整签名刷新。SQL 不包含加密业务字段，因此不会把
 * 查询得到的原始密文再次提交给字段加密拦截器。</p>
 */
public class UpdateSignatureById extends AbstractMethod {

    /**
     * 使用标准增强方法名创建注入器。
     */
    public UpdateSignatureById() {
        super(EnhanceSqlMethod.UPDATE_SIGNATURE_BY_ID.getMethod());
    }

    /**
     * 为声明签名存储字段的实体注入签名列更新语句。
     *
     * @param mapperClass Mapper 接口类型
     * @param modelClass  实体类型
     * @param tableInfo   实体表元数据
     * @return 已注册的更新语句
     */
    @Override
    public MappedStatement injectMappedStatement(Class<?> mapperClass, Class<?> modelClass, TableInfo tableInfo) {
        TableFieldInfo signatureField = TableFieldHelper.getTableSignatureStoreFieldInfo(tableInfo)
                .orElseThrow(() -> new IllegalStateException(
                        "No stored table signature field found on " + modelClass.getName()));
        EnhanceSqlMethod sqlMethod = EnhanceSqlMethod.UPDATE_SIGNATURE_BY_ID;
        String sql = String.format(sqlMethod.getSql(),
                tableInfo.getTableName(),
                signatureField.getColumn(), signatureField.getProperty(),
                tableInfo.getKeyColumn(), tableInfo.getKeyProperty(),
                tableInfo.getLogicDeleteSql(true, true));
        SqlSource sqlSource = createSqlSource(configuration, sql, modelClass);
        return addUpdateMappedStatement(mapperClass, modelClass, methodName, sqlSource);
    }
}
