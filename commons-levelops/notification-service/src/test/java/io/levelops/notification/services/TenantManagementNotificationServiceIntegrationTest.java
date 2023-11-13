package io.levelops.notification.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.exceptions.EmailException;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraInternalClientFactory;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.SQLException;

public class TenantManagementNotificationServiceIntegrationTest {
    @ClassRule
    public static SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private TenantManagementNotificationService notificationService;
    private static DataSource dataSource;
    private static TenantConfigService tenantConfigService;

    @Before
    public void setUp() throws SQLException {
        dataSource = pg.getEmbeddedPostgres().getPostgresDatabase();
        tenantConfigService = new TenantConfigService(dataSource);

        OkHttpClient okHttpClient = new OkHttpClient();
        var slack_token = MoreObjects.firstNonNull(System.getenv("SLACK_TOKEN") , "token");
        var email_token = MoreObjects.firstNonNull(System.getenv("EMAIL_TOKEN") , "token");
        var jira_api_token = MoreObjects.firstNonNull(System.getenv("JIRA_TOKEN") , "token");
        var slackClientFactory = SlackBotInternalClientFactory.builder()
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(okHttpClient)
                .token(slack_token)
                .build();
        var jiraInternalClientFactory = new JiraInternalClientFactory(
                DefaultObjectMapper.get(),
                okHttpClient,
                5.0,
                jira_api_token,
                "tamvada@propelo.ai"
        );
        TemplateService templateService = new TemplateService();
        EmailService emailService = new EmailService(email_token);
        var prodBaseUrl = "https://app.propelo.ai";
        var devBaseUrl = "https://testui1.propelo.ai";
        this.notificationService = new TenantManagementNotificationService(
                slackClientFactory,
                jiraInternalClientFactory,
                DefaultObjectMapper.get(),
                templateService,
                emailService,
                tenantConfigService,
                devBaseUrl,
                "C03NCTPS6CW",
                "tenantStatus@propelo.ai",
                "SlackBot",
                "5deadfb2752c1b0d114f9db3",
                "10020" );
    }


    @Test
    public void testSendCreationSuccessfullNotification() throws SlackClientException, JsonProcessingException, EmailException {
        notificationService.sendTenantCreationSuccessfulNotifications("SidTenant-test", "PROP-277");
    }

    @Test
    public void testCreateJiraTicket() throws JiraClientException {
        notificationService.createTenantCreationJiraTicket("SidTenantTest", "Sid", "sid@propelo.ai");
    }
}
