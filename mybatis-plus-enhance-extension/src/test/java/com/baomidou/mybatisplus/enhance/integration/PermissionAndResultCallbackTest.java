package com.baomidou.mybatisplus.enhance.integration;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.enhance.annotation.datascope.DataScopePlus;
import com.baomidou.mybatisplus.enhance.datascope.handler.DataScopeAnnotationHandler;
import com.baomidou.mybatisplus.enhance.datascope.handler.SqlDataScopeHandler;
import com.baomidou.mybatisplus.enhance.datascope.toolkit.DataScopeExpressions;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.util.Objects;
import com.baomidou.mybatisplus.enhance.plugins.MybatisPlusEnhanceInterceptor;
import com.baomidou.mybatisplus.enhance.plugins.inner.QueryResultCallbackInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.exceptions.PersistenceException;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class PermissionAndResultCallbackTest {
    @Test
    public void scopeFiltersReadAndWriteAndGroupsOrOnBothSides() throws Exception {
        try (SqlSession session = open("scope", false, new AtomicInteger(), "dept = 10 OR dept = 11")) {
            Rows mapper = session.getMapper(Rows.class);
            assertEquals(Arrays.asList(1, 3), mapper.visible(1));
            assertEquals(Arrays.asList(1, 3), mapper.unionRows());
            IPage<Integer> page = mapper.pageRows(new Page<>(2, 1));
            assertEquals(2, page.getTotal());
            assertEquals(Arrays.asList(3), page.getRecords());
            assertEquals(1, mapper.rename(1));
            assertEquals(0, mapper.rename(2));
            assertEquals(0, mapper.remove(2));
            assertEquals(1, mapper.remove(3));
            assertEquals(2, mapper.countAll());
        }
    }

    @Test
    public void structuredPermissionGroupsOrAndScopesBothJoinAliases() throws Exception {
        MultiDataPermissionHandler permission = new DataScopeAnnotationHandler((table, where, id, annotation) -> {
            String alias = table.getAlias().getName();
            return DataScopeExpressions.or(DataScopeExpressions.eq(alias, "dept", 10),
                    DataScopeExpressions.eq(alias, "dept", 11));
        });
        try (SqlSession session = open("structured_join", false, new AtomicInteger(), permission)) {
            assertEquals(Arrays.asList(1), session.getMapper(Rows.class).joinRows());
        }
    }

    @Test
    public void invalidPermissionStopsWriteBeforeExecution() throws Exception {
        try (SqlSession session = open("bad_scope", false, new AtomicInteger(), "dept = (")) {
            Rows mapper = session.getMapper(Rows.class);
            try {
                mapper.remove(1);
                fail("Invalid permission should reject the write");
            } catch (PersistenceException expected) {
                assertTrue(expected.getMessage().contains("Invalid data scope"));
            }
            assertEquals(3, mapper.countAll());
        }
    }

    @Test
    public void invokesCallbackOncePerSuccessfulQueryWithEitherRegistration() throws Exception {
        for (boolean plain : new boolean[]{false, true}) {
            AtomicInteger calls = new AtomicInteger();
            try (SqlSession session = open("callback_" + plain, plain, calls, (MultiDataPermissionHandler) null)) {
                Rows mapper = session.getMapper(Rows.class);
                assertEquals(Arrays.asList(1, 2, 3), mapper.visible(1));
                assertEquals(Arrays.asList(1, 2, 3), mapper.visible(1));
                assertEquals(2, calls.get());
                try {
                    mapper.invalid();
                    fail("Invalid SQL should fail");
                } catch (PersistenceException expected) {
                    assertEquals(2, calls.get());
                }
            }
        }
    }

    private SqlSession open(String name, boolean plain, AtomicInteger calls, String scope) throws Exception {
        return open(name, plain, calls, new SqlDataScopeHandler(id -> id.endsWith(".countAll") ? null : scope));
    }

    private SqlSession open(String name, boolean plain, AtomicInteger calls,
                            MultiDataPermissionHandler permission) throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
        try (Connection connection = source.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE scope_entry(id INT PRIMARY KEY, dept INT, status INT)");
            statement.execute("INSERT INTO scope_entry VALUES(1, 10, 1), (2, 20, 1), (3, 11, 2)");
            statement.execute("CREATE TABLE scope_detail(id INT PRIMARY KEY, dept INT)");
            statement.execute("INSERT INTO scope_detail VALUES(1, 10), (2, 10), (3, 20)");
        }
        Configuration config = plain ? new Configuration() : new MybatisConfiguration();
        config.setEnvironment(new Environment(name, new JdbcTransactionFactory(), source));
        config.addMapper(Rows.class);
        QueryResultCallbackInterceptor callback = new QueryResultCallbackInterceptor((parameters, rows) -> {
            assertNotNull(rows);
            if (Objects.isNull(permission)) {
                assertEquals(Arrays.asList(1), Arrays.asList(parameters.toArray()));
            }
            calls.incrementAndGet();
        });
        if (plain) {
            config.addInterceptor(callback);
        } else {
            MybatisPlusEnhanceInterceptor chain = new MybatisPlusEnhanceInterceptor();
            if (Objects.nonNull(permission)) {
                chain.addInnerInterceptor(new DataPermissionInterceptor(permission));
            }
            chain.addInnerInterceptor(new PaginationInnerInterceptor());
            chain.addInnerInterceptor(callback);
            config.addInterceptor(chain);
        }
        return new SqlSessionFactoryBuilder().build(config).openSession(true);
    }

    public interface Rows {
        @DataScopePlus
        @Select("SELECT a.id FROM scope_entry a JOIN scope_detail b ON a.id = b.id"
                + " WHERE a.status = 1 OR a.id = 3 ORDER BY a.id")
        java.util.List<Integer> joinRows();

        @Select("SELECT id FROM scope_entry WHERE status = #{status} OR id = 3 ORDER BY id")
        java.util.List<Integer> visible(@Param("status") int status);

        @Select("SELECT id FROM scope_entry WHERE status = 1 UNION ALL SELECT id FROM scope_entry WHERE status = 2 ORDER BY id")
        java.util.List<Integer> unionRows();

        @Select("SELECT id FROM scope_entry ORDER BY id")
        IPage<Integer> pageRows(Page<Integer> page);

        @Select("SELECT id FROM missing_scope_table")
        java.util.List<Integer> invalid();

        @Select("SELECT COUNT(*) FROM scope_entry")
        int countAll();

        @Update("UPDATE scope_entry SET status = 3 WHERE id = #{id}")
        int rename(@Param("id") int id);

        @Delete("DELETE FROM scope_entry WHERE id = #{id}")
        int remove(@Param("id") int id);
    }
}
