package io.levelops.commons.databases.models.database.snyk;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykLicenseIssue;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import javax.validation.constraints.NotNull;
import java.util.*;

@Value
@Builder(toBuilder = true)
@JsonDeserialize
public class DbSnykIssue {

    @JsonProperty("id")
    String id;

    @JsonProperty("integration_id")
    String integrationId;

    @JsonProperty("org_id")
    String orgId;

    @JsonProperty("org")
    String org;

    @JsonProperty("project_id")
    String projectId;

    @JsonProperty("project_name")
    String projectName;

    @JsonProperty("issue_id")
    String issueId;

    @JsonProperty("url")
    String url;

    @JsonProperty("title")
    String title;

    @JsonProperty("type")
    String type;

    @JsonProperty("package")
    String packageName;

    @JsonProperty("version")
    String version;

    @JsonProperty("severity")
    String severity;

    @JsonProperty("exploit_maturity")
    String exploitMaturity;

    @JsonProperty("upgradable")
    Boolean upgradable;

    @JsonProperty("patchable")
    Boolean patchable;

    @JsonProperty("pinnable")
    Boolean pinnable;

    @JsonProperty("publication_time")
    Date publicationTime;

    @JsonProperty("disclosure_time")
    Date disclosureTime;

    @JsonProperty("language")
    String language;

    @JsonProperty("package_manager")
    String packageManager;

    @JsonProperty("ignored")
    Boolean ignored;

    @JsonProperty("patched")
    Boolean patched;

    @JsonProperty("cvssv3")
    String cvssv3;

    @JsonProperty("cvssv_score")
    Double cvssScore;

    List<SnykVulnerability.Patch> patches;

    @JsonProperty("ingested_at")
    Date ingestedAt;

    public static List<DbSnykIssue> fromIssues(@NotNull SnykIssues snykIssues, String integrationId, Date fetchTime) {
        Date truncatedDate = DateUtils.truncate(fetchTime, Calendar.DATE);
        List<DbSnykIssue> licenses = licensesFromIssues(snykIssues, integrationId, truncatedDate);
        List<DbSnykIssue> vulnerabilities = vulnerabilityFromIssues(snykIssues, integrationId, truncatedDate);
        licenses.addAll(vulnerabilities);
        return licenses;
    }

    private static List<DbSnykIssue> licensesFromIssues(@NotNull SnykIssues snykIssues, String integrationId, Date truncatedDate) {
        List<DbSnykIssue> results = new LinkedList<>();
        for (SnykLicenseIssue license: Optional.ofNullable(snykIssues.getIssues())
                .map(SnykIssues.Issues::getLicenses).orElse(List.of())) {
            results.add(DbSnykIssue.builder()
                    .integrationId(integrationId)
                    .ingestedAt(truncatedDate)
                    .orgId(snykIssues.getOrgId())
                    .org(snykIssues.getOrg())
                    .projectId(snykIssues.getProjectId())
                    .projectName(snykIssues.getProjectName())
                    .issueId(license.getId())
                    .url(license.getUrl())
                    .title(license.getTitle())
                    .type(StringUtils.defaultIfEmpty(license.getType(), "license"))
                    .packageName(license.getPackageName())
                    .version(license.getVersion())
                    .severity(license.getSeverity())
                    .language(license.getLanguage())
                    .packageManager(license.getPackageManager())
                    .ignored(license.getIgnored())
                    .patched(license.getPatched())
                    .build());
        }
        return results;
    }

    private static List<DbSnykIssue> vulnerabilityFromIssues(@NotNull SnykIssues snykIssues, String integrationId, Date truncatedDate) {
        List<DbSnykIssue> results = new LinkedList<>();
        for (SnykVulnerability vulnerability: Optional.ofNullable(snykIssues.getIssues())
                .map(SnykIssues.Issues::getVulnerabilities).orElse(List.of())) {
            results.add(DbSnykIssue.builder()
                    .integrationId(integrationId)
                    .ingestedAt(truncatedDate)
                    .org(snykIssues.getOrg())
                    .orgId(snykIssues.getOrgId())
                    .projectId(snykIssues.getProjectId())
                    .projectName(snykIssues.getProjectName())
                    .issueId(vulnerability.getId())
                    .url(vulnerability.getUrl())
                    .title(vulnerability.getTitle())
                    .type(StringUtils.defaultIfEmpty(vulnerability.getType(), "vuln"))
                    .packageName(vulnerability.getPackageName())
                    .version(vulnerability.getVersion())
                    .severity(vulnerability.getSeverity())
                    .exploitMaturity(vulnerability.getExploitMaturity())
                    .upgradable(vulnerability.getUpgradable())
                    .patchable(vulnerability.getPatchable())
                    .pinnable(vulnerability.getPinnable())
                    .publicationTime(vulnerability.getPublicationTime())
                    .disclosureTime(vulnerability.getDisclosureTime())
                    .language(vulnerability.getLanguage())
                    .packageManager(vulnerability.getPackageManager())
                    .ignored(vulnerability.getIgnored())
                    .patched(vulnerability.getPatched())
                    .cvssv3(vulnerability.getCvssv3())
                    .cvssScore(vulnerability.getCvssScore())
                    .patches(vulnerability.getPatches())
                    .build());
        }
        return results;
    }
}
