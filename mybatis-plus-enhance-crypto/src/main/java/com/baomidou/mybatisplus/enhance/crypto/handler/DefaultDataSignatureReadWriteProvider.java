package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;

import static com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataSignatureHandler.PARAM_PAIRS_RE;

/**
 * 默认的数据签名读写提供者，直接通过反射读写签名字段
 */
public class DefaultDataSignatureReadWriteProvider implements DataSignatureReadWriteProvider {

    /**
     * 从对象中读取签名值
     *
     * @param rawObject 数据对象
     * @param tableInfo 对象表信息
     * @return 签名值
     */
    @Override
    public Optional<Object> readSignature(Object rawObject, TableInfo tableInfo) {
        // 1、获取存储签名结果的字段
        Optional<TableFieldInfo> signatureStoreFieldInfo = TableFieldHelper.getTableSignatureStoreFieldInfo(tableInfo);
        // 2、如果存储签名结果的字段存在，则进行签名验证
        if (signatureStoreFieldInfo.isPresent()) {
            // 2.1、如果存储签名结果的字段是Map类型，则从Map中获取签名值
            if (rawObject instanceof Map) {
                Map<?, ?> rawMap = (Map<?, ?>) rawObject;
                Object signFieldValue = MapUtil.getStr(rawMap, signatureStoreFieldInfo.get().getProperty());
                return Optional.ofNullable(signFieldValue);
            }
            // 2.2、如果存储签名结果的字段是对象类型，则从对象中获取签名值
            Object signFieldValue = ReflectUtil.getFieldValue(rawObject, signatureStoreFieldInfo.get().getProperty());
            return Optional.ofNullable(signFieldValue);
        }
        // 3、如果存储签名结果的字段不存在，则返回空
        return Optional.empty();
    }

    /**
     * 将签名值写到对象中
     *
     * @param rawObject 数据对象
     * @param tableInfo 对象表信息
     * @param signValue 签名值
     * @return 签名写出后是否继续执行数据更新操作
     */
    @Override
    public boolean writeSignature(Object rawObject, TableInfo tableInfo, AbstractWrapper<?, ?, ?> updateWrapper, String signValue) {
        // 1、获取存储的签名结果的字段
        Optional<TableFieldInfo> signatureStoreFieldInfo = TableFieldHelper.getTableSignatureStoreFieldInfo(tableInfo);
        // 3、如果数据表的HMAC字段存在，则将HMAC签名值通过反射设置到HMAC字段上
        if (signatureStoreFieldInfo.isPresent()) {
            // 3.1、如果存储签名结果的字段是Map类型，则从Map中获取签名值
            if (rawObject instanceof Map) {
                Map propMap = (Map) rawObject;
                if (Objects.nonNull(updateWrapper)) {
                    // 3.1.1、获取字段的原始值
                    String el = MapUtil.getStr(propMap, signatureStoreFieldInfo.get().getProperty());
                    // 3.1.2、进行参数正则匹配，如果匹配成功，则对参数进行签名处理
                    Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                    if (matcher.matches()) {
                        // 3.1.2.1、获取参数变量名
                        String valueKey = matcher.group(1);
                        // 3.1.2.2、替换参数变量值为签名后的值
                        updateWrapper.getParamNameValuePairs().put(valueKey, signValue);
                    }
                } else {
                    // 3.1.3、将签名值写入Map中
                    propMap.put(signatureStoreFieldInfo.get().getProperty(), signValue);
                }
            } else {
                if (Objects.nonNull(updateWrapper)) {
                    // 3.2、如果存储签名结果的字段是对象类型，则从对象中获取签名值
                    String el = (String) ReflectUtil.getFieldValue(rawObject, signatureStoreFieldInfo.get().getProperty());
                    // 3.2.1、进行参数正则匹配，如果匹配成功，则对参数进行签名处理
                    Matcher matcher = PARAM_PAIRS_RE.matcher(el);
                    if (matcher.matches()) {
                        // 3.1.2.1、获取参数变量名
                        String valueKey = matcher.group(1);
                        // 3.1.2.2、替换参数变量值为签名后的值
                        updateWrapper.getParamNameValuePairs().put(valueKey, signValue);
                    }
                } else {
                    // 3.3、将签名值写入对象中
                    ReflectUtil.setFieldValue(rawObject, signatureStoreFieldInfo.get().getField(), signValue);
                }
            }
            // 4、签名写出后继续执行数据更新操作
            return Boolean.TRUE;
        }
        // 5、未进行签名，不继续执行数据更新操作
        return Boolean.FALSE;
    }

}
