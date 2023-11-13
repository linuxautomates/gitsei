package io.levelops.ingestion.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.ingestion.sinks.StorageDataSink;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

public class SinglePageIntegrationController<D, Q extends IntegrationQuery>
        implements IntegrationController<Q> {

    private final ObjectMapper objectMapper;
    private final DataSource<D, Q> dataSource;
    private final StorageStrategy storageStrategy;
    private final Class<Q> queryClass;
    @Getter
    private final String integrationType;
    @Getter
    private final String dataType;

    @Builder
    public SinglePageIntegrationController(ObjectMapper objectMapper,
                                           StorageDataSink storageDataSink,
                                           DataSource<D, Q> dataSource,
                                           Class<Q> queryClass,
                                           String integrationType,
                                           String dataType) {
        this.objectMapper = objectMapper;
        this.dataSource = dataSource;
        this.queryClass = queryClass;
        this.integrationType = integrationType;
        this.dataType = dataType;
        storageStrategy = new StorageStrategy(objectMapper, storageDataSink);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, Q query) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notNull(jobContext, "jobContext cannot be null.");
        Validate.notBlank(jobContext.getJobId(), "jobContext.getJobId() cannot be null or empty.");

        Stream<Data<D>> data = dataSource.fetchMany(jobContext, query);

        return storageStrategy.storeOnePage(
                query.getIntegrationKey(),
                integrationType,
                dataType,
                jobContext.getJobId(),
                ListResponse.of(Data.collect(data)),
                null /* not paginated */,
                null);
    }

    public Q parseQuery(Object o) {
        return objectMapper.convertValue(o, queryClass);
    }

}
