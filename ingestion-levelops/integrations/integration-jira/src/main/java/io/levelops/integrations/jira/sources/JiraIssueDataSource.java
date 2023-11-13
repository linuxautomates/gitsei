package io.levelops.integrations.jira.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraApiSearchQuery;
import io.levelops.integrations.jira.models.JiraApiSearchResult;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields;
import io.levelops.integrations.jira.models.JiraVersion;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class JiraIssueDataSource implements DataSource<JiraIssue, JiraIssueDataSource.JiraIssueQuery> {

    private static final Set<String> EXPAND = Set.of("changelog");
    private static final Set<String> FIELDS = Set.of("*all");

    private final JiraClientFactory jiraClientFactory;

    public JiraIssueDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public Data<JiraIssue> fetchOne(JiraIssueQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notNull(query.getIssueKey(), "query.getIssueKey() cannot be null.");
        try {
            JiraClient jiraClient = jiraClientFactory.get(query.getIntegrationKey());
            JiraApiSearchResult searchResult = jiraClient.search(JiraApiSearchQuery.builder()
                    .expand(EXPAND)
                    .fields(FIELDS)
                    .jql("key=" + query.getIssueKey())
                    .startAt(0)
                    .maxResults(1)
                    .build());
            if (CollectionUtils.isEmpty(searchResult.getIssues())) {
                return BasicData.empty(JiraIssue.class);
            }
            return BasicData.of(JiraIssue.class, enrichJiraIssue(searchResult.getIssues().get(0), jiraClient));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira projects", e);
        }
    }

    @Override
    public Stream<Data<JiraIssue>> fetchMany(JiraIssueQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            JiraClient jiraClient = jiraClientFactory.get(query.getIntegrationKey());
            String jql = StringUtils.defaultIfBlank(query.getJql(), "");
            JiraApiSearchResult searchResult = jiraClient.search(JiraApiSearchQuery.builder()
                    .expand(EXPAND)
                    .fields(FIELDS)
                    .jql(jql)
                    .startAt(query.getSkip())
                    .maxResults(query.getLimit())
                    .build());
            return searchResult.getIssues().stream()
                    .map(jiraIssue -> enrichJiraIssue(jiraIssue, jiraClient))
                    .map(BasicData.mapper(JiraIssue.class));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira issues", e);
        }
    }

    private JiraIssue enrichJiraIssue(JiraIssue issue, JiraClient jiraClient) {
        return issue.toBuilder()
                .fields(enrichJiraIssueFields(issue, jiraClient))
                .build();
    }

    private JiraIssueFields enrichJiraIssueFields(final JiraIssue issue, final JiraClient jiraClient) {
        var originalFields = issue.getFields();
        var newFields = issue.getFields().toBuilder()
                        .versions(getJiraVersions(jiraClient, issue.getFields().getVersions()))
                        .fixVersions(getJiraVersions(jiraClient, issue.getFields().getFixVersions()))
                        .build();
        // quick fix for LEV-3569. Best fix would be to fix the serialization/builder issue with dynamicFields in JiraIssueFields
        originalFields.getDynamicFields().entrySet().forEach(entry -> newFields.addDynamicField(entry.getKey(), entry.getValue()));
        return newFields;
    }

    private List<JiraVersion> getJiraVersions(JiraClient jiraClient, List<JiraVersion> versions) {
        List<JiraVersion> jiraVersions = Collections.emptyList();
        if (CollectionUtils.isNotEmpty(versions)) {
            jiraVersions = versions.stream().map(jiraVersion -> {
                try {
                    return jiraClient.getJiraVersion(jiraVersion);
                } catch (JiraClientException e) {
                    log.error("Unable to fetch jira fixVersion with id : " + jiraVersion.getId(), e);
                    return jiraVersion;
                }
            }).collect(Collectors.toList());
        }
        return jiraVersions;
    }

    @Value
    @Builder
    @JsonDeserialize(builder = JiraIssueQuery.JiraIssueQueryBuilder.class)
    public static class JiraIssueQuery implements DataQuery, IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        // region fetchMany
        @JsonProperty("jql")
        String jql;

        @JsonProperty("limit")
        Integer limit;

        @JsonProperty("skip")
        Integer skip;

        // endregion

        // region fetchOne
        /**
         * Needed to fetch one specific issue.
         */
        @JsonProperty("issue_key")
        String issueKey;
        //endregion
    }


}
