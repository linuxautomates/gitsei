package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Component;
import io.levelops.commons.models.ComponentType;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class ComponentsDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private ComponentsDatabaseService componentsService;

    private String company = "test";

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        componentsService = new ComponentsDatabaseService(dataSource);
        new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS test CASCADE;" + "CREATE SCHEMA test;");
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        componentsService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        Component component = Component.builder().name("test").type(ComponentType.INTEGRATION).build();
        String id = componentsService.insert(company, component);

        var dbRecord = componentsService.get(company, id);
        var dbRecordByTypeName = componentsService.getByTypeName(company, ComponentType.INTEGRATION, "test");
        var dbRecordById = componentsService.getById(company, UUID.fromString(id));

        Assertions.assertThat(dbRecordByTypeName).as("ComponentType by type and name should be equals to ComponentType by id").isEqualTo(dbRecordById);
        Assertions.assertThat(dbRecord).as("ComponentType by id (string) should be equals to ComponentType by id (UUID)").isEqualTo(dbRecordById);

        List<ComponentType> componentTypes = componentsService.getComponentTypes(company);
        List<ComponentType> expectedComponentTypes = List.of(
                ComponentType.INTEGRATION,
                ComponentType.PLUGIN_RESULT,
                ComponentType.ASSESSMENT,
                ComponentType.SMART_TICKET,
                ComponentType.TRIAGE_RULES_MATCHED,
                ComponentType.CUSTOM);
        Assertions.assertThat(componentTypes).as("description").containsExactlyInAnyOrderElementsOf(expectedComponentTypes);
    }

}