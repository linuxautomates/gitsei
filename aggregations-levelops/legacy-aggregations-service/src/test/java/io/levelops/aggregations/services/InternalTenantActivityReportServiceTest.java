package io.levelops.aggregations.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.models.TenantActivityReport;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Tenant;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.databases.services.TenantService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.licensing.exception.LicensingException;
import io.levelops.commons.licensing.model.License;
import io.levelops.commons.licensing.service.LicensingService;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.services.EmailService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@Log4j2
public class InternalTenantActivityReportServiceTest {

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private TenantService tenantService;
    private ActivityLogService activityLogService;
    private InternalTenantActivityReportService internalTenantActivityReportService;

    @Mock
    private LicensingService licensingService;

    @Mock
    private SlackBotInternalClientFactory slackBotFactory;

    @Mock
    private EmailService emailService;

    private final List<Tenant> all_tenants = new ArrayList<>();

    @Before
    public void setup() throws SQLException, LicensingException {
        MockitoAnnotations.initMocks(this);

        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        ObjectMapper mapper = DefaultObjectMapper.get();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.execute("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";");

        tenantService = new TenantService(dataSource);
        activityLogService = new ActivityLogService(dataSource, mapper);
        internalTenantActivityReportService = new InternalTenantActivityReportService(
                tenantService,
                licensingService,
                activityLogService,
                slackBotFactory,
                emailService,
                DefaultObjectMapper.get(),
                "C03NCTPS6CW",
                "levelops-api-and-data"
        );

        for (int i = 0; i < 5; i++) {
            all_tenants.add(Tenant.builder().tenantName("test" + i).id(String.valueOf(i)).build());
        }
        for (Tenant tenant : all_tenants) {
            template.execute("DROP SCHEMA IF EXISTS " + tenant.getTenantName() + "CASCADE;");
            template.execute("CREATE SCHEMA " + tenant.getTenantName());
            tenantService.ensureTableExistence(tenant.getTenantName());
            activityLogService.ensureTableExistence(tenant.getTenantName());
        }

        var tenant_to_license_map = List.of("limited_trial", "full", "limited_trial", "full", "limited_trial");
        for (int i = 0; i < tenant_to_license_map.size(); i++) {
            when(licensingService.getLicense(all_tenants.get(i).getTenantName())).thenReturn(License.builder()
                    .company("test").license(tenant_to_license_map.get(i)).build());
        }

        setupActivityLogForTenants();
        setupTenantTable();
    }

    private ActivityLog createSuccessfulLoginLog(String email) {
        return ActivityLog.builder()
                .targetItem("56139a73-3446-40db-a011-6fa293924376")
                .email(email)
                .targetItemType(ActivityLog.TargetItemType.USER_LOGIN)
                .body(String.format("%s User Login: %s.", "Password", "56139a73-3446-40db-a011-6fa293924377"))
                .details(Collections.emptyMap())
                .action(ActivityLog.Action.SUCCESS)
                .build();
    }

    private void setupActivityLogForTenants() throws SQLException {
        for (int i = 0; i < all_tenants.size(); i++) {
            activityLogService.insert(all_tenants.get(i).getTenantName(), createSuccessfulLoginLog("sid" + i + "@gmail.com"));
        }
    }

    private void setupTenantTable() throws SQLException {
        for (Tenant tenant : all_tenants) {
            tenantService.insert("", tenant);
        }
    }

    @Test
    public void runReportForTenantTest() throws LicensingException {
        @SuppressWarnings("OptionalGetWithoutIsPresent") var report = internalTenantActivityReportService.runReportForTenant(all_tenants.get(0)).get();
        assertThat(report.getTenantName()).isEqualTo(all_tenants.get(0).getTenantName());
        assertThat(report.getTenantId()).isEqualTo("0");
        assertThat(report.getLicenseType().toString()).isEqualTo("limited_trial");
        assertThat(report.getLastLoggedInUser()).isEqualTo("sid0@gmail.com");
    }

    private void assertResultOrder(List<TenantActivityReport> results,
                                   List<Pair<Tenant, Integer>> expectedResultOrder) {
        for (int i = 0; i < results.size(); i++) {
            var result = results.get(i);
            var expectedTenant = expectedResultOrder.get(i);
            assertThat(result.getTenantName()).isEqualTo(expectedTenant.getLeft().getTenantName());
            assertThat(result.getLastLoggedInUser()).isEqualTo("sid" + expectedTenant.getRight() + "@gmail.com");
        }
    }

    @Test
    public void runReportForAllTenantsTest() throws SQLException, InterruptedException {
        var results = internalTenantActivityReportService.runReportForAllTenants();
        log.info(results);
        assertThat(results.size()).isEqualTo(5);

        var expectedResultOrder = List.of(
                Pair.of(all_tenants.get(0), 0),
                Pair.of(all_tenants.get(2), 2),
                Pair.of(all_tenants.get(4), 4),
                Pair.of(all_tenants.get(1), 1),
                Pair.of(all_tenants.get(3), 3)
        );

        assertResultOrder(results, expectedResultOrder);

        // Add a new login entry after 1 second, this should change the expected order
        Thread.sleep(1000);
        activityLogService.insert(all_tenants.get(0).getTenantName(), createSuccessfulLoginLog("sid0@gmail.com"));
        var expectedResultOrder2 = List.of(
                Pair.of(all_tenants.get(2), 2),
                Pair.of(all_tenants.get(4), 4),
                Pair.of(all_tenants.get(0), 0),
                Pair.of(all_tenants.get(1), 1),
                Pair.of(all_tenants.get(3), 3)
        );
        var results2 = internalTenantActivityReportService.runReportForAllTenants();
        assertResultOrder(results2, expectedResultOrder2);
    }

//    @Test
//    public void runReportForAllTenantsIntegrationTest() throws EmailException, IOException, SlackClientException {
//        var realSlackBotFactory = new SlackBotInternalClientFactory(
//                DefaultObjectMapper.get(),
//                new OkHttpClient(),
//                System.getenv("SLACK_INTERNAL_TOKEN")
//        );
//        var realEmailService = new EmailService(System.getenv("SENDGRID_API_KEY"));
//
//        var internalTenantActivityReportServiceReal = new InternalTenantActivityReportService(
//                tenantService,
//                licensingService,
//                activityLogService,
//                realSlackBotFactory,
//                realEmailService,
//                DefaultObjectMapper.get(),
//                "C03NCTPS6CW",
//                "levelops-api-and-data"
//        );
//        internalTenantActivityReportServiceReal.runAndSendReport();
//    }
}