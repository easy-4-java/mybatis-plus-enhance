package com.baomidou.mybatisplus.enhance.injector;

import com.baomidou.mybatisplus.core.injector.AbstractMethod;
import com.baomidou.mybatisplus.core.injector.DefaultSqlInjector;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.injector.methods.*;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import org.apache.ibatis.session.Configuration;

import java.util.List;

/**
 * 增强 SQL 方法注入器。
 *
 * <p>保留 MyBatis-Plus 默认方法，并为 {@code EnhanceMapper} 注入跳过结果解密的查询语句。</p>
 */
public class EnhanceSqlInjector extends DefaultSqlInjector {

    /**
     * 构造指定 Mapper 的完整注入方法列表。
     *
     * @param configuration MyBatis 配置
     * @param mapperClass   Mapper 类型
     * @param tableInfo     实体表元数据
     * @return 默认方法与增强查询方法列表
     */
    @Override
    public List<AbstractMethod> getMethodList(Configuration configuration, Class<?> mapperClass, TableInfo tableInfo) {
        List<AbstractMethod> methodList = super.getMethodList(configuration, mapperClass, tableInfo);
        methodList.add(new SelectIgnoreDecryptMaps());
        methodList.add(new SelectIgnoreDecryptObjs());
        methodList.add(new SelectIgnoreDecryptList());
        if (tableInfo.havePK()) {
            // 根据ID 查询一条数据, 不解密
            methodList.add(new SelectIgnoreDecryptById());
            // 根据ID集合，批量查询数据，不解密
            methodList.add(new SelectIgnoreDecryptBatchIds());
            if (TableFieldHelper.getTableSignatureStoreFieldInfo(tableInfo).isPresent()) {
                methodList.add(new UpdateSignatureById());
            }
        } else {
            logger.warn(String.format("%s ,Not found @TableId annotation, Cannot use Mybatis-Plus 'xxById' Method.",
                    tableInfo.getEntityType()));
        }
        return methodList;
    }

}
