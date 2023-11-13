package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.helix_swarm.models.HelixSwarmQuery;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helix_swarm.sources.HelixSwarmReviewDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Log4j2
public class HelixSwarmReviewController implements DataController<HelixSwarmQuery> {

    private static final String INTEGRATION_TYPE = "helix_swarm";
    private static final String REVIEWS = "reviews";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<HelixSwarmReview, HelixSwarmQuery> reviewPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public HelixSwarmReviewController(ObjectMapper objectMapper, HelixSwarmReviewDataSource dataSource,
                                      StorageDataSink storageDataSink, int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.onboardingScanInDays = onboardingScanInDays;
        this.reviewPaginationStrategy = StreamedPaginationStrategy.<HelixSwarmReview, HelixSwarmQuery>builder()
                .storageDataSink(storageDataSink)
                .objectMapper(objectMapper)
                .dataSource(dataSource)
                .integrationType(INTEGRATION_TYPE)
                .dataType(REVIEWS)
                .skipEmptyResults(true)
                .build();
    }

    @Override
    public HelixSwarmQuery parseQuery(Object arg) {
        log.debug("parseQuery: received args: {}", arg);
        return objectMapper.convertValue(arg, HelixSwarmQuery.class);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, HelixSwarmQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        HelixSwarmQuery helixSwarmQuery = query.getFrom() != null ? query : query.toBuilder()
                .from(Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)))
                .build();
        StorageResult storageResult = reviewPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(),
                helixSwarmQuery);
        log.debug("ingest: ingested all reviews for jobId: {}, storage result: {}", jobContext.getJobId(), storageResult);
        return new ControllerIngestionResultList(storageResult);
    }
}
