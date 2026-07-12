package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.enhance.annotation.datascope.DataScopePlus;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

/**
 * 数据权限 SQL 表达式提供者。
 *
 * <p>实现根据当前用户、组织、角色或其他权限上下文生成附加 WHERE 条件。</p>
 */
public interface DataScopeExpressionProvider {

    /**
     * 获取数据权限 SQL 片段
     *
     * @param table               所执行的数据库表信息，可以通过此参数获取表名和表别名
     * @param where               原有的 where 条件信息
     * @param mappedStatementId   Mybatis MappedStatement Id 根据该参数可以判断具体执行方法
     * @param dataScopeAnnotation 数据权限注解
     * @return JSqlParser 条件表达式，返回的条件表达式会拼接在原有的表达式后面（不会覆盖原有的表达式）
     */
    Expression getDataScopeSqlSegment(Table table, Expression where, String mappedStatementId, DataScopePlus dataScopeAnnotation);

}
