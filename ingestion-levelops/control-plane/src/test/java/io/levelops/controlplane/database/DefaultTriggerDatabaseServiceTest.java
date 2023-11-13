package io.levelops.controlplane.database;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.controlplane.models.DbTriggerSettings;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class DefaultTriggerDatabaseServiceTest {
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    DefaultTriggerDatabaseService defaultTriggerDatabaseService;

    @Before
    public void setUp() throws Exception {
        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        defaultTriggerDatabaseService = new DefaultTriggerDatabaseService(template, DefaultObjectMapper.get());
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS control_plane CASCADE; ",
                "CREATE SCHEMA control_plane; "
        ).forEach(template.getJdbcTemplate()::execute);

        defaultTriggerDatabaseService.ensureTableExistence();
    }

    @Test
    public void testTriggerSettings() throws JsonProcessingException {
        UUID triggerID1 = UUID.randomUUID();
        UUID triggerID2 = UUID.randomUUID();
        UUID triggerID3 = UUID.randomUUID();
        defaultTriggerDatabaseService.createTrigger(
                triggerID1,
                "warriors",
                "1",
                false,
                "jira",
                60,
                null,
                null,
                null);

        var trigger = defaultTriggerDatabaseService.getTriggerById(triggerID1.toString()).get();
        assertThat(trigger.getSettings()).isEqualTo(null);

        // Create trigger with settings
        Map<String, String> metadata = Map.of("heu", "sid");
        defaultTriggerDatabaseService.createTrigger(
                triggerID2,
                "warriors",
                "2",
                false,
                "jira",
                60,
                DefaultObjectMapper.get().writeValueAsString(metadata),
                null,
                DbTriggerSettings.builder()
                        .backpressureThreshold(1)
                        .backwardScanSubjobSpanInMinutes(12L)
                        .build());
        var trigger2 = defaultTriggerDatabaseService.getTriggerById(triggerID2.toString()).get();
        assertThat(trigger2.getSettings().getBackpressureThreshold()).isEqualTo(1);
        assertThat(trigger2.getSettings().getBackwardScanSubjobSpanInMinutes()).isEqualTo(12);

        // Create trigger with partial settings
        defaultTriggerDatabaseService.createTrigger(
                triggerID3,
                "warriors",
                "3",
                false,
                "jira",
                60,
                DefaultObjectMapper.get().writeValueAsString(metadata),
                null,
                DbTriggerSettings.builder()
                        .backpressureThreshold(1)
                        .build());
        var trigger3 = defaultTriggerDatabaseService.getTriggerById(triggerID3.toString()).get();
        assertThat(trigger3.getSettings().getBackpressureThreshold()).isEqualTo(1);
        assertThat(trigger3.getSettings().getBackwardScanSubjobSpanInMinutes()).isEqualTo(null);
    }

}