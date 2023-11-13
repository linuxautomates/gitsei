package io.levelops.ingestion.strategies.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.DataCollector;
import io.levelops.ingestion.data.DataCollector.DataCollectorBuilder;
import io.levelops.ingestion.data.FailedData;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.exceptions.ResumableIngestException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.ingestion.strategies.IStorageStrategy;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Log4j2
public class StreamedPaginationStrategy<D, Q extends DataQuery> extends BasePaginationStrategy<D, Q> implements PaginationStrategy<D, Q> {

    private static final int DEFAULT_PAGE_SIZE = 500; // TODO make configurable
    private final DataSource<D, Q> dataSource;
    private final Integer pageSize;
    private final boolean skipEmptyResults;
    private final Predicate<List<D>> customEmptyPagePredicate;
    private final IStorageStrategy storageStrategy;

    /**
     * Strategy to ingest streamed data from a source and storing the output to GCS.
     *
     * @param skipEmptyResults         If False, at least 1 file will be written to GCS even if results are empty
     * @param customEmptyPagePredicate If defined, results matching this predicate will not be stored to GCS (honors skipEmptyResults) but ingestion won't be interrupted
     * @param outputPageSize           page size of files stored in GCS
     * @param storageStrategy          {@link IStorageStrategy} to be used, if defined, defaults to {@link StorageStrategy}
     * @param uniqueOutputFiles        If True, output files will not get overwritten between job attempts.
     */
    @Builder
    public StreamedPaginationStrategy(ObjectMapper objectMapper,
                                      StorageDataSink storageDataSink,
                                      String integrationType,
                                      String dataType,
                                      DataSource<D, Q> dataSource,
                                      Integer outputPageSize,
                                      Boolean skipEmptyResults,
                                      Predicate<List<D>> customEmptyPagePredicate,
                                      IStorageStrategy storageStrategy,
                                      Boolean uniqueOutputFiles) {
        super(objectMapper, integrationType, dataType, uniqueOutputFiles);
        this.dataSource = dataSource;
        this.pageSize = ObjectUtils.firstNonNull(outputPageSize, DEFAULT_PAGE_SIZE);
        this.skipEmptyResults = Boolean.TRUE.equals(skipEmptyResults);
        this.customEmptyPagePredicate = MoreObjects.firstNonNull(customEmptyPagePredicate, l -> false);
        this.storageStrategy = MoreObjects.firstNonNull(storageStrategy, new StorageStrategy(objectMapper, storageDataSink));
    }

    public StorageResult fetchAndStoreAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query, IntermediateStateUpdater intermediateStateUpdater) throws IngestException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notNull(jobContext, "jobContext cannot be null.");
        Validate.notBlank(jobContext.getJobId(), "jobContext.getJobId() cannot be null or empty.");

        DataCollector<GcsDataResult> storageResultsCollector = fetchAndStoreAllPagesFromStream(jobContext, integrationKey, dataSource.fetchMany(jobContext, query, intermediateStateUpdater));

        StorageResult storageResult = StorageResult.builder()
                .records(storageResultsCollector.getRecords())
                .ingestionFailures(storageResultsCollector.getIngestionFailures())
                .build();

        // if a resumable error was raised, we propagate the partial results and intermediate state
        if (storageResultsCollector.getError() != null) {
            throw ResumableIngestException.builder()
                    .result(storageResult)
                    .error(storageResultsCollector.getError())
                    .intermediateState(storageResultsCollector.getIntermediateState())
                    .build();
        }

        return storageResult;
    }

    public DataCollector<GcsDataResult> fetchAndStoreAllPagesFromStream(JobContext jobContext,
                                                                        IntegrationKey integrationKey,
                                                                        Stream<Data<D>> dataStream) throws IngestException {
        DataCollectorBuilder<GcsDataResult> storageResultsCollector = DataCollector.builder();

        String fileNamePrefix = generateFileNamePrefix();

        log.info("Starting streamed ingestion of {}'s {} for job_id={}", integrationType, dataType, jobContext.getJobId());

        MutableInt pageNumber = new MutableInt(0);
        try {
            StreamUtils.forEachPageTakingWhile(dataStream, pageSize, page -> {
                int currentPage = pageNumber.getValue();
                pageNumber.increment();

                log.info("integration_type={}, data_type={}, page_number={}, job_id={}", integrationType, dataType, currentPage, jobContext.getJobId());

                DataCollector<D> collectedData = DataCollector.collect(page.stream());

                storageResultsCollector.ingestionFailures(collectedData.getIngestionFailures());
                if (collectedData.getError() != null) {
                    storageResultsCollector.error(collectedData.getError());
                }
                if (collectedData.getIntermediateState() != null) {
                    storageResultsCollector.intermediateState(collectedData.getIntermediateState());
                }

                List<D> data = collectedData.getRecords();
                if (CollectionUtils.isEmpty(data) || customEmptyPagePredicate.test(data)) {
                    // if it is the first page - don't break unless allowed
                    // always break if some pages have already been written
                    if (skipEmptyResults || currentPage > 0) {
                        return;
                    }
                }

                try {
                    StorageResult storageResult = storageStrategy.storeOnePage(
                            integrationKey,
                            integrationType,
                            dataType,
                            jobContext.getJobId(),
                            ListResponse.of(data),
                            currentPage,
                            fileNamePrefix);
                    storageResultsCollector.records(storageResult.getRecords());
                } catch (IngestException e) {
                    throw new RuntimeStreamException(e);
                }
            }, FailedData::hasNotFailed, true);
        } catch (RuntimeStreamException e) {
            // If there was a critical exception during streaming and storing of the data, we cannot trust the collector
            // So we need to propagate the exception. (SEI-2590)
            log.error("Could not finish streamed ingestion: failed to fetch or store data for {}'s {} (job_id={})", integrationType, dataType, jobContext.getJobId(), e);
            Throwable cause = (e.getCause() != null) ? e.getCause() : e; // unwrapping cause
            throw new IngestException(String.format("Failed to fetch or store data for %s's %s", integrationType, dataType), cause);
        }

        DataCollector<GcsDataResult> collector = storageResultsCollector.build();
        log.info("Finished streamed ingestion {} of {}'s {} for job_id={} {}", (collector.getError() == null) ? "with success" : "with failures", integrationType, dataType, jobContext.getJobId(),
                (MapUtils.isNotEmpty(collector.getIntermediateState()) ? "(intermediate_state=true)" : ""));
        return collector;
    }

    @Override
    public StorageResult fetchAndStoreAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query) throws IngestException {
        throw new NotImplementedException("This method should not be called directly since intermediateStateUpdated is implemented.");
    }
}
