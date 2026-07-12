package com.baomidou.mybatisplus.enhance.util;

import com.baomidou.mybatisplus.core.toolkit.ArrayUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.util.*;

/**
 * MyBatis Mapper 参数归一化工具。
 *
 * <p>用于拦截器判断开关状态，并从实体、数组、集合和 ParamMap 中提取去重后的处理对象。</p>
 */
public class ParameterUtils {

    /**
     * 判断参数型增强是否应跳过。
     *
     * @param globalSwitch    全局开关
     * @param parameterObject Mapper 参数
     * @return 开关关闭、参数为空或参数为简单类型时返回 {@code true}
     */
    public static boolean isSwitchOff(boolean globalSwitch, Object parameterObject) {
        return !globalSwitch || Objects.isNull(parameterObject) || SimpleTypeRegistry.isSimpleType(parameterObject.getClass());
    }


    /**
     * 判断结果集增强是否应跳过。
     *
     * @param globalSwitch 全局开关
     * @param rtObjectList 查询结果列表
     * @return 开关关闭或结果为空时返回 {@code true}
     */
    public static boolean isSwitchOff(boolean globalSwitch, List<Object> rtObjectList) {
        return !globalSwitch || Objects.isNull(rtObjectList) || CollectionUtils.isEmpty(rtObjectList);
    }

    /**
     * 提取特殊key值 (只支持外层参数,嵌套参数不考虑)
     * {@code List<Map<?, ?>>} 可以提取外层元素，但不递归展开更深层容器。
     *
     * @param parameterObject 参数
     * @return 预期可能为填充参数值
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Collection<Object> extractParameters(Object parameterObject) {
        if (parameterObject instanceof Collection) {
            return (Collection) parameterObject;
        } else if (ArrayUtils.isArray(parameterObject)) {
            return toCollection(parameterObject);
        } else if (parameterObject instanceof Map) {
            Collection<Object> parameters = new ArrayList<>();
            Map<String, Object> parameterMap = (Map) parameterObject;
            Set<Object> objectSet = new HashSet<>();
            parameterMap.forEach((k, v) -> {
                if (objectSet.add(v)) {
                    Collection<Object> collection = toCollection(v);
                    parameters.addAll(collection);
                }
            });
            return parameters;
        } else {
            return Collections.singleton(parameterObject);
        }
    }

    /**
     * 将单值、对象数组或集合归一化为集合。
     *
     * @param value 待转换值
     * @return 非空集合；输入为 {@code null} 时返回空集合
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object> toCollection(Object value) {
        if (Objects.isNull(value)) {
            return Collections.emptyList();
        }
        if (ArrayUtils.isArray(value) && !value.getClass().getComponentType().isPrimitive()) {
            return Arrays.asList((Object[]) value);
        } else if (Collection.class.isAssignableFrom(value.getClass())) {
            return (Collection<Object>) value;
        } else {
            return Collections.singletonList(value);
        }
    }


}
