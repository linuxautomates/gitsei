package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.models.database.cicd.JobRunSegment;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@SuppressWarnings("unused")
public class CiCdJobRunStageDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static String company = "test";
    private static DataSource dataSource;
    private static CiCdJobRunStageDatabaseService service;
    private static UserService usersService;
    private static CiCdInstancesDatabaseService instancesService;
    private static CiCdJobsDatabaseService jobsService;
    private static CiCdJobRunsDatabaseService jobRunsService;
    private static IntegrationService integrationService;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;

    private static final String instanceUrl = "http://my.com/";

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "CREATE SCHEMA IF NOT EXISTS test; "
        ).forEach(template::execute);
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
        instancesService = new CiCdInstancesDatabaseService(dataSource);
        instancesService.ensureTableExistence(company);
        jobsService = new CiCdJobsDatabaseService(dataSource);
        jobsService.ensureTableExistence(company);
        jobRunsService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunsService.ensureTableExistence(company);
        service = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        service.ensureTableExistence(company);
        triageRulesService = new TriageRulesService(dataSource);
        triageRulesService.ensureTableExistence(company);
        ciCdJobRunStageStepsDatabaseService = new CiCdJobRunStageStepsDatabaseService(dataSource);
        ciCdJobRunStageStepsDatabaseService.ensureTableExistence(company);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, DefaultObjectMapper.get());
        triageRuleHitsService.ensureTableExistence(company);

    }

    @Test
    public void test() throws SQLException {
        var result = insertStages(1).iterator().next();
        var stage = result.getLeft();
        var id = result.getRight();

        Assertions.assertThat(id).isNotBlank();

        var dbStage = service.get(company, id);

        Assertions.assertThat(dbStage.get().getChildJobRuns()).isNotEmpty();
        Assertions.assertThat(dbStage.get().getChildJobRuns()).containsExactlyElementsOf(stage.getChildJobRuns());

        var replaceSet = Set.<UUID>of();

        Assertions
            .assertThat(dbStage.get())
            .isEqualTo(stage.toBuilder().id(UUID.fromString(id)).description(stage.getDescription() == null ? "" : stage.getDescription()).build());
        
        Assertions.assertThat(dbStage.get().getUrl()).isEqualTo(stage.getUrl());
        
        var id2 = service.insert(company, stage);

        Assertions.assertThat(id2).isEqualTo(id);
    }

    @Test
    public void testList() throws SQLException {
        var stages = insertStages(20);
        var results = service.list(company, 0, 10);
        
        Assertions.assertThat(results.getCount()).isEqualTo(10);
        Assertions.assertThat(results.getTotalCount()).isGreaterThan(19);// 20 or more from other tests if not cleaning up the db
    }

    @Test
    public void testListFilter() throws SQLException {
        var stages = insertStages(20);
        var results = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("ok"))
            .build());
        
        Assertions.assertThat(results.getCount()).isEqualTo(10);
        Assertions.assertThat(results.getTotalCount()).isGreaterThan(19);// 20 or more from other tests if not cleaning up the db
    }

    @Test
    public void testListFilterMultiLevel() throws SQLException {

        var stages = insertStages(1);
        var stage = stages.iterator().next().getLeft();
        
        var rule = TriageRule.builder().application("a").owner("b").description("").name("name").regexes(List.of("hello")).build();
        var ruleId = triageRulesService.insert(company, rule);

        var stepId = ciCdJobRunStageStepsDatabaseService.insert(company, JobRunStageStep.builder()
                .stepId("1")
                .cicdJobRunStageId(stage.getId())
                .displayName("test")
                .displayDescription("testing")
                .result("success")
                .state("success")
                .duration(100)
                .startTime(Instant.now())
                .gcsPath("test")
                .build());
        var ruleHit = TriageRuleHit.builder()
                .ruleId(ruleId)
                .stageId(stage.getId().toString())
                .stepId(stepId)
                .jobRunId(stage.getCiCdJobRunId().toString())
                .hitContent("hitContent")
                .count(1)
                .context(Map.of("step", "s1", "line", "1"))
                .build();
        var ruleHitId = triageRuleHitsService.insert(company, ruleHit);

        var dbStage = service.get(company, stage.getId().toString(), false);

        Assertions.assertThat(dbStage.get()).isEqualTo(stage.toBuilder().description("").url(stage.getUrl() + "#step-s1-log-1").build());
        // Assertions.assertThat(results.getTotalCount()).isGreaterThan(19);// 20 or more from other tests if not cleaning up the db
    }

    @Test
    public void testListFilterByStageId() throws SQLException {
        var stages = insertStages(20);
        for(var pair:stages) {
            var results = service.list(company, 0, 10, QueryFilter.builder()
                .strictMatch("stage_ids", List.of(pair.getRight()))
                .build(),
                "start_time",
                SortingOrder.DESC
                );
            Assertions.assertThat(results.getTotalCount()).isEqualTo(1);
            Assertions.assertThat(results.getRecords()).containsAll(List.of(
                pair.getKey().toBuilder().description(pair.getKey().getDescription() == null ? "": pair.getKey().getDescription()).build()));
        }
    }

    @Test
    public void testListFilterAndSort() throws SQLException {
        var stages = insertStages(20);
        var results = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("OK"))
            .build(),
            "start_time",
            SortingOrder.DESC
            );
        var results2 = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("OK"))
            .build(),
            "start_time",
            SortingOrder.ASC
            );
        var results3 = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("OK"))
            .build(),
            "start_time",
            SortingOrder.DESC
            );
        Assertions.assertThat(results.getRecords().get(0)).isNotEqualTo(results2.getRecords().get(0));
        Assertions.assertThat(results.getRecords().get(0)).isEqualTo(results3.getRecords().get(0));

        UUID childJobRun = IterableUtils.getFirst(IterableUtils.getFirst(stages).orElse(null).getKey().getChildJobRuns()).orElse(null);
        Assertions.assertThat(childJobRun).isNotNull();
        var results5 = service.list(company, 0, 10, QueryFilter.builder()
                        .strictMatch("child_job_runs", List.of(childJobRun.toString()))
                        .build(),
                "start_time",
                SortingOrder.DESC
        );
        Assertions.assertThat(results5.getRecords()).hasSize(1);
    }

    @Test
    public void testListFilterAndSortWithChildren() throws SQLException {
        var stages = insertStages(20);
        var results = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("OK"))
            .build(),
            "start_time",
            SortingOrder.DESC,
            false
            );
        var results2 = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("OK"))
            .build(),
            "start_time",
            SortingOrder.ASC,
            false
            );
        Assertions.assertThat(results.getRecords().get(0)).isNotEqualTo(results2.getRecords().get(0));
        var results3 = service.list(company, 0, 10, QueryFilter.builder()
            .strictMatch("result", List.of("OK"))
            .build(),
            "start_time",
            SortingOrder.DESC,
            false
            );
        Assertions.assertThat(results.getRecords().get(0)).isEqualTo(results3.getRecords().get(0));

        UUID childJobRun = IterableUtils.getFirst(IterableUtils.getFirst(stages).orElse(null).getKey().getChildJobRuns()).orElse(null);
        Assertions.assertThat(childJobRun).isNotNull();
        var results5 = service.list(company, 0, 10, QueryFilter.builder()
                        .strictMatch("child_job_runs", List.of(childJobRun.toString()))
                        .build(),
                "start_time",
                SortingOrder.DESC
        );
        Assertions.assertThat(results5.getRecords()).hasSize(1);
    }

    private Set<Pair<JobRunStage, String>> insertStages(final int count) throws SQLException {
        Set<Pair<JobRunStage, String>> results = new HashSet<>();
            long epochMilli = 1598474059L;
        for(var i=0; i<count; i++){
            var instance = CICDInstance.builder()
                    .id(UUID.randomUUID())
                    .name("instance_" + i)
                    .url(instanceUrl)
                    .integrationId("1")
                    .type("type")
                    .build();
            var instanceId = UUID.fromString(instancesService.insert(company, instance));

            var job = CICDJob.builder()
                .cicdInstanceId(instanceId)
                .branchName("branchName")
                .jobFullName("Folder sample/jobs/my job_" + i)
                .jobName("jobName")
                .jobNormalizedFullName("jobNormalizedFullName")
                .moduleName("moduleName")
                .scmUrl("scmUrl")
                .scmUserId("scmUserId")
                .build();
            var jobId = UUID.fromString(jobsService.insert(company, job));

            var jobRun = CICDJobRun.builder()
                .cicdJobId(jobId)
                .cicdUserId("cicdUserId")
                .duration(10)
                .jobRunNumber(1L * (1+i))
                .scmCommitIds(List.of("as"))
                .startTime(Instant.ofEpochSecond(1000000000L))
                .build();
            var runId = UUID.fromString(jobRunsService.insert(company, jobRun));

            var stage = JobRunStage.builder()
                .ciCdJobRunId(runId)
                .stageId("stageId_" + i)
                .name("test_" + i)
                .description(null)
                .result("OK")
                .state("finished")
                .duration(10*(i+1))
                .logs("logs")
                .url("http://test")
                .startTime(Instant.ofEpochMilli(epochMilli - (1000L*i)))
                .fullPath(Set.of(PathSegment.builder().name("name").position(1).type(SegmentType.CICD_STAGE).id("id").build()))
                .childJobRuns(Set.of(UUID.randomUUID()))
                .build();
            
            var id = service.insert(company, stage);
            results.add(Pair.of(stage.toBuilder().id(UUID.fromString(id))
                .url(instanceUrl + "blue/organizations/jenkins/Folder%20sample%2Fmy%20job_" + i +"/detail/my%20job_" + i + "/" + jobRun.getJobRunNumber() + "/pipeline/stageId_" + i).build(), id));
        }
        return results;
    }

    private Set<Pair<JobRunStage, String>> insertStagesCustom(boolean onlyStage, JobRunStage stage) throws SQLException {
        Set<Pair<JobRunStage, String>> results = new HashSet<>();
        long epochMilli = 1598474059L;
        var jobRun = CICDJobRun.builder().jobRunNumber(123456L).build();

        for(var i=0; i<3; i++){
            if (!onlyStage) {
                var instance = CICDInstance.builder()
                        .id(UUID.randomUUID())
                        .name("instance_" + i)
                        .url(instanceUrl)
                        .integrationId("1")
                        .type("type")
                        .build();
                var instanceId = UUID.fromString(instancesService.insert(company, instance));

                var job = CICDJob.builder()
                        .cicdInstanceId(instanceId)
                        .branchName("branchName")
                        .jobFullName("Folder sample/jobs/my job_" + i)
                        .jobName("jobName")
                        .jobNormalizedFullName("jobNormalizedFullName")
                        .moduleName("moduleName")
                        .scmUrl("scmUrl")
                        .scmUserId("scmUserId")
                        .build();
                var jobId = UUID.fromString(jobsService.insert(company, job));

                jobRun = CICDJobRun.builder()
                        .cicdJobId(jobId)
                        .cicdUserId("cicdUserId")
                        .duration(10)
                        .jobRunNumber(1L * (1 + i))
                        .scmCommitIds(List.of("as"))
                        .startTime(Instant.ofEpochSecond(1000000000L))
                        .build();
                var runId = UUID.fromString(jobRunsService.insert(company, jobRun));

                stage = JobRunStage.builder()
                        .ciCdJobRunId(runId)
                        .stageId("stageId_" + i)
                        .name("test_" + i)
                        .description(null)
                        .result("OK")
                        .state("finished")
                        .duration(10 * (i + 1))
                        .logs("logs")
                        .url("http://test")
                        .startTime(Instant.ofEpochMilli(epochMilli - (1000L * i)))
                        .fullPath(Set.of(PathSegment.builder().name("name").position(1).type(SegmentType.CICD_STAGE).id("id").build()))
                        .childJobRuns(Set.of(UUID.randomUUID()))
                        .build();
            }


            var id = service.insert(company, stage);
            results.add(Pair.of(stage.toBuilder().id(UUID.fromString(id))
                    .url(instanceUrl + "blue/organizations/jenkins/Folder%20sample%2Fmy%20job_" + i +"/detail/my%20job_" + i + "/" + jobRun.getJobRunNumber() + "/pipeline/stageId_" + i).build(), id));
            if (onlyStage)
                break;
        }
        return results;
    }

    @Test
    public void testUpdateStages() throws SQLException {
        Set<Pair<JobRunStage,String>> stages = this.insertStagesCustom(false, null);

        List<JobRunStage> oldStages = new ArrayList<>();
        List<JobRunStage> newStages = new ArrayList<>();
        stages.stream().forEach(stagePair -> {
            JobRunStage stage = stagePair.getLeft();
            oldStages.add(stage);
            stage = stage.toBuilder().result("PASSED WITH WARNING")
                    .duration(300)
                    .startTime(Instant.now())
                    .fullPath(Set.of(PathSegment.builder().id("1").name("path1").build()))
                    .childJobRuns(Set.of(UUID.randomUUID(), UUID.randomUUID()))
                    .build();
            try {
                newStages.add(new ArrayList<>(this.insertStagesCustom(true, stage)).get(0).getLeft());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });

        List<String> resultOld = oldStages.stream().map(JobRunStage::getResult).collect(Collectors.toList());
        List<String> resultNew = newStages.stream().map(JobRunSegment::getResult).collect(Collectors.toList());

        Assert.assertNotEquals(oldStages.stream().map(JobRunStage::getResult).collect(Collectors.toList()), newStages.stream().map(JobRunSegment::getResult).collect(Collectors.toList()));
        Assert.assertNotEquals(oldStages.stream().map(JobRunStage::getDuration).collect(Collectors.toList()), newStages.stream().map(JobRunSegment::getDuration).collect(Collectors.toList()));
        Assert.assertNotEquals(oldStages.stream().map(JobRunStage::getStartTime).collect(Collectors.toList()), newStages.stream().map(JobRunSegment::getStartTime).collect(Collectors.toList()));
        Assert.assertNotEquals(oldStages.stream().map(JobRunStage::getFullPath).collect(Collectors.toList()), newStages.stream().map(JobRunSegment::getFullPath).collect(Collectors.toList()));
        Assert.assertNotEquals(oldStages.stream().map(JobRunStage::getChildJobRuns).collect(Collectors.toList()), newStages.stream().map(JobRunStage::getChildJobRuns).collect(Collectors.toList()));
    }

    @Test
    public void testFullNameForSimplePipeline() throws SQLException {
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
            .jobFullName("simple job")
            .jobName("simple job")
            .jobNormalizedFullName("simple job")
            .moduleName("my module")
            .scmUrl("scmUrl")
            .scmUserId("scmUserId")
            .build();
        var jobId = UUID.fromString(jobsService.insert(company, job));

        var jobRun = CICDJobRun.builder()
            .cicdJobId(jobId)
            .cicdUserId("cicdUserId")
            .duration(10)
            .jobRunNumber(911L)
            .scmCommitIds(List.of("commit1"))
            .startTime(Instant.ofEpochSecond(1558800000L))
            .build();
        var runId = UUID.fromString(jobRunsService.insert(company, jobRun));

        var stage = JobRunStage.builder()
            .ciCdJobRunId(runId)
            .stageId("23")
            .name("step23")
            .description(null)
            .result("OK")
            .state("finished")
            .duration(190)
            .logs("logs")
            .url("http://test")
            .startTime(Instant.ofEpochMilli(155889000L))
            .fullPath(Set.of(PathSegment.builder().id("id").name("name").position(1).type(SegmentType.CICD_STAGE).build()))
            .childJobRuns(Set.of())
            .build();
        
        var id = service.insert(company, stage);

        var url = service.getFullUrl(company, runId, stage.getStageId(), "");
        
        Assertions.assertThat(url).isEqualTo(instanceUrl + "blue/organizations/jenkins/simple%20job/detail/simple%20job/911/pipeline/23");
    }

    @Test
    public void testFullNameForPipelineInFolder() throws SQLException {
        var instance = CICDInstance.builder()
            .id(UUID.randomUUID())
            .name("simple instance")
            .url(instanceUrl)
            .type("type")
            .build();
        var instanceId = UUID.fromString(instancesService.insert(company, instance));

        var job = CICDJob.builder()
            .cicdInstanceId(instanceId)
            .branchName("branch1")
            .jobFullName("Folder1/pipeline 1")
            .jobName("pipeline 1")
            .jobNormalizedFullName("pipeline 1")
            .moduleName("my module")
            .scmUrl("scmUrl")
            .scmUserId("scmUserId")
            .build();
        var jobId = UUID.fromString(jobsService.insert(company, job));

        var jobRun = CICDJobRun.builder()
            .cicdJobId(jobId)
            .cicdUserId("cicdUserId")
            .duration(10)
            .jobRunNumber(201L)
            .scmCommitIds(List.of("commit1"))
            .startTime(Instant.ofEpochSecond(1558800000L))
            .build();
        var runId = UUID.fromString(jobRunsService.insert(company, jobRun));

        var stage = JobRunStage.builder()
            .ciCdJobRunId(runId)
            .stageId("24")
            .name("step24")
            .description(null)
            .result("OK")
            .state("finished")
            .duration(190)
            .logs("logs")
            .url("http://test")
            .startTime(Instant.ofEpochMilli(155889000L))
            .fullPath(Set.of(PathSegment.builder().name("name").position(1).type(SegmentType.CICD_STAGE).id("id").build()))
            .childJobRuns(Set.of(UUID.randomUUID(),UUID.randomUUID()))
            .build();
        
        var id = service.insert(company, stage);

        var url = service.getFullUrl(company, runId, stage.getStageId(), "");
        
        Assertions.assertThat(url).isEqualTo(instanceUrl + "blue/organizations/jenkins/Folder1%2Fpipeline%201/detail/pipeline%201/201/pipeline/24");
    }

    @Test
    public void testFullNameForMultibranchPipeline() throws SQLException {
        var instance = CICDInstance.builder()
                .id(UUID.randomUUID())
                .name("simple instance")
                .url(instanceUrl)
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
            .fullPath(Set.of(PathSegment.builder().name("name").position(1).type(SegmentType.CICD_STAGE).id("id").build()))
            .childJobRuns(Set.of(UUID.randomUUID(),UUID.randomUUID()))
            .build();
        
        var id = service.insert(company, stage);

        var url = service.getFullUrl(company, runId, stage.getStageId(), "");
        
        Assertions.assertThat(url).isEqualTo(instanceUrl + "blue/organizations/jenkins/multi%20branch%20project/detail/branch1/101/pipeline/25");
    }
}