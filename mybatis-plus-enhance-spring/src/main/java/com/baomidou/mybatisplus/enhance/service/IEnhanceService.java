package com.baomidou.mybatisplus.enhance.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import com.baomidou.mybatisplus.enhance.context.SignatureUpdateContext;
import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;
import com.baomidou.mybatisplus.enhance.util.TableFieldHelper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.session.ResultHandler;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;

/**
 * 支持表级数据签名与验签的 MyBatis-Plus Service 契约。
 * <p>
 * 以 {@link IService} 为基础，为写入、单条查询、列表查询和分页查询提供带签名语义的对等 API。
 * “Signed”方法会在持久化后生成或更新表签名，并在读取后执行验签。
 * 验签失败的处理策略由 {@code DataSignatureHandler} 实现决定。
 *
 * @param <T> MyBatis-Plus 实体类型
 */
public interface IEnhanceService<T> extends IService<T> {

    /**
     * 获取 Signed API 用于解析签名元数据的实体类型。
     *
     * <p>该端口避免从 Wrapper 实体或查询投影推断类型。常规 MyBatis-Plus Service 可以复用
     * 官方实体类型解析，代理或多态 Service 可以显式覆盖。</p>
     *
     * @return 当前 Service 管理的实体类型
     */
    default Class<T> getSignedEntityClass() {
        return getEntityClass();
    }

    /**
     * 计算并写回实体的表级签名。
     *
     * @param entity 待签名实体
     * @param <RT>   实体类型
     * @return 签名值发生变化、需要持久化时返回 {@code true}
     */
    <RT> boolean doEntitySignature(RT entity);

    /**
     * 对单个查询结果执行表级验签。
     *
     * @param rowObject   待验签的查询结果
     * @param entityClass 用于解析签名注解和表元数据的实体类型
     * @param <RT>        结果类型
     */
    <RT> void doSignatureVerification(RT rowObject, Class<?> entityClass);

    /**
     * 插入一条记录（选择字段，策略插入）
     *
     * @param entity 实体对象
     * @return 插入并完成补签时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveSigned(T entity) {
        boolean result = SqlHelper.retBool(getBaseMapper().insert(entity));
        if (result) {
            this.doSignatureById(TableFieldHelper.getKeyValue(entity));
        }
        return result;
    }

    /**
     * 插入（批量）
     *
     * @param entityList 实体对象集合
     * @return 全部实体插入并完成补签时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveBatchSigned(Collection<T> entityList) {
        return saveBatchSigned(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 插入（批量）
     *
     * @param entityList 实体对象集合
     * @param batchSize  插入批次数量
     * @return 全部实体插入并完成补签时返回 {@code true}
     */
    boolean saveBatchSigned(Collection<T> entityList, int batchSize);

    /**
     * 批量修改插入
     *
     * @param entityList 实体对象集合
     * @return 批量保存或更新并完成补签时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean saveOrUpdateBatchSigned(Collection<T> entityList) {
        return saveOrUpdateBatchSigned(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 批量修改插入
     *
     * @param entityList 实体对象集合
     * @param batchSize  每次的数量
     * @return 批量保存或更新并完成补签时返回 {@code true}
     */
    boolean saveOrUpdateBatchSigned(Collection<T> entityList, int batchSize);

    /**
     * 根据 ID 选择修改
     *
     * @param entity 实体对象
     * @return 更新并完成补签时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean updateSignedById(T entity) {
        boolean result;
        try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                SignatureUpdateStrategy.DEFERRED_RESIGN)) {
            result = SqlHelper.retBool(getBaseMapper().updateById(entity));
        }
        if (result) {
            this.doSignatureById(TableFieldHelper.getKeyValue(entity));
        }
        return result;
    }

    /**
     * 根据ID 批量更新
     *
     * @param entityList 实体对象集合
     * @return 全部实体更新并完成补签时返回 {@code true}
     */
    @Transactional(rollbackFor = Exception.class)
    default boolean updateBatchSignedById(Collection<T> entityList) {
        return updateBatchSignedById(entityList, DEFAULT_BATCH_SIZE);
    }

    /**
     * 根据ID 批量更新
     *
     * @param entityList 实体对象集合
     * @param batchSize  更新批次数量
     * @return 全部实体更新并完成补签时返回 {@code true}
     */
    boolean updateBatchSignedById(Collection<T> entityList, int batchSize);

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return 保存或更新并完成补签时返回 {@code true}
     */
    boolean saveOrUpdateSigned(T entity);

    /**
     * 根据 ID 查询 签名验证通过的数据
     *
     * @param id 主键ID
     * @return 验签通过的实体；记录不存在时返回 {@code null}
     */
    default T getSignedById(Serializable id) {
        // 1、调用selectById查询数据
        T entity = getBaseMapper().selectById(id);
        if (entity == null) {
            return null;
        }
        // 2、验证签名
        this.doSignatureVerification(entity, entity.getClass());
        // 3、返回数据
        return entity;
    }

    /**
     * 根据 ID 查询，返回一个Option对象
     *
     * @param id 主键ID
     * @return {@link Optional}
     */
    default Optional<T> getSignedOptById(Serializable id) {
        return Optional.ofNullable(getSignedById(id));
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList 主键ID列表
     * @return 已逐条完成验签的实体列表
     */
    default List<T> listSignedByIds(Collection<? extends Serializable> idList) {
        // 1、调用selectBatchIds查询数据
        List<T> rtList = getBaseMapper().selectBatchIds(idList);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 查询（根据ID 批量查询）
     *
     * @param idList        idList 主键ID列表(不能为 null 以及 empty)
     * @param resultHandler resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedByIds(Collection<? extends Serializable> idList, ResultHandler<T> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
            // 3、调用结果处理器
            resultHandler.handleResult(context);
        };
        // 4、调用selectBatchIds查询数据
        getBaseMapper().selectBatchIds(idList, wapperHandler);
    }

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap 表字段 map 对象
     * @return 已逐条完成验签的实体列表
     */
    default List<T> listSignedByMap(Map<String, Object> columnMap) {
        // 1、调用selectByMap查询数据
        List<T> rtList = getBaseMapper().selectByMap(columnMap);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap     表字段 map 对象
     * @param resultHandler resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedByMap(Map<String, Object> columnMap, ResultHandler<T> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
            // 3、调用结果处理器
            resultHandler.handleResult(context);
        };
        // 4、调用selectByMap查询数据
        getBaseMapper().selectByMap(columnMap, wapperHandler);
    }

    /**
     * 根据 Wrapper，查询一条记录 <br>
     * <p>结果集，如果是多个会抛出异常，随机取一条加上限制条件 wrapper.last("LIMIT 1")</p>
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 验签通过的单条实体；无结果时返回 {@code null}
     */
    default T getSignedOne(Wrapper<T> queryWrapper) {
        return getSignedOne(queryWrapper, true);
    }

    /**
     * 根据 Wrapper，查询一条记录 <br>
     * <p>结果集，如果是多个会抛出异常，随机取一条加上限制条件 wrapper.last("LIMIT 1")</p>
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return {@link Optional} 返回一个Optional对象
     */
    default Optional<T> getSignedOneOpt(Wrapper<T> queryWrapper) {
        return getSignedOneOpt(queryWrapper, true);
    }

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param throwEx      有多个 result 是否抛出异常
     * @return 验签通过的单条实体；无结果时返回 {@code null}
     */
    T getSignedOne(Wrapper<T> queryWrapper, boolean throwEx);

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param throwEx      有多个 result 是否抛出异常
     * @return {@link Optional} 返回一个Optional对象
     */
    Optional<T> getSignedOneOpt(Wrapper<T> queryWrapper, boolean throwEx);

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 验签通过的字段 Map；无结果时返回 {@code null}
     */
    Map<String, Object> getSignedMap(Wrapper<T> queryWrapper);

    /**
     * 查询列表
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 已逐条完成验签的实体列表
     */
    default List<T> listSigned(Wrapper<T> queryWrapper) {
        // 1、调用selectList查询数据
        List<T> rtList = getBaseMapper().selectList(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper  实体对象封装操作类（可以为 null）
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSigned(Wrapper<T> queryWrapper, ResultHandler<T> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
            // 3、调用结果处理器
            resultHandler.handleResult(context);
        };
        // 4、调用selectList查询数据
        getBaseMapper().selectList(queryWrapper, wapperHandler);
    }

    /**
     * 查询列表
     *
     * @param page         分页条件
     * @param queryWrapper queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 列表数据
     * @since 3.5.3.2
     */
    default List<T> listSigned(IPage<T> page, Wrapper<T> queryWrapper) {
        // 1、调用selectList查询数据
        List<T> rtList = getBaseMapper().selectList(page, queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return rtList;
    }

    /**
     * 根据 entity 条件，查询全部记录（并翻页）
     *
     * @param page          分页查询条件
     * @param queryWrapper  实体对象封装操作类（可以为 null）
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSigned(IPage<T> page, Wrapper<T> queryWrapper, ResultHandler<T> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<T> wapperHandler = context -> {
            // 2、验证签名
            this.doSignatureVerification(context.getResultObject(), context.getResultObject().getClass());
            // 3、调用结果处理器
            resultHandler.handleResult(context);
        };
        // 4、调用selectList查询数据
        getBaseMapper().selectList(page, queryWrapper, wapperHandler);
    }

    /**
     * 查询所有
     *
     * @return 已逐条完成验签的全部实体
     * @see Wrappers#emptyWrapper()
     */
    default List<T> listSigned() {
        return listSigned(Wrappers.emptyWrapper());
    }

    /**
     * 分页查询单表数据
     *
     * @param page 分页条件
     * @return 列表数据
     * @since 3.5.3.2
     */
    default List<T> listSigned(IPage<T> page) {
        return listSigned(page, Wrappers.emptyWrapper());
    }

    /**
     * 翻页查询
     *
     * @param page         翻页对象
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param <E>          分页对象类型
     * @return 已填充并逐条完成验签的分页对象
     */
    default <E extends IPage<T>> E pageSigned(E page, Wrapper<T> queryWrapper) {
        // 1、查询数据
        page.setRecords(getBaseMapper().selectList(page, queryWrapper));
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(page.getRecords())) {
            page.getRecords().forEach(rowObject -> this.doSignatureVerification(rowObject, rowObject.getClass()));
        }
        return page;
    }

    /**
     * 无条件翻页查询
     *
     * @param page 翻页对象
     * @param <E>  分页对象类型
     * @return 已填充并逐条完成验签的分页对象
     * @see Wrappers#emptyWrapper()
     */
    default <E extends IPage<T>> E pageSigned(E page) {
        return pageSigned(page, Wrappers.emptyWrapper());
    }

    /**
     * 查询列表
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 已逐条完成验签的字段 Map 列表
     */
    default List<Map<String, Object>> listSignedMaps(Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowMap -> this.doSignatureVerification(rowMap, getSignedEntityClass()));
        }
        return rtList;
    }

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper  实体对象封装操作类
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedMaps(Wrapper<T> queryWrapper, ResultHandler<Map<String, Object>> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<Map<String, Object>> wapperHandler = context -> {
            // 2、验证签名
            this.doSignatureVerification(context.getResultObject(), getSignedEntityClass());
            // 3、调用结果处理器
            resultHandler.handleResult(context);
        };
        // 4、调用selectMaps查询数据
        getBaseMapper().selectMaps(queryWrapper, wapperHandler);
    }

    /**
     * 查询列表
     *
     * @param page         分页条件
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 列表数据
     * @since 3.5.3.2
     */
    default List<Map<String, Object>> listSignedMaps(IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper) {
        // 1、调用selectMaps查询数据
        List<Map<String, Object>> rtList = getBaseMapper().selectMaps(page, queryWrapper);
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(rtList)) {
            rtList.forEach(rowMap -> this.doSignatureVerification(rowMap, getSignedEntityClass()));
        }
        return rtList;
    }

    /**
     * 根据 Wrapper 条件，查询全部记录（并翻页）
     *
     * @param page          分页查询条件
     * @param queryWrapper  实体对象封装操作类
     * @param resultHandler 结果处理器 {@link ResultHandler}
     * @since 3.5.4
     */
    default void listSignedMaps(IPage<? extends Map<String, Object>> page, Wrapper<T> queryWrapper, ResultHandler<Map<String, Object>> resultHandler) {
        // 1、封装结果处理器
        ResultHandler<Map<String, Object>> wapperHandler = context -> {
            // 2、验证签名
            this.doSignatureVerification(context.getResultObject(), getSignedEntityClass());
            // 3、调用结果处理器
            resultHandler.handleResult(context);
        };
        // 4、调用selectMaps查询数据
        getBaseMapper().selectMaps(page, queryWrapper, wapperHandler);
    }

    /**
     * 查询所有列表
     *
     * @return 已逐条完成验签的全部字段 Map
     * @see Wrappers#emptyWrapper()
     */
    default List<Map<String, Object>> listSignedMaps() {
        return listSignedMaps(Wrappers.emptyWrapper());
    }

    /**
     * 查询列表
     *
     * @param page 分页条件
     * @return 已逐条完成验签的字段 Map 列表
     * @see Wrappers#emptyWrapper()
     */
    default List<Map<String, Object>> listSignedMaps(IPage<? extends Map<String, Object>> page) {
        return listSignedMaps(page, Wrappers.emptyWrapper());
    }

    /**
     * 翻页查询
     *
     * @param page         翻页对象
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param <E>          Map 分页对象类型
     * @return 已填充并逐条完成验签的 Map 分页对象
     */
    default <E extends IPage<Map<String, Object>>> E pageSignedMaps(E page, Wrapper<T> queryWrapper) {
        // 1、查询数据
        page.setRecords(getBaseMapper().selectMaps(page, queryWrapper));
        // 2、验证签名
        if (CollectionUtils.isNotEmpty(page.getRecords())) {
            page.getRecords().forEach(rowMap -> this.doSignatureVerification(rowMap, getSignedEntityClass()));
        }
        return page;
    }

    /**
     * 无条件翻页查询
     *
     * @param page 翻页对象
     * @param <E>  Map 分页对象类型
     * @return 已填充并逐条完成验签的 Map 分页对象
     * @see Wrappers#emptyWrapper()
     */
    default <E extends IPage<Map<String, Object>>> E pageSignedMaps(E page) {
        return pageSignedMaps(page, Wrappers.emptyWrapper());
    }

    /**
     * 根据 ID 对匹配的实体进行表签名
     *
     * @param id 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureById(Serializable id);

    /**
     * 根据 ID 批量对匹配的实体进行表签名
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureByBatchIds(Collection<? extends Serializable> idList);

    /**
     * 查询（根据 columnMap 条件）匹配的实体进行表签名
     *
     * @param columnMap 表字段 map 对象
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureByMap(Map<String, Object> columnMap);

    /**
     * 根据 Wrapper 条件，对匹配的实体进行表签名
     *
     * @param queryWrappers 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureByWrappers(List<Wrapper<T>> queryWrappers);

    /**
     * 对匹配的实体进行表签名
     *
     * @param entityList 实体对象集合
     * @param batchSize  每次的数量
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureByList(List<T> entityList, int batchSize);

    /**
     * 根据 ID 对匹配的实体进行表签名
     *
     * @param id 主键ID
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureVerificationById(Serializable id);

    /**
     * 根据 ID 批量对匹配的实体进行表签名
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureVerificationByBatchIds(Collection<? extends Serializable> idList);

    /**
     * 查询（根据 columnMap 条件）匹配的实体进行表签名
     *
     * @param columnMap 表字段 map 对象
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureVerificationByMap(Map<String, Object> columnMap);

    /**
     * 根据 Wrapper 条件，对匹配的实体进行表签名
     *
     * @param queryWrappers 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     */
    @Transactional(rollbackFor = Exception.class)
    void doSignatureVerificationByWrappers(List<Wrapper<T>> queryWrappers);

    /**
     * 获取对应 entity 的 BaseMapper
     *
     * @return BaseMapper
     */
    EnhanceBaseMapper<T> getEnhanceMapper();

}
