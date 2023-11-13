package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobRunDetails;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdJobRunDetailsDatabaseServiceTest {
    private final static Random random = new Random();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserService userService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobRunsDatabaseService cicdJobRunsDatabaseService;
    private static CiCdJobRunDetailsDatabaseService ciCdJobRunDetailsDatabaseService;
    private static String company = "test";
    private static IntegrationService integrationService;

    private static List<CICDInstance> cicdInstances;
    private static int n = 5;
    private static Integration integration;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        cicdJobRunsDatabaseService = new CiCdJobRunsDatabaseService(DefaultObjectMapper.get(), dataSource);
        ciCdJobRunDetailsDatabaseService = new CiCdJobRunDetailsDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        integrationService = new IntegrationService(dataSource);
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
        ciCdJobsDatabaseService.ensureTableExistence(company);
        cicdJobRunsDatabaseService.ensureTableExistence(company);
        ciCdJobRunDetailsDatabaseService.ensureTableExistence(company);
        cicdInstances = CiCdInstanceUtils.createCiCdInstances(ciCdInstancesDatabaseService, company, integration, n);
    }

    private void verifyRecord(CICDJobRunDetails a, CICDJobRunDetails e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getCicdJobRunId(), e.getCicdJobRunId());
        Assert.assertEquals(a.getGcsPath(), e.getGcsPath());
        Assert.assertNotNull(a.getCreatedAt());
        Assert.assertNotNull(a.getUpdatedAt());
    }
    private void verifyRecords(List<CICDJobRunDetails> a, List<CICDJobRunDetails> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, CICDJobRunDetails> actualMap = a.stream().collect(Collectors.toMap(CICDJobRunDetails::getId, x -> x));
        Map<UUID, CICDJobRunDetails> expectedMap = e.stream().collect(Collectors.toMap(CICDJobRunDetails::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    private void testGet(List<CICDJobRunDetails> expected) throws SQLException {
        for(CICDJobRunDetails c : expected) {
            CICDJobRunDetails actual = ciCdJobRunDetailsDatabaseService.get(company, c.getId().toString()).get();
            verifyRecord(actual, c);
        }
    }
    private void testListNoFilters(List<CICDJobRunDetails> expected) throws SQLException {
        DbListResponse<CICDJobRunDetails> dbListResponse = ciCdJobRunDetailsDatabaseService.list(company, 0, 100);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testListByFilters(List<CICDJobRunDetails> expected) throws SQLException {
        for(CICDJobRunDetails c : expected) {
            DbListResponse<CICDJobRunDetails> dbListResponse = ciCdJobRunDetailsDatabaseService.listByFilter(company, 0, 100, null, List.of(c.getCicdJobRunId()));
            Assert.assertNotNull(dbListResponse);
            Assert.assertEquals(1, dbListResponse.getRecords().size());
            verifyRecord(dbListResponse.getRecords().get(0), c);
        }

        List<UUID> cicdJobRunIds = expected.stream().map(CICDJobRunDetails::getCicdJobRunId).collect(Collectors.toList());
        DbListResponse<CICDJobRunDetails> dbListResponse = ciCdJobRunDetailsDatabaseService.listByFilter(company, 0, 100, null, cicdJobRunIds);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(expected.size(), dbListResponse.getRecords().size());
        verifyRecords(dbListResponse.getRecords(), expected);
    }
    private void testList(List<CICDJobRunDetails> expected) throws SQLException {
        testListNoFilters(expected);
        testListByFilters(expected);
    }

    private void testDelete(List<CICDJobRunDetails> expected) throws SQLException {
        while (!expected.isEmpty()) {
            CICDJobRunDetails current = expected.get(0);
            Boolean success = ciCdJobRunDetailsDatabaseService.delete(company, current.getId().toString());
            Assert.assertTrue(success);
            expected.remove(0);
            testGet(expected);
            testList(expected);
        }
        testGet(expected);
        testList(expected);
    }

    @Test
    public void test() throws SQLException {
        List<CICDJob> cicdJobs = new ArrayList<>();
        List<CICDJobRun> cicdJobRuns = new ArrayList<>();
        List<CICDJobRunDetails> expected = new ArrayList<>();

        for(int i=0; i < n; i++) {
            CICDJob cicdJob  = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, i, cicdInstances.get(i));
            CICDJobRun cicdJobRun = CICDJobRunUtils.createCICDJobRun(cicdJobRunsDatabaseService, cicdJob, company, i, Instant.now(), random.nextInt(), null, null);
            CICDJobRunDetails cicdJobRunDetails = CiCdJobRunDetailsUtils.createCICDJobRunDetails(ciCdJobRunDetailsDatabaseService, cicdJobRun, company, i);

            cicdJobs.add(cicdJob);
            cicdJobRuns.add(cicdJobRun);
            expected.add(cicdJobRunDetails);
        }
        testGet(expected);
        testList(expected);
        testDelete(expected);
    }
}