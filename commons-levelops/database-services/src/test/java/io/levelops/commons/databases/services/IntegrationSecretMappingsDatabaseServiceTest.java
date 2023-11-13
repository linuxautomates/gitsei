package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationSecretMapping;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IntegrationSecretMappingsDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    IntegrationSecretMappingsDatabaseService databaseService;
    String integrationId;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        IntegrationService integrationService = new IntegrationService(dataSource);
        databaseService = new IntegrationSecretMappingsDatabaseService(dataSource);
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS foo CASCADE; ",
                "CREATE SCHEMA foo; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        integrationService.ensureTableExistence("foo");
        integrationId = integrationService.insert("foo", Integration.builder()
                .name("testInteg")
                .status("enabled")
                .application("test")
                .build());
        databaseService.ensureTableExistence("foo");
    }

    @Test
    public void name() throws SQLException {
        IntegrationSecretMapping mapping = IntegrationSecretMapping.builder()
                .name("test")
                .integrationId(integrationId)
                .smConfigId("0")
                .smKey("field")
                .build();
        String id = databaseService.insert("foo", mapping);

        assertThatThrownBy(() -> databaseService.insert("foo", mapping))
                .hasMessageContaining("duplicate key value violates unique constraint \"unique_name\"");

        Optional<IntegrationSecretMapping> out = databaseService.get("foo", id);
        assertThat(out).isPresent();
        DefaultObjectMapper.prettyPrint(out.get());
        assertThat(out.get()).isEqualToIgnoringGivenFields(mapping.toBuilder()
                .id(id)
                .build(), "updatedAt", "createdAt");

        databaseService.update("foo", IntegrationSecretMapping.builder()
                .id(id)
                .name("edited")
                .smConfigId("1")
                .smKey("field-edited")
                .build());

        out = databaseService.get("foo", id);
        assertThat(out.map(IntegrationSecretMapping::getName)).contains("edited");
        assertThat(out.map(IntegrationSecretMapping::getSmConfigId)).contains("1");
        assertThat(out.map(IntegrationSecretMapping::getSmKey)).contains("field-edited");

        assertThat(IterableUtils.getFirst(databaseService.filter(0, 1, "foo", IntegrationSecretMappingsDatabaseService.Filter.builder()
                .integrationId(integrationId)
                .build()).getRecords()).map(IntegrationSecretMapping::getId)).contains(id);

        databaseService.delete("foo", id);
        assertThat(databaseService.get("foo", id)).isEmpty();
    }
}