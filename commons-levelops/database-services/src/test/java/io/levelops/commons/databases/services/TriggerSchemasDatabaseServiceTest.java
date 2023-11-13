package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.KvField;
import io.levelops.commons.databases.models.database.TriggerSchema;
import io.levelops.commons.databases.models.database.TriggerType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TriggerSchemasDatabaseServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private TriggerSchemasDatabaseService triggerSchemasService;

    private String company = "test";

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        triggerSchemasService = new TriggerSchemasDatabaseService(dataSource, DefaultObjectMapper.get());
        new JdbcTemplate(dataSource).execute("DROP SCHEMA IF EXISTS test CASCADE;" + "CREATE SCHEMA test;");
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        triggerSchemasService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        Assertions.assertThat(triggerSchemasService.getTriggerTypes(company))
            .as("Types from the db should match the inserted types only")
            .containsExactlyInAnyOrderElementsOf(List.of(TriggerType.MANUAL, TriggerType.SCHEDULED, TriggerType.COMPONENT_EVENT));

        assertThatThrownBy(()-> {
                    TriggerSchema triggerSchema = TriggerSchema.builder()
                            .triggerType(TriggerType.MANUAL)
                            .description("Trigger for events emited by the different components in LevelOps")
                            .examples(null)
                            .fields(Map.of("component_id", KvField.builder().key("component_id").required(true).type("text").validation("uuid").build()))
                            .build();
                    String id = triggerSchemasService.insert(company, triggerSchema);
                }).isInstanceOf(DuplicateKeyException.class);

        var dbRecord = triggerSchemasService.get(company, TriggerType.MANUAL.toString()).orElseThrow();
        var dbRecordById = triggerSchemasService.getById(company, dbRecord.getId()).orElseThrow();

        Assertions.assertThat(dbRecord.getTriggerType()).isEqualTo(TriggerType.MANUAL);
        Assertions.assertThat(dbRecord).as("Trigger by type should be equals to trigger by id").isEqualTo(dbRecordById);
        Assertions.assertThat(triggerSchemasService.getTriggerTypes(company))
            .as("Types from the db should match the inserted types only")
            .containsExactlyInAnyOrderElementsOf(List.of(TriggerType.COMPONENT_EVENT, TriggerType.SCHEDULED, TriggerType.MANUAL));
    }
    
}