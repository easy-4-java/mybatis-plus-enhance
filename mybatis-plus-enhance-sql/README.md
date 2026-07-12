# mybatis-plus-enhance-sql

提供 MySQL `INSERT IGNORE` 作用域与超长 SQL 检测。

`InsertIgnoreInnerInterceptor` 仅应在 MySQL 上注册；`LongSqlInnerInterceptor`
只检测 SQL 字符长度，不执行 EXPLAIN，也不代表慢 SQL 检测。
