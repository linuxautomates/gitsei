package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdJobsDatabaseServiceTest {
    private static final Boolean UPSERT = Boolean.TRUE;
    private static final Boolean INSERT_ONLY = Boolean.FALSE;
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private DataSource dataSource;
    private UserService userService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private CiCdJobsDatabaseService dbService;
    private String company = "test";
    private IntegrationService integrationService;
    private Integration integration;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        integrationService = new IntegrationService(dataSource);
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        dbService = new CiCdJobsDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService.ensureTableExistence(company);
        integration = Integration.builder()
                .id("1")
                .name("name")
                .url("http")
                .status("good")
                .application("jira")
                .description("desc")
                .satellite(true)
                .build();
        integrationService.insert(company, integration);
        userService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);

    }

    private void verifyRecord(CICDJob a, CICDJob e) {
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getJobName(), e.getJobName());
        Assert.assertEquals(a.getJobFullName(), e.getJobFullName());
        Assert.assertEquals(a.getJobNormalizedFullName(), e.getJobNormalizedFullName());
        Assert.assertEquals(a.getScmUrl(), e.getScmUrl());
        Assert.assertEquals(a.getScmUserId(), e.getScmUserId());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }

    private void verifyRecords(List<CICDJob> a, List<CICDJob> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, CICDJob> actualMap = a.stream().collect(Collectors.toMap(CICDJob::getId, x -> x));
        Map<UUID, CICDJob> expectedMap = e.stream().collect(Collectors.toMap(CICDJob::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(CICDJob expected) throws SQLException {
        CICDJob actual = dbService.get(company, expected.getId().toString()).get();
        verifyRecord(actual, expected);
    }

    private CICDJob testInsert(int i, UUID cicdInstanceId, boolean upsert) throws SQLException {
        String jobName = "jobname-" + i;
        String branchName = "branch-name-" + i;
        CICDJob.CICDJobBuilder bldr = CICDJob.builder()
                .jobName(jobName)
                .jobFullName(jobName + "/branches/" + branchName)
                .jobNormalizedFullName(jobName + "/" + branchName)
                .branchName(branchName)
                .moduleName("module-name" + i)
                .cicdInstanceId(cicdInstanceId);
        if (upsert) {
            bldr.scmUrl("url-" + i)
                    .scmUserId("user-git-" + i);
        }
        CICDJob cicdJob = bldr.build();
        String id = null;
        if (upsert) {
            id = dbService.insert(company, cicdJob);
        } else {
            id = dbService.insertOnly(company, cicdJob);
        }
        Assert.assertNotNull(id);
        CICDJob expected = cicdJob.toBuilder().id(UUID.fromString(id)).build();
        if (upsert) {
            testGet(expected);
        }
        return expected;
    }

    private List<CICDJob> testInserts(List<CICDInstance> cicdInstances, boolean upsert) throws SQLException {
        List<CICDJob> cicdJobs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            UUID cicdInstanceId = null;
            if (CollectionUtils.isNotEmpty(cicdInstances)) {
                cicdInstanceId = cicdInstances.get(i).getId();
            }
            CICDJob cicdJob = testInsert(i, cicdInstanceId, upsert);
            cicdJobs.add(cicdJob);
        }
        return cicdJobs;
    }

    private List<CICDJob> testUpsert(List<CICDInstance> cicdInstances, List<CICDJob> expected) throws SQLException {
        List<CICDJob> updatedJobs = testInserts(cicdInstances, UPSERT);
        Assert.assertEquals(expected.stream().map(CICDJob::getId).collect(Collectors.toSet()), updatedJobs.stream().map(CICDJob::getId).collect(Collectors.toSet()));

        testList(updatedJobs);
        testListByFilter(updatedJobs);
        return updatedJobs;
    }

    private void testInsertOnly(List<CICDInstance> cicdInstances, List<CICDJob> expected) throws SQLException {
        List<CICDJob> updatedJobs = testInserts(cicdInstances, INSERT_ONLY);
        Assert.assertEquals(expected.stream().map(CICDJob::getId).collect(Collectors.toSet()), updatedJobs.stream().map(CICDJob::getId).collect(Collectors.toSet()));

        testList(expected);
        testListByFilter(expected);
        return;
    }

    private void testList(List<CICDJob> expected) throws SQLException {
        DbListResponse<CICDJob> result = dbService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }

    private void testListByFilterJobNames(List<CICDJob> allExpected) throws SQLException {
        Map<String, List<CICDJob>> map = allExpected.stream().collect(Collectors.groupingBy(CICDJob::getJobName));
        for (String jobName : map.keySet()) {
            List<CICDJob> expected = map.get(jobName);
            DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, List.of(jobName), null,null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allJobNames = allExpected.stream().map(CICDJob::getJobName).collect(Collectors.toList());
        DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, allJobNames, null, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilterJobNormalizedFullNames(List<CICDJob> allExpected) throws SQLException {
        Map<String, List<CICDJob>> map = allExpected.stream().collect(Collectors.groupingBy(CICDJob::getJobNormalizedFullName));
        for (String jobNormalizedFullName : map.keySet()) {
            List<CICDJob> expected = map.get(jobNormalizedFullName);
            DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, null, List.of(jobNormalizedFullName), null, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allJobNormalizedFullNames = allExpected.stream().map(CICDJob::getJobNormalizedFullName).collect(Collectors.toList());
        DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, null, allJobNormalizedFullNames, null, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilterJobFullNames(List<CICDJob> allExpected) throws SQLException {
        Map<String, List<CICDJob>> map = allExpected.stream().collect(Collectors.groupingBy(CICDJob::getJobFullName));
        for (String jobFullName : map.keySet()) {
            List<CICDJob> expected = map.get(jobFullName);
            DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, null, null, List.of(jobFullName), null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<String> allFullNames = allExpected.stream().map(CICDJob::getJobFullName).collect(Collectors.toList());
        DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, null, null, allFullNames, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }

    private void testListByFilterCicdInstanceIds(List<CICDJob> allExpected) throws SQLException {
        Map<UUID, List<CICDJob>> map = allExpected.stream().filter(x -> x.getCicdInstanceId() != null).collect(Collectors.groupingBy(CICDJob::getCicdInstanceId));
        for (UUID cicdInstanceId : map.keySet()) {
            List<CICDJob> expected = map.get(cicdInstanceId);
            DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, null, null, null, List.of(cicdInstanceId));
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allCicdInstanceId = allExpected.stream().map(CICDJob::getCicdInstanceId).collect(Collectors.toList());
        DbListResponse<CICDJob> result = dbService.listByFilter(company, 0, 100, null, null, null, null, allCicdInstanceId);
        Assert.assertNotNull(result);
        Assert.assertEquals(map.size(), result.getTotalCount().intValue());
        Assert.assertEquals(map.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), map.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
    }

    private void testListByFilter(List<CICDJob> expected) throws SQLException {
        testListByFilterJobNames(expected);
        testListByFilterJobNormalizedFullNames(expected);
        testListByFilterJobFullNames(expected);
        testListByFilterCicdInstanceIds(expected);
    }

    private List<CICDJob> testUpdate(List<CICDJob> expected) throws SQLException {
        List<CICDJob> allUpdated = new ArrayList<>();
        for (int i = 0; i < expected.size(); i++) {
            CICDJob current = expected.get(i);
            String currentScmUrl = current.getScmUrl();
            String currentScmUserId = current.getScmUserId();
            CICDJob updated = current.toBuilder().scmUrl(currentScmUrl + "-" + i).scmUserId(currentScmUserId + "-" + i).build();
            Boolean success = dbService.update(company, updated);
            Assert.assertTrue(success);
            testGet(updated);
            allUpdated.add(updated);
        }
        testList(allUpdated);
        testListByFilter(allUpdated);
        return allUpdated;
    }

    private void testDelete(List<CICDJob> expected) throws SQLException {
        for (int i = 0; i < expected.size(); i++) {
            CICDJob current = expected.get(0);
            Boolean success = dbService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testList(expected);
        }
        testList(expected);
        testListByFilter(expected);
    }

    @Test
    public void testJenkinsInstanceNull() throws SQLException {
        int n = 5;
        List<CICDInstance> cicdInstances = null;

        List<CICDJob> expected = testInserts(cicdInstances, INSERT_ONLY);
        testList(expected);
        testListByFilter(expected);

        expected = testUpsert(cicdInstances, expected);
        testInsertOnly(cicdInstances, expected);
        expected = testUpdate(expected);
        testDelete(expected);
    }

    @Test
    public void testJenkinsInstanceNotNull() throws SQLException {
        int n = 5;
        List<CICDInstance> cicdInstances = CiCdInstanceUtils.createCiCdInstances(ciCdInstancesDatabaseService, company, integration, n);

        List<CICDJob> expected = testInserts(cicdInstances, INSERT_ONLY);
        testList(expected);
        testListByFilter(expected);

        expected = testUpsert(cicdInstances, expected);
        testInsertOnly(cicdInstances, expected);
        expected = testUpdate(expected);
        testDelete(expected);
    }

    private void testFlowValidations(String jobId, List<CICDJob> expected, int countOfJobsWithoutCiCdInstance) throws SQLException {
        CICDJob actual = dbService.get(company, jobId).orElse(null);
        verifyRecord(actual, expected.get(expected.size() - 1));
        DbListResponse<CICDJob> dbListResponse = dbService.list(company, 0, 10);
        Assert.assertEquals(dbListResponse.getCount().intValue(), expected.size());
        Assert.assertEquals(dbListResponse.getTotalCount().intValue(), expected.size());
        Assert.assertEquals(dbListResponse.getRecords().size(), expected.size());
        verifyRecords(dbListResponse.getRecords(), expected);
        // Check Count of Jobs without cicd instance
        Integer count = dbService.getCountOfJobsWithoutCiCdInstance(company);
        Assert.assertEquals(countOfJobsWithoutCiCdInstance, count.intValue());
    }

    private CICDJob testFlowInsertAndUpdate(int i, String jobName, String jobFullName, String jobNormalizedFullName, String branchName, String moduleName, UUID cicdInstanceId, CICDJob prevJob) throws SQLException {
        // Create Job instance and without git info
        CICDJob jobV1 = CICDJob.builder().cicdInstanceId(cicdInstanceId).jobName(jobName).jobFullName(jobFullName).jobNormalizedFullName(jobNormalizedFullName).branchName(branchName).moduleName(moduleName).build();
        // Insert Only in db - First run
        String jobId1 = dbService.insertOnly(company, jobV1);
        Assert.assertNotNull(jobId1);
        jobV1 = jobV1.toBuilder().id(UUID.fromString(jobId1)).build();
        // Read back - Make sure it is V1
        testFlowValidations(jobId1, (prevJob != null) ? List.of(prevJob, jobV1) : List.of(jobV1), 1);

        // Create Job without instance and with git info
        CICDJob jobV2 = CICDJob.builder().cicdInstanceId(cicdInstanceId).jobName(jobName).jobFullName(jobFullName).jobNormalizedFullName(jobNormalizedFullName).branchName(branchName).moduleName(moduleName).scmUrl("url-" + i).scmUserId("git-user-" + i).build();
        // Upsert in db - Second run
        String jobId2 = dbService.insert(company, jobV2);
        Assert.assertEquals(jobId2, jobId1);
        jobV2 = jobV2.toBuilder().id(UUID.fromString(jobId2)).build();
        // Read back - Make sure it is V2
        testFlowValidations(jobId2, (prevJob != null) ? List.of(prevJob, jobV2) : List.of(jobV2), 1);

        // Create Job without instance and without git info
        CICDJob jobV3 = CICDJob.builder().cicdInstanceId(cicdInstanceId).jobName(jobName).jobFullName(jobFullName).jobNormalizedFullName(jobNormalizedFullName).branchName(branchName).moduleName(moduleName).build();
        // Insert Only in db
        String jobId3 = dbService.insertOnly(company, jobV3);
        Assert.assertEquals(jobId3, jobId1);
        jobV3 = jobV3.toBuilder().id(UUID.fromString(jobId3)).build();
        // Read back - Make sure it is V2
        testFlowValidations(jobId3, (prevJob != null) ? List.of(prevJob, jobV2) : List.of(jobV2), 1);

        return jobV2;
    }

    @Test
    public void testFlow() throws SQLException {
        int i = 0;
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, i);
        UUID cicdInstanceId = cicdInstance.getId();
        String jobName = "jobname-" + i;
        String branchName = "branch-name-" + i;
        String jobFullName = jobName + "/branches/" + branchName;
        String jobNormalizedFullName = jobName + "/" + branchName;
        String moduleName = "module-name-" + i;

        CICDJob jobWithoutInstance = testFlowInsertAndUpdate(i, jobName, jobFullName, jobNormalizedFullName, branchName, moduleName, null, null);
        CICDJob jobWithInstance = testFlowInsertAndUpdate(i, jobName, jobFullName, jobNormalizedFullName, branchName, moduleName, cicdInstanceId, jobWithoutInstance);

        // Delete Jobs Without CiCd Instance using Job Name
        Boolean success = dbService.deleteByJobsWithoutCiCdInstanceByJobName(company, List.of(jobName));
        Assert.assertTrue(success);
        // Check List
        DbListResponse<CICDJob> dbListResponse = dbService.list(company, 0, 10);
        Assert.assertEquals(dbListResponse.getCount().intValue(), 1);
        Assert.assertEquals(dbListResponse.getTotalCount().intValue(), 1);
        Assert.assertEquals(dbListResponse.getRecords().size(), 1);
        verifyRecords(dbListResponse.getRecords(), List.of(jobWithInstance));
        // Check Count of Jobs without cicd instance
        Integer count = dbService.getCountOfJobsWithoutCiCdInstance(company);
        Assert.assertEquals(0, count.intValue());
    }

    @Test
    public void testUpdateJobScmUrl() throws SQLException {
        UUID instanceId = UUID.randomUUID();
        ciCdInstancesDatabaseService.insert(company, CICDInstance.builder()
                .id(instanceId)
                .integrationId("1")
                .name("azure-integration")
                .type(CICD_TYPE.azure_devops.toString())
                .build());
        CICDJob job1 = CICDJob.builder()
                .jobName("job-1")
                .projectName("project-1")
                .jobNormalizedFullName("job/name")
                .jobFullName("job-1")
                .cicdInstanceId(instanceId)
                .build();
        String jobId = dbService.insert(company, job1);

        int count = dbService.updateJobScmUrl(company, CICDJob.builder()
                .jobName("job-1")
                .projectName("project-1")
                .scmUrl("repo-test")
                .cicdInstanceId(instanceId)
                .build());
        Optional<CICDJob> updatedJob = dbService.get(company, jobId);
        Assert.assertEquals(updatedJob.get().getScmUrl(), "repo-test");
        Assert.assertEquals(count, 1);
    }
}