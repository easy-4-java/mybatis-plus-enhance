package io.ddd4j.alignment.mapper;

import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import io.ddd4j.alignment.entity.SharedUserEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * Plus 端 Mapper：继承共享 {@link SharedUserMapper} + Plus 版 {@link EnhanceBaseMapper}。
 *
 * <p>SQL 由 {@code EnhanceBaseMapper} 配合 mapper XML 自动注入。</p>
 */
@Mapper
public interface PlusUserMapper extends SharedUserMapper, EnhanceBaseMapper<SharedUserEntity> {
}
