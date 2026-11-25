# mybatis-plus-enhance-extension

集中提供 MyBatis-Plus Enhance 的可插拔增强实现：

- 字段透明加解密、HMAC、表签名和密文 Mapper；
- 官方 `TenantLineHandler` 的租户上下文适配；
- 官方数据权限拦截器的注解解析与表达式工具；
- Locale 上下文和查询结果国际化；
- `INSERT IGNORE` 作用域与超长 SQL 检测；
- SQL 执行耗时、异常观测与 `SqlObservationSink` SPI。

该模块依赖 `mybatis-plus-enhance-core`，但不依赖 Spring。需要增强 Service、事务或依赖注入时，
再单独引入 `mybatis-plus-enhance-spring`。

`TenantContext.open(tenantId)` 返回的作用域只能在创建线程关闭。同线程重复关闭保持幂等；
其他线程关闭（包括已关闭的作用域）会抛出 `IllegalStateException`，不会改变任一线程的租户或作用域状态。
异步任务应在任务线程内创建并关闭自己的作用域；上下文透传仍需 TTL 包装的执行器。
