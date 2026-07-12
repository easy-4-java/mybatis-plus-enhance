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
 * 将 {@link KeyValuePair} 列表适配为 JDK {@link ListResourceBundle}。
 * <p>
 * 该类适合把数据库或远程配置返回的键值列表接入标准 {@code ResourceBundle} 查找机制。
 */
public class I18nListResourceBundle extends ListResourceBundle {

    /** 国际化键值集合。 */
    private final List<KeyValuePair> i18nList;

    /**
     * @param i18nList 将按列表顺序导出的国际化键值集合
     */
    public I18nListResourceBundle(List<KeyValuePair> i18nList) {
        this.i18nList = Collections.unmodifiableList(new ArrayList<>(
                Objects.requireNonNull(i18nList, "i18nList must not be null")));
    }

    /**
     * @return JDK 资源包要求的二维键值数组
     */
    @Override
    protected Object[][] getContents() {
        Object[][] objects = new Object[this.i18nList.size()][2];
        for (int i = 0; i < this.i18nList.size(); i++) {
            KeyValuePair pair = this.i18nList.get(i);
            objects[i] = new Object[]{pair.getKey(), pair.getValue()};
        }
        return objects;
    }
}
