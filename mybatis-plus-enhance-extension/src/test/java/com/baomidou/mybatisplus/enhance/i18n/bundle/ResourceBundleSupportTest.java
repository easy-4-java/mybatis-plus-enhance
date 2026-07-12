package com.baomidou.mybatisplus.enhance.i18n.bundle;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * 动态资源包适配与组合查找测试。
 */
public class ResourceBundleSupportTest {

    @Test
    public void shouldResolveByBundlePriorityAndMergeKeys() {
        ResourceBundle primary = bundle("title=订单", "shared=主资源");
        ResourceBundle fallback = bundle("description=订单描述", "shared=备用资源");
        ResourceBundle combined = new MultipleResourceBundle(primary, fallback);

        assertEquals("订单", combined.getString("title"));
        assertEquals("订单描述", combined.getString("description"));
        assertEquals("主资源", combined.getString("shared"));
        assertEquals(new HashSet<>(Arrays.asList("title", "description", "shared")),
                new HashSet<>(combined.keySet()));
    }

    @Test
    public void shouldParseValuesContainingEqualsSign() {
        KeyValuePair pair = KeyValuePair.valueOf("url=https://example.com?a=1");

        assertEquals("url", pair.getKey());
        assertEquals("https://example.com?a=1", pair.getValue());
    }

    private ResourceBundle bundle(String... entries) {
        Set<KeyValuePair> pairs = new HashSet<>();
        for (String entry : entries) {
            pairs.add(KeyValuePair.valueOf(entry));
        }
        return new I18nListResourceBundle(Arrays.asList(pairs.toArray(new KeyValuePair[0])));
    }
}
