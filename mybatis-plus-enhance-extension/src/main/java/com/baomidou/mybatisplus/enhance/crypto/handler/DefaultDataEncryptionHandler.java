package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedField;
import com.baomidou.mybatisplus.enhance.crypto.annotation.EncryptedTable;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus 表元数据的默认加解密处理器。
 *
 * <p>通过 {@code @EncryptedTable} 与 {@code @EncryptedField} 识别加密范围，支持实体、
 * Wrapper SET 参数以及查询结果对象。具体密码运算委托给 {@link EncryptedFieldHandler}。</p>
 */
public class DefaultDataEncryptionHandler implements DataEncryptionHandler {
    /**
     * 变量占位符正则
     */
    public static final Pattern PARAM_PAIRS_RE = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");
    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;

    public DefaultDataEncryptionHandler(EncryptedFieldHandler encryptedFieldHandler) {
        this.encryptedFieldHandler = encryptedFieldHandler;
    }

    /**
     * 通过API（save、updateById等）修改数据库时
     *
     * @param entity 参数
     */
    @Override
    public <T> boolean doEntityEncrypt(T entity) {

        // 0、判断是否为空或者是否为简单类型
        if (Objects.isNull(entity) || SimpleTypeRegistry.isSimpleType(entity.getClass())) {
            return Boolean.FALSE;
        }

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, entity.getClass());
        if (Objects.isNull(encryptedTable)) {
            return Boolean.FALSE;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = TableFieldHelper.getEncryptedFieldInfos(entity.getClass());
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return Boolean.FALSE;
        }

        // 4、遍历字段，对字段进行加密处理
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {
            // 4.1、获取字段上的@EncryptedField注解，如果没有则跳过
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }
            // 4.2、获取加密字段的原始值
            Object rawValue = ReflectUtil.getFieldValue(entity, fieldInfo.getField());
            // 4.4、如果原始值不为空，则对原始值进行加密处理
            if (Objects.nonNull(rawValue)) {
                // 4.4.1、对原始值进行加密处理
                String newValue = getEncryptedFieldHandler().encrypt(rawValue);
                // 4.4.2、将加密后的值通过反射设置到字段上
                ReflectUtil.setFieldValue(entity, fieldInfo.getField(), newValue);
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 加密更新 Wrapper 中标记为加密字段的赋值参数。
     *
     * @param entityClass   实体类型
     * @param updateWrapper 更新 Wrapper
     * @return 是否继续数据库操作
     */
    @Override
    public boolean doWrapperEncrypt(Class<?> entityClass, AbstractWrapper<?, ?, ?> updateWrapper) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, entityClass);
        if (Objects.isNull(encryptedTable)) {
            return Boolean.FALSE;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = TableFieldHelper.getEncryptedFieldInfos(entityClass);
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return Boolean.FALSE;
        }

        // 4、获取 SQL 更新字段内容，例如：name='1', age=2
        String sqlSet = updateWrapper.getSqlSet();
        // 4.1、解析SQL更新字段内容，例如：name='1', age=2，解析为Map 对象
        String[] sqlSetArr = StringUtils.split(sqlSet, Constants.COMMA);
        Map<String, String> propMap = Arrays.stream(sqlSetArr).map(el -> el.split(Constants.EQUALS)).collect(Collectors.toMap(el -> el[0], el -> el[1]));

        // 5、遍历字段，对字段进行加密处理
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {
            // 5.1、获取字段上的@EncryptedField注解，如果没有则跳过
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }
            // 5.2、获取字段的原始值
            String el = MapUtil.getStr(propMap, fieldInfo.getProperty());
            // 5.3、进行参数正则匹配，如果匹配成功，则对参数进行加密处理
            Matcher matcher = PARAM_PAIRS_RE.matcher(el);
            if (matcher.matches()) {
                // 5.3.1、获取参数变量名
                String valueKey = matcher.group(1);
                // 5.3.2、获取参数变量值
                Object rawValue = updateWrapper.getParamNameValuePairs().get(valueKey);
                // 5.3.4、如果参数变量值不为空，则对参数变量值进行加密处理
                if (Objects.nonNull(rawValue)) {
                    // 5.3.4.1、对原始值进行加密处理
                    String newValue = getEncryptedFieldHandler().encrypt(rawValue);
                    // 5.3.4.2、替换参数变量值为加密后的值
                    updateWrapper.getParamNameValuePairs().put(valueKey, newValue);
                }
            }
        }
        return Boolean.FALSE;
    }

    /**
     * 解密查询结果中的实体字段或 Map 列值。
     *
     * @param rawObject   单个查询结果
     * @param entityClass 实体类型
     * @param <T>         实体泛型
     */
    @Override
    public <T> void doRawObjectDecrypt(Object rawObject, Class<T> entityClass) {

        // 0、判断是否为空或者是否为简单类型
        if (Objects.isNull(rawObject) || SimpleTypeRegistry.isSimpleType(rawObject.getClass())) {
            return;
        }

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        EncryptedTable encryptedTable = AnnotationUtils.findFirstAnnotation(EncryptedTable.class, entityClass);
        if (Objects.isNull(encryptedTable)) {
            return;
        }

        // 3、获取该类的所有标记为加密字段的属性列表
        List<TableFieldInfo> encryptedFieldInfos = TableFieldHelper.getEncryptedFieldInfos(entityClass);
        if (CollectionUtils.isEmpty(encryptedFieldInfos)) {
            return;
        }

        // 4、遍历字段，对字段进行解密处理
        for (TableFieldInfo fieldInfo : encryptedFieldInfos) {

            // 4.1、获取字段上的@EncryptedField注解，如果没有则跳过
            EncryptedField encryptedField = AnnotationUtils.findFirstAnnotation(EncryptedField.class, fieldInfo.getField());
            if (Objects.isNull(encryptedField)) {
                continue;
            }

            // 4.2、获取签名字段的原始值
            Object rawValue;
            if (rawObject instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) rawObject;
                rawValue = MapUtil.getStr(rawMap, fieldInfo.getProperty());
            } else {
                rawValue = ReflectUtil.getFieldValue(rawObject, fieldInfo.getField());
            }

            // 4.3、如果原始值不为空，则对原始值进行解密处理
            if (Objects.nonNull(rawValue)) {
                // 4.3.1、对原始值进行解密处理
                Object newValue = getEncryptedFieldHandler().decrypt(Objects.toString(rawValue), rawValue.getClass());
                // 4.3.2、将解密后的值通过反射设置到字段上
                if (rawObject instanceof Map) {
                    Map<String, Object> rawMap = (Map<String, Object>) rawObject;
                    rawMap.put(fieldInfo.getProperty(), newValue);
                } else {
                    ReflectUtil.setFieldValue(rawObject, fieldInfo.getField(), newValue);
                }
            }
        }
    }

}
