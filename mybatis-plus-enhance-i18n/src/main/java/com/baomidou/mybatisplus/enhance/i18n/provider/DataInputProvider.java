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
package com.baomidou.mybatisplus.enhance.i18n.provider;

/**
 * 国际化外部数据输入端口。
 *
 * @param <T> 输入数据类型
 */
public interface DataInputProvider<T> {

    /**
     * 获取本次国际化处理所需的外部数据。
     *
     * @return 输入数据
     */
    public T get();

}
