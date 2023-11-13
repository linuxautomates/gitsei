package io.levelops.notification.services;

import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackInteractiveIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import io.levelops.tenant_config.clients.TenantConfigClient;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

public class NotificationServiceTest {
    public static final String COMPANY = "test";

    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private NotificationService notificationService;
    @Mock
    private static TenantConfigClient tenantConfigClient;

    @Mock
    private EmailService emailService;
    @Mock
    private SlackIngestionService slackIngestionService;
    @Mock
    private SlackInteractiveIngestionService slackInteractiveIngestionService;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private SlackService slackService;
    @Mock
    private MSTeamsService msTeamsService;
    @Mock
    private SlackUserIngestionService slackUserIngestionService;
    @Mock
    private TemplateService templateService;

    @Before
    public void setup() throws SQLException, SlackClientException {
        MockitoAnnotations.initMocks(this);

        DataSource dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        dataSource.getConnection().prepareStatement("CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";").execute();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        List.of(
                "CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\";",
                "DROP SCHEMA IF EXISTS test CASCADE; ",
                "CREATE SCHEMA test; "
        ).forEach(template::execute);

        notificationService = new NotificationService(
                emailService, slackIngestionService, slackInteractiveIngestionService,
                slackUserIngestionService, templateService, inventoryService, slackService,
                msTeamsService, tenantConfigClient);
    }

    @Test
    public void testGetDefaultEmail() throws SQLException, InternalApiClientException {
        when(tenantConfigClient.get(COMPANY, "DEFAULT_TENANT_EMAIL_FROM_NAME")).thenReturn(DbListResponse.of(List.of(), 0));
        var email = notificationService.getDefaultEmailContact(COMPANY);
        assertThat(email.getName()).isEqualTo(COMPANY + " Admin");


        when(tenantConfigClient.get(COMPANY, "DEFAULT_TENANT_EMAIL_FROM_NAME")).thenReturn(DbListResponse.of(List.of(
                TenantConfig.builder().value("Example Name").build()
        ), 0));
        var customEmail = notificationService.getDefaultEmailContact(COMPANY);
        assertThat(customEmail.getName()).isEqualTo("Example Name");
    }
}
