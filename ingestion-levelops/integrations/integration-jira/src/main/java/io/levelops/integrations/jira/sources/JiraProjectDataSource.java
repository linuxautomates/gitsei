package io.levelops.integrations.jira.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraPriority;
import io.levelops.integrations.jira.models.JiraPriorityScheme;
import io.levelops.integrations.jira.models.JiraProject;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nonnull;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class JiraProjectDataSource implements DataSource<JiraProject, JiraProjectDataSource.JiraProjectQuery> {

    @JsonProperty("jira_client_factory")
    private final JiraClientFactory jiraClientFactory;


    public JiraProjectDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public Data<JiraProject> fetchOne(JiraProjectQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notNull(query.getProjectKey(), "query.getProjectKey() cannot be null.");
        try {
            JiraProject jiraProject = fetchOneProject(query.getIntegrationKey(), query.getProjectKey());
            return BasicData.of(JiraProject.class, jiraProject);
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira projects", e);
        }
    }

    @Override
    public Stream<Data<JiraProject>> fetchMany(JiraProjectQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            List<JiraProject> projects = jiraClientFactory.get(query.getIntegrationKey()).getProjects();
            return projects.stream()
                    .map(p -> {
                        try {
                            return fetchOneProject(query.getIntegrationKey(), p.getKey());
                        } catch (JiraClientException e) {
                            log.warn("Could not expand one project with key={}", p.getKey(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(JiraProject.class));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira projects", e);
        }
    }

    @Nonnull
    List<JiraPriority> fetchPriorities(IntegrationKey integrationKey, String projectKey) {
        List<JiraPriority> jiraPriorities;
        try {
            jiraPriorities = jiraClientFactory.get(integrationKey).getPriorities();
        } catch (JiraClientException e) {
            log.warn("fetchOneProject: Ignoring exception... Priorities are empty for project: " + projectKey);
            return Collections.emptyList();
        }
        MutableInt index = new MutableInt(1);
        return jiraPriorities.stream()
                .map(priority -> priority.toBuilder()
                        .priorityOrder(index.getAndIncrement())
                        .build())
                .collect(Collectors.toList());
    }

    JiraProject fetchOneProject(IntegrationKey integrationKey, String projectKey) throws JiraClientException {
        JiraProject project = jiraClientFactory.get(integrationKey).getProject(projectKey);

        List<JiraPriority> jiraPriorities = fetchPriorities(integrationKey, projectKey);
        JiraPriorityScheme priorityScheme = getJiraPriorityScheme(integrationKey, projectKey, jiraPriorities);

        Date currentTime = new Date();
        Long jiraProjectIngestedAt = DateUtils.truncate(currentTime, Calendar.DATE);

        project = project.toBuilder()
                .priorityScheme(priorityScheme)
                .defaultPriorities(jiraPriorities)
                .jiraProjectsIngestedAt(jiraProjectIngestedAt)
                .build();

        log.debug("Fetched Project with URI={}", project.getSelf());
        return project;
    }

    JiraPriorityScheme getJiraPriorityScheme(IntegrationKey integrationKey, String projectKey,
                                             List<JiraPriority> defaultPriorityList) {
        JiraPriorityScheme priorityScheme;
        try {
            priorityScheme = jiraClientFactory.get(integrationKey).getPrioritySchemes(projectKey);
        } catch (JiraClientException e) {
            log.debug("getJiraPriorityScheme: Ignoring exception...the integration could be a hosted Jira which " +
                    "does not support this API for integrationId: " + integrationKey);
            priorityScheme = JiraPriorityScheme.builder()
                    .priorities(defaultPriorityList)
                    .name("default")
                    .build();
        }
        return priorityScheme;
    }

    @Value
    @Builder
    @JsonDeserialize(builder = JiraProjectQuery.JiraProjectQueryBuilder.class)
    public static class JiraProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        /**
         * Needed to fetch one specific project. Both Key and Id work.
         */
        @JsonProperty("project_key")
        String projectKey;
    }
}
