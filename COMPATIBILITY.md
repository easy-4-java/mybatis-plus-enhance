# 版本兼容性策略

`mybatis-plus-enhance` 以主版本线区分 Java 基线。三条版本线分别维护源码、依赖和构建配置，
不通过 Maven Profile 在同一份源码中动态切换 Java 版本。

| 项目版本线 | Java 基线 | Maven 编译基线 | Spring 技术栈 | API 命名空间 | MyBatis-Plus JSqlParser |
|---|---:|---|---|---|---|
| `1.0.x` | JDK 8 | `source/target = 1.8` | Spring Framework 5.3.x | `javax.*` | `mybatis-plus-jsqlparser-4.9` |
| `2.0.x` | JDK 17 | `release = 17` | Spring Framework 6.2.x | `jakarta.*` | `mybatis-plus-jsqlparser` |
| `3.0.x` | JDK 21 | `release = 21` | 选择明确支持 JDK 21 的稳定版本 | `jakarta.*` | `mybatis-plus-jsqlparser` |

## 版本线约束

每条版本线必须同时满足以下条件，不能只修改 `maven.compiler.source`：

1. 主代码和测试代码只能使用该 Java 基线支持的语法与标准库 API；
2. 直接依赖和传递依赖的最低运行时版本不得高于该 Java 基线；
3. Maven 插件必须能在该版本线的基线 JDK 上运行；
4. 构建产物的 Class 文件版本必须与基线一致；
5. Spring、Servlet、Annotation 等生态依赖必须使用一致的 `javax` 或 `jakarta` 命名空间；
6. CI 必须至少在基线 JDK 上执行 `clean verify`，可额外在更高 JDK 上验证向前兼容性。
7. 每条版本线必须执行真实 H2 组合测试，覆盖缓存、加密、签名、补签和增强 SQL Injector。

## 当前 1.0.x 基线

当前分支属于 `1.0.x`，关键依赖固定为：

| 组件 | 版本或系列 | 选择原因 |
|---|---|---|
| Java | 8 | `1.0.x` 的最低编译与运行环境 |
| Spring Framework | 5.3.39 | Spring 5.3 是支持 JDK 8 的最后一个主版本系列 |
| MyBatis | 3.5.19 | 保持 Java 8 兼容的稳定版本 |
| MyBatis-Plus | 3.5.14 | 当前增强实现的验证基线 |
| JSqlParser | 4.9 | 与 MyBatis-Plus 的 JDK 8 专用适配包配套 |
| Annotation API | `javax.annotation-api` 1.3.2 | 与 JDK 8、Spring 5.3 的 `javax` 技术栈一致 |

`1.0.x` 不得引入 Spring 6、Jakarta Annotation 2、Jakarta Servlet 6、JSqlParser 5，
也不得使用 `record`、文本块、模式匹配、`var` 等 JDK 8 不支持的语法。

## 升级原则

- `1.0.x` 只接受兼容性修复和能够继续运行在 JDK 8 上的依赖升级；
- 需要 JDK 17、Spring 6 或 `jakarta.*` API 的变更进入 `2.0.x`；
- 使用 JDK 21 语言特性、标准库 API 或以 JDK 21 为最低运行环境的依赖进入 `3.0.x`；
- 跨版本线升级允许破坏二进制兼容，但必须在发布说明中列出包名、配置和依赖迁移项；
- 同一功能应优先保持公共 API 语义一致，使用不同版本线内部实现适配 Java 和三方组件差异。

## 发布前验证

仓库的兼容矩阵会分别检出 `feature/1.0.x`、`feature/2.0.x` 和 `feature/3.0.x`，并在
JDK 8、17、21 基线上执行 `mvn clean verify`；不同版本线不通过 Maven Profile 共享 Java 语法或依赖配置。

在对应版本线的基线 JDK 下执行：

```bash
mvn clean verify
```

`1.0.x` 发布前还应检查产物 Class 文件主版本为 `52`，并确认依赖树中不存在 Spring 6、
`jakarta.annotation-api`、`jakarta.servlet-api` 或 JSqlParser 5。
