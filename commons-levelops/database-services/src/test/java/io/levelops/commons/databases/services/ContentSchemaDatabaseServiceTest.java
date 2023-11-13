package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.ContentSchema;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ContentSchemaDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    ContentSchemaDatabaseService contentSchemaDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        contentSchemaDatabaseService = new ContentSchemaDatabaseService(dataSource, DefaultObjectMapper.get());
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        contentSchemaDatabaseService.populateData = false;
        contentSchemaDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void testPopulate() throws SQLException {
        contentSchemaDatabaseService.populateData = true;
        contentSchemaDatabaseService.ensureTableExistence("test");

        DbListResponse<ContentSchema> test = contentSchemaDatabaseService.list("test", 0, 10);
        assertThat(test.getRecords()).isNotEmpty();
    }

    @Test
    public void test() throws SQLException {
        ContentType contentType = ContentType.fromString("integration/test/test");
        ContentSchema contentSchema = ContentSchema.builder()
                .contentType(contentType)
                .build();
        String id = contentSchemaDatabaseService.insert("test", contentSchema);

        contentSchema = contentSchema.toBuilder().key(id).build();

        ContentSchema test = contentSchemaDatabaseService.get("test", id).orElse(null);
        assertThat(test).isEqualTo(contentSchema);

        contentSchemaDatabaseService.getByContentType("test", contentType).orElse(null);
        assertThat(test).isEqualTo(contentSchema);

        DbListResponse<ContentSchema> response = contentSchemaDatabaseService.filter("test", 0, 1, List.of(ContentType.fromString("integration/a/b")));
        assertThat(response.getRecords()).isEmpty();
    }
}