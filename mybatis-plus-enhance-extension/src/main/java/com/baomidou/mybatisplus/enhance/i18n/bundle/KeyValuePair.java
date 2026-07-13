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
package com.baomidou.mybatisplus.enhance.i18n.bundle;

import java.util.Objects;

/**
 * 不可变的国际化字符串键值对。
 *
 * <p>支持将 {@code key=value} 文本解析为资源键和值；只按第一个等号分隔，
 * 因此值中可以继续包含等号。该对象用于 {@link I18nListResourceBundle} 构造资源条目。</p>
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public final class KeyValuePair {

    /**
     * 缺省空值。
     */
    public static final String EMPTY = "";

    /**
     * 资源键，保证非空。
     */
    public final String key;

    /**
     * 资源值，保证非空。
     */
    public final String value;

    /**
     * 创建不可变键值对。
     *
     * @param key   资源键
     * @param value 资源值
     */
    private KeyValuePair(String key, String value) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * 解析 {@code key=value} 格式的文本。
     *
     * <p>文本不含等号时，整个文本作为键并使用空字符串作为值；等号后的内容原样保留。</p>
     *
     * @param asString 键值对文本
     * @return 解析得到的不可变键值对
     * @throws NullPointerException 输入文本为 {@code null} 时抛出
     */
    public static KeyValuePair valueOf(String asString) {
        Objects.requireNonNull(asString, "asString must not be null");
        int equalsIndex = asString.indexOf('=');
        if (equalsIndex == -1) {
            return new KeyValuePair(asString, EMPTY);
        }

        String aKey = asString.substring(0, equalsIndex);
        String aValue = equalsIndex == asString.length() - 1 ? EMPTY : asString.substring(equalsIndex + 1);

        return new KeyValuePair(aKey, aValue);
    }

    /**
     * 获取缺省空值。
     *
     * @return 空字符串常量
     */
    public static String getEmpty() {
        return EMPTY;
    }

    /**
     * 按键和值判断两个键值对是否相等。
     *
     * @param that 待比较对象
     * @return 键和值均相等时返回 {@code true}
     */
    @Override
    public boolean equals(Object that) {
        if (!(that instanceof KeyValuePair)) {
            return false;
        }

        KeyValuePair other = (KeyValuePair) that;
        return key.equals(other.key) && value.equals(other.value);
    }

    /**
     * 计算与 {@link #equals(Object)} 一致的哈希值。
     *
     * @return 键和值组合后的哈希值
     */
    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    /**
     * 按 {@code key=value} 格式输出键值对。
     *
     * @return 可再次由 {@link #valueOf(String)} 解析的文本
     */
    @Override
    public String toString() {
        return key + '=' + value;
    }

    /**
     * 获取资源键。
     *
     * @return 非空资源键
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取资源值。
     *
     * @return 非空资源值
     */
    public String getValue() {
        return value;
    }

}
