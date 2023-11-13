package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.util.Lists;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityResponse;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.IndustryDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.OrgDevProductivityReport;
import io.levelops.commons.databases.models.database.dev_productivity.RelativeScore;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.dev_productivity.SectionResponse;
import io.levelops.commons.databases.models.database.dev_productivity.UserDevProductivityReport;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.services.*;
import io.levelops.commons.databases.services.organization.*;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static io.levelops.commons.databases.services.ScmCommitUtils.arrayUniq;
import static org.assertj.core.api.Assertions.assertThat;
public class DevProductivityRelativeScoreServiceTest {
    String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static final ObjectMapper mapper = DefaultObjectMapper.get();
    private static OrgVersionsDatabaseService orgVersionsService;
    private static DataSource dataSource;
    JdbcTemplate template;
    UserDevProductivityReportDatabaseService userDevProductivityReportDatabaseService;
    UserDevProductivityESReportDatabaseService userDevProductivityESReportDatabaseService;
    private static UserIdentityService userIdentityService;
    OrgUsersDatabaseService orgUsersDatabaseService;
    private static IntegrationService integrationService;
    private static OrgAndUsersDevProductivityReportMappingsDBService productivityReportMappingsDBService;
    TicketCategorizationSchemeDatabaseService databaseService;
    DevProductivityParentProfileDatabaseService devProductivityParentProfileDatabaseService;
    DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    OrgIdentityService orgIdentityService;

    static DevProductivityRelativeScoreService devProductivityRelativeScoreService;
    OrgUnitsDatabaseService orgUnitsDatabaseService;
    private TagItemDBService tagItemService;
    private  OrgUsersDatabaseService usersService;
    private  OrgVersionsDatabaseService versionsService;
    OrgDevProductivityReportDatabaseService orgDevProductivityReportsDatabaseService;
    OrgDevProductivityESReportDatabaseService orgDevProductivityESReportsDatabaseService;
    private OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private OUOrgUserMappingDatabaseService ouOrgUserMappingDatabaseService;
    private UserService userService;
    private DashboardWidgetService dashboardWidgetService;
    private OrgProfileDatabaseService ouProfileDbService;
    private VelocityConfigsDatabaseService velocityConfigDbService;
    private CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private CiCdInstancesDatabaseService ciCdInstancesDatabaseService;

    private static final String LEVELOPS_INVENTORY_SCHEMA ="_levelops";
    IndustryDevProductivityReportDatabaseService industryDevProductivityReportDatabaseService;

    private UserDevProductivityReportV2DatabaseService userDevProductivityReportV2DatabaseService;
    private OrgDevProductivityReportV2DatabaseService orgDevProductivityReportV2DatabaseService;

    private static String sectionNames = "Quality, Speed, Volume, Impact, Proficieny, Leadership";

    private static String qualityFeatures = "Percentage of Rework, Percentage of Legacy Rework";
    private static String speedFeatures = "Average Coding days per week, Average PR Cycle Time, Average Issue Resolution Time";
    private static String volumeFeatures =  "Number of PRs per month, Number of Commits per month, Lines of Code per month, Number of Bugs fixed per month, Number of Stories resolved per month, Number of Story Points delivered per month";
    private static String impactFeatures = "Major bugs resolved per month, Major stories resolved per month";
    private static String proficiencyFeatures = "Technical Breadth - Number of unique file extension, Repo Breadth - Number of unique repo";
    private static String leadershipFeatues = "Number of PRs reviewed per month, Average response time for PR";

    private static String featureNames = "Percentage of Rework, Percentage of Legacy Rework, Major bugs resolved per month, Major stories resolved per month, Number of PRs per month, Number of Commits per month, Lines of Code per month, Number of Bugs fixed per month,"+
            "Number of Stories resolved per month, Number of Story Points delivered per month, Average Coding days per week, Average PR Cycle Time, Average Issue Resolution Time, Technical Breadth - Number of unique file extension,"+
            "Repo Breadth - Number of unique repo, Number of PRs reviewed per month, Average response time for PR";

    @Before
    public void setUp() throws Exception {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement(arrayUniq)
                .execute();
        userDevProductivityReportDatabaseService = new UserDevProductivityReportDatabaseService(dataSource, DefaultObjectMapper.get());
        template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);
        System.out.println(template.queryForObject("SELECT current_database();", String.class));
        userDevProductivityReportDatabaseService.populateData = false;
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence("test");
        userIdentityService=new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence("test");
        productivityReportMappingsDBService = new OrgAndUsersDevProductivityReportMappingsDBService(dataSource);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence("test");
        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,mapper);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,mapper,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        orgUsersDatabaseService.ensureTableExistence("test");
        databaseService= new TicketCategorizationSchemeDatabaseService(dataSource,mapper);
        databaseService.ensureTableExistence("test");
        devProductivityProfileDatabaseService=new DevProductivityProfileDatabaseService(dataSource,mapper);
        devProductivityProfileDatabaseService.ensureTableExistence("test");
        userDevProductivityReportDatabaseService.ensureTableExistence("test");
        orgIdentityService = new OrgIdentityService(dataSource);
        userService = new UserService(dataSource,mapper);
        userService.ensureTableExistence(company);
        industryDevProductivityReportDatabaseService = new IndustryDevProductivityReportDatabaseService(dataSource, DefaultObjectMapper.get());
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(LEVELOPS_INVENTORY_SCHEMA);
        industryDevProductivityReportDatabaseService.ensureTableExistence(LEVELOPS_INVENTORY_SCHEMA);
        tagItemService=new TagItemDBService(dataSource);
        versionsService=new OrgVersionsDatabaseService(dataSource);
        usersService=new OrgUsersDatabaseService(dataSource,mapper,versionsService, userIdentityService);
        new ProductService(dataSource).ensureTableExistence(company);
        dashboardWidgetService = new DashboardWidgetService(dataSource,mapper);
        dashboardWidgetService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, usersService, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService=new OrgUnitsDatabaseService(dataSource,mapper,tagItemService,usersService, versionsService, dashboardWidgetService);
        orgUnitsDatabaseService.ensureTableExistence("test");
        orgDevProductivityReportsDatabaseService = new OrgDevProductivityReportDatabaseService(dataSource, DefaultObjectMapper.get());
        orgDevProductivityReportsDatabaseService.ensureTableExistence("test");
        productivityReportMappingsDBService.ensureTableExistence(company);
        ouOrgUserMappingDatabaseService = new OUOrgUserMappingDatabaseService(dataSource);
        ouOrgUserMappingDatabaseService.ensureTableExistence(company);

        userDevProductivityReportV2DatabaseService = new UserDevProductivityReportV2DatabaseService(dataSource, mapper, orgUsersDatabaseService);
        userDevProductivityReportV2DatabaseService.ensureTableExistence(company);
        orgDevProductivityReportV2DatabaseService = new OrgDevProductivityReportV2DatabaseService(dataSource, mapper, orgUnitsDatabaseService);
        orgDevProductivityReportV2DatabaseService.ensureTableExistence(company);

        userDevProductivityESReportDatabaseService = new UserDevProductivityESReportDatabaseService(dataSource, mapper);
        userDevProductivityESReportDatabaseService.ensureTableExistence(company);
        orgDevProductivityESReportsDatabaseService = new OrgDevProductivityESReportDatabaseService(dataSource, mapper);
        orgDevProductivityESReportsDatabaseService.ensureTableExistence(company);

        devProductivityRelativeScoreService = new DevProductivityRelativeScoreService(dataSource, orgIdentityService, userDevProductivityReportDatabaseService, orgDevProductivityReportsDatabaseService, industryDevProductivityReportDatabaseService, orgDevProductivityESReportsDatabaseService, userDevProductivityESReportDatabaseService, userDevProductivityReportV2DatabaseService, orgDevProductivityReportV2DatabaseService);
    }

    @Test
    public void test() throws Exception {

        var integration1 = Integration.builder()
                .description("description1")
                .name("integ1")
                .url("url")
                .application("application")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));
        DBOrgUser orgUser1 = DBOrgUser.builder()
                .email("ashish@levelops.io")
                .fullName("ashish-levelops")
                .customFields(Map.of("test_name","test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("ashish").username("ashish").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        OrgUserId userId1 = orgUsersDatabaseService.upsert(company, orgUser1);

        UUID orgUserId = userId1.getId();
        UUID orgId = UUID.randomUUID();

        DevProductivityProfile devProductivityProfile = DevProductivityProfile.builder()
                .id(UUID.randomUUID())
                .associatedOURefIds(List.of("1"))
                .name("Default Profile").description("Default Profile")
                .defaultProfile(true)
                .sections(List.of())
                .settings(Map.of())
                .build();
        String id= devProductivityProfileDatabaseService.insert(company, devProductivityProfile);

        UUID profileId = UUID.fromString(id);

        DevProductivityResponse response = DevProductivityResponse.builder()
                .orgUserId(orgUserId)
                .orgId(orgId)
                .email("ashish@levelops.io")
                .fullName("ashish-levelops")
                .orgName("ou-1")
                .build();

        LinkedHashMap<String, Integer> industryMap = new LinkedHashMap<>();
        industryMap.put("month_jan", 80);
        industryMap.put("month_feb", 70);
        industryMap.put("month_mar", 60);
        industryMap.put("month_apr", 82);
        industryMap.put("month_may", 80);
        industryMap.put("month_jun", 58);
        industryMap.put("month_jul", 88);
        industryMap.put("month_aug", 76);
        industryMap.put("month_sep", 72);
        industryMap.put("month_oct", 89);
        industryMap.put("month_nov", 92);
        industryMap.put("month_dec", 88);

        industryMap.forEach( (interval, score)  -> {
            try {
                insertIndustryDevProductivityResponse(interval , score);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        LinkedHashMap<String, Integer> map = new LinkedHashMap<>();
        map.put("month_feb", 65);
        map.put("month_mar", 50);
        map.put("month_apr", 72);
        map.put("month_may", 80);
        map.put("month_jun", 68);
        map.put("month_jul", 78);
        map.put("month_aug", 66);
        map.put("month_sep", 92);
        map.put("month_oct", 86);
        map.put("month_nov", 82);
        map.put("month_dec", 80);
        map.put("month_jan", 90);

        List<Long> startTimeList = List.of(1612137600l, 1614556800l, 1617235200l, 1619827200l,
                1622505600l,1625097600l, 1627776000l, 1630454400l,1633046400l, 1635724800l, 1638316800l, 1640995200l);

        AtomicInteger i = new AtomicInteger();
        map.forEach( (interval, score)  -> {
            try {
                insertDevProductivityResponse(response, interval , score, profileId, startTimeList.get(i.getAndIncrement()));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        var ou1 = DBOrgUnit.builder()
                .name("ou-1")
                .description("ou-1")
                .active(true)
                .managers(Set.of())
                .refId(1)
                .build();
        var unitId = orgUnitsDatabaseService.insertForId(company, ou1);

        LinkedHashMap<String, Integer> map2 = new LinkedHashMap<>();
        map2.put("month_mar", 50);
        map2.put("month_apr", 72);
        map2.put("month_may", 80);
        map2.put("month_jun", 68);
        map2.put("month_jul", 78);
        map2.put("month_aug", 66);
        map2.put("month_sep", 92);
        map2.put("month_oct", 86);
        map2.put("month_nov", 82);

        AtomicInteger j = new AtomicInteger(1);

        map2.forEach( (interval, score)  -> {
            try {
                insertOrgProductivityResponse(unitId.getKey(), interval , score, profileId, startTimeList.get(j.getAndIncrement()));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        DevProductivityUserIds orgIds1 = DevProductivityUserIds.builder()
                .idType(IdType.ORG_IDS)
                .orgIds(List.of(unitId.getKey()))
                .build();

        DevProductivityUserIds userIds2 = DevProductivityUserIds.builder()
                .idType(IdType.OU_USER_IDS)
                .id(userId1.getId())
                .build();
        ouOrgUserMappingDatabaseService.upsertOUOrgUserMappingsBulk(company,unitId.getLeft(), List.of(userId1.getId()));
        List<DevProductivityUserIds>  list =  List.of(orgIds1, userIds2);
        Map<UUID,List<UUID>> ouIdDevProdProfilesMap = Maps.newHashMap();
        ouIdDevProdProfilesMap.put(userIds2.getId(),List.of(profileId));
        ouIdDevProdProfilesMap.put(unitId.getKey(),List.of(profileId));
        List<RelativeScore> res = devProductivityRelativeScoreService.listByFilter(company, ouIdDevProdProfilesMap, AGG_INTERVAL.month, list, null, false, 0, 100).getRecords();
        Assertions.assertNotNull(res);
        assertThat(res.size()).isEqualTo(24);
        assertThat(res.get(0).getReportList().size()).isEqualTo(1);
        assertThat(res.get(4).getReportList().size()).isEqualTo(1);

        var ou2 = DBOrgUnit.builder()
                .name("ou-2")
                .description("ou-2")
                .active(true)
                .managers(Set.of())
                .refId(2)
                .build();

        var unitId2 = orgUnitsDatabaseService.insertForId(company, ou2);

        LinkedHashMap<String, Integer> map3 = new LinkedHashMap<>();
        map3.put("month_feb", 66);
        map3.put("month_mar", 48);
        map3.put("month_apr", 80);
        map3.put("month_may", 64);
        map3.put("month_jun", 72);
        map3.put("month_jul", 76);
        map3.put("month_aug", 60);
        map3.put("month_sep", 45);
        map3.put("month_oct", 38);

        AtomicInteger k = new AtomicInteger();

        map3.forEach( (interval, score)  -> {
            try {
                insertOrgProductivityResponse(unitId2.getKey(), interval , score, profileId, startTimeList.get(k.getAndIncrement()));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        DevProductivityUserIds orgIds2 = DevProductivityUserIds.builder()
                .idType(IdType.ORG_IDS)
                .id(unitId2.getKey())
                .build();

        list =  List.of(orgIds1, orgIds2);
        res = devProductivityRelativeScoreService.listByFilter(company, ouIdDevProdProfilesMap, AGG_INTERVAL.month, list, null, false, 0, 100).getRecords();
        Assertions.assertNotNull(res);
        assertThat(res.size()).isEqualTo(21);
        assertThat(res.get(4).getReportList().size()).isEqualTo(1);


        list =  List.of(orgIds1, orgIds2, userIds2);
        res = devProductivityRelativeScoreService.listByFilter(company, ouIdDevProdProfilesMap, AGG_INTERVAL.month, list, null, false, 0, 100).getRecords();
        Assertions.assertNotNull(res);
        assertThat(res.size()).isEqualTo(24);
        assertThat(res.get(0).getReportList().size()).isEqualTo(1);
        assertThat(res.get(4).getReportList().size()).isEqualTo(1);

    }

    @Test
    public void testWeeklyAndBiWeekly() throws Exception {

        Integration integration1 = IntegrationUtils.createIntegration(integrationService, company, 1);
        Integration integration2 = IntegrationUtils.createIntegration(integrationService, company, 2);
        var integrationId1 = Integer.parseInt(integration1.getId());
        var integrationId2 = Integer.parseInt(integration2.getId());
        DBOrgUser orgUser1 = DBOrgUser.builder()
                .email("ashish@levelops.io")
                .fullName("ashish-levelops")
                .customFields(Map.of("test_name","test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("ashish").username("ashish").integrationType(integration1.getApplication()).integrationId(integrationId1).build()))
                .versions(Set.of(1))
                .build();
        OrgUserId userId1 = orgUsersDatabaseService.upsert(company, orgUser1);

        UUID orgUserId = userId1.getId();
        UUID orgId = UUID.randomUUID();

        DevProductivityProfile devProductivityProfile = DevProductivityProfile.builder()
                .id(UUID.randomUUID())
                .associatedOURefIds(List.of("1"))
                .name("Default Profile").description("Default Profile")
                .defaultProfile(true)
                .sections(List.of())
                .settings(Map.of())
                .build();
        String id= devProductivityProfileDatabaseService.insert(company, devProductivityProfile);

        UUID profileId = UUID.fromString(id);

        DevProductivityResponse response = DevProductivityResponse.builder()
                .orgUserId(orgUserId)
                .orgId(orgId)
                .email("ashish@levelops.io")
                .fullName("ashish-levelops")
                .orgName("ou-1")
                .build();

        LinkedHashMap<String, Integer> industryMap = new LinkedHashMap<>();
        industryMap.put("last_week", 80);

        industryMap.forEach( (interval, score)  -> {
            try {
                insertIndustryDevProductivityResponse(interval , score);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        int sampleSize = 18;
        List<Integer> scoresList = List.of(65,50,72,80,68,78,66,92,86,82,80,90,74,83,61,53,29,35);
        List<Long> startTimeList = new ArrayList<>();
        List<Long> endTimeList = new ArrayList<>();
        List<Integer> weeksList = new ArrayList<>();
        List<Integer> yearsList = new ArrayList<>();
        Long startTime = 1670198400l;
        Long endTime = 1670846399l;
        LocalDate date = null;
        for(int i = 0; i < sampleSize; i++){
            Long startTime1 = startTime+(i*86400*7);
            startTimeList.add(startTime1);
            endTimeList.add(endTime+(i*86400*7));
            date = LocalDate.ofInstant(Instant.ofEpochSecond(startTime1+1000l), ZoneId.of("UTC"));
            weeksList.add(date.get(ChronoField.ALIGNED_WEEK_OF_YEAR));
            yearsList.add(date.getYear());
        }

        AtomicInteger i = new AtomicInteger();
        scoresList.forEach( (score)  -> {
            try {
                int i1 = i.getAndIncrement();
                insertDevProductivityResponse(response, "last_week" , score, profileId, startTimeList.get(i1), endTimeList.get(i1), weeksList.get(i1), yearsList.get(i1));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        List<DBOrgUnit> dbOrgUnitsV1 = OrgUnitsDatabaseServiceTestUtils.createDBOrgUnits(orgUnitsDatabaseService, orgUnitCategoryDatabaseService, company, 1, integration1, integration2);
        AtomicInteger j = new AtomicInteger();

        scoresList.forEach( (score)  -> {
            try {
                int i1 = j.getAndIncrement();
                insertOrgProductivityResponse(dbOrgUnitsV1.get(0).getId(), "last_week" , score, profileId, startTimeList.get(i1), endTimeList.get(i1), weeksList.get(i1), yearsList.get(i1));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        });

        DevProductivityUserIds orgIds1 = DevProductivityUserIds.builder()
                .idType(IdType.ORG_IDS)
                .orgIds(List.of(dbOrgUnitsV1.get(0).getId()))
                .build();

        DevProductivityUserIds userIds2 = DevProductivityUserIds.builder()
                .idType(IdType.OU_USER_IDS)
                .id(userId1.getId())
                .build();
        ouOrgUserMappingDatabaseService.upsertOUOrgUserMappingsBulk(company,dbOrgUnitsV1.get(0).getId(), List.of(userId1.getId()));
        List<DevProductivityUserIds>  list =  List.of(orgIds1, userIds2);
        Map<UUID,List<UUID>> ouIdDevProdProfilesMap = Maps.newHashMap();
        ouIdDevProdProfilesMap.put(userIds2.getId(),List.of(profileId));
        ouIdDevProdProfilesMap.put(dbOrgUnitsV1.get(0).getId(),List.of(profileId));
        List<RelativeScore> res = devProductivityRelativeScoreService.listByFilterV2(company, ouIdDevProdProfilesMap, AGG_INTERVAL.week, list, null, false, 0, 100).getRecords();
        Assertions.assertNotNull(res);
        assertThat(res.size()).isEqualTo(18);
        assertThat(res.get(0).getReportList().size()).isEqualTo(3);
        assertThat(res.get(4).getReportList().size()).isEqualTo(2);

        res = devProductivityRelativeScoreService.listByFilterV2(company, ouIdDevProdProfilesMap, AGG_INTERVAL.week, list, null, false, 0, 12).getRecords();
        Assertions.assertNotNull(res);
        assertThat(res.size()).isEqualTo(12);
        assertThat(res.get(0).getReportList().size()).isEqualTo(3);
        assertThat(res.get(11).getReportList().size()).isEqualTo(2);
        assertThat(res.get(0).getKey()).isEqualTo(1680480000l);
        assertThat(res.get(11).getKey()).isEqualTo(1673827200l);

    }

    private void insertIndustryDevProductivityResponse( String interval, Integer score) throws SQLException {

        IndustryDevProductivityReport report = IndustryDevProductivityReport.builder()
                .interval(ReportIntervalType.fromString(interval))
                .score(score)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("ashish@levelops.io").fullName("ashish-levelops").build())
                .build();
        industryDevProductivityReportDatabaseService.insert(LEVELOPS_INVENTORY_SCHEMA, report);
    }

    private void insertOrgProductivityResponse(UUID key, String interval, Integer score, UUID profileId, Long startTime) throws SQLException{

        OrgDevProductivityReport report = OrgDevProductivityReport.builder()
                .ouID(key).ouRefId(1)
                .devProductivityProfileId(profileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.fromString(interval))
                .startTime(Instant.ofEpochSecond(startTime))
                .endTime(Instant.now().plusSeconds(10L))
                .score(score)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("ashish@levelops.io").fullName("ashish-levelops").build())
                .build();

        orgDevProductivityReportsDatabaseService.insert(company,report);
    }

    private void insertOrgProductivityResponse(UUID key, String interval, Integer score, UUID profileId, Long startTime, Long endTime, Integer weekOfYear, Integer year) throws SQLException{

        OrgDevProductivityReport report = OrgDevProductivityReport.builder()
                .ouID(key).ouRefId(1)
                .devProductivityProfileId(profileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.fromString(interval))
                .startTime(Instant.ofEpochSecond(startTime))
                .endTime(Instant.ofEpochSecond(endTime))
                .weekOfYear(weekOfYear)
                .year(year)
                .score(score)
                .report(DevProductivityResponse.builder().orgUserId(UUID.randomUUID()).orgName("test")
                        .email("ashish@levelops.io").fullName("ashish-levelops").build())
                .build();

        orgDevProductivityReportV2DatabaseService.upsert(company,report);
    }

    private void insertDevProductivityResponse(DevProductivityResponse response, String interval, int score, UUID profileId, Long startTime) throws SQLException {

        response = response.toBuilder()
                .score(score)
                .sectionResponses(getSectionResponse(score))
                .build();

        UserDevProductivityReport report = UserDevProductivityReport.builder()
                .orgUserId(response.getOrgUserId()).orgUserRefId(1)
                .devProductivityProfileId(profileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.fromString(interval))
                .startTime(Instant.ofEpochSecond(startTime))
                .endTime(Instant.now())
                .score(response.getScore())
                .report(response)
                .build();

        userDevProductivityReportDatabaseService.insert(company, report);
    }

    private void insertDevProductivityResponse(DevProductivityResponse response, String interval, int score, UUID profileId, Long startTime, Long endTime, Integer weekOfYear, Integer year) throws SQLException {

        response = response.toBuilder()
                .score(score)
                .sectionResponses(getSectionResponse(score))
                .build();

        UserDevProductivityReport report = UserDevProductivityReport.builder()
                .orgUserId(response.getOrgUserId()).orgUserRefId(1)
                .devProductivityProfileId(profileId).devProductivityProfileTimestamp(Instant.now())
                .interval(ReportIntervalType.fromString(interval))
                .startTime(Instant.ofEpochSecond(startTime))
                .endTime(Instant.ofEpochSecond(endTime))
                .weekOfYear(weekOfYear).year(year)
                .score(response.getScore())
                .report(response)
                .build();

        userDevProductivityReportV2DatabaseService.upsert(company, report);
    }

    private List<SectionResponse> getSectionResponse(int score) {

        LinkedHashMap<String, List<String>> sections = new LinkedHashMap<>();
        sections.put("Quality", Arrays.asList(qualityFeatures.split(",")));
        sections.put("Impact", Arrays.asList(impactFeatures.split(",")));
        sections.put("Speed", Arrays.asList(speedFeatures.split(",")));
        sections.put("Volume", Arrays.asList(volumeFeatures.split(",")));
        sections.put("Proficiency", Arrays.asList(proficiencyFeatures.split(",")));
        sections.put("Leadership", Arrays.asList(leadershipFeatues.split(",")));

        List<SectionResponse> sectionResponses = Lists.newArrayList();
        int sectionOrder = 0;
        for (Map.Entry<String, List<String>> entry : sections.entrySet()) {
            List<FeatureResponse> featureResponses = Lists.newArrayList();
            int featureOrder = 0;
            for (String feature : entry.getValue()) {
                FeatureResponse featureResponse = FeatureResponse.builder()
                        .name(feature)
                        .description(feature)
                        //.mean(10d)
                        //.result(10l)
                        .score(score)
                        .order(featureOrder)
                        .sectionOrder(sectionOrder)
                        .build();
                featureResponses.add(featureResponse);
                featureOrder++;
            }
            SectionResponse sectionResponse = SectionResponse.builder()
                    .name(entry.getKey())
                    .description(entry.getKey())
                    .featureResponses(featureResponses)
                    .order(sectionOrder)
                    .score(score)
                    .build();
            sectionOrder++;
            sectionResponses.add(sectionResponse);
        }
        return sectionResponses;
    }
}