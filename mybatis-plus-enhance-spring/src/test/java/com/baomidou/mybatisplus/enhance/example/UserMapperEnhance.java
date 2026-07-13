package com.baomidou.mybatisplus.enhance.example;

import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapperEnhance extends EnhanceBaseMapper<UserEntity> {

}
