package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykIssues.SnykIssuesBuilder.class)
public class SnykIssues {
    @JsonProperty("org_id")
    private final String orgId;
    @JsonProperty("org")
    String org;
    @JsonProperty("project_id")
    private final String projectId;
    @JsonProperty("project_name")
    String projectName;
    @JsonProperty("scm_url")
    private final String scmUrl;
    @JsonProperty("scm_repo_name_partial")
    private final String scmRepoNamePartial;
    @JsonProperty("ok")
    private final Boolean ok;
    @JsonProperty("issues")
    private Issues issues;
    @JsonProperty("dependencyCount")
    private final Integer dependencyCount;
    @JsonProperty("packageManager")
    private final String packageManager;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Issues.IssuesBuilder.class)
    public static final class Issues {
        @JsonProperty("vulnerabilities")
        private final List<SnykVulnerability> vulnerabilities;
        @JsonProperty("licenses")
        private final List<SnykLicenseIssue> licenses;
    }
}