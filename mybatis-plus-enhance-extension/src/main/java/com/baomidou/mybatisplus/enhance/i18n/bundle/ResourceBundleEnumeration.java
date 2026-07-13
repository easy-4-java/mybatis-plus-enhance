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

import java.util.*;

/**
 * 对父资源包和多个子资源包的键进行去重合并的枚举器。
 */
public class ResourceBundleEnumeration implements Enumeration<String> {

    /**
     * 指向去重后资源键集合的迭代器。
     */
    private final Iterator<String> iterator;

    /**
     * @param bundles 待合并的资源包
     */
    public ResourceBundleEnumeration(ResourceBundle... bundles) {
        this(null, bundles);
    }

    /**
     * @param parent  可选父资源包
     * @param bundles 待合并的子资源包
     */
    public ResourceBundleEnumeration(ResourceBundle parent, ResourceBundle... bundles) {
        Set<String> keys = new LinkedHashSet<>();
        if (Objects.nonNull(parent)) {
            keys.addAll(parent.keySet());
        }
        ResourceBundle[] sourceBundles = Objects.isNull(bundles) ? new ResourceBundle[0] : bundles;
        for (ResourceBundle bundle : sourceBundles) {
            if (Objects.isNull(bundle)) {
                continue;
            }
            keys.addAll(bundle.keySet());
        }
        this.iterator = keys.iterator();
    }

    /**
     * 判断是否还有未返回的资源键。
     *
     * @return 尚有资源键时返回 {@code true}
     */
    @Override
    public boolean hasMoreElements() {
        return iterator.hasNext();
    }

    /**
     * 返回下一个资源键。
     *
     * @return 下一个去重后的资源键
     * @throws NoSuchElementException 已遍历完全部资源键时抛出
     */
    @Override
    public String nextElement() {
        return iterator.next();
    }

}
