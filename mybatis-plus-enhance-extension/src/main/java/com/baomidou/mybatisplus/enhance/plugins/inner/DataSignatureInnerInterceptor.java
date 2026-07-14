package com.baomidou.mybatisplus.enhance.plugins.inner;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.update.Update;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.AnnotationUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.enhance.context.SignatureUpdateContext;
import com.baomidou.mybatisplus.enhance.context.SignatureVerificationContext;
import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.util.EnhanceConstants;
import com.baomidou.mybatisplus.enhance.util.ParameterUtils;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.SimpleTypeRegistry;

import java.sql.SQLException;
import java.util.*;
import org.apache.ibatis.enhance.annotation.crypto.TableSignature;

/**
 * 表级数据签名与验签拦截器。
 * <p>
 * 写入前根据参数生成签名，查询后可选地验证结果完整性。若签名覆盖加密后的字段，
 * 写入顺序必须是 {@link DataEncryptionInnerInterceptor} 后接本拦截器；读取顺序必须先验签、
 * 再由 {@link DataDecryptionInnerInterceptor} 解密。签名和验签开关相互独立，便于渐进式迁移历史数据。
 */
@Slf4j
public class DataSignatureInnerInterceptor extends JsqlParserSupport implements EnhanceInnerInterceptor {

    @Override
    public EnhancePhase phase() {
        return EnhancePhase.DATA_SIGNATURE;
    }

    /**
     * 对写入参数生成签名并对查询结果执行完整性验证的处理器。
     */
    @Getter
    private final DataSignatureHandler dataSignatureHandler;
    /**
     * 是否在 INSERT、UPDATE 执行前生成数据签名。
     */
    @Getter
    private final boolean signSwitch;
    /**
     * 是否在查询结果映射完成后执行签名验证。
     */
    @Getter
    private final boolean signVerify;

    /**
     * 启用写入签名，关闭查询验签。
     *
     * @param dataSignatureHandler 签名与验签处理器
     */
    public DataSignatureInnerInterceptor(DataSignatureHandler dataSignatureHandler) {
        this(dataSignatureHandler, true, false);
    }

    /**
     * 配置写入签名开关，关闭查询验签。
     *
     * @param dataSignatureHandler 签名与验签处理器
     * @param signSwitch           是否生成签名
     */
    public DataSignatureInnerInterceptor(DataSignatureHandler dataSignatureHandler, boolean signSwitch) {
        this(dataSignatureHandler, signSwitch, false);
    }

    /**
     * 配置独立的签名和验签开关。
     *
     * @param dataSignatureHandler 签名与验签处理器
     * @param signSwitch           是否生成签名
     * @param signVerify           是否验证查询结果
     */
    public DataSignatureInnerInterceptor(DataSignatureHandler dataSignatureHandler, boolean signSwitch, boolean signVerify) {
        this.dataSignatureHandler = dataSignatureHandler;
        this.signSwitch = signSwitch;
        this.signVerify = signVerify;
    }

    /**
     * 如果查询条件是签名数据列，那么要将查询条件进行数据签名。
     * 例如，手机号签名存储后，按手机号查询时，先把要查询的手机号进行签名，再和数据库存储的签名数据进行匹配
     */
    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(signSwitch, parameterObject)) {
            return;
        }
        // 2、如果参数
        if (!(parameterObject instanceof Map)) {
            return;
        }
        // 3、Map类型参数
        Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
        // 4、参数去重，否则多次签名会导致查询失败
        Set<?> set = new HashSet<>(paramMap.values());
        // 5、遍历参数，进行签名处理
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
                // Wrapper、String类型查询参数，无法获取参数变量上的注解，无法确认是否需要签名，因此不做判断
                continue;
            }
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(param);
        }
    }

    /**
     * 新增、更新数据时，如果包含隐私数据，则进行签名
     */
    @Override
    public void beforeUpdate(Executor executor, MappedStatement mappedStatement, Object parameterObject) throws SQLException {
        // 1、如果参数为空，或者参数是简单类型，或全局未启用 则直接返回
        if (ParameterUtils.isSwitchOff(signSwitch, parameterObject)) {
            return;
        }
        SignatureUpdateStrategy updateStrategy = SignatureUpdateContext.current();
        if (mappedStatement.getSqlCommandType() == SqlCommandType.UPDATE) {
            if (updateStrategy == SignatureUpdateStrategy.DEFERRED_RESIGN
                    || updateStrategy == SignatureUpdateStrategy.SIGNATURE_ONLY) {
                return;
            }
            if (updateStrategy == SignatureUpdateStrategy.REJECT_PARTIAL) {
                rejectSignedPartialUpdate(mappedStatement, parameterObject);
                return;
            }
        }
        // 2、通过MybatisPlus自带API（save、insert等）新增数据库时
        if (!(parameterObject instanceof Map)) {
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(parameterObject);
            return;
        }
        // 3、Map类型参数
        Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
        Object param;
        // 4、通过MybatisPlus自带API（update、updateById等）修改数据库时
        if (paramMap.containsKey(Constants.ENTITY) && null != (param = paramMap.get(Constants.ENTITY))) {
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(param);
            return;
        }
        // 5、通过在mapper.xml中自定义API修改数据库时
        if (paramMap.containsKey(EnhanceConstants.CUSTOM_ENTITY) && null != (param = paramMap.get(EnhanceConstants.CUSTOM_ENTITY))) {
            // 对参数进行签名处理
            getDataSignatureHandler().doEntitySignature(param);
            return;
        }
        // 6、通过UpdateWrapper、LambdaUpdateWrapper修改数据库时
        if (paramMap.containsKey(Constants.WRAPPER) && null != (param = paramMap.get(Constants.WRAPPER))) {
            // 6.1、判断是否是UpdateWrapper、LambdaUpdateWrapper类型
            if (param instanceof Update && param instanceof AbstractWrapper) {
                Class<?> entityClass = resolveEntityClass(mappedStatement, (AbstractWrapper<?, ?, ?>) param);
                getDataSignatureHandler().doWrapperSignature(entityClass, (AbstractWrapper<?, ?, ?>) param);
            }
        }
    }

    /**
     * 默认拒绝签名表的部分更新，防止使用不完整参数生成错误整行签名。
     */
    private void rejectSignedPartialUpdate(MappedStatement mappedStatement, Object parameterObject) {
        Class<?> entityClass = resolveEntityClass(mappedStatement, parameterObject);
        if (Objects.nonNull(entityClass)
                && Objects.nonNull(AnnotationUtils.findFirstAnnotation(TableSignature.class, entityClass))) {
            throw ExceptionUtils.mpe("签名表【%s】的 UPDATE 必须显式使用 FULL_ROW 或 DEFERRED_RESIGN 策略",
                    entityClass.getName());
        }
    }

    /**
     * 从实体、Wrapper 或 Mapper 泛型解析当前语句对应的实体类型。
     */
    private Class<?> resolveEntityClass(MappedStatement mappedStatement, Object parameterObject) {
        Object candidate = parameterObject;
        if (parameterObject instanceof Map) {
            Map<?, ?> parameterMap = (Map<?, ?>) parameterObject;
            if (parameterMap.containsKey(Constants.ENTITY)) {
                candidate = parameterMap.get(Constants.ENTITY);
            } else if (parameterMap.containsKey(Constants.WRAPPER)) {
                candidate = parameterMap.get(Constants.WRAPPER);
            }
        }
        if (candidate instanceof AbstractWrapper) {
            Class<?> wrapperType = ((AbstractWrapper<?, ?, ?>) candidate).getEntityClass();
            if (Objects.nonNull(wrapperType)) {
                return wrapperType;
            }
        } else if (Objects.nonNull(candidate) && !(candidate instanceof Map)) {
            return candidate.getClass();
        }
        String statementId = mappedStatement.getId();
        int separator = statementId.lastIndexOf('.');
        if (separator <= 0) {
            return null;
        }
        try {
            Class<?> mapperClass = Class.forName(statementId.substring(0, separator));
            Class<?>[] typeArguments = GenericTypeUtils.resolveTypeArguments(mapperClass, BaseMapper.class);
            return Objects.isNull(typeArguments) || typeArguments.length == 0 ? null : typeArguments[0];
        } catch (ClassNotFoundException exception) {
            throw ExceptionUtils.mpe("无法解析 Mapper 实体类型: %s", exception, statementId);
        }
    }

    /**
     * 对查询结果逐条验签，简单类型和空值不参与验签。
     *
     * @param rtList MyBatis 已映射的结果列表
     */
    @Override
    public List<Object> afterQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds,
                                   ResultHandler<?> resultHandler, BoundSql boundSql,
                                   List<Object> rtList) throws SQLException {
        // 1、如果参数为空，或者参数元素为0，或全局未启用 则直接返回
        if (SignatureVerificationContext.isIgnored() || ParameterUtils.isSwitchOff(signVerify, rtList)) {
            return rtList;
        }
        for (Object rawObject : rtList) {
            if (Objects.isNull(rawObject) || SimpleTypeRegistry.isSimpleType(rawObject.getClass())) {
                continue;
            }
            getDataSignatureHandler().doSignatureVerification(rawObject, rawObject.getClass());
        }
        return rtList;
    }

}
