package io.levelops.ingestion.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.Validate;

public class PaginatedIntegrationController<D, Q extends IntegrationQuery>
        implements IntegrationController<Q> {

    private final ObjectMapper objectMapper;
    private final Class<Q> queryClass;
    private final PaginationStrategy<D, Q> paginationStrategy;
    @Getter
    private final String integrationType;
    @Getter
    private final String dataType;

    @Builder
    public PaginatedIntegrationController(Class<Q> queryClass,
                                          PaginationStrategy<D, Q> paginationStrategy) {
        this.objectMapper = paginationStrategy.get();
        this.integrationType = paginationStrategy.getIntegrationType();
        this.dataType = paginationStrategy.getDataType();
        this.queryClass = queryClass;
        this.paginationStrategy = paginationStrategy;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, Q query) throws IngestException {
        return ingest(jobContext, query, null);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, Q query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notNull(jobContext, "jobContext cannot be null.");
        Validate.notBlank(jobContext.getJobId(), "currentJobId cannot be null or empty.");

        return paginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query, intermediateStateUpdater);
    }

    public Q parseQuery(Object o) {
        return objectMapper.convertValue(o, queryClass);
    }
}
