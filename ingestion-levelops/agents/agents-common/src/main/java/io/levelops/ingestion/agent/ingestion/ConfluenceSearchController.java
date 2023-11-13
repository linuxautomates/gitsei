package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.strategies.pagination.NumberedPaginationStrategy;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.integrations.confluence.models.ConfluenceSearchQuery;
import io.levelops.integrations.confluence.models.ConfluenceSearchResult;
import io.levelops.integrations.confluence.sources.ConfluenceSearchDataSource;
import io.levelops.ingestion.sinks.StorageDataSink;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;

@Log4j2
public class ConfluenceSearchController implements DataController<ConfluenceSearchQuery> {

    private static final int PAGE_SIZE = 25;
    private final ObjectMapper objectMapper;

    private final PaginationStrategy<ConfluenceSearchResult, ConfluenceSearchQuery> paginationStrategy;

    @Builder
    public ConfluenceSearchController(ObjectMapper objectMapper,
                                      StorageDataSink storageDataSink,
                                      ConfluenceSearchDataSource searchDataSource,
                                      @Nullable Integer outputPageSize) {
        this.objectMapper = objectMapper;

        paginationStrategy = NumberedPaginationStrategy.<ConfluenceSearchResult, ConfluenceSearchQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .integrationType("confluence")
                .dataType("search")
                .pageDataSupplier((query, page) -> searchDataSource.fetchMany(query.toBuilder()
                        .skip(page.getPageNumber() * PAGE_SIZE)
                        .limit(PAGE_SIZE)
                        .build()))
                .outputPageSize(outputPageSize)
                .build();
    }


    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, ConfluenceSearchQuery query) throws IngestException {
        return paginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query);
    }

    @Override
    public ConfluenceSearchQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, ConfluenceSearchQuery.class);
    }
}
