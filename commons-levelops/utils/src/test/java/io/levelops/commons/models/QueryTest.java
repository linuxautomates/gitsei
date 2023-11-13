package io.levelops.commons.models;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class QueryTest {
    @Test
    public void testSelectAndFromClause() {
        String sql = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("foo.test"))
                .build()
                .toSql();
        String expectedSql = "SELECT * FROM foo.test";
        assertSqlStatementsAreSame(sql, expectedSql);
    }

    @Test
    public void testWithWhereClause() {
        String sql = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("foo.test"))
                .where(Query.conditions(Arrays.asList("id = 1", "name = foo"), Collections.emptyMap()),
                        Query.Condition.AND)
                .build()
                .toSql();
        String expectedSql = "SELECT * FROM foo.test WHERE id = 1 AND name = foo";
        assertSqlStatementsAreSame(sql, expectedSql);
    }

    @Test
    public void testWithGroupBy() {
        String sql = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("foo.test"))
                .groupBy(List.of(Query.groupByField("name")))
                .build()
                .toSql();
        String expectedSql = "SELECT * FROM foo.test GROUP BY name";
        assertSqlStatementsAreSame(sql, expectedSql);
    }

    @Test
    public void testWithSortBy() {
        String sql = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("foo.test"))
                .orderBy(Query.sortByField("name", "desc", false))
                .build()
                .toSql();
        String expectedSql = "SELECT * FROM foo.test ORDER BY name desc";
        assertSqlStatementsAreSame(sql, expectedSql);
    }

    @Test
    public void testWithLimit() {
        String sql = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("foo.test"))
                .limit(3)
                .build()
                .toSql();

        String expectedSql = "SELECT * FROM foo.test LIMIT 3";
        assertSqlStatementsAreSame(sql, expectedSql);
    }

    @Test
    public void testNestedQuery() {
        String nestedQuery = Query.builder()
                .select(Query.selectField("field1"),
                        Query.selectField("field2"))
                .from(Query.fromField("foo"))
                .build()
                .toSql();
        String outerQuery = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("( " +  nestedQuery + " )"))
                .groupBy(List.of(Query.groupByField("field1")))
                .offset(10)
                .limit(5)
                .build()
                .toSql();
        String expectedSql = "SELECT * FROM ( SELECT field1 , field2 FROM foo ) GROUP BY field1 OFFSET 10 LIMIT 5";
        assertSqlStatementsAreSame(outerQuery, expectedSql);
    }

    @Test
    public void testSql() {
        Query.QueryConditions queryConditions = Query.conditions(Arrays.asList("id = 1", "name = foo"),
                Map.of("name", "foo"));
        String sql = Query.builder()
                .select(Query.selectField("*"))
                .from(Query.fromField("foo.test"))
                .where(queryConditions, Query.Condition.AND)
                .groupBy(List.of(Query.groupByField("name")))
                .orderBy(Collections.singletonList(Query.sortByField("name", "desc", false)))
                .limit(-1)
                .build()
                .toSql();
        String expectedSql = "SELECT * FROM foo.test WHERE id = 1 AND name = foo GROUP BY name ORDER BY name desc";
        assertSqlStatementsAreSame(sql, expectedSql);
    }

    private void assertSqlStatementsAreSame(String sql, String expectedSql) {
        Assertions.assertThat(sql.split("[\\s]+")).isEqualTo(expectedSql.split("[\\s]+"));
    }
}
