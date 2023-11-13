package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.DashboardReport;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DashboardWidgetServiceTest {
    private final static String company = "test";
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DashboardWidgetService dws;
    private static UserService userService;
    private static OUDashboardService ouDashboardService;
    private static DataSource dataSource;
    private static String userId1;
    private static String userId2;
    private static OrgUnitsDatabaseService orgUnitsDatabaseService;
    private static TagItemDBService tagItemDbService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static OrgUsersDatabaseService usersService;
    private static IntegrationService integrationService;
    private static UserIdentityService userIdentityService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static ProductService productService;


    @BeforeClass
    public static void beforeClass() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        dws = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        usersService = new OrgUsersDatabaseService(dataSource, DefaultObjectMapper.get(), orgVersionsService, userIdentityService);
        orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, DefaultObjectMapper.get(), tagItemDbService, usersService, orgVersionsService, dws);
        ouDashboardService=new OUDashboardService(dataSource,DefaultObjectMapper.get(),orgUnitsDatabaseService);
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        dws.ensureTableExistence(company);
        userId1 = userService.insert(company, User.builder()
                .userType(RoleType.ADMIN)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        userId2 = userService.insert(company, User.builder()
                .userType(RoleType.ADMIN)
                .bcryptPassword("fasdffad")
                .email("john@doe.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("john")
                .lastName("doe")
                .build());
        dws.ensureTableExistence(company);
        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        userIdentityService = new UserIdentityService(dataSource);
        userIdentityService.ensureTableExistence(company);
        orgVersionsService.ensureTableExistence(company);
        usersService.ensureTableExistence(company);
        productService = new ProductService(dataSource);
        productService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, DefaultObjectMapper.get(), null, null, orgVersionsService, dws), integrationService), DefaultObjectMapper.get());
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService.ensureTableExistence(company);
        ouDashboardService.ensureTableExistence(company);
    }

    @Test
    public void testInsert() throws SQLException {
        // -- dashboard
        Dashboard dashboard1 = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId1)
                .metadata(Map.of())
                .query(Map.of())
                .widgets(List.of(Widget.builder()
                        .name("widget")
                        .displayInfo(Map.of("description", "test"))
                        .build()))
                .build();
        String id1 = dws.insert(company, dashboard1);
        assertThat(id1).isNotNull();

        Dashboard output = dws.get(company, id1).orElse(null);
        DefaultObjectMapper.prettyPrint(output);

        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo("name");
        assertThat(output.getOwnerId()).isEqualTo(userId1);
        assertThat(output.getWidgets().get(0).getDisplayInfo()).isNotNull();

        // -- dashboard with missing owner
        Dashboard dashboard2 = Dashboard.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .widgets(List.of(Widget.builder()
                        .name("widget")
                        .displayInfo(Map.of("description", "test1"))
                        .build()))
                .build();
        String id2 = dws.insert(company, dashboard2);
        assertThat(id2).isNotNull();

        output = dws.get(company, id2).orElse(null);
        DefaultObjectMapper.prettyPrint(output);

        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo("name");
        assertThat(output.getOwnerId()).isNull();
        assertThat(output.getWidgets().get(0).getDisplayInfo()).isNotNull();
        Integer projExeDashboardId=dws.createDemoDashboards(company,"all_projects_dora_metrics");
        Optional<Dashboard> projExeDashboard=dws.get(company,projExeDashboardId.toString());
        assertThat(projExeDashboardId).isGreaterThan(0);
        assertThat(projExeDashboard.isPresent()).isEqualTo(true);
        assertThat(projExeDashboard.get().getName()).isEqualTo("DORA Metrics_"+projExeDashboardId);
        Integer projPlanningDashboardId=dws.createDemoDashboards(company,"all_projects_planning");
        Optional<Dashboard> projPlanDashboard=dws.get(company,projPlanningDashboardId.toString());
        assertThat(projPlanningDashboardId).isGreaterThan(0);
        assertThat(projPlanDashboard.isPresent()).isEqualTo(true);
        assertThat(projPlanDashboard.get().getName()).isEqualTo("Planning_"+projPlanningDashboardId);
        Integer projAlignDashboardId=dws.createDemoDashboards(company,"all_projects_alignment");
        Optional<Dashboard> projAlignDashboard=dws.get(company,projAlignDashboardId.toString());
        assertThat(projAlignDashboardId).isGreaterThan(0);
        assertThat(projAlignDashboard.isPresent()).isEqualTo(true);
        assertThat(projAlignDashboard.get().getName()).isEqualTo("Alignment_"+projAlignDashboardId);
        Integer sprintPlanningDashboardId=dws.createDemoDashboards(company,"all_sprints_planning");
        Optional<Dashboard> sprintPlanningdashboard=dws.get(company,sprintPlanningDashboardId.toString());
        assertThat(sprintPlanningDashboardId).isGreaterThan(0);
        assertThat(sprintPlanningdashboard.isPresent()).isEqualTo(true);
        assertThat(sprintPlanningdashboard.get().getName()).isEqualTo("Planning_"+sprintPlanningDashboardId);
        Integer teamExecDashboardId=dws.createDemoDashboards(company,"all_teams_dora_metrics");
        Optional<Dashboard> teamExeDashboard=dws.get(company,teamExecDashboardId.toString());
        assertThat(teamExecDashboardId).isGreaterThan(0);
        assertThat(teamExeDashboard.isPresent()).isEqualTo(true);
        assertThat(teamExeDashboard.get().getName()).isEqualTo("DORA Metrics_"+teamExecDashboardId);
        Integer teamPlanningDashboardId=dws.createDemoDashboards(company,"all_teams_planning");
        Optional<Dashboard> teamPlanningDashboard=dws.get(company,teamPlanningDashboardId.toString());
        assertThat(teamPlanningDashboardId).isGreaterThan(0);
        assertThat(teamPlanningDashboard.isPresent()).isEqualTo(true);
        assertThat(teamPlanningDashboard.get().getName()).isEqualTo("Planning_"+teamPlanningDashboardId);
        Integer devInsightDashboardId=dws.createDemoDashboards(company,"all_teams_dev_insights");
        Optional<Dashboard> devInsightDashboard=dws.get(company,devInsightDashboardId.toString());
        assertThat(devInsightDashboardId).isGreaterThan(0);
        assertThat(devInsightDashboard.isPresent()).isEqualTo(true);
        assertThat(devInsightDashboard.get().getName()).isEqualTo("Dev Insights_"+devInsightDashboardId);
        Integer teamTrellisDashboardId=dws.createDemoDashboards(company,"all_teams_trellis_score");
        Optional<Dashboard> teamTrellisDashboard=dws.get(company,teamTrellisDashboardId.toString());
        assertThat(teamTrellisDashboardId).isGreaterThan(0);
        assertThat(teamTrellisDashboard.isPresent()).isEqualTo(true);
        assertThat(teamTrellisDashboard.get().getName()).isEqualTo("Trellis Score_"+teamTrellisDashboardId);

        // -- report
        DashboardReport report = DashboardReport.builder()
                .dashboardId(id1)
                .createdBy(userId1)
                .fileId("asdasdasdasd")
                .name("asdasdasdasdasd")
                .build();
        String reportId1 = dws.insertReport(company, report);
        String reportId2 = dws.insertReport(company, report);
        assertThat(reportId1).isNotNull();
        assertThat(reportId2).isNotNull();

        DbListResponse<DashboardReport> dwsReps = dws.listReports(company,
                null, null, null, 0, 100);
        assertThat(dwsReps.getTotalCount()).isEqualTo(2);
        dwsReps = dws.listReports(company,
                "asd", id1, userId1, 0, 100);
        assertThat(dwsReps.getTotalCount()).isEqualTo(2);
        System.out.println(DefaultObjectMapper.writeAsPrettyJson(dwsReps));

        dwsReps = dws.listReports(company,
                null, id2, "2", 0, 100);
        assertThat(dwsReps.getTotalCount()).isEqualTo(0);

        assertThat(dws.deleteReport(company, reportId1)).isTrue();
        assertThat(dws.delete(company, id1)).isTrue();
        assertThat(dws.deleteReport(company, reportId1)).isFalse();
    }

    @Test
    public void testListDashboard() throws SQLException {
        Dashboard dashboard1 = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId1)
                .metadata(Map.of())
                .query(Map.of())
                .widgets(List.of(Widget.builder()
                        .name("widget")
                                .type("type1")
                        .displayInfo(Map.of("description", "test"))
                        .build()))
                .isPublic(true)
                .build();
        String id1 = dws.insert(company, dashboard1);
        Dashboard dashboard2 = Dashboard.builder()
                .name("db-name")
                .type("db-type")
                .ownerId(userId2)
                .metadata(Map.of())
                .query(Map.of())
                .widgets(List.of(Widget.builder()
                        .name("widget")
                        .type("type2")
                        .displayInfo(Map.of("description", "test"))
                        .build(),
                        Widget.builder()
                                .name("widget3")
                                .type("type3")
                                .displayInfo(Map.of("description", "test"))
                                .build()))
                .isPublic(false)
                .build();
        String id2 = dws.insert(company, dashboard2);
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();

        DbListResponse<Dashboard> response = dws.listByFilters(company, "db-type", userId2, "db-name", false, null, 0, 1, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(1);
        assertThat(response.getRecords().get(0).getName()).isEqualTo("db-name");
        assertThat(response.getRecords().get(0).getFirstName()).isEqualTo("john");
        assertThat(response.getRecords().get(0).getLastName()).isEqualTo("doe");
        assertThat(response.getRecords().get(0).isPublic()).isEqualTo(false);
        assertThat(response.getRecords().get(0).getName()).isEqualTo("db-name");
        assertThat(response.getRecords().get(0).getOwnerId()).isEqualTo(userId2);

        response = dws.listByFilters(company, "type", userId1, "name", true, null, 0, 1, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(1);
        assertThat(response.getRecords().get(0).getName()).isEqualTo("name");
        assertThat(response.getRecords().get(0).getFirstName()).isEqualTo("asd");
        assertThat(response.getRecords().get(0).getLastName()).isEqualTo("asd");
        assertThat(response.getRecords().get(0).isPublic()).isEqualTo(true);
        assertThat(response.getRecords().get(0).getName()).isEqualTo("name");
        assertThat(response.getRecords().get(0).getOwnerId()).isEqualTo(userId1);

        response = dws.listByFilters(company, DashboardWidgetService.DashboardFilter.builder().ids(List.of("8")).build(), 0, 10, List.of(Map.of()));
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(1);
        assertThat(response.getRecords().get(0).getName()).isEqualTo("name");
        assertThat(response.getRecords().get(0).getFirstName()).isEqualTo("asd");
        assertThat(response.getRecords().get(0).getLastName()).isEqualTo("asd");
        assertThat(response.getRecords().get(0).isPublic()).isEqualTo(true);
        assertThat(response.getRecords().get(0).getName()).isEqualTo("name");
        assertThat(response.getRecords().get(0).getOwnerId()).isEqualTo(userId1);

        response = dws.listByFilters(company, null,  null, null, null, null, 0, 3, null);
        DefaultObjectMapper.prettyPrint(response);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(3);
        assertThat(response.getRecords().get(2).getName()).isEqualTo("name");
        assertThat(response.getRecords().get(2).getFirstName()).isEqualTo("asd");
        assertThat(response.getRecords().get(2).getLastName()).isEqualTo("asd");
//        assertThat(response.getRecords().get(2).isPublic()).isEqualTo(true); // FIXME
        assertThat(response.getRecords().get(2).getOwnerId()).isEqualTo(userId1);

        assertThat(response.getRecords().get(1).getName()).isEqualTo("db-name");
        assertThat(response.getRecords().get(1).getFirstName()).isEqualTo("john");
        assertThat(response.getRecords().get(1).getLastName()).isEqualTo("doe");
        assertThat(response.getRecords().get(1).isPublic()).isEqualTo(false);
        assertThat(response.getRecords().get(1).getOwnerId()).isEqualTo(userId2);

        assertThat(response.getRecords().get(0).getName()).isEqualTo("Demo Dashboard");

        userService.delete(company, userId2);

        response = dws.listByFilters(company, null,  null, null, null, null, 0, 3, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords().size()).isEqualTo(3);

        assertThat(response.getRecords().get(2).getName()).isEqualTo("name");
        assertThat(response.getRecords().get(2).getFirstName()).isEqualTo("asd");
        assertThat(response.getRecords().get(2).getLastName()).isEqualTo("asd");
//        assertThat(response.getRecords().get(2).isPublic()).isEqualTo(true);  // FIXME
        assertThat(response.getRecords().get(2).getOwnerId()).isEqualTo(userId1);

        assertThat(response.getRecords().get(1).getName()).isEqualTo("db-name");
        assertThat(response.getRecords().get(1).getFirstName()).isNull();
        assertThat(response.getRecords().get(1).getLastName()).isNull();
        assertThat(response.getRecords().get(1).isPublic()).isEqualTo(false);
        assertThat(response.getRecords().get(1).getOwnerId()).isNull();

        assertThat(response.getRecords().get(0).getName()).isEqualTo("Demo Dashboard");

        response = dws.listByFilters(company, DashboardWidgetService.DashboardFilter.builder()
                        .isDemo(true)
                .build(), 0, 3, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords()).hasSize(1);

        response = dws.listByFilters(company, DashboardWidgetService.DashboardFilter.builder()
                .exactName("demo dashboard")
                .build(), 0, 3, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords()).isEmpty();

        response = dws.listByFilters(company, DashboardWidgetService.DashboardFilter.builder()
                .exactName("Demo Dashboard")
                .build(), 0, 3, null);
        assertThat(response).isNotNull();
        assertThat(response.getRecords()).hasSize(1);

        List<Widget> widgets = dws.listWidgetsByFilters(company, null, null, null, 0, 100000).getRecords();
        assertThat(widgets.size()).isEqualTo(3);
        assertThat(dws.listWidgetsByFilters(company, null, List.of("type1", "type2"), null, 0, 100000).getRecords().size()).isEqualTo(2);
        assertThat(dws.listWidgetsByFilters(company, null, List.of("bad-request"), null, 0, 100000).getRecords().size()).isEqualTo(0);
        assertThat(dws.listWidgetsByFilters(company, null, null, true, 0, 100000).getRecords().size()).isEqualTo(0);
        assertThat(dws.listWidgetsByFilters(company, null, null, false, 0, 100000).getRecords().size()).isEqualTo(3);

        Widget w1 = dws.listWidgetsByFilters(company, null, List.of("type1"), null, 0, 100000).getRecords().get(0);
        assertThat(w1.getPrecalculateFrequencyInMins()).isEqualTo(0);
        Widget w2 = dws.listWidgetsByFilters(company, null, List.of("type2"), null, 0, 100000).getRecords().get(0);
        assertThat(w2.getPrecalculateFrequencyInMins()).isEqualTo(0);

        assertThat(dws.listWidgetsByFilters(company, List.of(UUID.fromString(w1.getId()), UUID.fromString(w2.getId())), null, null, 0, 100000).getRecords().size()).isEqualTo(2);
        assertThat(dws.listWidgetsByFilters(company, List.of(UUID.fromString(w1.getId())), null, null, 0, 100000).getRecords().size()).isEqualTo(1);
        assertThat(dws.listWidgetsByFilters(company, List.of(UUID.randomUUID()), null, null, 0, 100000).getRecords().size()).isEqualTo(0);

        dws.enableWidgetPrecalculation(company, List.of(UUID.fromString(w1.getId()), UUID.fromString(w2.getId())), 100);
        w1 = dws.listWidgetsByFilters(company, null, List.of("type1"), null, 0, 100000).getRecords().get(0);
        assertThat(w1.getPrecalculateFrequencyInMins()).isEqualTo(100);
        w2 = dws.listWidgetsByFilters(company, null, List.of("type2"), null, 0, 100000).getRecords().get(0);
        assertThat(w2.getPrecalculateFrequencyInMins()).isEqualTo(100);

        assertThat(dws.listWidgetsByFilters(company, null, List.of("type1", "type2"), null, 0, 100000).getRecords().size()).isEqualTo(2);
        assertThat(dws.listWidgetsByFilters(company, null, List.of("bad-request"), null, 0, 100000).getRecords().size()).isEqualTo(0);
        assertThat(dws.listWidgetsByFilters(company, null, null, true, 0, 100000).getRecords().size()).isEqualTo(2);
        assertThat(dws.listWidgetsByFilters(company, null, null, false, 0, 100000).getRecords().size()).isEqualTo(1);

        dws.bulkDelete(company, List.of(id1, id2));
    }

    private Dashboard createDashboard(String name, boolean isPublic, String dashboardPermissions, Boolean allUsers, List<String> users) {
        Map<String, Object> metadata = new HashMap<>();
        Map<String, Object> rbac = new HashMap<>();
        if (StringUtils.isNotEmpty(dashboardPermissions)){
            rbac.put("dashboardPermission", dashboardPermissions);
        }
        if (Objects.nonNull(allUsers)) {
            rbac.put("allUsers", allUsers);
        }
        if (Objects.nonNull(users)) {
            Map<String, Object> userMap = new HashMap<>();
            for(String user : users) {
                userMap.put(user, Map.of("permissions", "owner"));
            }
            rbac.put("users", userMap);
        }
        metadata.put("rbac", rbac);
        return Dashboard.builder()
                .name(name)
                .type("type")
                .ownerId(userId1)
                .metadata(metadata)
                .query(Map.of())
                .isPublic(isPublic)
                .build();
    }

    @Test
    public void testListDashboardRbacFilter() throws SQLException {
        Dashboard dashboard1 = createDashboard("1", true, "limited", true, null);
        Dashboard dashboard2 = createDashboard("2", true, "limited", null, List.of("sid@propelo.ai"));
        Dashboard dashboard3 = createDashboard("3", true, "limited", false, null);
        Dashboard dashboard4 = createDashboard("4", true, "public", null, null);
        Dashboard dashboard5 = createDashboard("5", false, null, null, null);
        String id1 = dws.insert(company, dashboard1);
        String id2 = dws.insert(company, dashboard2);
        String id3 = dws.insert(company, dashboard3);
        String id4 = dws.insert(company, dashboard4);
        String id5 = dws.insert(company, dashboard5);
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();
        assertThat(id3).isNotNull();
        assertThat(id4).isNotNull();
        assertThat(id5).isNotNull();

        var results = dws.list(company, 0, 1000);
        assertThat(results.getCount()).isEqualTo(6); // 1 demo dashboard created automatically

        results = dws.listByFilters(
                company,
                DashboardWidgetService.DashboardFilter.builder().rbacUserEmail("sid@propelo.ai").build(),
                0, 100, List.of());
        assertThat(results.getCount()).isEqualTo(3);
        assertThat(results.getRecords().stream().map(Dashboard::getId).collect(Collectors.toSet())).isEqualTo(
                Set.of(id1, id2, id4)
        );

        results = dws.listByFilters(
                company,
                DashboardWidgetService.DashboardFilter.builder().rbacUserEmail("junk@propelo.ai").build(),
                0, 100, List.of());
        assertThat(results.getCount()).isEqualTo(2);
        assertThat(results.getRecords().stream().map(Dashboard::getId).collect(Collectors.toSet())).isEqualTo(
                Set.of(id1, id4)
        );
        dws.bulkDelete(company, List.of(id1, id2, id3, id4, id5));
    }

    @Test
    public void testUpdate() throws SQLException {
        Dashboard.DashboardBuilder bldr = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId1)
                .metadata(Map.of())
                .query(Map.of());
        String val = dws.insert(company, bldr.build());
        assertThat(dws.update(company, bldr.id(val).name("name1").build())).isTrue();
        assertThat(dws.update(company, bldr
                .id(val)
                .widgets(List.of(Widget.builder().name("asd").type("bsd").displayInfo(Map.of("description","test1"))
                        .build()))
                .build()))
                .isTrue();

        var dbDashboard = dws.get(company, val);
        Assertions.assertThat(dbDashboard.get().isPublic()).isFalse();
        Assertions.assertThat(dbDashboard.get().getWidgets().get(0).getDisplayInfo()).isNotNull();
        
        assertThat(dws.update(company, bldr
                .id(val)
                .isPublic(true)
                .widgets(List.of(Widget.builder().name("asd").type("bsd").displayInfo(Map.of("description","test"))
                        .build()))
                .build()))
                .isTrue();
        
        dbDashboard = dws.get(company, val);
        Assertions.assertThat(dbDashboard.get().isPublic()).isTrue();
        Assertions.assertThat(dbDashboard.get().getWidgets().get(0).getDisplayInfo()).isNotNull();
    }

    @Test
    public void testDeleteBulk() throws SQLException {
        Dashboard dashboard1 = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId1)
                .metadata(Map.of())
                .query(Map.of())
                .build();
        String id1 = dws.insert(company, dashboard1);
        assertThat(id1).isNotNull();

        // -- dashboard with missing owner
        Dashboard dashboard2 = Dashboard.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .build();
        String id2 = dws.insert(company, dashboard2);
        assertThat(id2).isNotNull();

        List<String> dashboardIds = List.of(id1,id2);
        dws.bulkDelete(company,dashboardIds);

        Dashboard output = dws.get(company, id1).orElse(null);
        DefaultObjectMapper.prettyPrint(output+" delete");
        assertThat(output).isNull();
        Dashboard output1 = dws.get(company, id2).orElse(null);
        DefaultObjectMapper.prettyPrint(output1);
        assertThat(output).isNull();
    }

    @Test
    public void testBulkDeleteDashboardReport() throws SQLException {
        String name1 = "dash1";
        String name2 = "dash2";
        String name3 = "dash3";
        Dashboard dashboard1 = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId1)
                .metadata(Map.of())
                .query(Map.of())
                .build();
        String dashboardId = dws.insert(company, dashboard1);
        String createdBy = userId1;
        DashboardReport dashboardReport1 = DashboardReport.builder()
                .name(name1)
                .fileId("1")
                .createdBy(createdBy)
                .dashboardId(dashboardId)
                .build();
        DashboardReport dashboardReport2 = DashboardReport.builder()
                .name(name2)
                .fileId("1")
                .createdBy(createdBy)
                .dashboardId(dashboardId)
                .build();
        DashboardReport dashboardReport3 = DashboardReport.builder()
                .name(name3)
                .fileId("1")
                .createdBy(createdBy)
                .dashboardId(dashboardId)
                .build();
        String dashboardId1 = dws.insertReport(company, dashboardReport1);
        String dashboardId2 = dws.insertReport(company, dashboardReport2);
        String dashboardId3 = dws.insertReport(company, dashboardReport3);
        dws.deleteBulkReport(company, List.of(dashboardId1, dashboardId2, dashboardId3));
        DbListResponse<DashboardReport> dashboardReportDbListResponse1 = dws.listReports(company, name1, dashboardId, createdBy, 1, 10);
        DbListResponse<DashboardReport> dashboardReportDbListResponse2 = dws.listReports(company, name2, dashboardId, createdBy, 1, 10);
        DbListResponse<DashboardReport> dashboardReportDbListResponse3 = dws.listReports(company, name3, dashboardId, createdBy, 1, 10);
        Assertions.assertThat(dashboardReportDbListResponse1.getCount()).isEqualTo(0);
        Assertions.assertThat(dashboardReportDbListResponse2.getCount()).isEqualTo(0);
        Assertions.assertThat(dashboardReportDbListResponse3.getCount()).isEqualTo(0);
    }
}