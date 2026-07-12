package com.baomidou.mybatisplus.enhance.interceptor;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.enhance.crypto.annotation.IgnoreEncrypted;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.DefaultDataEncryptionHandler;
import com.baomidou.mybatisplus.enhance.crypto.handler.EncryptedFieldHandler;
import com.baomidou.mybatisplus.enhance.util.EnhanceConstants;
import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
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
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 字段加密内部拦截器。
 * <p>
 * 在查询前加密实体型查询参数，在插入或更新前加密实体与 UpdateWrapper 中的受保护字段。
 * Mapper 方法标注 {@link IgnoreEncrypted} 时跳过处理。由于查询参数会就地修改，调用方不应在
 * 同一请求中重复使用已加密实体。若同时启用表签名，本拦截器必须排在
 * {@link DataSignatureInnerInterceptor} 之前，使签名覆盖实际入库密文。
 */
@Slf4j
public class DataEncryptionInnerInterceptor extends JsqlParserSupport implements InnerInterceptor {

    @Getter
    private final DataEncryptionHandler dataEncryptionHandler;
    /**
     * 是否开启数据加密
     */
    @Getter
    private final boolean encryptSwitch;

    /**
     * 使用默认数据加密处理器并启用加密。
     *
     * @param encryptedFieldHandler 字段级加解密算法
     */
    public DataEncryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler) {
        this(new DefaultDataEncryptionHandler(encryptedFieldHandler), true);
    }

    /**
     * 使用默认数据加密处理器。
     *
     * @param encryptedFieldHandler 字段级加解密算法
     * @param encryptSwitch         是否启用加密
     */
    public DataEncryptionInnerInterceptor(EncryptedFieldHandler encryptedFieldHandler, boolean encryptSwitch) {
        this(new DefaultDataEncryptionHandler(encryptedFieldHandler), encryptSwitch);
    }

    /**
     * 使用自定义数据加密处理器并启用加密。
     *
     * @param dataEncryptionHandler 数据加密处理器
     */
    public DataEncryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler) {
        this(dataEncryptionHandler, true);
    }

    /**
     * 使用自定义数据加密处理器。
     *
     * @param dataEncryptionHandler 数据加密处理器
     * @param encryptSwitch         是否启用加密
     */
    public DataEncryptionInnerInterceptor(DataEncryptionHandler dataEncryptionHandler, boolean encryptSwitch) {
        this.dataEncryptionHandler = dataEncryptionHandler;
        this.encryptSwitch = encryptSwitch;
    }

    /**
     * 如果查询条件是加密数据列，那么要将查询条件进行数据加密。
     * 例如，手机号加密存储后，按手机号查询时，先把要查询的手机号进行加密，再和数据库存储的加密数据进行匹配
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(encryptSwitch, parameterObject)) {
            log.debug("DataEncryptionInnerInterceptor.beforeQuery encryptSwitch is off, return directly.");
            return;
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
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("DataDecryptionInnerInterceptor.afterQuery ClassNotFoundException", e);
        }
        // 2、如果参数
        if (!(parameterObject instanceof Map)) {
            // 对参数进行加密处理
            getDataEncryptionHandler().doEntityEncrypt(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
        // 4、参数去重，否则多次加密会导致查询失败
        Set<?> set = new HashSet<>(paramMap.values());
        // 5、遍历参数，进行加密处理
        for (Object param : set) {
            /*
             *  仅支持类型是自定义Entity的参数，不支持mapper的参数是QueryWrapper、String等，例如：
             *
             *  支持：findList(@Param(value = "query") UserEntity query);
             *  支持：findPage(@Param(value = "query") UserEntity query, Page<UserEntity> page);
             *
             *  不支持：findOne(@Param(value = "mobile") String mobile);
             *  不支持：findList(QueryWrapper wrapper);
             */
            if (Objects.isNull(param) || SimpleTypeRegistry.isSimpleType(param.getClass()) || param instanceof AbstractWrapper) {
                // Wrapper、String类型查询参数，无法获取参数变量上的注解，无法确认是否需要加密，因此不做判断
                continue;
            }
            // 对参数进行加密处理
            getDataEncryptionHandler().doEntityEncrypt(param);
        }
    }

    /**
     * 新增、更新数据时，如果包含隐私数据，则进行加密
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameterObject) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(encryptSwitch, parameterObject)) {
            log.debug("DataEncryptionInnerInterceptor.beforeUpdate encryptSwitch is off, return directly.");
            return;
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
                    return;
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("DataDecryptionInnerInterceptor.afterQuery ClassNotFoundException", e);
        }
        // 2、通过MybatisPlus自带API（save、insert等）新增数据库时
        if (!(parameterObject instanceof Map)) {
            // 对参数进行加密处理
            getDataEncryptionHandler().doEntityEncrypt(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
        Object param;
        // 4、通过MybatisPlus自带API（update、updateById等）修改数据库时
        if (paramMap.containsKey(Constants.ENTITY) && null != (param = paramMap.get(Constants.ENTITY))) {
            // 对参数进行加密处理
            getDataEncryptionHandler().doEntityEncrypt(param);
            return;
        }
        // 5、通过在mapper.xml中自定义API修改数据库时
        if (paramMap.containsKey(EnhanceConstants.CUSTOM_ENTITY) && null != (param = paramMap.get(EnhanceConstants.CUSTOM_ENTITY))) {
            // 对参数进行加密处理
            getDataEncryptionHandler().doEntityEncrypt(param);
            return;
        }
        // 6、通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
        if (paramMap.containsKey(Constants.WRAPPER) && null != (param = paramMap.get(Constants.WRAPPER))) {
            // 6.1、判断是否是UpdateWrapper、LambdaUpdateWrapper类型
            if (param instanceof Update && param instanceof AbstractWrapper) {
                Class<?> entityClass = ms.getParameterMap().getType();
                getDataEncryptionHandler().doWrapperEncrypt(entityClass, (AbstractWrapper<?, ?, ?>) param);
            }
        }
    }

}
