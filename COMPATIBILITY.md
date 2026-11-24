# 版本兼容性策略

`mybatis-plus-enhance` 以主版本线区分 Java 基线。三条版本线分别维护源码、依赖和构建配置，
不通过 Maven Profile 在同一份源码中动态切换 Java 版本。

| 项目版本线   | Java 基线 | Maven 编译基线            | Spring 技术栈             | API 命名空间    | MyBatis-Plus JSqlParser       |
|---------|--------:|-----------------------|------------------------|-------------|-------------------------------|
| `1.0.x` |   JDK 8 | `source/target = 1.8` | Spring Framework 5.3.x | `javax.*`   | `mybatis-plus-jsqlparser-4.9` |
| `2.0.x` |  JDK 17 | `release = 17`        | Spring Framework 6.2.x | `jakarta.*` | `mybatis-plus-jsqlparser`     |
| `3.0.x` |  JDK 21 | `release = 21`        | 选择明确支持 JDK 21 的稳定版本    | `jakarta.*` | `mybatis-plus-jsqlparser`     |

## 版本线约束

每条版本线必须同时满足以下条件，不能只修改 `maven.compiler.source`：

1. 主代码和测试代码只能使用该 Java 基线支持的语法与标准库 API；
2. 直接依赖和传递依赖的最低运行时版本不得高于该 Java 基线；
3. Maven 插件必须能在该版本线的基线 JDK 上运行；
4. 构建产物的 Class 文件版本必须与基线一致；
5. Spring、Servlet、Annotation 等生态依赖必须使用一致的 `javax` 或 `jakarta` 命名空间；
6. CI 必须至少在基线 JDK 上执行 `clean verify`，可额外在更高 JDK 上验证向前兼容性。
7. 每条版本线必须执行真实 H2 组合测试，覆盖缓存、加密、签名、补签和增强 SQL Injector。

## 当前 2.0.x 基线

当前分支属于 `2.0.x`，关键依赖固定为：

| 组件               | 版本或系列                          | 选择原因                               |
|------------------|--------------------------------|------------------------------------|
| Java             | 17                             | `2.0.x` 的最低编译与运行环境                 |
| Spring Framework | 6.2.11                         | 与 JDK 17 和 Jakarta API 技术栈配套       |
| MyBatis          | 3.5.19                         | 当前增强实现的稳定基础版本                      |
| MyBatis-Plus     | 3.5.18.1                         | 当前增强实现的验证基线                        |
| JSqlParser       | 5.3                            | 与 MyBatis-Plus 标准 JSqlParser 适配包配套 |
| Annotation API   | `jakarta.annotation-api` 2.1.1 | 与 Spring 6 的 Jakarta 技术栈一致         |

`2.0.x` 不得降级到 Spring 5、`javax.annotation` 或 JSqlParser 4.9，也不得使用
JDK 17 不支持的 Java 21 专属语法和标准库 API。

## 升级原则

- `1.0.x` 只接受兼容性修复和能够继续运行在 JDK 8 上的依赖升级；
- 需要 JDK 17、Spring 6 或 `jakarta.*` API 的变更进入 `2.0.x`；
- 使用 JDK 21 语言特性、标准库 API 或以 JDK 21 为最低运行环境的依赖进入 `3.0.x`；
- 跨版本线升级允许破坏二进制兼容，但必须在发布说明中列出包名、配置和依赖迁移项；
- 同一功能应优先保持公共 API 语义一致，使用不同版本线内部实现适配 Java 和三方组件差异。

## 对象 HMAC 的跨版本兼容契约

`DefaultEncryptedFieldHandler` 的 `MPEH1` 签名生成统一关闭 Jackson 的
`SORT_PROPERTIES_ALPHABETICALLY`，延续 1.0.x/2.0.x 默认 POJO 属性发现顺序。
在实体类型、密钥及其余序列化配置一致时，三条版本线生成相同的对象签名。
显式注解、命名策略、Map 顺序和自定义序列化器仍由调用方配置决定，不承诺不同配置之间签名相同。

验签同时接受关闭和开启字母排序时生成的签名，包括旧 Jackson 3 默认排序产生的历史签名，
并继续按签名中的 keyId 查找历史密钥。该兼容范围也覆盖嵌套 POJO；篡改后的对象必须验签失败。
签名处理使用调用方 ObjectMapper 配置的独立副本，不修改调用方实例，也不改变字段加解密序列化。
字符串签名（内置整行签名使用的输入）、`MPE1` 密文格式和 `MPEH1` 签名格式保持不变。

升级需先让读端具备兼容验签能力；未升级的 Jackson 3 读端仍可能拒绝新生成的非字母排序对象签名。
`HmacBranchCompatibilityTest` 使用修复前真实生成的两组固定签名验证生成、历史验签、嵌套对象、
篡改拒绝、密钥轮换、调用方配置隔离以及新旧构造器的一致行为。

## Spring Service 依赖适配

本版本线的 MyBatis-Plus 版本与 DDD4J 对应版本线一致。增强 Service 使用
`com.baomidou.mybatisplus.spring.service.IService` 和
`com.baomidou.mybatisplus.spring.service.impl.ServiceImpl`，由既有 `mybatis-plus-spring` 依赖提供。
增强组件自身的 Service 包名、签名查询和事务方法契约保持不变。
批量插入、更新及保存或更新的回调返回实际写入结果，兼容新版 BiFunction 回调，
并保留写入后收集主键、批处理成功后补签的顺序。

升级时应统一应用中的 MyBatis-Plus 组件版本，并重新编译 Service 消费者；本版本不再支持
以 MyBatis-Plus 3.5.14 替换上述依赖。core/extension 继续保持 Spring 无关。
验证必须包括完整 reactor 的 `clean verify`、`SignedServiceContractTest` 和独立 `alignment-test`。

## 发布前验证

在对应版本线的基线 JDK 下执行：

```bash
mvn clean verify
```

`2.0.x` 发布前还应检查产物 Class 文件主版本为 `61`，并确认依赖树中不存在 Spring 5、
`javax.annotation-api`、`javax.servlet-api` 或 JSqlParser 4.9。

## 自动兼容矩阵

仓库通过 `.github/workflows/compatibility-matrix.yml` 对三条维护分支执行基线验证：

| Git 分支 | 基线 JDK | 必须执行 |
|---|---:|---|
| `feature/1.0.x` | 8 | `mvn clean verify` |
| `feature/2.0.x` | 17 | `mvn clean verify` |
| `feature/3.0.x` | 21 | `mvn clean verify` |

矩阵直接检出对应版本线，避免在同一源码树中用 Profile 假装兼容不同 Java 语法和依赖生态。
