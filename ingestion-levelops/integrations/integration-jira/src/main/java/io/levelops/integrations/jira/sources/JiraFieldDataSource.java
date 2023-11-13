package io.levelops.integrations.jira.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraProject;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

@Log4j2
public class JiraFieldDataSource implements DataSource<JiraField, BaseIntegrationQuery> {

    private final JiraClientFactory jiraClientFactory;

    public JiraFieldDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    private JiraProject fetchOneProject(IntegrationKey integrationKey, String projectKey) throws JiraClientException {
        JiraProject project = jiraClientFactory.get(integrationKey).getProject(projectKey);
        log.debug("Fetched Project with URI={}", project.getSelf());
        return project;
    }

    @Override
    public Data<JiraField> fetchOne(BaseIntegrationQuery query) throws FetchException {
        throw new UnsupportedOperationException("Cannot fetch single field");
    }

    @Override
    public Stream<Data<JiraField>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            return jiraClientFactory.get(query.getIntegrationKey())
                    .getFields()
                    .stream()
                    .map(BasicData.mapper(JiraField.class));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira fields", e);
        }
    }


}
