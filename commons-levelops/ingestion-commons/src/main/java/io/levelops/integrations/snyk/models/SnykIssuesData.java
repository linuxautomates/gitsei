package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykIssuesData.SnykIssuesDataBuilder.class)
public class SnykIssuesData {
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
    @JsonProperty("description")
    private String description;
    @JsonProperty("from")
    private final List<String> from;
    @JsonProperty("package")
    private final String packageName;
    @JsonProperty("version")
    private final String version;
    @JsonProperty("severity")
    private String severity;
    @JsonProperty("exploitMaturity")
    private final String exploitMaturity;
    @JsonProperty("language")
    private final String language;
    @JsonProperty("packageManager")
    private final String packageManager;
    @JsonProperty("semver")
    private final SnykSemver semver;
    @JsonProperty("publicationTime")
    private final Date publicationTime;
    @JsonProperty("disclosureTime")
    private final Date disclosureTime;
    @JsonProperty("isUpgradable")
    private final Boolean upgradable;
    @JsonProperty("isPatchable")
    private final Boolean patchable;
    @JsonProperty("isPinnable")
    private final Boolean pinnable;
    @JsonProperty("identifiers")
    private final SnykVulnerability.Identifiers identifiers;
    @JsonProperty("credit")
    private final List<String> credit;
    @JsonProperty("CVSSv3")
    private final String cvssv3;
    @JsonProperty("cvssScore")
    private final Double cvssScore;

    @JsonProperty("patches")
    private final List<SnykVulnerability.Patch> patches;

    @JsonProperty("isIgnored")
    private final Boolean ignored;
    @JsonProperty("isPatched")
    private final Boolean patched;
    @JsonProperty("upgradePath")
    private final List<String> upgradePath;

    @JsonProperty("ignored")
    private final List<SnykIgnored> ignoredList;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SnykVulnerability.Identifiers.IdentifiersBuilder.class)
    public static final class Identifiers {
        @JsonProperty("CVE")
        List<String> cve;
        @JsonProperty("CWE")
        List<String> cwe;
        @JsonProperty("ALTERNATIVE")
        List<String> alternative;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = SnykVulnerability.Patch.PatchBuilder.class)
    public static final class Patch {
        @JsonProperty("id")
        private final String id;
        @JsonProperty("urls")
        private final List<String> urls;
        @JsonProperty("version")
        private final String version;
        @JsonProperty("comments")
        private final List<String> comments;
        @JsonProperty("modificationTime")
        private final Date modificationTime;
    }
}
