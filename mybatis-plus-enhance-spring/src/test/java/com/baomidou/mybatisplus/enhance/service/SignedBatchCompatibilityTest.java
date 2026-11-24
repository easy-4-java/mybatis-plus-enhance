package com.baomidou.mybatisplus.enhance.service;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.enhance.mapper.EnhanceBaseMapper;
import com.baomidou.mybatisplus.enhance.service.impl.EnhanceServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.session.SqlSession;
import org.junit.Test;

import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** 验证新版批处理回调返回实际写入结果，并在批处理后补签。 */
public class SignedBatchCompatibilityTest {
    @Test
    public void shouldReturnInsertResultAndResignGeneratedId() {
        TestService service = service();
        assertTrue(service.saveBatchSigned(Collections.singletonList(new Row()), 10));
        assertEquals(Integer.valueOf(3), service.results.get(0));
        assertEquals(Collections.singletonList(42L), service.signedIds);
    }

    @Test
    public void shouldReturnUpdateResultAndResignExistingId() {
        TestService service = service();
        Row row = new Row();
        row.id = 7L;
        assertTrue(service.updateBatchSignedById(Collections.singletonList(row), 10));
        assertEquals(Integer.valueOf(5), service.results.get(0));
        assertEquals(Collections.singletonList(7L), service.signedIds);
    }

    private TestService service() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "batch-test");
        assistant.setCurrentNamespace(TestMapper.class.getName());
        TableInfoHelper.initTableInfo(assistant, Row.class);
        return new TestService();
    }

    public interface TestMapper extends EnhanceBaseMapper<Row> {
    }

    public static class Row {
        @TableId
        public Long id;
    }

    private static class TestService extends EnhanceServiceImpl<TestMapper, Row> {
        private final List<Integer> results = new ArrayList<>();
        private List<? extends Serializable> signedIds;

        private TestService() {
            super(null);
        }

        @Override
        public Class<TestMapper> getMapperClass() {
            return TestMapper.class;
        }

        @Override
        public Class<Row> getEntityClass() {
            return Row.class;
        }

        @Override
        protected <E> boolean executeBatch(Collection<E> rows, int batchSize,
                                          BiFunction<SqlSession, E, Integer> consumer) {
            SqlSession session = (SqlSession) Proxy.newProxyInstance(SqlSession.class.getClassLoader(),
                    new Class<?>[]{SqlSession.class}, (proxy, method, args) -> {
                        if ("insert".equals(method.getName())) {
                            ((Row) args[1]).id = 42L;
                            return 3;
                        }
                        if ("update".equals(method.getName())) {
                            return 5;
                        }
                        throw new UnsupportedOperationException(method.getName());
                    });
            for (E row : rows) {
                results.add(consumer.apply(session, row));
            }
            return true;
        }

        @Override
        public void doSignatureByBatchIds(Collection<? extends Serializable> ids) {
            signedIds = new ArrayList<>(ids);
        }
    }
}
