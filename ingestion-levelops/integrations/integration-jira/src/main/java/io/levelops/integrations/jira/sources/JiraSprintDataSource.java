package io.levelops.integrations.jira.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraSprint;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class JiraSprintDataSource implements DataSource<JiraSprint, JiraSprintDataSource.JiraSprintQuery> {

    private final JiraClientFactory jiraClientFactory;

    public JiraSprintDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }


    @Override
    public Data<JiraSprint> fetchOne(JiraSprintQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<JiraSprint>> fetchMany(JiraSprintQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            JiraClient jiraClient = jiraClientFactory.get(query.getIntegrationKey());
            return jiraClient.streamBoards()
                    .filter(Objects::nonNull)
                    .flatMap(jiraBoard -> {
                        try {
                            return jiraClient.streamSprints(jiraBoard.getId());
                        } catch (JiraClientException e) {
                            log.warn("Error while fetching sprints for board {} integrationKey {}",
                                    jiraBoard.getId(), query.getIntegrationKey(), ExceptionUtils.getRootCause(e));
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(JiraSprint.class));
        } catch (JiraClientException e) {
            log.warn("fetchMany: Error while fetching jira boards for integrationKey {}", query.getIntegrationKey(), ExceptionUtils.getRootCause(e));
        }
        return Stream.empty();
    }

    @Value
    @Builder
    @JsonDeserialize(builder = JiraSprintDataSource.JiraSprintQuery.JiraSprintQueryBuilder.class)
    public static class JiraSprintQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;
    }

}
