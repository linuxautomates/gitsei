package io.levelops.notification.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.MessageTemplate;
import io.levelops.commons.databases.models.database.NotificationMode;
import io.levelops.commons.databases.models.database.SlackChatInteractiveMessageResult;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.exceptions.EmailException;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.slack.services.SlackIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackInteractiveIngestionService;
import io.levelops.ingestion.integrations.slack.services.SlackUserIngestionService;
import io.levelops.models.Email;
import io.levelops.models.EmailContact;
import io.levelops.notification.clients.msteams.MSTeamsClientException;
import io.levelops.notification.models.NotificationResult;
import io.levelops.notification.models.NotificationResult.NotificationResultBuilder;
import io.levelops.notification.models.SlackNotificationResult;
import io.levelops.services.EmailService;
import io.levelops.services.TemplateService;
import io.levelops.tenant_config.clients.TenantConfigClient;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Log4j2
public class NotificationService {

    private static final String LEVELOPS_BRAND_NAME = "Propelo";
    private static final String SLACK_LEVELOPS_BRAND_NAME_HEADER = String.format("_-Powered by %s-_\n", LEVELOPS_BRAND_NAME);
    private static final String FROM_EMAIL = "do-not-reply@levelops.io";
    public static final String DEFAULT_TENANT_EMAIL_FROM_NAME = "DEFAULT_TENANT_EMAIL_FROM_NAME";

    private final EmailService emailService;
    private final SlackIngestionService slackIngestionService;
    private final SlackInteractiveIngestionService slackInteractiveIngestionService; //used only for sendInteractiveNotification functionality
    private final SlackUserIngestionService slackUserIngestionService;
    private final TemplateService templateService;
    private final InventoryService inventoryService;
    private final SlackService slackService;
    private final MSTeamsService msTeamsService;
    private final TenantConfigClient tenantConfigClient;
    private final LoadingCache<String, IntegrationKey> SLACK_INTEGRATION_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(0) // DISABLED CACHING FOR NOW
            .build(new CacheLoader<>() {
                @Override
                public IntegrationKey load(@Nonnull String tenantId) throws Exception {
                    return findSlackIntegration(tenantId)
                            .orElseThrow(() -> new Exception("Could not find an active Slack integration for tenant: "
                                    + tenantId));
                }
            });
    private final LoadingCache<String, IntegrationKey> MS_TEAMS_INTEGRATION_CACHE = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(0) // DISABLED CACHING FOR NOW
            .build(new CacheLoader<>() {
                @Override
                public IntegrationKey load(@Nonnull String tenantId) throws Exception {
                    return findMSTeamsIntegration(tenantId)
                            .orElseThrow(() -> new Exception("Could not find an active MS Teams integration for tenant: "
                                    + tenantId));
                }
            });

    @Builder
    public NotificationService(
            EmailService emailService,
            SlackIngestionService slackIngestionService,
            SlackInteractiveIngestionService slackInteractiveIngestionService, SlackUserIngestionService slackUserIngestionService, TemplateService templateService,
            InventoryService inventoryService,
            SlackService slackService,
            MSTeamsService msTeamsService,
            TenantConfigClient tenantConfigClient) {
        this.emailService = emailService;
        this.slackIngestionService = slackIngestionService;
        this.slackInteractiveIngestionService = slackInteractiveIngestionService;
        this.slackUserIngestionService = slackUserIngestionService;
        this.templateService = templateService;
        this.inventoryService = inventoryService;
        this.slackService = slackService;
        this.msTeamsService = msTeamsService;
        this.tenantConfigClient = tenantConfigClient;
    }

    public void sendQuestionnairePushNotification(MessageTemplate template, String tenantId, String userEmail,
                                                  String artifact, String text) throws IOException {
        String artifactMetadata = StringUtils.isNotEmpty(artifact) ? "\n\nRegarding: " + artifact : "";
        String textWithMetadata = text + artifactMetadata;
        sendNotification(template, tenantId, userEmail, textWithMetadata);
    }

    public SlackNotificationResult sendWorkItemInteractiveNotification(
            final String tenantId, final UUID workItemId, final NotificationMode mode, final String text,
            final List<ImmutablePair<String, String>> fileUploads, final List<String> recipients, final String botName,
            final String callbackUrl, Boolean sendSync) throws IOException {
        if((mode == null) || (mode != NotificationMode.SLACK)) {
            throw  new UnsupportedOperationException("NotificationMode {} is not supported!");
        }
        IntegrationKey integrationKey = null;
        try {
            integrationKey = SLACK_INTEGRATION_CACHE.get(tenantId);
            if (BooleanUtils.isTrue(sendSync)) {
                log.info("sendWorkItemInteractiveNotification: Sending slack notifications synchronously for" +
                                " tenant {}, workItemId {}, botName {}, callbackUrl {}, recipients {}", tenantId, workItemId,
                        botName, callbackUrl, recipients);
                final SlackChatInteractiveMessageResult result = slackService.sendWorkItemInteractiveNotification(
                        integrationKey, workItemId, null, text, fileUploads, recipients, botName);
                log.info("sendWorkItemInteractiveNotification: Successfully sent notifications with result {}" +
                                " for tenant {}, workItemId {}, botName {}, callbackUrl {}, recipients {}", result, tenantId,
                        workItemId, botName, callbackUrl, recipients);
                return SlackNotificationResult.builder()
                        .messageResult(result)
                        .build();
            } else {
                String jobId = slackInteractiveIngestionService.sendChatInteractiveMessage(integrationKey, workItemId, null, text, fileUploads, recipients, botName, callbackUrl).getJobId();
                log.info("Successfully submitted notification job id {} for tenant {}, workItemId {}, botName {}, callbackUrl {}, recipients {}", jobId, tenantId, workItemId, botName, callbackUrl, recipients);
                return SlackNotificationResult.builder()
                        .jobId(jobId)
                        .build();
            }
        } catch (ExecutionException | IngestionServiceException e) {
            throw new IOException("Failed to send Slack notification", e);
        }
    }

    public String sendQuestionnaireInteractiveNotification(final String tenantId, final UUID questionnaireId, final NotificationMode mode, final String text,
                                                      final List<String> recipients, final String botName, final String callbackUrl) throws IOException {
        if((mode == null) || (mode != NotificationMode.SLACK)) {
            throw  new UnsupportedOperationException("NotificationMode {} is not supported!");
        }
        IntegrationKey integrationKey = null;
        try {
            integrationKey = SLACK_INTEGRATION_CACHE.get(tenantId);
            String jobId = slackInteractiveIngestionService.sendChatInteractiveMessage(integrationKey, null, questionnaireId, text, null, recipients, botName, callbackUrl).getJobId();
            log.info("Successfully submitted notification job id {} for tenant {}, questionnaireId {}, botName {}, callbackUrl {}, recipients {}", jobId, tenantId, questionnaireId, botName, callbackUrl, recipients);
            return jobId;
        } catch (ExecutionException | IngestionServiceException e) {
            throw new IOException("Failed to send Slack notification", e);
        }
    }

    //ToDo: Move the functionality from Notification Service to its own service
    public String fetchSlackUser(final String tenantId, final UUID workItemNoteId, final String slackUserId, final String callbackUrl) throws IOException {
        IntegrationKey integrationKey = null;
        try {
            integrationKey = SLACK_INTEGRATION_CACHE.get(tenantId);
            String jobId = slackUserIngestionService.fetchUser(integrationKey, workItemNoteId, slackUserId, callbackUrl).getJobId();
            log.info("Successfully submitted fetch slack user job id {} for tenant {}, workItemNoteId {}, slackUserId {}, callbackUrl {}", jobId, tenantId, workItemNoteId, slackUserId, callbackUrl);
            return jobId;
        } catch (ExecutionException | IngestionServiceException e) {
            throw new IOException("Failed to send fetch Slack User", e);
        }
    }

    public NotificationResult sendNotification(final String tenantId, final MessageTemplate template, final String recipientEmail, final Map<String, Object> values) throws IOException {
        Validate.notBlank(recipientEmail, "userEmail cannot be null or empty.");
        return sendNotification(tenantId, template, List.of(recipientEmail), values);
    }

    public NotificationResult sendNotification(final String tenantId, final MessageTemplate template, final List<String> recipients, final Map<String, Object> values) throws IOException {
        try {
            NotificationResultBuilder resultBuilder = NotificationResult.builder()
                    .templateType(template.getType());
            String content = templateService.evaluateTemplate(template.getMessage(), values);
            switch (template.getType()) {
                case EMAIL: {
                    String title = templateService.evaluateTemplate(template.getEmailSubject(), values);
                    EmailContact from = getDefaultEmailContact(tenantId);
                    for (String recipientEmail : recipients) {
                        sendEmailNotification(title, content, from, recipientEmail);
                    }
                    break;
                }
                case MS_TEAMS: {
                    sendMSTeamsNotification(tenantId, content, recipients);
                    break;
                }
                case SLACK: {
                    String bot = templateService.evaluateTemplate(template.getBotName(), values);
                   // String textWithHeader = SLACK_LEVELOPS_BRAND_NAME_HEADER + content;
                    String jobId = sendSlackNotification(tenantId, content, recipients, bot);
                    resultBuilder.jobId(jobId);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported template type: " + template.getType());
            }
            return resultBuilder.build();
        } catch (EmailException | MSTeamsClientException | SQLException e) {
            throw new IOException("Failed to send notification", e);
        }

    }

    public NotificationResult sendNotification(MessageTemplate template, String tenantId, String recipientEmail, String text) throws IOException {
        Validate.notBlank(recipientEmail, "recipientEmail cannot be null or empty.");
        return sendNotification(template, tenantId, List.of(recipientEmail), text);
    }

    public NotificationResult sendNotification(MessageTemplate template, String tenantId, List<String> recipients, String text) throws IOException {
        try {
            NotificationResultBuilder resultBuilder = NotificationResult.builder()
                    .templateType(template.getType());
            switch (template.getType()) {
                case EMAIL: {
                    EmailContact from = getDefaultEmailContact(tenantId);
                    for (String recipientEmail : recipients) {
                        sendEmailNotification(template.getEmailSubject(), text, from, recipientEmail);
                    }
                    break;
                }
                case SLACK: {
                    //String textWithHeader = SLACK_LEVELOPS_BRAND_NAME_HEADER + text;
                    String jobId = sendSlackNotification(tenantId, text, recipients, template.getBotName());
                    resultBuilder.jobId(jobId);
                    break;
                }
                case MS_TEAMS: {
                    sendMSTeamsNotification(tenantId, text, recipients);
                    break;
                }
                default:
                    throw new IllegalArgumentException("Unsupported template type: " + template.getType());
            }
            return resultBuilder.build();
        } catch (EmailException | MSTeamsClientException | SQLException e) {
            throw new IOException("Failed to send notification", e);
        }
    }

    public EmailContact getDefaultEmailContact(String tenantId) throws SQLException, InternalApiClientException {
        DbListResponse<TenantConfig> tenantConfig = null;
        try {
            tenantConfig = tenantConfigClient.get(tenantId, DEFAULT_TENANT_EMAIL_FROM_NAME);
        } catch (Exception e) {
            log.error("Unable to query internal api tenant config for the default tenant email from field");
        }
        String defaultName = tenantId + " Admin";
        if (Objects.nonNull(tenantConfig) && !tenantConfig.getRecords().isEmpty()) {
            defaultName = tenantConfig.getRecords().get(0).getValue();
        }
        return EmailContact.builder()
                .name(defaultName)
                .email(FROM_EMAIL)
                .build();
    }

    public String generatePushMessage(String template, String title, String link, String text,
                                      String sender, String info) {
        return templateService.evaluateTemplate(template, Map.of(
                "title", StringUtils.defaultString(title), // title of the questionnaire
                "link", StringUtils.defaultString(link), // link to the questionnaire
                "text", StringUtils.defaultString(text), // Link to questionnaire in the message or data from bp item
                "sender", StringUtils.defaultString(sender), // admin's email
                "info", StringUtils.defaultString(info)
        ));
    }

    public void sendEmailNotification(String subject, String content, EmailContact from, String userEmail)
            throws EmailException {
        emailService.send(Email.builder()
                .subject(subject)
                .content(content)
                .contentType("text/html")
                .from(from)
                .recipient(userEmail)
                .build());
    }

    private void sendMSTeamsNotification(String tenantId, String text, List<String> recipients) throws MSTeamsClientException {
        IntegrationKey integrationKey = null;
        try {
            integrationKey = MS_TEAMS_INTEGRATION_CACHE.get(tenantId);
            for(String recipientEmail : recipients){
                sendMSTeamsNotification(integrationKey, text, recipientEmail);
            }
        } catch (ExecutionException | MSTeamsClientException e) {
            throw new MSTeamsClientException("Failed to send MS Teams notification", e);
        }
    }

    private void sendMSTeamsNotification(IntegrationKey integrationKey, String text, String recipient) throws MSTeamsClientException {
        msTeamsService.postChatMessage(integrationKey, text, recipient);
    }

    public String sendSlackNotification(String tenantId, String text, List<String> recipients, String botName) throws IOException {
        IntegrationKey integrationKey = null;
        try {
            integrationKey = SLACK_INTEGRATION_CACHE.get(tenantId);
            return sendSlackNotification(integrationKey, text, recipients, botName);
        } catch (ExecutionException | IngestionServiceException e) {
            throw new IOException("Failed to send Slack notification", e);
        }
    }

    private String sendSlackNotification(IntegrationKey integrationKey, String text, List<String> recipients, String botName) throws IngestionServiceException {
        return slackIngestionService.sendChatMessage(integrationKey, text, recipients, botName, null).getJobId();
    }

    private Optional<IntegrationKey> findSlackIntegration(String tenantId) throws InventoryException {
        DbListResponse<Integration> integrations = inventoryService.listIntegrationsByFilters(tenantId,
                "slack", null, null);
        if (integrations == null || CollectionUtils.isEmpty(integrations.getRecords())) {
            return Optional.empty();
        }
        return integrations.getRecords().stream()
                .filter(integration -> "ACTIVE".equalsIgnoreCase(integration.getStatus()))
                .map(integration -> IntegrationKey.builder()
                        .tenantId(tenantId)
                        .integrationId(integration.getId())
                        .build())
                .findAny();
    }

    private Optional<IntegrationKey> findMSTeamsIntegration(String tenantId) throws InventoryException {
        DbListResponse<Integration> integrations = inventoryService.listIntegrationsByFilters(tenantId,
                "ms_teams", null, null);
        if (integrations == null || CollectionUtils.isEmpty(integrations.getRecords())) {
            return Optional.empty();
        }
        return integrations.getRecords().stream()
                .filter(integration -> "ACTIVE".equalsIgnoreCase(integration.getStatus()))
                .map(integration -> IntegrationKey.builder()
                        .tenantId(tenantId)
                        .integrationId(integration.getId())
                        .build())
                .findAny();
    }
}
