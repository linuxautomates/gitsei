package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.github.DbGithubCardTransition;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubWebhookEvent;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.List;

@Log4j2
public class GithubWebhookAggServiceTest {
    private static final String company = "test";
    private static final ObjectMapper m = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static GithubAggService githubAggService;
    private static ScmAggService scmAggService;
    private static String gitHubIntegrationId;
    private static String insertedRowId;
    private static UserIdentityService userIdentityService;

    @BeforeClass
    public static void setup() throws Exception {
        if (dataSource != null)
            return;

        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();

        IntegrationService integrationService = new IntegrationService(dataSource);

        userIdentityService = new UserIdentityService(dataSource);
        scmAggService = new ScmAggService(dataSource, userIdentityService);
        githubAggService = new GithubAggService(dataSource);

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        gitHubIntegrationId = integrationService.insert(company, Integration.builder()
                .application("github")
                .name("github test")
                .status("enabled")
                .build());
        userIdentityService.ensureTableExistence(company);
        scmAggService.ensureTableExistence(company);
        githubAggService.ensureTableExistence(company);
    }

    @Test
    public void testCreateEvent() throws IOException {
        String inputEvent = ResourceUtils.getResourceAsString("json/databases/github_webhook_create_event.json");
        List<GithubWebhookEvent> events = m.readValue(inputEvent, m.getTypeFactory()
                .constructCollectionType(List.class, GithubWebhookEvent.class));
        events.forEach(event ->
        {
            DbGithubCardTransition status = DbGithubCardTransition.fromGithubCardTransitionCreateEvent(gitHubIntegrationId, event);
            githubAggService.insertCardTransition(company, status);
        });
        Assertions.assertThat(githubAggService.getTransition(company, "61424456", "14306028", 1621433171L,
                gitHubIntegrationId).get().getId()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61424456", "14306028", 1621433171L,
                gitHubIntegrationId).get().getEndTime()).isEqualTo(0L);
    }

    @Test
    public void testMovedEvent() throws IOException {
        String inputEvent = ResourceUtils.getResourceAsString("json/databases/github_webhook_moved_event.json");
        List<GithubWebhookEvent> events = m.readValue(inputEvent, m.getTypeFactory()
                .constructCollectionType(List.class, GithubWebhookEvent.class));
        events.forEach(event ->
        {
            DbGithubCardTransition insertCardStatus = DbGithubCardTransition.fromGithubCardTransitionCreateEvent(gitHubIntegrationId, event);
            DbGithubCardTransition updateCardStatus = DbGithubCardTransition.fromGithubCardTransitionMovedEvent(gitHubIntegrationId, event);
            githubAggService.insertCardTransition(company, insertCardStatus);
            githubAggService.insertCardTransition(company, updateCardStatus);
        });
        Assertions.assertThat(githubAggService.getTransition(company, "61582738", "14410601", 0,
                gitHubIntegrationId).get().getEndTime()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61582738", "14410601", 0,
                gitHubIntegrationId).get().getStartTime()).isEqualTo(0L);
        Assertions.assertThat(githubAggService.getTransition(company, "61582738", "14410602", 1621605289L,
                gitHubIntegrationId).get().getStartTime()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61582738", "14410602", 0,
                gitHubIntegrationId).get().getEndTime()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61582738", "14410603", 1621605291L,
                gitHubIntegrationId).get().getStartTime()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61582738", "14410603", 1621605291L,
                gitHubIntegrationId).get().getEndTime()).isNotNull();
    }

    @Test
    public void testDeleteEvent() throws IOException {
        String inputEvent = ResourceUtils.getResourceAsString("json/databases/github_webhook_deleted_event.json");
        List<GithubWebhookEvent> events = m.readValue(inputEvent, m.getTypeFactory()
                .constructCollectionType(List.class, GithubWebhookEvent.class));
        events.forEach(event ->
        {
            DbGithubCardTransition deleteStatus = DbGithubCardTransition.fromGithubCardTransitionDeleteEvent(gitHubIntegrationId, event);
            githubAggService.insertCardTransition(company, deleteStatus);
        });
        Assertions.assertThat(githubAggService.getTransition(company, "61098079", "14306030", 0,
                gitHubIntegrationId).get().getEndTime()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61098079", "14306030", 0,
                gitHubIntegrationId).get().getStartTime()).isEqualTo(0L);
        Assertions.assertThat(githubAggService.getTransition(company, "61583106", "14410603", 0,
                gitHubIntegrationId).get().getEndTime()).isNotNull();
        Assertions.assertThat(githubAggService.getTransition(company, "61583106", "14410603", 0,
                gitHubIntegrationId).get().getStartTime()).isEqualTo(0L);
    }
}
