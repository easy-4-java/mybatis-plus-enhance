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
 * <p>A simple string key/string value pair.</p>
 *
 * <p>This is useful as an argument type for options whose values take on the form {@code key=value}, such as JVM
 * command line system properties.</p>
 *
 * @author <a href="mailto:pholser@alumni.rice.edu">Paul Holser</a>
 */
public final class KeyValuePair {

    public static final String EMPTY = "";
    public final String key;
    public final String value;

    private KeyValuePair(String key, String value) {
        this.key = Objects.requireNonNull(key, "key must not be null");
        this.value = Objects.requireNonNull(value, "value must not be null");
    }

    /**
     * Parses a string assumed to be of the form {@code key=value} into its parts.
     *
     * @param asString key-value string
     * @return a key-value pair
     * @throws NullPointerException if {@code stringRepresentation} is {@code null}
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

    public static String getEmpty() {
        return EMPTY;
    }

    @Override
    public boolean equals(Object that) {
        if (!(that instanceof KeyValuePair)) {
            return false;
        }

        KeyValuePair other = (KeyValuePair) that;
        return key.equals(other.key) && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return key.hashCode() ^ value.hashCode();
    }

    @Override
    public String toString() {
        return key + '=' + value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

}
