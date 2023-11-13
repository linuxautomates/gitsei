package io.levelops.integrations.jira.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraUserEmail;
import io.levelops.ingestion.sources.DataSource;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.stream.Stream;

@Log4j2
public class JiraUserEmailsDataSource implements DataSource<JiraUserEmail, JiraUserEmailsDataSource.JiraUserEmailQuery> {
    private final JiraClientFactory jiraClientFactory;

    public JiraUserEmailsDataSource(JiraClientFactory jiraClientFactory) {
        this.jiraClientFactory = jiraClientFactory;
    }

    @Override
    public Data<JiraUserEmail> fetchOne(JiraUserEmailQuery query) throws FetchException {
        throw new UnsupportedOperationException("Not supported yet");
    }

    @Override
    public Stream<Data<JiraUserEmail>> fetchMany(JiraUserEmailQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        return Lists.partition(query.getAccountIds(), 100)
                .stream()
                .flatMap(accountIdsBatch -> {
                    try {
                        return jiraClientFactory.get(query.getIntegrationKey())
                                .getUserEmailBulk(query.getAccountIds()).stream()
                                .map(BasicData.mapper(JiraUserEmail.class));
                    } catch (JiraClientException e) {
                        throw new RuntimeException(new FetchException("Could not fetch Jira issues", e));
                    }
                });
    }

    @Value
    @Builder
    @JsonDeserialize(builder = JiraUserEmailsDataSource.JiraUserEmailQuery.JiraUserEmailQueryBuilder.class)
    public static class JiraUserEmailQuery implements DataQuery, IntegrationQuery {
        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("account_ids")
        List<String> accountIds;
    }
}
