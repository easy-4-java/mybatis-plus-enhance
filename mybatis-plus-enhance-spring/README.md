# mybatis-plus-enhance-spring

隔离 MyBatis-Plus Enhance 的 Spring 依赖，当前提供带表签名、验签和事务语义的
`IEnhanceService` 与 `EnhanceServiceImpl`。

本模块依赖 `mybatis-plus-enhance-extension`，因此会同时获得加密、签名、租户、数据权限、
国际化、SQL 和观测增强。不使用 Spring Service 的项目应直接引入 Extension，无需引入本模块。
