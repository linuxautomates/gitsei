package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.TicketCategorizationScheme;
import io.levelops.commons.databases.models.database.dev_productivity.*;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationUtils;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseServiceTestUtils;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.*;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseServiceTestUtils.createDevProdProfile;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.registerCustomDateFormat;

public class OrgDevProductivityReportV2DatabaseServiceTest {
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static OrgVersionsDatabaseService orgVersionsService;
    private static DataSource dataSource;
    private static UserIdentityService userIdentityService;
    private static IntegrationService integrationService;
    String company = "test";
    JdbcTemplate template;
    OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    TicketCategorizationSchemeDatabaseService ticketService;
    OrgUnitsDatabaseService orgUnitsDatabaseService;
    OrgUnitHelper orgUnitHelper;
    OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private TagItemDBService tagItemService;
    private OrgVersionsDatabaseService versionsService;


    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;


    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();

        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
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
        orgUnitHelper = new OrgUnitHelper(new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, orgUsersDatabaseService, versionsService, dashboardWidgetService), integrationService);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, orgUnitHelper, mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, mapper, tagItemService, orgUsersDatabaseService, versionsService, dashboardWidgetService);
        orgUnitsDatabaseService.ensureTableExistence("test");

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,mapper);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,mapper,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, mapper);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,mapper);
        devProductivityProfileDbService.ensureTableExistence(company);

        orgDevProductivityReportV2DatabaseService = new OrgDevProductivityReportV2DatabaseService(dataSource, DefaultObjectMapper.get(), orgUnitsDatabaseService);
        orgDevProductivityReportV2DatabaseService.ensureTableExistence("test");
    }

    @Test
    public void testProfileOUMappingChange() throws SQLException {
        Instant now = Instant.now();
        Instant t0 = now.minus(12, ChronoUnit.DAYS);
        Instant t1 = now.minus(6, ChronoUnit.DAYS);

        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        DBOrgUnit dbOrgUnitsV1 = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnit(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 0, integration1, integration2);

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        DevProductivityProfile profile1 = createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0, List.of(dbOrgUnitsV1.getRefId()));


        OrgDevProductivityReport u1p1r = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnitsV1.getId()).ouRefId(dbOrgUnitsV1.getRefId()).devProductivityProfileId(profile1.getId())
                .interval(ReportIntervalType.MONTH_JAN).startTime(now).endTime(now.plusSeconds(10L))
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test").email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id1 = orgDevProductivityReportV2DatabaseService.upsert("test", u1p1r);
        u1p1r = u1p1r.toBuilder().id(UUID.fromString(id1)).latest(true).build();
        OrgDevProductivityReport u1p1rRead = orgDevProductivityReportV2DatabaseService.get("test", id1).orElse(null);
        verifyRecord(u1p1r, u1p1rRead);

        DbListResponse<OrgDevProductivityReport> dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnitsV1.getId()), List.of(profile1.getId(), UUID.randomUUID()), null, null, null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p1rRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnitsV1.getId()), null, null, null, null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p1rRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                null, List.of(profile1.getId(), UUID.randomUUID()), null, List.of(dbOrgUnitsV1.getRefId()), null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p1rRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                null, null, null, List.of(dbOrgUnitsV1.getRefId()), null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p1rRead, dbListResponse.getRecords().get(0));

        profile1 = profile1.toBuilder().associatedOURefIds(List.of()).build();
        DevProductivityProfileDatabaseServiceTestUtils.updateProfile(devProductivityProfileDatabaseService, company, profile1);
        DevProductivityProfile profile2 = createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 1, List.of(dbOrgUnitsV1.getRefId()));

        OrgDevProductivityReport u1p2r = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnitsV1.getId()).ouRefId(dbOrgUnitsV1.getRefId()).devProductivityProfileId(profile2.getId())
                .interval(ReportIntervalType.MONTH_JAN).startTime(now).endTime(now.plusSeconds(10L))
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test").email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .weekOfYear(-1).year(-1)
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id2 = orgDevProductivityReportV2DatabaseService.upsert("test", u1p2r);
        u1p2r = u1p2r.toBuilder().id(UUID.fromString(id2)).latest(true).build();
        OrgDevProductivityReport u1p2rRead = orgDevProductivityReportV2DatabaseService.get("test", id2).orElse(null);
        verifyRecord(u1p2r, u1p2rRead);

        u1p1rRead = orgDevProductivityReportV2DatabaseService.get("test", id1).orElse(null);
        Assert.assertNull(u1p1rRead);

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnitsV1.getId()), List.of(profile1.getId(), profile2.getId()), null, null, null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p2rRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                List.of(dbOrgUnitsV1.getId()), null, null, null, null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p2rRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                null, List.of(profile1.getId(), profile2.getId()), null, List.of(dbOrgUnitsV1.getRefId()), null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p2rRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                null, null, null, List.of(dbOrgUnitsV1.getRefId()), null, false );
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1p2rRead, dbListResponse.getRecords().get(0));
    }

    @Test
    public void test1() throws SQLException {


        Instant now = Instant.now();
        Instant t0 = now.minus(12, ChronoUnit.DAYS);
        Instant t1 = now.minus(6, ChronoUnit.DAYS);

        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        List<DBOrgUnit> dbOrgUnitsV1 = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 2, integration1, integration2);

        List<Integer> ouRefIds = dbOrgUnitsV1.stream().map(o -> o.getRefId()).collect(Collectors.toList());
        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0, ouRefIds).getId();

        OrgDevProductivityReport u1v1r1 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnitsV1.get(0).getId()).ouRefId(dbOrgUnitsV1.get(0).getRefId()).devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.MONTH_JAN).startTime(now).endTime(now.plusSeconds(10L))
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test").email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id1 = orgDevProductivityReportV2DatabaseService.upsert("test", u1v1r1);
        u1v1r1 = u1v1r1.toBuilder().id(UUID.fromString(id1)).latest(true).build();
        OrgDevProductivityReport u1v1r1Read = orgDevProductivityReportV2DatabaseService.get("test", id1).orElse(null);
        verifyRecord(u1v1r1, u1v1r1Read);

        OrgDevProductivityReport u1v1r1Updated = u1v1r1.toBuilder().score(20).missingUserReportsCount(21).staleUserReportsCount(22).build();
        Assert.assertEquals(id1, orgDevProductivityReportV2DatabaseService.upsert("test", u1v1r1Updated));
        OrgDevProductivityReport u1v1r1UpdatedRead = orgDevProductivityReportV2DatabaseService.get("test", u1v1r1.getId().toString()).orElse(null);
        verifyRecord(u1v1r1Updated, u1v1r1UpdatedRead);

        OrgDevProductivityReport u1v1r2 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnitsV1.get(0).getId()).ouRefId(dbOrgUnitsV1.get(0).getRefId()).devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.PAST_YEAR).startTime(now).endTime(now.plusSeconds(10L))
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test").email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id2 = orgDevProductivityReportV2DatabaseService.upsert("test", u1v1r2);
        u1v1r2 = u1v1r2.toBuilder().id(UUID.fromString(id2)).latest(true).build();
        OrgDevProductivityReport u1v1r2Read = orgDevProductivityReportV2DatabaseService.get("test", id2).orElse(null);
        verifyRecord(u1v1r2, u1v1r2Read);

        OrgDevProductivityReport u1v1r2Updated = u1v1r2.toBuilder().score(20).missingUserReportsCount(21).staleUserReportsCount(22).build();
        Assert.assertEquals(id2, orgDevProductivityReportV2DatabaseService.upsert("test", u1v1r2Updated));
        OrgDevProductivityReport u1v1r2UpdatedRead = orgDevProductivityReportV2DatabaseService.get("test", u1v1r2.getId().toString()).orElse(null);
        verifyRecord(u1v1r2Updated, u1v1r2UpdatedRead);

        int sizeBeforeUpsert = orgDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        //Test Upsert
        OrgDevProductivityReport u2v1r1 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnitsV1.get(1).getId()).ouRefId(dbOrgUnitsV1.get(1).getRefId()).devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.PAST_YEAR).startTime(t0).endTime(t0)
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();
        String id3 = orgDevProductivityReportV2DatabaseService.upsert(company, u2v1r1);
        u2v1r1 = u2v1r1.toBuilder().id(UUID.fromString(id3)).latest(true).build();
        int sizeAfterUpsert1 = orgDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert1 == sizeBeforeUpsert + 1);
        OrgDevProductivityReport u2v1r1Read = orgDevProductivityReportV2DatabaseService.get(company, id3).get();
        verifyRecord(u2v1r1, u2v1r1Read);

        List<DBOrgUnit> dbOrgUnitsV2 = OrgUnitsDatabaseServiceTestUtils.updateDBOrgUnits(orgUnitsDatabaseService, orgUnitHelper, company, dbOrgUnitsV1);

        OrgDevProductivityReport u1v2r2 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnitsV2.get(0).getId()).ouRefId(dbOrgUnitsV2.get(0).getRefId()).devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.PAST_YEAR).startTime(now).endTime(now.plusSeconds(10L))
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test").email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id4 = orgDevProductivityReportV2DatabaseService.upsert("test", u1v2r2);
        u1v2r2 = u1v2r2.toBuilder().id(UUID.fromString(id4)).latest(true).build();
        OrgDevProductivityReport u1v2r2Read = orgDevProductivityReportV2DatabaseService.get("test", id4).orElse(null);
        verifyRecord(u1v2r2, u1v2r2Read);

        DbListResponse<OrgDevProductivityReport> dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, null, List.of(devProdProfileId), List.of(ReportIntervalType.MONTH_JAN), List.of(dbOrgUnitsV2.get(0).getRefId()), null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r1UpdatedRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, null, List.of(devProdProfileId), List.of(ReportIntervalType.PAST_YEAR), List.of(dbOrgUnitsV2.get(0).getRefId()), null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v2r2Read, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, null, List.of(devProdProfileId), List.of(ReportIntervalType.PAST_YEAR), List.of(dbOrgUnitsV2.get(1).getRefId()), null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u2v1r1Read, dbListResponse.getRecords().get(0));


        //=====================

        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, List.of(dbOrgUnitsV1.get(0).getId()), List.of(devProdProfileId), List.of(ReportIntervalType.MONTH_JAN), null, null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r1UpdatedRead, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, List.of(dbOrgUnitsV2.get(0).getId()), List.of(devProdProfileId), List.of(ReportIntervalType.MONTH_JAN), null, null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v1r1UpdatedRead, dbListResponse.getRecords().get(0));


        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, List.of(dbOrgUnitsV1.get(0).getId()), List.of(devProdProfileId), List.of(ReportIntervalType.PAST_YEAR), null, null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v2r2Read, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, List.of(dbOrgUnitsV2.get(0).getId()), List.of(devProdProfileId), List.of(ReportIntervalType.PAST_YEAR), null, null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u1v2r2Read, dbListResponse.getRecords().get(0));


        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, List.of(dbOrgUnitsV1.get(1).getId()), List.of(devProdProfileId), List.of(ReportIntervalType.PAST_YEAR), null, null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u2v1r1Read, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService
                .listByFilter(company, 0, 100, null, List.of(dbOrgUnitsV2.get(1).getId()), List.of(devProdProfileId), List.of(ReportIntervalType.PAST_YEAR), null, null, false);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(u2v1r1Read, dbListResponse.getRecords().get(0));
    }

    @Test
    public void test() throws SQLException {
        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        List<DBOrgUnit> dbOrgUnits = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 2, integration1, integration2);
        List<Integer> ouRefIds = dbOrgUnits.stream().map(o -> o.getRefId()).collect(Collectors.toList());

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0, ouRefIds).getId();

        Instant now = Instant.now();
        Map<Integer, List<OrgDevProductivityReport>> ouRefIdToReportMap = new HashMap<>();

        OrgDevProductivityReport report1 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnits.get(0).getId()).ouRefId(dbOrgUnits.get(0).getRefId()).devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.MONTH_JAN).startTime(now).endTime(now.plusSeconds(10L))
                .weekOfYear(-1).year(-1)
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id1 = orgDevProductivityReportV2DatabaseService.upsert("test", report1);
        report1 = report1.toBuilder().id(UUID.fromString(id1)).latest(true).build();

        OrgDevProductivityReport report1Read = orgDevProductivityReportV2DatabaseService.get("test", id1).orElse(null);
        verifyRecord(report1, report1Read);

        OrgDevProductivityReport report1Updated = report1.toBuilder().score(20).missingUserReportsCount(21).staleUserReportsCount(22).build();
        orgDevProductivityReportV2DatabaseService.upsert("test", report1Updated);
        OrgDevProductivityReport report1UpdatedRead = orgDevProductivityReportV2DatabaseService.get("test", report1.getId().toString()).orElse(null);
        verifyRecord(report1Updated, report1UpdatedRead);
        ouRefIdToReportMap.computeIfAbsent(dbOrgUnits.get(0).getRefId(), k -> new ArrayList<>()).add(report1UpdatedRead);

        Instant t0 = now.minus(12, ChronoUnit.DAYS);
        Instant t1 = now.minus(6, ChronoUnit.DAYS);

        int sizeBeforeUpsert = orgDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        //Test Upsert
        OrgDevProductivityReport report2 = OrgDevProductivityReport.builder()
                .ouID(dbOrgUnits.get(1).getId()).ouRefId(dbOrgUnits.get(1).getRefId()).devProductivityProfileId(devProdProfileId)
                .interval(ReportIntervalType.PAST_YEAR).startTime(t0).endTime(t0)
                .weekOfYear(-1).year(-1)
                .score(10).report(DevProductivityResponse.builder().build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();
        String id2 = orgDevProductivityReportV2DatabaseService.upsert(company, report2);
        report2 = report2.toBuilder().id(UUID.fromString(id2)).latest(true).build();
        int sizeAfterUpsert1 = orgDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert1 == sizeBeforeUpsert + 1);
        OrgDevProductivityReport report2Read = orgDevProductivityReportV2DatabaseService.get(company, id2).get();
        verifyRecord(report2, report2Read);
        ouRefIdToReportMap.computeIfAbsent(dbOrgUnits.get(1).getRefId(), k -> new ArrayList<>()).add(report2Read);

        //Test List By Filters
        List<OrgDevProductivityReport> expectedReports = List.of(report1UpdatedRead, report2Read);
        testAllListByFilters(expectedReports, ouRefIdToReportMap);
        DbListResponse<OrgDevProductivityReport> dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id2)), null, null, null, null, null, false);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report2, dbListResponse.getRecords().get(0));

        dbListResponse = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(UUID.fromString(id1)), null, null, null, null, null, false);
        Assert.assertNotNull(dbListResponse);
        Assert.assertEquals(1, dbListResponse.getTotalCount().intValue());
        Assert.assertEquals(1, dbListResponse.getRecords().size());
        verifyRecord(report1Updated, dbListResponse.getRecords().get(0));

        OrgDevProductivityReport report2Updated = report2.toBuilder()
                .startTime(t1).endTime(t1).score(20).report(DevProductivityResponse.builder().build())
                .missingUserReportsCount(21).staleUserReportsCount(22)
                .build();
        id2 = orgDevProductivityReportV2DatabaseService.upsert(company, report2Updated);
        int sizeAfterUpsert2 = orgDevProductivityReportV2DatabaseService.list(company, 0, 1000).getRecords().size();
        Assert.assertTrue(sizeAfterUpsert2 == sizeAfterUpsert1);
        OrgDevProductivityReport report2UpdatedRead = orgDevProductivityReportV2DatabaseService.get(company, id2).get();
        verifyRecord(report2Updated, report2UpdatedRead);
    }

    //region List By Filter
    private void testListByFiltersOURefIds(Map<Integer, List<OrgDevProductivityReport>> ouRefIdToReportMap) throws SQLException {
        for(Map.Entry<Integer, List<OrgDevProductivityReport>> e : ouRefIdToReportMap.entrySet()) {
            DbListResponse<OrgDevProductivityReport> res = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null,
                    null, null, null, List.of(e.getKey()), null, false);
            Assert.assertEquals(e.getValue().size(), res.getTotalCount().intValue());
            Assert.assertEquals(e.getValue().size(), res.getRecords().size());
            verifyRecords(e.getValue(), res.getRecords());
        }
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, null, null, List.of(Integer.MAX_VALUE), null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIntervals(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<ReportIntervalType, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getInterval));
        for (ReportIntervalType interval : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(interval);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null,null, List.of(interval), null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<ReportIntervalType> allIds = allExpected.stream().map(OrgDevProductivityReport::getInterval).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, null,allIds, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);
    }
    private void testListByFiltersDevProductivityProfileIds(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getDevProductivityProfileId));
        for (UUID devProductivityProfileId : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(devProductivityProfileId);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, List.of(devProductivityProfileId), null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(OrgDevProductivityReport::getDevProductivityProfileId).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, allIds, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, null, List.of(UUID.randomUUID()), null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersOUIds(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getOuID));
        for (UUID orgUserId : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(orgUserId);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(orgUserId),null, null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(OrgDevProductivityReport::getOuID).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, allIds, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, null, List.of(UUID.randomUUID()), null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(0, result.getTotalCount().intValue());
        Assert.assertTrue(CollectionUtils.isEmpty(result.getRecords()));
    }
    private void testListByFiltersIds(List<OrgDevProductivityReport> allExpected) throws SQLException {
        Map<UUID, List<OrgDevProductivityReport>> map = allExpected.stream().collect(Collectors.groupingBy(OrgDevProductivityReport::getId));
        for (UUID id : map.keySet()) {
            List<OrgDevProductivityReport> expected = map.get(id);
            DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(id), null, null, null, null, null, false);
            Assert.assertNotNull(result);
            Assert.assertEquals(expected.size(), result.getTotalCount().intValue());
            Assert.assertEquals(expected.size(), result.getCount().intValue());
            verifyRecords(result.getRecords(), expected);
        }
        List<UUID> allIds = allExpected.stream().map(OrgDevProductivityReport::getId).distinct().collect(Collectors.toList());
        DbListResponse<OrgDevProductivityReport> result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, allIds, null, null, null, null, null, false);
        Assert.assertNotNull(result);
        Assert.assertEquals(allExpected.size(), result.getTotalCount().intValue());
        Assert.assertEquals(allExpected.size(), result.getCount().intValue());
        verifyRecords(result.getRecords(), allExpected);

        result = orgDevProductivityReportV2DatabaseService.listByFilter(company, 0, 100, List.of(UUID.randomUUID()), null, null, null, null, null, false);
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
    }
    //endregion

    //region Verify Records
    private void verifyRecords(List<OrgDevProductivityReport> e, List<OrgDevProductivityReport> a) {
        Assert.assertEquals(CollectionUtils.isEmpty(e), CollectionUtils.isEmpty(a));
        if (CollectionUtils.isEmpty(e)) {
            return;
        }
        Assert.assertEquals(e.size(), a.size());
        Map<UUID, OrgDevProductivityReport> expectedMap = e.stream().collect(Collectors.toMap(OrgDevProductivityReport::getId, x -> x));
        Map<UUID, OrgDevProductivityReport> actualMap = a.stream().collect(Collectors.toMap(OrgDevProductivityReport::getId, x -> x));

        for (UUID key : expectedMap.keySet()) {
            verifyRecord(expectedMap.get(key), actualMap.get(key));
        }
    }
    private void verifyRecord(OrgDevProductivityReport expected, OrgDevProductivityReport actual) {
        Assert.assertEquals(expected.getOuID(), actual.getOuID());
        Assert.assertEquals(expected.getDevProductivityProfileId(), actual.getDevProductivityProfileId());
        Assert.assertEquals(expected.getInterval(), actual.getInterval());
        Assert.assertEquals(expected.getStartTime(), actual.getStartTime());
        Assert.assertEquals(expected.getEndTime(), actual.getEndTime());
        Assert.assertEquals(expected.getLatest(), actual.getLatest());
        Assert.assertEquals(expected.getScore(), actual.getScore());
        Assert.assertEquals(expected.getReport(), actual.getReport());
        Assert.assertNotNull(actual.getRequestedOUId());
        Assert.assertEquals(expected.getMissingUserReportsCount(), actual.getMissingUserReportsCount());
        Assert.assertEquals(expected.getStaleUserReportsCount(), actual.getStaleUserReportsCount());
    }
    //endregion

    @Test
    public void testData() throws SQLException {
        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        var orgUnit = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnit(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 0, integration1, integration2);

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0, List.of(orgUnit.getRefId()))).getId();

        String id2 = orgDevProductivityReportV2DatabaseService.upsert("test", OrgDevProductivityReport.builder()
                .ouID(orgUnit.getId()).ouRefId(orgUnit.getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.LAST_MONTH)
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(10L))
                        .weekOfYear(-1).year(-1)
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build());
        DbListResponse<OrgDevProductivityReport> output = orgDevProductivityReportV2DatabaseService.list("test", 0, 20);
        orgDevProductivityReportV2DatabaseService.upsert("test", output.getRecords().get(0));
        OrgDevProductivityReport output2 = orgDevProductivityReportV2DatabaseService.get("test", id2).orElse(null);
        assertThat(output.getRecords().size()).isGreaterThanOrEqualTo(1);
        assertThat(output2.getScore()).isEqualTo(10);
        assertThat(orgDevProductivityReportV2DatabaseService.list("test", 0, 5).getRecords().size()).isGreaterThanOrEqualTo(1);
        orgDevProductivityReportV2DatabaseService.delete("test", id2);
        OrgDevProductivityReport output3 = orgDevProductivityReportV2DatabaseService.get("test", id2).orElse(null);
        assertThat(output3).isEqualTo(null);
    }

    @Test
    public void testWeeklyAndBiWeeklyData() throws SQLException {
        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        var orgUnit = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnit(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 0, integration1, integration2);

        TicketCategorizationScheme scheme = TicketCategorizationSchemeDatabaseServiceTestUtils.createTicketCategorizationScheme(ticketService, company, 1);
        UUID devProdProfileId = (createDevProdProfile(devProductivityProfileDatabaseService, company, scheme.getId(), 0, List.of(orgUnit.getRefId()))).getId();

        ReportIntervalType week_26_2023 = ReportIntervalType.LAST_WEEK;
        IntervalTimeRange range_26_2023 = week_26_2023.getIntervalTimeRange(convertDate("6-26-2023"));

        ReportIntervalType week26_2023_1 = ReportIntervalType.LAST_WEEK;
        IntervalTimeRange range26_2023_1 = week26_2023_1.getIntervalTimeRange(convertDate("6-29-2023"));

        ReportIntervalType week_27_2023 = ReportIntervalType.LAST_WEEK;
        IntervalTimeRange range_27_2023 = week_27_2023.getIntervalTimeRange(convertDate("7-4-2023"));

        OrgDevProductivityReport expected1 = OrgDevProductivityReport.builder()
                .ouID(orgUnit.getId()).ouRefId(orgUnit.getRefId())
                .devProductivityProfileId(devProdProfileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.LAST_WEEK)
                .startTime(Instant.ofEpochSecond(range_26_2023.getTimeRange().getLeft()))
                .endTime(Instant.ofEpochSecond(range_26_2023.getTimeRange().getRight()))
                .weekOfYear(range_26_2023.getWeekOfTheYear())
                .year(range_26_2023.getYear())
                .score(10)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("satish@levelops.io").fullName("Satish Kumar Singh").build())
                .missingUserReportsCount(11).staleUserReportsCount(12)
                .build();

        String id1 = orgDevProductivityReportV2DatabaseService.upsert(company, expected1);
        expected1 = expected1.toBuilder().id(UUID.fromString(id1)).latest(true).build();

        OrgDevProductivityReport actual1 = orgDevProductivityReportV2DatabaseService.get(company, id1).orElse(null);
        verifyRecord(expected1, actual1);

        DbListResponse<OrgDevProductivityReport> output = orgDevProductivityReportV2DatabaseService.list(company, 0, 20);
        verifyRecords(List.of(expected1), output.getRecords());

        expected1 = expected1.toBuilder().weekOfYear(range26_2023_1.getWeekOfTheYear())
                .year(range26_2023_1.getYear()).build();

        String id2 = orgDevProductivityReportV2DatabaseService.upsert(company, expected1);
        output = orgDevProductivityReportV2DatabaseService.list(company, 0, 20);
        Assert.assertEquals(output.getRecords().get(0).getLatest(), true);
        Assert.assertEquals(output.getRecords().size(),1);
        Assert.assertEquals(id1, id2);

        expected1 = expected1.toBuilder().weekOfYear(range_27_2023.getWeekOfTheYear())
                .year(range_27_2023.getYear()).build();

        String id3 = orgDevProductivityReportV2DatabaseService.upsert(company, expected1);
        output = orgDevProductivityReportV2DatabaseService.list(company, 0, 20);
        Assert.assertEquals(output.getRecords().size(),2);
        Assert.assertEquals(output.getRecords().get(0).getLatest(), true);
        Assert.assertEquals(output.getRecords().get(1).getLatest(), false);
        Assert.assertNotEquals(id1, id3);
        OrgDevProductivityReport actual_week_27 = orgDevProductivityReportV2DatabaseService.get(company, id3).orElse(null);
        expected1 = expected1.toBuilder().id(UUID.fromString(id3)).latest(true).build();
        verifyRecord(expected1, actual_week_27);

    }

    private static Instant convertDate(String date) {
        return LocalDate.parse(date, DateTimeFormatter.ofPattern("M-d-yyyy", Locale.US)).atStartOfDay().atZone(ZoneId.of("UTC")).toInstant();
    }

}