/*
 * Copyright (c) 2018 (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.baomidou.mybatisplus.enhance.i18n.annotation;

import java.lang.annotation.*;

/**
 * 声明一个需要国际化映射的目标字段。
 *
 * <p>通过 {@link #i18n()} 描述不同语言对应的来源列。</p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface I18nColumn {

    /**
     * 目标对象中的字段或列名。
     *
     * @return 目标字段名；空字符串表示由使用位置推断
     */
    public abstract String column() default "";

    /**
     * 不同语言与来源列的映射。
     *
     * @return 国际化列定义
     */
    public abstract I18nLocale[] i18n();

}
