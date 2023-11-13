package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.models.database.TriageRuleHit.RuleHitType;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TriageRuleHitsServiceTest {

    @ClassRule
    public static final SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private final static String company = "test";
    private static CiCdInstancesDatabaseService instancesService;
    private static CiCdJobsDatabaseService jobsService;
    private static CiCdJobRunsDatabaseService jobRunsService;
    private static CiCdJobRunStageDatabaseService jobRunStageService;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService service;
    private static UserService usersService;
    private static ProductService productService;
    private static DataSource dataSource;
    private static NamedParameterJdbcTemplate template;
    private static IntegrationService integrationService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",

                "CREATE SCHEMA IF NOT EXISTS " + company
        ).forEach(template.getJdbcTemplate()::execute);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        Integration integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration);
        usersService = new UserService(dataSource, DefaultObjectMapper.get());
        usersService.ensureTableExistence(company);
        productService = new ProductService(dataSource);
        productService.ensureTableExistence(company);
        instancesService = new CiCdInstancesDatabaseService(dataSource);
        instancesService.ensureTableExistence(company);
        jobsService = new CiCdJobsDatabaseService(dataSource);
        jobsService.ensureTableExistence(company);
        jobRunsService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunsService.ensureTableExistence(company);
        jobRunStageService = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        jobRunStageService.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        triageRulesService = new TriageRulesService(dataSource);
        triageRulesService.ensureTableExistence(company);
        service = new TriageRuleHitsService(dataSource, DefaultObjectMapper.get());
        service.ensureTableExistence(company);

    }

    @Test
    public void test() throws SQLException {
        var instanceUrl = "http://test";
        var rule = TriageRule.builder()
                .name("name")
                .regexes(List.of("hello"))
                .application("asd")
                .owner("asb")
                .description("")
                .build();
        var ruleId = triageRulesService.insert(company, rule);

        var instance = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("simple instance")
                .url(instanceUrl)
                .integrationId("1")
                .type("type")
                .build();
        var instanceId = UUID.fromString(instancesService.insert(company, instance));

        var job = CICDJob.builder()
                .cicdInstanceId(instanceId)
                .branchName("branch1")
                .jobFullName("multi branch project/branches/branch1")
                .jobName("branch1")
                .jobNormalizedFullName("branch1")
                .moduleName("my module")
                .scmUrl("scmUrl")
                .scmUserId("scmUserId")
                .build();
        var jobId = UUID.fromString(jobsService.insert(company, job));

        var jobRun = CICDJobRun.builder()
                .cicdJobId(jobId)
                .cicdUserId("cicdUserId")
                .duration(10)
                .jobRunNumber(101L)
                .scmCommitIds(List.of("commit1"))
                .startTime(Instant.ofEpochSecond(1558800000L))
                .build();
        var runId = UUID.fromString(jobRunsService.insert(company, jobRun));

        var stage = JobRunStage.builder()
                .ciCdJobRunId(runId)
                .stageId("25")
                .name("step25")
                .description(null)
                .result("OK")
                .state("finished")
                .duration(190)
                .logs("logs")
                .url("http://test")
                .startTime(Instant.ofEpochMilli(155889000L))
                .fullPath(Set.of(PathSegment.builder().id("id").name("name").position(1).type(SegmentType.CICD_STAGE).build()))
                .childJobRuns(Set.of(UUID.randomUUID()))
                .build();
        var stageId = jobRunStageService.insert(company, stage);
        var stepId1 = ciCdJobRunStageStepsDatabaseService.insert(company, JobRunStageStep.builder()
                .stepId("1")
                .cicdJobRunStageId(UUID.fromString(stageId))
                .displayName("test")
                .displayDescription("testing")
                .result("success")
                .state("success")
                .duration(100)
                .startTime(Instant.now())
                .gcsPath("test")
                .build());
        var stepId2 = ciCdJobRunStageStepsDatabaseService.insert(company, JobRunStageStep.builder()
                .stepId("2")
                .cicdJobRunStageId(UUID.fromString(stageId))
                .displayName("test")
                .displayDescription("testing")
                .result("success")
                .state("success")
                .duration(100)
                .startTime(Instant.now())
                .gcsPath("test")
                .build());
        var ruleHits = List.of(TriageRuleHit.builder()
                        .count(1)
                        .jobRunId(runId.toString())
                        .ruleId(ruleId)
                        .stageId(stageId)
                        .stepId(stepId1)
                        .hitContent("test")
                        .type(RuleHitType.JENKINS)
                        .context(Map.of("step", "1"))
                        .build(),
                TriageRuleHit.builder()
                        .count(1)
                        .jobRunId(runId.toString())
                        .ruleId(ruleId)
                        .stageId(stageId)
                        .stepId(stepId2)
                        .hitContent("test")
                        .type(RuleHitType.JENKINS)
                        .context(Map.of("step", "1"))
                        .build()
        );
        String ruleHitId = "";
        for (var ruleHit : ruleHits) {
            ruleHitId = service.insert(company, ruleHit);
        }
        var dbHit = service.get(company, ruleHitId);
        Assertions.assertThat(dbHit.get().toBuilder().createdAt(null).build())
                .isEqualTo(ruleHits.get(1).toBuilder().id(ruleHitId).build());

        DbListResponse<TriageRuleHit> response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of("5a6a4223-35da-49ce-9ebe-eaf3f61a8b8e"), List.of(), List.of(), List.of(), List.of(),  0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(0);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of(stepId1), List.of(), List.of(), List.of(), List.of(),  0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(runId.toString()), List.of(stageId), List.of(stepId1, stepId2), List.of(ruleId),
                List.of("branch1"), List.of("branch1"), List.of(instanceId.toString()), 0, 5);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(2);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(runId.toString()), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(stageId), List.of(), List.of(), List.of(), List.of(), List.of(), 0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of(stepId1, stepId2), List.of(), List.of(), List.of(), List.of(), 0, 2);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(2);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of(), List.of(ruleId), List.of(), List.of(), List.of(),  0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of(), List.of(), List.of("branch1"), List.of(), List.of(),  0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of("branch1"), List.of(),  0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);

        response = service.listJenkinsRuleHits(company, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(instanceId.toString()),  0, 1);
        Assertions.assertThat(response.getRecords().size()).isEqualTo(1);
    }
}