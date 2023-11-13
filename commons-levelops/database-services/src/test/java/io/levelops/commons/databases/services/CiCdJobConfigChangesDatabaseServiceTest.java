package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobConfigChange;
import io.levelops.commons.databases.models.database.CICDJobRun;
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
import java.util.UUID;
import java.util.stream.Collectors;

public class CiCdJobConfigChangesDatabaseServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static UserService userService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdJobConfigChangesDatabaseService dbService;
    private static String company = "test";
    private static IntegrationService integrationService;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        dbService = new CiCdJobConfigChangesDatabaseService(dataSource);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
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
        userService.ensureTableExistence(company);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        dbService.ensureTableExistence(company);

    }


    private void verifyRecord(CICDJobConfigChange a, CICDJobConfigChange e){
        Assert.assertEquals(a.getId(), e.getId());
        Assert.assertEquals(a.getCicdJobId(), e.getCicdJobId());
        Assert.assertEquals(a.getChangeTime(), e.getChangeTime());
        Assert.assertEquals(a.getChangeType(), e.getChangeType());
        Assert.assertEquals(a.getCicdUserId(), e.getCicdUserId());
    }
    private void verifyRecords(List<CICDJobConfigChange> a, List<CICDJobConfigChange> e){
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if(CollectionUtils.isEmpty(a)){
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, CICDJobConfigChange> actualMap = a.stream().collect(Collectors.toMap(CICDJobConfigChange::getId, x -> x));
        Map<UUID, CICDJobConfigChange> expectedMap = e.stream().collect(Collectors.toMap(CICDJobConfigChange::getId, x -> x));

        for(UUID key : actualMap.keySet()){
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }

    private void testGet(CICDJobConfigChange expected) throws SQLException {
        CICDJobConfigChange actual = dbService.get(company, expected.getId().toString()).get();
        verifyRecord(actual,expected);
    }

    private CICDJobConfigChange testDBInsert(CICDJobConfigChange cicdJobConfigChange) throws SQLException {
        String id = dbService.insert(company, cicdJobConfigChange);
        Assert.assertNotNull(id);
        CICDJobConfigChange expected = cicdJobConfigChange.toBuilder().id(UUID.fromString(id)).build();
        testGet(expected);
        return expected;
    }
    private CICDJobConfigChange testInsert(int i, CICDJob cicdJob) throws SQLException {
        List<CICDJobRun.JobRunParam> params = new ArrayList<>();
        CICDJobConfigChange cicdJobConfigChange = CICDJobConfigChange.builder()
                .cicdJobId(cicdJob.getId())
                .changeTime(Instant.now())
                .changeType("changed")
                .cicdUserId("user-jenkins-" + i)
                .build();
        return testDBInsert(cicdJobConfigChange);
    }

    private List<CICDJobConfigChange> testInserts(CICDJob cicdJob) throws SQLException {
        List<CICDJobConfigChange> cicdJobConfigChanges = new ArrayList<>();
        for(int i=0; i< 5; i++){
            CICDJobConfigChange cicdJobRun = testInsert(i, cicdJob);
            cicdJobConfigChanges.add(cicdJobRun);
        }
        return cicdJobConfigChanges;
    }

    private void testDuplicateInsert(List<CICDJobConfigChange> expected) throws SQLException {
        List<CICDJobConfigChange> duplicates = new ArrayList<>();
        for(CICDJobConfigChange current : expected){
            CICDJobConfigChange duplicate = testDBInsert(current.toBuilder().id(null).build());
            duplicates.add(duplicate);
        }
        Assert.assertEquals(expected.stream().map(CICDJobConfigChange::getId).collect(Collectors.toSet()),duplicates.stream().map(CICDJobConfigChange::getId).collect(Collectors.toSet()));
    }

    private void testList(List<CICDJobConfigChange> expected) throws SQLException {
        DbListResponse<CICDJobConfigChange> result = dbService.list(company, 0, 100);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }

    private void testListByFilter(List<CICDJobConfigChange> expected) throws SQLException {
        List<UUID> ids = expected.stream().map(CICDJobConfigChange::getId).collect(Collectors.toList());
        DbListResponse<CICDJobConfigChange> result = dbService.listByFilter(company, 0, 100, ids);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(expected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), expected);
    }

    private void testListSmallPageSize(List<CICDJobConfigChange> expected) throws SQLException {
        DbListResponse<CICDJobConfigChange> result = dbService.list(company, 0, 1);
        Assert.assertNotNull(result);
        Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(1, result.getCount().intValue());
        verifyRecord(result.getRecords().get(0), expected.get(expected.size() -1));
    }

    private void testValidDelete(List<CICDJobConfigChange> expected) throws SQLException {
        for(int i=0; i< expected.size(); i++){
            CICDJobConfigChange current = expected.get(0);
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

    private void testDelete(List<CICDJobConfigChange> expected) throws SQLException {
        testInvalidDelete();
        testValidDelete(expected);
    }

    @Test
    public void test() throws SQLException {
        CICDInstance cicdInstance = CiCdInstanceUtils.createCiCdInstance(ciCdInstancesDatabaseService, company, 0);
        CICDJob cicdJob = CiCdJobUtils.createCICDJob(ciCdJobsDatabaseService, company, 0, cicdInstance);
        List<CICDJobConfigChange> expected = testInserts(cicdJob);
        testDuplicateInsert(expected);
        testList(expected);
        testListByFilter(expected);
        testListSmallPageSize(expected);
        testDelete(expected);
    }
}