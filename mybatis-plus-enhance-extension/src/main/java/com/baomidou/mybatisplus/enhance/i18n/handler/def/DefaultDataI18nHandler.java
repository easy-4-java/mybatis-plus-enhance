package com.baomidou.mybatisplus.enhance.i18n.handler.def;

import org.apache.mybatis.enhance.annotation.i18n.I18nColumn;
import org.apache.mybatis.enhance.annotation.i18n.I18nLocale;
import org.apache.mybatis.enhance.annotation.i18n.I18nMapper;
import org.apache.mybatis.enhance.annotation.i18n.I18nSwitch;
import com.baomidou.mybatisplus.enhance.i18n.handler.DataI18nHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.MappedStatement;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 {@link I18nColumn} 的默认同行字段国际化处理器。
 * <p>
 * 对于实体结果，处理字段上声明的 {@code I18nColumn}；对于 Map 或需要方法级
 * 配置的查询，处理 Mapper 方法上 {@link I18nMapper} 或 {@link I18nSwitch} 声明的字段。
 * 实现只在已查出的同一行数据中选择语言列，不执行额外 SQL，因此不会产生 N+1 查询。
 */
@Slf4j
public class DefaultDataI18nHandler implements DataI18nHandler {

    /**
     * MyBatis-Plus 分页 COUNT 语句使用的大写方法名后缀。
     */
    private static final String COUNT_SUFFIX_UPPER = "_COUNT";

    /**
     * 兼容部分分页实现使用的小写 COUNT 方法名后缀。
     */
    private static final String COUNT_SUFFIX_LOWER = "_count";

    /**
     * 按 MappedStatement ID 缓存 Mapper 方法上的国际化列定义。
     */
    private final ConcurrentMap<String, List<I18nColumn>> methodMappingCache = new ConcurrentHashMap<>();

    /**
     * 按结果类型、Locale 和语句 ID 缓存实体字段映射。
     */
    private final ConcurrentMap<String, List<FieldMapping>> fieldMappingCache = new ConcurrentHashMap<>();

    /**
     * 根据当前 Locale 对查询结果执行同行字段替换。
     *
     * <p>先应用 Mapper 方法声明，再应用实体字段声明。结果列表、列表元素可以为空；
     * Locale 与 MappedStatement 属于调用契约，不能为空。</p>
     *
     * @param locale          当前请求语言区域
     * @param mappedStatement 本次查询的 MyBatis 映射语句
     * @param results         MyBatis 已完成映射的结果列表
     */
    @Override
    public void handle(Locale locale, MappedStatement mappedStatement, List<Object> results) {
        Objects.requireNonNull(locale, "Locale must not be null");
        Objects.requireNonNull(mappedStatement, "MappedStatement must not be null");
        if (Objects.isNull(results) || results.isEmpty()) {
            return;
        }

        List<I18nColumn> methodDefinitions = methodMappingCache.computeIfAbsent(mappedStatement.getId(), this::resolveMethodDefinitions);
        for (Object result : results) {
            if (Objects.isNull(result)) {
                continue;
            }
            applyMethodDefinitions(result, locale, methodDefinitions);
            applyFieldDefinitions(result, locale, mappedStatement.getId());
        }
    }

    /**
     * 应用 Mapper 方法上声明的国际化列映射。
     *
     * @param result      单条实体或 Map 查询结果
     * @param locale      当前语言区域
     * @param definitions 方法级国际化列定义
     */
    private void applyMethodDefinitions(Object result, Locale locale,
                                        List<I18nColumn> definitions) {
        for (I18nColumn definition : definitions) {
            String targetName = definition.column();
            String sourceName = resolveSourceName(definition, locale);
            if (StringUtils.isBlank(targetName) || StringUtils.isBlank(sourceName)) {
                continue;
            }
            copyProperty(result, sourceName, targetName);
        }
    }

    /**
     * 应用实体字段上的国际化列定义；Map 结果仅使用方法级定义。
     *
     * @param result      单条实体查询结果
     * @param locale      当前语言区域
     * @param statementId MappedStatement ID，用于隔离映射缓存
     */
    private void applyFieldDefinitions(Object result, Locale locale, String statementId) {
        if (result instanceof Map) {
            return;
        }
        String cacheKey = result.getClass().getName() + '|' + locale.toLanguageTag() + '|' + statementId;
        List<FieldMapping> mappings = fieldMappingCache.computeIfAbsent(
                cacheKey, ignored -> resolveFieldMappings(result.getClass(), locale));
        for (FieldMapping mapping : mappings) {
            copyProperty(result, mapping.sourceName, mapping.targetName);
        }
    }

    /**
     * 扫描结果类型及其父类字段，构建目标字段到语言列的映射。
     *
     * @param resultType 查询结果类型
     * @param locale     当前语言区域
     * @return 不可变字段映射列表
     */
    private List<FieldMapping> resolveFieldMappings(Class<?> resultType, Locale locale) {
        List<FieldMapping> mappings = new ArrayList<>();
        for (Class<?> current = resultType;
             Objects.nonNull(current) && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                I18nColumn definition = field.getAnnotation(I18nColumn.class);
                if (Objects.isNull(definition)) {
                    continue;
                }
                String sourceName = resolveSourceName(definition, locale);
                if (StringUtils.isNotBlank(sourceName)) {
                    mappings.add(new FieldMapping(field.getName(), sourceName));
                }
            }
        }
        return Collections.unmodifiableList(mappings);
    }

    /**
     * 根据 Locale 选择国际化来源列；未匹配具体语言时回退到默认列。
     *
     * @param definition 国际化列定义
     * @param locale     当前语言区域
     * @return 应读取的来源列或属性名
     */
    private String resolveSourceName(I18nColumn definition, Locale locale) {
        for (I18nLocale localeDefinition : definition.i18n()) {
            if (sameLocale(locale, localeDefinition.locale().getLocale())) {
                return localeDefinition.column();
            }
        }
        return definition.column();
    }

    /**
     * 比较请求 Locale 与注解配置 Locale，兼容语言标签和传统字符串形式。
     *
     * @param requested  请求语言区域
     * @param configured 注解配置的语言区域
     * @return 两者表示同一语言区域时返回 {@code true}
     */
    private boolean sameLocale(Locale requested, Locale configured) {
        return requested.toLanguageTag().equalsIgnoreCase(configured.toLanguageTag())
                || requested.toString().equalsIgnoreCase(configured.toString());
    }

    /**
     * 将来源属性值复制到目标属性，兼容 Map 与普通 Java 对象。
     *
     * @param result     单条查询结果
     * @param sourceName 来源列或属性名
     * @param targetName 需要覆盖的目标列或属性名
     * @throws IllegalStateException 字段存在但反射访问失败时抛出
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void copyProperty(Object result, String sourceName, String targetName) {
        if (result instanceof Map) {
            Map map = (Map) result;
            if (map.containsKey(sourceName)) {
                map.put(targetName, map.get(sourceName));
            }
            return;
        }

        Field source = findField(result.getClass(), sourceName);
        Field target = findField(result.getClass(), targetName);
        if (Objects.isNull(source) || Objects.isNull(target)) {
            log.debug("Skip i18n field mapping because property is missing: {} -> {} on {}",
                    sourceName, targetName, result.getClass().getName());
            return;
        }
        try {
            source.setAccessible(true);
            target.setAccessible(true);
            target.set(result, source.get(result));
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException("Unable to apply i18n field mapping "
                    + sourceName + " -> " + targetName, exception);
        }
    }

    /**
     * 在指定类型及其父类层次中查找字段。
     *
     * @param type      起始类型
     * @param fieldName 字段名
     * @return 找到的字段；不存在时返回 {@code null}
     */
    private Field findField(Class<?> type, String fieldName) {
        for (Class<?> current = type;
             Objects.nonNull(current) && current != Object.class;
             current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                // 继续查找父类字段。
            }
        }
        return null;
    }

    /**
     * 解析 Mapper 方法上的 {@link I18nMapper} 或 {@link I18nSwitch} 定义。
     *
     * @param mappedStatementId Mapper 方法的全限定语句 ID
     * @return 不可变国际化列定义列表；无法解析或未声明时返回空列表
     */
    private List<I18nColumn> resolveMethodDefinitions(String mappedStatementId) {
        int separator = mappedStatementId.lastIndexOf('.');
        if (separator <= 0 || separator == mappedStatementId.length() - 1) {
            return Collections.emptyList();
        }
        String mapperClassName = mappedStatementId.substring(0, separator);
        String methodName = normalizeMethodName(mappedStatementId.substring(separator + 1));
        try {
            Class<?> mapperClass = Class.forName(mapperClassName);
            for (Method method : mapperClass.getDeclaredMethods()) {
                if (!method.getName().equals(methodName)) {
                    continue;
                }
                I18nMapper mapper = method.getAnnotation(I18nMapper.class);
                if (Objects.nonNull(mapper)) {
                    return immutableDefinitions(mapper.value());
                }
                I18nSwitch i18nSwitch = method.getAnnotation(I18nSwitch.class);
                if (Objects.nonNull(i18nSwitch)) {
                    return immutableDefinitions(i18nSwitch.value());
                }
            }
        } catch (ClassNotFoundException exception) {
            log.warn("Unable to resolve mapper class for i18n: {}", mapperClassName, exception);
        }
        return Collections.emptyList();
    }

    /**
     * 将注解数组复制为不可变列表，避免缓存结果被调用方修改。
     *
     * @param definitions 注解中的国际化列定义
     * @return 不可变定义列表
     */
    private List<I18nColumn> immutableDefinitions(I18nColumn[] definitions) {
        List<I18nColumn> result = new ArrayList<>(definitions.length);
        Collections.addAll(result, definitions);
        return Collections.unmodifiableList(result);
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

    /**
     * 已解析的实体国际化字段复制关系。
     */
    private static final class FieldMapping {

        /**
         * 接收国际化值的目标属性名。
         */
        private final String targetName;

        /**
         * 当前 Locale 对应的来源属性名。
         */
        private final String sourceName;

        /**
         * 创建字段复制关系。
         *
         * @param targetName 目标属性名
         * @param sourceName 来源属性名
         */
        private FieldMapping(String targetName, String sourceName) {
            this.targetName = targetName;
            this.sourceName = sourceName;
        }
    }
}
