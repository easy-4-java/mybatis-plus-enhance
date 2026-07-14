package com.baomidou.mybatisplus.enhance.crypto;

import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.enhance.context.SignatureUpdateContext;
import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.plugins.inner.DataSignatureInnerInterceptor;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.enhance.annotation.crypto.TableSignature;
import org.junit.After;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * 签名表更新策略的拦截器约束测试。
 */
public class DataSignatureInnerInterceptorTest {

    @After
    public void tearDown() {
        SignatureUpdateContext.clear();
    }

    @Test
    public void shouldRejectSignedPartialUpdateByDefault() throws Exception {
        DataSignatureInnerInterceptor interceptor = new DataSignatureInnerInterceptor(handler(new AtomicBoolean()));

        try {
            interceptor.beforeUpdate(null, updateStatement(), parameters(new SignedEntity()));
            fail("Expected partial signed update to be rejected");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("DEFERRED_RESIGN"));
        }
    }

    @Test
    public void shouldDeferSigningInsideDeferredScope() throws Exception {
        AtomicBoolean signed = new AtomicBoolean();
        DataSignatureInnerInterceptor interceptor = new DataSignatureInnerInterceptor(handler(signed));

        try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                SignatureUpdateStrategy.DEFERRED_RESIGN)) {
            interceptor.beforeUpdate(null, updateStatement(), parameters(new SignedEntity()));
        }

        assertFalse(signed.get());
    }

    @Test
    public void shouldSignExplicitFullRowUpdate() throws Exception {
        AtomicBoolean signed = new AtomicBoolean();
        DataSignatureInnerInterceptor interceptor = new DataSignatureInnerInterceptor(handler(signed));

        try (SignatureUpdateContext.Scope ignored = SignatureUpdateContext.open(
                SignatureUpdateStrategy.FULL_ROW)) {
            interceptor.beforeUpdate(null, updateStatement(), parameters(new SignedEntity()));
        }

        assertTrue(signed.get());
    }

    private MapperMethod.ParamMap<Object> parameters(Object entity) {
        MapperMethod.ParamMap<Object> parameters = new MapperMethod.ParamMap<>();
        parameters.put(Constants.ENTITY, entity);
        return parameters;
    }

    private MappedStatement updateStatement() {
        Configuration configuration = new Configuration();
        return new MappedStatement.Builder(configuration, "Mapper.updateById",
                new StaticSqlSource(configuration, "UPDATE sample SET value = ?"), SqlCommandType.UPDATE)
                .build();
    }

    private DataSignatureHandler handler(AtomicBoolean signed) {
        return new DataSignatureHandler() {
            @Override
            public <T> boolean doEntitySignature(T entity) {
                signed.set(true);
                return true;
            }

            @Override
            public boolean doWrapperSignature(Class<?> entityClass,
                                              com.baomidou.mybatisplus.core.conditions.AbstractWrapper<?, ?, ?> updateWrapper) {
                return false;
            }

            @Override
            public <T> void doSignatureVerification(Object rawObject, Class<T> entityClass) {
            }
        };
    }

    @TableSignature
    private static class SignedEntity {
    }
}
