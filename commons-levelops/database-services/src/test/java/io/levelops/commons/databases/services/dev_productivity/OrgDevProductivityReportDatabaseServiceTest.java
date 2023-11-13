package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseServiceTestUtils.createDevProdProfile;
import static org.assertj.core.api.Assertions.assertThat;

public class OrgDevProductivityReportDatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static OrgVersionsDatabaseService orgVersionsService;
    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    String company = "test";
    JdbcTemplate template;
    OrgDevProductivityReportDatabaseService orgDevProductivityReportDatabaseService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    TicketCategorizationSchemeDatabaseService ticketService;
    OrgUnitsDatabaseService orgUnitsDatabaseService;
    OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private TagItemDBService tagItemService;
    private OrgVersionsDatabaseService versionsService;


    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        orgDevProductivityReportDatabaseService = new OrgDevProductivityReportDatabaseService(dataSource, DefaultObjectMapper.get());
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        orgDevProductivityReportDatabaseService.populateData = false;
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence("test");
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence("test");

        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence("test");
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        orgUsersDatabaseService.ensureTableExistence("test");
        ticketService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketService.ensureTableExistence("test");
        devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, mapper);
        devProductivityProfileDatabaseService.ensureTableExistence("test");
        tagItemService = new TagItemDBService(dataSource);
        versionsService = new OrgVersionsDatabaseService(dataSource);
        UserService userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, orgUsersDatabaseService, versionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, orgUsersDatabaseService, versionsService, dashboardWidgetService);
        orgUnitsDatabaseService.ensureTableExistence("test");
        orgDevProductivityReportDatabaseService.ensureTableExistence("test");
    }

    @Test
    public void test() throws SQLException {
        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        List<DBOrgUnit> dbOrgUnits = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 2, integration1, integration2);

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0).getId();

        Map<Integer, List<OrgDevProductivityReport>> ouRefIdToReportMap = new HashMap<>();

        OrgDevProductivityReport report1 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnits.get(0).getId()).ouRefId(dbOrgUnits.get(0).getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.MONTH_JAN)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10L))
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .build();

        String id2 = orgDevProductivityReportDatabaseService.insert("test", report1);
        report1 = report1.toBuilder().id(UUID.fromString(id2)).build();

        OrgDevProductivityReport report1Read = orgDevProductivityReportDatabaseService.get("test", id2).orElse(null);
        assertThat(report1Read.getScore()).isEqualTo(10);
        OrgDevProductivityReport report1Updated = report1.toBuilder().devProductivityProfileId(report1.getDevProductivityProfileId())
                .ouID(report1.getOuID())
                .id(report1.getId())
                .score(20)
                .report(report1.getReport()).build();
        orgDevProductivityReportDatabaseService.update("test", report1Updated);
        OrgDevProductivityReport report1UpdatedRead = orgDevProductivityReportDatabaseService.get("test", report1.getId().toString()).orElse(null);
        assertThat(report1UpdatedRead.getScore()).isEqualTo(20);
        assertThat(report1UpdatedRead.getReport().getEmail()).isEqualTo("satish@levelops.io");
        assertThat(orgDevProductivityReportDatabaseService.list("test", 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);
        ouRefIdToReportMap.computeIfAbsent(dbOrgUnits.get(0).getRefId(), k -> new ArrayList<>()).add(report1Updated);

        Instant now = Instant.now();
        Instant t0 = now.minus(12, ChronoUnit.DAYS);
        Instant t1 = now.minus(6, ChronoUnit.DAYS);

        int sizeBeforeUpsert = orgDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        //Test Upsert
        OrgDevProductivityReport report = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnits.get(1).getId()).ouRefId(dbOrgUnits.get(1).getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.PAST_YEAR)
                .startTime(t0).endTime(t0).score(10).report(DevProductivityResponse.builder().build())
                .build();
        String id = orgDevProductivityReportDatabaseService.upsert(company, report);
        report = report.toBuilder().id(UUID.fromString(id)).build();
        int sizeAfterUpsert1 = orgDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert1 == sizeBeforeUpsert + 1);
        verifyRecord(report, orgDevProductivityReportDatabaseService.get(company, id).get());
        ouRefIdToReportMap.computeIfAbsent(dbOrgUnits.get(1).getRefId(), k -> new ArrayList<>()).add(report);

        //Test List By Filters
        List<OrgDevProductivityReport> expectedReports = List.of(report1Updated, report);
        testAllListByFilters(expectedReports, ouRefIdToReportMap);
        DbListResponse<OrgDevProductivityReport> dbListResponse = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id)), null, null, null, null, null, null, false);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id2)), null, null, null, null, null, null, false);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report1Updated, dbListResponse.getRecords().get(0));

        OrgDevProductivityReport updatedReport = report.toBuilder()
                .startTime(t1).endTime(t1).score(20).report(DevProductivityResponse.builder().build())
                .build();
        id = orgDevProductivityReportDatabaseService.upsert(company, updatedReport);
        int sizeAfterUpsert2 = orgDevProductivityReportDatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert2 == sizeAfterUpsert1);
        verifyRecord(updatedReport, orgDevProductivityReportDatabaseService.get(company, id).get());
    }

    //region List By Filter
    private void testListByFiltersOURefIds(Map<Integer, List<OrgDevProductivityReport>> ouRefIdToReportMap) throws SQLException {
        for(Map.Entry<Integer, List<OrgDevProductivityReport>> e : ouRefIdToReportMap.entrySet()) {
            DbListResponse<OrgDevProductivityReport> res = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null,
                    null, null, null, List.of(e.getKey()), null, null, false);
            Assert.assertEquals(e.getValue().size(), res.getTotalCount().intValue());
            Assert.assertEquals(e.getValue().size(), res.getRecords().size());
            verifyRecords(e.getValue(), res.getRecords());
        }
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null, null, List.of(Integer.MAX_VALUE), null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIntervals(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<ReportIntervalType, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getInterval));
        for (ReportIntervalType interval : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(interval);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null,null, List.of(interval), null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ReportIntervalType> allIds = allExpected.stream().map(OrgDevProductivityReport::getInterval).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null,allIds, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersDevProductivityProfileIds(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getDevProductivityProfileId));
        for (UUID devProductivityProfileId : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(devProductivityProfileId);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, List.of(devProductivityProfileId), null, null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(OrgDevProductivityReport::getDevProductivityProfileId).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, allIds, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, List.of(UUID.randomUUID()), null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersOUIds(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getOuID));
        for (UUID orgUserId : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(orgUserId);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, List.of(orgUserId),null, null, null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(OrgDevProductivityReport::getOuID).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, allIds, null, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, List.of(UUID.randomUUID()), null, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIds(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getId));
        for (UUID id : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(id);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(id), null, null, null, null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(OrgDevProductivityReport::getId).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, allIds, null, null, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIsOUActive(List<OrgDevProductivityReport> allExpected) throws SQLException {
        //isOUActive => null
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        //isOUActive => true
        result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null, null, null, true, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        //isOUActive => false
        result = orgDevProductivityReportDatabaseService.listByFilter(company, 0, 100, null, null, null, null, null, false, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testAllListByFilters(List<OrgDevProductivityReport> allExpected, Map<Integer, List<OrgDevProductivityReport>> ouRefIdToReportMap) throws SQLException {
        testListByFiltersIds(allExpected);
        testListByFiltersOUIds(allExpected);
        testListByFiltersDevProductivityProfileIds(allExpected);
        testListByFiltersIntervals(allExpected);
        testListByFiltersOURefIds(ouRefIdToReportMap);
        testListByFiltersIsOUActive(allExpected);
    }
    //endregion

    //region Verify Records
    private void verifyRecords(List<OrgDevProductivityReport> a, List<OrgDevProductivityReport> e) {
        Assert.assertEquals(CollectionUtils.isEmpty(a), CollectionUtils.isEmpty(e));
        if (CollectionUtils.isEmpty(a)) {
            return;
        }
        Assert.assertEquals(a.size(), e.size());
        Map<UUID, OrgDevProductivityReport> actualMap = a.stream().collect(Collectors.toMap(OrgDevProductivityReport::getId, x -> x));
        Map<UUID, OrgDevProductivityReport> expectedMap = e.stream().collect(Collectors.toMap(OrgDevProductivityReport::getId, x -> x));

        for (UUID key : actualMap.keySet()) {
            verifyRecord(actualMap.get(key), expectedMap.get(key));
        }
    }
    private void verifyRecord(OrgDevProductivityReport expected, OrgDevProductivityReport actual) {
        Assert.assertEquals(expected.getOuID(), actual.getOuID());
        Assert.assertEquals(expected.getDevProductivityProfileId(), actual.getDevProductivityProfileId());
        Assert.assertEquals(expected.getInterval(), actual.getInterval());
        Assert.assertEquals(expected.getStartTime(), actual.getStartTime());
        Assert.assertEquals(expected.getEndTime(), actual.getEndTime());
        Assert.assertEquals(expected.getScore(), actual.getScore());
        Assert.assertEquals(expected.getReport(), actual.getReport());
    }
    //endregion
    
    @Test
    public void testData() throws SQLException {
        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        var orgUnit = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnit(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 0, integration1, integration2);

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0)).getId();

        String id2 = orgDevProductivityReportDatabaseService.insert("test", OrgDevProductivityReport.builder()
                .ouID(orgUnit.getId()).ouRefId(orgUnit.getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.LAST_MONTH)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10L))
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .build());
        DbListResponse<OrgDevProductivityReport> output = orgDevProductivityReportDatabaseService.list("test", 0, 20);
        orgDevProductivityReportDatabaseService.update("test", output.getRecords().get(0));
        OrgDevProductivityReport output2 = orgDevProductivityReportDatabaseService.get("test", id2).orElse(null);
        assertThat(output.getRecords().size()).isGreaterThanOrEqualTo(1);
        assertThat(output2.getScore()).isEqualTo(10);
        assertThat(orgDevProductivityReportDatabaseService.list("test", 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);
        orgDevProductivityReportDatabaseService.delete("test", id2);
        OrgDevProductivityReport output3 = orgDevProductivityReportDatabaseService.get("test", id2).orElse(null);
        assertThat(output3).isEqualTo(null);
    }
}
