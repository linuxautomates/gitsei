package io.levelops.integrations.jira.sources;

import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraStatus;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

@Log4j2
public class JiraStatusDataSource implements DataSource<JiraStatus, BaseIntegrationQuery> {

    private final JiraClientFactory jiraClientFactory;

    public JiraStatusDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public Data<JiraStatus> fetchOne(BaseIntegrationQuery query) throws FetchException {
        throw new UnsupportedOperationException("Cannot fetch single status");
    }

    @Override
    public Stream<Data<JiraStatus>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            return jiraClientFactory.get(query.getIntegrationKey())
                    .getStatuses()
                    .stream()
                    .map(BasicData.mapper(JiraStatus.class));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira statuses", e);
        }
    }


}
