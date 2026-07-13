package com.baomidou.mybatisplus.enhance.plugins.inner;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import org.apache.ibatis.enhance.annotation.crypto.IgnoreEncrypted;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.result.ReflectionResultObjectCopier;
import com.baomidou.mybatisplus.enhance.result.ResultObjectCopier;
import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * 查询结果字段解密拦截器。
 * <p>
 * MyBatis 完成结果映射后，本拦截器逐条处理非简单类型对象；Mapper 方法标注
 * {@link IgnoreEncrypted} 时保留原始密文。若签名是基于密文计算，验签拦截器应排在本拦截器之前。
 * 解密会就地修改结果对象，因此不应对同一结果重复执行。
 */
@Slf4j
public class DataDecryptionInnerInterceptor implements EnhanceInnerInterceptor {

    @Override
    public EnhancePhase phase() {
        return EnhancePhase.RESULT_DECRYPTION;
    }

    /**
     * 对查询结果执行字段解密的处理器。
     */
    @Getter
    private final DataEncryptionHandler dataEncryptionHandler;

    /**
     * 全局查询结果解密开关。
     */
    @Getter
    private final boolean decryptSwitch;

    /**
     * 在解密前复制缓存结果对象的策略。
     */
    @Getter
    private final ResultObjectCopier resultObjectCopier;

    /**
     * 使用默认数据加密处理器并启用解密。
     *
     * @param encryptedFieldHandler 字段级加解密算法
     */
    public DataDecryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this(new DefaultDataEncryptionHandler(encryptedFieldHandler), true);
    }

    /**
     * 使用默认数据加密处理器。
     *
     * @param encryptedFieldHandler 字段级加解密算法
     * @param decryptSwitch         是否启用解密
     */
    public DataDecryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler, boolean decryptSwitch) {
        this(new DefaultDataEncryptionHandler(encryptedFieldHandler), decryptSwitch);
    }

    /**
     * 使用自定义处理器构造拦截器，默认不启用解密。
     *
     * @param dataEncryptionHandler 数据加密处理器
     */
    public DataDecryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler) {
        this(dataEncryptionHandler, false);
    }

    /**
     * 使用自定义处理器和显式开关构造拦截器。
     *
     * @param dataEncryptionHandler 数据加密处理器
     * @param decryptSwitch         是否启用解密
     */
    public DataDecryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler, boolean decryptSwitch) {
        this(dataEncryptionHandler, decryptSwitch, new ReflectionResultObjectCopier());
    }

    /**
     * 使用自定义结果复制策略创建查询结果解密拦截器。
     *
     * <p>若应用已禁用 MyBatis 本地缓存，可通过 {@code ResultObjectCopier.noCopy()} 跳过
     * 对象拷贝，直接就地解密以节省内存：</p>
     * <pre>{@code
     * new DataDecryptionInnerInterceptor(handler, true, ResultObjectCopier.noCopy());
     * }</pre>
     *
     * @param dataEncryptionHandler 数据加密处理器
     * @param decryptSwitch         是否启用解密
     * @param resultObjectCopier    查询结果复制策略
     */
    public DataDecryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler, boolean decryptSwitch,
                                          ResultObjectCopier resultObjectCopier) {
        this.decryptSwitch = decryptSwitch;
        this.dataEncryptionHandler = Objects.requireNonNull(dataEncryptionHandler,
                "dataEncryptionHandler must not be null");
        this.resultObjectCopier = Objects.requireNonNull(resultObjectCopier,
                "resultObjectCopier must not be null");
    }

    /**
     * 在查询完成后解密结果集中的受保护字段。
     *
     * @param rtList MyBatis 已映射的结果列表
     */
    @Override
    public List<Object> afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                                   ResultHandler<?> resultHandler, BoundSql boundSql,
                                   List<Object> rtList) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(decryptSwitch, rtList)) {
            log.debug("DataDecryptionInnerInterceptor.afterQuery decryptSwitch is off, return directly.");
            return rtList;
        }
        // 2、检查Mapper接口类和方法名
        try {
            String mappedStatementId = ms.getId();
            Class<?> mapperClazz = Class.forName(mappedStatementId.substring(0, mappedStatementId.lastIndexOf(".")));
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(".") + 1);
            Method method = ReflectUtil.getMethodByName(mapperClazz, methodName);
            if (Objects.nonNull(method)) {
                // 获取 @EncryptedTable 注解
                IgnoreEncrypted ignoreEncrypted = AnnotationUtils.findFirstAnnotation(IgnoreEncrypted.class, method);
                if (ObjectUtils.isNotEmpty(ignoreEncrypted)) {
                    log.debug("mappedStatementId：{}, ignoreEncrypted is on, return directly.", mappedStatementId);
                    return rtList;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("DataDecryptionInnerInterceptor.afterQuery ClassNotFoundException", e);
        }
        // 3、对查询结果进行解密
        List<Object> detachedResults = new ArrayList<>(rtList.size());
        for (Object rawObject : rtList) {
            if (Objects.isNull(rawObject) || SimpleTypeRegistry.isSimpleType(rawObject.getClass())) {
                detachedResults.add(rawObject);
                continue;
            }
            Object detachedObject = getResultObjectCopier().copy(rawObject);
            // 逐一解密
            getDataEncryptionHandler().doRawObjectDecrypt(detachedObject, rawObject.getClass());
            detachedResults.add(detachedObject);
        }
        return detachedResults;
    }

}
