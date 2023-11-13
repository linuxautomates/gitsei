package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.jira.models.JiraEditIssueQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraDeploymentType;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraServerInfo;
import io.levelops.integrations.jira.models.JiraTransition;
import io.levelops.integrations.jira.models.JiraUpdateIssue;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.models.JiraVersion;
import io.levelops.integrations.jira.sources.JiraIssueDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Log4j2
public class JiraEditIssueController implements DataController<JiraEditIssueQuery> {

    private final ObjectMapper objectMapper;
    private final JiraClientFactory jiraClientFactory;
    private final JiraIssueDataSource jiraIssueDataSource;

    @Builder
    public JiraEditIssueController(ObjectMapper objectMapper,
                                   JiraClientFactory jiraClientFactory,
                                   JiraIssueDataSource jiraIssueDataSource) {
        this.objectMapper = objectMapper;
        this.jiraClientFactory = jiraClientFactory;
        this.jiraIssueDataSource = jiraIssueDataSource;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, JiraEditIssueQuery query) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");

        try {
            JiraClient jiraClient = jiraClientFactory.get(query.getIntegrationKey());

            JiraServerInfo serverInfo = jiraClient.getServerInfo();
            JiraDeploymentType deploymentType = Optional.ofNullable(serverInfo).map(JiraServerInfo::getDeploymentType).orElse(JiraDeploymentType.CLOUD);

            // --
            JiraIssue currentIssue = null;
            JiraProject currentProject = null;
            boolean needsCurrentIssueAndProject = CollectionUtils.isNotEmpty(query.getVersions()) || CollectionUtils.isNotEmpty(query.getFixVersions());
            if (needsCurrentIssueAndProject) {
                Data<JiraIssue> jiraIssueData = jiraIssueDataSource.fetchOne(JiraIssueDataSource.JiraIssueQuery.builder()
                        .integrationKey(query.getIntegrationKey())
                        .issueKey(query.getIssueKey())
                        .build());
                currentIssue= jiraIssueData.getPayload();

                String projectId = null;
                if (currentIssue != null && currentIssue.getFields() != null && currentIssue.getFields().getProject() != null) {
                    projectId = currentIssue.getFields().getProject().getId();
                }
                if (projectId != null) {
                    currentProject = jiraClient.getProject(projectId);
                }
            }

            List<JiraUser> searchResults = Collections.emptyList();
            if (StringUtils.isNotBlank(query.getAssigneeSearchString())) {
                searchResults = ListUtils.emptyIfNull(jiraClient.searchUsers(query.getAssigneeSearchString(), deploymentType));
            }
            JiraUser assignee = getJiraUser(deploymentType, searchResults);

            boolean hasUpdate = false;
            JiraUpdateIssue.JiraUpdateIssueBuilder update = JiraUpdateIssue.builder();
            if (Strings.isNotEmpty(query.getSummary())) {
                update.setSummary(query.getSummary());
                hasUpdate = true;
            }
            if (Strings.isNotEmpty(query.getDescription())) {
                update.setDescription(query.getDescription());
                hasUpdate = true;
            }
            if (assignee != null) {
                update.setAssignee(assignee);
                hasUpdate = true;
            }

            // -- labels
            List<Map<JiraUpdateIssue.JiraUpdateOp, String>> labelUpdates = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(query.getLabelsToAdd())) {
                query.getLabelsToAdd().forEach(label -> labelUpdates.add(Map.of(JiraUpdateIssue.JiraUpdateOp.ADD, label)));
            }
            if (CollectionUtils.isNotEmpty(query.getLabelsToRemove())) {
                query.getLabelsToRemove().forEach(label -> labelUpdates.add(Map.of(JiraUpdateIssue.JiraUpdateOp.REMOVE, label)));
            }
            if (!labelUpdates.isEmpty()) {
                update.labels(labelUpdates);
                hasUpdate = true;
            }

            // -- versions
            List<Map<JiraUpdateIssue.JiraUpdateOp, Map<String, String>>> versionUpdates = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(query.getVersions()) && currentProject != null) {
                ListUtils.emptyIfNull(JiraCreateIssueController.getVersions(jiraClient, currentProject, query.getVersions()))
                        .forEach(version -> versionUpdates.add(Map.of(JiraUpdateIssue.JiraUpdateOp.ADD, Map.of("id", version.getId()))));
            }
            if (!versionUpdates.isEmpty()) {
                update.versions(versionUpdates);
                hasUpdate = true;
            }

            // -- fixVersions
            List<Map<JiraUpdateIssue.JiraUpdateOp, Map<String, String>>> fixVersionUpdates = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(query.getFixVersions()) && currentProject != null) {
                ListUtils.emptyIfNull(JiraCreateIssueController.getVersions(jiraClient, currentProject, query.getFixVersions()))
                        .forEach(version -> fixVersionUpdates.add(Map.of(JiraUpdateIssue.JiraUpdateOp.ADD, Map.of("id", version.getId()))));
            }
            if (!versionUpdates.isEmpty()) {
                update.fixVersions(fixVersionUpdates);
                hasUpdate = true;
            }

            // -- custom fields
            if (MapUtils.isNotEmpty(query.getCustomFields())) {
                query.getCustomFields().forEach(update::setCustomField);
                hasUpdate = true;
            }

            // -- due date
            if (StringUtils.isNotBlank(query.getDueDate())) {
                update.setDuedate(StringUtils.trim(query.getDueDate()));
                hasUpdate = true;
            }

            // -- status
            String transitionId = null;
            if (StringUtils.isNotBlank(query.getStatus())) {
                transitionId = ListUtils.emptyIfNull(jiraClient.getTransitions(query.getIssueKey()).getTransitions()).stream()
                        .filter(transition -> query.getStatus().equalsIgnoreCase(transition.getName()))
                        .findFirst()
                        .map(JiraTransition::getId)
                        .orElse(null);
            }

            // -- priority
            if (StringUtils.isNotBlank(query.getPriority())) {
                update.setPriority(JiraIssueFields.JiraPriority.builder().name(query.getPriority()).build());
                hasUpdate = true;
            }

            // ------------

            if (hasUpdate) {
                jiraClient.editIssue(query.getIssueKey(), update.build());
            }

            if (transitionId != null) {
                jiraClient.doTransition(query.getIssueKey(), JiraUpdateIssue.builder().build(), transitionId);
            }

            updateWatchers(jiraClient, deploymentType, query.getIssueKey(), query.getWatchersToAdd());

            return new EmptyIngestionResult();
        } catch (JiraClientException e) {
            throw new IngestException("Failed to edit Jira issue for integration=" + query.getIntegrationKey(), e);
        }
    }


    private void updateWatchers(JiraClient jiraClient, JiraDeploymentType deploymentType, String issueIdOrKey, List<String> watchersToAdd) throws JiraClientException {
        for (String watcher : ListUtils.emptyIfNull(watchersToAdd)) {

            List<JiraUser> searchResults;
            try {
                searchResults = ListUtils.emptyIfNull(jiraClient.searchUsers(watcher, deploymentType));
            } catch (JiraClientException e) {
                log.warn("Could not add watcher '{}': Jira user search failed", watcher, e);
                continue;
            }

            JiraUser user = getJiraUser(deploymentType, searchResults);
            String identifier = getUserIdentifier(deploymentType, user);
            if (identifier == null) {
                log.warn("Could not add watcher '{}': identifier could not be extracted", watcher);
                continue;
            }

            try {
                jiraClient.addWatcher(issueIdOrKey, identifier);
            } catch (JiraClientException e) {
                log.warn("Could not add watcher '{}'", watcher, e);
            }
        }
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

    private String getUserIdentifier(JiraDeploymentType deploymentType, JiraUser user) {
        if (user == null) {
            return null;
        }
        if (deploymentType == JiraDeploymentType.SERVER) {
            return user.getName();
        } else {
            return user.getAccountId();
        }
    }

    @Override
    public JiraEditIssueQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, JiraEditIssueQuery.class);
    }

}
