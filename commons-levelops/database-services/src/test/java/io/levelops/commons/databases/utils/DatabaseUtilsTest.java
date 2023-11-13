package io.levelops.commons.databases.utils;

import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class DatabaseUtilsTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static NamedParameterJdbcTemplate template;

    @BeforeClass
    public static void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(jdbcTemplate::execute);
        template = new NamedParameterJdbcTemplate(jdbcTemplate);
    }

    enum TestEnum {
        ABC,
        DEF,
        GHI
    }

    @Test
    public void test() {
        String sql = DatabaseUtils.generateSqlToCreateEnumType("test_enum_t", TestEnum.class);
        assertThat(sql).isEqualTo("CREATE TYPE test_enum_t AS ENUM ('abc', 'def', 'ghi')");
    }

    @Test
    public void testArray() throws SQLException {
        Array array = Mockito.mock(Array.class);

        when(array.getArray()).thenReturn(new String[]{"a", "b", "c"});
        assertThat(DatabaseUtils.fromSqlArray(array, String.class)).containsExactly("a", "b", "c");

        when(array.getArray()).thenReturn(new Integer[]{1, 2, 3});
        assertThat(DatabaseUtils.fromSqlArray(array, Integer.class)).containsExactly(1, 2, 3);
    }

    @Test
    public void toSqlArray() {
        assertThat(DatabaseUtils.toSqlArray(null)).isEqualTo("{}");
        assertThat(DatabaseUtils.toSqlArray(List.of("a", "b", "c"))).isEqualTo("{\"a\",\"b\",\"c\"}");

        String expected = "{\"some \\\"quoted\\\" text\",\"single 'quote'\",\"some \\\"double \\\"quoted\\\"\\\" text\"}";
        assertThat(DatabaseUtils.toSqlArray(List.of("some \"quoted\" text", "single 'quote'", "some \"double \"quoted\"\" text"))).isEqualTo(expected);
    }

    @Test
    public void testSqlArrayInDb() {
        List<String> arrayWithQuotes = List.of(
                "some \"quoted\" text",
                "single 'quote'",
                "some \"double \"quoted\"\" text");

        template.getJdbcTemplate().execute("CREATE TABLE IF NOT EXISTS test.tmp (id INT, arr VARCHAR[])");
        template.update("INSERT INTO test.tmp (id, arr) VALUES (:id, :arr::varchar[])",
                Map.of("id", 0,
                        "arr", DatabaseUtils.toSqlArray(arrayWithQuotes)));
        List<Map<String, Object>> output = template.query("SELECT id, arr FROM test.tmp", (rs, rowNum) -> Map.of(
                "arr", Arrays.asList((String[]) rs.getArray("arr").getArray())
        ));
        DefaultObjectMapper.prettyPrint(output);
        assertThat(output).hasSize(1);
        //noinspection unchecked
        assertThat((List<String>) output.get(0).get("arr")).containsExactlyElementsOf(arrayWithQuotes);
    }
}