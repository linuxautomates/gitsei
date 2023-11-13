package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.github.models.GithubWebhookEnrichQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.github.GithubWebhookEnrichDataSource;
import io.levelops.integrations.github.models.GithubWebhookEnrichResponse;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;

public class GithubWebhookEnrichmentController implements DataController<GithubWebhookEnrichQuery> {

    private static final String DATA_TYPE = "enriched_webhook_events";
    private static final String INTEGRATION_TYPE = "github";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<GithubWebhookEnrichResponse, GithubWebhookEnrichDataSource.GithubWebhookEnrichQuery> webhookEnrichQueryPaginationStrategy;

    @Builder
    public GithubWebhookEnrichmentController(ObjectMapper objectMapper,
                                             StorageDataSink storageDataSink,
                                             GithubWebhookEnrichDataSource githubWebhookEnrichDataSource) {
        this.objectMapper = objectMapper;
        this.webhookEnrichQueryPaginationStrategy = StreamedPaginationStrategy.<GithubWebhookEnrichResponse, GithubWebhookEnrichDataSource.GithubWebhookEnrichQuery>builder()
                        .objectMapper(objectMapper)
                        .dataSource(githubWebhookEnrichDataSource)
                        .storageDataSink(storageDataSink)
                        .integrationType(INTEGRATION_TYPE)
                        .dataType(DATA_TYPE)
                        .skipEmptyResults(true)
                        .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GithubWebhookEnrichQuery query) throws IngestException {
        StorageResult results = webhookEnrichQueryPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(),
                GithubWebhookEnrichDataSource.GithubWebhookEnrichQuery.builder()
                        .integrationKey(query.getIntegrationKey())
                        .enrichmentRequests(query.getRequests())
                        .build());
        return new ControllerIngestionResultList(results);
    }

    @Override
    public GithubWebhookEnrichQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, GithubWebhookEnrichQuery.class);
    }
}
