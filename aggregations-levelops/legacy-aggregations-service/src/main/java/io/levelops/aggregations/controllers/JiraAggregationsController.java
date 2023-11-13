package io.levelops.aggregations.controllers;

import com.google.cloud.storage.Storage;
import io.levelops.aggregations.helpers.JiraAggHelper;
import io.levelops.aggregations.models.messages.AppAggMessage;
import io.levelops.aggregations.utils.LoggingUtils;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.services.AggregationsDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.ProductMappingService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DbListResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Log4j2
@SuppressWarnings("unused")
public class JiraAggregationsController implements AggregationsController<AppAggMessage> {
    private static final String AGGREGATION_VERSION = "V0.1";

    private final JiraAggHelper helper;
    private final String subscriptionName;
    private final JiraFieldService fieldService;
    private final JiraIssueService issueService;
    private final IntegrationService integrationService;
    private final ControlPlaneService controlPlaneService;
    private final ProductMappingService productMappingService;
    private final TenantConfigService tenantConfigService;

    @Autowired
    public JiraAggregationsController(Storage storage,
                                      JiraAggHelper helper,
                                      JiraIssueService issueService,
                                      JiraFieldService fieldService,
                                      IntegrationService integrationService,
                                      ControlPlaneService controlPlaneService,
                                      @Value("${JIRA_AGG_SUB:dev-jira-sub}") String jiraSub,
                                      AggregationsDatabaseService aggregationsDatabaseService,
                                      ProductMappingService productMappingService, TenantConfigService tenantConfigService) {
        this.helper = helper;
        this.subscriptionName = jiraSub;
        this.issueService = issueService;
        this.fieldService = fieldService;
        this.integrationService = integrationService;
        this.controlPlaneService = controlPlaneService;
        this.productMappingService = productMappingService;
        this.tenantConfigService = tenantConfigService;
    }

    @Override
    @Async("jiraTaskExecutor")
    public void doTask(AppAggMessage message) {
        try {
            LoggingUtils.setupThreadLocalContext(message);
            log.info("Starting work on Jira Agg: messageId={} tenant={} integration={}", message.getMessageId(), message.getCustomer(), message.getIntegrationId());
            Integration it = integrationService.get(
                            message.getCustomer(),
                            message.getIntegrationId())
                    .orElse(null);
            if (it == null ||
                    IntegrationType.JIRA != IntegrationType.fromString(it.getApplication())) {
                return;
            }
            MultipleTriggerResults results = controlPlaneService.getAllTriggerResults(
                    IntegrationKey.builder()
                            .tenantId(message.getCustomer()).integrationId(it.getId()).build(),
                    false,
                    false,
                    true);
            if (!helper.setupJiraSprints(message.getCustomer(), message.getIntegrationId(), results)) {
                log.warn("Failed to setup jira sprints for customer {} integrationId{}",
                        message.getCustomer(), message.getIntegrationId());
            }
            IntegrationConfig config = integrationService.listConfigs(message.getCustomer(), List.of(it.getId()),
                    0, 1).getRecords().stream().findFirst().orElse(null);
            List<DbJiraField> customFields = fieldService.listByFilter(message.getCustomer(),
                    List.of(message.getIntegrationId()),
                    Boolean.TRUE,
                    null,
                    null,
                    null,
                    0,
                    1000000).getRecords();
            Date currentTime = new Date();
            List<String> productIds = productMappingService.getProductIds(message.getCustomer(), message.getIntegrationId());
            Optional<DbJiraField> sprintField = getSprintField(message, config);
            String storyPointField = getStoryPointField(message.getCustomer(), message, config);
            JiraIssueParser.JiraParserConfig parserConfig = JiraIssueParser.JiraParserConfig.builder()
                    .epicLinkField(getEpicField(message, config))
                    .storyPointsField(storyPointField)
                    .sprintFieldKey(sprintField.map(DbJiraField::getFieldKey).orElse(""))
                    .sprintFieldName(sprintField.map(DbJiraField::getName).orElse(""))
                    .dueDateField(getDueDateField(message, config))
                    .customFieldProperties(customFields)
                    .customFieldConfig((config == null || config.getConfig() == null) ? null : config.getConfig().get("agg_custom_fields"))
                    .salesforceConfig((config == null || config.getConfig() == null) ? null : config.getConfig().get("salesforce_fields"))
                    .build();
            DbListResponse<TenantConfig> tenantConfigDbListResponse = tenantConfigService.listByFilter(message.getCustomer(), "HIDE_EXTERNAL_USER_INFO", 0, 1);
            TenantConfig tenantConfig = tenantConfigDbListResponse.getRecords().size() != 0 ? tenantConfigDbListResponse.getRecords().get(0) : null;
            Boolean isEnableForMasking = tenantConfig != null && tenantConfig.getValue() != null && Boolean.valueOf(tenantConfig.getValue());
            if (!helper.setupJiraIssues(
                    message.getCustomer(),
                    currentTime,
                    message.getIntegrationId(),
                    parserConfig,
                    results,
                    productIds,
                    isEnableForMasking)) {
                log.error("Failed to setup jira issues. ending agg. will not clean up old data from db.");
                return;
            }
            //cleanup data older than 91 days.
            log.info("cleaning up data: issues count - {}",
                    issueService.cleanUpOldData(message.getCustomer(),
                            currentTime.toInstant().getEpochSecond(),
                            86400 * 91L));

            log.info("Completed work on Jira Agg for messageId={} ", message.getMessageId());
        } catch (Exception e) {
            log.warn("Could not complete Jira Agg for messageId={}", message.getMessageId(), e);
        } catch (Error e) {
            log.error("Could not complete Jira Agg for messageId={}", message.getMessageId(), e);
            throw e; // this could be OOM, etc.
        } finally {
            LoggingUtils.clearThreadLocalContext();
        }
    }

    @NotNull
    public String getEpicField(AppAggMessage message, IntegrationConfig config) throws SQLException {
        String fieldFromConfig = getFieldKeyFromConfig(message.getCustomer(), message.getIntegrationId(), config, "epic_field");
        if (StringUtils.isNotBlank(fieldFromConfig)) {
            return fieldFromConfig;
        }
        return findFieldByName(message.getCustomer(), message.getIntegrationId(), "Epic Link")
                .map(DbJiraField::getFieldKey)
                .orElse("");
    }

    @NotNull
    private String getDueDateField(AppAggMessage message, IntegrationConfig config) throws SQLException {
        String fieldFromConfig = getFieldKeyFromConfig(message.getCustomer(), message.getIntegrationId(), config, "due_date_field");
        if (StringUtils.isNotBlank(fieldFromConfig)) {
            return fieldFromConfig;
        }
        return findFieldByName(message.getCustomer(), message.getIntegrationId(), "Due Date")
                .map(DbJiraField::getFieldKey)
                .orElse("");
    }

    @NotNull
    private Optional<DbJiraField> getSprintField(AppAggMessage message, IntegrationConfig config) throws SQLException {
        String fieldKey = getFieldKeyFromConfig(message.getCustomer(), message.getIntegrationId(), config, "sprint_field");
        if (StringUtils.isNotBlank(fieldKey)) {
            Optional<DbJiraField> field = findFieldByKey(message.getCustomer(), message.getIntegrationId(), fieldKey);
            if (field.isPresent()) {
                return field;
            }
        }
        return findFieldByName(message.getCustomer(), message.getIntegrationId(), "Sprint");
    }

    @NotNull
    private String getStoryPointField(String company, AppAggMessage message, IntegrationConfig config) throws SQLException {
        String fieldFromConfig = getFieldKeyFromConfig(message.getCustomer(), message.getIntegrationId(), config, "story_points_field");
        if (StringUtils.isNotBlank(fieldFromConfig)) {
            return fieldFromConfig;
        }
        String field = findFieldByName(message.getCustomer(), message.getIntegrationId(), "Story Points")
                .map(DbJiraField::getFieldKey)
                .orElse("");
        if (StringUtils.isNotBlank(field)) {
            updateStoryPointConfig(company, field, message.getIntegrationId(), config);
            return field;
        }
        field = findFieldByName(message.getCustomer(), message.getIntegrationId(), "Story point estimate")
                .map(DbJiraField::getFieldKey)
                .orElse("");
        if (StringUtils.isNotBlank(field)) {
            updateStoryPointConfig(company, field, message.getIntegrationId(), config);
        }
        return field;
    }

    public void updateStoryPointConfig(String company, String field, String integrationId, IntegrationConfig config) {
        try {
            if (StringUtils.isBlank(field)) {
                return;
            }
            if (config == null) {
                config = IntegrationConfig.builder()
                        .integrationId(integrationId)
                        .config(new HashMap<>())
                        .build();
            }
            Map<String, List<IntegrationConfig.ConfigEntry>> configMap = MapUtils.emptyIfNull(config.getConfig());
            if (CollectionUtils.isNotEmpty(configMap.get("story_points_field"))) {
                // if field is already set, we won't overwrite it
                return;
            }
            configMap = new HashMap<>(configMap); // making it mutable
            configMap.put("story_points_field", List.of(IntegrationConfig.ConfigEntry.builder()
                    .name("story points field")
                    .key(field)
                    .build()));
            integrationService.insertConfig(company, config.toBuilder()
                    .config(configMap)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to update story point config for tenant={}", company, e);
        }
    }

    @Nullable
    private String getFieldKeyFromConfig(String customer, String integrationId, @Nullable IntegrationConfig config, String fieldKeyInConfig) throws SQLException {
        return Optional.ofNullable(config)
                .map(IntegrationConfig::getConfig)
                .map(map -> map.get(fieldKeyInConfig))
                .filter(CollectionUtils::isNotEmpty)
                .map(entries -> entries.get(0).getKey())
                .orElse(null);
    }

    @NotNull
    private Optional<DbJiraField> findFieldByName(String customer, String integrationId, String defaultFieldName) throws SQLException {
        return IterableUtils.getFirst(fieldService.listByFilter(customer, List.of(integrationId), true, defaultFieldName, null, List.of(), 0, 1).getRecords());
    }

    @NotNull
    private Optional<DbJiraField> findFieldByKey(String customer, String integrationId, String fieldKey) throws SQLException {
        return IterableUtils.getFirst(fieldService.listByFilter(customer, List.of(integrationId), true, null, null, List.of(fieldKey), 0, 1).getRecords());
    }

    @Override
    public IntegrationType getIntegrationType() {
        return IntegrationType.JIRA;
    }

    @Override
    public Class<AppAggMessage> getMessageType() {
        return AppAggMessage.class;
    }

    @Override
    public String getSubscriptionName() {
        return this.subscriptionName;
    }

}
