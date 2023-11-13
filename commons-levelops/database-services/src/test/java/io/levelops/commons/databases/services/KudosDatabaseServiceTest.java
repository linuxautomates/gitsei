package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.QueryFilter;
import io.levelops.commons.databases.models.database.dashboard.Dashboard;
import io.levelops.commons.databases.models.database.dashboard.Widget;
import io.levelops.commons.databases.models.database.kudos.DBKudos;
import io.levelops.commons.databases.models.database.kudos.DBKudosSharing;
import io.levelops.commons.databases.models.database.kudos.DBKudosWidget;
import io.levelops.commons.databases.models.database.kudos.DBKudosSharing.KudosSharingType;
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
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class KudosDatabaseServiceTest {

    private static final String company = "test";
    private static final ObjectMapper mapper = DefaultObjectMapper.get();

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;
    private static KudosDatabaseService dbService;
    private static DashboardWidgetService widgetsService;
    private static UserService userService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                "CREATE SCHEMA IF NOT EXISTS " + company,
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
        .forEach(template.getJdbcTemplate()::execute);

        userService = new UserService(dataSource, mapper);
        userService.ensureTableExistence(company);

        widgetsService = new DashboardWidgetService(dataSource, mapper);
        widgetsService.ensureTableExistence(company);

        dbService = new KudosDatabaseService(mapper, dataSource);
        dbService.ensureTableExistence(company);
    }

    @Test
    public void test() throws SQLException{
        var wId = UUID.randomUUID();
        var dashboard = Dashboard.builder()
            .firstName("firstName")
            .lastName("lastName")
            .email("email")
            .name("name")
            .ownerId("ownerId")
            .type("type")
            .widgets(List.of(Widget.builder()
                .id(wId.toString())
                .name("my")
                .build()))
            .build();
        
        var dId = widgetsService.insert(company, dashboard);
        
        var item = DBKudos.builder()
            .id(UUID.randomUUID())
            .anonymousLink(false)
            .author("author")
            .body("body")
            .breadcrumbs("breadcrumbs")
            .dashboardId(UUID.randomUUID())
            .expiration(Instant.now())
            .icon("icon")
            .includeWidgetDetails(true)
            .level("level")
            .type("type")
            .widgets(Set.of(DBKudosWidget.builder()
                .data(Map.of("n1", 1, "n2", "t"))
                .position(0)
                .size(10)
                .widgetId(wId)
                .build()))
            .sharings(Set.of(DBKudosSharing.builder()
                .type(KudosSharingType.SLACK)
                .target("test@test.test")
                .build()))
            .build();
        var id = dbService.insert(company, item);

        Assertions.assertThat(id).isNotBlank();
        System.out.println("ID: " + id);

        var results = dbService.list(company, 0, 10);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getCount()).isEqualTo(1);

        results = dbService.filter(company, QueryFilter.builder().strictMatch("type", "type").build(), 0, 1);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getCount()).isEqualTo(1);

        results = dbService.filter(company, QueryFilter.builder().strictMatch("type", "noType").build(), 0, 1);
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.getCount()).isEqualTo(0);

        var kudos = dbService.get(company, id);
        Assertions.assertThat(kudos).isNotEmpty();
        // Assertions.assertThat(kudos);

        var sharings = dbService.getKudosSharings(company, UUID.fromString(id));
        Assertions.assertThat(sharings).isNotEmpty();

        var widgetKudos = dbService.getKudosWidgets(company, UUID.fromString(id));
        Assertions.assertThat(widgetKudos).isNotEmpty();
    }
    
}
