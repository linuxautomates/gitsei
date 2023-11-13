package io.levelops.etl.jobs.jira;

import com.google.common.cache.LoadingCache;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.TenantConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
@Setter
public class JiraJobState {
    // JiraIssuesStage
    private Map<Integer, Long> sprintIdCache;
    private LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache;
    private LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache;
    private LoadingCache<String, Optional<String>> userIdByDisplayNameCache;
    private LoadingCache<String, Optional<String>> userIdByCloudIdCache;
    private List<DbJiraField> customFields;
    private List<String> productIds;
    private Optional<DbJiraField> sprintField;
    private String storyPointField;
    private String epicField;
    private String dueDateField;
    private TenantConfig tenantConfig;
    private Long configVersion;
    private List<IntegrationConfig.ConfigEntry> customFieldConfigs;
    private List<IntegrationConfig.ConfigEntry> salesForceFieldConfigs;
    private Boolean isEnableForMasking;

    // JiraProjectStage
    private List<DbJiraProject> jiraProjects;

    // JiraFieldsStage
    Map<String, DbJiraField>  existingFields;
    List<DbJiraField> newFields;
}
