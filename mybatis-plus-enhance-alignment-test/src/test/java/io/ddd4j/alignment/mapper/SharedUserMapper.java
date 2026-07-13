package io.ddd4j.alignment.mapper;

import io.ddd4j.alignment.entity.SharedUserEntity;

import java.util.List;

/**
 * 共享 Mapper 接口：不引用任何框架特定类。
 *
 * <p>两侧各自的实现（{@code PlusUserMapper} / {@code EnhanceUserMapper}）继承此接口
 * 并附加框架特定的 base mapper（{@code EnhanceBaseMapper<SharedUserEntity>} /
 * {@code EnhanceMapper<SharedUserEntity>}）。</p>
 */
public interface SharedUserMapper {

    // insert 由框架基类（EnhanceBaseMapper / EnhanceMapper）提供，无需在共享接口中重复声明

    /**
     * 清空表（测试用）。
     */
    void deleteAll();

    SharedUserEntity selectById(Long id);

    List<SharedUserEntity> selectList();

    SharedUserEntity selectIgnoreDecryptById(Long id);

    List<SharedUserEntity> selectBatchIds(List<Long> idList);
}
