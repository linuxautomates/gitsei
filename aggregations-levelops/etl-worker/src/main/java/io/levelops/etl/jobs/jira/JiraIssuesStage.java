package io.levelops.etl.jobs.jira;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import io.levelops.aggregations_shared.helpers.JiraAggHelperService;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.TenantConfigService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.etl.models.JobMetadata;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.ProductMappingService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.commons.utils.MapUtils;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.integrations.jira.models.JiraIssue;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Log4j2
public class JiraIssuesStage extends BaseIngestionResultProcessingStage<JiraIssue, JiraJobState> {
    private static final String EPIC_STORY_POINTS_CHECKPOINT_KEY = "bulk_update_epic_story_points_offset";
    private final JiraAggHelperService helper;
    private final JiraFieldService fieldService;
    private final JiraIssueService issueService;
    private final IntegrationService integrationService;
    private final TenantConfigService tenantConfigService;
    private final ProductMappingService productMappingService;
    private final ControlPlaneService controlPlaneService;
    private final int threadCount;
    private final boolean enableParallelWhitelist;
    private final List<IntegrationWhitelistEntry> integrationIdWhitelist;
    private final Set<String> disableStoryPointsBulkUpdateForTenants;
    private final int epicStoryPointsReadPageSize;
    private final int epicStoryPointsWritePageSize;

    @Autowired
    public JiraIssuesStage(JiraAggHelperService helper,
                           JiraFieldService fieldService,
                           JiraIssueService issueService,
                           IntegrationService integrationService,
                           TenantConfigService tenantConfigService,
                           ProductMappingService productMappingService,
                           ControlPlaneService controlPlaneService,
                           @Value("${JIRA_ISSUE_STAGE_THREAD_COUNT:5}") final Integer jiraIssueStageThreadCount,
                           @Value("${JIRA_ISSUE_STAGE_PARALLEL_ENABLE_WHITELIST:true}") Boolean enableParallelWhitelist,
                           @Value("${JIRA_ISSUE_STAGE_PARALLEL_INTEGRATION_ID_WHITELIST:}") String integrationIdWhitelist,
                           @Value("${JIRA_ISSUE_STAGE_DISABLE_STORY_POINTS_BULK_UPDATE_FOR_TENANTS:}") String disableStoryPointsBulkUpdateForTenantsString,
                           @Value("${JIRA_ISSUE_STAGE_STORY_POINTS_BULK_UPDATE_READ_PAGE_SIZE:1000}") int epicStoryPointsReadPageSize,
                           @Value("${JIRA_ISSUE_STAGE_STORY_POINTS_BULK_UPDATE_WRITE_PAGE_SIZE:100}") int epicStoryPointsWritePageSize) {
        this.helper = helper;
        this.fieldService = fieldService;
        this.issueService = issueService;
        this.integrationService = integrationService;
        this.tenantConfigService = tenantConfigService;
        this.productMappingService = productMappingService;
        this.controlPlaneService = controlPlaneService;
        this.threadCount = jiraIssueStageThreadCount;
        this.enableParallelWhitelist = enableParallelWhitelist;
        this.integrationIdWhitelist = IntegrationWhitelistEntry.fromCommaSeparatedString(integrationIdWhitelist);
        this.disableStoryPointsBulkUpdateForTenants = CommaListSplitter.splitToSet(disableStoryPointsBulkUpdateForTenantsString);
        this.epicStoryPointsReadPageSize = epicStoryPointsReadPageSize;
        this.epicStoryPointsWritePageSize = epicStoryPointsWritePageSize;
    }

    @Override
    public String getName() {
        return "Jira Issue stage";
    }

    @Override
    public void preStage(JobContext context, JiraJobState state) throws SQLException {
        IntegrationConfig config = integrationService.listConfigs(context.getTenantId(), List.of(context.getIntegrationId()),
                0, 1).getRecords().stream().findFirst().orElse(null);
        List<DbJiraField> customFields = fieldService.listByFilter(context.getTenantId(),
                List.of(context.getIntegrationId()),
                Boolean.TRUE,
                null,
                null,
                null,
                0,
                1000000).getRecords();
        Date currentTime = new Date();
        DbListResponse<TenantConfig> tenantConfigDbListResponse = tenantConfigService.listByFilter(context.getTenantId(), "HIDE_EXTERNAL_USER_INFO", 0, 1);
        TenantConfig tenantConfig = tenantConfigDbListResponse.getRecords().size() != 0 ? tenantConfigDbListResponse.getRecords().get(0) : null;

        log.info("currentTime {}", currentTime);
        String customer = context.getTenantId();
        String integrationId = context.getIntegrationId();
        state.setSprintIdCache(new ConcurrentHashMap<>());
        state.setStatusIdToStatusCategoryCache(CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(statusId -> helper.findStatusCategoryByStatusId(customer, integrationId, statusId))));
        state.setDbSprintLoadingCache(CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(sprintId -> helper.findDbSprintById(customer, integrationId, sprintId))));
        state.setUserIdByDisplayNameCache(CacheBuilder.from("maximumSize=100000")
                .build(CacheLoader.from(displayName -> helper.getUserIdByDisplayName(customer, integrationId, displayName))));
        state.setUserIdByCloudIdCache(CacheBuilder.from("maximumSize=100000")
                .build(CacheLoader.from(cloudId -> helper.getUserIdByCloudId(customer, integrationId, cloudId))));
        state.setCustomFields(customFields);
        state.setProductIds(productMappingService.getProductIds(context.getTenantId(), context.getIntegrationId()));
        state.setSprintField(getSprintField(context.getTenantId(), context.getIntegrationId(), config));
        state.setStoryPointField(getStoryPointField(context.getTenantId(), context.getIntegrationId(), config));
        state.setTenantConfig(tenantConfig);
        state.setEpicField(getEpicField(context.getTenantId(), context.getIntegrationId(), config));
        state.setDueDateField(getDueDateField(context.getTenantId(), context.getIntegrationId(), config));
        state.setCustomFieldConfigs((config == null || config.getConfig() == null) ?
                null : config.getConfig().get("agg_custom_fields"));
        state.setSalesForceFieldConfigs((config == null || config.getConfig() == null) ?
                null : config.getConfig().get("salesforce_fields"));
        state.setIsEnableForMasking(tenantConfig != null && tenantConfig.getValue() != null && Boolean.valueOf(tenantConfig.getValue()));
    }

    @Override
    public void process(JobContext context, JiraJobState state, String ingestionJobId, JiraIssue entity) throws SQLException {
        JobDTO ingestionJobDto = null;
        try {
            ingestionJobDto = context.getIngestionJobDto(ingestionJobId, controlPlaneService);
        } catch (IngestionServiceException e) {
            throw new RuntimeException(e);
        }
        helper.processJiraIssue(
                context.getTenantId(),
                context.getIntegrationId(),
                ingestionJobDto,
                entity,
                context.getJobScheduledStartTime(),
                JiraIssueParser.JiraParserConfig.builder()
                        .epicLinkField(state.getEpicField())
                        .storyPointsField(state.getStoryPointField())
                        .sprintFieldKey(state.getSprintField().map(DbJiraField::getFieldKey).orElse(""))
                        .sprintFieldName(state.getSprintField().map(DbJiraField::getName).orElse(""))
                        .dueDateField(state.getDueDateField())
                        .customFieldProperties(state.getCustomFields())
                        .customFieldConfig(state.getCustomFieldConfigs())
                        .salesforceConfig(state.getSalesForceFieldConfigs())
                        .build(),
                state.getConfigVersion(),
                context.getReprocessingRequested(),
                state.getProductIds(),
                state.getSprintIdCache(),
                state.getDbSprintLoadingCache(),
                state.getStatusIdToStatusCategoryCache(),
                state.getUserIdByDisplayNameCache(),
                state.getUserIdByCloudIdCache(),
                state.getIsEnableForMasking());
    }

    @Override
    public void postStage(JobContext context, JiraJobState jobState) {
        if (disableStoryPointsBulkUpdateForTenants.contains(context.getTenantId())) {
            log.info("Skipping story points bulk update for tenant={}", context.getTenantId());
        } else {
            try {
                bulkUpdateEpicStoryPoints(context);
            } catch (SQLException e) {
                log.warn("Failed to update story points for parents.", e);
            }
        }
    }

    protected void bulkUpdateEpicStoryPoints(JobContext context) throws SQLException {
        Long ingestedAt = DateUtils.truncate(context.getJobScheduledStartTime(), Calendar.DATE);

        JobMetadata metadata = context.getMetadata();
        Map<String, Object> checkpoint = MapUtils.emptyIfNull(metadata.getCheckpoint());
        int startingOffset = (Integer) checkpoint.getOrDefault(EPIC_STORY_POINTS_CHECKPOINT_KEY, 0);

        int offset;
        boolean hasMore;
        for (offset = startingOffset, hasMore = true; hasMore; offset += epicStoryPointsReadPageSize) {
            // update checkpoint data
            checkpoint = MapUtils.append(checkpoint, EPIC_STORY_POINTS_CHECKPOINT_KEY, offset);
            context.setMetadata(context.getMetadata().toBuilder()
                    .checkpoint(checkpoint)
                    .build());

            hasMore = issueService.bulkUpdateEpicStoryPointsSinglePage(context.getTenantId(), context.getIntegrationId(),
                    ingestedAt, epicStoryPointsReadPageSize, offset, epicStoryPointsWritePageSize);
        }
    }

    @Override
    public String getDataTypeName() {
        return "issues";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public boolean allowParallelProcessing(String tenantId, String integrationId) {
        if (enableParallelWhitelist) {
            return integrationIdWhitelist.contains(IntegrationWhitelistEntry.builder()
                    .tenantId(tenantId)
                    .integrationId(integrationId)
                    .build());
        }
        return true;
    }

    @Override
    public int getParallelProcessingThreadCount() {
        return threadCount;
    }

    // region helpers

    @NotNull
    private Optional<DbJiraField> getSprintField(String tenantId, String integrationId, IntegrationConfig config) throws SQLException {
        String fieldKey = getFieldKeyFromConfig(tenantId, integrationId, config, "sprint_field");
        if (StringUtils.isNotBlank(fieldKey)) {
            Optional<DbJiraField> field = findFieldByKey(tenantId, integrationId, fieldKey);
            if (field.isPresent()) {
                return field;
            }
        }
        return findFieldByName(tenantId, integrationId, "Sprint");
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
    private Optional<DbJiraField> findFieldByKey(String customer, String integrationId, String fieldKey) throws SQLException {
        return IterableUtils.getFirst(fieldService.listByFilter(customer, List.of(integrationId), true, null, null, List.of(fieldKey), 0, 1).getRecords());
    }

    @NotNull
    private Optional<DbJiraField> findFieldByName(String customer, String integrationId, String defaultFieldName) throws SQLException {
        return IterableUtils.getFirst(fieldService.listByFilter(customer, List.of(integrationId), true, defaultFieldName, null, List.of(), 0, 1).getRecords());
    }

    @NotNull
    private String getStoryPointField(String tenantId, String integrationId, IntegrationConfig config) throws SQLException {
        String fieldFromConfig = getFieldKeyFromConfig(tenantId, integrationId, config, "story_points_field");
        if (StringUtils.isNotBlank(fieldFromConfig)) {
            return fieldFromConfig;
        }
        String field = findFieldByName(tenantId, integrationId, "Story Points")
                .map(DbJiraField::getFieldKey)
                .orElse("");
        if (StringUtils.isNotBlank(field)) {
            updateStoryPointConfig(tenantId, field, integrationId, config);
            return field;
        }
        field = findFieldByName(tenantId, integrationId, "Story point estimate")
                .map(DbJiraField::getFieldKey)
                .orElse("");
        if (StringUtils.isNotBlank(field)) {
            updateStoryPointConfig(tenantId, field, integrationId, config);
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

    @NotNull
    public String getEpicField(String tenantId, String integrationId, IntegrationConfig config) throws SQLException {
        String fieldFromConfig = getFieldKeyFromConfig(tenantId, integrationId, config, "epic_field");
        if (StringUtils.isNotBlank(fieldFromConfig)) {
            return fieldFromConfig;
        }
        return findFieldByName(tenantId, integrationId, "Epic Link")
                .map(DbJiraField::getFieldKey)
                .orElse("");
    }

    @NotNull
    private String getDueDateField(String tenantId, String integrationId, IntegrationConfig config) throws SQLException {
        String fieldFromConfig = getFieldKeyFromConfig(tenantId, integrationId, config, "due_date_field");
        if (StringUtils.isNotBlank(fieldFromConfig)) {
            return fieldFromConfig;
        }
        return findFieldByName(tenantId, integrationId, "Due Date")
                .map(DbJiraField::getFieldKey)
                .orElse("");
    }

    // endregion
}