package com.baomidou.mybatisplus.enhance.i18n.handler.def;

import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nColumn;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nLocale;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nMapper;
import com.baomidou.mybatisplus.enhance.i18n.annotation.I18nSwitch;
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

    private static final String COUNT_SUFFIX_UPPER = "_COUNT";
    private static final String COUNT_SUFFIX_LOWER = "_count";

    private final ConcurrentMap<String, List<I18nColumn>> methodMappingCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, List<FieldMapping>> fieldMappingCache = new ConcurrentHashMap<>();

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

    private String resolveSourceName(I18nColumn definition, Locale locale) {
        for (I18nLocale localeDefinition : definition.i18n()) {
            if (sameLocale(locale, localeDefinition.locale().getLocale())) {
                return localeDefinition.column();
            }
        }
        return definition.column();
    }

    private boolean sameLocale(Locale requested, Locale configured) {
        return requested.toLanguageTag().equalsIgnoreCase(configured.toLanguageTag())
                || requested.toString().equalsIgnoreCase(configured.toString());
    }

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

    private List<I18nColumn> immutableDefinitions(I18nColumn[] definitions) {
        List<I18nColumn> result = new ArrayList<>(definitions.length);
        Collections.addAll(result, definitions);
        return Collections.unmodifiableList(result);
    }

    private String normalizeMethodName(String methodName) {
        if (methodName.endsWith(COUNT_SUFFIX_UPPER) || methodName.endsWith(COUNT_SUFFIX_LOWER)) {
            return methodName.substring(0, methodName.length() - COUNT_SUFFIX_UPPER.length());
        }
        return methodName;
    }

    private static final class FieldMapping {

        private final String targetName;
        private final String sourceName;

        private FieldMapping(String targetName, String sourceName) {
            this.targetName = targetName;
            this.sourceName = sourceName;
        }
    }
}
