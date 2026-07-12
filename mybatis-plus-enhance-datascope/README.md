# mybatis-plus-enhance-datascope

基于 MyBatis-Plus 官方 `DataPermissionInterceptor` 和 `MultiDataPermissionHandler`
提供注解式数据权限。

模块不提供默认放行策略；应用必须实现 `DataScopeExpressionProvider`。
`DataScopeExpressions` 可用于构造安全的 JSqlParser 条件。
