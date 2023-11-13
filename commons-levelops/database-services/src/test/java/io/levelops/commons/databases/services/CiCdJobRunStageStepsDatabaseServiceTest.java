package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdJobRunStageStepsDatabaseServiceTest {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private String company = "test";

    private DataSource dataSource;
    private UserService userService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private CiCdJobRunStageStepsDatabaseService dbService;
    private IntegrationService integrationService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        jobRunStageDatabaseService = new CiCdJobRunStageDatabaseService(dataSource, DefaultObjectMapper.get());
        dbService  = new CiCdJobRunStageStepsDatabaseService(dataSource);


        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
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
        ciCdJobRunsDatabaseService.ensureTableExistence(company);
        jobRunStageDatabaseService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);

    }

    private void verifyRecord(JobRunStageStep a, JobRunStageStep e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getCicdJobRunStageId(), e.getCicdJobRunStageId());
        Assert.assertEquals(a.getStepId(), e.getStepId());
        Assert.assertEquals(a.getDisplayName(), e.getDisplayName());
        Assert.assertEquals(a.getDisplayDescription(), e.getDisplayDescription());
        Assert.assertEquals(a.getStartTime(), e.getStartTime());
        Assert.assertEquals(a.getResult(), e.getResult());
        Assert.assertEquals(a.getState(), e.getState());
        Assert.assertEquals(a.getDuration(), e.getDuration());
        Assert.assertEquals(a.getGcsPath(), e.getGcsPath());
    }
    private void verifyRecords(List<JobRunStageStep> a, List<JobRunStageStep> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, JobRunStageStep> actualMap = a.stream().collect(Collectors.toMap(JobRunStageStep::getId, x -> x));
        Map<UUID, JobRunStageStep> expectedMap = e.stream().collect(Collectors.toMap(JobRunStageStep::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(JobRunStageStep expected) throws SQLException {
        Optional<JobRunStageStep> opt = dbService.get(company, expected.getId().toString());
        Assert.assertNotNull(opt);
        Assert.assertTrue(opt.isPresent());
        JobRunStageStep actual = opt.get();
        verifyRecord(actual, expected);
    }

    private JobRunStageStep testInsert(JobRunStage jobRunStage, int i) throws SQLException {
        JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                .cicdJobRunStageId(jobRunStage.getId())
                .stepId("step-" + i)
                .displayName("display-name-" + i)
                .displayDescription("display-description-" + i)
                .startTime(Instant.now())
                .result("result-" + i)
                .state("state-" + i)
                .duration(RANDOM.nextInt())
                .gcsPath("gcs-path-" + i)
                .build();
        String id = dbService.insert(company, jobRunStageStep);
        Assert.assertNotNull(id);
        jobRunStageStep = jobRunStageStep.toBuilder().id(UUID.fromString(id)).build();

        testGet(jobRunStageStep);
        return jobRunStageStep;
    }

    private List<JobRunStageStep> testInserts(List<JobRunStage> jobRunStages) throws SQLException {
        List<JobRunStageStep> jobRunStageSteps = new ArrayList<>();
        for(int i=0; i< jobRunStages.size(); i++) {
            jobRunStageSteps.add(testInsert(jobRunStages.get(i), i));
        }
        return jobRunStageSteps;
    }

    private void testListId(List<JobRunStageStep> expected) throws SQLException {
        for(JobRunStageStep step : expected) {
            DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, List.of(step.getId()), null, null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), step);
        }
        List<UUID> ids = expected.stream().map(JobRunStageStep::getId).collect(Collectors.toList());
        DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, ids, null, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListCiCdJobRunStageId(List<JobRunStageStep> expected) throws SQLException {
        for(JobRunStageStep step : expected) {
            DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, null, List.of(step.getCicdJobRunStageId()),  null);
            Assert.assertNotNull(dbListResponse);
            Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), step);
        }
        List<UUID> cicdJobRunStageIds = expected.stream().map(JobRunStageStep::getCicdJobRunStageId).collect(Collectors.toList());
        DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, null, cicdJobRunStageIds, null);
        Assert.assertNotNull(dbListResponse);
        Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListStepId(List<JobRunStageStep> expected) throws SQLException {
        for(JobRunStageStep step : expected) {
            DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, null,  null, List.of(step.getStepId()));
            Assert.assertNotNull(dbListResponse);
            Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), step);
        }
        List<String> stepIds = expected.stream().map(JobRunStageStep::getStepId).collect(Collectors.toList());
        DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, null, null, stepIds);
        Assert.assertNotNull(dbListResponse);
        Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testListCiCdJobRunStageIdAndStepId(List<JobRunStageStep> expected) throws SQLException {
        for(JobRunStageStep step : expected) {
            DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, null, List.of(step.getCicdJobRunStageId()),  List.of(step.getStepId()));
            Assert.assertNotNull(dbListResponse);
            Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), step);
        }
        List<UUID> cicdJobRunStageIds = expected.stream().map(JobRunStageStep::getCicdJobRunStageId).collect(Collectors.toList());
        List<String> stepIds = expected.stream().map(JobRunStageStep::getStepId).collect(Collectors.toList());
        DbListResponse<JobRunStageStep> dbListResponse = dbService.listByFilter(company, 0, 300, null, cicdJobRunStageIds, stepIds);
        Assert.assertNotNull(dbListResponse);
        Assert.assertTrue(CollectionUtils.isNotEmpty(dbListResponse.getRecords()));
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }

    private void testList(List<JobRunStageStep> expected) throws SQLException {
        testListId(expected);
        testListCiCdJobRunStageId(expected);
        testListStepId(expected);
        testListCiCdJobRunStageIdAndStepId(expected);
    }

    private JobRunStageStep testUpdate(JobRunStageStep expected, int i) throws SQLException {
        JobRunStageStep jobRunStageStep = expected.toBuilder()
                .displayName(expected.getDisplayName() + "-" + i)
                .displayDescription(expected.getDisplayDescription() + "-" + i)
                .startTime(Instant.now())
                .result(expected.getResult() + "-" + i)
                .state(expected.getState() + "-" + i)
                .duration(RANDOM.nextInt())
                .gcsPath(expected.getGcsPath() + "-" + i)
                .build();
        boolean success = dbService.update(company, jobRunStageStep);
        Assert.assertTrue(success);
        testGet(jobRunStageStep);
        return jobRunStageStep;
    }

    private List<JobRunStageStep> testUpdates(List<JobRunStageStep> expected) throws SQLException {
        List<JobRunStageStep> updated = new ArrayList<>();
        for(int i=0; i< expected.size(); i++) {
            JobRunStageStep updatedStep = testUpdate(expected.get(i), i);
            updated.add(updatedStep);
        }
        return updated;
    }

    private void testValidDelete(List<JobRunStageStep> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            JobRunStageStep current = expected.get(0);
            Boolean success = dbService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testList(expected);
        }
        testList(expected);
    }

    private void testInvalidDelete() throws SQLException {
        Boolean success = dbService.delete(company, UUID.randomUUID().toString());
        Assert.assertFalse(success);
    }

    private void testDelete(List<JobRunStageStep> expected) throws SQLException {
        testInvalidDelete();
        testValidDelete(expected);
    }

    @Test
    public void test() throws SQLException {
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        CICDJobRun cicdJobRun = CICDJobRunUtils.createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, 0, Instant.now(), 1234, null, null);
        List<JobRunStage> jobRunStages = new ArrayList<>();
        for(int i=0; i < 5; i++) {
            jobRunStages.add(JobRunStageUtils.createJobRunStage(jobRunStageDatabaseService, cicdJobRun, company, i));
        }
        List<JobRunStageStep> jobRunStageSteps = testInserts(jobRunStages);
        Assert.assertTrue(CollectionUtils.isNotEmpty(jobRunStageSteps));

        testList(jobRunStageSteps);

        jobRunStageSteps = testUpdates(jobRunStageSteps);
        testList(jobRunStageSteps);

        testDelete(jobRunStageSteps);
    }

}