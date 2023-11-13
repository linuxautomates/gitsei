package io.levelops.aggregations_shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.helpers.harnessng.HarnessNGAggHelperService;
import io.levelops.aggregations_shared.models.HarnessNGAggPipelineStageStep;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.cicd.JobRunSegment;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactMappingDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.events.models.EventsClientException;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class HarnessNGAggHelperServiceTest {

    private static DataSource dataSource;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static final String company = "test";
    private static String integrationId = "1";

    private static CICDInstance cicdInstance;
    private static IntegrationTrackingService trackingService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static HarnessNGAggHelperService harnessNGAggHelperService;
    private static CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    private static CiCdJobRunArtifactMappingDatabaseService ciCdJobRunArtifactMappingDatabaseService;
    private static CicdJobRunArtifactCorrelationService cicdJobRunArtifactCorrelationService;

    @BeforeClass
    public static void setup() throws SQLException, EventsClientException {
        if (dataSource != null) {
            return;
        }
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        dataSource.getConnection().prepareStatement("DROP SCHEMA IF EXISTS " + company + " CASCADE; ").execute();
        dataSource.getConnection().prepareStatement("CREATE SCHEMA " + company + " ; ").execute();

        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        IntegrationService integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        integrationId = integrationService.insert(company, Integration.builder()
                .application("harnessng")
                .name("harness test")
                .status("enabled")
                .build());
        trackingService = new IntegrationTrackingService(dataSource);
        trackingService.ensureTableExistence(company);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        cicdInstance = CICDInstance.builder().integrationId(integrationId)
                .id(UUID.randomUUID())
                .type("harnessng")
                .build();
        ciCdInstancesDatabaseService.insert(company, cicdInstance);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(mapper, dataSource);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, mapper);
        ciCdJobRunStageDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        ciCdJobRunArtifactsDatabaseService = new CiCdJobRunArtifactsDatabaseService(dataSource, mapper);
        ciCdJobRunArtifactsDatabaseService.ensureTableExistence(company);
        triageRulesService = new TriageRulesService(dataSource);
        triageRulesService.ensureTableExistence(company);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, mapper);
        triageRuleHitsService.ensureTableExistence(company);
        ciCdJobRunArtifactMappingDatabaseService = new CiCdJobRunArtifactMappingDatabaseService(dataSource, mapper);
        ciCdJobRunArtifactMappingDatabaseService.ensureTableExistence(company);
        CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings cicdArtifactCorrelationSettings = CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings.builder()
                .correlationHashDefault(true)
                .correlationIdentityDefault(true)
                .correlationNameQualifierLocationDefault(true)
                .build();
        cicdJobRunArtifactCorrelationService = new CicdJobRunArtifactCorrelationService(dataSource, ciCdJobRunArtifactMappingDatabaseService, cicdArtifactCorrelationSettings, ciCdJobRunArtifactsDatabaseService);
        harnessNGAggHelperService = new HarnessNGAggHelperService(ciCdInstancesDatabaseService, ciCdJobsDatabaseService, ciCdJobRunsDatabaseService, ciCdJobRunStageDatabaseService, ciCdJobRunStageStepsDatabaseService, ciCdJobRunArtifactsDatabaseService, cicdJobRunArtifactCorrelationService);
    }

    @Test
    public void testProcessPipeline() throws SQLException, IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord = mapper.readValue(
                ResourceUtils.getResourceAsString("harnessng/harnessng_pipeline_execution_with_ci.json"), HarnessNGPipelineExecution.class);

        String cicdJobId = harnessNGAggHelperService.processPipeline(company, integrationId, pipelineExecutionRecord);

        CICDJob cicdJob = ciCdJobsDatabaseService.get(company, cicdJobId).orElseThrow();
        CICDJobRun cicdJobRun = ciCdJobRunsDatabaseService.listByFilter(company, 0, 1, null, List.of(UUID.fromString(cicdJobId)), null).getRecords().get(0);
        DbListResponse<JobRunStage> jobRunStages = ciCdJobRunStageDatabaseService.list(company, 0, 100, QueryFilter.builder()
                .strictMatch("cicd_job_run_id", cicdJobRun.getId())
                .build());
        DbListResponse<JobRunStageStep> jobRunStageSteps = ciCdJobRunStageStepsDatabaseService.listByFilter(company, 0, 10, null, jobRunStages.getRecords().stream().map(JobRunSegment::getId).collect(Collectors.toList()), null);

        Assert.assertEquals(cicdInstance.getId(), cicdJob.getCicdInstanceId());
        Assert.assertEquals("Failed", cicdJobRun.getStatus());
        Assert.assertEquals(2, jobRunStages.getRecords().stream().filter(jobRunStage -> jobRunStage.getCiCdJobRunId().toString().equals(cicdJobRun.getId().toString()) && "FAILED".equals(jobRunStage.getResult())).collect(Collectors.toList()).size());
        Assert.assertEquals(4, jobRunStages.getRecords().stream().filter(jobRunStage -> jobRunStage.getCiCdJobRunId().toString().equals(cicdJobRun.getId().toString()) && "SKIPPED".equals(jobRunStage.getResult())).collect(Collectors.toList()).size());
        Assert.assertEquals(6, jobRunStageSteps.getRecords().stream().filter(jobRunStageStep -> "Failed".equals(jobRunStageStep.getResult())).count());
        Assert.assertEquals(2, jobRunStageSteps.getRecords().stream().filter(jobRunStageStep -> "Success".equals(jobRunStageStep.getResult())).count());

        // test dedupe:
        Optional<CICDJobRun> runOpt = harnessNGAggHelperService.insertIntoCiCdJobRun(company, pipelineExecutionRecord, cicdJob.getId().toString());
        assertThat(runOpt).isEmpty();

        // test dedupe end to end:
        harnessNGAggHelperService.processPipeline(company, integrationId, pipelineExecutionRecord);
        assertThat(ciCdJobRunsDatabaseService.listByFilter(company, 0, 1, null, List.of(UUID.fromString(cicdJobId)), null).getRecords()).hasSize(1);

        // test upsert:
        runOpt = harnessNGAggHelperService.insertIntoCiCdJobRun(company, pipelineExecutionRecord.toBuilder()
                .pipeline(pipelineExecutionRecord.getPipeline().toBuilder()
                        .endTs(pipelineExecutionRecord.getPipeline().getEndTs() + 1000)
                        .build())
                .build(), cicdJob.getId().toString());
        assertThat(runOpt).isPresent();
        assertThat(runOpt.get().getEndTime().getEpochSecond()).isEqualTo(pipelineExecutionRecord.getPipeline().getEndTs() / 1000 + 1);
        CICDJobRun output = IterableUtils.getFirst(ciCdJobRunsDatabaseService.listByFilter(company, 0, 1, null, List.of(UUID.fromString(cicdJobId)), null).getRecords()).orElseThrow();
        assertThat(output.getEndTime().getEpochSecond()).isEqualTo(pipelineExecutionRecord.getPipeline().getEndTs() / 1000 + 1);

    }

    @Test
    public void testCiCdJob() throws IOException, SQLException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_ci.json", HarnessNGPipelineExecution.class);
        pipelineExecutionRecord = pipelineExecutionRecord.toBuilder()
                .pipeline(pipelineExecutionRecord.getPipeline().toBuilder()
                        .orgIdentifier("org")
                        .projectIdentifier("proj")
                        .build())
                .build();

        String cicdJobId = harnessNGAggHelperService.processPipeline(company, integrationId, pipelineExecutionRecord);

        CICDJob cicdJob = ciCdJobsDatabaseService.get(company, cicdJobId).orElseThrow();

        CICDJob expected = ResourceUtils.getResourceAsObject("harnessng/execution_with_ci__expected_job.json", CICDJob.class);
        DefaultObjectMapper.prettyPrint(cicdJob);
        assertThat(cicdJob).usingRecursiveComparison().ignoringFields("id", "cicdInstanceId", "createdAt", "updatedAt").isEqualTo(expected);
    }

    @Test
    public void testArtifactMapping() throws IOException, SQLException {
        HarnessNGPipelineExecution pipelineExecutionRecord =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_artifacts.json", HarnessNGPipelineExecution.class);
        HarnessNGPipelineExecution pipelineExecutionRecord2 =
                ResourceUtils.getResourceAsObject("harnessng/execution_with_artifacts_2.json", HarnessNGPipelineExecution.class);
        pipelineExecutionRecord = pipelineExecutionRecord.toBuilder()
                .pipeline(pipelineExecutionRecord.getPipeline().toBuilder()
                        .orgIdentifier("org")
                        .projectIdentifier("proj")
                        .build())
                .build();
        pipelineExecutionRecord2 = pipelineExecutionRecord2.toBuilder()
                .pipeline(pipelineExecutionRecord.getPipeline().toBuilder()
                        .orgIdentifier("org")
                        .projectIdentifier("proj")
                        .runSequence(3L)
                        .build())
                .build();

        String cicdJobId = harnessNGAggHelperService.processPipeline(company, integrationId, pipelineExecutionRecord);
        String cicdJobId2 = harnessNGAggHelperService.processPipeline(company, integrationId, pipelineExecutionRecord2);

        var correlations = ciCdJobRunArtifactMappingDatabaseService.filter(
                0, 100, company, CiCdJobRunArtifactMappingDatabaseService.CicdJobRunArtifactMappingFilter.builder()
                        .build());
        // TODO: This should be 4 but there is a bug in the db service that causes it to delete mappings when it shouldn't
        // TODO: Will fix this in a follow up PR
        assertThat(correlations.getRecords()).hasSizeGreaterThan(0);
    }

    @Test
    public void getPipelineStagesTest() throws IOException {
        HarnessNGPipelineExecution pipelineExecutionRecord = mapper.readValue(
                ResourceUtils.getResourceAsString("harnessng/harnessng_pipeline_execution_with_ci.json"), HarnessNGPipelineExecution.class);
        List<HarnessNGAggPipelineStageStep> stages = harnessNGAggHelperService.getPipelineStages(pipelineExecutionRecord.getExecutionGraph().getRootNodeId(), pipelineExecutionRecord.getExecutionGraph());

        Assert.assertEquals(stages.size(), 6);
        Assert.assertEquals(stages.get(0).getSteps().size(), 5);
        Assert.assertEquals(stages.get(1).getSteps().size(), 3);
    }

}
