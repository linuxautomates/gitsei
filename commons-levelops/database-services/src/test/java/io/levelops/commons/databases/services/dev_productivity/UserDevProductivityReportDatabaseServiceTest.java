package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.database.velocity.VelocityConfig;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
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
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseServiceTestUtils.createDevProdProfile;
import static org.assertj.core.api.Assertions.assertThat;

public class UserDevProductivityReportDatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static OrgVersionsDatabaseService orgVersionsService;
    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    String company = "test";
    JdbcTemplate template;
    UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    TicketCategorizationSchemeDatabaseService ticketService;

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
        userDevProductivityReportDatabaseService = new UserDevProductivityReportDatabaseService(dataSource, DefaultObjectMapper.get());
        userDevProductivityReportDatabaseService.ensureTableExistence(company);
        userDevProductivityReportDatabaseService.populateData = false;
    }

    @Test
    public void test() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 1);
        DBOrgUser orgUser = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 1, integration);
        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0)).getId();

        UserDevProductivityReport report1 =  UserDevProductivityReport.builder()
                .orgUserId(orgUser.getId()).orgUserRefId(orgUser.getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.MONTH_JAN)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(company)
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .build();
        String id1 = userDevProductivityReportDatabaseService.insert(company, report1);
        report1 = report1.toBuilder().id(UUID.fromString(id1)).build();
        UserDevProductivityReport report1Read = userDevProductivityReportDatabaseService.get(company, id1).orElse(null);
        assertThat(report1Read.getScore()).isEqualTo(10);
        UserDevProductivityReport report1Updated = report1.toBuilder().score(20).build();
        userDevProductivityReportDatabaseService.update(company, report1Updated);
        UserDevProductivityReport report1UpdatedRead = userDevProductivityReportDatabaseService.get(company, report1Read.getId().toString()).orElse(null);
        assertThat(report1UpdatedRead.getScore()).isEqualTo(20);
        assertThat(report1Read.getReport().getEmail()).isEqualTo("satish@levelops.io");
        assertThat(userDevProductivityReportDatabaseService.list(company, 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);

        Instant now = Instant.now();
        Instant t0 = now.minus(12, ChronoUnit.DAYS);
        Instant t1 = now.minus(6, ChronoUnit.DAYS);

        int sizeBeforeUpsert = userDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        //Test Upsert
        UserDevProductivityReport report = UserDevProductivityReport.builder()
                .orgUserId(orgUser.getId()).orgUserRefId(orgUser.getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.PAST_YEAR).startTime(t0).endTime(t0).score(10).report(DevProductivityResponse.builder().build())
                .build();
        String id = userDevProductivityReportDatabaseService.upsert(company, report);
        report = report.toBuilder().id(UUID.fromString(id)).build();
        int sizeAfterUpsert1 = userDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert1 == sizeBeforeUpsert + 1);
        verifyRecord(report, userDevProductivityReportDatabaseService.get(company, id).get());


        //Test List By Filters
        List<UserDevProductivityReport> expectedReports = List.of(report1Updated, report);
        testAllListByFilters(expectedReports);
        DbListResponse<UserDevProductivityReport> dbListResponse = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id)), null, null, null, null, false);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report, dbListResponse.getRecords().get(0));

        dbListResponse = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id1)), null, null, null, null, false);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report1Updated, dbListResponse.getRecords().get(0));

        UserDevProductivityReport updatedReport = report.toBuilder()
                .startTime(t1).endTime(t1).score(20).report(DevProductivityResponse.builder().build())
                .build();
        id = userDevProductivityReportDatabaseService.upsert(company, updatedReport);
        int sizeAfterUpsert2 = userDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert2 == sizeAfterUpsert1);
        verifyRecord(updatedReport, userDevProductivityReportDatabaseService.get(company, id).get());
    }

    private void testListByFiltersIntervals(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<ReportIntervalType, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getInterval));
        for (ReportIntervalType interval : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(interval);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null,null, List.of(interval), null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ReportIntervalType> allIds = allExpected.stream().map(UserDevProductivityReport::getInterval).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null,allIds, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersDevProductivityProfileIds(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getDevProductivityProfileId));
        for (UUID devProductivityProfileId : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(devProductivityProfileId);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, List.of(devProductivityProfileId), null, null,false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getDevProductivityProfileId).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, allIds, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, List.of(UUID.randomUUID()), null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersOrgUserIds(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getOrgUserId));
        for (UUID orgUserId : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(orgUserId);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, List.of(orgUserId),null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getOrgUserId).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, allIds, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, List.of(UUID.randomUUID()), null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIds(List<UserDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<UserDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(UserDevProductivityReport::getId));
        for (UUID id : map.keySet()) {
            List<UserDevProductivityReport> expected = map.get(id);
            DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(id), null, null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(UserDevProductivityReport::getId).distinct().collect(Collectors.toList());
        DbListResponse<UserDevProductivityReport> result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, allIds, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = userDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testAllListByFilters(List<UserDevProductivityReport> allExpected) throws SQLException {
        testListByFiltersIds(allExpected);
        testListByFiltersOrgUserIds(allExpected);
        testListByFiltersDevProductivityProfileIds(allExpected);
        testListByFiltersIntervals(allExpected);
    }

    private void verifyRecords(List<UserDevProductivityReport> a, List<UserDevProductivityReport> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, UserDevProductivityReport> actualMap = a.stream().collect(Collectors.toMap(UserDevProductivityReport::getId, x -> x));
        Map<UUID, UserDevProductivityReport> expectedMap = e.stream().collect(Collectors.toMap(UserDevProductivityReport::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    private void verifyRecord(UserDevProductivityReport expected, UserDevProductivityReport actual) {
        Assert.assertEquals(expected.getOrgUserId(), actual.getOrgUserId());
        Assert.assertEquals(expected.getOrgUserRefId(), actual.getOrgUserRefId());
        Assert.assertEquals(expected.getDevProductivityProfileId(), actual.getDevProductivityProfileId());
        Assert.assertEquals(expected.getDevProductivityProfileTimestamp(), actual.getDevProductivityProfileTimestamp());
        Assert.assertEquals(expected.getInterval(), actual.getInterval());
        Assert.assertEquals(expected.getStartTime(), actual.getStartTime());
        Assert.assertEquals(expected.getEndTime(), actual.getEndTime());
        Assert.assertEquals(expected.getScore(), actual.getScore());
        Assert.assertEquals(expected.getReport(), actual.getReport());
    }

    @Test
    public void testData() throws SQLException {
        Integration integration = IntegrationUtils.createIntegration(integrationService, company, 1);
        DBOrgUser orgUser = OrgUsersDatabaseServiceTestUtils.createDBOrgUser(orgUsersDatabaseService, company, 1, integration);
        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0)).getId();

        String id2 = userDevProductivityReportDatabaseService.insert(company, UserDevProductivityReport.builder()
                .orgUserId(orgUser.getId()).orgUserRefId(orgUser.getRefId())
                .devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.LAST_MONTH)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName(company)
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                        .devProductivityProfileTimestamp(Instant.now())
                .build());
        DbListResponse<UserDevProductivityReport> output = userDevProductivityReportDatabaseService.list(company, 0, 20);
        userDevProductivityReportDatabaseService.update(company, output.getRecords().get(0));
        UserDevProductivityReport output2 = userDevProductivityReportDatabaseService.get(company, id2).orElse(null);
        assertThat(output.getRecords().size()).isGreaterThanOrEqualTo(1);
        assertThat(output2.getScore()).isEqualTo(10);
        assertThat(userDevProductivityReportDatabaseService.list(company, 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);
        userDevProductivityReportDatabaseService.delete(company, id2);
        UserDevProductivityReport output3 = userDevProductivityReportDatabaseService.get(company, id2).orElse(null);
        assertThat(output3).isEqualTo(null);
    }

}
