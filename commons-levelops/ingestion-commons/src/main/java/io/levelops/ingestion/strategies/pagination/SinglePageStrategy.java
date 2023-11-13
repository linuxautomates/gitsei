package io.levelops.ingestion.strategies.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.DataCollector;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.ingestion.strategies.IStorageStrategy;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.ingestion.sinks.StorageDataSink;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

@Log4j2
public class SinglePageStrategy<D, Q extends DataQuery> extends BasePaginationStrategy<D, Q> {

    private final DataSource<D, Q> dataSource;
    private final IStorageStrategy storageStrategy;
    private final boolean skipEmptyResults;

    @Builder
    public SinglePageStrategy(ObjectMapper objectMapper,
                              StorageDataSink storageDataSink,
                              DataSource<D, Q> dataSource,
                              String integrationType,
                              String dataType,
                              IStorageStrategy storageStrategy,
                              Boolean skipEmptyResults,
                              Boolean uniqueOutputFiles) {
        super(objectMapper, integrationType, dataType, uniqueOutputFiles);
        this.dataSource = dataSource;
        this.skipEmptyResults = Boolean.TRUE.equals(skipEmptyResults);
        this.storageStrategy = MoreObjects.firstNonNull(storageStrategy, new StorageStrategy(objectMapper, storageDataSink));
    }

    @Override
    public StorageResult fetchAndStoreAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query) throws IngestException {
        log.info("Ingesting non-paginated data for {}'s {}, job_id={}", integrationType, dataType, jobContext.getJobId());

        Stream<Data<D>> dataStream = dataSource.fetchMany(jobContext, query);

        DataCollector<D> collector = DataCollector.collect(dataStream);
        List<D> data = collector.getRecords();
        if (data.isEmpty() && skipEmptyResults) {
            return StorageResult.builder().records(Collections.emptyList()).ingestionFailures(collector.getIngestionFailures()).build();
        }

        StorageResult storageResult = storageStrategy.storeOnePage(
                integrationKey,
                integrationType,
                dataType,
                jobContext.getJobId(),
                ListResponse.of(data),
                null,
                generateFileNamePrefix());// not paginated

        // log level could be lowered
        log.info("Ingested non-paginated data for {}'s {}, job_id={}", integrationType, dataType, jobContext.getJobId());

        return StorageResult.builder()
                .records(storageResult.getRecords())
                .ingestionFailures(collector.getIngestionFailures())
                .build();
    }
}