package com.baomidou.mybatisplus.enhance.crypto.handler;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import org.apache.mybatis.enhance.annotation.crypto.TableSignature;
import org.apache.mybatis.enhance.annotation.crypto.TableSignatureField;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于 MyBatis-Plus 表元数据的默认数据签名处理器。
 *
 * <p>按照 {@code @TableSignatureField(order)} 的稳定顺序拼接签名原文，通过
 * {@link EncryptedFieldHandler#hmac(Object)} 计算签名，并委托
 * {@link DataSignatureReadWriteProvider} 读写签名结果。</p>
 */
@Slf4j
public class DefaultDataSignatureHandler implements DataSignatureHandler {

    /**
     * 变量占位符正则
     */
    public static final Pattern PARAM_PAIRS_RE = Pattern.compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");
    /**
     * 加解密处理器，加解密的情况都在该处理器中自行判断
     */
    @Getter
    private final EncryptedFieldHandler encryptedFieldHandler;

    /**
     * 负责从实体、Map 或 Wrapper 参数读写签名值的策略。
     */
    @Getter
    private final DataSignatureReadWriteProvider signatureReadWriteProvider;

    /**
     * 使用默认实体字段签名读写器创建处理器。
     *
     * @param encryptedFieldHandler 提供 HMAC 能力的字段密码处理器
     */
    public DefaultDataSignatureHandler(EncryptedFieldHandler encryptedFieldHandler) {
        this(encryptedFieldHandler, new DefaultDataSignatureReadWriteProvider());
    }

    /**
     * 使用自定义签名存储策略创建处理器。
     *
     * @param encryptedFieldHandler      提供 HMAC 能力的字段密码处理器
     * @param signatureReadWriteProvider 签名读写策略
     */
    public DefaultDataSignatureHandler(EncryptedFieldHandler encryptedFieldHandler, DataSignatureReadWriteProvider signatureReadWriteProvider) {
        this.encryptedFieldHandler = encryptedFieldHandler;
        this.signatureReadWriteProvider = signatureReadWriteProvider;
    }

    /**
     * 通过API（save、updateById等）修改数据库时
     *
     * @param parameter 参数
     * @return 签名完成后是否继续执行数据更新操作
     */
    @Override
    public boolean doEntitySignature(Object parameter) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 3、判断自定义Entity类是否被@TableSignature所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, parameter.getClass());
        if (Objects.isNull(tableSignature)) {
            return Boolean.FALSE;
        }

        // 4、获取自定义Entity类联合签名的字段信息列表（排序后）
        TableInfo tableInfo = TableInfoHelper.getTableInfo(parameter.getClass());
        List<TableFieldInfo> signatureFieldInfos = TableFieldHelper.getSortedSignatureFieldInfos(tableInfo);
        if (CollectionUtils.isEmpty(signatureFieldInfos)) {
            return Boolean.FALSE;
        }

        // 5、遍历字段，对字段进行签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 5.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 5.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行签名处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 5.2.1、获取签名字段的原始值
                Object fieldValue = ReflectUtil.getFieldValue(parameter, fieldInfo.getField());
                // 5.2.2、如果签名字段需要进行HMAC签名，则将原始值加入到HMAC签名列表中
                hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
            }
        }

        // 6、如果实体类需要进行单表数据存储完整性验证，则对数据表进行签名处理
        if (hmacJoiner.length() > 0) {
            // 6.1、对数据进行签名处理
            String hmacValue = getEncryptedFieldHandler().hmac(hmacJoiner.toString());
            // 6.2、调用签名读写提供者，将签名值写入到实体类中或外部存储
            return getSignatureReadWriteProvider().writeSignature(parameter, tableInfo, null, hmacValue);
        }
        return Boolean.FALSE;
    }

    /**
     * 通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
     *
     * @param entityClass   实体类
     * @param updateWrapper 更新条件
     * @return 签名完成后是否继续执行数据更新操作
     */
    @Override
    public boolean doWrapperSignature(Class<?> entityClass, AbstractWrapper<?, ?, ?> updateWrapper) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 2、判断自定义Entity的类是否被@EncryptedTable所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, entityClass);
        if (Objects.isNull(tableSignature)) {
            return Boolean.FALSE;
        }

        throw ExceptionUtils.mpe("签名表【%s】不允许通过 UpdateWrapper 直接计算整行签名；"
                        + "请使用 DEFERRED_RESIGN 策略读取完整行后补签",
                entityClass.getName());
    }

    /**
     * 对单个对象进行解密
     *
     * @param rawObject   单个对象或Map
     * @param entityClass 实体类
     * @param <T>         对象类型
     */
    @Override
    public <T> void doSignatureVerification(Object rawObject, Class<T> entityClass) {

        // 1、判断加解密处理器不为空，为空则抛出异常
        ExceptionUtils.throwMpe(null == encryptedFieldHandler, "Please implement EncryptedFieldHandler processing logic");

        // 3、判断自定义Entity的类是否被@TableSignature所注解
        TableSignature tableSignature = AnnotationUtils.findFirstAnnotation(TableSignature.class, entityClass);
        if (Objects.isNull(tableSignature)) {
            return;
        }

        // 4、获取自定义Entity类联合签名的字段信息列表（排序后）
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entityClass);
        List<TableFieldInfo> signatureFieldInfos = TableFieldHelper.getSortedSignatureFieldInfos(tableInfo);
        if (CollectionUtils.isEmpty(signatureFieldInfos)) {
            return;
        }

        // 5、遍历字段，对字段进行签名处理
        StringJoiner hmacJoiner = new StringJoiner(Constants.PIPE);
        for (TableFieldInfo fieldInfo : signatureFieldInfos) {
            // 5.1、获取字段上的@TableSignatureField注解
            TableSignatureField signatureField = AnnotationUtils.findFirstAnnotation(TableSignatureField.class, fieldInfo.getField());
            // 5.2、如果Entity类被@TableSignature注解，并且 unionAll = true；或者字段被@TableSignatureField注解，并且 stored = false，则进行验签处理
            if (tableSignature.unionAll() || (Objects.nonNull(signatureField) && !signatureField.stored())) {
                // 5.2.1、获取签名字段的原始值
                Object fieldValue;
                if (rawObject instanceof Map) {
                    Map<?, ?> rawMap = (Map<?, ?>) rawObject;
                    fieldValue = rawMap.containsKey(fieldInfo.getProperty())
                            ? rawMap.get(fieldInfo.getProperty())
                            : rawMap.get(fieldInfo.getColumn());
                } else {
                    fieldValue = ReflectUtil.getFieldValue(rawObject, fieldInfo.getField());
                }
                // 5.2.2、将原始值加入到联合签名字符串中
                hmacJoiner.add(Objects.toString(fieldValue, Constants.EMPTY));
            }
        }

        // 6、对联合签名字符串进行签名处理，获取签名值，并进行签名验证
        this.doSignatureVerification(tableInfo, signatureFieldInfos, hmacJoiner, rawObject);

    }

    /**
     * 读取已持久化签名，并与当前字段组合计算出的 HMAC 进行比较。
     *
     * <p>缺少签名、签名不一致时均抛出 MyBatis-Plus 异常；输入不完整时直接跳过，
     * 便于子类复用该模板步骤并定制签名原文的收集过程。</p>
     *
     * @param tableInfo          实体对应的 MyBatis-Plus 表元数据
     * @param signatureFieldInfos 参与签名且已按顺序排列的字段元数据
     * @param hmacJoiner         已按签名顺序拼接的原文
     * @param rawObject          待读取已存签名的实体或 Map
     * @param <T>                保留给子类扩展的结果类型参数
     */
    protected <T> void doSignatureVerification(TableInfo tableInfo, List<TableFieldInfo> signatureFieldInfos, StringJoiner hmacJoiner, Object rawObject) {
        // 1、如果实体类需要进行单表数据存储完整性验证，则对数据表进行签名处理
        if (Objects.isNull(rawObject) || CollectionUtils.isEmpty(signatureFieldInfos) || Objects.isNull(hmacJoiner) || hmacJoiner.length() == 0) {
            return;
        }
        // 6.1、获取之前存储的签名
        Optional<Object> signatureValue = getSignatureReadWriteProvider().readSignature(rawObject, tableInfo);
        // 6.2、如果签名结果存在，则进行签名验证
        if (signatureValue.isPresent()) {
            // 6.2.3、对联合签名字符串进行签名处理，获取签名值
            // 6.2.4、按签名携带的 keyId 验证，支持密钥轮换后的历史数据
            ExceptionUtils.throwMpe(!getEncryptedFieldHandler().verifyHmac(
                            hmacJoiner.toString(), Objects.toString(signatureValue.get(), null)),
                    "表【%s】的数据列【%s】,数据签名不匹配，数据存储完整性验证不通过，请检查数据完整性",
                    tableInfo.getTableName(),
                    signatureFieldInfos.stream().map(TableFieldInfo::getColumn).reduce((a, b) -> a + Constants.COMMA + b).orElse(Constants.EMPTY));
        } else {
            // 6.3、如果签名结果不存在，则抛出异常
            throw ExceptionUtils.mpe("表【%s】的数据列【%s】,原来签名不存在，数据存储完整性验证不通过，请先进行数据签名",
                    tableInfo.getTableName(),
                    signatureFieldInfos.stream().map(TableFieldInfo::getColumn).reduce((a, b) -> a + Constants.COMMA + b).orElse(Constants.EMPTY));
        }
    }

}
