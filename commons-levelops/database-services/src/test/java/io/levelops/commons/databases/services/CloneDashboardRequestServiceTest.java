package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Product;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.CloneDashboardRequest;
import io.levelops.commons.databases.models.database.dashboard.CloneDashboardResponse;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.mappings.ProductIntegMapping;
import io.levelops.commons.databases.services.organization.OrgUnitCategoryDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUnitsDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.databases.services.organization.OrgVersionsDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.web.exceptions.BadRequestException;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class CloneDashboardRequestServiceTest {
    private final String company1 = "test2";
    private final String company2 = "test3";
    private final List<Widget> widgets = new ArrayList<>();
    private String prodId;
    private String integId;
    private static OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService;

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DataSource dataSource;
    private ProductService prodService;
    private ProductIntegMappingService productIntegMappingService;
    private TagItemDBService tagItemDBService;
    private TagsService tagsService;
    private IntegrationService integrationService;
    private DashboardWidgetService dws;
    private UserService userService;
    private CannedDashboardService cds;
    private String userId;
    private String destUserId;
    private OUDashboardService ouDashboardService;
    private OrgUnitsDatabaseService orgUnitsDatabaseService;
    private static OrgVersionsDatabaseService orgVersionsService;
    private static OrgUsersDatabaseService usersService;
    private static UserIdentityService userIdentityService;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        prodService = new ProductService(dataSource);
        tagItemDBService = new TagItemDBService(dataSource);
        tagsService = new TagsService(dataSource);
        integrationService =  new IntegrationService(dataSource);
        productIntegMappingService = new ProductIntegMappingService(dataSource, DefaultObjectMapper.get());
        userIdentityService = new UserIdentityService(dataSource);
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        dws = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        cds = new CannedDashboardService(dws, prodService, integrationService, productIntegMappingService, userService);
        DatabaseSchemaService databaseSchemaService = new DatabaseSchemaService(dataSource);
        orgVersionsService=new OrgVersionsDatabaseService(dataSource);
        usersService = new OrgUsersDatabaseService(dataSource, DefaultObjectMapper.get(), orgVersionsService, userIdentityService);
        databaseSchemaService.ensureSchemaExistence(company1);
        databaseSchemaService.ensureSchemaExistence(company2);
        tagsService.ensureTableExistence(company1);
        tagsService.ensureTableExistence(company2);
        tagItemDBService.ensureTableExistence(company1);
        tagItemDBService.ensureTableExistence(company2);
        integrationService.ensureTableExistence(company1);
        integrationService.ensureTableExistence(company2);
        userService.ensureTableExistence(company1);
        userService.ensureTableExistence(company2);
        prodService.ensureTableExistence(company1);
        prodService.ensureTableExistence(company2);
        userIdentityService.ensureTableExistence(company1);
        userIdentityService.ensureTableExistence(company2);
        productIntegMappingService.ensureTableExistence(company1);
        productIntegMappingService.ensureTableExistence(company2);
        orgVersionsService.ensureTableExistence(company1);
        orgVersionsService.ensureTableExistence(company2);
        usersService.ensureTableExistence(company1);
        usersService.ensureTableExistence(company2);
        orgUnitsDatabaseService = new OrgUnitsDatabaseService(dataSource, DefaultObjectMapper.get(), tagItemDBService, null, orgVersionsService, dws);
        orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource, new OrgUnitHelper(
                new OrgUnitsDatabaseService(dataSource, DefaultObjectMapper.get(), tagItemDBService, null, orgVersionsService, dws), integrationService), DefaultObjectMapper.get());
        ouDashboardService=new OUDashboardService(dataSource,DefaultObjectMapper.get(),orgUnitsDatabaseService);
        dws.ensureTableExistence(company1);
        dws.ensureTableExistence(company2);
        orgUnitCategoryDatabaseService.ensureTableExistence(company1);
        orgUnitCategoryDatabaseService.ensureTableExistence(company2);
        orgUnitsDatabaseService.ensureTableExistence(company2);
        orgUnitsDatabaseService.ensureTableExistence(company1);
        ouDashboardService.ensureTableExistence(company1);
        ouDashboardService.ensureTableExistence(company2);

        userId = userService.insert(company1, User.builder()
                .userType(RoleType.ADMIN)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        destUserId = userService.insert(company2, User.builder()
                .userType(RoleType.ADMIN)
                .bcryptPassword("asd")
                .email("asd@asdsw.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        prodService.insert(company1, Product.builder()
                .name("Default Product1")
                .key("Default Product1")
                .description("test")
                .ownerId(userId)
                .bootstrapped(true)
                .immutable(true)
                .build());
        prodId = prodService.insert(company2, Product.builder()
                .name("Default Product2")
                .key("Default Product2")
                .description("test")
                .ownerId(destUserId)
                .bootstrapped(true)
                .immutable(true)
                .build());

        integId = integrationService.insert(company2 , Integration.builder()
                .name("abc")
                .application("asd")
                .authentication(Integration.Authentication.valueOf("NONE"))
                .status("enable")
                .satellite(false)
                .url("")
                .build());

        productIntegMappingService.insert(company2, ProductIntegMapping.builder().
                productId(prodId).id("1")
                .integrationId(integId)
                .build());

    }

    @Test
    public void testDashboardCloneInsert() throws SQLException, BadRequestException {
        Widget widget = Widget.builder()
                .name("widget1")
                .type("dashboard")
                .query(Map.of("across","assignee"))
                .metadata(Map.of())
                .build();
        widgets.add(widget);

        Dashboard dashboard1 = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId)
                .widgets(widgets)
                .metadata(Map.of())
                .query(Map.of())
                .build();
        String id1 = dws.insert(company1, dashboard1);
        Dashboard dashboard2 = Dashboard.builder()
                .name("name1")
                .type("type1")
                .ownerId(userId)
                .widgets(widgets)
                .metadata(Map.of())
                .query(Map.of())
                .build();
        String id2 = dws.insert(company1, dashboard2);
        assertThat(id1).isNotNull();
        assertThat(id2).isNotNull();

        // -- CannedDashboard with Id
        CloneDashboardRequest cloneDashboardRequest = CloneDashboardRequest.builder()
                .dashboardId(id1)
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output1 = cds.cloneDashboard(cloneDashboardRequest);
        Optional<Dashboard> output2 = dws.get(output1.getDestinationTenant(), output1.getDashboardId());

        assertThat(output1.getDashboardName()).isEqualTo(output2.get().getName());
        assertThat(output1.getDashboardId()).isEqualTo(output2.get().getId());
        assertThat(output2.get().getWidgets()).isNotNull();
        assertThat(output2.get().getWidgets().stream().map(Widget::getName).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getName).collect(Collectors.toList()));
        assertThat(output2).isNotNull();

        // -- CannedDashboard with name
        CloneDashboardRequest cloneDashboardRequest1 = CloneDashboardRequest.builder()
                .dashboardName("name")
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output3 = cds.cloneDashboard(cloneDashboardRequest1);
        Optional<Dashboard> output4 = dws.get(output1.getDestinationTenant(), output1.getDashboardId());

        assertThat(output3.getDashboardName()).isNotEqualTo(output4.get().getName());
        assertThat(output3.getDashboardId()).isNotEqualTo(output4.get().getId());
        assertThat(output4.get().getWidgets().stream().map(Widget::getType).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getType).collect(Collectors.toList()));
        assertThat(output4.get().getWidgets()).isNotNull();
        assertThat(output4).isNotNull();

        // -- CannedDashboard with product_id and integration_id
        CloneDashboardRequest cloneDashboardRequest3 = CloneDashboardRequest.builder()
                .dashboardId(id1)
                .productId(prodId)
                .integrationIds(List.of(integId))
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output6 = cds.cloneDashboard(cloneDashboardRequest3);
        Optional<Dashboard> output7 = dws.get(output6.getDestinationTenant(), output6.getDashboardId());

        assertThat(output6.getDashboardName()).isEqualTo(output7.get().getName());
        assertThat(output6.getDashboardId()).isEqualTo(output7.get().getId());
        assertThat(output7.get().getWidgets().stream().map(Widget::getName).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getName).collect(Collectors.toList()));
        assertThat(output7.get().getWidgets()).isNotNull();
        assertThat(output7.get().getQuery()).isNotNull();

        // -- CannedDashboard with product_id, integration_id and owner_id
        CloneDashboardRequest cloneDashboardRequest4 = CloneDashboardRequest.builder()
                .dashboardId(id1)
                .ownerId(destUserId)
                .productId(prodId)
                .integrationIds(List.of(integId))
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output8 = cds.cloneDashboard(cloneDashboardRequest4);
        Optional<Dashboard> output9 = dws.get(output8.getDestinationTenant(), output8.getDashboardId());

        assertThat(output8.getDashboardName()).isEqualTo(output9.get().getName());
        assertThat(output8.getDashboardId()).isEqualTo(output9.get().getId());
        assertThat(output9.get().getWidgets().stream().map(Widget::getQuery).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getQuery).collect(Collectors.toList()));
        assertThat(output9.get().getWidgets()).isNotNull();
        assertThat(output9.get().getQuery()).isNotNull();

        // -- CannedDashboard with owner_id
        CloneDashboardRequest cloneDashboardRequest5 = CloneDashboardRequest.builder()
                .dashboardId(id1)
                .ownerId(destUserId)
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output10 = cds.cloneDashboard(cloneDashboardRequest5);
        Optional<Dashboard> output11 = dws.get(output10.getDestinationTenant(), output10.getDashboardId());

        assertThat(output10.getDashboardName()).isEqualTo(output11.get().getName());
        assertThat(output10.getDashboardId()).isEqualTo(output11.get().getId());
        assertThat(output2.get().getWidgets().stream().map(Widget::getMetadata).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getMetadata).collect(Collectors.toList()));
        assertThat(output11.get().getWidgets()).isNotNull();
        assertThat(output11.get().getQuery()).isNotNull();

        // -- CannedDashboard with duplicate name
        CloneDashboardRequest cloneDashboardRequest6 = CloneDashboardRequest.builder()
                .dashboardName("name")
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output12 = cds.cloneDashboard(cloneDashboardRequest6);
        Optional<Dashboard> output13 = dws.get(output12.getDestinationTenant(), output12.getDashboardId());

        assertThat(output12.getDashboardName()).isEqualTo(output13.get().getName());
        assertThat(output12.getDashboardName()).isEqualTo(output13.get().getName());
        assertThat(output2.get().getWidgets().stream().map(Widget::getType).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getType).collect(Collectors.toList()));
        assertThat(output13.get().getWidgets()).isNotNull();
        assertThat(output13).isNotNull();

        // -- CannedDashboard with new dashboard name
        CloneDashboardRequest cloneDashboardRequest7 = CloneDashboardRequest.builder()
                .dashboardName("name1")
                .sourceTenant("test2")
                .destinationTenant("test3")
                .build();
        CloneDashboardResponse output14 = cds.cloneDashboard(cloneDashboardRequest7);
        Optional<Dashboard> output15 = dws.get(output14.getDestinationTenant(), output14.getDashboardId());

        assertThat(output14.getDashboardName()).isEqualTo(output15.get().getName());
        assertThat(output14.getDashboardName()).isEqualTo(dashboard2.getName());
        assertThat(output15.get().getWidgets()).isNotNull();
        assertThat(output15.get().getWidgets().stream().map(Widget::getName).collect(Collectors.toList()))
                .isEqualTo(dashboard1.getWidgets().stream().map(Widget::getName).collect(Collectors.toList()));
        assertThat(output15).isNotNull();
    }
}
