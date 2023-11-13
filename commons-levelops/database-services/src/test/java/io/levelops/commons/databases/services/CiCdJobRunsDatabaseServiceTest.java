package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.databases.models.database.TriageRuleHit.RuleHitType;
import io.levelops.commons.databases.models.database.cicd.CICDJobRunCommits;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.database.cicd.SegmentType;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdJobRunsDatabaseServiceTest {
    private final static Random random = new Random();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserService userService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService dbService;
    private static CiCdJobRunStageDatabaseService cicdStages;
    private static CiCdJobRunStageStepsDatabaseService cicdSteps;
    private static TriageRulesService triageRulesService;
    private static TriageRuleHitsService triageRuleHitsService;
    private static String company = "test";
    private static IntegrationService integrationService;


    private static NamedParameterJdbcTemplate template;
    
    private static CICDInstance cicdInstance;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.of("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").forEach(template.getJdbcTemplate()::execute);
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        dbService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        cicdStages = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        cicdSteps = new CiCdJobRunStageStepsDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
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
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);
        cicdStages.ensureTableExistence(company);
        cicdSteps.ensureTableExistence(company);
        cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        triageRulesService = new TriageRulesService(dataSource);
        triageRulesService.ensureTableExistence(company);
        triageRuleHitsService = new TriageRuleHitsService(dataSource, DefaultObjectMapper.get());
        triageRuleHitsService.ensureTableExistence(company);
    }

    @Before
    public void resetData() {
        List.of(
            "DELETE FROM {0}.cicd_job_run_stage_steps",
            "DELETE FROM {0}.cicd_job_run_stages",
            "DELETE FROM {0}.cicd_job_runs;").stream()
            .map(statement -> MessageFormat.format(statement, company)).forEach(template.getJdbcTemplate()::execute);
    }

    private void verifyParams(List<CICDJobRun.JobRunParam> a, List<CICDJobRun.JobRunParam> e){
        Assert.assertEquals(CollectionUtils.isEmpty(e),CollectionUtils.isEmpty(a));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Map<String, CICDJobRun.JobRunParam> actualMap = a.stream().collect(Collectors.toMap(CICDJobRun.JobRunParam::getName, x -> x));
        Map<String, CICDJobRun.JobRunParam> expectedMap = e.stream().collect(Collectors.toMap(CICDJobRun.JobRunParam::getName, x -> x));
        for(String key : actualMap.keySet()){
            CICDJobRun.JobRunParam actualParam = actualMap.get(key);
            CICDJobRun.JobRunParam expectedParam = expectedMap.get(key);
            Assert.assertEquals(expectedParam.getName(), actualParam.getName());
            Assert.assertEquals(expectedParam.getType(), actualParam.getType());
            Assert.assertEquals(expectedParam.getValue(), actualParam.getValue());
        }
    }

    private void verifyRecord(CICDJobRun a, CICDJobRun e){
        Assert.assertEquals(e.getId(), a.getId());
        Assert.assertEquals(e.getCicdJobId(), a.getCicdJobId());
        Assert.assertEquals(e.getJobRunNumber(), a.getJobRunNumber());
        Assert.assertEquals(e.getStatus(), a.getStatus());
        Assert.assertEquals(e.getStartTime(), a.getStartTime());
        Assert.assertEquals(Integer.valueOf(Math.max(e.getDuration(), 0)), a.getDuration());
        Assert.assertEquals(e.getCicdUserId(), a.getCicdUserId());
        Assert.assertEquals(e.getMetadata(), a.getMetadata());
        Assert.assertEquals(e.getSource(), a.getSource());
        Assert.assertEquals(e.getReferenceId(), a.getReferenceId());
        Assert.assertEquals(CollectionUtils.isEmpty(e.getScmCommitIds()),CollectionUtils.isEmpty(a.getScmCommitIds()));
        if(CollectionUtils.isNotEmpty(e.getScmCommitIds())){
            Assert.assertEquals(e.getScmCommitIds().stream().collect(Collectors.toSet()), a.getScmCommitIds().stream().collect(Collectors.toSet()));
        }
        verifyParams(a.getParams(), e.getParams());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<CICDJobRun> a, List<CICDJobRun> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, CICDJobRun> actualMap = a.stream().collect(Collectors.toMap(CICDJobRun::getId, x -> x));
        Map<UUID, CICDJobRun> expectedMap = e.stream().collect(Collectors.toMap(CICDJobRun::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(CICDJobRun expected) throws SQLException {
        CICDJobRun actual = dbService.get(company, expected.getId().toString()).get();
        verifyRecord(actual,expected);
    }

    private CICDJobRun testDBInsert(CICDJobRun cicdJobRun, boolean skipTestGet) throws SQLException {
        String id = dbService.insert(company, cicdJobRun);
        Assert.assertNotNull(id);
        CICDJobRun expected = cicdJobRun.toBuilder().id(UUID.fromString(id)).build();
        if(!skipTestGet) {
            testGet(expected);
        }

        return expected;
    }
    private CICDJobRun testDBInsert(CICDJobRun cicdJobRun) throws SQLException {
        return testDBInsert(cicdJobRun, false);
    }
    private CICDJobRun testInsert(int i, CICDJob cicdJob) throws SQLException {
        List<CICDJobRun.JobRunParam> params = new ArrayList<>();
        for(int j=0; j<3; j++){
            CICDJobRun.JobRunParam param = CICDJobRun.JobRunParam.builder()
                    .type("string")
                    .name("name-" + j)
                    .value("value-" + j)
                    .build();
            params.add(param);
        }
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .jobRunNumber(Long.valueOf(i))
                .status("SUCCESS")
                .startTime(Instant.now())
                .duration(random.nextInt())
                .metadata(new HashMap<>())
                .cicdUserId("user-jenkins-" + i)
                .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                .referenceId(UUID.randomUUID().toString())
                .scmCommitIds(List.of("commit-id-1","commit-id-2","commit-id-3"))
                .params(params)
                .build();
        return testDBInsert(cicdJobRun);
    }

    private List<CICDJobRun> testInserts(CICDJob cicdJob) throws SQLException {
        List<CICDJobRun> cicdJobRuns = new ArrayList<>();
        for(int i=0; i< 5; i++){
            CICDJobRun cicdJobRun = testInsert(i, cicdJob);
            if(i%2 > 0) {
                cicdJobRun = cicdJobRun.toBuilder().triggers(Set.of(CICDJobTrigger.builder()
                        .buildNumber("10")
                        .id("TestJob1")
                        .type("UserCause")
                        .build()))
                    .build();
            }
            else {
                cicdJobRun = cicdJobRun.toBuilder().triggers(Set.of(CICDJobTrigger.builder()
                        .id("TestJob2")
                        .type("UpstreamCause")
                        .buildNumber("11")
                        .directParents(Set.of(CICDJobTrigger.builder().id("TestJob/Origin1").type("UserCause").buildNumber("2").build()))
                        .build()))
                    .build();
            }
            cicdJobRuns.add(cicdJobRun);
        }
        return cicdJobRuns;
    }

    private void testDuplicateInsert(List<CICDJobRun> expected) throws SQLException {
        List<CICDJobRun> duplicates = new ArrayList<>();
        for(CICDJobRun current : expected){
            CICDJobRun duplicate = testDBInsert(current.toBuilder().id(null).build());
            duplicates.add(duplicate);
        }
        Assert.assertEquals(expected.stream().map(CICDJobRun::getId).collect(Collectors.toSet()),duplicates.stream().map(CICDJobRun::getId).collect(Collectors.toSet()));
    }

    private void testDuplicateInsertWithPartialData(List<CICDJobRun> expected) throws SQLException {
        List<CICDJobRun> duplicates = new ArrayList<>();
        for(CICDJobRun current : expected){
            CICDJobRun jobRunPartialData = current.toBuilder().id(null).source(CICDJobRun.Source.JOB_RUN_COMPLETE_EVENT).referenceId(null).build();
            CICDJobRun duplicate = testDBInsert(jobRunPartialData, true);
            duplicates.add(duplicate);
        }
        Assert.assertEquals(expected.stream().map(CICDJobRun::getId).collect(Collectors.toSet()),duplicates.stream().map(CICDJobRun::getId).collect(Collectors.toSet()));
    }

    @Test
    public void testInsertMetadata() throws SQLException {
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);

        Map<String, Object> metadata = Map.of(
                "a", List.of("1", "2", "3"),
                "b", false);
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .jobRunNumber(1L)
                .ci(true)
                .cd(true)
                .metadata(metadata)
                .build();
        String runId = dbService.insert(company, cicdJobRun);

        CICDJobRun output = dbService.get(company, runId).orElse(null);

        assertThat(output).isNotNull();
        assertThat(output.getCd()).isEqualTo(true);
        assertThat(output.getCi()).isEqualTo(true);
        assertThat(output.getMetadata()).usingRecursiveComparison().isEqualTo(metadata);
    }

    private List<CICDJobRun> testList(List<CICDJobRun> expected) throws SQLException {
        DbListResponse<CICDJobRun> result = dbService.list(company, 0, 100);
        Assert.assertNotNull(result);
        assertThat(result.getTotalCount().intValue()).isGreaterThanOrEqualTo(expected.size());
        assertThat(result.getCount().intValue()).isGreaterThanOrEqualTo(expected.size());
        for (CICDJobRun jobRun:expected) {
            assertThat(jobRun.getTriggers()).isNotNull();
            var trigger = jobRun.getTriggers().iterator().next();
            assertThat(trigger.getType()).isNotBlank();
            if (jobRun.getTriggers().stream().filter(t -> "TestJob2".equalsIgnoreCase(trigger.getId())).findAny().isPresent()) {
                assertThat(trigger.getId()).isEqualTo("TestJob2");
                assertThat(trigger.getDirectParents()).isNotEmpty();
                assertThat(trigger.getDirectParents().iterator().next().getId()).isEqualTo("TestJob/Origin1");
            }
            else {
                assertThat(trigger.getId()).isEqualTo("TestJob1");
            }
        }
        verifyRecords(result.getRecords(), expected);
        return result.getRecords();
    }

    private List<CICDJobRun> testUpdate(List<CICDJobRun> expected) throws SQLException {
        List<CICDJobRun> allUpdated = new ArrayList<>();
        for(int i=0; i< expected.size(); i++){
            CICDJobRun current = expected.get(i);
            CICDJobRun updated = current.toBuilder()
                    .status("ABORTED")
                    .startTime(Instant.now())
                    .duration(random.nextInt())
                    .cicdUserId("user-jenkins-" + i + "-" + i)
                    .logGcspath("asd")
                    .metadata(Map.of("rollback", false,"env_ids", List.of("env-1")))
                    .build();
            Boolean success = dbService.update(company, updated);
            Assert.assertTrue(success);
            testGet(updated);
            allUpdated.add(updated);
        }
        testList(allUpdated);
        return allUpdated;
    }

    private void testDelete(List<CICDJobRun> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            CICDJobRun current = expected.get(0);
            Boolean success = dbService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testList(expected);
        }
        testList(expected);
    }

    private List<CICDJobRunCommits> populateExpectedJobRunCommits(CICDJob cicdJob) throws SQLException {
        return dbService.list(company, 0, 100).getRecords().stream()
                .map(r -> CICDJobRunCommits.builder().id(r.getId()).scmCommitIds(r.getScmCommitIds()).cicdJobId(cicdJob.getId()).jobScmUrl(cicdJob.getScmUrl()).jobRunUpdatedAt(r.getUpdatedAt()).build())
                .sorted((a,b) -> a.getJobRunUpdatedAt().compareTo(b.getJobRunUpdatedAt()))
                .collect(Collectors.toList());
    }
    @Test
    public void test() throws SQLException, InterruptedException {
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        List<CICDJobRun> expected = testInserts(cicdJob);
        testDuplicateInsert(expected);
        testList(expected);
        testDuplicateInsertWithPartialData(expected);
        testList(expected);
        expected = testUpdate(expected);
        testDelete(expected);

        DbListResponse<CICDJobRunCommits> result = dbService.getJobRunsCommits(company, Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli(), null, 0, 200);
        Assert.assertNotNull(result);

        List<CICDJobRunCommits> e = populateExpectedJobRunCommits(cicdJob);

        //Time Null
        Assert.assertEquals(e, dbService.getJobRunsCommitsV2(company, null, 0, 200));
        Assert.assertEquals(List.of(e.get(0)), dbService.getJobRunsCommitsV2(company, null, 0, 1));
        Assert.assertEquals(List.of(e.get(1)), dbService.getJobRunsCommitsV2(company, null, 1, 1));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, null, 2, 1)));

        //Time Before
        Long updatedAtStart = 0l;
        Assert.assertEquals(e, dbService.getJobRunsCommitsV2(company, updatedAtStart, 0, 200));
        Assert.assertEquals(List.of(e.get(0)), dbService.getJobRunsCommitsV2(company, updatedAtStart, 0, 1));
        Assert.assertEquals(List.of(e.get(1)), dbService.getJobRunsCommitsV2(company, updatedAtStart, 1, 1));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, updatedAtStart, 2, 1)));

        //Time After
        updatedAtStart = Instant.now().plus(365, ChronoUnit.DAYS).getEpochSecond();
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, updatedAtStart, 0, 200)));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, updatedAtStart, 0, 1)));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, updatedAtStart, 1, 1)));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, updatedAtStart, 2, 1)));

        List<CICDJobRun> jobRuns = dbService.list(company, 0, 10).getRecords();
        final UUID firstJobRunId = e.get(0).getId();
        final UUID secondJobRunId = e.get(1).getId();
        CICDJobRun firstJobRun = jobRuns.stream().filter(r -> r.getId().equals(firstJobRunId)).findFirst().get();
        CICDJobRun secondJobRun = jobRuns.stream().filter(r -> r.getId().equals(secondJobRunId)).findFirst().get();

        //Time Between
        updatedAtStart = firstJobRun.getUpdatedAt().getEpochSecond() + 1;
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        dbService.update(company, secondJobRun);
        e = populateExpectedJobRunCommits(cicdJob); //Job Run has been updated, timestamp has changed.

        //Test getJobRunsCommitsV2
        Assert.assertEquals(List.of(e.get(1)), dbService.getJobRunsCommitsV2(company, updatedAtStart, 0, 200));
        Assert.assertEquals(List.of(e.get(1)), dbService.getJobRunsCommitsV2(company, updatedAtStart, 0, 1));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsV2(company, updatedAtStart, 1, 1)));

        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsForCommitShas(company, null)));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsForCommitShas(company, List.of())));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsForCommitShas(company, List.of(""))));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-0"))));
        Assert.assertTrue(CollectionUtils.isEmpty(dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-4"))));

        Assert.assertEquals(e, dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-1")));
        Assert.assertEquals(e, dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-2")));
        Assert.assertEquals(e, dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-3")));
        Assert.assertEquals(e, dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-1", "commit-id-2")));
        Assert.assertEquals(e, dbService.getJobRunsCommitsForCommitShas(company, List.of("commit-id-1", "commit-id-2", "commit-id-3")));
    }

    @Test
    public void testListByStartTime() throws SQLException {
        DbListResponse<CICDJobRunCommits> result = dbService.getJobRunsCommits(company, Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli(), null, 0, 200);
        Assert.assertNotNull(result);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testTriageGridViewAggs() throws SQLException {

        var parentCicdJob = CICDJob.builder()
                .jobName("My Parent Job")
                .jobFullName("My Parent Job")
                .jobNormalizedFullName("My Parent Job")
                .projectName("My Project")
                .branchName("branchName")
                .moduleName("moduleName")
                .scmUrl("scmUrl")
                .scmUserId("scmUserId")
                .cicdInstanceId(cicdInstance.getId())
                .createdAt(Instant.ofEpochSecond(1599955200))
                .updatedAt(Instant.ofEpochSecond(1599955200))
                .build();
        var parentCicdJobId = UUID.fromString(ciCdJobsDatabaseService.insert(company, parentCicdJob));

        var parentJobRun = CICDJobRun.builder()
            .cicdJobId(parentCicdJobId)
            .cicdUserId("cicdUserId")
            .duration(12)
            .jobRunNumber(1L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status")
            .triggers(Set.of())
            .build();
        var parentCicdJobRunId = dbService.insert(company, parentJobRun);

        var cicdJob = CICDJob.builder()
                .jobName("My Job")
                .jobFullName("My Full Job")
                .jobNormalizedFullName("My Job")
                .projectName("My New Project")
                .branchName("branchName")
                .moduleName("moduleName")
                .scmUrl("scmUrl")
                .scmUserId("scmUserId")
                .cicdInstanceId(cicdInstance.getId())
                .createdAt(Instant.ofEpochSecond(1599955200))
                .updatedAt(Instant.ofEpochSecond(1599955200))
                .build();
        var cicdJobId = UUID.fromString(ciCdJobsDatabaseService.insert(company, cicdJob));

        var jobRun1 = CICDJobRun.builder()
            .cicdJobId(cicdJobId)
            .cicdUserId("cicdUserId1")
            .duration(12)
            .jobRunNumber(1L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status1")
            .triggers(Set.of(CICDJobTrigger.builder()
                .id("My Parent Job")
                .buildNumber("1")
                .type("UpstreamCause")
                .build()))
            .build();
        var cicdJobRunId1 = UUID.fromString(dbService.insert(company, jobRun1));

        var trigger = dbService.getJobRunTrigger(company, cicdJobRunId1);
        assertThat(trigger).isNotNull();
        assertThat(trigger.getId()).isEqualTo("My Parent Job");

        var jobRun2 = CICDJobRun.builder()
            .cicdJobId(cicdJobId)
            .cicdUserId("cicdUserId2")
            .duration(13)
            .jobRunNumber(2L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status1")
            .triggers(Set.of(CICDJobTrigger.builder()
                .id("My Parent Job")
                .buildNumber("1")
                .type("UpstreamCause")
                .build()))
            .build();
        var cicdJobRunId2 = UUID.fromString(dbService.insert(company, jobRun2));

        trigger = dbService.getJobRunTrigger(company, cicdJobRunId2);
        assertThat(trigger).isNotNull();
        assertThat(trigger.getId()).isEqualTo("My Parent Job");

        QueryFilter filters = QueryFilter.builder()
            .strictMatch("parent_job_ids", Set.of(parentCicdJobRunId))
            .build();
        var results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(0);

        var jobRun3 = CICDJobRun.builder()
            .cicdJobId(cicdJobId)
            .cicdUserId("cicdUserId3")
            .duration(13)
            .jobRunNumber(3L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status2")
            .triggers(Set.of(CICDJobTrigger.builder()
                .id("My Parent Job")
                .buildNumber("1")
                .type("UpstreamCause")
                .build()))
            .build();
        var cicdJobRunId3 = UUID.fromString(dbService.insert(company, jobRun3));

        trigger = dbService.getJobRunTrigger(company, cicdJobRunId3);
        assertThat(trigger).isNotNull();
        assertThat(trigger.getId()).isEqualTo("My Parent Job");

        filters = QueryFilter.builder()
            .strictMatch("parent_job_ids", Set.of(parentCicdJobId.toString()))
            .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results
            .getRecords()
                .stream()
                .map(item -> item.get("id"))
                .collect(Collectors.toList()))
            .containsExactlyElementsOf(List.of(cicdJobId.toString()));
        var aggs = (Map<String, Object>) ((List<Map<String, Object>>) results.getRecords().get(0).get("aggs")).get(0);
        assertThat(aggs.get("key").toString()).isEqualTo("1599868800");
        var totals = (Map<String, Integer>) aggs.get("totals");
        assertThat(totals.get("status1")).isEqualTo(2);
        assertThat(totals.get("status2")).isEqualTo(1);

        filters = QueryFilter.builder()
            .strictMatch("results", Set.of("status2"))
            .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results
            .getRecords()
                .stream()
                .map(item -> item.get("id"))
                .collect(Collectors.toList()))
            .containsExactlyElementsOf(List.of(cicdJobId.toString()));
        assertThat(results.getRecords().get(0).get("name")).isEqualTo("My Job");
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo("My Full Job");
        aggs = (Map<String, Object>) ((List<Map<String, Object>>) results.getRecords().get(0).get("aggs")).get(0);
        assertThat(aggs.get("key").toString()).isEqualTo("1599868800");
        totals = (Map<String, Integer>) aggs.get("totals");
        assertThat(totals.get("status2")).isEqualTo(1);

        filters = QueryFilter.builder()
            .strictMatch("job_ids", Set.of(cicdJobId.toString()))
            .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results
            .getRecords()
                .stream()
                .map(item -> item.get("id"))
                .collect(Collectors.toList()))
            .containsExactlyElementsOf(List.of(cicdJobId.toString()));
        assertThat(results.getRecords().get(0).get("name")).isEqualTo("My Job");
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo("My Full Job");
        aggs = (Map<String, Object>) ((List<Map<String, Object>>) results.getRecords().get(0).get("aggs")).get(0);
        assertThat(aggs.get("key").toString()).isEqualTo("1599868800");
        totals = (Map<String, Integer>) aggs.get("totals");
        assertThat(totals.get("status1")).isEqualTo(2);
        assertThat(totals.get("status2")).isEqualTo(1);

        filters = QueryFilter.builder()
            .strictMatch("job_ids", Set.of(parentCicdJobId.toString()))
            .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results
            .getRecords()
                .stream()
                .map(item -> item.get("id"))
                .collect(Collectors.toList()))
            .containsExactlyElementsOf(List.of(parentCicdJobId.toString()));
        assertThat(results.getRecords().get(0).get("name")).isEqualTo("My Parent Job");
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo("My Parent Job");
        aggs = (Map<String, Object>) ((List<Map<String, Object>>) results.getRecords().get(0).get("aggs")).get(0);
        assertThat(aggs.get("key").toString()).isEqualTo("1599868800");
        totals = (Map<String, Integer>) aggs.get("totals");
        assertThat(totals.get("status")).isEqualTo(1);

        var rule1 = TriageRule.builder()
            .createdAt(1623803969L)
            .description("description")
            .name("rule1")
            .regexes(List.of("test"))
            .application("application")
            .owner("owner")
            .build();
        var ruleId1 = UUID.fromString(triageRulesService.insert(company, rule1));


        var stage1 = JobRunStage.builder()
            .ciCdJobRunId(cicdJobRunId3)
            .description("description")
            .duration(5)
            .jobNumber("1")
            .logs("logs")
            .name("name")
            .stageId("1")
            .state("state")
            .startTime(Instant.now())
            .type(SegmentType.CICD_STAGE)
            .url("url")
            .result("result")
            .fullPath(Set.of())
            .childJobRuns(Set.of())
            .build();
        var stageId1 = UUID.fromString(cicdStages.insert(company, stage1));

        var step1 = JobRunStageStep.builder()
            .cicdJobRunStageId(stageId1)
            .createdAt(Instant.now())
            .duration(5)
            .gcsPath("gcsPath")
            .result("result")
            .state("state")
            .startTime(Instant.now())
            .stepId("stepId")
            .build();
        var stepId1 = UUID.fromString(cicdSteps.insert(company, step1));

        var ruleHit1 = TriageRuleHit.builder()
            .ruleId(ruleId1.toString())
            .jobRunId(cicdJobRunId3.toString())
            .stepId(stepId1.toString())
            .count(1)
            .context(Map.of())
            .hitContent("test")
            .type(RuleHitType.JENKINS)
            .build();
        triageRuleHitsService.insert(company, ruleHit1);

        filters = QueryFilter.builder()
            .strictMatch("triage_rule_ids", Set.of(ruleId1.toString()))
            .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(jobRun3.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(cicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(cicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("cicd_user_ids", Set.of("cicdUserId"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(parentJobRun.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(parentCicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(parentCicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("cicd_user_ids", Set.of("cicdUserId", "cicdUserId1", "cicdUserId2", "cicdUserId3"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(parentJobRun.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(parentCicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(parentCicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job", "My Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .strictMatch("job_names", Set.of("My Parent Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(parentJobRun.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(parentCicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(parentCicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("job_names", Set.of("My Parent Job", "My Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .strictMatch("project_names", Set.of("My Project"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(parentJobRun.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(parentCicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(parentCicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("project_names", Set.of("My Project", "My New Project"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();

        filters = QueryFilter.builder()
                .strictMatch("cicd_instance_ids", Set.of(cicdInstance.getId().toString()))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .strictMatch("project_names", Set.of("My Project", "My New Project"))
                .strictMatch("job_names", Set.of("My Parent Job", "My Job"))
                .strictMatch("cicd_user_ids", Set.of("cicdUserId", "cicdUserId1", "cicdUserId2", "cicdUserId3"))
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job", "My Job"))
                .strictMatch("cicd_instance_ids", Set.of(cicdInstance.getId().toString()))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .strictMatch("cicd_user_ids", Set.of("cicdUserId"))
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(parentJobRun.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(parentCicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(parentCicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("cicd_user_ids", Set.of("cicdUserId", "cicdUserId1", "cicdUserId2", "cicdUserId3"))
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job", "My Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .strictMatch("triage_rule_ids", Set.of(ruleId1.toString()))
                .strictMatch("cicd_user_ids", Set.of("cicdUserId", "cicdUserId1", "cicdUserId2", "cicdUserId3"))
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job", "My Job"))
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(jobRun3.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(cicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(cicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .partialMatch("job_name", "My")
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(2);

        filters = QueryFilter.builder()
                .partialMatch("job_full_name", "My Full Job")
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);

        filters = QueryFilter.builder()
                .strictMatch("triage_rule_ids", Set.of(ruleId1.toString()))
                .strictMatch("cicd_user_ids", Set.of("cicdUserId", "cicdUserId1", "cicdUserId2", "cicdUserId3"))
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job", "My Job"))
                .partialMatch("job_name", "My")
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results.getRecords().get(0).get("id")).isEqualTo(jobRun3.getCicdJobId().toString());
        assertThat(results.getRecords().get(0).get("name")).isEqualTo(cicdJob.getJobName());
        assertThat(results.getRecords().get(0).get("full_name")).isEqualTo(cicdJob.getJobFullName());

        filters = QueryFilter.builder()
                .strictMatch("triage_rule_ids", Set.of(ruleId1.toString()))
                .strictMatch("cicd_user_ids", Set.of("cicdUserId", "cicdUserId1", "cicdUserId2", "cicdUserId3"))
                .strictMatch("job_normalized_full_names", Set.of("My Parent Job", "My Job"))
                .partialMatch("job_name", "invalid")
                .build();
        results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(0);
    }

    @Test 
    public void testGetSubjobs() throws SQLException {
        var job1 = CICDJob.builder()
        .jobName("My Job1")
        .jobFullName("My Job1")
        .jobNormalizedFullName("My Job1")
        .branchName("branchName")
        .moduleName("moduleName")
        .scmUrl("scmUrl")
        .scmUserId("scmUserId")
        .cicdInstanceId(cicdInstance.getId())
        .createdAt(Instant.ofEpochSecond(1599955200))
        .updatedAt(Instant.ofEpochSecond(1599955200))
        .build();
        var jobId1 = UUID.fromString(ciCdJobsDatabaseService.insert(company, job1));

        var job2 = CICDJob.builder()
        .jobName("My Job2")
        .jobFullName("My Job2")
        .jobNormalizedFullName("My Job2")
        .branchName("branchName")
        .moduleName("moduleName")
        .scmUrl("scmUrl")
        .scmUserId("scmUserId")
        .cicdInstanceId(cicdInstance.getId())
        .createdAt(Instant.ofEpochSecond(1599955200))
        .updatedAt(Instant.ofEpochSecond(1599955200))
        .build();
        var jobId2 =  UUID.fromString(ciCdJobsDatabaseService.insert(company, job2));

        var job3a = CICDJob.builder()
        .jobName("My Job3a")
        .jobFullName("My Job3a")
        .jobNormalizedFullName("My Job3a")
        .branchName("branchName")
        .moduleName("moduleName")
        .scmUrl("scmUrl")
        .scmUserId("scmUserId")
        .cicdInstanceId(cicdInstance.getId())
        .createdAt(Instant.ofEpochSecond(1599955200))
        .updatedAt(Instant.ofEpochSecond(1599955200))
        .build();
        var jobId3a = UUID.fromString(ciCdJobsDatabaseService.insert(company, job3a));

        var job3b = CICDJob.builder()
        .jobName("My Job3b")
        .jobFullName("My Job3b")
        .jobNormalizedFullName("My Job3b")
        .branchName("branchName")
        .moduleName("moduleName")
        .scmUrl("scmUrl")
        .scmUserId("scmUserId")
        .cicdInstanceId(cicdInstance.getId())
        .createdAt(Instant.ofEpochSecond(1599955200))
        .updatedAt(Instant.ofEpochSecond(1599955200))
        .build();
        var jobId3b = UUID.fromString(ciCdJobsDatabaseService.insert(company, job3b));

        var jobRun1 = CICDJobRun.builder()
            .cicdJobId(jobId1)
            .cicdUserId("cicdUserId")
            .duration(12)
            .jobRunNumber(1291L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status1")
            .triggers(Set.of())
            .build();
        var jobRunId1 = dbService.insert(company, jobRun1);

        var jobRun2 = CICDJobRun.builder()
            .cicdJobId(jobId2)
            .cicdUserId("cicdUserId")
            .duration(12)
            .jobRunNumber(1191L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status2")
            .triggers(Set.of(CICDJobTrigger.builder()
                .id("My Job1")
                .buildNumber("1291")
                .type("UpstreamCause")
                .build()))
            .build();
        var jobRunId2 = dbService.insert(company, jobRun2);

        var jobRun3a = CICDJobRun.builder()
            .cicdJobId(jobId3a)
            .cicdUserId("cicdUserId")
            .duration(12)
            .jobRunNumber(3L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status3")
            .triggers(Set.of(
                CICDJobTrigger.builder()
                    .id("My Job2")
                    .buildNumber("1191")
                    .type("UpstreamCause")
                    .build()
                    ))
            .build();
        var jobRunId3a = dbService.insert(company, jobRun3a);

        var jobRun3b = CICDJobRun.builder()
            .cicdJobId(jobId3b)
            .cicdUserId("cicdUserId")
            .duration(12)
            .jobRunNumber(4L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status4")
            .triggers(Set.of(
                CICDJobTrigger.builder()
                    .id("My Job2")
                    .buildNumber("1191")
                    .type("UpstreamCause")
                    .build()
                    ))
            .build();
        var jobRunId3b = dbService.insert(company, jobRun3b);

        var subJobs = dbService.getSubjobs(company, jobRunId1, null, null, 0, 10);
        assertThat(subJobs.getTotalCount()).isEqualTo(3); // should be 3
        subJobs.getRecords().forEach(subJob -> {
            if (subJob.getId().toString().equals(jobRunId2)) {
                assertThat(subJob.getFullPath().size()).isEqualTo(2);
            }
            if (subJob.getId().toString().equals(jobRunId3a)) {
                assertThat(subJob.getFullPath().size()).isEqualTo(3);
            }
            if (subJob.getId().toString().equals(jobRunId3b)) {
                assertThat(subJob.getFullPath().size()).isEqualTo(3);
            }
        });

        var subJobs2 = dbService.getSubjobs(company, jobRunId2, null, null, 0, 10);
        assertThat(subJobs2.getTotalCount()).isEqualTo(2); // should be 2
        subJobs2.getRecords().forEach(subJob -> {
            if (subJob.getId().toString().equals(jobRunId3a)) {
                assertThat(subJob.getFullPath().size()).isEqualTo(3);
            }
            if (subJob.getId().toString().equals(jobRunId3b)) {
                assertThat(subJob.getFullPath().size()).isEqualTo(3);
            }
        });

        var subJobs3a = dbService.getSubjobs(company, jobRunId3a, null, null, 0, 10);
        assertThat(subJobs3a.getTotalCount()).isEqualTo(0); // should be 0

        var subJobs3b = dbService.getSubjobs(company, jobRunId3b, null, null, 0, 10);
        assertThat(subJobs3b.getTotalCount()).isEqualTo(0); // should be 0

        var subJobsStatus1 = dbService.getSubjobs(company, jobRunId1, QueryFilter.builder().strictMatch("result", List.of("status1")).build(), null, 0, 10);
        assertThat(subJobsStatus1.getTotalCount()).isEqualTo(0); // should be 0

        var subJobsStatus2 = dbService.getSubjobs(company, jobRunId1, QueryFilter.builder().strictMatch("result", List.of("status2")).build(), null, 0, 10);
        assertThat(subJobsStatus2.getTotalCount()).isEqualTo(1); // should be 1
        subJobsStatus2.getRecords().forEach(subJob -> {
            assertThat(subJob.getFullPath().size()).isEqualTo(2);
        });
    }

    @Test
    public void getLogsTest() throws SQLException {
        var job1 = CICDJob.builder()
            .jobName("My Job For Logs")
            .jobFullName("My Job For Logs")
            .jobNormalizedFullName("My Job For Logs")
            .branchName("branchName1")
            .moduleName("moduleName1")
            .scmUrl("scmUrl1")
            .scmUserId("scmUserId1")
            .cicdInstanceId(cicdInstance.getId())
            .createdAt(Instant.ofEpochSecond(1599955200))
            .updatedAt(Instant.ofEpochSecond(1599955200))
            .build();
        var jobId1 = UUID.fromString(ciCdJobsDatabaseService.insert(company, job1));

        var jobRun1 = CICDJobRun.builder()
            .cicdJobId(jobId1)
            .cicdUserId("cicdUserId1")
            .duration(30)
            .jobRunNumber(11L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status0")
            .logGcspath("logGcspath")
            .triggers(Set.of())
            .build();
        var jobRunId1 = dbService.insert(company, jobRun1);

        var logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs).containsAll(Set.of("logGcspath"));

        var jobRunStage1 = JobRunStage.builder()
            .ciCdJobRunId(UUID.fromString(jobRunId1))
            .description("description")
            .duration(10)
            .jobNumber("jobNumber")
            .name("name")
            .stageId("stageId")
            .state("state")
            .result("result")
            .url("url")
            .startTime(Instant.now())
            .fullPath(Set.of())
            .childJobRuns(Set.of())
            .logs("stageLogs1")
            .build();

        var stageId1 = cicdStages.insert(company, jobRunStage1);

        logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs).containsAll(Set.of("logGcspath", "stageLogs1"));

        var jobRunStageStep1 = JobRunStageStep.builder()
            .cicdJobRunStageId(UUID.fromString(stageId1))
            .createdAt(Instant.now())
            .displayName("displayName")
            .displayDescription("displayDescription")
            .duration(5)
            .gcsPath("stepLogs1")
            .result("result")
            .state("state")
            .startTime(Instant.now())
            .stepId("stepId")
            .build();

        cicdSteps.insert(company, jobRunStageStep1);

        logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs).containsAll(Set.of("logGcspath", "stageLogs1", "stepLogs1"));
    }

    @Test
    public void getLogsOnlyStageTest() throws SQLException {
        var job1 = CICDJob.builder()
            .jobName("My Job For Logs")
            .jobFullName("My Job For Logs")
            .jobNormalizedFullName("My Job For Logs")
            .branchName("branchName1")
            .moduleName("moduleName1")
            .scmUrl("scmUrl1")
            .scmUserId("scmUserId1")
            .cicdInstanceId(cicdInstance.getId())
            .createdAt(Instant.ofEpochSecond(1599955200))
            .updatedAt(Instant.ofEpochSecond(1599955200))
            .build();
        var jobId1 = UUID.fromString(ciCdJobsDatabaseService.insert(company, job1));

        var jobRun1 = CICDJobRun.builder()
            .cicdJobId(jobId1)
            .cicdUserId("cicdUserId1")
            .duration(30)
            .jobRunNumber(11L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status0")
            .triggers(Set.of())
            .build();
        var jobRunId1 = dbService.insert(company, jobRun1);

        var logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs.size()).isEqualTo(0);

        var jobRunStage1 = JobRunStage.builder()
            .ciCdJobRunId(UUID.fromString(jobRunId1))
            .description("description")
            .duration(10)
            .jobNumber("jobNumber")
            .name("name")
            .stageId("stageId")
            .state("state")
            .result("result")
            .url("url")
            .startTime(Instant.now())
            .fullPath(Set.of())
            .childJobRuns(Set.of())
            .logs("stageLogs1")
            .build();

        var stageId1 = cicdStages.insert(company, jobRunStage1);

        logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs).containsAll(Set.of("stageLogs1"));

        var jobRunStageStep1 = JobRunStageStep.builder()
            .cicdJobRunStageId(UUID.fromString(stageId1))
            .createdAt(Instant.now())
            .displayName("displayName")
            .displayDescription("displayDescription")
            .duration(5)
            .result("result")
            .state("state")
            .startTime(Instant.now())
            .stepId("stepId")
            .build();

        cicdSteps.insert(company, jobRunStageStep1);

        logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs).containsAll(Set.of("stageLogs1"));
    }

    @Test
    public void getLogsOnlyStepsTest() throws SQLException {
        var job1 = CICDJob.builder()
            .jobName("My Job For Logs")
            .jobFullName("My Job For Logs")
            .jobNormalizedFullName("My Job For Logs")
            .branchName("branchName1")
            .moduleName("moduleName1")
            .scmUrl("scmUrl1")
            .scmUserId("scmUserId1")
            .cicdInstanceId(cicdInstance.getId())
            .createdAt(Instant.ofEpochSecond(1599955200))
            .updatedAt(Instant.ofEpochSecond(1599955200))
            .build();
        var jobId1 = UUID.fromString(ciCdJobsDatabaseService.insert(company, job1));

        var jobRun1 = CICDJobRun.builder()
            .cicdJobId(jobId1)
            .cicdUserId("cicdUserId1")
            .duration(30)
            .jobRunNumber(11L)
            .scmCommitIds(List.of())
            .startTime(Instant.ofEpochSecond(1599955200))
            .status("status0")
            .triggers(Set.of())
            .build();
        var jobRunId1 = dbService.insert(company, jobRun1);

        var logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs.size()).isEqualTo(0);

        var jobRunStage1 = JobRunStage.builder()
            .ciCdJobRunId(UUID.fromString(jobRunId1))
            .description("description")
            .duration(10)
            .jobNumber("jobNumber")
            .name("name")
            .stageId("stageId")
            .state("state")
            .result("result")
            .url("url")
            .startTime(Instant.now())
            .fullPath(Set.of())
            .childJobRuns(Set.of())
            .logs("")
            .build();

        var stageId1 = cicdStages.insert(company, jobRunStage1);

        logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs.size()).isEqualTo(0);

        var jobRunStageStep1 = JobRunStageStep.builder()
            .cicdJobRunStageId(UUID.fromString(stageId1))
            .createdAt(Instant.now())
            .displayName("displayName")
            .displayDescription("displayDescription")
            .duration(5)
            .gcsPath("stepLogs1")
            .result("result")
            .state("state")
            .startTime(Instant.now())
            .stepId("stepId")
            .build();

        cicdSteps.insert(company, jobRunStageStep1);

        logs = dbService.getLogs(company, jobRunId1);

        assertThat(logs).containsAll(Set.of("stepLogs1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetTriageAggsNullStartTime() throws SQLException {
        var parentCicdJob = CICDJob.builder()
                .jobName("My Parent Job")
                .jobFullName("My Parent Job")
                .jobNormalizedFullName("My Parent Job")
                .branchName("branchName")
                .moduleName("moduleName")
                .scmUrl("scmUrl")
                .scmUserId("scmUserId")
                .cicdInstanceId(cicdInstance.getId())
                .createdAt(Instant.ofEpochSecond(1599955200))
                .updatedAt(Instant.ofEpochSecond(1599955200))
                .build();
        var parentCicdJobId = UUID.fromString(ciCdJobsDatabaseService.insert(company, parentCicdJob));

        var parentJobRun = CICDJobRun.builder()
                .cicdJobId(parentCicdJobId)
                .cicdUserId("cicdUserId")
                .duration(12)
                .jobRunNumber(1L)
                .scmCommitIds(List.of())
                .startTime(Instant.ofEpochSecond(1599955200))
                .status("status")
                .triggers(Set.of())
                .build();
        dbService.insert(company, parentJobRun);

        var cicdJob = CICDJob.builder()
                .jobName("My Job")
                .jobFullName("My Job")
                .jobNormalizedFullName("My Job")
                .branchName("branchName")
                .moduleName("moduleName")
                .scmUrl("scmUrl")
                .scmUserId("scmUserId")
                .cicdInstanceId(cicdInstance.getId())
                .createdAt(Instant.ofEpochSecond(1599955200))
                .updatedAt(Instant.ofEpochSecond(1599955200))
                .build();
        var cicdJobId = UUID.fromString(ciCdJobsDatabaseService.insert(company, cicdJob));

        var jobRun1 = CICDJobRun.builder()
                .cicdJobId(cicdJobId)
                .cicdUserId("cicdUserId1")
                .duration(12)
                .jobRunNumber(1L)
                .scmCommitIds(List.of())
                .startTime(Instant.ofEpochSecond(1599955200))
                .status("status1")
                .triggers(Set.of(CICDJobTrigger.builder()
                        .id("My Parent Job")
                        .buildNumber("1")
                        .type("UpstreamCause")
                        .build()))
                .build();
        dbService.insert(company, jobRun1);

        var jobRun2 = CICDJobRun.builder()
                .cicdJobId(cicdJobId)
                .cicdUserId("cicdUserId2")
                .duration(13)
                .jobRunNumber(2L)
                .scmCommitIds(List.of())
                .startTime(Instant.ofEpochSecond(1599955200))
                .status("status1")
                .triggers(Set.of(CICDJobTrigger.builder()
                        .id("My Parent Job")
                        .buildNumber("1")
                        .type("UpstreamCause")
                        .build()))
                .build();
        dbService.insert(company, jobRun2);

        var jobRun3 = CICDJobRun.builder()
                .cicdJobId(cicdJobId)
                .cicdUserId("cicdUserId3")
                .duration(13)
                .jobRunNumber(3L)
                .scmCommitIds(List.of())
                .status("status2")
                .triggers(Set.of(CICDJobTrigger.builder()
                        .id("My Parent Job")
                        .buildNumber("1")
                        .type("UpstreamCause")
                        .build()))
                .build();
        dbService.insert(company, jobRun3);

        QueryFilter filters = QueryFilter.builder()
                .strictMatch("parent_job_ids", Set.of(parentCicdJobId.toString()))
                .build();
        DbListResponse<Map<String, Object>> results = dbService.getTriageGridAggs(company, filters, null, 0, 10);
        assertThat(results).isNotNull();
        assertThat(results.getRecords().size()).isEqualTo(1);
        assertThat(results
                .getRecords()
                .stream()
                .map(item -> item.get("id"))
                .collect(Collectors.toList()))
                .containsExactlyElementsOf(List.of(cicdJobId.toString()));
        var aggs = (Map<String, Object>) ((List<Map<String, Object>>) results.getRecords().get(0).get("aggs")).get(0);
        assertThat(aggs.get("key").toString()).isEqualTo("1599868800");
        var totals = (Map<String, Integer>) aggs.get("totals");
        assertThat(totals.get("status1")).isEqualTo(2);
        assertThat(totals.get("status2")).isNull();
    }

    @Test
    public void testOldestJobRunTime() throws SQLException {
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);

        Instant firstJobRun = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant secondJobRun = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant thirdJobRun = Instant.now();


        CICDJobRun cicdJobRun1 = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .startTime(firstJobRun)
                .jobRunNumber(1L)
                .build();

        CICDJobRun cicdJobRun2 = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .startTime(secondJobRun)
                .jobRunNumber(2L)
                .build();

        CICDJobRun cicdJobRun3 = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .startTime(thirdJobRun)
                .jobRunNumber(3L)
                .build();
        dbService.insert(company, cicdJobRun1);
        dbService.insert(company, cicdJobRun2);
        dbService.insert(company, cicdJobRun3);

        Long epochtime =  dbService.getOldestJobRunStartTimeInMillis(company, 1 );

        Assert.assertTrue(epochtime != null);
        Assert.assertEquals((firstJobRun.getEpochSecond())*1000, epochtime.longValue());
    }

    @Test
    public void testOldestJobRunTimeWithInvalidIntegration() throws SQLException, InterruptedException {
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);

        Instant firstJobRun = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant secondJobRun = Instant.now().minus(1, ChronoUnit.DAYS);
        Instant thirdJobRun = Instant.now();


        CICDJobRun cicdJobRun1 = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .startTime(firstJobRun).duration(60).endTime(firstJobRun.plus(60, ChronoUnit.SECONDS))
                .jobRunNumber(1L)
                .scmCommitIds(List.of())
                .build();

        CICDJobRun cicdJobRun2 = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .startTime(secondJobRun).duration(70).endTime(firstJobRun.plus(70, ChronoUnit.SECONDS))
                .jobRunNumber(2L)
                .scmCommitIds(List.of())
                .build();

        CICDJobRun cicdJobRun3 = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId()).duration(80).endTime(firstJobRun.plus(80, ChronoUnit.SECONDS))
                .startTime(thirdJobRun)
                .jobRunNumber(3L)
                .scmCommitIds(List.of())
                .build();

        Assert.assertTrue(dbService.getLatestUpdatedJobRun(company).isEmpty());

        String id = dbService.insert(company, cicdJobRun1);
        cicdJobRun1 = cicdJobRun1.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(dbService.getLatestUpdatedJobRun(company).get().getJobRunNumber(), cicdJobRun1.getJobRunNumber());

        id = dbService.insert(company, cicdJobRun2);
        cicdJobRun2 = cicdJobRun2.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(dbService.getLatestUpdatedJobRun(company).get().getJobRunNumber(), cicdJobRun2.getJobRunNumber());

        id = dbService.insert(company, cicdJobRun3);
        cicdJobRun3 = cicdJobRun3.toBuilder().id(UUID.fromString(id)).build();
        Assert.assertEquals(dbService.getLatestUpdatedJobRun(company).get().getJobRunNumber(), cicdJobRun3.getJobRunNumber());

        Long epochtime =  dbService.getOldestJobRunStartTimeInMillis(company, 2 );
        Assert.assertTrue(epochtime == null);

        dbService.update(company, cicdJobRun1);
        Assert.assertEquals(dbService.getLatestUpdatedJobRun(company).get().getJobRunNumber(), cicdJobRun1.getJobRunNumber());

        dbService.update(company, cicdJobRun2);
        Assert.assertEquals(dbService.getLatestUpdatedJobRun(company).get().getJobRunNumber(), cicdJobRun2.getJobRunNumber());

        dbService.update(company, cicdJobRun3);
        Assert.assertEquals(dbService.getLatestUpdatedJobRun(company).get().getJobRunNumber(), cicdJobRun3.getJobRunNumber());
    }

    @Test
    public void testUpdateJobCiCdUserId() throws SQLException {
        CICDJob job1 = CICDJob.builder()
                .jobName("job-1")
                .jobFullName("job-1")
                .jobNormalizedFullName("job/name")
                .projectName("project-1")
                .branchName("branchName")
                .moduleName("moduleName")
                .scmUrl("scmUrl")
                .scmUserId("UNKNOWN")
                .cicdInstanceId(cicdInstance.getId())
                .build();
        String jobId = ciCdJobsDatabaseService.insert(company, job1);
        CICDJobRun jobRun1 = CICDJobRun.builder()
                .cicdJobId(UUID.fromString(jobId))
                .duration(12)
                .jobRunNumber(1L)
                .scmCommitIds(List.of())
                .startTime(Instant.ofEpochSecond(1599955200))
                .status("completed")
                .triggers(Set.of())
                .build();
        String jobRunId = dbService.insert(company, jobRun1);
        int count = dbService.updateJobCiCdUserId(company, CICDJobRun.builder()
                .cicdUserId("cicdjobrun/user1")
                .jobRunNumber(1L)
                .cicdJobId(UUID.fromString(jobId))
                .build());
        Optional<CICDJobRun> updatedJob = dbService.get(company, jobRunId);
        Assert.assertNotNull(updatedJob);
        Assert.assertEquals(updatedJob.get().getCicdJobId().toString(), jobId);
        Assert.assertEquals(updatedJob.get().getCicdUserId(), "cicdjobrun/user1");
        Assert.assertEquals(count, 1);
    }
}