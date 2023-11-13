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
public class LegacyVelocityConfigDTO {
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

    @JsonProperty("integration_families")
    private final EnumSet<IntegrationFamilyType> integrationFamilies; //This will be corrected during get/fetch enrich

    @Getter
    public enum EventType {
        JIRA_STATUS("Ticket Status Changed",EnumSet.of(IntegrationFamilyType.ISSUE_MANAGEMENT), true),
        WORKITEM_STATUS("Workitem Status Changed",EnumSet.of(IntegrationFamilyType.ISSUE_MANAGEMENT),true),
        SCM_COMMIT_CREATED("First Commit",EnumSet.of(IntegrationFamilyType.SCM_COMMITS),false,true),
        SCM_PR_CREATED("Pull Request Created",EnumSet.of(IntegrationFamilyType.SCM_PRS),false,true),
        SCM_PR_LABEL_ADDED("Label added to Pull Request",EnumSet.of(IntegrationFamilyType.SCM_PRS),false,true),
        SCM_PR_REVIEW_STARTED("Pull Request Review Started",EnumSet.of(IntegrationFamilyType.SCM_PRS),false,true),
        SCM_PR_APPROVED("Pull Request Approved",EnumSet.of(IntegrationFamilyType.SCM_PRS),false,true),
        SCM_PR_MERGED("Pull Request Merged",EnumSet.of(IntegrationFamilyType.SCM_PRS),false,true),
        DEPLOYMENT_JOB_RUN("Cicd Deployment job run",EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS),false,false,true),
        CICD_JOB_RUN("Cicd Job Run",EnumSet.of(IntegrationFamilyType.CICD_JOB_RUNS),false,false,true);

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
            this(description,integrationFamilies, false,false,false);
        }

        EventType(String description,final EnumSet<IntegrationFamilyType> integrationFamilies, Boolean hasIssueManagement) {
            this(description, integrationFamilies, hasIssueManagement,false,false);
        }

        EventType(String description,final EnumSet<IntegrationFamilyType> integrationFamilies, Boolean hasIssueManagement, Boolean isScmFamily) {
            this(description,integrationFamilies, hasIssueManagement,isScmFamily,false);
        }

        EventType(String description, final EnumSet<IntegrationFamilyType> integrationFamilies, Boolean hasIssueManagement, Boolean isScmFamily,Boolean isCiCdFamily){
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
            if(valueInSeconds <= lowerLimitUnit.toSeconds(lowerLimitValue)) {
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

        public String buildEventDescription(Map<UUID, String> cicdIdAndNameMap) {
            if(EventType.CICD_JOB_RUN.equals(type)) {
                Map<UUID, String> cicdIdAndNameMapSanitized = MapUtils.emptyIfNull(cicdIdAndNameMap);
                List<String> cicdJobNames = CollectionUtils.emptyIfNull(values).stream()
                        .map(UUID::fromString)
                        .map(i -> cicdIdAndNameMapSanitized.getOrDefault(i, null))
                        .filter(StringUtils::isNotBlank)
                        .collect(Collectors.toList());
                return String.format("%s completed %s", type.getDescription(), String.join(",", cicdJobNames));
            } else if(EventType.JIRA_STATUS.equals(type) || EventType.WORKITEM_STATUS.equals(type)) {
                return String.format("%s from %s", type.getDescription(), String.join(",", values));
            } else if (EventType.SCM_PR_LABEL_ADDED.equals(type)) {
                if(isAnyLabelAdded()) {
                    return type.getDescription();
                } else {
                    return String.format("%s label added to Pull Request", String.join(",", values));
                }
            } else {
                return type.getDescription();
            }
        }

        public boolean isAnyLabelAdded() {
            if(type != EventType.SCM_PR_LABEL_ADDED) {
                return false;
            }
            if(MapUtils.isEmpty(params)) {
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
}
