package io.levelops.notification.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.enviornment.PropeloEnvironmentType;
import io.levelops.commons.enviornment.PropeloEnvironmentUtils;
import io.levelops.exceptions.EmailException;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraInternalClientFactory;
import io.levelops.integrations.jira.models.JiraCreateIssueFields;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.models.Email;
import io.levelops.models.EmailContact;
import io.levelops.notification.clients.SlackBotClient;
import io.levelops.notification.clients.SlackBotInternalClientFactory;
import io.levelops.notification.clients.SlackClientException;
import io.levelops.notification.utils.SlackHelper;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import lombok.extern.log4j.Log4j2;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Log4j2
public class TenantManagementNotificationService {
    private final String notificationEmailAddress;
    private final String slack_bot_name;
    private final String jira_ticket_assignee_account_number;
    private final String jira_itops_project_id;

    private static final String TENANT_CREATION_SUCCESS_SLACK_TEMPLATE = "Tenant for $tenant successfully created!" +
            " Environment: $env, Jira Key: $jiraUrl";
    private static final String TENANT_CREATION_SUCCESS_EMAIL_TEMPLATE = "Tenant for $tenant successfully created!" +
            " Environment: $env, Jira Key: $jiraUrl";
    private static final String TENANT_CREATION_JIRA_TICKET_TEMPLATE = "Company Name: $tenantCompanyName\n" +
            "Primary Customer Contact: $userName\n" +
            "Created by: $createdBy";
    private static final String FIRST_LOGIN_TENANT_CONFIG_NAME = "FIRST_LOGIN_USER";
    private static final String FIRST_LOGIN_MESSAGE_TEMPLATE = "First login for tenant $tenant detected. User: $user";
    private final SlackBotInternalClientFactory slackBotInternalClientFactory;
    private final JiraInternalClientFactory jiraInternalClientFactory;
    private final ObjectMapper objectMapper;
    private final SlackHelper slackHelper;
    private final TemplateService templateService;
    private final EmailService emailService;
    private final TenantConfigService tenantConfigService;
    private final EmailContact from;

    private final PropeloEnvironmentType environmentType;
    private final String slackChannelId;


    public TenantManagementNotificationService(
            SlackBotInternalClientFactory slackBotInternalClientFactory,
            JiraInternalClientFactory jiraInternalClientFactory,
            ObjectMapper objectMapper,
            TemplateService templateService,
            EmailService emailService,
            TenantConfigService tenantConfigService,
            final String oAuthBaseURL,
            final String slackChannelId,
            final String notificationEmailAddress,
            final String slack_bot_name,
            final String jira_ticket_assignee_account_number,
            final String jira_itops_project_id
            ) {
        this.notificationEmailAddress = notificationEmailAddress;
        this.slack_bot_name = slack_bot_name;
        this.jira_ticket_assignee_account_number = jira_ticket_assignee_account_number;
        this.jira_itops_project_id = jira_itops_project_id;
        this.slackBotInternalClientFactory = slackBotInternalClientFactory;
        this.jiraInternalClientFactory = jiraInternalClientFactory;
        this.objectMapper = objectMapper;
        this.slackHelper = new SlackHelper(objectMapper);
        this.templateService = templateService;
        this.emailService = emailService;
        this.tenantConfigService = tenantConfigService;
        this.from = EmailContact.builder()
                .email("do-not-reply@propelo.ai")
                .name("Propelo")
                .build();
        this.environmentType = PropeloEnvironmentUtils.getEnvironmentFromOauthBaseUrl(oAuthBaseURL);
        this.slackChannelId = slackChannelId;
    }

    private void sendTenantCreationEmail(String tenantName, String jiraUrl) throws EmailException {
        if (!environmentType.isProd()) {
            log.info("Not sending tenant creation email for non-prod environment. Tenant name: " + tenantName);
            return;
        }
        var content = templateService.evaluateTemplate(
                TENANT_CREATION_SUCCESS_EMAIL_TEMPLATE, Map.of(
                        "tenant", tenantName,
                        "env", environmentType.toString(),
                        "jiraUrl", jiraUrl));
        emailService.send(Email.builder()
                .subject("Tenant created for " + tenantName)
                .content(content)
                .contentType("text/html")
                .from(from)
                .recipient(notificationEmailAddress)
                .build());
    }

    private void sendTenantCreationSlack(String tenantName, String jiraUrl) throws JsonProcessingException, SlackClientException {
        SlackBotClient slackClient = slackBotInternalClientFactory.get();
        String slackContents = templateService.evaluateTemplate(
                TENANT_CREATION_SUCCESS_SLACK_TEMPLATE, Map.of(
                        "tenant", tenantName,
                        "env", environmentType.toString(),
                        "jiraUrl", jiraUrl));
        slackClient.postChatInteractiveMessage(
                slackChannelId, slackHelper.getPlainTextSlackBlock(slackContents), slack_bot_name);
    }

    public void sendTenantCreationSuccessfulNotifications(String tenantName, String jiraUrl) throws JsonProcessingException, SlackClientException, EmailException {
        sendTenantCreationSlack(tenantName, jiraUrl);
        sendTenantCreationEmail(tenantName, jiraUrl);
    }

    public Optional<JiraIssue> createTenantCreationJiraTicket(
            String tenantCompanyName,
            String userName,
            String createdBy) throws JiraClientException {
        if (!environmentType.isProd()) {
            log.info("Not creating tenant creation jira ticket for non-prod enviornments. Tenant: " + tenantCompanyName);
            return Optional.empty();
        }
        JiraClient jiraClient = jiraInternalClientFactory.get();
        return Optional.ofNullable(jiraClient.createIssue(
                JiraCreateIssueFields.builder()
                        .summary("Create new tenant for " + tenantCompanyName)
                        .assignee(JiraUser.builder().accountId(jira_ticket_assignee_account_number).build())
                        .description(templateService.evaluateTemplate(
                                TENANT_CREATION_JIRA_TICKET_TEMPLATE,
                                Map.of("tenantCompanyName", tenantCompanyName,
                                        "userName", userName,
                                        "createdBy", createdBy)
                        ))
                        .issueType(JiraIssueType.builder().id(10002L).build())
                        .project(JiraProject.builder().id(jira_itops_project_id).build())
                        .labels(List.of("tenantCreation"))
                        .priority(JiraIssueFields.JiraPriority.builder().name("High").build())
                        .build()
        ));
    }

    private boolean isFirstLogin(String companyName) {
        try {
            return tenantConfigService.listByFilter(companyName, FIRST_LOGIN_TENANT_CONFIG_NAME, 0, 1)
                    .getRecords()
                    .stream()
                    .findFirst()
                    .isEmpty();
        } catch (SQLException e) {
            log.error("SQL query to find first login info failed: " + e.getMessage());
            return false;
        }
    }

    public static boolean isPropeloEmail(String email) {
        return email.contains("@propelo.ai") || email.contains("@levelops.io");
    }

    public void checkFirstLoginAndNotifyIfNeeded(String companyName, String loggedInUserEmail) {
        if (!isPropeloEmail(loggedInUserEmail) && isFirstLogin(companyName)) {
            try {
                tenantConfigService.insert(companyName, TenantConfig.builder()
                        .name(FIRST_LOGIN_TENANT_CONFIG_NAME)
                        .value(loggedInUserEmail).build());
            } catch (SQLException e) {
                log.error("Failed to insert first login info: " + e.getMessage());
            }

            String messageContent = templateService.evaluateTemplate(
                    FIRST_LOGIN_MESSAGE_TEMPLATE, Map.of("tenant", companyName, "user", loggedInUserEmail)
            );

            try {
                SlackBotClient slackClient = slackBotInternalClientFactory.get();

                slackClient.postChatInteractiveMessage(
                        slackChannelId,
                        slackHelper.getPlainTextSlackBlock(messageContent),
                        slack_bot_name);
            } catch (SlackClientException | JsonProcessingException e) {
                log.error("Failed to send first login slack message. Error: " + e.getMessage(), e);
            }

            if (environmentType.isProd()) {
                try {
                    emailService.send(Email.builder()
                            .subject("First user logged in for " + companyName)
                            .content(messageContent)
                            .contentType("text/html")
                            .from(from)
                            .recipient(notificationEmailAddress)
                            .build());
                } catch (EmailException e) {
                    log.error("Failed to send first login email. Error: " + e.getMessage());
                }
            }
        }
    }
}
