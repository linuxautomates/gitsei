package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.jira.models.JiraCreateIssueQuery;
import io.levelops.ingestion.integrations.jira.models.JiraCreateIssueResult;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraComponent;
import io.levelops.integrations.jira.models.JiraCreateIssueFields;
import io.levelops.integrations.jira.models.JiraDeploymentType;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraIssueType;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraServerInfo;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraVersion;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class JiraCreateIssueController implements DataController<JiraCreateIssueQuery> {

    private final ObjectMapper objectMapper;
    private final JiraClientFactory jiraClientFactory;

    @Builder
    public JiraCreateIssueController(ObjectMapper objectMapper,
                                     JiraClientFactory jiraClientFactory) {
        this.objectMapper = objectMapper;
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, JiraCreateIssueQuery query) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notBlank(query.getIssueTypeName(), "query.getIssueTypeName() cannot be null or empty.");
        Validate.notBlank(query.getProjectKey(), "query.getProjectKey() cannot be null or empty.");

        try {
            JiraClient jiraClient = jiraClientFactory.get(query.getIntegrationKey());

            JiraServerInfo serverInfo = jiraClient.getServerInfo();
            JiraDeploymentType deploymentType = Optional.ofNullable(serverInfo).map(JiraServerInfo::getDeploymentType).orElse(JiraDeploymentType.CLOUD);

            JiraProject project = jiraClient.getProject(query.getProjectKey());

            List<JiraUser> searchResults = Collections.emptyList();
            if (StringUtils.isNotBlank(query.getAssigneeSearchString())) {
                searchResults = ListUtils.emptyIfNull(jiraClient.searchUsers(query.getAssigneeSearchString(), deploymentType));
            }

            JiraUser assignee = getJiraUser(deploymentType, searchResults);
            JiraIssueType issueType = getIssueType(project, query);
            List<JiraComponent> components = getComponents(project, query);
            List<JiraVersion> versions = getVersions(jiraClient, project, query.getVersions());
            List<JiraVersion> fixVersions = getVersions(jiraClient, project, query.getFixVersions());

            JiraCreateIssueFields.JiraCreateIssueFieldsBuilder fields = JiraCreateIssueFields.builder()
                    .summary(query.getSummary())
                    .description(query.getDescription())
                    .project(JiraProject.builder()
                            .id(project.getId())
                            .build())
                    .issueType(JiraIssueType.builder()
                            .id(issueType.getId())
                            .build())
                    .assignee(assignee)
                    .labels(CollectionUtils.isNotEmpty(query.getLabels()) ? query.getLabels() : null)
                    .components(CollectionUtils.isNotEmpty(components) ? components : null)
                    .versions(CollectionUtils.isNotEmpty(versions) ? versions : null)
                    .fixVersions(CollectionUtils.isNotEmpty(fixVersions) ? fixVersions : null)
                    .priority(StringUtils.isNotBlank(query.getPriority()) ? JiraIssueFields.JiraPriority.builder().name(StringUtils.trim(query.getPriority())).build() : null);

            if (MapUtils.isNotEmpty(query.getCustomFields())) {
                query.getCustomFields().forEach(fields::setCustomField);
            }

            JiraIssue issue = jiraClient.createIssue(fields.build());

            return JiraCreateIssueResult.builder()
                    .id(issue.getId())
                    .key(issue.getKey())
                    .build();
        } catch (JiraClientException e) {
            throw new IngestException("Failed to create Jira issue for integration=" + query.getIntegrationKey(), e);
        }
    }

    private JiraIssueType getIssueType(JiraProject project, JiraCreateIssueQuery query) throws IngestException {
        return ListUtils.emptyIfNull(project.getIssueTypes()).stream()
                .filter(Objects::nonNull)
                .filter(jiraIssueType -> query.getIssueTypeName().equalsIgnoreCase(jiraIssueType.getName()))
                .findAny()
                .orElseThrow(() -> new IngestException("Could not find issue type named '" + query.getIssueTypeName() + "' in project " + query.getProjectKey()));
    }

    private JiraUser getJiraUser(JiraDeploymentType deploymentType, List<JiraUser> searchResults) {
        if (deploymentType == JiraDeploymentType.SERVER) {
            return searchResults.stream()
                    .filter(Objects::nonNull)
                    .map(JiraUser::getName)
                    .filter(StringUtils::isNotBlank)
                    .map(name -> JiraUser.builder()
                            .name(name)
                            .build())
                    .findFirst()
                    .orElse(null);
        } else {
            return searchResults.stream()
                    .filter(Objects::nonNull)
                    .map(JiraUser::getAccountId)
                    .filter(StringUtils::isNotBlank)
                    .map(id -> JiraUser.builder()
                            .accountId(id)
                            .build())
                    .findFirst()
                    .orElse(null);
        }
    }

    @Nullable
    private List<JiraComponent> getComponents(JiraProject project, JiraCreateIssueQuery query) {
        List<String> componentNames = ListUtils.emptyIfNull(query.getComponentNames()).stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(componentNames)) {
            return null;
        }
        return ListUtils.emptyIfNull(project.getComponents()).stream()
                .filter(Objects::nonNull)
                .filter(comp -> StringUtils.isNotBlank(comp.getName()))
                .filter(comp -> query.getComponentNames().contains(comp.getName()))
                .map(JiraComponent::getId)
                .map(id -> JiraComponent.builder()
                        .id(id)
                        .build())
                .collect(Collectors.toList());
    }

    @Nullable
    public static List<JiraVersion> getVersions(@Nonnull JiraClient jiraClient, @Nonnull JiraProject project, @Nonnull List<String> versionsToGetOrCreate) {
        Map<String, String> existingVersionIdsByName = ListUtils.emptyIfNull(project.getVersions()).stream()
                .filter(Objects::nonNull)
                .filter(version -> StringUtils.isNotBlank(version.getName()))
                .collect(Collectors.toMap(JiraVersion::getName, JiraVersion::getId));

        List<JiraVersion> versions = ListUtils.emptyIfNull(versionsToGetOrCreate).stream()
                .filter(StringUtils::isNotBlank)
                .map(versionName -> {
                    // -- if the version already exists, return it
                    String existingId = existingVersionIdsByName.get(versionName);
                    if (existingId != null) {
                        return existingId;
                    }
                    // -- if the version doesn't exist, attempt to create it:
                    try {
                        return jiraClient.createVersion(project.getId(), versionName).getId();
                    } catch (JiraClientException e) {
                        log.warn("Skipping version '{}': Failed to create version for projectId={}", versionName, project.getId(), e);
                        return null;
                    }
                })
                .filter(StringUtils::isNotEmpty)
                .map(id -> JiraVersion.builder()
                        .id(id)
                        .build())
                .collect(Collectors.toList());
        return versions.isEmpty() ? null : versions;
    }

    @Override
    public JiraCreateIssueQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, JiraCreateIssueQuery.class);
    }

}
