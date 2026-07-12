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
 * 描述指定语言对应的数据列和别名。
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface I18nLocale {

    /**
     * 该列对应的语言环境。
     *
     * @return 语言枚举
     */
    public abstract LocaleEnum locale() default LocaleEnum.zh_CN;

    /**
     * 数据库来源列名。
     *
     * @return 来源列名
     */
    public abstract String column();

    /**
     * 查询结果中的可选列别名。
     *
     * @return 列别名
     */
    public abstract String alias() default "";

}
