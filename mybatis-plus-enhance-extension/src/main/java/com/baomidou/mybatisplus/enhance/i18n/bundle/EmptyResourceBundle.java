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

import java.util.Collections;
import java.util.Enumeration;
import java.util.ResourceBundle;

/**
 * 不包含任何键值的资源包，用作国际化查找链的空对象。
 */
public class EmptyResourceBundle extends ResourceBundle {

    /**
     * @return 始终为空的键枚举器
     */
    @Override
    public Enumeration<String> getKeys() {
        return Collections.emptyEnumeration();
    }

    /**
     * @param key 资源键
     * @return 始终返回 {@code null}
     */
    @Override
    protected Object handleGetObject(String key) {
        return null;
    }

}
