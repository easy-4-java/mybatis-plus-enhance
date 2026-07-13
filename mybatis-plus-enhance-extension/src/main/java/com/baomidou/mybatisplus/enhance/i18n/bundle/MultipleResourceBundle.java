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

import java.util.Enumeration;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * 按声明顺序组合多个 {@link ResourceBundle} 的复合资源包。
 * <p>
 * 查找时返回第一个命中值，键集合则合并父资源包和所有子资源包。
 */
public class MultipleResourceBundle extends ResourceBundle {

    /**
     * 按查找优先级排列的子资源包快照。
     */
    private final ResourceBundle[] bundles;

    /** 创建待由子类或反射配置的空复合资源包。 */
    public MultipleResourceBundle() {
        this.bundles = new ResourceBundle[0];
    }

    /**
     * 创建复合资源包，并复制传入数组以隔离调用方后续修改。
     *
     * @param bundles 按查找优先级排列的资源包
     */
    public MultipleResourceBundle(ResourceBundle... bundles) {
        this.bundles = Objects.requireNonNull(bundles, "bundles must not be null").clone();
    }

    /**
     * 按声明顺序查找第一个包含指定键的资源包。
     *
     * @param key 资源键，不能为空
     * @return 第一个命中的资源值；全部未命中时返回 {@code null}
     */
    @Override
    protected Object handleGetObject(String key) {
        Objects.requireNonNull(key, "key must not be null");
        for (ResourceBundle bundle : bundles) {
            if (Objects.isNull(bundle)) {
                continue;
            }
            try {
                return bundle.getObject(key);
            } catch (MissingResourceException ignored) {
                // 当前资源包未命中时继续按优先级查找。
            }
        }
        return null;
    }

    /**
     * 合并父资源包与全部子资源包的键，并保持首次出现顺序。
     *
     * @return 去重后的资源键枚举
     */
    @Override
    public Enumeration<String> getKeys() {
        return new ResourceBundleEnumeration(this.parent, this.bundles);
    }

}
