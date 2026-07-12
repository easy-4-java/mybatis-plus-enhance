# mybatis-plus-enhance

`mybatis-plus-enhance` 是面向 MyBatis-Plus 的基础增强组件，在不替代官方插件体系的前提下，补充以下能力：

- 字段透明加密、查询结果解密与 HMAC；
- 表级数据签名、验签与历史数据补签；
- 基于官方 `TenantLineInnerInterceptor` 的租户上下文和默认适配器；
- 基于官方 `DataPermissionInterceptor` 的注解解析与数据权限表达式扩展点；
- 实体字段国际化映射；
- MySQL `INSERT IGNORE` 作用域；
- 超长 SQL 检测、真实执行耗时观测和慢 SQL 日志；
- 带查询后、更新后和执行完成回调的统一增强拦截器链。

本项目的定位是“增强 MyBatis-Plus”，不会重新实现分页、乐观锁、多租户或数据权限等官方已有基础设施。租户和数据权限能力应与
MyBatis-Plus 官方拦截器组合使用。

## 运行要求

项目主版本线与 Java 基线严格对应：

| 项目版本线 | Java 基线 | Spring 技术栈 | API 命名空间 | JSqlParser 适配 |
|---|---:|---|---|---|
| `1.0.x` | JDK 8 | Spring Framework 5.3.x | `javax.*` | `mybatis-plus-jsqlparser-4.9` |
| `2.0.x` | JDK 17 | Spring Framework 6.2.x | `jakarta.*` | `mybatis-plus-jsqlparser` |
| `3.0.x` | JDK 21 | 支持 JDK 21 的稳定版本 | `jakarta.*` | `mybatis-plus-jsqlparser` |

当前分支是 `1.0.x`，源码与构建基线如下：

| 组件           | 版本     |
|--------------|--------|
| Java         | 8+     |
| Maven        | 3.9.6+ |
| MyBatis      | 3.5.19 |
| MyBatis-Plus | 3.5.14 |
| Hutool       | 5.8.40 |

完整的依赖选择、源码约束和发布验证规则见 [版本兼容性策略](COMPATIBILITY.md)。

Spring 集成已隔离到 `mybatis-plus-enhance-spring`。`core` 与 `extension` 保持 Spring 无关；
普通 MyBatis-Plus 项目只需按能力选择 `core` 或 `extension`。

公共加密和国际化注解统一复用 `mybatis-enhance-annotation`，MyBatis-Plus 项目不维护重复协议；
唯一的 Plus 专属数据权限注解随 Extension 提供，不为单个注解拆分独立 Maven 模块。

## 引入依赖

```xml
<dependencies>
    <dependency>
        <groupId>io.github.hiwepy</groupId>
        <artifactId>mybatis-plus-enhance-extension</artifactId>
        <version>1.0.x.20260630-SNAPSHOT</version>
    </dependency>
</dependencies>
```

`extension` 已包含 Core 以及加密、租户、数据权限、国际化、SQL 和观测增强。需要带签名语义的
Service、事务和依赖注入时，改为引入 `mybatis-plus-enhance-spring`，它会传递引入 Extension。

若使用快照版本，请在应用的 Maven 配置中启用对应快照仓库。生产环境建议锁定经过验证的发布版本，不要使用动态版本范围。

## 模块结构

| 模块                              | 职责 |
|---------------------------------|------|
| `mybatis-plus-enhance-core`      | 增强拦截器契约、查询后/更新后/执行完成生命周期和公共工具 |
| `mybatis-plus-enhance-extension` | 加密、签名、租户、数据权限、国际化、SQL 处理和执行观测 |
| `mybatis-plus-enhance-spring`    | 带签名语义的 Spring Service、依赖注入和事务集成 |

## 配置增强拦截器链

需要查询后解密、查询后验签或真实执行耗时观测时，应使用 `MybatisPlusEnhanceInterceptor` 作为唯一的外层 MyBatis-Plus
拦截器链入口。官方 `InnerInterceptor` 可以直接加入该链，无需适配。

```java
@Configuration
public class MybatisConfiguration {

    @Bean
    public MybatisPlusEnhanceInterceptor mybatisPlusInterceptor(
            EncryptedFieldHandler encryptedFieldHandler,
            DataSignatureHandler dataSignatureHandler,
            TenantLineHandler tenantLineHandler) {

        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();

        // 官方插件仍然直接使用。
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(tenantLineHandler));

        // 写入：先加密，后按最终密文生成表签名。
        interceptor.addInnerInterceptor(
                new DataEncryptionInnerInterceptor(encryptedFieldHandler, true));
        interceptor.addInnerInterceptor(
                new DataSignatureInnerInterceptor(dataSignatureHandler, true, true));

        // 读取：注册顺序决定 afterQuery 顺序，因此先验签，后解密。
        interceptor.addInnerInterceptor(
                new DataDecryptionInnerInterceptor(encryptedFieldHandler, true));

        SqlObservationInnerInterceptor observation =
                new SqlObservationInnerInterceptor(new SlowSqlLoggingSink(500));
        interceptor.addInnerInterceptor(observation);
        return interceptor;
    }
}
```

拦截器顺序是正确性约束，而不只是性能配置：

1. 租户、数据权限等 SQL 条件插件先完成条件注入；
2. 写入参数先加密，再基于最终入库值生成签名；
3. 查询结果先验签，再解密为业务值；
4. SQL 观测放在链尾，接收最终执行结果。

同一个 MyBatis `Configuration` 不要同时注册官方 `MybatisPlusInterceptor` 和 `MybatisPlusEnhanceInterceptor`
，否则内部插件可能重复执行。

## 字段透明加解密

### 声明实体

```java
@EncryptedTable
public class CustomerPO {

    private Long id;

    @EncryptedField
    private String mobile;

    @EncryptedField
    private String idCard;
}
```

`@EncryptedTable` 声明实体参与透明加解密，只有标记 `@EncryptedField` 的字段会执行密码运算。Mapper 方法标记
`@IgnoreEncrypted` 后，会跳过参数加密或结果解密，适用于验签、迁移和受控运维场景。

### 提供密码处理器

核心扩展点是 `EncryptedFieldHandler`：

```java
public interface EncryptedFieldHandler {
    <T> String encrypt(T value);
    <T> T decrypt(String value, Class<T> rtType);
    <T> String hmac(T value);
}
```

项目提供基于 Hutool、Jackson 和 HMAC 的 `DefaultEncryptedFieldHandler`。构造器接收的密钥和 IV 是 Base64 文本；应用应从
KMS、密钥文件或受保护的环境配置加载，不应硬编码在仓库中。

```java
EncryptedFieldHandler handler = new DefaultEncryptedFieldHandler(
        objectMapper,
        SymmetricAlgorithmType.AES,
        HmacAlgorithm.HmacSHA256,
        Mode.CBC,
        Padding.PKCS5Padding,
        base64Key,
        base64Iv,
        true
);
```

注意：

- 加密字段不适合数据库端的 `LIKE`、范围比较、排序和聚合；
- 等值查询只有在算法输出稳定且查询参数经过同一处理器时才可匹配；
- Wrapper 中无法定位实体字段元数据的简单参数不会被盲目加密；
- 默认处理器不记录明文、完整密文、密钥、IV 或 HMAC；自定义处理器也必须遵守相同约束；
- 密钥轮换需要显式的版本字段、双读或离线迁移方案，本组件不会自动猜测密钥版本。

## 表级签名与验签

表签名用于检测关键业务字段是否被绕过应用直接篡改，它不是访问控制或数据加密的替代品。

```java
@TableSignature
public class OrderPO {

    @TableSignatureField(order = 1)
    private String orderNo;

    @TableSignatureField(order = 2)
    private BigDecimal amount;

    @TableSignatureField(stored = true)
    private String rowSignature;
}
```

- `order` 决定签名原文的稳定拼接顺序；
- `stored = true` 标记保存签名结果的字段，该字段不参与原文拼接；
- `@TableSignature(unionAll = true)` 会把除签名存储字段外的可持久化字段纳入签名；
- `DataSignatureReadWriteProvider` 可将签名保存在实体字段或外部存储中；
- `DefaultDataSignatureHandler` 使用 `EncryptedFieldHandler.hmac` 计算签名。

需要批量补签、按原始密文验签或使用带签名语义的 Service API 时：

```java
public interface OrderMapper extends EnhanceMapper<OrderPO> {
}

@Service
public class OrderService
        extends EnhanceServiceImpl<OrderMapper, OrderPO> {
}
```

同时配置 `EnhanceSqlInjector`，以注入 `selectIgnoreDecryptById`、`selectIgnoreDecryptList` 等内部查询方法。应用若已有自定义
`ISqlInjector`，应合并其方法列表，不能注册两个互相覆盖的注入器。

`saveSigned`、`saveBatchSigned`、`updateSignedById`、`pageSigned` 等方法会将签名或验签纳入调用语义。批量写入必须在事务中执行，确保业务数据与签名同时提交或回滚。

## 多租户

本项目不重新实现租户 SQL 解析，而是提供可透传上下文和官方处理器适配器：

```java
TenantContext tenantContext = new TenantContext();
TenantLineHandler handler = new DefaultTenantLineHandler(
        tenantContext,
        "tenant_id",
        tableName -> "sys_config".equalsIgnoreCase(tableName)
);

MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(handler));
```

推荐使用作用域 API，异常和嵌套调用都能恢复先前租户：

```java
try (TenantContext.Scope ignored = tenantContext.open(tenantId)) {
    orderMapper.selectList(Wrappers.emptyWrapper());
}
```

`TenantContext` 使用 Alibaba TransmittableThreadLocal。异步任务必须使用 TTL 提供的 Executor 包装器或 Agent；只使用
`TransmittableThreadLocal` 类型本身并不能让任意线程池自动安全透传。请求出口仍应清理上下文，防止线程复用造成租户串扰。

缺少租户 ID 时，`DefaultTenantLineHandler` 会快速失败，而不是生成无租户条件的 SQL。

## 数据权限

数据权限基于 MyBatis-Plus 官方 `DataPermissionInterceptor` 和 `MultiDataPermissionHandler` 机制扩展。`@DataScopePlus` 描述
Mapper 方法的数据范围元数据，`DataScopeAnnotationHandler` 负责查找注解，`DataScopeExpressionProvider` 负责生成 JSqlParser
表达式。

```java
@DataScopePlus(
        tableAlias = "o",
        oneselfScopeName = "creator_id"
)
List<OrderPO> selectAuthorizedOrders(OrderQuery query);
```

生产系统必须实现 `DataScopeExpressionProvider`，从自己的认证、组织和角色模型中读取授权范围。
模块不提供默认放行实现，避免误配置时静默绕过权限。可使用 `DataScopeExpressions`
构造等值、IN、AND 和 OR 表达式，避免手工拼接 SQL。

数据权限属于安全边界：不要把前端传入的表名、列名或 SQL 片段直接拼接到表达式；超级管理员绕过、空权限集、跨部门角色合并和子查询别名都应有独立测试。

## 国际化

`i18n` 模块现在提供完整的同行多语言字段映射链：

- `I18nContext` 使用 TTL 保存当前 Locale；
- `DataI18nInnerInterceptor` 在查询结果映射完成后触发国际化；
- `DefaultDataI18nHandler` 处理实体字段和 Mapper 方法上的 `@I18nColumn`；
- 默认实现不执行额外 SQL，不会产生 N+1 查询。

```java
I18nContext i18nContext = new I18nContext();
interceptor.addInnerInterceptor(new DataI18nInnerInterceptor(
        i18nContext, new DefaultDataI18nHandler()));

try (I18nContext.Scope ignored = i18nContext.open(Locale.US)) {
    productMapper.selectList(Wrappers.emptyWrapper());
}
```

国际化映射应放在验签、解密之后，避免翻译值参与数库完整性校验。

## MySQL INSERT IGNORE

`InsertIgnoreInnerInterceptor` 只支持 MySQL 方言，并且仅在显式作用域内改写普通 INSERT：

```java
interceptor.addInnerInterceptor(new InsertIgnoreInnerInterceptor());

try (InsertIgnoreContext.Scope ignored = InsertIgnoreContext.open()) {
    orderMapper.insert(order);
}
```

作用域可嵌套，关闭后自动恢复。不要在 PostgreSQL、Oracle、SQL Server 等数据库上注册该拦截器；`INSERT IGNORE`
会把部分数据错误降级为警告，使用前应确认这符合业务一致性要求。

## SQL 观测

### 慢 SQL

`SqlObservationInnerInterceptor` 在真实执行完成后发布 `SqlObservation`，包含 Mapper ID、SQL、纳秒级耗时和执行异常。接收器实现
`SqlObservationSink`：

```java
SqlObservationInnerInterceptor observation = new SqlObservationInnerInterceptor();
observation.addSink(new SlowSqlLoggingSink(500, 1000));
observation.addSink(item -> metrics.record(
        item.getMappedStatementId(), item.getElapsedMillis(), item.isSuccess()));
```

也可通过 Java `ServiceLoader` 注册：

```text
META-INF/services/com.baomidou.mybatisplus.enhance.observation.SqlObservationSink
```

文件内容为实现类的全限定名。接收器运行在 SQL 调用线程中，应快速、无阻塞；发送网络指标或审计事件时应使用有界队列，并定义丢弃、重试和降级策略。单个接收器的运行时异常会被隔离，不会改变
SQL 结果。

### 超长 SQL

`LongSqlInnerInterceptor` 按 SQL 字符数检测超长语句：

```java
interceptor.addInnerInterceptor(
        new LongSqlInnerInterceptor(4000,
                (mappedStatementId, sql) -> alert(mappedStatementId)));
```

它不执行数据库 `EXPLAIN`，也不根据耗时判断慢 SQL。超长 SQL 与慢 SQL 是两个独立信号：前者用于发现巨型 `IN`
、异常拼接等结构问题，后者反映真实执行延迟。

## 扩展执行生命周期

自定义增强可以实现 `EnhanceInnerInterceptor`：

```java
public final class AuditInnerInterceptor implements EnhanceInnerInterceptor {

    @Override
    public void afterExecution(
            Executor executor,
            MappedStatement mappedStatement,
            Object parameter,
            BoundSql boundSql,
            Object result,
            Throwable failure,
            long elapsedNanos) {
        // 只做轻量旁路处理。
    }
}
```

该接口保留官方 `InnerInterceptor` 的全部前置钩子，并增加：

- `afterQuery`：查询成功并完成结果映射后；
- `afterUpdate`：更新成功后；
- `afterExecution`：查询或更新最终完成后，成功与失败都会调用。

`afterQuery`、`afterUpdate` 属于主执行链，异常会影响业务调用；`afterExecution`
是旁路通知，运行时异常会记录并隔离。需要强一致审计时，不应依赖被隔离的旁路回调，应使用业务事务或 Outbox。

## 安全与运维建议

- 密钥、IV 和 HMAC 密钥必须由受控密钥系统提供，禁止提交到 Git、打印到日志或暴露在异常信息中；
- 加密与签名建议使用不同密钥，并记录密钥版本以支持轮换；
- 不要记录完整 SQL 参数、明文敏感字段或解密结果；
- 租户与数据权限必须采用“上下文缺失即拒绝”的默认策略；
- 为拦截器顺序建立集成测试，覆盖插入、更新、批量操作、Wrapper、自定义 XML、缓存命中和异常路径；
- `@IgnoreEncrypted` 与原始密文 Mapper 方法应限制调用范围并纳入审计；
- 高并发场景应评估反射、JSON 序列化、JSqlParser 和密码运算的成本。

## 构建与测试

```bash
mvn clean verify
```

项目强制 Maven 3.9.6+。父 POM 会统一校验 Maven、Java 和模块依赖边界：Core 与 Extension
的传递依赖中不允许出现 Spring、MyBatis-Spring 或 MyBatis-Plus-Spring，只有
`mybatis-plus-enhance-spring` 明确放行 Spring。

项目使用 Maven CI-Friendly Version 和 `flatten-maven-plugin` 管理多模块版本：

- 根项目、所有子模块父版本和内部模块依赖统一使用 `${revision}`；
- 默认版本由父 POM 的 `revision` 属性定义；
- CI 可使用 `-Drevision=1.0.0` 构建指定的正式版本，无需批量修改所有 POM；
- `process-resources` 阶段为每个模块生成 `.flattened-pom.xml`；
- `install` 和 `deploy` 使用已经固化版本的 flattened POM；
- `mvn clean` 会删除生成的 flattened POM，文件也已加入 `.gitignore`。

例如，验证一个不修改源码 POM 的正式版本构建：

```bash
mvn -Drevision=1.0.0 -DskipTests package
```

发布前可在不签名、不上传的情况下检查源码包和 Javadoc 包：

```bash
mvn -Prelease -Dgpg.skip=true -DskipTests package
```

`release` Profile 会生成主构件、sources、Javadoc 和 GPG 签名，并通过 Sonatype Central
Publishing Maven Plugin 上传。发布账号在本机 `settings.xml` 中使用 `central` Server ID 配置，
仓库中不得保存令牌：

```xml
<server>
    <id>central</id>
    <username>${env.CENTRAL_USERNAME}</username>
    <password>${env.CENTRAL_TOKEN}</password>
</server>
```

```bash
mvn -Prelease deploy
```

默认 `autoPublish=false`，上传并校验成功后仍需在 Central Portal 确认发布，避免 CI 误发布。

项目同时沿用同组织组件的 Aliyun Maven 仓库：普通 `mvn deploy` 会根据版本后缀把
`SNAPSHOT` 和正式版本分别路由到父 POM 的 `snapshotRepository` 与 `repository`。对应 Server ID
的凭据同样只能存放在本机或 CI 的 `settings.xml`，不得写入项目 POM。

新增密码算法、权限表达式或拦截器时，应补充单元测试和真实数据库集成测试。尤其要验证参数对象是否被就地修改、上下文是否在异常后恢复、插件顺序是否符合预期。

## License

本项目采用 [Apache License 2.0](LICENSE) 发布。
