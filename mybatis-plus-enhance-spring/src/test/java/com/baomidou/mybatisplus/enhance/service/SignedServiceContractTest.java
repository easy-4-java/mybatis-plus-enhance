package com.baomidou.mybatisplus.enhance.service;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.enhance.crypto.handler.DataSignatureHandler;
import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import com.baomidou.mybatisplus.enhance.service.impl.EnhanceServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.executor.result.DefaultResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Signed Service API 安全语义回归测试。
 */
public class SignedServiceContractTest {

    @Test
    public void shouldVerifyMapQueriesUsingServiceEntityType() {
        RecordingSignatureHandler handler = new RecordingSignatureHandler(false);
        TestService service = new TestService(handler, mapper(Collections.singletonMap("value", "ok"), null));
        List<Map<String, Object>> rows = service.listSignedMaps(Wrappers.emptyWrapper());
        assertEquals(1, rows.size());
        assertSame(SignedRow.class, handler.entityClass);
        assertEquals(1, handler.verificationCount);
    }

    @Test
    public void shouldUseVerifiedPathForMapPageShortcut() {
        RecordingSignatureHandler handler = new RecordingSignatureHandler(false);
        TestService service = new TestService(handler, mapper(Collections.singletonMap("value", "ok"), null));
        IPage<Map<String, Object>> page = new Page<Map<String, Object>>(1, 10);
        IPage<Map<String, Object>> result = service.pageSignedMaps(page);
        assertSame(page, result);
        assertEquals(1, handler.verificationCount);
        assertSame(SignedRow.class, handler.entityClass);
    }

    @Test
    public void shouldVerifyBeforeCallingBusinessResultHandler() {
        RecordingSignatureHandler handler = new RecordingSignatureHandler(true);
        TestService service = new TestService(handler, mapper(null, new SignedRow()));
        AtomicBoolean businessInvoked = new AtomicBoolean();
        try {
            service.listSigned(Wrappers.emptyWrapper(), context -> businessInvoked.set(true));
            fail("Expected signature verification failure");
        } catch (IllegalStateException expected) {
            assertEquals("invalid signature", expected.getMessage());
        }
        assertFalse(businessInvoked.get());
        assertEquals(1, handler.verificationCount);
    }

    @SuppressWarnings("unchecked")
    private TestMapper mapper(Map<String, Object> mapResult, SignedRow entityResult) {
        return (TestMapper) Proxy.newProxyInstance(
                TestMapper.class.getClassLoader(), new Class<?>[]{TestMapper.class},
                (proxy, method, arguments) -> {
                    if ("selectMaps".equals(method.getName())) {
                        return Collections.singletonList(mapResult);
                    }
                    if ("selectList".equals(method.getName())
                            && arguments.length == 2 && arguments[1] instanceof ResultHandler) {
                        DefaultResultContext<SignedRow> context = new DefaultResultContext<SignedRow>();
                        context.nextResultObject(entityResult);
                        ((ResultHandler<SignedRow>) arguments[1]).handleResult(context);
                        return null;
                    }
                    if (method.getReturnType() == int.class) {
                        return 0;
                    }
                    if (method.getReturnType() == boolean.class) {
                        return false;
                    }
                    return null;
                });
    }

    private interface TestMapper extends EnhanceBaseMapper<SignedRow> {
    }

    private static class TestService extends EnhanceServiceImpl<TestMapper, SignedRow> {

        private TestService(DataSignatureHandler handler, TestMapper mapper) {
            super(handler);
            this.baseMapper = mapper;
        }

        @Override
        public Class<SignedRow> getSignedEntityClass() {
            return SignedRow.class;
        }
    }

    private static class RecordingSignatureHandler implements DataSignatureHandler {

        private final boolean reject;
        private int verificationCount;
        private Class<?> entityClass;

        private RecordingSignatureHandler(boolean reject) {
            this.reject = reject;
        }

        @Override
        public <T> boolean doEntitySignature(T entity) {
            return false;
        }

        @Override
        public boolean doWrapperSignature(Class<?> entityClass, AbstractWrapper<?, ?, ?> updateWrapper) {
            return false;
        }

        @Override
        public <T> void doSignatureVerification(Object rawObject, Class<T> entityClass) {
            verificationCount++;
            this.entityClass = entityClass;
            if (reject) {
                throw new IllegalStateException("invalid signature");
            }
        }
    }

    private static class SignedRow {
    }
}
