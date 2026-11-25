# mybatis-plus-enhance

[English](./README.md) | [简体中文](./README.zh-CN.md)

[![Java](https://img.shields.io/badge/Java-21-orange)](https://github.com/easy-4-java/mybatis-plus-enhance) [![License](https://img.shields.io/badge/license-Apache%202.0-green)](./LICENSE)

Modular enhancement framework for MyBatis-Plus: transparent encryption, table-level signature, tenant & data-scope support, i18n and SQL observation
[简体中文](./README.zh-CN.md)

> **Current branch**: `feature/3.0.x`
> **Version**: `3.0.x.x.20260630-SNAPSHOT`
> **JDK baseline**: 8
> **Project status**: maintenance (1.0.x line). Not yet published to Maven Central; artifacts are distributed via the Aliyun Maven repository and GitHub Releases.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Features & Status](#2-features--status)
- [3. Requirements & Compatibility](#3-requirements--compatibility)
- [4. Architecture & Modules](#4-architecture--modules)
- [5. Installation](#5-installation)
- [6. Quick Start](#6-quick-start)
- [7. Configuration](#7-configuration)
- [8. Core Usage](#8-core-usage)
- [9. Testing & Build](#9-testing--build)
- [10. Versioning & Branches](#10-versioning--branches)
- [11. Contributing & License](#11-contributing--license)

## 1. Project Overview

### 1.1 What it is

**mybatis-plus-enhance** is a modular enhancement layer on top of MyBatis-Plus (3.5.14). It adds capabilities without replacing the official plugin system:

- Transparent field encryption, decryption of query results and HMAC verification;
- Table-level data signature, verification and historical back-filling;
- Tenant context and a default adapter for the official `TenantLineInnerInterceptor`;
- Annotation-based data-scope metadata with expression extension points for the official `DataPermissionInterceptor`;
- `SqlDataScopeHandler` adapts legacy statement-based SQL predicates to the official permission interceptor.
- `QueryResultCallbackInterceptor` delivers normalized Mapper arguments and results after decryption and i18n. Register it either inside the enhance chain or as a standalone MyBatis plugin.
- Entity field internationalization (i18n) mapping;
- MySQL `INSERT IGNORE` scope;
- Long-SQL detection, real execution-time observation and slow-SQL logging;
- A unified enhancement interceptor chain with post-query, post-update and after-execution callbacks.

### 1.2 What it is not

- **Not a replacement for MyBatis-Plus.** Pagination, optimistic lock, tenant SQL parsing and data-permission parsing remain the official interceptors' job. Tenant and data-scope capabilities here are designed to be *combined* with the official ones.
- **Not a Spring Boot starter.** Spring integration (transactional services, dependency injection) is isolated in the `mybatis-plus-enhance-spring` module; `core` and `extension` stay Spring-free.
- **Not a re-implementation of JSqlParser-based SQL rewriting.** SQL condition injection is delegated to the official plugins.

### 1.3 Typical scenarios

| Scenario | Recommended entry | Result |
|---|---|---|
| Encrypt sensitive columns (mobile, ID card) transparently | `DataEncryptionInnerInterceptor` + `EncryptedFieldHandler` | Ciphertext persisted, plaintext on read |
| Detect out-of-band tampering of key business rows | `DataSignatureInnerInterceptor` + `@TableSignature` | HMAC-based row signature verified on read |
| Multi-tenant app without re-parsing SQL | `TenantContext` + `DefaultTenantLineHandler` + official `TenantLineInnerInterceptor` | Tenant condition injected safely |
| Row-level authorization beyond SQL conditions | `@DataScopePlus` + `DataScopeExpressionProvider` | Data-scope expressions on authorized queries |
| Multi-language entity fields | `DataI18nInnerInterceptor` + `I18nContext` | Same-row translated values, no extra SQL |
| Find oversized statements and real latency | `LongSqlInnerInterceptor` + `SqlObservationInnerInterceptor` | Structural + performance signals separated |

<a id="2-features--status"></a>
## 2. Features & Status

| Capability | Status | Notes |
|---|:---:|---|
| Transparent field encryption / decryption | Available | `@EncryptedField`; random IV per encryption; versioned envelope with `version/keyId/algorithm/mode/padding/iv/ciphertext/mac` |
| HMAC verification | Available | `EncryptedFieldHandler.verifyHmac`; separate encryption and authentication keys |
| Table-level signature & verification | Available | `@TableSignature` / `@TableSignatureField`; batch back-fill via signed Service API |
| Multi-tenant context | Available | TTL-based `TenantContext`; missing tenant ID fails fast instead of producing un-scoped SQL |
| Data-scope (data permission) extension | Available | `@DataScopePlus` + `DataScopeAnnotationHandler` + `DataScopeExpressionProvider` |
| Entity i18n mapping | Available | `@I18nColumn` handled by `DefaultDataI18nHandler`; no N+1 queries |
| MySQL `INSERT IGNORE` | Available | Explicit scope only; MySQL dialect only |
| Long-SQL detection | Available | Length-based, no `EXPLAIN`, no timing heuristics |
| SQL observation & slow-SQL logging | Available | `SqlObservation` carries mapper ID, SQL, nanos and failure; sinks via API or `ServiceLoader` |
| Post-query / post-update / after-execution lifecycle | Available | `EnhanceInnerInterceptor` adds the three callbacks on top of official `InnerInterceptor` hooks |
| Enhanced SQL injector | Available | `EnhanceSqlInjector` injects `selectIgnoreDecryptById` / `selectIgnoreDecryptList` / `updateSignatureById` and friends |

<a id="3-requirements--compatibility"></a>
## 3. Requirements & Compatibility

### 3.1 Baseline (this branch)

| Component | Version | Notes |
|---|---:|---|
| JDK | 21+ | Enforced by `maven-enforcer-plugin` |
| Maven | 3.0+ | Enforcer minimum; CI-friendly `${revision}` requires Maven 3.5+ |
| MyBatis | 3.5.19 | Pinned |
| MyBatis-Plus | 3.5.14 | Pinned |
| JSqlParser | 4.9 | Via `mybatis-plus-jsqlparser-4.9` |
| Spring Framework | 5.3.39 | `mybatis-plus-enhance-spring` only |
| Hutool core + crypto | 5.8.40 | Used by default crypto handlers |
| Jackson | 2.17.2 | JSON envelope serialization |
| API namespace | `javax.*` | `javax.annotation-api` 1.3.2 |

### 3.2 Version-line matrix

| Version line | Branch | JDK | Version pattern | Stack |
|---|---|---:|---|---|
| 1.0.x | `feature/3.0.x` | 8 | `1.0.x.*` | Spring 5.3.x, `javax.*`, JSqlParser 4.9 |
| 2.0.x | `feature/2.0.x` | 17 | `2.0.x.*` | Spring 6.2.x, `jakarta.*`, `mybatis-plus-jsqlparser` |
| 3.0.x | `feature/3.0.x` | 21 | `3.0.x.*` | `jakarta.*`, `mybatis-plus-jsqlparser` |

Version lines are maintained per branch (no shared-source profile switching). The `1.0.x` line only accepts compatibility fixes and dependency upgrades that still run on JDK 8. See `COMPATIBILITY.md` for the full rules.

<a id="4-architecture--modules"></a>
## 4. Architecture & Modules

```text
[ Your Application ]
        |
        | MyBatis-Plus + mybatis-plus-enhance
        v
+-----------------------------------------+
| MybatisPlusEnhanceInterceptor (chain)   |
|  SQL_REWRITE      Tenant / DataScope   |
|  PARAMETER_ENCRYPT  DataEncryption     |
|  DATA_SIGNATURE     DataSignature      |
|  RESULT_DECRYPTION  DataDecryption     |
|  RESULT_I18N        DataI18n           |
|  OBSERVATION        SqlObservation     |
+-----------------------------------------+
        |
        v
[ MyBatis / MyBatis-Plus + JDBC ] -> [ DB ]
```

| Module | Responsibility |
|---|---|
| `mybatis-plus-enhance-core` | Enhancement interceptor contract, post-query / post-update / after-execution lifecycle (`EnhanceInnerInterceptor`, `EnhancePhase`) and common base classes. Spring-free. |
| `mybatis-plus-enhance-extension` | Pluggable implementations: encryption, signature, tenant, data scope, i18n, SQL processing and observation. Spring-free. Pulls in `mybatis-plus-enhance-core` and `mybatis-enhance-annotation`. |
| `mybatis-plus-enhance-spring` | Spring integration: signed `IEnhanceService` / `EnhanceServiceImpl`, dependency injection and transaction support. |

Rule of thumb: plain MyBatis-Plus projects pick `core` or `extension`; projects that need signed Service semantics, DI and transactions pick `spring`.

<a id="5-installation"></a>
## 5. Installation

Maven:

```xml
<dependency>
    <groupId>io.github.easy4j</groupId>
    <artifactId>mybatis-plus-enhance-extension</artifactId>
    <version>3.0.x.x.20260630-SNAPSHOT</version>
</dependency>
```

Gradle:

```groovy
implementation 'io.github.easy4j:mybatis-plus-enhance-extension:3.0.x.x.20260630-SNAPSHOT'
```

Use `mybatis-plus-enhance-spring` instead when signed services / transactions are needed (it transitively includes extension). For the minimal interceptor contract only, depend on `mybatis-plus-enhance-core`.

Snapshot builds require an enabled snapshot repository (Aliyun Maven snapshot repository per `distributionManagement` in `pom.xml`). For production, pin a verified release version; do not use dynamic version ranges.

<a id="6-quick-start"></a>
## 6. Quick Start

The following enables decryption-on-read, signature-on-write and slow-SQL logging in one chain:

```java
@Configuration
public class MybatisConfiguration {

    @Bean
    public MybatisPlusEnhanceInterceptor mybatisPlusInterceptor(
            EncryptedFieldHandler encryptedFieldHandler,
            DataSignatureHandler dataSignatureHandler) {

        MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();

        // Write path: encrypt first, then sign the final ciphertext row.
        interceptor.addInnerInterceptor(
                new DataEncryptionInnerInterceptor(encryptedFieldHandler, true));
        interceptor.addInnerInterceptor(
                new DataSignatureInnerInterceptor(dataSignatureHandler, true, true));

        // Read path: registration order decides afterQuery order — verify, then decrypt.
        interceptor.addInnerInterceptor(
                new DataDecryptionInnerInterceptor(encryptedFieldHandler, true));

        interceptor.addInnerInterceptor(
                new SqlObservationInnerInterceptor(new SlowSqlLoggingSink(500)));
        return interceptor;
    }
}
```

Declare the entity:

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

**Expected result**: inserts write ciphertext (+ HMAC) to the `mobile` / `idCard` columns; selects transparently verify and decrypt back to plaintext; statements slower than 500 ms produce a slow-SQL log line via `SlowSqlLoggingSink`.

<a id="7-configuration"></a>
## 7. Configuration

This library is configured in code through the interceptor chain; there are no Spring configuration properties (the Spring module is bean-based). Interceptor order is a correctness constraint, not just performance tuning:

1. Tenant / data-scope SQL-condition plugins first (official `TenantLineInnerInterceptor` etc.);
2. Write parameters encrypted, then signed from the final values;
3. Query results verified first, then decrypted to business values;
4. SQL observation last, receiving the final execution outcome.

Do not register both the official `MybatisPlusInterceptor` and `MybatisPlusEnhanceInterceptor` on the same MyBatis `Configuration` — inner interceptors would run twice.

Key extension points:

| Extension point | Purpose |
|---|---|
| `EncryptedFieldHandler` | Encrypt / decrypt / HMAC contract; `DefaultEncryptedFieldHandler` (Hutool + Jackson + HMAC) provided |
| `CryptoKeyProvider` | Supplies current + controlled historical keys; `StaticCryptoKeyProvider` for tests |
| `DataSignatureHandler` / `DataSignatureReadWriteProvider` | Row signature compute and storage (entity field or external store) |
| `TenantContext` | TTL-based tenant holder with `open(tenantId)` scope API |
| `DataScopeExpressionProvider` | Builds JSqlParser expressions from your auth model (`DataScopeExpressions` helpers available) |
| `I18nContext` | TTL-based locale holder with `open(Locale)` scope API |
| `SqlObservationSink` | Consumes `SqlObservation`; auto-discoverable via Java `ServiceLoader` |
| `ResultObjectCopier` | `ReflectionResultObjectCopier` (default) or `noCopy()` when MyBatis local cache is disabled |

Secrets (keys, IVs, HMAC keys) must come from a managed key system — never commit them to the repository, log them or expose them in exception messages. Use different keys for encryption vs. authentication, and record key versions (`keyId`) to support rotation.

<a id="8-core-usage"></a>
## 8. Core Usage

### 8.1 Table-level signature

```java
@TableSignature
public class OrderPO {
    @TableSignatureField(order = 1)
    private String orderNo;
    @TableSignatureField(order = 2)
    private BigDecimal amount;
    @TableSignatureField(stored = true)
    private String rowSignature;   // stores the signature, not part of the signed payload
}
```

```java
public interface OrderMapper extends EnhanceBaseMapper<OrderPO> { }

@Service
public class OrderService extends EnhanceServiceImpl<OrderMapper, OrderPO> { }
```

Register `EnhanceSqlInjector` to enable internal queries such as `selectIgnoreDecryptById`, `selectIgnoreDecryptList` and `updateSignatureById`. If you already have a custom `ISqlInjector`, merge its method list — never register two injectors that overwrite each other. `saveSigned`, `saveBatchSigned`, `updateSignedById`, `pageSigned` carry signature semantics; batch writes must run inside a transaction so data and signature commit or roll back together.

### 8.2 Custom lifecycle enhancement

```java
public final class AuditInnerInterceptor implements EnhanceInnerInterceptor {

    @Override
    public void afterExecution(Executor executor, MappedStatement ms,
            Object parameter, BoundSql boundSql, Object result,
            Throwable failure, long elapsedNanos) {
        // Lightweight side-effect only.
    }
}
```

`afterQuery` / `afterUpdate` run inside the main execution chain — exceptions affect the business call. `afterExecution` is a side-channel notification: its runtime exceptions are logged and isolated, so do not build strongly-consistent auditing on it (use business transactions or an Outbox instead). Mapper methods marked `@IgnoreEncrypted` skip parameter encryption / result decryption — restrict their call scope and audit them.

<a id="9-testing--build"></a>
## 9. Testing & Build

```bash
mvn clean verify
```

- The parent POM enforces Maven version and Java version, and bans Spring / `mybatis-spring` / `mybatis-plus-spring` from `core` and `extension` transitive dependencies (only `mybatis-plus-enhance-spring` is allowed to pull Spring).
- JaCoCo runs `prepare-agent` and `report`, and checks the **90% line-coverage** rule on the `verify` phase (`haltOnFailure=false` so the build still completes with a report).
- Per `COMPATIBILITY.md`, the 1.0.x line must run real H2 combination tests covering cache, encryption, signature, back-fill and the enhanced SQL injector. The repo workflow `.github/workflows/compatibility-matrix.yml` checks out all three version lines and runs `mvn clean verify` on the respective JDK baselines.
- CI-Friendly versions: the root POM and all modules use `${revision}` (default `3.0.x.x.20260630-SNAPSHOT`), flattened via `flatten-maven-plugin`. Build a concrete release without touching POMs:

```bash
mvn -Drevision=1.0.0 -DskipTests package
```

- Dry-run the release packaging without signing/uploading:

```bash
mvn -Prelease -Dgpg.skip=true -DskipTests package
```

`mvn -Prelease deploy` produces main jar + sources + javadoc + GPG signatures. The `release` profile is wired for Sonatype Central Publishing with `autoPublish=false` (release must be confirmed in the Central Portal), while plain `mvn deploy` routes SNAPSHOT/release artifacts to the Aliyun Maven repository per `distributionManagement`. Repository credentials belong in your local/CI `settings.xml`, never in the project POM.

<a id="10-versioning--branches"></a>
## 10. Versioning & Branches

| Branch | Version pattern | JDK | Maintenance policy |
|---|---|---|---|
| `feature/1.0.x` (this branch) | `1.0.x.*` | 8 | Compatibility fixes and JDK-8-safe dependency upgrades only |
| `feature/2.0.x` | `2.0.x.*` | 17 | JDK 17 / Spring 6 / `jakarta.*` changes |
| `feature/3.0.x` | `3.0.x.*` | 21 | JDK 21 language/runtime features |

Cross-line upgrades may break binary compatibility; release notes must list package, configuration and dependency migrations. Keep public API semantics consistent across lines wherever possible.

<a id="11-contributing--license"></a>
## 11. Contributing & License

Contributions are welcome. Before opening a pull request, run `mvn clean verify` and describe compatibility, testing, documentation and migration impact. When adding crypto algorithms, permission expressions or interceptors, add unit tests and real-database integration tests — especially for in-place parameter mutation, context restoration after exceptions and interceptor ordering.

This project is licensed under the [Apache License 2.0](LICENSE).
