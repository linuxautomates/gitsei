package io.levelops.commons.databases.services;

import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class WidgetServiceTest {
    private final String company = "test";
    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private DashboardWidgetService dws;
    private UserService userService;
    private DataSource dataSource;
    private String userId;
    private String dashboardId;

    @Before
    public void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        userService = new UserService(dataSource, DefaultObjectMapper.get());
        dws = new DashboardWidgetService(dataSource, DefaultObjectMapper.get());
        new DatabaseSchemaService(dataSource)
                .ensureSchemaExistence(company);
        userService.ensureTableExistence(company);
        userId = userService.insert(company, User.builder()
                .userType(RoleType.ADMIN)
                .bcryptPassword("asd")
                .email("asd@asd.com")
                .passwordAuthEnabled(Boolean.FALSE)
                .samlAuthEnabled(false)
                .firstName("asd")
                .lastName("asd")
                .build());
        dws.ensureTableExistence(company);

        Dashboard dashboard = Dashboard.builder()
                .name("name")
                .type("type")
                .ownerId(userId)
                .metadata(Map.of())
                .query(Map.of())
                .build();
        dashboardId = dws.insert(company, dashboard);
    }

    @Test
    public void testInsert() throws SQLException {
        Widget widget = Widget.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .displayInfo(Map.of("description","test Widget"))
                .build();

        String id1 = dws.insertWidget(company, widget, dashboardId);
        assertThat(id1).isNotNull();
        Widget output = dws.getWidget(company, id1, dashboardId).orElse(null);
        DefaultObjectMapper.prettyPrint(output);

        assertThat(output).isNotNull();
        assertThat(output.getName()).isEqualTo("name");
        assertThat(output.getDashboardId()).isEqualTo(dashboardId);
        assertThat(output.getDisplayInfo()).isNotNull();
        Map displayInfo = DefaultObjectMapper.get().convertValue(output.getDisplayInfo(), Map.class);
        assertThat(displayInfo.get("description")).isEqualTo("test Widget");
    }

    @Test
    public void testDeleteBulk() throws SQLException {
        Widget widget1 = Widget.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .displayInfo(Map.of())
                .build();
        String id1 = dws.insertWidget(company, widget1, dashboardId);
        assertThat(id1).isNotNull();

        Widget widget2 = Widget.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .displayInfo(Map.of())
                .build();
        String id2 = dws.insertWidget(company, widget2, dashboardId);
        assertThat(id2).isNotNull();

        List<String> widgetIds = List.of(id1, id2);
        int count = dws.bulkWidgetsDelete(company, widgetIds);
        assertThat(count).isEqualTo(2);
        Widget output = dws.getWidget(company, id1, dashboardId).orElse(null);
        DefaultObjectMapper.prettyPrint(output + " deleteWidget");
        assertThat(output).isNull();
        Widget output1 = dws.getWidget(company, id2, dashboardId).orElse(null);
        DefaultObjectMapper.prettyPrint(output1);
        assertThat(output).isNull();
    }

    @Test
    public void testDelete() throws SQLException {
        Widget widget1 = Widget.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .displayInfo(Map.of())
                .build();
        String id1 = dws.insertWidget(company, widget1, dashboardId);
        assertThat(id1).isNotNull();
        dws.deleteWidget(company, id1, dashboardId);

        Widget output = dws.getWidget(company, id1, dashboardId).orElse(null);
        DefaultObjectMapper.prettyPrint(output + " deleteWidget");
        assertThat(output).isNull();
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testUpdate() throws SQLException, IOException {
        Widget.WidgetBuilder bldr = Widget.builder()
                .name("name")
                .type("type")
                .metadata(Map.of())
                .query(Map.of())
                .displayInfo(Map.of());
        String val = dws.insertWidget(company, bldr.build(), dashboardId);
        assertThat(dws.updateWidget(company, bldr
                .id(val)
                .name("name1")
                .displayInfo(Map.of("description","updated"))
                .build())).isTrue();
        assertThat(dws.updateWidget(company, bldr
                .id(val)
                .build()))
                .isTrue();

        Widget dbWidget = dws.getWidget(company, val, dashboardId).orElse(null);
        Assertions.assertThat(dbWidget.getName()).isEqualTo("name1");
        Map displayInfo = DefaultObjectMapper.get().convertValue( dbWidget.getDisplayInfo(), Map.class);
        Assertions.assertThat(displayInfo.get("description")).isEqualTo("updated");
    }
}
