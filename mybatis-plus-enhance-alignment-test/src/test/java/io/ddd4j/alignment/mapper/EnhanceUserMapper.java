package io.ddd4j.alignment.mapper;

import io.ddd4j.alignment.entity.SharedUserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.enhance.mapper.EnhanceMapper;

/**
 * non-Plus 端 Mapper：继承共享 {@link SharedUserMapper} + non-Plus 版 {@link EnhanceMapper}。
 *
 * <p>SQL 必须在 mapper XML 中显式声明（non-Plus 版无自动 SQL 注入）。</p>
 */
@Mapper
public interface EnhanceUserMapper extends SharedUserMapper, EnhanceMapper<SharedUserEntity> {
}
