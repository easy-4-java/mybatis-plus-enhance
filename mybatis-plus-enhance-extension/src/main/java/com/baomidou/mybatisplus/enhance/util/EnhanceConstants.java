package com.baomidou.mybatisplus.enhance.util;

import com.baomidou.mybatisplus.core.toolkit.Constants;

/**
 * 增强拦截器与自定义 Mapper XML 之间共享的参数名契约。
 */
public interface EnhanceConstants extends Constants {

    /**
     * 自定义 SQL 中实体参数的默认名称。
     */
    String CUSTOM_ENTITY = "entity";

}
