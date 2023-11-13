package io.levelops.api.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.api.controllers.ProductsController;
import io.levelops.api.utils.DatabaseTestUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.OUDashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.organization.DBOrgContentSection;
import io.levelops.commons.databases.models.database.organization.DBOrgUnit;
import io.levelops.commons.databases.models.database.organization.DBOrgUser;
import io.levelops.commons.databases.models.database.organization.OrgUnitCategory;
import io.levelops.commons.databases.models.database.organization.OrgUserId;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.DashboardWidgetService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.OUDashboardService;
import io.levelops.commons.databases.services.ProductService;
import io.levelops.commons.databases.services.TagItemDBService;
import io.levelops.commons.databases.services.TagsService;
import io.levelops.commons.databases.services.TicketCategorizationSchemeDatabaseService;
import io.levelops.commons.databases.services.UserIdentityService;
import io.levelops.commons.databases.services.UserService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.databases.services.dev_productivity.DevProductivityProfileDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.databases.services.velocity.OrgProfileDatabaseService;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.workitems.clients.WorkItemsClient;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Log4j2
public class WorkspaceBulkOperationServiceTest {
    private static final String company = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;
    private static ProductsController productController;
    private static ObjectMapper objectMapper;
    private static ProductService productService;
    private static ActivityLogService activityLogService;
    private static TagItemDBService tagItemService;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;
    private static OrgUsersDatabaseService orgUsersDatabaseService;
    private static OrgVersionsDatabaseService orgVersionsDatabaseService;
    private static UserIdentityService userIdentityService;
    private static DashboardWidgetService dashboardWidgetService;
    private static OUDashboardService ouDashboardService;
    private static OrgUnitHelper orgUnitHelper;
    private static OrgUnitsDatabaseService orgUnitsDatabaseService;
    private static IntegrationService integrationService;
    private static UserService userService;
    private static WorkspaceBulkOperationService workspaceBulkOperationService;
    private static TenantConfigService configService;


    private static OrgProfileDatabaseService ouProfileDbService;
    private static VelocityConfigsDatabaseService velocityConfigDbService;
    private static DevProductivityProfileDatabaseService devProductivityProfileDbService;
    private static CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private static TicketCategorizationSchemeDatabaseService ticketCategorizationSchemeDatabaseService;

    private String userId;
    private String integrationId;
    private String dashBoardId1;
    private String dashBoardId2;
    private OrgUserId orgUserId;
    private OrgUserId orgUserId2;
    private Set<OrgUserId> managers;

    @Mock
    private static WorkItemsClient workItemsClient;

    @Mock
    private static CiCdJobsDatabaseService ciCdJobsDatabaseService;

    @Before
    public void setup() throws Exception {
        MockitoAnnotations.initMocks(this);
        if (dataSource != null) {
            return;
        }
        dataSource = DatabaseTestUtils.setUpDataSource(pg, company);
        objectMapper = DefaultObjectMapper.get();
        productService = new ProductService(dataSource);
        activityLogService = new ActivityLogService(dataSource, objectMapper);
        tagItemService = new TagItemDBService(dataSource);
        orgVersionsDatabaseService = new OrgVersionsDatabaseService(dataSource);
        userIdentityService = new UserIdentityService(dataSource);
        orgUsersDatabaseService = new OrgUsersDatabaseService(dataSource, objectMapper, orgVersionsDatabaseService, userIdentityService);
        dashboardWidgetService = new DashboardWidgetService(dataSource, objectMapper);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, objectMapper, tagItemService, orgUsersDatabaseService, orgVersionsDatabaseService, dashboardWidgetService);
        integrationService = new IntegrationService(dataSource);
        orgUnitHelper = new OrgUnitHelper(orgUnitsDatabaseService, integrationService);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, orgUnitHelper, objectMapper);
        ouDashboardService = new OUDashboardService(dataSource, objectMapper, orgUnitsDatabaseService);
         configService =new TenantConfigService(dataSource);
        productController = new ProductsController(
                productService,
                activityLogService,
                workItemsClient,
                ciCdJobsDatabaseService,
                orgUnitCategoryDatabaseService,
                orgUnitHelper,
                orgUnitsDatabaseService,dashboardWidgetService,ouDashboardService,configService,
                null, null, null, true);
        userService = new UserService(dataSource, objectMapper);
        var tagService = new TagsService(dataSource);
        var userIdentityService = new UserIdentityService(dataSource);
        workspaceBulkOperationService = new WorkspaceBulkOperationService(
                productService,
                orgUnitCategoryDatabaseService,
                activityLogService,
                orgUnitsDatabaseService,
                ouDashboardService,
                dashboardWidgetService,
                orgUnitHelper);

        integrationService.ensureTableExistence(company);
        userIdentityService.ensureTableExistence(company);
        userService.ensureTableExistence(company);
        activityLogService.ensureTableExistence(company);
        tagService.ensureTableExistence(company);
        tagItemService.ensureTableExistence(company);
        orgVersionsDatabaseService.ensureTableExistence(company);
        orgUsersDatabaseService.ensureTableExistence(company);
        dashboardWidgetService.ensureTableExistence(company);
        productService.ensureTableExistence(company);
        orgUnitCategoryDatabaseService.ensureTableExistence(company);
        orgUnitsDatabaseService.ensureTableExistence(company);
        ouDashboardService.ensureTableExistence(company);

        ciCdInstancesDatabaseService = new CiCdInstancesDatabaseService(dataSource);
        ciCdInstancesDatabaseService.ensureTableExistence(company);
        ciCdJobsDatabaseService = new CiCdJobsDatabaseService(dataSource);
        ciCdJobsDatabaseService.ensureTableExistence(company);
        ouProfileDbService = new OrgProfileDatabaseService(dataSource,objectMapper);
        ouProfileDbService.ensureTableExistence(company);
        velocityConfigDbService = new VelocityConfigsDatabaseService(dataSource,objectMapper,ouProfileDbService);
        velocityConfigDbService.ensureTableExistence(company);
        ticketCategorizationSchemeDatabaseService = new TicketCategorizationSchemeDatabaseService(dataSource, objectMapper);
        ticketCategorizationSchemeDatabaseService.ensureTableExistence(company);
        devProductivityProfileDbService = new DevProductivityProfileDatabaseService(dataSource,objectMapper);
        devProductivityProfileDbService.ensureTableExistence(company);

        userId = userService.insert(company, User.builder()
                .userType(RoleType.LIMITED_USER)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        integrationId = integrationService.insert(company, Integration.builder()
                .id("1")
                .application("jira")
                .name("jira test")
                .status("enabled")
                .build());
        var orgUser1 = DBOrgUser.builder()
                .email("email1")
                .fullName("fullName1")
                .active(true)
                .customFields(Map.of("test_name", "test1"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("sid").username("sid").integrationType("jira")
                        .integrationId(Integer.parseInt(integrationId)).build()))
                .versions(Set.of(1))
                .build();
        var orgUser2 = DBOrgUser.builder()
                .email("email2")
                .fullName("fullName2")
                .active(true)
                .customFields(Map.of("test_name", "test2"))
                .ids(Set.of(DBOrgUser.LoginId.builder().cloudId("sid").username("sid").integrationType("jira")
                        .integrationId(Integer.parseInt(integrationId)).build()))
                .versions(Set.of(1))
                .build();
        orgUserId = orgUsersDatabaseService.upsert(company, orgUser1);
        orgUserId2 = orgUsersDatabaseService.upsert(company, orgUser2);
        var manager1 = OrgUserId.builder().id(orgUserId.getId()).refId(orgUserId.getRefId()).fullName(orgUser1.getFullName()).email(orgUser1.getEmail()).build();
        var manager2 = OrgUserId.builder().id(orgUserId2.getId()).refId(orgUserId2.getRefId()).fullName(orgUser2.getFullName()).email(orgUser2.getEmail()).build();
        managers = Set.of(
                manager1,
                manager2
        );

        dashBoardId1 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 1")
                .firstName("Sid")
                .lastName("Bidasaria")
                .isDefault(true)
                .type("TYPE 1")
                .email("sid@propelo.ai")
                .build());
        dashBoardId2 = dashboardWidgetService.insert(company, Dashboard.builder()
                .name("Dashboard 2")
                .firstName("Sid")
                .lastName("Bidasaria")
                .isDefault(false)
                .type("TYPE 1")
                .email("sid@propelo.ai")
                .build());

        // Ensure all widgets have different names, the test depends on it.
        var widgetId1 = dashboardWidgetService.insertWidget(company, Widget.builder()
                .name("widget1")
                .dashboardId(dashBoardId1)
                .precalculateFrequencyInMins(1)
                .type("Sid's widget - 1")
                .query(Map.of("key", "value"))
                .metadata(Map.of("some", "value1"))
                .build(), dashBoardId1);

        dashboardWidgetService.insertWidget(company, Widget.builder()
                .name("widget2")
                .dashboardId(dashBoardId1)
                .precalculateFrequencyInMins(1)
                .type("Sid's widget")
                .query(Map.of("key", "value"))
                .metadata(Map.of("children", List.of(widgetId1)))
                .build(), dashBoardId1);
    }

    @Test
    public void simpleCloneTest() throws Exception {
        var oldWorkspace = Product.builder()
                .name("Sid workspace")
                .description("Test workspace original")
                .bootstrapped(false)
                .key("sid")
                .integrationIds(Set.of(Integer.parseInt(integrationId)))
                .immutable(false)
                .ownerId(userId)
                .build();
        var sessionUser = "sid@propelo.ai";
        var oldWorkspaceRefId = productController.createWorkspace(oldWorkspace, sessionUser, company);

        var oldCategories = getCategories(oldWorkspaceRefId);
        var customOuCategory = oldCategories.getRecords().get(0);
        var customOuOld = DBOrgUnit.builder()
                .ouCategoryId(customOuCategory.getId())
                .name("Custom OU")
                .description("This is a custom ou")
                .managers(managers)
                .sections(Set.of(DBOrgContentSection.builder()
                        .integrationId(Integer.parseInt(integrationId))
                        .integrationFilters(Map.of(
                                "assignees", List.of("sample")
                        ))
                        .defaultSection(false)
                        .users(Set.of(1, 2, 3))
                        .build()))
                .active(true)
                .defaultDashboardId(Integer.parseInt(dashBoardId1))
                .parentRefId(customOuCategory.getRootOuRefId())
                .workspaceId(Integer.parseInt(oldWorkspaceRefId))
                .build();
        var customOldOuId = orgUnitHelper.insertNewOrgUnits(company, Stream.of(customOuOld)).stream().findFirst().get();
        customOuOld = orgUnitsDatabaseService.get(company, customOldOuId).get();
        ouDashboardService.insert(company, OUDashboard.builder()
                .dashboardId(Integer.parseInt(dashBoardId1))
                .dashboardOrder(0)
                .ouId(customOuOld.getId())
                .build());
        ouDashboardService.insert(company, OUDashboard.builder()
                .dashboardId(Integer.parseInt(dashBoardId2))
                .dashboardOrder(1)
                .ouId(customOuOld.getId())
                .build());

        String newWorkspaceId = workspaceBulkOperationService.cloneWorkspace(oldWorkspaceRefId, "newWorkspace", "newKey", company, sessionUser);
        compareWorkspaces(oldWorkspaceRefId, newWorkspaceId);
        compareDashboards(dashboardWidgetService.get(company, dashBoardId1).get());
        compareDashboards(dashboardWidgetService.get(company, dashBoardId2).get());
    }

    private void compareWorkspaces(String oldWorkspaceId, String newWorkspaceId) throws SQLException {
        var oldWorkspace = productService.get(company, oldWorkspaceId).get();
        var newWorkspace = productService.get(company, newWorkspaceId).get();
        assertThat(newWorkspace.getDescription()).isEqualTo(oldWorkspace.getDescription());
        assertThat(newWorkspace.getBootstrapped()).isEqualTo(oldWorkspace.getBootstrapped());
        assertThat(newWorkspace.getImmutable()).isEqualTo(oldWorkspace.getImmutable());
        assertThat(newWorkspace.getOwnerId()).isEqualTo(oldWorkspace.getOwnerId());
        assertThat(newWorkspace.getIntegrationIds()).isEqualTo(oldWorkspace.getIntegrationIds());
        assertThat(newWorkspace.getIntegrations()).isEqualTo(oldWorkspace.getIntegrations());
        assertThat(newWorkspace.getId()).isNotEqualTo(oldWorkspace.getId());
        assertThat(newWorkspace.getKey()).isNotEqualTo(oldWorkspace.getKey());

        var oldCategories = getCategories(oldWorkspaceId);
        var newCategories = getCategories(newWorkspaceId);

        assertThat(oldCategories.getTotalCount()).isEqualTo(3);
        assertThat(oldCategories.getTotalCount()).isEqualTo(newCategories.getTotalCount());
        newCategories.getRecords().forEach(newCategory -> {
            assertThat(newCategory.getWorkspaceId()).isEqualTo(Integer.parseInt(newWorkspaceId));
            var oldCategory = oldCategories.getRecords().stream()
                    .filter(category -> category.getName().equals(newCategory.getName()))
                    .findFirst()
                    .get();
            compareCategories(oldCategory, newCategory);
        });

        var oldOus = getAllOus(oldWorkspaceId);
        var newOus = getAllOus(newWorkspaceId);
        assertThat(oldOus.getTotalCount()).isEqualTo(4);
        assertThat(oldOus.getTotalCount()).isEqualTo(newOus.getTotalCount());
        newOus.getRecords().forEach(newOu -> {
            assertThat(newOu.getWorkspaceId()).isEqualTo(Integer.parseInt(newWorkspaceId));
            var oldOu = oldOus.getRecords().stream()
                    .filter(ou -> ou.getName().equals(newOu.getName()))
                    .findFirst()
                    .get();
            try {
                compareOus(oldOu, newOu);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void compareDashboardMappings(DBOrgUnit oldOu, DBOrgUnit newOu) throws SQLException {
        var oldMappings = ouDashboardService.listByOuFilters(company, OUDashboardService.DashboardFilter.builder().ouId(oldOu.getId()).build(),
                oldOu, 0, 100, null).getRecords();
        var newMappings = ouDashboardService.listByOuFilters(company, OUDashboardService.DashboardFilter.builder().ouId(newOu.getId()).build(),
                newOu, 0, 100, null).getRecords();
        assertThat(newMappings.size()).isEqualTo(oldMappings.size());
        oldMappings.forEach(oldMapping -> {
            var match = newMappings.stream().anyMatch(newMapping ->
                    Objects.equals(newMapping.getDashboardOrder(), oldMapping.getDashboardOrder()) &&
                            Objects.equals(newMapping.getName(), oldMapping.getName()) &&
                            !Objects.equals(newMapping.getDashboardId(), oldMapping.getDashboardId()) &&
                            !Objects.equals(newMapping.getOuId(), oldMapping.getOuId()));
            assertThat(match).isTrue();
        });
    }

    private void compareDashboards(Dashboard oldDashboard) throws SQLException {
        var dashboards = dashboardWidgetService.listByFilters(company, DashboardWidgetService.DashboardFilter.builder()
                .exactName(oldDashboard.getName()).build(), 0, 100, null).getRecords();
        assertThat(dashboards.size()).isEqualTo(2);
        assertThat(dashboards.get(0).getName()).isEqualTo(dashboards.get(1).getName());
        assertThat(dashboards.get(0).getDashboardOrder()).isEqualTo(dashboards.get(1).getDashboardOrder());
        assertThat(dashboards.get(0).getEmail()).isEqualTo(dashboards.get(1).getEmail());
        assertThat(dashboards.get(0).getDemo()).isEqualTo(dashboards.get(1).getDemo());
        assertThat(dashboards.get(0).getIsDefault()).isEqualTo(dashboards.get(1).getIsDefault());
        assertThat(dashboards.get(0).getFirstName()).isEqualTo(dashboards.get(1).getFirstName());
        assertThat(dashboards.get(0).getMetadata()).isEqualTo(dashboards.get(1).getMetadata());
        assertThat(dashboards.get(0).getQuery()).isEqualTo(dashboards.get(1).getQuery());

        // Query again because listByFilters does not fetch the widgets
        var oldDash = dashboardWidgetService.get(company, dashboards.get(0).getId()).get();
        var newDash = dashboardWidgetService.get(company, dashboards.get(1).getId()).get();

        if(Objects.isNull(oldDash.getWidgets())) {
            assertThat(newDash.getWidgets()).isNull();
        } else {
            oldDash.getWidgets().forEach(oldWidget -> {

                var newWidget= newDash.getWidgets().stream().filter(
                                w -> oldWidget.getName().equals(w.getName()) && oldWidget.getType().equals(w.getType()))
                        .findFirst().get();
                assertThat(oldWidget.getQuery()).isEqualTo(newWidget.getQuery());
                assertThat(oldWidget.getPrecalculate()).isEqualTo(newWidget.getPrecalculate());
                assertThat(oldWidget.getDisplayInfo()).isEqualTo(newWidget.getDisplayInfo());
                Map<String, Object> oldMetadata = (Map<String, Object>) (oldWidget.getMetadata());
                Map<String, Object> newMetadata = (Map<String, Object>) (newWidget.getMetadata());
                List<String> oldChildren = null;
                List<String> newChildren = null;
                if (oldMetadata.containsKey("children")) {
                    oldChildren = (List<String>) oldMetadata.get("children");
                    newChildren = (List<String>) newMetadata.get("children");
                    assertThat(oldChildren.size()).isEqualTo(newChildren.size());
                    assertThat(oldChildren).isNotEqualTo(newChildren);
                }
            });
        }
    }

    private void compareCategories(OrgUnitCategory originalCategory, OrgUnitCategory newCategory) {
        assertThat(originalCategory.getWorkspaceId()).isNotEqualTo(newCategory.getWorkspaceId());
        assertThat(originalCategory.getRootOuRefId()).isNotEqualTo(newCategory.getRootOuRefId());
        assertThat(originalCategory.getRootOuId()).isNotEqualTo(newCategory.getRootOuId());
        assertThat(originalCategory.getName()).isEqualTo(newCategory.getName());
        assertThat(originalCategory.getDescription()).isEqualTo(newCategory.getDescription());
        assertThat(originalCategory.getIsPredefined()).isEqualTo(newCategory.getIsPredefined());
        assertThat(originalCategory.getOusCount()).isEqualTo(newCategory.getOusCount());
        assertThat(originalCategory.getEnabled()).isEqualTo(newCategory.getEnabled());
    }

    private void compareOus(DBOrgUnit originalOu, DBOrgUnit newOu) throws SQLException {
        assertThat(newOu.getDescription()).isEqualTo(originalOu.getDescription());
        assertThat(newOu.getDefaultDashboardId()).isEqualTo(originalOu.getDefaultDashboardId());
        assertThat(newOu.getNoOfDashboards()).isEqualTo(originalOu.getNoOfDashboards());
        assertThat(newOu.getPath()).isEqualTo(originalOu.getPath());
        assertThat(newOu.getManagers()).isEqualTo(originalOu.getManagers());
        assertThat(newOu.getSections().size()).isEqualTo(originalOu.getSections().size());
        newOu.getSections().forEach(section -> {
            var foundMatch = originalOu.getSections().stream().anyMatch(oldSection ->
                    Objects.equals(oldSection.getIntegrationId(), section.getIntegrationId()) &&
                            Objects.equals(oldSection.getDynamicUsers(), section.getDynamicUsers()) &&
                            Objects.equals(oldSection.getIntegrationFilters(), section.getIntegrationFilters()) &&
                            Objects.equals(oldSection.getIntegrationType(), section.getIntegrationType()));
            assertThat(foundMatch).isEqualTo(true);
        });

        assertThat(newOu.getWorkspaceId()).isNotEqualTo(originalOu.getWorkspaceId());
        if (newOu.getParentRefId() != null || originalOu.getParentRefId() != null) {
            assertThat(newOu.getParentRefId()).isNotEqualTo(originalOu.getParentRefId());
        }
        assertThat(newOu.getOuCategoryId()).isNotEqualTo(originalOu.getOuCategoryId());
        compareDashboardMappings(originalOu, newOu);
    }

    private DbListResponse<DBOrgUnit> getAllOus(String workspaceId) throws SQLException {
        return orgUnitsDatabaseService.filter(company, QueryFilter.builder().strictMatch("workspace_id", Integer.parseInt(workspaceId)).build(), 0, 100);
    }

    private DbListResponse<OrgUnitCategory> getCategories(String workspaceId) throws SQLException {
        return orgUnitCategoryDatabaseService.list(company, QueryFilter.builder().strictMatch("workspace_id", Integer.parseInt(workspaceId)).build(), 0, 100);
    }
}
