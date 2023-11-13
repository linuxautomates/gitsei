package io.levelops.etl.jobs.jira_user_emails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations_shared.database.JobDefinitionDatabaseService;
import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.database.models.DbJobDefinition;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobPriority;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.etl.jobs.jira_user_emails.UserEmailsStage.UserEmailStageMetadata;
import io.levelops.etl.utils.GcsUtils;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.services.ControlPlaneJobService;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.jira.models.JiraUserEmail;
import io.levelops.integrations.storage.models.StorageContent;
import io.levelops.integrations.storage.models.StorageMetadata;
import io.levelops.integrations.storage.models.StorageResult;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UserEmailsStageTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static String company = "test_company";
    private static JiraUserEmailsService jiraUserEmailsService;
    private static ObjectMapper objectMapper;
    private static DataSource dataSource;
    private static JobDefinitionDatabaseService jobDefinitionDatabaseService;
    private static JobInstanceDatabaseService jobInstanceDatabaseService;
    private static UserIdentityService userIdentityService;
    private static DbJobDefinition jobDefinition;
    private static UUID jobDefinitionId;
    private static UserEmailsStage userEmailsStage;
    private static String integrationId = "1";
    private static Integration integration;

    @Mock
    private static Storage storage;
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
        userEmailsStage = new UserEmailsStage(objectMapper, jiraUserEmailsService, inventoryService, jobDefinitionDatabaseService);
        when(inventoryService.getIntegration(any(), any())).thenReturn(integration);
        when(inventoryService.getIntegration(any())).thenReturn(integration);
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
                .authentication(Integration.Authentication.ATLASSIAN_CONNECT_JWT)
                .build();
        integrationId = integrationService.insert(company, integration);
        integration = integration.toBuilder().id(integrationId).build();
        jobDefinitionDatabaseService = new JobDefinitionDatabaseService(objectMapper, dataSource);
        jobInstanceDatabaseService = new JobInstanceDatabaseService(objectMapper, dataSource, jobDefinitionDatabaseService);
        jobDefinitionDatabaseService.ensureTableExistence();
        jobInstanceDatabaseService.ensureTableExistence();
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        jobDefinition = DbJobDefinition.builder()
                .tenantId(company)
                .integrationId(integrationId)
                .integrationType("jira")
                .ingestionTriggerId(UUID.randomUUID().toString())
                .jobType(JobType.GENERIC_INTEGRATION_JOB)
                .isActive(true)
                .defaultPriority(JobPriority.HIGH)
                .attemptMax(10)
                .retryWaitTimeInMinutes(11)
                .timeoutInMinutes(12L)
                .frequencyInMinutes(13)
                .fullFrequencyInMinutes(14)
                .aggProcessorName("JiraUserEmailsEtlProcessor")
                .metadata(null)
                .build();

        jobDefinitionId = jobDefinitionDatabaseService.insert(jobDefinition);
//        jobInstanceDatabaseService.insert(DbJobInstance.builder().build())

    }

    private JobContext createJobContext() {
        return JobContext.builder()
                .jobInstanceId(JobInstanceId.builder().jobDefinitionId(jobDefinitionId).instanceId(1).build())
                .integrationId(integrationId)
                .jobScheduledStartTime(new Date())
                .tenantId(company)
                .integrationType("jira")
                .stageProgressMap(new HashMap<>())
                .stageProgressDetailMap(new HashMap<>())
                .etlProcessorName("JiraUserEmailsEtlProcessor")
                .isFull(false)
                .jobType(JobType.GENERIC_INTEGRATION_JOB)
                .jobInstanceDatabaseService(jobInstanceDatabaseService)
                .build();
    }

    UserEmailStageMetadata getMetadata() {
        return objectMapper.convertValue(
                jobDefinitionDatabaseService.get(jobDefinitionId).get().getMetadata().get("user_email_stage_metadata"), UserEmailStageMetadata.class);
    }

    private void insertIntegrationUser(String cloudId) throws SQLException {
        userIdentityService.insert(company, DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(cloudId)
                .originalDisplayName(cloudId)
                .build());
    }

    private JobDTO createIngestionJobDto(JobStatus jobStatus, String ingestionJobId) {
        var dataTypes = List.of("jira_user_emails");
        var storageResults = dataTypes.stream()
                .map(dataType -> StorageResult.builder()
                        .record(GcsDataResult.builder()
                                .htmlUri("htmluri-" + RandomStringUtils.random(5))
                                .blobId(null)
                                .uri("uri" + RandomStringUtils.random(5))
                                .build())
                        .storageMetadata(StorageMetadata.builder()
                                .dataType(dataType)
                                .build())
                        .build()
                ).toList();

        return JobDTO.builder()
                .status(jobStatus)
                .id(ingestionJobId)
                .result(objectMapper.convertValue(storageResults.get(0), Map.class))
                .build();
    }

    private void setupGcsResponse(List<JiraUserEmail> jiraUserEmails) throws JsonProcessingException {
        when(gcsUtils.fetchRecordsFromGcs(any(), any(), any(), anyString(), anyString())).thenReturn(StorageContent.<ListResponse<Object>>builder()
                .data(ListResponse.of(new ArrayList<>(jiraUserEmails)))
                .build());
    }

    private void clearMocks() {
        reset(controlPlaneJobService);
    }

    @Before
    public void resetDb() throws SQLException {
        String sql = "DELETE FROM " + company + "." + UserIdentityService.USER_IDS_TABLE + ";";
        dataSource.getConnection().prepareStatement(sql).execute();
    }

    @Test
    public void test() throws IngestionServiceException, SQLException, JsonProcessingException {
        String ingestionJob1 = UUID.randomUUID().toString();
        when(controlPlaneService.submitJob(any())).thenReturn(SubmitJobResponse.builder()
                .jobId(ingestionJob1)
                .build());

        // There are no integration users in the DB. So not ingestion jobs should be created
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );
        var metadata = getMetadata();
        assertThat(metadata.getJobIds()).isEmpty();

        // Add some integration users without emails
        insertIntegrationUser("steph-curry");
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );
        metadata = getMetadata();
        assertThat(metadata.getJobIds()).hasSize(1);
        verify(controlPlaneJobService, never()).getJobIfComplete(ingestionJob1);

        // Next job should wait for the ingestion results
        when(controlPlaneJobService.getJobIfComplete(ingestionJob1))
                .thenReturn(Optional.of(createIngestionJobDto(JobStatus.SUCCESS, ingestionJob1)));
        setupGcsResponse(List.of(
                JiraUserEmail.builder()
                        .email("steph_the_goat@warriors.com")
                        .accountId("steph-curry")
                        .build()
        ));
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );
        verify(controlPlaneJobService, times(1)).getJobIfComplete(ingestionJob1);
        var steph = userIdentityService.getUserByCloudId(company, integrationId, "steph-curry");
        assertThat(steph.get().getEmails()).containsExactly("steph_the_goat@warriors.com");

        // The ingestion metadata should be cleared out
        metadata = getMetadata();
        assertThat(metadata.getJobIds()).isEmpty();


        // Add some more integration users and run the job again
        clearMocks();
        insertIntegrationUser("klay-thompson");
        insertIntegrationUser("cp-3");
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );
        // More ingestion jobs should be created
        metadata = getMetadata();
        assertThat(metadata.getJobIds()).hasSize(1);
        verify(controlPlaneJobService, never()).getJobIfComplete(ingestionJob1);

        when(controlPlaneJobService.getJobIfComplete(ingestionJob1))
                .thenReturn(Optional.of(createIngestionJobDto(JobStatus.SUCCESS, ingestionJob1)));
        setupGcsResponse(List.of(
                JiraUserEmail.builder()
                        .email("klay-hot-hands@warriors.com")
                        .accountId("klay-thompson")
                        .build(),
                JiraUserEmail.builder()
                        .email("cp3-love-hate@warriors.com")
                        .accountId("cp-3")
                        .build()
        ));
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );
        verify(controlPlaneJobService, times(1)).getJobIfComplete(ingestionJob1);
        var klay = userIdentityService.getUserByCloudId(company, integrationId, "klay-thompson");
        assertThat(klay.get().getEmails()).containsExactly("klay-hot-hands@warriors.com");
        var cp3= userIdentityService.getUserByCloudId(company, integrationId, "cp-3");
        assertThat(cp3.get().getEmails()).containsExactly("cp3-love-hate@warriors.com");
    }

    @Test
    public void testFailedIngestionJob() throws IngestionServiceException, SQLException, JsonProcessingException {
        String ingestionJob1 = UUID.randomUUID().toString();
        when(controlPlaneService.submitJob(any())).thenReturn(SubmitJobResponse.builder()
                .jobId(ingestionJob1)
                .build());
        insertIntegrationUser("steph-curry");
        insertIntegrationUser("dray-dray");
        insertIntegrationUser("klay-klay");
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );

        // The ingestion jobs should be created at this point
        var metadata = getMetadata();
        assertThat(metadata.getJobIds()).hasSize(1);

        // Make the ingestion job fail. The job should still be cleared from the metadata
        when(controlPlaneJobService.getJobIfComplete(ingestionJob1))
                .thenReturn(Optional.of(createIngestionJobDto(JobStatus.FAILURE, ingestionJob1)));
        userEmailsStage.process(
                createJobContext(),
                new JiraUserEmailsJobState()
        );
        verify(controlPlaneJobService, times(1)).getJobIfComplete(ingestionJob1);
        verify(gcsUtils, never()).fetchRecordsFromGcs(any(), any(), any(), anyString(), anyString());
        metadata = getMetadata();
        assertThat(metadata.getJobIds()).isEmpty();
    }

}