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
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraUser;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

@Log4j2
public class JiraUserDataSource implements DataSource<JiraUser, JiraUserDataSource.JiraUserQuery> {

    private final JiraClientFactory jiraClientFactory;

    public JiraUserDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public Data<JiraUser> fetchOne(JiraUserQuery query) throws FetchException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Stream<Data<JiraUser>> fetchMany(JiraUserQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            return jiraClientFactory.get(query.getIntegrationKey())
                    .streamUsers()
                    .map(BasicData.mapper(JiraUser.class));
        } catch (JiraClientException e) {
            throw new FetchException("Could not fetch Jira issues", e);
        }
    }

    @Value
    @Builder
    @JsonDeserialize(builder = JiraUserQuery.JiraUserQueryBuilder.class)
    public static class JiraUserQuery implements DataQuery, IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

    }


}
