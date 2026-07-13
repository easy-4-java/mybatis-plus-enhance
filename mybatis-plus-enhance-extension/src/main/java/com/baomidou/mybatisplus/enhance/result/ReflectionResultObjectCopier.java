package com.baomidou.mybatisplus.enhance.result;

import cn.hutool.core.util.ReflectUtil;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 基于反射的默认查询结果浅复制器。
 *
 * <p>Map 会复制为新的 {@link LinkedHashMap}；普通实体通过无参构造器创建，并复制实例字段。
 * 解密和国际化只替换实体顶层字段，因此浅复制能够隔离 MyBatis 缓存对象，同时避免对
 * 业务对象图执行代价不可控的深复制。</p>
 */
public class ReflectionResultObjectCopier implements ResultObjectCopier {

    /**
     * 复制单条查询结果。
     *
     * @param source 原始查询结果
     * @return 可安全修改的结果副本
     * @throws IllegalStateException 实体无法实例化或字段无法复制时抛出
     */
    @Override
    public Object copy(Object source) {
        if (Objects.isNull(source) || SimpleTypeRegistry.isSimpleType(source.getClass())) {
            return source;
        }
        if (source instanceof Map) {
            return new LinkedHashMap<>((Map<?, ?>) source);
        }
        Object target = ReflectUtil.newInstanceIfPossible(source.getClass());
        if (Objects.isNull(target)) {
            throw new IllegalStateException("Query result type requires a custom ResultObjectCopier: "
                    + source.getClass().getName());
        }
        for (Field field : instanceFields(source.getClass())) {
            try {
                field.setAccessible(true);
                field.set(target, field.get(source));
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Unable to copy query result field: "
                        + source.getClass().getName() + '.' + field.getName(), exception);
            }
        }
        return target;
    }

    private List<Field> instanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> current = type;
             Objects.nonNull(current) && current != Object.class;
             current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }
}
