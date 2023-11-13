package io.levelops.integrations.snyk.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.snyk.client.SnykClient;
import io.levelops.integrations.snyk.client.SnykClientException;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.models.SnykIssueRevised;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykIssuesData;
import io.levelops.integrations.snyk.models.SnykLicenseIssue;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import io.levelops.integrations.snyk.models.api.SnykApiListProjectsResponse;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class SnykIssueDataSource implements DataSource<SnykIssues, BaseIntegrationQuery> {
    private final SnykClientFactory clientFactory;

    public SnykIssueDataSource(SnykClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private SnykIssues fetchOneProject(IntegrationKey integrationKey) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Data<SnykIssues> fetchOne(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        SnykIssues snykIssues = fetchOneProject(query.getIntegrationKey());
        return BasicData.of(SnykIssues.class, snykIssues);
    }

    String parseScmRepoNamePartial(String projectName) {
        if (StringUtils.isBlank(projectName)) {
            return null;
        }
        String[] split = projectName.split(":");
        if ((split == null) || (split.length != 2)) {
            return null;
        }
        return split[0];
    }

    @Value
    @Builder(toBuilder = true)
    public static class ProjectMetadata {
        String orgId;
        String orgName;
        String projectId;
        String projectRemoteRepoUrl;
        String projectName;
    }

    @Override
    public Stream<Data<SnykIssues>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");

        try {
            SnykClient client = clientFactory.get(query.getIntegrationKey());

            // -- loading list of projects by org (so that we don't hold the connection open for too long)
            List<ProjectMetadata> projectMetadataList = ListUtils.emptyIfNull(client.getOrgs().getOrgs()).stream()
                    .flatMap(currentOrg -> {
                        try {
                            SnykApiListProjectsResponse projectsResponse = client.getProjects(currentOrg.getId());
                            return ListUtils.emptyIfNull(projectsResponse.getProjects()).stream()
                                    .map(currentProject -> ProjectMetadata.builder()
                                            .orgId(currentOrg.getId())
                                            .orgName(currentOrg.getName())
                                            .projectId(currentProject.getId())
                                            .projectRemoteRepoUrl(currentProject.getRemoteRepoUrl())
                                            .projectName(currentProject.getName())
                                            .build());
                        } catch (SnykClientException e) {
                            log.warn("Failed to get Snyk projects for orgId={}", currentOrg.getId());
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());

            // -- lazily stream lists of issues for each project
            return projectMetadataList.stream()
                    .map(projectMetadata -> {
                        SnykIssueRevised issuesResponse;
                        try {
                            issuesResponse = client.getIssues(projectMetadata.getOrgId(), projectMetadata.getProjectId());
                        } catch (SnykClientException e) {
                            log.warn("Failed to get Snyk issues for orgId={}, projectId={}", projectMetadata.getOrgId(), projectMetadata.getProjectId());
                            return null;
                        }
                        if (issuesResponse == null) {
                            return null;
                        }
                        SnykIssues snykIssue = getSnykIssue(issuesResponse);
                        return parseIssues(projectMetadata.getOrgId(), projectMetadata.getProjectId(), projectMetadata.getProjectRemoteRepoUrl(),
                                parseScmRepoNamePartial(projectMetadata.getProjectName()), snykIssue, projectMetadata.getProjectName(),
                                projectMetadata.getOrgName());
                    })
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(SnykIssues.class));
        } catch (SnykClientException e) {
            throw new FetchException("Could not fetch Snyk Issues", e);
        }
    }

    public static SnykIssues parseIssues(final String orgId, final String projectId, final String scmUrl, final String scmRepoNamePartial,
                                         final SnykIssues apiIssues, String projectName, String orgName) {
        return apiIssues.toBuilder()
                .orgId(orgId)
                .projectId(projectId)
                .scmUrl(scmUrl)
                .scmRepoNamePartial(scmRepoNamePartial)
                .projectName(projectName)
                .org(orgName)
                .build();
    }

    private SnykIssues getSnykIssue(SnykIssueRevised issue) {
        return SnykIssues.builder()
                .issues(getSnynkVulnAndLicense(issue))
                .build();
    }

    private SnykIssues.Issues getSnynkVulnAndLicense(SnykIssueRevised issue) {
        return SnykIssues.Issues.builder()
                .vulnerabilities(getSnykVulnerabilityList(issue))
                .licenses(getSnykLicenseList(issue))
                .build();
    }

    private List<SnykLicenseIssue> getSnykLicenseList(SnykIssueRevised issue) {

        return issue.getIssues().stream().filter(i -> "license".equals(i.getIssueType()))
                .map(i -> {
                    SnykIssuesData data = i.getIssueData();
                    return SnykLicenseIssue.builder()
                            .id(data.getId())
                            .orgId(data.getOrgId())
                            .projectId(data.getProjectId())
                            .url(data.getUrl())
                            .title(data.getTitle())
                            .type(data.getType())
                            .from(data.getFrom())
                            .packageName(i.getPackageName())
                            .version(i.getVersion().toString())
                            .severity(data.getSeverity())
                            .language(data.getLanguage())
                            .packageManager(data.getPackageManager())
                            .semver(data.getSemver())
                            .ignored(i.getIgnored())
                            .patched(i.getPatched())
                            .ignoredList(data.getIgnoredList())
                            .build();
                }).collect(Collectors.toList());
    }

    private List<SnykVulnerability> getSnykVulnerabilityList(SnykIssueRevised issue) {

        return issue.getIssues().stream().filter(i -> "vuln".equals(i.getIssueType()))
                .map(i -> {
                    SnykIssuesData data = i.getIssueData();
                    return SnykVulnerability.builder()
                            .id(data.getId())
                            .orgId(data.getOrgId())
                            .projectId(data.getProjectId())
                            .url(data.getUrl())
                            .title(data.getTitle())
                            .type(data.getType())
                            .description(data.getDescription())
                            .from(data.getFrom())
                            .packageName(i.getPackageName())
                            .version(i.getVersion().toString())
                            .severity(data.getSeverity())
                            .exploitMaturity(data.getExploitMaturity())
                            .language(data.getLanguage())
                            .packageManager(data.getPackageManager())
                            .semver(data.getSemver())
                            .publicationTime(data.getPublicationTime())
                            .disclosureTime(data.getDisclosureTime())
                            .upgradable(data.getUpgradable())
                            .patchable(data.getPatchable())
                            .pinnable(data.getPinnable())
                            .identifiers(data.getIdentifiers())
                            .credit(data.getCredit())
                            .cvssv3(data.getCvssv3())
                            .cvssScore(data.getCvssScore())
                            .patches(data.getPatches())
                            .ignored(i.getIgnored())
                            .patched(i.getPatched())
                            .upgradePath(data.getUpgradePath())
                            .ignoredList(data.getIgnoredList())
                            .build();
                }).collect(Collectors.toList());
    }
}
