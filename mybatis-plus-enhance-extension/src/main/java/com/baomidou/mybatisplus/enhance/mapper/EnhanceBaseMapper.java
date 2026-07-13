package com.baomidou.mybatisplus.enhance.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.mybatis.enhance.annotation.crypto.IgnoreEncrypted;
import org.apache.ibatis.annotations.Param;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * MyBatis-Plus 增强 Mapper。
 *
 * <p>在 {@link BaseMapper} 基础上增加“查询原始密文、不执行结果解密”的方法。
 * 对应 MappedStatement 由 {@code EnhanceSqlInjector} 注入。</p>
 *
 * @param <T> 实体类型
 */
public interface EnhanceBaseMapper<T> extends BaseMapper<T> {

    /**
     * 根据ID 查询一条数据
     *
     * @param id 主键ID
     * @return 实体对象
     */
    @IgnoreEncrypted
    T selectIgnoreDecryptById(Serializable id);

    /**
     * 根据ID集合，批量查询数据
     *
     * @param idList 主键ID列表(不能为 null 以及 empty)
     * @return 实体对象集合
     */
    @IgnoreEncrypted
    List<T> selectIgnoreDecryptBatchIds(@Param(Constants.COLL) Collection<? extends Serializable> idList);

    /**
     * 根据 Wrapper 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类
     * @return 原始列值 Map 列表
     */
    @IgnoreEncrypted
    List<Map<String, Object>> selectIgnoreDecryptMaps(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 查询（根据 columnMap 条件）
     *
     * @param columnMap 表字段 map 对象
     * @return 不执行解密的实体列表
     */
    default List<T> selectIgnoreDecryptByMap(Map<String, Object> columnMap) {
        return this.selectIgnoreDecryptList(Wrappers.<T>query().allEq(columnMap));
    }

    /**
     * 根据 entity 条件，查询全部记录
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @return 不执行解密的实体列表
     */
    @IgnoreEncrypted
    List<T> selectIgnoreDecryptList(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 根据 Wrapper 条件，查询全部记录
     * <p>注意： 只返回第一个字段的值</p>
     *
     * @param queryWrapper 实体对象封装操作类（可以为 null）
     * @param <E>          首列值类型
     * @return 每行第一列的原始值列表
     */
    @IgnoreEncrypted
    <E> List<E> selectIgnoreDecryptObjs(@Param(Constants.WRAPPER) Wrapper<T> queryWrapper);

    /**
     * 按主键仅更新实体的表签名存储列。
     *
     * <p>该方法由框架补签流程调用，不更新其他业务字段，并通过
     * {@link IgnoreEncrypted} 避免原始密文被再次加密。</p>
     *
     * @param entity 包含主键和最新签名值的实体
     * @return 受影响行数
     */
    @IgnoreEncrypted
    int updateSignatureById(@Param(Constants.ENTITY) T entity);

}
