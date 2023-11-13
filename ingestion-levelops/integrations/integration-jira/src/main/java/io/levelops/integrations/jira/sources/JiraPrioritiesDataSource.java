package io.levelops.integrations.jira.sources;

import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraPriority;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

@Log4j2
public class JiraPrioritiesDataSource implements DataSource<JiraPriority, BaseIntegrationQuery> {
    private final JiraClientFactory jiraClientFactory;

    public JiraPrioritiesDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public Data<JiraPriority> fetchOne(BaseIntegrationQuery query) throws FetchException {
        throw new UnsupportedOperationException("Cannot fetch single status");
    }

    @Override
    public Stream<Data<JiraPriority>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            return jiraClientFactory.get(query.getIntegrationKey())
                    .getPriorities()
                    .stream()
                    .map(BasicData.mapper(JiraPriority.class));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira priorites", e);
        }
    }
}
