package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.jira.models.JiraGetIssueQuery;
import io.levelops.ingestion.integrations.jira.models.JiraGetIssueResult;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.sources.JiraIssueDataSource;

public class JiraGetIssueController implements DataController<JiraGetIssueQuery> {

    private final ObjectMapper objectMapper;
    private final JiraIssueDataSource jiraIssueDataSource;

    public JiraGetIssueController(ObjectMapper objectMapper, JiraIssueDataSource jiraIssueDataSource) {
        this.objectMapper = objectMapper;
        this.jiraIssueDataSource = jiraIssueDataSource;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, JiraGetIssueQuery query) throws IngestException {
        Data<JiraIssue> jiraIssueData = jiraIssueDataSource.fetchOne(JiraIssueDataSource.JiraIssueQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .issueKey(query.getIssueKey())
                .build());

        return JiraGetIssueResult.builder()
                .issue(jiraIssueData.getPayload())
                .build();
    }

    @Override
    public JiraGetIssueQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, JiraGetIssueQuery.class);
    }

}
