package io.levelops.commons.databases.services.organization;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Integration.Authentication;
import io.levelops.commons.databases.models.database.organization.Workspace;
import io.levelops.commons.databases.services.IntegrationService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;

import org.assertj.core.api.Assertions;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class WorkspaceDatabaseServiceTest {

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private static DataSource dataSource;

    private static NamedParameterJdbcTemplate template;

    private static final String company = "test";
    private static IntegrationService integrationService;
    private static WorkspaceDatabaseService workspaceService;

    @BeforeClass
    public static void setup() throws SQLException, IOException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        template = new NamedParameterJdbcTemplate(dataSource);
        List.<String>of(
                "CREATE SCHEMA IF NOT EXISTS " + company,
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";"
        )
        .forEach(template.getJdbcTemplate()::execute);

        integrationService = new IntegrationService(dataSource);
        integrationService.ensureTableExistence(company);
        workspaceService = new WorkspaceDatabaseService(dataSource);
        workspaceService.ensureTableExistence(company);
        // userIdentityService = new UserIdentityService(dataSource);
        // userIdentityService.ensureTableExistence(company);
        // orgVersionsService = new OrgVersionsDatabaseService(dataSource);
        // orgVersionsService.ensureTableExistence(company);
        // orgUsersService = new OrgUsersDatabaseService(dataSource, mapper, orgVersionsService, userIdentityService);
        // orgUsersService.ensureTableExistence(company);
        // UserService userService = new UserService(dataSource, mapper);
        // userService.ensureTableExistence(company);
        // DashboardWidgetService dashboardWidgetService = new DashboardWidgetService(dataSource, mapper);
        // dashboardWidgetService.ensureTableExistence(company);

        // OrgUnitCategoryDatabaseService orgUnitCategoryDatabaseService = new OrgUnitCategoryDatabaseService(dataSource);
        // orgUnitCategoryDatabaseService.ensureTableExistence(company);
        // orgUnitsService = new OrgUnitsDatabaseService(dataSource, mapper, null, orgUsersService, orgVersionsService, dashboardWidgetService);
        // orgUnitsService.ensureTableExistence(company);

        // var firstVersion = orgVersionsService.insert(company, OrgAssetType.USER);
        // orgVersionsService.update(company, firstVersion, true);
    }

    // @Test
    public void test() throws SQLException{
        var integration1 = Integration.builder()
            .authentication(Authentication.API_KEY)
            .name("integ1")
            .description("my integ1")
            .application("application")
            .metadata(new HashMap<>())
            .satellite(false)
            .status("ok")
            .build();
        var integId1 = integrationService.insert(company, integration1);

        var integration2 = Integration.builder()
            .authentication(Authentication.API_KEY)
            .name("integ2")
            .description("my integ2")
            .application("application")
            .metadata(new HashMap<>())
            .satellite(false)
            .status("ok")
            .build();
        var integId2 = integrationService.insert(company, integration2);

        var workspace = Workspace.builder()
            .name("w1")
            .description("my w1")
            .integrationIds(Set.of(Integer.valueOf(integId1), Integer.valueOf(integId2)))
            .build();
        var workspaceId1 = workspaceService.insert(company, workspace);

        Assertions.assertThat(workspaceId1).isNotBlank();

        log.info("id1: {}", workspaceId1);
        var ws1 = workspaceService.get(company, workspaceId1);

        Assertions.assertThat(ws1).isNotEmpty();
        Assertions.assertThat(ws1.get().getName()).isEqualTo(workspace.getName());
        Assertions.assertThat(ws1.get().getDescription()).isEqualTo(workspace.getDescription());
        Assertions.assertThat(ws1.get().getIntegrationIds()).isEqualTo(workspace.getIntegrationIds());

        var list = workspaceService.list(company, 0,1);

        Assertions.assertThat(list).isNotNull();
        Assertions.assertThat(list.getCount()).isEqualTo(1);
        Assertions.assertThat(list.getRecords().get(0).getName()).isEqualTo(workspace.getName());
        Assertions.assertThat(list.getRecords().get(0).getDescription()).isEqualTo(workspace.getDescription());
        Assertions.assertThat(list.getRecords().get(0).getIntegrationIds()).isEqualTo(workspace.getIntegrationIds());
    }
    
}
