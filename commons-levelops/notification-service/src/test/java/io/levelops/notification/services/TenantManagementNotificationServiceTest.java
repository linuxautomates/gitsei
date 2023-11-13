package io.levelops.notification.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.exceptions.EmailException;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraInternalClientFactory;
import io.levelops.notification.clients.SlackBotClient;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import io.zonky.test.db.postgres.junit.EmbeddedPostgresRules;
import io.zonky.test.db.postgres.junit.SingleInstancePostgresRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TenantManagementNotificationServiceTest {
    public static final String COMPANY = "test";

    @Rule
    public SingleInstancePostgresRule pg = EmbeddedPostgresRules.singleInstance();
    private TenantManagementNotificationService prodNotificationService;
    private TenantManagementNotificationService nonProdNotificationService;
    @Mock
    private SlackBotInternalClientFactory slackBotInternalClientFactory;
    @Mock
    private SlackBotClient slackBotClient;
    @Mock
    private EmailService emailService;
    @Mock
    private JiraInternalClientFactory jiraInternalClientFactory;
    @Mock
    private JiraClient jiraClient;
    private final TemplateService templateService = new TemplateService();
    private TenantConfigService tenantConfigService;
    private final String channelId = "dummyId";

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

        tenantConfigService = new TenantConfigService(dataSource);
        tenantConfigService.ensureTableExistence(COMPANY);

        when(slackBotInternalClientFactory.get()).thenReturn(slackBotClient);
        when(jiraInternalClientFactory.get()).thenReturn(jiraClient);

        prodNotificationService = createNotificationService("https://app.propelo.ai");
        nonProdNotificationService = createNotificationService("https://testui1.propelo.ai");
    }

    private TenantManagementNotificationService createNotificationService(String baseUrl) {
        return new TenantManagementNotificationService(
                slackBotInternalClientFactory,
                jiraInternalClientFactory,
                DefaultObjectMapper.get(),
                templateService,
                emailService,
                tenantConfigService,
                baseUrl,
                channelId,
                "tenantStatus@propelo.ai",
                "SlackBot",
                "5deadfb2752c1b0d114f9db3",
                "10020"
        );
    }

    @Test
    public void isPropeloEmailTest() {
        assertThat(TenantManagementNotificationService.isPropeloEmail("sid@propelo.ai")).isTrue();
        assertThat(TenantManagementNotificationService.isPropeloEmail("sid@levelops.io")).isTrue();
        assertThat(TenantManagementNotificationService.isPropeloEmail("sid@gmail.com")).isFalse();
    }

    @Test
    public void checkFirstLoginAndNotifyIfNeededTest() throws SlackClientException, EmailException {
        prodNotificationService.checkFirstLoginAndNotifyIfNeeded(COMPANY, "sid@gmail.com");
        prodNotificationService.checkFirstLoginAndNotifyIfNeeded(COMPANY, "sid@gmail.com");
        prodNotificationService.checkFirstLoginAndNotifyIfNeeded(COMPANY, "sidSecondary@gmail.com");

        // Slack message and email should only be sent once
        verify(slackBotClient, times(1)).postChatInteractiveMessage(eq(channelId), anyString(), anyString());
        verify(emailService, times(1)).send(any());
    }

    @Test
    public void ensurePropeloLoginDoesNotNotify() throws SlackClientException, EmailException {
        prodNotificationService.checkFirstLoginAndNotifyIfNeeded(COMPANY, "sid@propelo.ai");

        // Slack message and email should not be sent for @propelo.ai emails
        verify(slackBotClient, never()).postChatInteractiveMessage(eq(channelId), anyString(), anyString());
        verify(emailService, never()).send(any());
    }

    @Test
    public void prodTenantCreationTest() throws EmailException, SlackClientException, JsonProcessingException, JiraClientException {
        prodNotificationService.sendTenantCreationSuccessfulNotifications("sid", "PROP-277");
        prodNotificationService.createTenantCreationJiraTicket("sid", "sid@gmail.com","sid@propelo.ai");

        verify(slackBotClient, times(1)).postChatInteractiveMessage(eq(channelId), anyString(), anyString());
        verify(jiraClient, times(1)).createIssue(any());
        verify(emailService, times(1)).send(any());
    }

    @Test
    public void nonProdTenantCreationTest() throws EmailException, SlackClientException, JsonProcessingException, JiraClientException {
        nonProdNotificationService.sendTenantCreationSuccessfulNotifications("sid", "PROP-277");
        nonProdNotificationService.createTenantCreationJiraTicket("sid", "sid@gmail.com","sid@propelo.ai");

        verify(slackBotClient, times(1)).postChatInteractiveMessage(eq(channelId), anyString(), anyString());
        verify(jiraClient, never()).createIssue(any());
        verify(emailService, never()).send(any());
    }

}