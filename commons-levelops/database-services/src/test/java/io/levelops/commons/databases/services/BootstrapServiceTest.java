package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.services.velocity.VelocityConfigsDatabaseService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

public class BootstrapServiceTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();

    private static DataSource dataSource;
    private static ObjectMapper mapper = DefaultObjectMapper.get();
    private static TenantConfigService tenantConfigService;

    @BeforeClass
    public static void setup() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        tenantConfigService = new TenantConfigService(dataSource);
    }

    private BootstrapService setupForTenant(final String company) throws SQLException {
        new DatabaseSchemaService(dataSource).ensureSchemaExistence(company);
        new UserService(dataSource, mapper).ensureTableExistence(company);
        new DashboardWidgetService(dataSource, mapper).ensureTableExistence(company);
        new BestPracticesService(dataSource).ensureTableExistence(company);
        new QuestionnaireTemplateDBService(dataSource).ensureTableExistence(company);
        new TicketTemplateDBService(dataSource, mapper).ensureTableExistence(company);
        tenantConfigService.ensureTableExistence(company);
        new IntegrationService(dataSource).ensureTableExistence(company);
        new CiCdInstancesDatabaseService(dataSource).ensureTableExistence(company);
        new CiCdJobsDatabaseService(dataSource).ensureTableExistence(company);
        VelocityConfigsDatabaseService velocityConfigsDatabaseService = new VelocityConfigsDatabaseService(dataSource, mapper, null);
        velocityConfigsDatabaseService.ensureTableExistence(company);
        return new BootstrapService(dataSource, mapper, tenantConfigService, velocityConfigsDatabaseService);
    }

    @Test
    public void test() throws SQLException {
        BootstrapService bootstrapService = setupForTenant("test1");
        bootstrapService.bootstrapTenant("test1", null, null, null);
        assertThat(IterableUtils.getFirst(tenantConfigService.listByFilter("test1", "DEFAULT_SCM_VELOCITY_CONFIG_ID", 0, 1).getRecords())).isPresent();
    }

    @Test
    public void testBootstrapWithUser() throws SQLException {
        BootstrapService bootstrapService = setupForTenant("test2");
        bootstrapService.bootstrapTenant("test2", "Name", "LastName", "test@test.test");
    }

    @Test
    public void testBootstrapWithCustomerSuccess() throws SQLException {
        BootstrapService bootstrapService = setupForTenant("test3");
        bootstrapService.bootstrapTenant("test3", "customersuccess", "customersuccess", "sei-cs@harness.io");
    }
}
