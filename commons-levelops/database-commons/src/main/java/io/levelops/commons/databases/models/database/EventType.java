package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.commons.models.ComponentType;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Map;

import static io.levelops.commons.databases.models.database.EventType.EventTypeCategory.*;

@Getter
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = EventType.EventTypeBuilder.class)
public enum EventType {
    ALL(ComponentType.UNKNOWN, "", "levelops", null),
    UNKNOWN(ComponentType.UNKNOWN, "", "levelops", null),
    // LEVELOPS COMPONENTS
    SMART_TICKET_CREATED(ComponentType.SMART_TICKET, "smart ticket", "levelops", EventTypeCategory.LEVELOPS_TICKETS),
    SMART_TICKET_UPDATED(ComponentType.SMART_TICKET, "smart ticket", "levelops", EventTypeCategory.LEVELOPS_TICKETS),
    SMART_TICKET_CLOSED(ComponentType.SMART_TICKET, "smart ticket", "levelops", EventTypeCategory.LEVELOPS_TICKETS),
    SMART_TICKET_RESOLVED(ComponentType.SMART_TICKET, "smart ticket", "levelops", EventTypeCategory.LEVELOPS_TICKETS),
    SMART_TICKET_NEW_ASSIGNEE(ComponentType.SMART_TICKET, "smart ticket", "levelops", EventTypeCategory.LEVELOPS_TICKETS),
    SMART_TICKET_NOTIFIED(ComponentType.SMART_TICKET, "smart ticket", "levelops", EventTypeCategory.LEVELOPS_TICKETS),
    ASSESSMENT_CREATED(ComponentType.ASSESSMENT, "assessment", "levelops", EventTypeCategory.LEVELOPS_ASSESSMENTS),
    ASSESSMENT_SUBMITTED(ComponentType.ASSESSMENT, "assessment", "levelops",  EventTypeCategory.LEVELOPS_ASSESSMENTS),
    ASSESSMENT_UPDATED(ComponentType.ASSESSMENT, "assessment", "levelops",  EventTypeCategory.LEVELOPS_ASSESSMENTS),
    ASSESSMENT_NOTIFIED(ComponentType.ASSESSMENT, "assessment", "levelops",  EventTypeCategory.LEVELOPS_ASSESSMENTS),
    // PLUGINS
    PRAETORIAN_REPORT_CREATED(ComponentType.PLUGIN_RESULT, "Praetorian Report", "praetorian", PRAETORIAN),
    PRAETORIAN_REPORT_NEW_ISSUE(ComponentType.PLUGIN_RESULT, "Praetorian Report", "praetorian", PRAETORIAN),
    PRAETORIAN_REPORT_UPDATED(ComponentType.PLUGIN_RESULT, "Praetorian Report", "praetorian", PRAETORIAN),
    PRAETORIAN_REPORT_ISSUE_CLOSED(ComponentType.PLUGIN_RESULT, "Praetorian Report", "praetorian", PRAETORIAN),
    MS_TMT_REPORT_CREATED(ComponentType.PLUGIN_RESULT, "Microsoft Threat Modeling Tool Report", "microsoft", MS_TMT),
    MS_TMT_REPORT_NEW_ISSUE(ComponentType.PLUGIN_RESULT, "Microsoft Threat Modeling Tool Report", "microsoft", MS_TMT),
    MS_TMT_REPORT_UPDATED(ComponentType.PLUGIN_RESULT, "Microsoft Threat Modeling Tool Report", "microsoft", MS_TMT),
    MS_TMT_REPORT_ISSUE_CLOSED(ComponentType.PLUGIN_RESULT, "Microsoft Threat Modeling Tool Report", "microsoft", MS_TMT),
    NCCGROUP_REPORT_CREATED(ComponentType.PLUGIN_RESULT, "NCC Group Report", "ncc", NCCGROUP),
    NCCGROUP_REPORT_NEW_ISSUE(ComponentType.PLUGIN_RESULT, "NCC Group Report", "ncc", NCCGROUP),
    NCCGROUP_REPORT_UPDATED(ComponentType.PLUGIN_RESULT, "NCC Group Report", "ncc", NCCGROUP),
    NCCGROUP_REPORT_ISSUE_CLOSED(ComponentType.PLUGIN_RESULT, "NCC Group Report", "ncc", NCCGROUP),
    CSV_CREATED(ComponentType.PLUGIN_RESULT, "CSV", "levelops", CSV),
    // INTEGRATIONS
    JIRA_NEW_INGESTION(ComponentType.INTEGRATION, "jira", "jira", JIRA),
    JIRA_HISTORICAL_DATA(ComponentType.INTEGRATION, "jira", "jira", JIRA),
    JIRA_HISTORICAL_DATA_SUB_TASK(ComponentType.INTEGRATION, "jira", "jira", JIRA),
    JIRA_ISSUE_CREATED(ComponentType.INTEGRATION, "jira", "jira", JIRA),
    JIRA_ISSUE_UPDATED(ComponentType.INTEGRATION, "jira", "jira", JIRA),
    JIRA_ISSUE_CLOSED(ComponentType.INTEGRATION, "jira", "jira", JIRA),
    COVERITY_NEW_INGESTION(ComponentType.INTEGRATION, "coverity", "coverity", COVERITY),
    COVERITY_DEFECT_CREATED(ComponentType.INTEGRATION, "coverity", "coverity", COVERITY),
    GITHUB_NEW_INGESTION(ComponentType.INTEGRATION, "github", "github", GITHUB),
    GITHUB_NEW_AGGREGATION(ComponentType.INTEGRATION, "github", "github", GITHUB),
    GITHUB_PULL_REQUEST_CREATED(ComponentType.INTEGRATION, "github", "github", GITHUB),
    GITHUB_PULL_REQUEST_CLOSED(ComponentType.INTEGRATION, "github", "github", GITHUB),
    GITHUB_PULL_REQUEST_MERGED(ComponentType.INTEGRATION, "github", "github", GITHUB),
    GITHUB_PULL_REQUEST_UPDATED(ComponentType.INTEGRATION, "github", "github", GITHUB),
    PAGERDUTY_NEW_INGESTION(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    PAGERDUTY_NEW_INCIDENT(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    PAGERDUTY_NEW_ALERT(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    PAGERDUTY_ALERT_RESOLVED(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    PAGERDUTY_ALERT_UPDATED(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    PAGERDUTY_INCIDENT_RESOLVED(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    PAGERDUTY_INCIDENT_ACKNOWLEDGED(ComponentType.INTEGRATION, "pagerduty", "pagerduty", PAGERDUTY),
    SNYK_NEW_INGESTION(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    SNYK_REPORT_CREATED(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    SNYK_NEW_VULNERABILITY(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    SNYK_VULNERABILITY_UPDATED(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    SNYK_VULNERABILITY_CLOSED(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    SNYK_VULNERABILITY_SUPRESSED(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    SNYK_VULNERABILITY_EXPIRED_SUPRESSION(ComponentType.INTEGRATION, "snyk", "snyk", SNYK),
    JENKINS_JOB_RUN_STARTED(ComponentType.PLUGIN_RESULT, "Jenkins Plugin", "jenkins", JENKINS),
    JENKINS_CONFIG_CREATED(ComponentType.PLUGIN_RESULT, "Jenkins Plugin", "jenkins", JENKINS),
    JENKINS_JOB_RUN_FAILED(ComponentType.PLUGIN_RESULT, "Jenkins Plugin", "jenkins", JENKINS),
    SPLUNK_SEARCH_JOB_COMPLETED(ComponentType.INTEGRATION, "splunk", "splunk", SPLUNK),
    GERRIT_NEW_AGGREGATION(ComponentType.INTEGRATION, "gerrit", "gerrit", GERRIT),
    BITBUCKET_NEW_AGGREGATION(ComponentType.INTEGRATION, "bitbucket", "bitbucket", BITBUCKET),
    BITBUCKET_SERVER_NEW_AGGREGATION(ComponentType.INTEGRATION, "Bitbucket Server", "bitbucket_server", BITBUCKET_SERVER),
    GITLAB_NEW_AGGREGATION(ComponentType.INTEGRATION,"gitlab","gitlab", GITLAB),
    DRONECI_NEW_AGGREGATION(ComponentType.INTEGRATION,"droneci","droneci", DRONECI),
    HARNESSNG_NEW_AGGREGATION(ComponentType.INTEGRATION,"harnessng","harnessng", HARNESSNG),
    CIRCLECI_NEW_AGGREGATION(ComponentType.INTEGRATION,"circleci","circleci", CIRCLECI),
    JENKINS_JOB_RUN_COMPLETED(ComponentType.PLUGIN_RESULT, "Jenkins Plugin", "jenkins", JENKINS),
    JENKINS_TRIAGE_RULES_MATCHED(ComponentType.TRIAGE_RULES_MATCHED, "jenkins", "jenkins", JENKINS),
    SONARQUBE_NEW_ISSUE(ComponentType.INTEGRATION, "sonarqube", "sonarqube", SONARQUBE),
    CUSTOM_TRIGGER(ComponentType.CUSTOM, "trigger", "levelops", LEVELOPS), // for customer-initiated events (triggered by external api call)
    HELIX_CORE_NEW_AGGREGATION(ComponentType.INTEGRATION, "Helix Core", "helix_core", HELIX_CORE),
    HELIX_SWARM_NEW_AGGREGATION(ComponentType.INTEGRATION, "Helix Swarm", "helix_swarm", HELIX_SWARM),
    HELIX_NEW_AGGREGATION(ComponentType.INTEGRATION, "Helix", "helix", HELIX),
    AZURE_DEVOPS_NEW_AGGREGATON(ComponentType.INTEGRATION, "azure_devops", "azure_devops", AZURE_DEVOPS),
    CHECKMARX_SAST_NEW_AGGREGATION(ComponentType.INTEGRATION, "cxsast", "cxsast", CHECKMARX_SAST),
    CHECKMARX_SAST_NEW_ISSUE(ComponentType.INTEGRATION, "cxsast", "cxsast", CHECKMARX_SAST);

    @JsonProperty("component")
    private Component component;

    @JsonProperty("description")
    private String description;

    @JsonProperty("data")
    private Map<String, KvField> data;

    @JsonProperty("icon")
    private final String icon; // TODO put this in db?

    /**
     * For UI grouping purposes
     */
    @JsonProperty("category")
    private final EventTypeCategory category; // TODO put this in db?

    public enum EventTypeCategory {
        LEVELOPS("General"),
        LEVELOPS_TICKETS("Issues"),
        LEVELOPS_ASSESSMENTS("Assessments"),
        PRAETORIAN("Praetorian Report"),
        MS_TMT("Microsoft Threat Modeling Tool"),
        NCCGROUP("NCC Group"),
        CSV("CSV"),
        JIRA("Jira"),
        GITHUB("GitHub"),
        PAGERDUTY("PagerDuty"),
        SNYK("Snyk"),
        JENKINS("Jenkins"),
        SPLUNK("Splunk"),
        GERRIT("Gerrit"),
        BITBUCKET("BitBucket"),
        BITBUCKET_SERVER("BitBucket Server"),
        GITLAB("GitLab"),
        DRONECI("DroneCI"),
        HARNESSNG("HarnessNG"),
        CIRCLECI("CircleCI"),
        SONARQUBE("SonarQube"),
        HELIX("Helix"),
        HELIX_CORE("Helix Core"),  // to be removed
        HELIX_SWARM("Helix Swarm"),// to be removed
        AZURE_DEVOPS("Azure Devops"),
        COVERITY("coverity"),
        CHECKMARX_SAST("CheckmarxSAST");

        @Getter
        private final String displayName; // customer visible!

        EventTypeCategory(String displayName) {
            this.displayName = displayName;
        }

        @JsonValue
        public String toString() {
            return super.toString().toLowerCase();
        }

        @Nullable
        @JsonCreator
        public static EventTypeCategory fromString(final String value) {
            return EnumUtils.getEnumIgnoreCase(EventTypeCategory.class, value);
        }
    }

    EventType(final ComponentType type, final String name, String icon, @Nullable EventTypeCategory category) {
        this.component = Component.builder().type(type).name(name).build();
        this.icon = icon;
        this.category = category;
    }

    @Override
    @JsonGetter("type")
    public String toString() {
        return super.toString().toLowerCase();
    }

    @Nullable
    @JsonCreator
    public static EventType fromString(final String eventType) {
        return EnumUtils.getEnumIgnoreCase(EventType.class, eventType);
    }

    public static String getIcon(@Nonnull TriggerType triggerType, @Nullable EventType eventType) {
        if (triggerType == TriggerType.COMPONENT_EVENT && eventType != null) {
            return eventType.getIcon();
        }
        return triggerType.getIcon();
    }

    public static EventTypeBuilder builder() {
        return new EventTypeBuilder();
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class EventTypeBuilder {
        private String type;
        private String description;
        private Component component;
        private Map<String, KvField> data;

        private EventTypeBuilder() {

        }

        @JsonSetter("type")
        public EventTypeBuilder type(String type) {
            this.type = type;
            return this;
        }

        public EventTypeBuilder component(Component component) {
            this.component = component;
            return this;
        }

        @JsonAnySetter
        public EventTypeBuilder any(Map<String, Object> any) {
            return this;
        }

        public EventTypeBuilder description(String description) {
            this.description = description;
            return this;
        }

        public EventTypeBuilder data(Map<String, KvField> data) {
            this.data = data;
            return this;
        }

        public EventType build() {
            EventType eventType = fromString(this.type);
            if (eventType == null) {
                throw new IllegalArgumentException("Invalid event type: " + this.type);
            }
            eventType.data = this.data;
            eventType.description = this.description;
            eventType.component = this.component;
            return eventType;
        }
    }
}