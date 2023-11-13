package io.levelops.commons.databases.models.database.velocity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.models.IntegrationFamilyType;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.Accessors;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = VelocityConfigDTO.VelocityConfigDTOBuilder.class)
public class VelocityConfigDTO {
    @JsonProperty("id")
    private final UUID id; //This will be corrected during get/fetch enrich
    @JsonProperty("name")
    private final String name;
    @JsonProperty("default_config")
    private final Boolean defaultConfig;
    @JsonProperty("description")
    private final String description;

    @JsonProperty("created_at")
    Instant createdAt; //This will be corrected during get/fetch enrich

    @JsonProperty("updated_at")
    Instant updatedAt; //This will be corrected during get/fetch enrich

    @JsonProperty("scm_config")
    private final ScmConfig scmConfig;

    @JsonProperty("pre_development_custom_stages")
    private final List<Stage> preDevelopmentCustomStages;

    @JsonProperty("fixed_stages")
    private final List<Stage> fixedStages;

    @JsonProperty("post_development_custom_stages")
    private final List<Stage> postDevelopmentCustomStages;

    @JsonProperty("cicd_job_id_name_mappings")
    private final Map<UUID, String> cicdJobIdNameMappings; //This will be corrected during get/fetch enrich

    /*
    For calculation ticket_velocity, this field is ignored.
    For calculation pr_velocity, if starting_event_is_commit_created is true, then starting event for Lead Time will be commit created
    For calculation pr_velocity, if starting_event_is_commit_created is NOT true, then starting event for Lead Time will be issue created
    */
    @JsonProperty("starting_event_is_commit_created")
    private final Boolean startingEventIsCommitCreated;

    /*
    For calculation pr_velocity, this field is ignored.
    For calculation ticket_velocity, if starting_event_is_generic_event is true, then starting event for Lead Time will be first event from generic event associated with Jira/WI
    For calculation ticket_velocity, if starting_event_is_generic_event is NOT true, then starting event for Lead Time will be issue created
    */
    @JsonProperty("starting_event_is_generic_event")
    private final Boolean startingEventIsGenericEvent;

    @JsonProperty("starting_generic_event_types")
    private final List<String> startingGenericEventTypes;

    /*
    Ideally we need field to identify just jira vs non jira issues.
    But the requirement needs it to be a dropdown not radio button, e.g. Jira, Azure DevOps, Rally. ¯\_(ツ)_/¯
    At this point the user will select only one, but ideally user could select more than one non jira. e.g. selecting Azure Devops & Rally should work together.
     */
    @JsonProperty("issue_management_integrations")
    private final EnumSet<IntegrationType> issueManagementIntegrations;

    @JsonProperty("jira_only")
    private final boolean isJiraOnly;

    @JsonProperty("integration_families")
    private final EnumSet<IntegrationFamilyType> integrationFamilies; //This will be corrected during get/fetch enrich

    @JsonProperty("lead_time_for_changes")
    private final LeadTimeForChange leadTimeForChanges;

    @JsonProperty("mean_time_to_restore")
    private final LeadTimeForChange meanTimeToRestore;

    @JsonProperty("deployment_frequency")
    private final DeploymentFrequency deploymentFrequency;

    @JsonProperty("change_failure_rate")
    private final ChangeFailureRate changeFailureRate;

    @JsonProperty("associated_ou_ref_ids")
    List<String> associatedOURefIds;

    @Getter
    public enum EventType {
        JIRA_STATUS("Ticket Status Changed", EnumSet.of(IntegrationFamilyType.ISSUE_MANAGEMENT), true),
        WORKITEM_STATUS("Workitem Status Changed", EnumSet.of(IntegrationFamilyType.ISSUE_MANAGEMENT), true),
        JIRA_RELEASE("Jira Release stage", EnumSet.of(IntegrationFamilyType.ISSUE_MANAGEMENT),false),
        SCM_COMMIT_CREATED("First Commit", EnumSet.of(IntegrationFamilyType.SCM_COMMITS), false, true),
        SCM_PR_CREATED("Pull Request Created", EnumSet.of(IntegrationFamilyType.SCM_PRS), false, true),
        SCM_PR_LABEL_ADDED("Label added to Pull Request", EnumSet.of(IntegrationFamilyType.SCM_PRS), false, true),
        SCM_PR_REVIEW_STARTED("Pull Request Review Started", EnumSet.of(IntegrationFamilyType.SCM_PRS), false, true),
        SCM_PR_APPROVED("Pull Request Approved", EnumSet.of(IntegrationFamilyType.SCM_PRS), false, true),
        SCM_PR_MERGED("Pull Request Merged", EnumSet.of(IntegrationFamilyType.SCM_PRS), false, true),
        DEPLOYMENT_JOB_RUN("Cicd Deployment job run", EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS), false, false, true),
        CICD_JOB_RUN("Cicd Job Run", EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS), false, false, true),
        HARNESSCI_JOB_RUN("Harness CI Job Run", EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS), false, false, true),
        HARNESSCD_JOB_RUN("Harness CD Job Run", EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS), false, false, true),
        GITHUB_ACTIONS_JOB_RUN("Github Actions Job Run", EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS), false, false, true),
        SCM_PR_SOURCE_BRANCH("Pull Request Created from source branch", EnumSet.of(IntegrationFamilyType.SCM_PRS), false, true);

        @JsonProperty("description")
        private final String description;

        @JsonProperty("integration_families")
        @Accessors(fluent = true)
        private final EnumSet<IntegrationFamilyType> integrationFamilies;

        @Accessors(fluent = true)
        private final boolean hasIssueManagement;

        @Accessors(fluent = true)
        private final boolean isScmFamily;

        @Accessors(fluent = true)
        private final boolean isCiCdFamily;

        EventType(String description, final EnumSet<IntegrationFamilyType> integrationFamilies) {
            this(description, integrationFamilies, false, false, false);
        }

        EventType(String description, final EnumSet<IntegrationFamilyType> integrationFamilies, Boolean hasIssueManagement) {
            this(description, integrationFamilies, hasIssueManagement, false, false);
        }

        EventType(String description, final EnumSet<IntegrationFamilyType> integrationFamilies, Boolean hasIssueManagement, Boolean isScmFamily) {
            this(description, integrationFamilies, hasIssueManagement, isScmFamily, false);
        }

        EventType(String description, final EnumSet<IntegrationFamilyType> integrationFamilies, Boolean hasIssueManagement, Boolean isScmFamily, Boolean isCiCdFamily) {
            this.description = description;
            this.integrationFamilies = integrationFamilies;
            this.hasIssueManagement = hasIssueManagement;
            this.isScmFamily = isScmFamily;
            this.isCiCdFamily = isCiCdFamily;
        }

        public static EventType fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(EventType.class, st);
        }

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    @Getter
    public enum Rating {
        GOOD("Good"),
        NEEDS_ATTENTION("Needs_Attention"),
        SLOW("Slow"),
        MISSING("Missing");

        private final String displayText;

        Rating(String displayText) {
            this.displayText = displayText;
        }

        public static Rating fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(Rating.class, st);
        }

        @Override
        public String toString() {
            return this.getDisplayText();
        }

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Stage.StageBuilder.class)
    public static final class Stage {
        @JsonProperty("name")
        private final String name;

        @JsonProperty("description")
        private final String description;

        @JsonProperty("order")
        private final Integer order;

        @JsonProperty("event")
        private final Event event;

        @JsonProperty("filter")
        Map<String, Object> filter;

        @JsonProperty("lower_limit_value")
        private final Long lowerLimitValue;

        @JsonProperty("lower_limit_unit")
        private final TimeUnit lowerLimitUnit;

        @JsonProperty("upper_limit_value")
        private final Long upperLimitValue;

        @JsonProperty("upper_limit_unit")
        private final TimeUnit upperLimitUnit;

        public Rating calculateRating(Long valueInSeconds) {
            if (valueInSeconds == null) {
                return Rating.MISSING;
            }
            if (valueInSeconds <= lowerLimitUnit.toSeconds(lowerLimitValue)) {
                return Rating.GOOD;
            } else if (valueInSeconds <= upperLimitUnit.toSeconds(upperLimitValue)) {
                return Rating.NEEDS_ATTENTION;
            } else {
                return Rating.SLOW;
            }
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Event.EventBuilder.class)
    public static final class Event {
        @JsonProperty("type")
        private final EventType type;

        @JsonProperty("values")
        private final List<String> values;

        @JsonProperty("params")
        private final Map<String, List<String>> params;

        @JsonProperty("scm_filters")
        Map<String, Map<String, List<String>>> scmFilters;

        public String buildEventDescription(Map<UUID, String> cicdIdAndNameMap) {
            if (EventType.CICD_JOB_RUN.equals(type)) {
                Map<UUID, String> cicdIdAndNameMapSanitized = MapUtils.emptyIfNull(cicdIdAndNameMap);
                List<String> cicdJobNames = CollectionUtils.emptyIfNull(values).stream()
                        .map(UUID::fromString)
                        .map(i -> cicdIdAndNameMapSanitized.getOrDefault(i, null))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
                return String.format("%s completed %s", type.getDescription(), String.join(",", cicdJobNames));
            } else if (EventType.JIRA_STATUS.equals(type) || EventType.WORKITEM_STATUS.equals(type)) {
                return String.format("%s from %s", type.getDescription(), String.join(",", values));
            } else if (EventType.SCM_PR_LABEL_ADDED.equals(type)) {
                if (isAnyLabelAdded()) {
                    return type.getDescription();
                } else {
                    return String.format("%s label added to Pull Request", String.join(",", values));
                }
            } else {
                return type.getDescription();
            }
        }

        public boolean isAnyLabelAdded() {
            if (type != EventType.SCM_PR_LABEL_ADDED) {
                return false;
            }
            if (MapUtils.isEmpty(params)) {
                return false;
            }
            Set<String> values = CollectionUtils.emptyIfNull(params.getOrDefault("any_label_added", null)).stream().collect(Collectors.toSet());
            return values.contains("true");
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ScmConfig.ScmConfigBuilder.class)
    public static class ScmConfig {

        @JsonProperty("release")
        Map<Field, Map<String, List<String>>> release;

        @JsonProperty("deployment")
        Map<Field, Map<String, List<String>>> deployment;

        @JsonProperty("hotfix")
        Map<Field, Map<String, List<String>>> hotfix;

        @JsonProperty("defect")
        Map<Field, Map<String, List<String>>> defect;

        public enum Field {
            target_branch,
            source_branch,
            commit_branch,
            tags,
            labels
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = LeadTimeForChange.LeadTimeForChangeBuilder.class)
    public static class LeadTimeForChange {

        @JsonProperty("pre_development_custom_stages")
        List<Stage> preDevelopmentCustomStages;

        @JsonProperty("fixed_stages")
        List<Stage> fixedStages;

        @JsonProperty("post_development_custom_stages")
        List<Stage> postDevelopmentCustomStages;

        @JsonProperty("issue_management_integrations")
        private final EnumSet<IntegrationType> issueManagementIntegrations;

        @JsonProperty("starting_event_is_commit_created")
        Boolean startingEventIsCommitCreated;

        @JsonProperty("starting_event_is_generic_event")
        Boolean startingEventIsGenericEvent;

        @JsonProperty("starting_generic_event_types")
        List<String> startingGenericEventTypes;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DeploymentFrequency.DeploymentFrequencyBuilder.class)
    public static class DeploymentFrequency {

        @JsonProperty("application")
        String application;

        @JsonProperty("integration_id")
        Integer integrationId;

        @JsonProperty("integration_ids")
        List<Integer> integrationIds;

        @JsonProperty("calculation_field")
        CalculationField calculationField;

        @JsonProperty("filters")
        VelocityConfigFilters velocityConfigFilters;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ChangeFailureRate.ChangeFailureRateBuilder.class)
    public static class ChangeFailureRate {

        @JsonProperty("application")
        String application;

        @JsonProperty("integration_id")
        Integer integrationId;

        @JsonProperty("integration_ids")
        List<Integer> integrationIds;

        @JsonProperty("is_absolute")
        Boolean isAbsoulte;

        @JsonProperty("calculation_field")
        CalculationField calculationField;

        @JsonProperty("filters")
        VelocityConfigFilters velocityConfigFilters;

    }

    @Getter
    public enum DeploymentRoute {
        pr,
        commit
    }

    @Getter
    public enum DeploymentCriteria {
        pr_merged, pr_closed,
        pr_merged_closed,

        commit_merged_to_branch,
        commit_with_tag,
        commit_merged_to_branch_with_tag
    }

    @Getter
    public enum CalculationField {
        // SCM
        pr_merged_at,
        pr_closed_at,
        commit_pushed_at,
        committed_at,
        tag_added_at,

        // CICD
        end_time,
        start_time,

        // Jira
        issue_resolved_at,
        issue_updated_at,
        released_in,
        // ADO
        workitem_updated_at,
        workitem_resolved_at
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = VelocityConfigFilters.VelocityConfigFiltersBuilder.class)
    public static class VelocityConfigFilters {

        @JsonProperty("failed_deployment")
        FilterTypes failedDeployment;

        @JsonProperty("total_deployment")
        FilterTypes totalDeployment;

        @JsonProperty("deployment_frequency")
        FilterTypes deploymentFrequency;

        @JsonProperty("mean_time_to_restore")
        FilterTypes meanTimeToRestore;

        @JsonProperty("lead_time_for_changes")
        FilterTypes leadTimeToChanges;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FilterTypes.FilterTypesBuilder.class)
    public static class FilterTypes {

        @JsonProperty("integration_type")
        private final String integrationType;

        @JsonProperty("deployment_route")
        DeploymentRoute deploymentRoute;

        @JsonProperty("deployment_criteria")
        DeploymentCriteria deploymentCriteria;

        @JsonProperty("calculation_field")
        CalculationField calculationField;

        @JsonProperty("scm_filters")
        Map<String, Map<String, List<String>>> scmFilters;

        @JsonProperty("event")
        Event event;

        @JsonProperty("filter")
        Map<String, Object> filter;

        @JsonProperty("is_ci_job")
        Boolean isCiJob;

        @JsonProperty("is_cd_job")
        Boolean isCdJob;
    }

    @JsonProperty("is_new")
    private Boolean isNew;
}
