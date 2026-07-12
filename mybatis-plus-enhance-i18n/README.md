# mybatis-plus-enhance-i18n

提供查询后同行多语言字段映射，包括 `I18nContext`、
`DataI18nInnerInterceptor`、`DataI18nHandler` 和 `DefaultDataI18nHandler`。

默认处理器不执行额外 SQL；外部翻译表或远程翻译源可通过自定义
`DataI18nHandler` 扩展。
