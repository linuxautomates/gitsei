package io.levelops.etl.jobs.jira_user_emails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.ingestion.services.ControlPlaneJobService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.jira.models.JiraUserEmail;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class JiraUserEmailsServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static String company = "test_company";
    private static JiraUserEmailsService jiraUserEmailsService;
    private static UserIdentityService userIdentityService;
    private static ObjectMapper objectMapper = DefaultObjectMapper.get();
    private static String integrationId = "1";
    private static Integration integration;


    @Mock
    private static ControlPlaneJobService controlPlaneJobService;
    @Mock
    private static ControlPlaneService controlPlaneService;
    @Mock
    private static InventoryServiceImpl inventoryService;
    @Mock
    private static GcsUtils gcsUtils;

    @Before
    public void setupNonStatic() throws InventoryException {
        MockitoAnnotations.initMocks(this);

        jiraUserEmailsService = new JiraUserEmailsService(
                controlPlaneJobService,
                objectMapper,
                gcsUtils,
                userIdentityService,
                controlPlaneService,
                inventoryService
        );
        when(inventoryService.getIntegration(any(), any())).thenReturn(integration);
        when(inventoryService.getIntegration(any())).thenReturn(integration);
    }

    @Before
    public void resetDb() throws SQLException {
        String sql = "DELETE FROM " + company + "." + UserIdentityService.USER_IDS_TABLE + ";";
        dataSource.getConnection().prepareStatement(sql).execute();
    }

    @BeforeClass
    public static void setup() throws SQLException, JsonProcessingException {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE; ").execute();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA " + company + " ; ").execute();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA " + " _levelops_etl; ").execute();
        objectMapper = DefaultObjectMapper.get();
        var integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        integration = Integration.builder()
                .id("1")
                .name("test")
                .application("jira")
                .satellite(false)
                .status("active")
                .build();
        integrationId = integrationService.insert(company, integration);
        integration = integration.toBuilder().id(integrationId).build();
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
    }

    private void insertIntegrationUser(String cloudId, String email) throws SQLException {
        userIdentityService.insert(company, DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(cloudId)
                .originalDisplayName(cloudId)
                .emails(email != null ? List.of(email) : null)
                .build());
    }

    private JobContext createJobContext() {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(UUID.randomUUID()).instanceId(1).build())
                .integrationId(integrationId)
                .jobScheduledStartTime(new Date())
                .tenantId(company)
                .integrationType("jira")
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .etlProcessorName("JiraUserEmailsEtlProcessor")
                .isFull(false)
                .jobType(JobType.GENERIC_INTEGRATION_JOB)
                .build();
    }

    @Test
    public void testIncrementalPayload() throws SQLException {
        insertIntegrationUser("stephen-the-goat", null);
        insertIntegrationUser("klay-man", null);
        insertIntegrationUser("bron-the-master-flopper", "bron-flops-hard@loser.com");
        insertIntegrationUser("kumingod", "kumingod@warrios.com");
        var payload = jiraUserEmailsService.getIncrementalPayload(createJobContext());
        assertThat(payload).containsExactlyInAnyOrder("stephen-the-goat", "klay-man");
    }


    @Test
    public void testFullPayload() throws SQLException {
        insertIntegrationUser("stephen-the-goat", null);
        insertIntegrationUser("klay-man", null);
        insertIntegrationUser("bron-the-master-flopper", "bron-flops-hard@loser.com");
        insertIntegrationUser("kumingod", "kumingod@warrios.com");
        var payload = jiraUserEmailsService.getFullPayload(createJobContext());
        assertThat(payload).containsExactlyInAnyOrder("stephen-the-goat", "klay-man", "bron-the-master-flopper", "kumingod");
    }

    @Test
    public void testProcessUserEmail() throws SQLException {
        insertIntegrationUser("stephen-the-goat", "goat@warriors.com");
        jiraUserEmailsService.processUserEmail(JiraUserEmail.builder()
                .email("steph@warriors.com")
                .accountId("stephen-the-goat")
                .build(), company, integrationId);
        var user = userIdentityService.getUserByCloudId(company, integrationId,"stephen-the-goat");
        assertThat(user.get().getEmails()).containsExactlyInAnyOrder("steph@warriors.com", "goat@warriors.com");
    }
}