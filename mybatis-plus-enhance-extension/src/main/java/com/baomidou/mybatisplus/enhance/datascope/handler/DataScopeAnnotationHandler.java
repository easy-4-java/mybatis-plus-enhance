package com.baomidou.mybatisplus.enhance.datascope.handler;

import com.baomidou.mybatisplus.enhance.annotation.datascope.DataScopePlus;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 数据权限注解解析与 SQL 条件装配器。
 * <p>
 * 实现 MyBatis-Plus 官方 {@link MultiDataPermissionHandler}，根据 MappedStatement ID
 * 定位 Mapper 方法并解析 {@link DataScopePlus}。方法级配置优先于类级配置，
 * 因此可以通过 {@code enabled = false} 关闭单个 Mapper 方法的数据权限。
 * 解析结果按 MappedStatement ID 缓存，避免每次 SQL 执行都重复扫描反射元数据。
 */
@Slf4j
public class DataScopeAnnotationHandler implements MultiDataPermissionHandler {

    /**
     * MyBatis-Plus 分页 COUNT 语句使用的大写方法名后缀。
     */
    private static final String COUNT_SUFFIX_UPPER = "_COUNT";

    /**
     * 兼容部分分页实现使用的小写 COUNT 方法名后缀。
     */
    private static final String COUNT_SUFFIX_LOWER = "_count";

    /**
     * 根据业务权限上下文生成 JSqlParser 条件的扩展端口。
     */
    @Getter
    private final DataScopeExpressionProvider dataScopeExpressionProvider;

    /**
     * 按 MappedStatement ID 缓存方法级或 Mapper 级数据权限声明。
     */
    private final ConcurrentMap<String, Optional<DataScopePlus>> annotationCache = new ConcurrentHashMap<>();

    /**
     * 创建数据权限处理器。
     *
     * @param dataScopeExpressionProvider 不能为空的数据权限表达式提供者
     */
    public DataScopeAnnotationHandler(DataScopeExpressionProvider dataScopeExpressionProvider) {
        this.dataScopeExpressionProvider = Objects.requireNonNull(
                dataScopeExpressionProvider, "DataScopeExpressionProvider must not be null");
    }

    /**
     * 解析当前 Mapper 语句的数据权限声明并构建附加条件。
     *
     * @param table             SQL 当前正在处理的表
     * @param where             原始 WHERE 表达式
     * @param mappedStatementId Mapper 方法的全限定语句 ID
     * @return 需要追加的权限表达式；未声明或显式关闭时返回 {@code null}
     */
    @Override
    public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
        Optional<DataScopePlus> resolved = annotationCache.computeIfAbsent(mappedStatementId, this::resolveAnnotation);
        if (!resolved.isPresent() || !resolved.get().enabled()) {
            return null;
        }
        return dataScopeExpressionProvider.getDataScopeSqlSegment(
                table, where, mappedStatementId, resolved.get());
    }

    /**
     * 解析语句对应的 {@link DataScopePlus}，方法声明优先于 Mapper 类型声明。
     *
     * @param mappedStatementId Mapper 方法的全限定语句 ID
     * @return 已解析的声明；语句无声明或无法解析 Mapper 类型时返回空
     */
    private Optional<DataScopePlus> resolveAnnotation(String mappedStatementId) {
        int methodSeparator = mappedStatementId.lastIndexOf('.');
        if (methodSeparator <= 0 || methodSeparator == mappedStatementId.length() - 1) {
            log.warn("Invalid mapped statement id for data scope: {}", mappedStatementId);
            return Optional.empty();
        }

        String mapperClassName = mappedStatementId.substring(0, methodSeparator);
        String methodName = normalizeMethodName(mappedStatementId.substring(methodSeparator + 1));
        try {
            Class<?> mapperClass = Class.forName(mapperClassName);
            DataScopePlus methodAnnotation = findMethodAnnotation(mapperClass, methodName);
            if (Objects.nonNull(methodAnnotation)) {
                return Optional.of(methodAnnotation);
            }
            return Optional.ofNullable(mapperClass.getAnnotation(DataScopePlus.class));
        } catch (ClassNotFoundException exception) {
            log.warn("Unable to resolve mapper class for data scope: {}", mapperClassName, exception);
            return Optional.empty();
        }
    }

    /**
     * 在 Mapper 自身声明的方法中查找数据权限注解。
     *
     * @param mapperClass Mapper 类型
     * @param methodName  已移除分页后缀的方法名
     * @return 方法级注解；不存在时返回 {@code null}
     */
    private DataScopePlus findMethodAnnotation(Class<?> mapperClass, String methodName) {
        for (Method method : mapperClass.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            DataScopePlus annotation = method.getAnnotation(DataScopePlus.class);
            if (Objects.nonNull(annotation)) {
                return annotation;
            }
        }
        return null;
    }

    /**
     * 将分页插件生成的 COUNT 语句名还原为原始 Mapper 方法名。
     *
     * @param methodName MappedStatement 中的方法名
     * @return 去除 COUNT 后缀后的方法名
     */
    private String normalizeMethodName(String methodName) {
        if (methodName.endsWith(COUNT_SUFFIX_UPPER) || methodName.endsWith(COUNT_SUFFIX_LOWER)) {
            return methodName.substring(0, methodName.length() - COUNT_SUFFIX_UPPER.length());
        }
        return methodName;
    }
}
