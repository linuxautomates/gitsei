package io.levelops.aggregations_shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedArtifact;
import io.levelops.commons.databases.models.database.cicd.DbCiCdPushedJobRunParam;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactMappingDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdPushedParamsDatabaseService;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService;
import io.levelops.commons.databases.services.DatabaseSchemaService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.TriageRuleHitsService;
import io.levelops.commons.databases.services.TriageRulesService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github_actions.models.GithubActionsEnrichedWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class GithubActionsAggHelperServiceTest {

    private static DataSource dataSource;
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static final String company = "test";
    private static String integrationId = "1";
    private static UUID instanceId;
    private static IntegrationTrackingService trackingService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private static CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    private static CiCdPushedArtifactsDatabaseService ciCdPushedArtifactsDatabaseService;
    private static CiCdPushedParamsDatabaseService ciCdPushedParamsDatabaseService;
    private static CiCdJobRunArtifactMappingDatabaseService ciCdJobRunArtifactMappingDatabaseService;
    private static CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings cicdArtifactCorrelationSettings;
    private static CicdJobRunArtifactCorrelationService cicdJobRunArtifactCorrelationService;
    private static GithubActionsAggHelperService githubActionsAggHelperService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static TriageRulesService triageRulesService;

    @BeforeClass
    public static void setup() throws SQLException {
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
                .application("github_actions")
                .name("github actions test")
                .status("enabled")
                .build());
        trackingService = new IntegrationTrackingService(dataSource);
        trackingService.ensureTableExistence(company);
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        instanceId = UUID.randomUUID();
        ciCdInstancesDatabaseService.insert(company, CICDInstance.builder()
                .id(instanceId)
                .integrationId("1")
                .name("github-actions-integration")
                .type(CICD_TYPE.github_actions.toString())
                .build());
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        triageRulesService = new TriageRulesService(dataSource);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, mapper);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(mapper, dataSource);
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, mapper);
        ciCdJobRunStageDatabaseService.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        triageRulesService.ensureTableExistence(company);
        triageRuleHitsService.ensureTableExistence(company);

        ciCdJobRunArtifactsDatabaseService = new CiCdJobRunArtifactsDatabaseService(dataSource, mapper);
        ciCdJobRunArtifactsDatabaseService.ensureTableExistence(company);
        ciCdPushedArtifactsDatabaseService = new CiCdPushedArtifactsDatabaseService(dataSource, ciCdJobRunArtifactsDatabaseService);
        ciCdPushedArtifactsDatabaseService.ensureTableExistence(company);
        ciCdPushedParamsDatabaseService = new CiCdPushedParamsDatabaseService(dataSource);
        ciCdPushedParamsDatabaseService.ensureTableExistence(company);

        ciCdJobRunArtifactMappingDatabaseService = new CiCdJobRunArtifactMappingDatabaseService(dataSource, mapper);
        ciCdJobRunArtifactMappingDatabaseService.ensureTableExistence(company);
        cicdArtifactCorrelationSettings = CicdJobRunArtifactCorrelationService.CicdArtifactCorrelationSettings.builder().build();
        cicdJobRunArtifactCorrelationService = new CicdJobRunArtifactCorrelationService(dataSource,ciCdJobRunArtifactMappingDatabaseService, cicdArtifactCorrelationSettings, ciCdJobRunArtifactsDatabaseService);

        githubActionsAggHelperService = new GithubActionsAggHelperService(ciCdJobsDatabaseService,
                ciCdJobRunsDatabaseService, ciCdJobRunStageDatabaseService, ciCdJobRunStageStepsDatabaseService,
                ciCdInstancesDatabaseService, ciCdPushedArtifactsDatabaseService, ciCdPushedParamsDatabaseService, cicdJobRunArtifactCorrelationService);
    }

    @Test
    public void testProcessGithubActionsWorkflowRun() throws IOException, SQLException {
        List<GithubActionsEnrichedWorkflowRun> enrichedWorkflowRuns = ResourceUtils.getResourceAsObject(
                "github_actions/github-actions-workflow-runs.json",
                mapper.getTypeFactory().constructParametricType(List.class, GithubActionsEnrichedWorkflowRun.class));
        insertPushedArtifactsPushedParams();
        List<String> artifactIds = new ArrayList<>();
        List<String> paramIds = new ArrayList<>();
        for (var enrichedWorkflowRun : enrichedWorkflowRuns) {
            try {
                githubActionsAggHelperService.processGithubActionsWorkflowRun(enrichedWorkflowRun, company, integrationId, artifactIds, paramIds);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        DbListResponse<CICDJob> cicdJobDbListResponse = ciCdJobsDatabaseService.list(company, 0, 100);
        DbListResponse<CICDJobRun> cicdJobRunDbListResponse = ciCdJobRunsDatabaseService.list(company, 0, 100);
        DbListResponse<JobRunStage> cicdJobRunStageDbListResponse = ciCdJobRunStageDatabaseService.list(company, 0, 100);
        DbListResponse<JobRunStageStep> cicdJobRunStageStepDbListResponse = ciCdJobRunStageStepsDatabaseService.list(company, 0, 100);

        Assert.assertEquals(2, artifactIds.size());
        Assert.assertEquals(2, paramIds.size());

        // Test CICDJobRun from insertIntoCiCdJobRuns()
        CICDJobRun ciCdJobRun = cicdJobRunDbListResponse.getRecords().get(0);
        GithubActionsWorkflowRun githubActionsWorkflowRun = enrichedWorkflowRuns.get(0).getWorkflowRun();
        Assert.assertEquals(githubActionsWorkflowRun.getRunNumber(), ciCdJobRun.getJobRunNumber());
        Assert.assertEquals(githubActionsWorkflowRun.getConclusion(), ciCdJobRun.getStatus());
        Assert.assertEquals(githubActionsWorkflowRun.getRunStartedAt().toInstant(), ciCdJobRun.getStartTime());
        Assert.assertEquals(githubActionsWorkflowRun.getUpdatedAt().toInstant(), ciCdJobRun.getEndTime());
        Assert.assertEquals(githubActionsWorkflowRun.getTriggeringActor().getLogin(), ciCdJobRun.getCicdUserId());
        Assert.assertEquals(List.of(githubActionsWorkflowRun.getHeadSha()), ciCdJobRun.getScmCommitIds());

        Assert.assertEquals(3, cicdJobDbListResponse.getRecords().size());
        Assert.assertEquals(3, cicdJobRunDbListResponse.getRecords().size());
        Assert.assertEquals(3, cicdJobRunStageDbListResponse.getRecords().size());
        Assert.assertEquals(24, cicdJobRunStageStepDbListResponse.getRecords().size());
        Assert.assertTrue(cicdJobRunStageDbListResponse.getRecords().stream().anyMatch(stage -> Objects.equals(stage.getResult(), "SUCCESS")));
        Assert.assertTrue(cicdJobRunStageStepDbListResponse.getRecords().stream().anyMatch(step -> Objects.equals(step.getResult(), "failure")));

        int cleanUpArtifactsCount = githubActionsAggHelperService.cleanUpPushedArtifactData(company, artifactIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        int cleanUpParamsCount = githubActionsAggHelperService.cleanUpPushedParamsData(company, paramIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        Assert.assertTrue(cleanUpArtifactsCount!=0);
        Assert.assertTrue(cleanUpParamsCount!=0);

        // Test to update
        GithubActionsEnrichedWorkflowRun enrichedWorkflowRun = enrichedWorkflowRuns.get(0).toBuilder()
                .workflowRun(enrichedWorkflowRuns.get(0).getWorkflowRun().toBuilder()
                        .updatedAt(new Date())
                        .conclusion("need_update_status_failed")
                        .build())
                .build();
        insertPushedArtifactsPushedParams();
        artifactIds = new ArrayList<>();
        paramIds = new ArrayList<>();
        try {
            githubActionsAggHelperService.processGithubActionsWorkflowRun(enrichedWorkflowRun, company, integrationId, artifactIds, paramIds);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        DbListResponse<CICDJobRun> updatedDbJobRuns = ciCdJobRunsDatabaseService.list(company, 0, 100);
        Assert.assertTrue(updatedDbJobRuns.getRecords().stream().anyMatch(cicdJobRun -> Objects.equals(cicdJobRun.getStatus(), "need_update_status_failed")));
        Assert.assertEquals(2, artifactIds.size());
        Assert.assertEquals(2, paramIds.size());

        cleanUpArtifactsCount = githubActionsAggHelperService.cleanUpPushedArtifactData(company, artifactIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        cleanUpParamsCount = githubActionsAggHelperService.cleanUpPushedParamsData(company, paramIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        Assert.assertTrue(cleanUpArtifactsCount!=0);
        Assert.assertTrue(cleanUpParamsCount!=0);
    }

    public void insertPushedArtifactsPushedParams() throws IOException {
        List<DbCiCdPushedArtifact> dbCiCdPushedArtifacts = ResourceUtils.getResourceAsObject(
                "github_actions/db-cicd-pushed-artifacts.json",
                mapper.getTypeFactory().constructParametricType(List.class, DbCiCdPushedArtifact.class));
        List<DbCiCdPushedJobRunParam> dbCiCdPushedJobRunParams = ResourceUtils.getResourceAsObject(
                "github_actions/db-cicd-pushed-params.json",
                mapper.getTypeFactory().constructParametricType(List.class, DbCiCdPushedJobRunParam.class));
        ciCdPushedArtifactsDatabaseService.insertPushedArtifacts(company, dbCiCdPushedArtifacts);
        ciCdPushedParamsDatabaseService.insertPushedJobRunParams(company, dbCiCdPushedJobRunParams);
    }
}
