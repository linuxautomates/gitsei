package io.levelops.ingestion.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
public enum IntegrationType {
    AHA,
    APP_DYNAMICS,
    AWSDEVTOOLS,
    AZURE_DEVOPS(true, true, true),
    AZURE_PIPELINES("azure_", true),
    BITBUCKET_SERVER(false, true),
    BITBUCKET(false, true),
    BLACKDUCK,
    CIRCLECI(false, false, true),
    CONFLUENCE,
    COVERITY,
    CUSTOM,
    CXSAST,
    DATADOG,
    DRONECI(false, false, true),
    HARNESSNG(false, false, true),
    GERRIT(false, true),
    GITHUB(false, true),
    GITHUB_ACTIONS(false, false, true),
    GITLAB(false, true, true),
    HELIX_CORE(false, true),
    HELIX_SWARM(false, true),
    HELIX(false, true),
    JENKINS(false, false, true),
    JIRA(true, "jira_"),
    KNOWBE4,
    MS_TEAMS,
    OKTA,
    PAGERDUTY,
    POSTGRES,
    PROMETHEUS,
    RAPID_7,
    REPORT_MS_TMT,
    REPORT_NCCGROUP,
    REPORT_PRAETORIAN,
    SALESFORCE,
    SAST_BRAKEMAN,
    SERVICENOW(true),
    SLACK,
    SNYK,
    SONARQUBE,
    SPLUNK,
    TENABLE,
    TESTRAILS,
    TRIAGE_RULE,
    ZENDESK("zendesk");

    @Accessors(fluent = true)
    private final Set<IntegrationCategory> categories;
    @Accessors(fluent = true)
    private final Map<IntegrationCategory, String> categoryPrefixMapping;
    @Accessors(fluent = true)
    private final boolean hasIssueManagement;
    @Accessors(fluent = true)
    private final boolean isScmFamily;
    @Accessors(fluent = true)
    private final boolean isCiCdFamily;
    @Accessors(fluent = true)
    private final String requestPrefix;

    IntegrationType() {
        this(false, false, "", false);
    }

    IntegrationType(final String requestPrefix) {
        this(false, false, requestPrefix, false);
    }

    IntegrationType(final String requestPrefix, boolean isCiCdFamily) {
        this(false, false, requestPrefix, isCiCdFamily);
    }


    IntegrationType(boolean hasIssueManagement) {
        this(hasIssueManagement, false, "", false);
    }

    IntegrationType(boolean hasIssueManagement, final String requestPrefix) {
        this(hasIssueManagement, false, requestPrefix, false);
    }

    IntegrationType(boolean hasIssueManagement, boolean isScmFamily) {
        this(hasIssueManagement, isScmFamily, "", false);
    }

    IntegrationType(boolean hasIssueManagement, boolean isScmFamily, boolean isCiCdFamily) {
        this(hasIssueManagement, isScmFamily, "", isCiCdFamily);
    }

    IntegrationType(boolean hasIssueManagement, boolean isScmFamily, final String requestPrefix, boolean isCiCdFamily) {
        this.hasIssueManagement = hasIssueManagement;
        this.isScmFamily = isScmFamily;
        this.requestPrefix = requestPrefix;
        this.isCiCdFamily = isCiCdFamily;
        this.categoryPrefixMapping = hasIssueManagement && isScmFamily 
            ? Map.of(IntegrationCategory.ISSUE_MGMT, requestPrefix, IntegrationCategory.SCM, requestPrefix)
            : hasIssueManagement
                ? Map.of(IntegrationCategory.ISSUE_MGMT, requestPrefix)
                : isScmFamily
                    ? Map.of(IntegrationCategory.SCM, requestPrefix)
                    : Map.of();
        this.categories = hasIssueManagement && isScmFamily
            ? Set.of(IntegrationCategory.ISSUE_MGMT, IntegrationCategory.SCM) 
                : hasIssueManagement
                    ? Set.of(IntegrationCategory.ISSUE_MGMT)
                    : isScmFamily
                        ? Set.of(IntegrationCategory.SCM)
                        : Set.of();
    }

    IntegrationType(IntegrationCategory category, final String requestPrefix) {
        this.hasIssueManagement = category == IntegrationCategory.ISSUE_MGMT;
        this.isScmFamily = category == IntegrationCategory.SCM;
        this.isCiCdFamily = category == IntegrationCategory.CICD;
        this.requestPrefix = requestPrefix;
        this.categoryPrefixMapping = Map.of(category, requestPrefix);
        this.categories = Set.of(category);
    }

    IntegrationType(Map<IntegrationCategory, String> categoryPrefixMappings) {
        this.hasIssueManagement = categoryPrefixMappings.keySet().contains(IntegrationCategory.ISSUE_MGMT);
        this.isScmFamily = categoryPrefixMappings.keySet().contains(IntegrationCategory.SCM);
        this.isCiCdFamily = categoryPrefixMappings.keySet().contains(IntegrationCategory.CICD);
        this.requestPrefix = "";
        this.categoryPrefixMapping = categoryPrefixMappings;
        this.categories = categoryPrefixMappings.keySet();
    }

    @JsonCreator
    @Nullable
    public static IntegrationType fromString(@Nullable String value) {
        return EnumUtils.getEnumIgnoreCase(IntegrationType.class, value);
    }

    public static EnumSet<IntegrationType> getIssueManagementIntegrationTypes() {
        return EnumSet.copyOf(Arrays.stream(IntegrationType.values()).filter(IntegrationType::hasIssueManagement).collect(Collectors.toSet()));
    }

    public static EnumSet<IntegrationType> getSCMIntegrationTypes() {
        return EnumSet.copyOf(Arrays.stream(IntegrationType.values()).filter(IntegrationType::isScmFamily).collect(Collectors.toSet()));
    }

    public static EnumSet<IntegrationType> getCICDIntegrationTypes() {
        return EnumSet.copyOf(Arrays.stream(IntegrationType.values()).filter(IntegrationType::isCiCdFamily).collect(Collectors.toSet()));
    }

    public static EnumSet<IntegrationType> getIntegrationTypesByCategory(final IntegrationCategory category) {
        if(category == null){
            return EnumSet.copyOf(Arrays.stream(IntegrationType.values()).collect(Collectors.toSet()));
        }
        return EnumSet.copyOf(Arrays.stream(IntegrationType.values()).filter(i -> i.categoryPrefixMapping.keySet().contains(category)).collect(Collectors.toSet()));
    }

    public Map<IntegrationCategory, String> getCategoryPrefixMappings(){
        return this.categoryPrefixMapping;
    }

    public Set<String> getPrefixes(){
        return this.categoryPrefixMapping.entrySet().stream().map(e -> e.getValue()).collect(Collectors.toSet());
    }

    public String getRequestPrefix(){
        return this.requestPrefix;
    }

    @JsonValue
    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }
}
