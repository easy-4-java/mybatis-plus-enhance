package com.baomidou.mybatisplus.enhance.context;

import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * {@link SignatureUpdateContext} 嵌套作用域测试。
 */
public class SignatureUpdateContextTest {

    @After
    public void tearDown() {
        SignatureUpdateContext.clear();
    }

    @Test
    public void shouldRestorePreviousStrategyAfterNestedScope() {
        assertEquals(SignatureUpdateStrategy.REJECT_PARTIAL, SignatureUpdateContext.current());

        try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                SignatureUpdateStrategy.DEFERRED_RESIGN)) {
            assertEquals(SignatureUpdateStrategy.DEFERRED_RESIGN, SignatureUpdateContext.current());
            try (SignatureUpdateContext.Scope nested = SignatureUpdateContext.open(
                    SignatureUpdateStrategy.SIGNATURE_ONLY)) {
                assertEquals(SignatureUpdateStrategy.SIGNATURE_ONLY, SignatureUpdateContext.current());
            }
            assertEquals(SignatureUpdateStrategy.DEFERRED_RESIGN, SignatureUpdateContext.current());
        }

        assertEquals(SignatureUpdateStrategy.REJECT_PARTIAL, SignatureUpdateContext.current());
    }
}
