package com.baomidou.mybatisplus.enhance.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import com.baomidou.mybatisplus.enhance.service.IEnhanceService;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import lombok.Getter;
import org.apache.ibatis.binding.MapperMethod;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;

/**
 * {@link IEnhanceService} 的抽象基础实现。
 * <p>
 * 该类把签名和验签流程与 MyBatis-Plus 的批量写入、Wrapper 查询及自定义
 * {@link EnhanceBaseMapper} 能力组合起来。子类只需声明具体 Mapper 和实体类型，并注入
 * {@link DataSignatureHandler}。批量写入和补签方法应在事务中调用，以保证业务数据与签名原子提交。
 *
 * @param <M> 实体对应的增强 Mapper 类型
 * @param <T> MyBatis-Plus 实体类型
 */
public abstract class EnhanceServiceImpl<M extends EnhanceBaseMapper<T>, T> extends ServiceImpl<M, T> implements IEnhanceService<T> {

    /**
     * 数据签名和验签 Handler
     */
    @Getter
    protected final DataSignatureHandler dataSignatureHandler;

    public EnhanceServiceImpl(DataSignatureHandler dataSignatureHandler) {
        this.dataSignatureHandler = dataSignatureHandler;
    }

    /**
     * 获取增强 Mapper，并在注入缺失时快速失败。
     *
     * @return 当前 Service 绑定的增强 Mapper
     */
    @Override
    public M getBaseMapper() {
        Assert.notNull(this.baseMapper, "baseMapper can not be null");
        return this.baseMapper;
    }

    /**
     * 获取具备原始密文查询能力的增强 Mapper。
     *
     * <p>当前泛型已经约束 {@code M extends EnhanceBaseMapper<T>}，因此直接复用经过非空
     * 校验的 {@link #getBaseMapper()}，避免每个业务 Service 重复实现同一适配方法。</p>
     *
     * @return 当前 Service 绑定的增强 Mapper
     */
    @Override
    public M getEnhanceMapper() {
        return getBaseMapper();
    }

    /**
     * 委托签名处理器计算并写回签名值。
     *
     * @param entity 待签名实体
     * @param <RT>   实体类型
     * @return 是否需要持久化新签名
     */
    @Override
    public <RT> boolean doEntitySignature(RT entity) {
        return getDataSignatureHandler().doEntitySignature(entity);
    }

    /**
     * 委托签名处理器验证查询结果的完整性。
     *
     * @param rowObject   待验签结果
     * @param entityClass 实体元数据类型
     * @param <RT>        结果类型
     */
    @Override
    public <RT> void doSignatureVerification(RT rowObject, Class<?> entityClass) {
        getDataSignatureHandler().doSignatureVerification(rowObject, entityClass);
    }

    /**
     * 批量插入
     *
     * @param entityList 待插入实体集合
     * @param batchSize  每批提交的实体数量
     * @return 批量插入是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveBatchSigned(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.INSERT_ONE);
        Set<Serializable> idSet = new HashSet<>(entityList.size());
        try {
            return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
                // 保存数据
                sqlSession.insert(sqlStatement, entity);
                // 获取主键值
                idSet.add(TableFieldHelper.getKeyValue(entity));
            });
        } finally {
            // 批量签名
            this.doSignatureByBatchIds(idSet);
        }
    }

    /**
     * 按主键是否存在批量插入或更新，随后统一刷新受影响行的签名。
     *
     * @param entityList 待保存或更新的实体集合
     * @param batchSize  每批处理的实体数量
     * @return 批量操作是否成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateBatchSigned(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        Set<Serializable> idSet = new HashSet<>(entityList.size());
        try {
            return SqlHelper.saveOrUpdateBatch(getSqlSessionFactory(), this.getMapperClass(), this.log, entityList, batchSize, (sqlSession, entity) -> {
                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                idSet.add((Serializable) idVal);
                return StringUtils.checkValNull(idVal)
                        || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
            }, (sqlSession, entity) -> {
                MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, entity);
                sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                idSet.add((Serializable) idVal);
            });
        } finally {
            // 批量签名
            this.doSignatureByBatchIds(idSet);
        }
    }

    /**
     * 按主键批量更新实体，并在同一事务中刷新表签名。
     *
     * @param entityList 待更新实体集合
     * @param batchSize  每批处理的实体数量
     * @return 批量更新是否成功
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchSignedById(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo, "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty, "error: can not execute. because can not find column for id from entity!");
        Set<Serializable> idSet = new HashSet<>(entityList.size());
        try {
            String sqlStatement = getSqlStatement(SqlMethod.UPDATE_BY_ID);
            return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
                doEntitySignature(entity);
                MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, entity);
                sqlSession.update(sqlStatement, param);

                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                idSet.add((Serializable) idVal);
            });
        } finally {
            // 批量签名
            this.doSignatureByBatchIds(idSet);
        }
    }

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveOrUpdateSigned(T entity) {
        boolean result = getBaseMapper().insertOrUpdate(entity);
        if (result) {
            this.doSignatureById(TableFieldHelper.getKeyValue(entity));
        }
        return result;
    }

    /**
     * 查询最多一条数据并在返回前验签。
     *
     * @param queryWrapper 查询条件
     * @param throwEx      结果多于一条时是否抛出异常
     * @return 验签后的实体，无结果时返回 {@code null}
     */
    @Override
    public T getSignedOne(Wrapper<T> queryWrapper, boolean throwEx) {
        // 1、调用selectOne查询数据
        T entity = getBaseMapper().selectOne(queryWrapper, throwEx);
        if (entity == null) {
            return null;
        }
        // 2、验证签名
        this.doSignatureVerification(entity, entity.getClass());
        // 3、返回数据
        return entity;
    }

    /**
     * 查询最多一条数据、完成验签并以 {@link Optional} 返回。
     *
     * @param queryWrapper 查询条件
     * @param throwEx      结果多于一条时是否抛出异常
     * @return 验签后的可选实体
     */
    @Override
    public Optional<T> getSignedOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        // 1、调用selectOne查询数据
        T entity = getBaseMapper().selectOne(queryWrapper, throwEx);
        if (entity == null) {
            return Optional.empty();
        }
        // 2、验证签名
        this.doSignatureVerification(entity, entity.getClass());
        // 3、返回数据
        return Optional.of(entity);
    }

    /**
     * 查询单条 Map 结果并按 Wrapper 关联实体元数据验签。
     *
     * @param queryWrapper 查询条件，必须能提供实体类型
     * @return 验签后的单条 Map 结果
     */
    @Override
    public Map<String, Object> getSignedMap(Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowMap -> this.doSignatureVerification(rowMap, queryWrapper.getEntity().getClass()));
        }
        return SqlHelper.getObject(log, rtList);
    }

    /**
     * 查询单个对象值，并使用转换函数生成目标类型。
     *
     * @param queryWrapper 查询条件
     * @param mapper       结果转换函数
     * @param <V>          目标类型
     * @return 转换后的单值结果
     */
    @Override
    public <V> V getSignedObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return SqlHelper.getObject(log, listSignedObjs(queryWrapper, mapper));
    }

    /**
     * 根据 ID 对匹配的实体进行表签名
     *
     * @param id 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureById(Serializable id) {
        // 1、根据 ID 查询原始数据
        T entity = getBaseMapper().selectIgnoreDecryptById(id);
        // 2、如果原始数据不为空，则对原始数据进行签名
        if (Objects.nonNull(entity)) {
            // 2.1、对原始数据进行签名
            boolean doUpdate = this.doEntitySignature(entity);
            // 2.2、如果 doUpdate = true, 则更新数据
            if (doUpdate) {
                this.updateById(entity);
            }
        }
    }

    /**
     * 根据 ID 批量对匹配的实体进行表签名
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureByBatchIds(Collection<? extends Serializable> idList) {
        // 1、根据 ID 批量查询原始数据
        List<T> rtList = getEnhanceMapper().selectIgnoreDecryptBatchIds(idList);
        // 2、批量对原始数据进行签名
        this.doSignatureByList(rtList, Constants.DEFAULT_BATCH_SIZE);
    }

    /**
     * 查询（根据 columnMap 条件）匹配的实体进行表签名
     *
     * @param columnMap 表字段 map 对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureByMap(Map<String, Object> columnMap) {
        // 1、根据 columnMap 查询原始数据
        List<T> rtList = getBaseMapper().selectIgnoreDecryptByMap(columnMap);
        // 2、批量对原始数据进行签名
        this.doSignatureByList(rtList, Constants.DEFAULT_BATCH_SIZE);
    }

    /**
     * 根据 Wrapper 条件，对匹配的实体进行表签名
     *
     * @param queryWrappers 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doSignatureByWrappers(List<Wrapper<T>> queryWrappers) {
        for (Wrapper<T> queryWrapper : queryWrappers) {
            // 1、根据 Wrapper 条件查询原始数据
            List<T> rtList = getBaseMapper().selectIgnoreDecryptList(queryWrapper);
            // 2、批量对原始数据进行签名
            this.doSignatureByList(rtList, Constants.DEFAULT_BATCH_SIZE);
        }
    }

    /**
     * 对匹配的实体进行表签名
     *
     * @param entityList 实体对象集合
     * @param batchSize  每次的数量
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doSignatureByList(List<T> entityList, int batchSize) {
        if (CollectionUtils.isEmpty(entityList)) {
            return;
        }
        // 2、对原始数据进行签名
        for (T entity : entityList) {
            // 2.1、对原始数据进行签名
            boolean doUpdate = this.doEntitySignature(entity);
            // 2.2、如果 doUpdate = true, 则更新数据
            if (!doUpdate) {
                entityList.removeIf(rowObject -> TableFieldHelper.getKeyValue(entity).equals(TableFieldHelper.getKeyValue(rowObject)));
            }
        }
        // 3、批量更新数据
        if (CollectionUtils.isNotEmpty(entityList)) {
            this.updateBatchById(entityList, batchSize);
        }
    }

    /**
     * 根据 ID 对匹配的实体进行表签名
     *
     * @param id 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureVerificationById(Serializable id) {
        // 1、根据 ID 查询原始数据
        T entity = getEnhanceMapper().selectIgnoreDecryptById(id);
        // 2、如果原始数据不为空，则对原始数据进行验签
        if (Objects.nonNull(entity)) {
            // 2.1、对原始数据进行验签
            this.doSignatureVerification(entity, entity.getClass());
        }
    }

    /**
     * 根据 ID 批量对匹配的实体进行表签名
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureVerificationByBatchIds(Collection<? extends Serializable> idList) {
        // 1、根据 ID 批量查询原始数据
        List<T> rtList = getEnhanceMapper().selectIgnoreDecryptBatchIds(idList);
        if (CollectionUtils.isNotEmpty(rtList)) {
            // 2、对原始数据进行验签
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
    }

    /**
     * 查询（根据 columnMap 条件）匹配的实体进行表签名
     *
     * @param columnMap 表字段 map 对象
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureVerificationByMap(Map<String, Object> columnMap) {
        // 1、根据 columnMap 查询原始数据
        List<T> rtList = getEnhanceMapper().selectIgnoreDecryptByMap(columnMap);
        if (CollectionUtils.isNotEmpty(rtList)) {
            // 2、对原始数据进行验签
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
    }

    /**
     * 根据 Wrapper 条件，对匹配的实体进行表签名
     *
     * @param queryWrappers 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void doSignatureVerificationByWrappers(List<Wrapper<T>> queryWrappers) {
        for (Wrapper<T> queryWrapper : queryWrappers) {
            // 1、根据 Wrapper 条件查询原始数据
            List<T> rtList = getEnhanceMapper().selectIgnoreDecryptList(queryWrapper);
            if (CollectionUtils.isNotEmpty(rtList)) {
                // 2、对原始数据进行验签
                rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
            }
        }
    }

}
