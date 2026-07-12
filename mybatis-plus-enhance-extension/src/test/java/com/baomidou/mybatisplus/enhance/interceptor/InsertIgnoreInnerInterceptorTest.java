package com.baomidou.mybatisplus.enhance.plugins.inner;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InsertIgnoreInnerInterceptorTest {

    @Test
    public void shouldRewriteInsertOnlyOnce() {
        assertEquals("INSERT IGNORE INTO orders(id) VALUES (?)",
                InsertIgnoreInnerInterceptor.rewriteSql("INSERT INTO orders(id) VALUES (?)"));
        assertEquals("insert IGNORE into orders(id) values (?)",
                InsertIgnoreInnerInterceptor.rewriteSql("insert IGNORE into orders(id) values (?)"));
    }

    @Test
    public void shouldPreserveNonInsertSql() {
        assertEquals("UPDATE orders SET status = ?",
                InsertIgnoreInnerInterceptor.rewriteSql("UPDATE orders SET status = ?"));
    }
}
