package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykLicenseIssue.SnykLicenseIssueBuilder.class)
public class SnykLicenseIssue {
    @JsonProperty("org_id")
    private final String orgId;
    @JsonProperty("project_id")
    private final String projectId;

    @JsonProperty("id")
    private final String id;
    @JsonProperty("url")
    private final String url;
    @JsonProperty("title")
    private final String title;
    @JsonProperty("type")
    private final String type;
    @JsonProperty("from")
    private final List<String> from;
    @JsonProperty("package")
    private final String packageName;
    @JsonProperty("version")
    private final String version;
    @JsonProperty("severity")
    private final String severity;
    @JsonProperty("language")
    private final String language;

    @JsonProperty("packageManager")
    private final String packageManager;
    @JsonProperty("semver")
    private final SnykSemver semver;

    @JsonProperty("isIgnored")
    private final Boolean ignored;
    @JsonProperty("isPatched")
    private final Boolean patched;

    @JsonProperty("ignored")
    private final List<SnykIgnored> ignoredList;
}
