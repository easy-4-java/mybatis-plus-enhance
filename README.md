## Mybatis-Plus-Enhance

### 项目介绍

> 基于 [MyBatis Plus](https://baomidou.com/introduce/) 的 `数据加解密(Data Encryption And Decryption)`、`数据签名与验签(Data Signature)`、`数据脱敏(Data Masking)`、`数据权限(Data Permission)`、`多租户数据隔离(Multi Tenant Data Isolation)`、`数据国际化(Data Internationalized)` 增强扩展。

Github： https://github.com/easy-4-java/mybatis-plus-enhance

**MyBatis Plus 增强扩展计划提供以下支持：**

- [x] 提供`3级等保密评`要求的`存储机密性`解决方案
- [x] 提供`3级等保密评`要求的`存储完整性`解决方案
- [ ] 提供`网络数据安全管理条例`要求的`数据权限`解决方案
- [ ] 提供`网络数据安全管理条例`要求的`数据脱敏`解决方案
- [ ] 提供`多租户SaaS平台`需要的`业务数据的隔离`解决方案
- [ ] 提供`多语言系统`需要的`数据国际化`解决方案

**说明：**

该组件参考了以下资料：
- https://blog.csdn.net/tianmaxingkonger/article/details/130986784


### 先决条件

`mybatis-plus-enhance` 依赖 `MyBatis Plus`，请确保您的项目中已经引入了 `MyBatis Plus` 相关依赖。

#### 依赖配置

请将以下依赖项添加到项目的 Maven `pom.xml` 文件中：

```xml
<!-- For Mybatis -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
</dependency>
<dependency>
   <groupId>io.github.easy4j</groupId>
   <artifactId>mybatis-plus-enhance</artifactId>
   <version>1.0.0-SNAPSHOT</version>
</dependency>
```

或者，在你的 Gradle 构建文件 `build.gradle` 中添加：

```groovy
dependencies {
    implementation 'com.baomidou:mybatis-plus-boot-starter',
    implementation 'io.github.easy4j:mybatis-plus-enhance'
}
```

### 实现原理

#### 一、基于 MyBatis Plus 插件的 `数据加解密(Data Encryption And Decryption)` 实现原理

> `数据加解密(Data Encryption And Decryption)` 是对数据进行保护的一种方式，通过对数据进行加密`加密`和`解密`，可以保证数据在传输和存储过程中的机密性。

1、增强 `MyBatis Plus` 拦截器逻辑，新增 `EnhanceInnerInterceptor`，用于增强`数据库查询`完成后的能力。

```java
public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)} 操作前置处理
     * <p>
     * 改改sql啥的
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     */
    default void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // do nothing
    }

}
```

2、自定义注解 `@EncryptedTable` 和 `@EncryptField`，用于标记需要加密和解密的实体类和字段。

```java
/**
 * 需要加解密的实体类用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface EncryptedTable {

}
/**
 * 需要加解密的字段用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EncryptedField {
}
```

3、自定义 `MyBatis Plus` Inner 插件 `DataEncryptionInnerInterceptor`，用于拦截数据库`更新操作`，对数据进行加密。

```java
/**
 * 数据加解密和签名拦截器，用于对新增/更新数据进行加密和签名操作
 */
@Slf4j
public class DataEncryptionInnerInterceptor extends JsqlParserSupport implements InnerInterceptor {
    // 详细实现请参考源码
}
```

4、自定义 `MyBatis Plus` Inner 插件 `DataDecryptionInnerInterceptor`，用于拦截数据库`查询操作`，对查询结果进行解密。

```java
/**
 * 数据解密拦截器，用于对查询结果进行解密操作
 */
@Slf4j
public class DataDecryptionInnerInterceptor implements EnhanceInnerInterceptor {
    // 详细实现请参考源码
}
```

5、自定义 `MyBatis Plus` 全局插件 `MybatisPlusEnhanceInterceptor` 替代默认的 `MybatisPlusInterceptor`，在原有的基础上增强了查询后的数据处理能力。

```java
/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
 * 参考：
 * - https://blog.csdn.net/tianmaxingkonger/article/details/130986784
 */
@Intercepts(
    {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
    }
)
public class MybatisPlusEnhanceInterceptor extends MybatisPlusInterceptor {
    // 详细实现请参考源码
}
```

#### 二、基于 MyBatis Plus 插件的 `数据签名与验签(Data Signature)` 实现原理（基于注解的自动签名和验签，适用于新项目）

> `数据签名与验签(Data Signature)` 是对数据进行完整性验证的一种方式，通过对数据进行签名`签名`和`验签`，可以保证数据在传输和存储过程中的完整性。
- 此方案逻辑是通过 `MyBatis Plus` 自定义插件和`自定义注解`实现对数据库中的数据进行`签名`和`验签`。
- 此方案逻辑是在 `数据加解密(Data Encryption And Decryption)` 方案基础上的扩展，对数据进行签名和验签操作。

1、增强 `MyBatis Plus` 拦截器逻辑，新增 `EnhanceInnerInterceptor`，用于增强`数据库查询`完成后的能力。

```java
public interface EnhanceInnerInterceptor extends InnerInterceptor {

    /**
     * {@link Executor#query(MappedStatement, Object, RowBounds, ResultHandler, CacheKey, BoundSql)} 操作前置处理
     * <p>
     * 改改sql啥的
     *
     * @param executor      Executor(可能是代理对象)
     * @param ms            MappedStatement
     * @param parameter     parameter
     * @param rowBounds     rowBounds
     * @param resultHandler resultHandler
     * @param boundSql      boundSql
     */
    default void afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql, List<Object> rtList) throws SQLException {
        // do nothing
    }

}
```

2、自定义注解 `@TableSignature` 和 `@TableSignatureField`，用于标记需要签名和验签的实体类和字段。

```java
/**
 * 需要签名的实体类用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface TableSignature {

    /**
     * 是否将实体类的所有字段进行联合签名
     */
    boolean unionAll() default false;

}
/**
 * 需要签名或存储签名的字段用这个注解
 * @author wandl
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface TableSignatureField {

    /**
     * 待加密字段的顺序，用于多个字段进行加密签名时保证加密字段的顺序
     */
    int order() default 0;

    /**
     * 是否作为签名结果的存储字段
     */
    boolean stored() default false;

}
```

3、自定义 `MyBatis Plus` Inner 插件 `DataSignatureInnerInterceptor`，用于拦截`数据库查询`和`数据库更新`操作，对数据进行签名和验签。

```java
/**
 * 数据签名和验签拦截器
 * 1、用于对新增/更新数据进行签名操作
 * 2、用于对查询数据进行验签操作
 */
@Slf4j
public class DataSignatureInnerInterceptor extends JsqlParserSupport implements EnhanceInnerInterceptor {
    // 详细实现请参考源码
}
```

4、自定义 `MyBatis Plus` 全局插件 `MybatisPlusEnhanceInterceptor` 替代默认的 `MybatisPlusInterceptor`，在原有的基础上增强了查询后的数据处理能力。

```java
/**
 * MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
 * 参考：
 * - https://blog.csdn.net/tianmaxingkonger/article/details/130986784
 */
@Intercepts(
    {
        @Signature(type = StatementHandler.class, method = "prepare", args = {Connection.class, Integer.class}),
        @Signature(type = StatementHandler.class, method = "getBoundSql", args = {}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class, CacheKey.class, BoundSql.class})
    }
)
public class MybatisPlusEnhanceInterceptor extends MybatisPlusInterceptor {
    // 详细实现请参考源码
}
```

#### 三、基于 MyBatis `接口增强` 的 `数据签名与验签(Data Signature)` 实现原理（手动调用签名和验签，适用于老项目改造）

1、自定义增强接口和抽象实现 `IEnhanceService`、`EnhanceServiceImpl` 和 `BaseEnhanceMapper`，替代默认的 `IService`、`ServiceImpl` 和 `BaseMapper`，并定义查询和更新接口和实现。

```java
/**
 * 增强 Service 接口
 * @param <T> 实体类
 */
public interface IEnhanceService<T> extends IService<T> {
    // 详细实现请参考源码
}
/**
 * EnhanceServiceImpl
 * <p>
 * 1. 数据签名和验签
 * 2. 重写 getSignedOne、getSignedOneOpt、getSignedMap、getSignedObj
 * 3. 重写 getBaseEnhanceMapper、getDataSignatureHandler
 *
 * @param <M> Mapper
 * @param <T> Entity
 */
public abstract class EnhanceServiceImpl<M extends BaseEnhanceMapper<T>, T> extends ServiceImpl<M, T> implements IEnhanceService<T> {
    // 详细实现请参考源码
}
```

#### 四、基于 MyBatis Plus 插件的 `数据脱敏(Data Masking)` 实现原理

> 通过 `MyBatis Plus` 插件实现数据写入或读取时对数据进行`脱敏`，保证数据在数据库中的存储安全性。

**数据脱敏的方法**

数据脱敏可以通过多种方法实现，常用的方法包括：

* ‌掩码算法（Masking）‌：例如，将身份证号码的前几位保留，其他位用“X”或“*”代替。
* ‌伪造姓名（Pseudonymization）‌：将真实姓名替换成随机生成的假名。
* ‌删除‌：随机删除敏感数据中的部分内容，如电话号码中的某些数字。
* ‌重排‌：打乱原始数据中某些字符或字段的顺序。
* ‌加噪‌：在数据中注入随机生成的字符或噪音。
* ‌加密‌：使用哈希函数如MD5或SHA-256将敏感数据转换为密文。


#### 五、基于 MyBatis Plus 插件的 `数据权限(Data Permission)` 实现原理

#### 六、基于 MyBatis Plus 插件的 `多租户数据隔离(Multi Tenant Data Isolation)` 实现原理

#### 七、基于 MyBatis Plus 插件的 `数据国际化(Data Internationalized)` 实现原理


### 使用指南

#### 一、基于 MyBatis Plus 插件的 `数据加解密(Data Encryption And Decryption)` 使用指南

1、实现 `EncryptedFieldHandler` 的接口的加解密和签名逻辑或使用默认实现 `DefaultEncryptedFieldHandler` 创建Handler实例。

```java
@Bean
public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
    // 随机生成sm4加密key
    String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
    System.out.println("sm4Key:"+sm4Key);
    sm4Key = Base64.encode(sm4Key.getBytes());
    System.out.println("sm4Key-Base64:"+ sm4Key);
    /**
     * 偏移向量，加盐
     */
    String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
    System.out.println("sm4Iv:"+sm4Iv);
    sm4Iv = Base64.encode(sm4Iv.getBytes());
    System.out.println("sm4Iv-Base64:"+ sm4Iv);

    ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
    return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
            Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
}
```

2、配置 `DataEncryptionInnerInterceptor` 拦截器

```java
/**
* mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
*/
@Bean
@Order(3)
public DataEncryptionInnerInterceptor dataEncryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
	return new DataEncryptionInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
}
```

3、配置 `DataDecryptionInnerInterceptor` 拦截器【可选】

```java
/**
 * mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
 */
@Bean
@Order(5)
public DataDecryptionInnerInterceptor dataDecryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
	return new DataDecryptionInnerInterceptor(true, encryptedFieldHandlerProvider.getIfAvailable());
}
```

4、配置 `Mybatis Plus` 全局拦截器 `MybatisPlusEnhanceInterceptor` ，用于替代 Mybatis Plus 的原生拦截器 `MybatisPlusInterceptor`，实现对查询后操作的增强。

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
													 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
	// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
	MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
	// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
	// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
	innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
	return interceptor;
}
```

5、`Mybatis Plus` 完整配置

>[danger] 这里要特别的注意，如果你的业务中，加解密拦截器和签名拦截器一同使用时，务必要调整好拦截器的执行顺序，注意对象的 @Order 注解值
- **更新操作执行顺序**：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
- **查询操作执行顺序**：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor

```java
@Configuration
@MapperScan({ "com.xxx.**.dao", "com.xxx.**.mapper" })
public class MybatisPlusConfiguration {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor =
     * false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
														 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
		// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
		MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
		// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
		// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
		innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
		return interceptor;
	}

    @Bean
    public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // 随机生成sm4加密key
        String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
        System.out.println("sm4Key:"+sm4Key);
        sm4Key = Base64.encode(sm4Key.getBytes());
        System.out.println("sm4Key-Base64:"+ sm4Key);
        /**
         * 偏移向量，加盐
         */
        String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
        System.out.println("sm4Iv:"+sm4Iv);
        sm4Iv = Base64.encode(sm4Iv.getBytes());
        System.out.println("sm4Iv-Base64:"+ sm4Iv);

        ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
                Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
    }

/**
	 * mybatis-plus：乐观锁插件
	 */
	@Bean
	@Order(1)
	public OptimisticLockerInnerInterceptor optimisticLockerInterceptor() {
		return new OptimisticLockerInnerInterceptor();
	}

	/**
	 * mybatis-plus：分页插件<br>
	 * 文档：http://mp.baomidou.com<br>
	 */
	@Bean
	@Order(2)
	public PaginationInnerInterceptor paginationInterceptor() {
		PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
		paginationInterceptor.setOverflow(false);
		return paginationInterceptor;
	}

	/**
	 * mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
	 */
	@Bean
	@Order(3)
	public DataEncryptionInnerInterceptor dataEncryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
		return new DataEncryptionInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
	}

	/**
	 * mybatis-plus 扩展：数据加解密拦截器，用于对新增/更新数据进行加密操作
	 */
	@Bean
	@Order(5)
	public DataDecryptionInnerInterceptor dataDecryptionInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
		return new DataDecryptionInnerInterceptor(true, encryptedFieldHandlerProvider.getIfAvailable());
	}

    /**
     * mybatis-plus：防止全表更新与删除插件
     @Bean
     public BlockAttackInnerInterceptor blockAttackInnerInterceptor() {
     BlockAttackInnerInterceptor sqlExplainInterceptor = new BlockAttackInnerInterceptor();
     return sqlExplainInterceptor;
     }
     */

    /**
     * 注入sql注入器
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }

    /**
     * 注入主键生成器
     @Bean
     public IKeyGenerator keyGenerator() {
     return new H2KeyGenerator();
     }
     */

    /*
     * oracle数据库配置JdbcTypeForNull
     * 参考：https://gitee.com/baomidou/mybatisplus-boot-starter/issues/IHS8X
     * 不需要这样配置了，参考 yml: mybatis-plus: confuguration dbc-type-for-null: 'null'
     *
     * @Bean public ConfigurationCustomizer configurationCustomizer(){ return new
     * MybatisPlusCustomizers(); }
     *
     * class MybatisPlusCustomizers implements ConfigurationCustomizer {
     *
     * @Override public void customize(org.apache.ibatis.session.Configuration
     * configuration) { configuration.setJdbcTypeForNull(JdbcType.NULL); } }
     */

}
```

6、定义PO类，使用 `@EncryptedTable` 和 `@EncryptedField` 注解

> 实体类上使用自定义注解，来标记需要进行加解密

```java
// 必须使用@EncryptedTable注解
@EncryptedTable
@TableName(value = "wsp_user")
public class UserEntity implements Serializable {
private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    // 使用@EncryptedField注解
    @EncryptedField
    private String mobile;
    // 使用@EncryptedField注解
    @EncryptedField
    private String email;
}
```

7、定义API接口

> 通过`Mybatis Plus` 提供的 `BaseMapper` API、Lambda、自定义mapper接口三种方式进行使用

```java
/**
 * 用户表控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {
 
    @Resource(name = "userServiceImpl")
    private IUserService userService;
    @Resource(name = "userXmlServiceImpl")
    private IUserService userXmlService;
 
    /**
     * 测试解密
     */
    @GetMapping(name = "测试解密", value = "/detail")
    public UserEntity detail(Long id) {
        // 测试MP API
//        UserEntity entity = userService.getById(id);
 
        // 测试自定义Mapper接口
        UserEntity entity = userXmlService.getById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }
 
    /**
     * 新增用户表，测试加密
     */
    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        // 测试MP API
//        userService.save(entity);
 
        // 测试自定义Mapper接口
        userXmlService.save(entity);
        return entity;
    }
 
    /**
     * 修改用户表
     */
    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        // 测试MP API
//        userService.updateById(entity);
 
        // 测试Lambda
//        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.eq(UserEntity::getId, entity.getId());
//        wrapper.set(UserEntity::getMobile, entity.getMobile());
//        wrapper.set(UserEntity::getName, entity.getName());
//        wrapper.set(UserEntity::getEmail, entity.getEmail());
//        userService.update(wrapper);
 
        // 测试自定义Mapper接口
        userXmlService.updateById(entity);
        return entity;
    }
}
```

#### 二、基于 MyBatis Plus 插件的 `数据签名与验签(Data Signature)` 使用指南（基于注解的自动签名和验签，适用于新项目）


1、实现 `EncryptedFieldHandler` 的接口的加解密和签名逻辑或使用默认实现 `DefaultEncryptedFieldHandler` 创建Handler实例。

```java
@Bean
public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
    // 随机生成sm4加密key
    String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
    System.out.println("sm4Key:"+sm4Key);
    sm4Key = Base64.encode(sm4Key.getBytes());
    System.out.println("sm4Key-Base64:"+ sm4Key);
    /**
     * 偏移向量，加盐
     */
    String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
    System.out.println("sm4Iv:"+sm4Iv);
    sm4Iv = Base64.encode(sm4Iv.getBytes());
    System.out.println("sm4Iv-Base64:"+ sm4Iv);

    ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
    return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
            Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
}
```

2、配置 Mybatis Plus 数字签名和签名验证拦截器 `DataSignatureInnerInterceptor`

```java
/**
 * mybatis-plus 扩展：数据签名和验签拦截器
 * 1、用于对新增/更新数据进行签名操作
 * 2、用于对查询数据进行验签操作
 * 3、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
 */
@Bean
@Order(4)
public DataSignatureInnerInterceptor dataSignatureInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
	return new DataSignatureInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
}
```

3、配置 `Mybatis Plus` 全局拦截器 `MybatisPlusEnhanceInterceptor` ，用于替代 Mybatis Plus 的原生拦截器 `MybatisPlusInterceptor`，实现对查询后操作的增强。

```java
@Bean
public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
													 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
	// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
	MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
	// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
	// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
	innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
	return interceptor;
}
```

4、`Mybatis Plus` 完整配置

>[danger] 这里要特别的注意，如果你的业务中，加解密拦截器和签名拦截器一同使用时，务必要调整好拦截器的执行顺序，注意对象的 @Order 注解值
- **更新操作执行顺序**：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
- **查询操作执行顺序**：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor

```java
@Configuration
@MapperScan({ "com.xxx.**.dao", "com.xxx.**.mapper" })
public class MybatisPlusConfiguration {

    /**
     * 新的分页插件,一缓和二缓遵循mybatis的规则,需要设置 MybatisConfiguration#useDeprecatedExecutor =
     * false 避免缓存出现问题(该属性会在旧插件移除后一同移除)
     */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider,
														 ObjectProvider<InnerInterceptor> innerInterceptorProvider) {
		// MybatisPlus 解密和签名验证拦截器，用于替代 MybatisPlus 的原生拦截器，实现对数据库字段的解密和签名验证操作
		MybatisPlusEnhanceInterceptor interceptor = new MybatisPlusEnhanceInterceptor();
		// 更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
		// 查询操作执行顺序：OptimisticLockerInnerInterceptor -> PaginationInnerInterceptor -> DataSignatureInnerInterceptor -> DataDecryptionInnerInterceptor
		innerInterceptorProvider.stream().sorted().forEach(interceptor::addInnerInterceptor);
		return interceptor;
	}

    @Bean
    public EncryptedFieldHandler encryptedFieldHandler(ObjectProvider<ObjectMapper> objectMapperProvider) {
        // 随机生成sm4加密key
        String sm4Key = RandomUtil.randomString(RandomUtil.BASE_CHAR_NUMBER, 16);
        System.out.println("sm4Key:"+sm4Key);
        sm4Key = Base64.encode(sm4Key.getBytes());
        System.out.println("sm4Key-Base64:"+ sm4Key);
        /**
         * 偏移向量，加盐
         */
        String sm4Iv = RandomStringUtils.secureStrong().nextAscii(16);
        System.out.println("sm4Iv:"+sm4Iv);
        sm4Iv = Base64.encode(sm4Iv.getBytes());
        System.out.println("sm4Iv-Base64:"+ sm4Iv);

        ObjectMapper objectMapper =  objectMapperProvider.getIfAvailable(ObjectMapper::new);
        return new DefaultEncryptedFieldHandler(objectMapper, SymmetricAlgorithmType.SM4, HmacAlgorithm.HmacSM3,
                Mode.CBC, Padding.PKCS5Padding, sm4Key, sm4Iv);
    }

/**
	 * mybatis-plus：乐观锁插件
	 */
	@Bean
	@Order(1)
	public OptimisticLockerInnerInterceptor optimisticLockerInterceptor() {
		return new OptimisticLockerInnerInterceptor();
	}

	/**
	 * mybatis-plus：分页插件<br>
	 * 文档：http://mp.baomidou.com<br>
	 */
	@Bean
	@Order(2)
	public PaginationInnerInterceptor paginationInterceptor() {
		PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor();
		paginationInterceptor.setOverflow(false);
		return paginationInterceptor;
	}

	/**
	 * mybatis-plus 扩展：数据签名和验签拦截器
	 * 1、用于对新增/更新数据进行签名操作
	 * 2、用于对查询数据进行验签操作
	 * 3、更新操作执行顺序：DataEncryptionInnerInterceptor -> DataSignatureInnerInterceptor
	 */
	@Bean
	@Order(4)
	public DataSignatureInnerInterceptor dataSignatureInnerInterceptor(ObjectProvider<EncryptedFieldHandler> encryptedFieldHandlerProvider) {
		return new DataSignatureInnerInterceptor(encryptedFieldHandlerProvider.getIfAvailable());
	}

    /**
     * mybatis-plus：防止全表更新与删除插件
     @Bean
     public BlockAttackInnerInterceptor blockAttackInnerInterceptor() {
     BlockAttackInnerInterceptor sqlExplainInterceptor = new BlockAttackInnerInterceptor();
     return sqlExplainInterceptor;
     }
     */

    /**
     * 注入sql注入器
     */
    @Bean
    public ISqlInjector sqlInjector() {
        return new DefaultSqlInjector();
    }

    /**
     * 注入主键生成器
     @Bean
     public IKeyGenerator keyGenerator() {
     return new H2KeyGenerator();
     }
     */

    /*
     * oracle数据库配置JdbcTypeForNull
     * 参考：https://gitee.com/baomidou/mybatisplus-boot-starter/issues/IHS8X
     * 不需要这样配置了，参考 yml: mybatis-plus: confuguration dbc-type-for-null: 'null'
     *
     * @Bean public ConfigurationCustomizer configurationCustomizer(){ return new
     * MybatisPlusCustomizers(); }
     *
     * class MybatisPlusCustomizers implements ConfigurationCustomizer {
     *
     * @Override public void customize(org.apache.ibatis.session.Configuration
     * configuration) { configuration.setJdbcTypeForNull(JdbcType.NULL); } }
     */

}
```

5、定义PO类，使用 `@TableSignature` 和 `@TableSignatureField`注解

> 实体类上使用自定义注解，来标记需要进行加解密

```java
@TableSignature
@TableName(value = "wsp_user")
public class UserEntity implements Serializable {
private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 1)
    private String mobile;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 2)
    private String email;
    // 需要存储HMAC的字段用这个注解
    @TableSignatureField(stored = true)
    private String hamc;
}
```

6、定义API接口

> 通过`Mybatis Plus` 提供的 `BaseMapper` API、Lambda、自定义mapper接口三种方式进行使用

```java
/**
 * 用户表控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {
 
    @Resource(name = "userServiceImpl")
    private IUserService userService;
    @Resource(name = "userXmlServiceImpl")
    private IUserService userXmlService;
 
    /**
     * 测试解密
     */
    @GetMapping(name = "测试解密", value = "/detail")
    public UserEntity detail(Long id) {
        // 测试MP API
//        UserEntity entity = userService.getById(id);
 
        // 测试自定义Mapper接口
        UserEntity entity = userXmlService.getById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }
 
    /**
     * 新增用户表，测试加密
     */
    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        // 测试MP API
//        userService.save(entity);
 
        // 测试自定义Mapper接口
        userXmlService.save(entity);
        return entity;
    }
 
    /**
     * 修改用户表
     */
    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        // 测试MP API
//        userService.updateById(entity);
 
        // 测试Lambda
//        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<>();
//        wrapper.eq(UserEntity::getId, entity.getId());
//        wrapper.set(UserEntity::getMobile, entity.getMobile());
//        wrapper.set(UserEntity::getName, entity.getName());
//        wrapper.set(UserEntity::getEmail, entity.getEmail());
//        userService.update(wrapper);
 
        // 测试自定义Mapper接口
        userXmlService.updateById(entity);
        return entity;
    }
}
```

#### 三、基于 MyBatis 接口增强 的 `数据签名与验签(Data Signature)` 使用指南（手动调用签名和验签，适用于老项目改造）

1、定义业务接口 `UserServiceImpl` 和 `UserService`，并继承 `EnhanceServiceImpl` 和 `IEnhanceService` 接口。

UserService 继承 `IEnhanceService` 接口。

```java
import com.baomidou.mybatisplus.enhance.service.IEnhanceService;

public interface UserService extends IEnhanceService<MyEntity> {

}
```

UserServiceImpl 继承 `EnhanceServiceImpl` 接口，实现 `UserService` 接口。

```java
import com.baomidou.mybatisplus.enhance.service.impl.EnhanceServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends EnhanceServiceImpl<UserMapper, UserEntity> implements MyService {

}
```

UserMapper 和默认用法一样，继承 `BaseMapper` 接口。

```java
@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

}
```

2、定义PO类，使用 `@TableSignature` 和 `@TableSignatureField`注解

> 实体类上使用自定义注解，来标记需要进行加解密

```java
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignature;
import com.baomidou.mybatisplus.enhance.crypto.annotation.TableSignatureField;
import lombok.Data;

import java.io.Serializable;

@Data
@TableSignature
@TableName(value = "my_entity")
public class MyEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    private String name;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 1)
    private String mobile;
    // 使用@TableSignatureField 注解
    @TableSignatureField(order = 2)
    private String email;
    // 需要存储HMAC的字段用这个注解
    @TableSignatureField(stored = true)
    private String hamc;
}
```

3、定义API接口

> 通过`Mybatis Plus` 提供的 `BaseMapper` API、Lambda、自定义mapper接口三种方式进行使用

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
/**
 * 用户表控制器
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    /**
     * 测试验签
     */
    @GetMapping(name = "测试查询签名验证", value = "/detail")
    public UserEntity detail(Long id) {
        // 测试MP API（查询结果并验签）
        UserEntity entity = userService.getSignedById(id);
        if (null == entity) {
            return new UserEntity();
        }
        return entity;
    }

    /**
     * 新增用户表，测试加密
     */
    @GetMapping(name = "新增用户表，测试加密", value = "/add")
    public UserEntity add(UserEntity entity) {
        // 测试MP API
        userService.saveSigned(entity);
        return entity;
    }

    /**
     * 修改用户表
     */
    @GetMapping(name = "修改用户表", value = "/update")
    public UserEntity update(UserEntity entity) {
        // 测试MP API
        userService.updateSignedById(entity);
        return entity;
    }
}
```

#### 四、基于 MyBatis Plus 插件的 `数据脱敏(Data Masking)` 使用指南

2，在vo类上添加功能注解使得插件生效：
```java
@SensitiveEncryptEnabled
@Data
public class UserDTO {

   private Integer id;
    /**
     * 用户名
     */
    @EncryptField
    private String userName;
    /**
     * 脱敏的用户名
     */
    @SensitiveField(SensitiveType.CHINESE_NAME)
    private String userNameSensitive;
    /**
     * 值的赋值不从数据库取，而是从userName字段获得。
     */
    @SensitiveBinded(bindField = "userName",value = SensitiveType.CHINESE_NAME)
    private String userNameOnlyDTO;
    /**
     * 身份证号
     */
    @EncryptField
    private String idcard;
    /**
     * 脱敏的身份证号
     */
    @SensitiveField(SensitiveType.ID_CARD)
    private String idcardSensitive;
    /**
     * 一个json串，需要脱敏
     * SensitiveJSONField标记json中需要脱敏的字段
     */
    @SensitiveJSONField(sensitivelist = {
            @SensitiveJSONFieldKey(key = "idcard",type = SensitiveType.ID_CARD),
            @SensitiveJSONFieldKey(key = "username",type = SensitiveType.CHINESE_NAME),
    })
    private String jsonStr;

    private int age;

    @SensitiveField(SensitiveType.EMAIL)
    private String email;
}
```
## 注解详解
#### @SensitiveEncryptEnabled

    标记在类上，声明此数据库映射的model对象开启数据加密和脱敏功能。

#### @EncryptField

    标记在字段上，必须是字符串，声明此字段在和数据库交互前将数据加密。
    update,select,insert 都会将指定的字段设置为密文与数据库进行交互。
    在select的结果集里，此字段会自动解密成明文。因此，业务是无感知的。

#### @SensitiveField(SensitiveType.CHINESE_NAME)

    标记在字段上，必须是字符串。
    声明此字段在入库或者修改时，会脱敏存储。
    SensitiveType是脱敏类型，详见脱敏类型章节。
    
    一般考虑如下场景。
    用户的手机号需要在数据库存储为加密的密文，为了查询方便，可能数据库也会有一个脱敏的手机号字段。
    那就可以这样定义两个字段：
    
    //在数据库加密存储的
    @EncryptField
    private String phone;
    //在数据库脱敏存储的
    @SensitiveField(SensitiveType.MOBILE_PHONE)
    private String phoneSensitive;
    
    而业务代码赋值时，可以赋值两次：
    
    ......
    user.setPhone("18233586969");
    user.setPhoneSensitive("18233586969");
    ......
    此时，数据库两个字段，一个会加密，一个会脱敏。
    在查询请求的响应结果集里，phone是明文，phoneSensitive是脱敏的。
#### @SensitiveJSONField

    标记在json字符串上，声明此json串在入库前会将json中指定的字段脱敏。
    
    例如：
    @SensitiveJSONField(sensitivelist = {
                @SensitiveJSONFieldKey(key = "idcard",type = SensitiveType.ID_CARD),
                @SensitiveJSONFieldKey(key = "username",type = SensitiveType.CHINESE_NAME),
        })
    private String jsonStr;
    
    如果jsonStr原文为
    {
      "age":18,
      "idcard":"130722188284646474",
      "username":"吴彦祖",
      "city":"北京"
    }
    则脱敏后为：
    {
      "age":18,
      "idcard":"130***********6474",
      "username":"吴**",
      "city":"北京"
    
    }
    使用场景：
    有时候数据库会存储一些第三方返回的json串，可能会包含敏感信息。
    业务里不需要用到敏感信息的明文，此时可以脱敏存储整个json串。

###### @SensitiveBinded(bindField = "userName",value = SensitiveType.CHINESE_NAME)

     此注解适用于如下场景：
     例如，数据库只存了username字段的加密信息，没有冗余脱敏展示的字段。
     我的响应类里希望将数据库的加密的某个字段映射到响应的两个属性上（一个解密的属性，一个脱敏的属性）就可以使用该注解。
     例如，dto里有如下字段：
     @EncryptField
     private String username
     
     @SensitiveBinded(bindField = "userName",value = SensitiveType.CHINESE_NAME)
     private String userNameOnlyDTO;
     
     则当查询出结果时，userNameOnlyDTO会赋值为username解密后再脱敏的值。
     相当于数据库的一个字段的值以不同的形式映射到了对象的两个字段上。
###### 脱敏类型
```java
    public enum SensitiveType {
        /**
         * 不脱敏
         */
        NONE,
        /**
         * 默认脱敏方式
         */
        DEFAUL,
        /**
         * 中文名
         */
        CHINESE_NAME,
        /**
         * 身份证号
         */
        ID_CARD,
        /**
         * 座机号
         */
        FIXED_PHONE,
        /**
         * 手机号
         */
        MOBILE_PHONE,
        /**
         * 地址
         */
        ADDRESS,
        /**
         * 电子邮件
         */
        EMAIL,
        /**
         * 银行卡
         */
        BANK_CARD,
        /**
         * 公司开户银行联号
         */
        CNAPS_CODE,
        /**
         * 支付签约协议号
         */
        PAY_SIGN_NO
    }
```

#### 五、基于 MyBatis Plus 插件的 `数据权限(Data Permission)` 使用指南

#### 六、基于 MyBatis Plus 插件的 `多租户数据隔离(Multi Tenant Data Isolation)` 使用指南

#### 七、基于 MyBatis Plus 插件的 `数据国际化(Data Internationalized)` 使用指南
