package io.levelops.commons.databases.services.dev_productivity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityUserIds;
import io.levelops.commons.databases.models.database.dev_productivity.IdType;
import io.levelops.commons.databases.models.database.dev_productivity.OrgAndUsersDevProductivityReportMappings;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.ReportIntervalType;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrgIdentityServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static OrgUsersDatabaseService orgUsersService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static OrgIdentityService orgIdentityService;
    private static OrgUnitsDatabaseService orgUnitsService;
    private static UserService userService;
    private static OrgAndUsersDevProductivityReportMappingsDBService orgAndUsersDevProductivityReportMappingsDBService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUnitCategory orgGroup1;
    private static String orgGroupId1;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                "CREATE SCHEMA IF NOT EXISTS " + company,
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        ).forEach(template.getJdbcTemplate()::execute);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgVersionsService.ensureTableExistence(company);
        orgUsersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        orgUsersService.ensureTableExistence(company);
        orgIdentityService = new OrgIdentityService(dataSource);
        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);
        DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        dashboardWidgetService.ensureTableExistence(company);
        new ProductService(dataSource).ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, mapper, null, orgUsersService, orgVersionsService, dashboardWidgetService), integrationService), mapper);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, null, orgUsersService, orgVersionsService, dashboardWidgetService);
        orgUnitsService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, DefaultObjectMapper.get());
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDatabaseService = new DevProductivityProfileDatabaseService(dataSource, DefaultObjectMapper.get());
        devProductivityProfileDatabaseService.ensureTableExistence(company);
        orgAndUsersDevProductivityReportMappingsDBService = new OrgAndUsersDevProductivityReportMappingsDBService(dataSource);
        orgAndUsersDevProductivityReportMappingsDBService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException {

        var integration1 = Integration.builder()
                .description("Test Jira Integration")
                .id("1")
                .name("Jira-Integration")
                .url("Jira-Url")
                .application("Jira")
                .status("active")
                .build();
        var integrationId1 = Integer.valueOf(integrationService.insert(company, integration1));

        var integration2 = Integration.builder()
                .description("Test github Integration")
                .id("2")
                .name("Github-Integration")
                .url("Git-Url")
                .application("Github")
                .status("active")
                .build();

        var integrationId2 = Integer.valueOf(integrationService.insert(company, integration2));

        var orgUser1 = DBOrgUser.builder()
                .email("ashish@levelops.io")
                .fullName("Ashish")
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("ashish-levelops").username("ashish-levelops").
                                integrationType(integration1.getApplication()).integrationId(integrationId1).build(),
                        DBOrgUser.LoginId.builder().cloudId("ashish").username("ashish").
                                integrationType(integration2.getApplication()).integrationId(integrationId2).build()))
                .versions(Set.of(1))
                .active(true)
                .build();

        var userId1 = Integer.valueOf(orgUsersService.insert(company, orgUser1));

        DBOrgUser dbUser1 = orgUsersService.get(company, userId1).get();

        DBOrgContentSection dbOrgContentSection1 = DBOrgContentSection.builder()
                .integrationId(Integer.valueOf(integrationId1))
                .integrationName("application")
                .users(Set.of(1, 2))
                .defaultSection(true)
                .build();
        orgGroup1 = OrgUnitCategory.builder()
                .name("TEAM A")
                .description("Sample team")
                .isPredefined(true)
                .build();
        orgGroupId1 = orgUnitCategoryDatabaseService.insert(company, orgGroup1);
        DBOrgUnit unit1 = DBOrgUnit.builder()
                .refId(1)
                .name("ou-1")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection1))
                .versions(Set.of(1))
                .managers(Set.of(dbUser1))
                .ouCategoryId(UUID.fromString(orgGroupId1))
                .build();

        UUID refId1 = orgUnitsService.insertForId(company, unit1).getLeft();
        orgUnitsService.update(company,refId1,true);

        DBOrgUnit unit2 = DBOrgUnit.builder()
                .refId(2)
                .name("ou-2")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection1))
                .versions(Set.of(1))
                .managers(Set.of(dbUser1))
                .build();

        UUID refId2 = orgUnitsService.insertForId(company, unit2).getLeft();
        orgUnitsService.update(company,refId2,true);

        DBOrgUnit unit3 = DBOrgUnit.builder()
                .refId(3)
                .name("ou-3")
                .active(true)
                .parentRefId(null)
                .sections(Set.of(dbOrgContentSection1))
                .versions(Set.of(1))
                .managers(Set.of(dbUser1))
                .build();

        UUID refId3 = orgUnitsService.insertForId(company, unit3).getLeft();
        orgUnitsService.update(company,refId3,true);

        DevProductivityUserIds devUserId1 = DevProductivityUserIds.builder()
                .idType(IdType.OU_USER_IDS)
                .userIdList(List.of(dbUser1.getId()))
                .build();

        DevProductivityProfile profile1 = DevProductivityProfile.builder()
                .name("test1")
                .associatedOURefIds(List.of("1","2"))
                .build();
        DevProductivityProfile profile2 = DevProductivityProfile.builder()
                .name("test2")
                .associatedOURefIds(List.of("3"))
                .build();

        String profileId1 = devProductivityProfileDatabaseService.insert(company, profile1);
        String profileId2 = devProductivityProfileDatabaseService.insert(company, profile2);
        OrgAndUsersDevProductivityReportMappings mapping1 = OrgAndUsersDevProductivityReportMappings.builder()
                .ouID(refId1)
                .devProductivityProfileId(UUID.fromString(profileId1))
                .orgUserIds(List.of(dbUser1.getId()))
                .interval(ReportIntervalType.LAST_MONTH)
                .build();

        OrgAndUsersDevProductivityReportMappings mapping2 = OrgAndUsersDevProductivityReportMappings.builder()
                .ouID(refId2)
                .devProductivityProfileId(UUID.fromString(profileId1))
                .orgUserIds(List.of(dbUser1.getId()))
                .interval(ReportIntervalType.LAST_MONTH)
                .build();

        OrgAndUsersDevProductivityReportMappings mapping3 = OrgAndUsersDevProductivityReportMappings.builder()
                .ouID(refId3)
                .devProductivityProfileId(UUID.fromString(profileId2))
                .orgUserIds(List.of(dbUser1.getId()))
                .interval(ReportIntervalType.LAST_QUARTER)
                .build();

        orgAndUsersDevProductivityReportMappingsDBService.upsert(company,mapping1);
        orgAndUsersDevProductivityReportMappingsDBService.upsert(company,mapping2);
        orgAndUsersDevProductivityReportMappingsDBService.upsert(company,mapping3);

        List<OrgUserDetails> userDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company, devUserId1, 0, 100).getRecords();

        Assertions.assertThat(userDetailsList.size()).isEqualTo(1);
        Assertions.assertThat(userDetailsList.get(0).getFullName()).isEqualTo("Ashish");
        Assertions.assertThat(userDetailsList.get(0).getEmail()).isEqualTo("ashish@levelops.io");
        Assertions.assertThat(userDetailsList.get(0).getCustomFields()).isEqualTo(Map.of("test_name", "test1"));
        Assertions.assertThat(userDetailsList.get(0).getIntegrationUserDetailsList().size()).isEqualTo(2);
        Assertions.assertThat(userDetailsList.get(0).getDevProductivityProfiles().size()).isEqualTo(1);
        // Assertions.assertThat(userDetailsList.get(0).getDevProductivityProfiles().get(0).getAssociatedOUs().size()).isEqualTo(2);

        DevProductivityUserIds devUserId2 = DevProductivityUserIds.builder()
                .userIdType(IdType.INTEGRATION_USER_IDS)
                .userIdList(userDetailsList.get(0).getIntegrationUserDetailsList().stream()
                        .map(i -> i.getIntegrationUserId()).collect(Collectors.toList()))
                .build();

        orgUsersService.linkCloudIds(company, devUserId1.getUserIdList().get(0), orgUser1.getIds(), DbScmUser.MappingStatus.MANUAL);
        userDetailsList = orgIdentityService.getUserIdentityForAllIntegrations(company,devUserId2,0,100).getRecords();
        List<UUID> id=orgIdentityService.getOrgUnitForOrgRef(company,List.of(1));
        Assertions.assertThat(id).isNotEqualTo(null);
        Assertions.assertThat(userDetailsList.size()).isEqualTo(1);
        Assertions.assertThat(userDetailsList.get(0).getFullName()).isEqualTo("Ashish");
        Assertions.assertThat(userDetailsList.get(0).getEmail()).isEqualTo("ashish@levelops.io");
        Assertions.assertThat(userDetailsList.get(0).getIntegrationUserDetailsList().size()).isEqualTo(2);
    }
}
