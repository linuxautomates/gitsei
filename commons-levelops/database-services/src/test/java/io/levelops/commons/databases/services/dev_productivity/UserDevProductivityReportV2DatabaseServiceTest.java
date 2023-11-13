package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUsersHelper;
import io.levelops.commons.helper.organization.OrgUsersLockService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseServiceTestUtils.createDevProdProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UserDevProductivityReportV2DatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static OrgVersionsDatabaseService orgVersionsService;
    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    String company = "test";
    JdbcTemplate template;
    UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    TicketCategorizationSchemeDatabaseService ticketService;
    OrgUsersHelper orgUsersHelper;

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);

        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        orgUsersDatabaseService.ensureTableExistence(company);
        ticketService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketService.ensureTableExistence(company);
        devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, mapper);
        devProductivityProfileDatabaseService.ensureTableExistence(company);
        userDevProductivityReportV2DatabaseService = new UserDevProductivityReportV2DatabaseService(dataSource, DefaultObjectMapper.get(), orgUsersDatabaseService);
        userDevProductivityReportV2DatabaseService.ensureTableExistence(company);
        OrgUsersLockService orgUsersLockService = mock(OrgUsersLockService.class);
        when(orgUsersLockService.lock(anyString(), anyInt())).thenReturn(true);
        when(orgUsersLockService.unlock(anyString())).thenReturn(true);
        orgUsersHelper = new OrgUsersHelper(orgUsersDatabaseService, orgVersionsService, orgUsersLockService);
    }

    @Test
    public void test() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 1);
        DBOrgUser user1 = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 1, integration);
        DBOrgUser user2 = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 2, integration);
        orgUsersHelper.insertNewVersionUsers(company, List.of(user1, user2).stream());

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0)).getId();

        Instant now = Instant.now();

        UserDevProductivityReport u1v1r1 =  UserDevProductivityReport.builder()
                .orgUserId(user1.getId()).orgUserRefId(user1.getRefId())
                .devProductivityProfileId(devProdProfileId).interval(ReportIntervalType.MONTH_JAN).startTime(now).endTime(now)
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(company).email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME.toString(), DevProductivityProfile.FeatureType.PRS_AVG_APPROVAL_TIME.toString()))
                .build();
        String id1 = userDevProductivityReportV2DatabaseService.upsert(company, u1v1r1);
        u1v1r1 = u1v1r1.toBuilder().id(UUID.fromString(id1)).latest(true).build();

        UserDevProductivityReport u1v1r1Read = userDevProductivityReportV2DatabaseService.get(company, id1).orElse(null);
        verifyRecord(u1v1r1, u1v1r1Read);

        UserDevProductivityReport u1v1r1Updated = u1v1r1.toBuilder().score(20).build();
        String id1Updated = userDevProductivityReportV2DatabaseService.upsert(company, u1v1r1Updated);
        Assert.assertEquals(id1, id1Updated);
        UserDevProductivityReport u1v1r1UpdatedRead = userDevProductivityReportV2DatabaseService.get(company, u1v1r1Read.getId().toString()).orElse(null);
        verifyRecord(u1v1r1Updated, u1v1r1UpdatedRead);

        assertThat(userDevProductivityReportV2DatabaseService.list(company, 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);

        int sizeBeforeUpsert = userDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        //Test Upsert
        UserDevProductivityReport u1v1r2 = UserDevProductivityReport.builder()
                .orgUserId(user1.getId()).orgUserRefId(user1.getRefId())
                .devProductivityProfileId(devProdProfileId).interval(ReportIntervalType.PAST_YEAR).startTime(now.minus(12, ChronoUnit.DAYS)).endTime(now.minus(6, ChronoUnit.DAYS))
                .weekOfYear(-1).year(-1)
                .score(40).report(DevProductivityResponse.builder().build())
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.NUMBER_OF_COMMITS_PER_MONTH.toString(), DevProductivityProfile.FeatureType.NUMBER_OF_PRS_PER_MONTH.toString()))
                .build();
        String id = userDevProductivityReportV2DatabaseService.upsert(company, u1v1r2);
        u1v1r2 = u1v1r2.toBuilder().id(UUID.fromString(id)).latest(true).build();
        int sizeAfterUpsert1 = userDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert1 == sizeBeforeUpsert + 1);
        verifyRecord(u1v1r2, userDevProductivityReportV2DatabaseService.get(company, id).get());

        //Test List By Filters
        List<UserDevProductivityReport> expectedReports = List.of(u1v1r1Updated, u1v1r2);
        testAllListByFilters(expectedReports, false);
        DbListResponse<UserDevProductivityReport> dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id)), null, null, null, null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r2, dbListResponse.getRecords().get(0));

        dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id1)), null, null, null, null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r1Updated, dbListResponse.getRecords().get(0));

        UserDevProductivityReport u1v1r2Updated = u1v1r2.toBuilder()
                .startTime(now.minus(12, ChronoUnit.DAYS)).endTime(now.minus(6, ChronoUnit.DAYS))
                .score(50).report(DevProductivityResponse.builder().build())
                .incomplete(false).missingFeatures(List.of())
                .build();
        id = userDevProductivityReportV2DatabaseService.upsert(company, u1v1r2Updated);
        int sizeAfterUpsert2 = userDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert2 == sizeAfterUpsert1);
        verifyRecord(u1v1r2Updated, userDevProductivityReportV2DatabaseService.get(company, id).get());

        UserDevProductivityReport u2v1r1 =  UserDevProductivityReport.builder()
                .orgUserId(user2.getId()).orgUserRefId(user2.getRefId())
                .devProductivityProfileId(devProdProfileId).interval(ReportIntervalType.MONTH_JAN).startTime(now).endTime(now)
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(company).email("satish@levelops.io").fullName("Satish Kumar Singh").customFields(Map.of("filed1","val1")).build())
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME.toString(), DevProductivityProfile.FeatureType.PRS_AVG_APPROVAL_TIME.toString()))
                .build();
        String u2v1r1Id = userDevProductivityReportV2DatabaseService.upsert(company, u2v1r1);
        u2v1r1 = u2v1r1.toBuilder().id(UUID.fromString(u2v1r1Id)).latest(true).build();

        UserDevProductivityReport u2v1r1Read = userDevProductivityReportV2DatabaseService.get(company, u2v1r1Id).orElse(null);
        verifyRecord(u2v1r1, u2v1r1Read);

        orgUsersHelper.insertNewVersionUsers(company, List.of(user1).stream());
        DBOrgUser user1V2 = orgUsersDatabaseService.get(company, user1.getRefId()).orElse(null);

        UserDevProductivityReport u1v2r1 = u1v1r1Updated.toBuilder()
                .orgUserId(user1V2.getId()).score(60)
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.NUMBER_OF_PRS_APPROVED_PER_MONTH.toString(), DevProductivityProfile.FeatureType.NUMBER_OF_PRS_COMMENTED_ON_PER_MONTH.toString()))
                .build();
        String id3 = userDevProductivityReportV2DatabaseService.upsert(company, u1v2r1);
        u1v2r1 = u1v2r1.toBuilder().id(UUID.fromString(id3)).latest(true).build();
        u1v1r1Updated = u1v1r1Updated.toBuilder().latest(false).build();

        UserDevProductivityReport u1v2r1Read = userDevProductivityReportV2DatabaseService.get(company, id3).orElse(null);
        verifyRecord(u1v2r1, u1v2r1Read);

        expectedReports = List.of(u1v1r1Updated, u1v1r2Updated, u1v2r1, u2v1r1);
        testAllListByFilters(expectedReports, true);

        dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(user1.getId()), List.of(devProdProfileId), List.of(u1v1r1.getInterval()), null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v2r1, dbListResponse.getRecords().get(0));
        Assert.assertEquals(user1V2.getId(), dbListResponse.getRecords().get(0).getOrgUserId());
        Assert.assertEquals(user1.getId(), dbListResponse.getRecords().get(0).getRequestedOrgUserId());

        dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(user1V2.getId()), List.of(devProdProfileId), List.of(u1v1r1.getInterval()), null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v2r1, dbListResponse.getRecords().get(0));
        Assert.assertEquals(user1V2.getId(), dbListResponse.getRecords().get(0).getOrgUserId());
        Assert.assertEquals(user1V2.getId(), dbListResponse.getRecords().get(0).getRequestedOrgUserId());

        dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(user1.getId()), List.of(devProdProfileId), List.of(u1v1r2.getInterval()), null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r2Updated, dbListResponse.getRecords().get(0));
        Assert.assertEquals(user1.getId(), dbListResponse.getRecords().get(0).getOrgUserId());
        Assert.assertEquals(user1.getId(), dbListResponse.getRecords().get(0).getRequestedOrgUserId());

        dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(user1V2.getId()), List.of(devProdProfileId), List.of(u1v1r2.getInterval()), null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r2Updated, dbListResponse.getRecords().get(0));
        Assert.assertEquals(user1.getId(), dbListResponse.getRecords().get(0).getOrgUserId());
        Assert.assertEquals(user1V2.getId(), dbListResponse.getRecords().get(0).getRequestedOrgUserId());

        dbListResponse = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(user2.getId()), List.of(devProdProfileId), List.of(u2v1r1.getInterval()), null, false, null);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u2v1r1, dbListResponse.getRecords().get(0));
        Assert.assertEquals(user2.getId(), dbListResponse.getRecords().get(0).getOrgUserId());
        Assert.assertEquals(user2.getId(), dbListResponse.getRecords().get(0).getRequestedOrgUserId());
    }

    //region Test ListByFilters
    private void testListByFiltersAbsoluteOrgUserIds(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getOrgUserId));
        for (UUID orgUserId : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(orgUserId);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null,null, null, null, false, List.of(orgUserId));
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getOrgUserId).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, null, null, null, false, allIds);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());

        result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, null, null, null, false, List.of(UUID.randomUUID()));
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIntervals(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<ReportIntervalType, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getInterval));
        for (ReportIntervalType interval : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(interval);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null,null, List.of(interval), null, false, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        List<ReportIntervalType> allIds = allExpected.stream().map(UserDevProductivityReport::getInterval).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, null,allIds, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());
    }
    private void testListByFiltersDevProductivityProfileIds(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getDevProductivityProfileId));
        for (UUID devProductivityProfileId : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(devProductivityProfileId);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, List.of(devProductivityProfileId), null, null,false, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getDevProductivityProfileId).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, allIds, null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());

        result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, List.of(UUID.randomUUID()), null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersOrgUserIds(List<UserDevProductivityReport> allExpected, boolean skipAllOrgUserIds) throws SQLException {
        Map<Integer, List<UserDevProductivityReport>> map = allExpected.stream().filter(r -> r.getLatest()==true).collect(Collectors.groupingBy(UserDevProductivityReport::getOrgUserRefId));
        Map<UUID, Integer> orgUserIdToRefIdMap = allExpected.stream().collect(Collectors.toMap(x -> x.getOrgUserId(), x -> x.getOrgUserRefId(), (o1,o2)->o1));
        for (UUID orgUserId : orgUserIdToRefIdMap.keySet()) {
            Integer orgUserRefId = orgUserIdToRefIdMap.get(orgUserId);
            List<UserDevProductivityReport> expected = map.get(orgUserRefId);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(orgUserId),null, null, null, false, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }

        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(UUID.randomUUID()), null, null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));

        if (skipAllOrgUserIds) {
            return;
        }

        Map<String,UserDevProductivityReport> allExpectedMap = new HashMap<>();
        for(UserDevProductivityReport r : allExpected) {
            if(!r.getLatest()) {
                continue;
            }
            String key = r.getOrgUserRefId().toString()+ "_" + r.getDevProductivityProfileId().toString() + "_" + r.getInterval().toString();
            allExpectedMap.put(key, r);
        }

        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getOrgUserId).distinct().collect(Collectors.toList());
        List<Integer> allRefIds = allExpected.stream().map(UserDevProductivityReport::getOrgUserRefId).distinct().collect(Collectors.toList());
        List<UUID> allProfileIds = allExpected.stream().map(UserDevProductivityReport::getDevProductivityProfileId).distinct().collect(Collectors.toList());
        List<ReportIntervalType> intervalTypes = allExpected.stream().map(UserDevProductivityReport::getInterval).distinct().collect(Collectors.toList());

        List<UserDevProductivityReport> allExpectedList = new ArrayList<>();
        for(UUID id: allIds) {
            for(UUID profileId : allProfileIds) {
                for(ReportIntervalType i: intervalTypes) {
                    String key = orgUserIdToRefIdMap.get(id).toString()+ "_" + profileId.toString() + "_" + i.toString();
                    UserDevProductivityReport report = allExpectedMap.get(key);
                    allExpectedList.add(report);
                }
            }
        }
        result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, allIds, null, null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpectedList.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpectedList.size(), result.getCount().intValue());
        verifyRecords(allExpectedList, result.getRecords());
    }
    private void testListByFiltersIds(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getId));
        for (UUID id : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(id);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(id), null, null, null, null, false, null);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(expected, result.getRecords());
        }
        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getId).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, allIds, null, null, null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(allExpected, result.getRecords());

        result = userDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null, null, null, false, null);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testAllListByFilters(List<UserDevProductivityReport> allExpected, boolean skipAllOrgUserIds) throws SQLException {
        testListByFiltersIds(allExpected);
        testListByFiltersOrgUserIds(allExpected, skipAllOrgUserIds);
        testListByFiltersDevProductivityProfileIds(allExpected);
        testListByFiltersIntervals(allExpected);
        testListByFiltersAbsoluteOrgUserIds(allExpected);
    }
    //endregion

    //region Verify
    private void verifyRecords(List<UserDevProductivityReport> e, List<UserDevProductivityReport> a) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, UserDevProductivityReport> actualMap = a.stream().collect(Collectors.toMap(UserDevProductivityReport::getId, x -> x));
        Map<UUID, UserDevProductivityReport> expectedMap = e.stream().collect(Collectors.toMap(UserDevProductivityReport::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(expectedMap.get(key), actualMap.get(key));
        }
    }
    private void verifyRecord(UserDevProductivityReport expected, UserDevProductivityReport actual) {
        Assert.assertEquals(expected.getOrgUserId(), actual.getOrgUserId());
        Assert.assertEquals(expected.getOrgUserRefId(), actual.getOrgUserRefId());
        Assert.assertEquals(expected.getDevProductivityProfileId(), actual.getDevProductivityProfileId());
        Assert.assertNull(expected.getDevProductivityProfileTimestamp());
        Assert.assertEquals(expected.getInterval(), actual.getInterval());
        Assert.assertEquals(expected.getStartTime(), actual.getStartTime());
        Assert.assertEquals(expected.getEndTime(), actual.getEndTime());
        Assert.assertEquals(expected.getWeekOfYear(), actual.getWeekOfYear());
        Assert.assertEquals(expected.getYear(), actual.getYear());
        Assert.assertEquals(actual.toString(), expected.getLatest(), actual.getLatest());
        Assert.assertEquals(expected.getScore(), actual.getScore());
        Assert.assertEquals(expected.getReport(), actual.getReport());
        Assert.assertNotNull(actual.getRequestedOrgUserId());
        Assert.assertEquals(expected.getIncomplete(), actual.getIncomplete());
        Assert.assertEquals(expected.getMissingFeatures(), actual.getMissingFeatures());
    }
    //endregion

    @Test
    public void testData() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 1);
        DBOrgUser orgUser = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 1, integration);
        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0)).getId();

        UserDevProductivityReport expected1 = UserDevProductivityReport.builder()
                .orgUserId(orgUser.getId()).orgUserRefId(orgUser.getRefId())
                .devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.LAST_MONTH)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .weekOfYear(-1).year(-1)
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(company)
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME.toString(), DevProductivityProfile.FeatureType.PRS_AVG_APPROVAL_TIME.toString()))
                .build();

        String id1 = userDevProductivityReportV2DatabaseService.upsert(company, expected1);
        expected1 = expected1.toBuilder().id(UUID.fromString(id1)).latest(true).build();

        UserDevProductivityReport actual1 = userDevProductivityReportV2DatabaseService.get(company, id1).orElse(null);
        verifyRecord(expected1, actual1);

        DbListResponse<UserDevProductivityReport> output = userDevProductivityReportV2DatabaseService.list(company, 0, 20);
        verifyRecords(List.of(expected1), output.getRecords());

        String id1Upserted = userDevProductivityReportV2DatabaseService.upsert(company, actual1);
        UserDevProductivityReport actual2 = userDevProductivityReportV2DatabaseService.get(company, id1Upserted).orElse(null);
        verifyRecord(expected1, actual2);

        output = userDevProductivityReportV2DatabaseService.list(company, 0, 20);
        verifyRecords(List.of(expected1), output.getRecords());

        userDevProductivityReportV2DatabaseService.delete(company, id1);
        UserDevProductivityReport output3 = userDevProductivityReportV2DatabaseService.get(company, id1).orElse(null);
        assertThat(output3).isEqualTo(null);
    }

    @Test
    public void testData1() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 1);
        DBOrgUser orgUser = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 1, integration);
        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0)).getId();

        ReportIntervalType week_26_2023 = ReportIntervalType.LAST_WEEK;
        IntervalTimeRange range_26_2023 = week_26_2023.getIntervalTimeRange(convertDate("6-26-2023"));

        ReportIntervalType week26_2023_1 = ReportIntervalType.LAST_WEEK;
        IntervalTimeRange range26_2023_1 = week26_2023_1.getIntervalTimeRange(convertDate("6-29-2023"));

        ReportIntervalType week_27_2023 = ReportIntervalType.LAST_WEEK;
        IntervalTimeRange range_27_2023 = week_27_2023.getIntervalTimeRange(convertDate("7-4-2023"));

        UserDevProductivityReport expected1 = UserDevProductivityReport.builder()
                .orgUserId(orgUser.getId()).orgUserRefId(orgUser.getRefId())
                .devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.LAST_WEEK)
                .startTime(Instant.ofEpochSecond(range_26_2023.getTimeRange().getLeft()))
                .endTime(Instant.ofEpochSecond(range_26_2023.getTimeRange().getRight()))
                .weekOfYear(range_26_2023.getWeekOfTheYear())
                .year(range_26_2023.getYear())
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(company)
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .incomplete(true).missingFeatures(List.of(DevProductivityProfile.FeatureType.AVG_ISSUE_RESOLUTION_TIME.toString(), DevProductivityProfile.FeatureType.PRS_AVG_APPROVAL_TIME.toString()))
                .build();

        String id1 = userDevProductivityReportV2DatabaseService.upsert(company, expected1);
        expected1 = expected1.toBuilder().id(UUID.fromString(id1)).latest(true).build();

        UserDevProductivityReport actual1 = userDevProductivityReportV2DatabaseService.get(company, id1).orElse(null);
        verifyRecord(expected1, actual1);

        DbListResponse<UserDevProductivityReport> output = userDevProductivityReportV2DatabaseService.list(company, 0, 20);
        verifyRecords(List.of(expected1), output.getRecords());

        expected1 = expected1.toBuilder().weekOfYear(range26_2023_1.getWeekOfTheYear())
                .year(range26_2023_1.getYear())
                .startTime(Instant.ofEpochSecond(range26_2023_1.getTimeRange().getLeft()))
                .endTime(Instant.ofEpochSecond(range26_2023_1.getTimeRange().getRight()))
                .build();

        String id2 = userDevProductivityReportV2DatabaseService.upsert(company, expected1);
        output = userDevProductivityReportV2DatabaseService.list(company, 0, 20);
        Assert.assertEquals(output.getRecords().size(),1);
        Assert.assertEquals(output.getRecords().get(0).getLatest(), true);
        Assert.assertEquals(id1, id2);

        expected1 = expected1.toBuilder().weekOfYear(range_27_2023.getWeekOfTheYear())
                .year(range_27_2023.getYear()).build();

        String id3 = userDevProductivityReportV2DatabaseService.upsert(company, expected1);
        output = userDevProductivityReportV2DatabaseService.list(company, 0, 20);
        Assert.assertEquals(output.getRecords().size(),2);
        Assert.assertEquals(output.getRecords().get(0).getLatest(), true);
        Assert.assertEquals(output.getRecords().get(1).getLatest(), false);
        Assert.assertNotEquals(id1, id3);
        UserDevProductivityReport actual_week_27 = userDevProductivityReportV2DatabaseService.get(company, id3).orElse(null);
        expected1 = expected1.toBuilder().id(UUID.fromString(id3)).latest(true).build();
        verifyRecord(expected1, actual_week_27);

    }

    private static Instant convertDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("M-d-yyyy", Locale.US)).atStartOfDay().atZone(ZoneId.of("UTC")).toInstant();
    }

}