/**
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

/**
 * 数据库整型布尔值枚举。
 *
 * <p>通过 MyBatis-Plus {@link IEnum} 将 {@code false/true} 分别映射为 {@code 0/1}。</p>
 */
@Getter
public enum BooleanEnum implements IEnum<Integer> {

    /**
     * 数据库存储值 {@code 0}，表示逻辑假。
     */
    IS_FALSE(false, "否"),

    /**
     * 数据库存储值 {@code 1}，表示逻辑真。
     */
    IS_TRUE(true, "是");

    /**
     * Java 布尔语义值。
     */
    private final boolean booleanValue;

    /**
     * 面向中文展示的枚举名称。
     */
    private final String nameCn;

    /**
     * 创建数据库整型布尔枚举。
     *
     * @param booleanValue Java 布尔语义值
     * @param nameCn       中文展示名称
     */
    BooleanEnum(boolean booleanValue, String nameCn) {
        this.booleanValue = booleanValue;
        this.nameCn = nameCn;
    }

    /**
     * 根据数据库整型值获取枚举。
     *
     * @param value 数据库存储值，只支持 0 和 1
     * @return 对应的布尔枚举
     */
    public static BooleanEnum valueOf(int value) {
        if (value == IS_FALSE.getValue()) {
            return IS_FALSE;
        }
        if (value == IS_TRUE.getValue()) {
            return IS_TRUE;
        }
        throw new IllegalArgumentException("Unsupported boolean database value: " + value);
    }

    /**
     * 获取 MyBatis-Plus 写入数据库的枚举值。
     *
     * @return {@code false} 对应 {@code 0}，{@code true} 对应 {@code 1}
     */
    @Override
    public Integer getValue() {
        return booleanValue ? 1 : 0;
    }
}
