package com.baomidou.mybatisplus.enhance.observation;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SqlObservationTest {

    @Test
    public void shouldExposeElapsedMillisAndStatus() {
        SqlObservation success = new SqlObservation("mapper.select", "SELECT 1", 2_500_000L, null);
        assertEquals(2L, success.getElapsedMillis());
        assertTrue(success.isSuccess());

        SqlObservation failure = new SqlObservation("mapper.select", "SELECT 1", 0L,
                new IllegalStateException("failed"));
        assertFalse(failure.isSuccess());
    }
}
