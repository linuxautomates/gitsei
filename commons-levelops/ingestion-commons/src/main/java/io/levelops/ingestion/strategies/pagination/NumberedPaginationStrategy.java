package io.levelops.ingestion.strategies.pagination;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.data.DataCollector;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.strategies.IStorageStrategy;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.gcs.models.GcsDataResult;
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.ingestion.sinks.StorageDataSink;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Log4j2
public class NumberedPaginationStrategy<D, Q extends DataQuery> extends BasePaginationStrategy<D, Q> implements PaginationStrategy<D, Q> {

    private static final int DEFAULT_OUTPUT_PAGE_SIZE = 500;
    private final PageDataSupplier<Q, D> pageDataSupplier;
    private final boolean skipEmptyResults;
    private final int outputPageSize;
    private final IStorageStrategy storageStrategy;
    private final Predicate<List<D>> customEmptyPagePredicate;

    @FunctionalInterface
    public interface PageDataSupplier<Q extends DataQuery, D> {
        Stream<Data<D>> fetchPage(Q query, Page page) throws FetchException;
    }

    /**
     * Strategy to ingest numbered pages of data (i.e indexed by a page number) from a source and storing the output to GCS.
     *
     * @param skipEmptyResults If False, at least 1 file will be written to GCS even if results are empty
     * @param customEmptyPagePredicate If defined, results matching this predicate will not be stored to GCS (honors skipEmptyResults) but ingestion won't be interrupted
     * @param outputPageSize page size of files stored in GCS
     * @param storageStrategy {@link IStorageStrategy} to be used, if defined, defaults to {@link StorageStrategy}
     * @param uniqueOutputFiles        If True, output files will not get overwritten between job attempts.
     */
    @Builder
    public NumberedPaginationStrategy(ObjectMapper objectMapper,
                                      StorageDataSink storageDataSink,
                                      String integrationType,
                                      String dataType,
                                      PageDataSupplier<Q, D> pageDataSupplier,
                                      Boolean skipEmptyResults,
                                      Predicate<List<D>> customEmptyPagePredicate,
                                      @Nullable Integer outputPageSize,
                                      IStorageStrategy storageStrategy,
                                      Boolean uniqueOutputFiles) {
        super(objectMapper, integrationType, dataType, uniqueOutputFiles);
        this.pageDataSupplier = pageDataSupplier;
        this.skipEmptyResults = Boolean.TRUE.equals(skipEmptyResults);
        this.outputPageSize = MoreObjects.firstNonNull(outputPageSize, DEFAULT_OUTPUT_PAGE_SIZE);
        this.customEmptyPagePredicate = MoreObjects.firstNonNull(customEmptyPagePredicate, l -> false);
        this.storageStrategy = MoreObjects.firstNonNull(storageStrategy, new StorageStrategy(objectMapper, storageDataSink));
    }

    @Override
    public StorageResult fetchAndStoreAllPages(JobContext jobContext, IntegrationKey integrationKey, Q query) throws IngestException {
        try {
            ImmutablePair<List<GcsDataResult>, List<IngestionFailure>> dataStoreResult =
                    fetchAndStoreAllPagesUnchecked(jobContext, integrationKey, query);

            return StorageResult.builder()
                    .records(dataStoreResult.getLeft())
                    .ingestionFailures(dataStoreResult.getRight())
                    .build();
        } catch (RuntimeIngestException e) {
            if (e.getCause() instanceof IngestException) {
                throw (IngestException) e.getCause();
            }
            throw e;
        }
    }

    private ImmutablePair<List<GcsDataResult>, List<IngestionFailure>> fetchAndStoreAllPagesUnchecked(JobContext jobContext, IntegrationKey integrationKey, Q query) throws RuntimeIngestException {
        log.info("Starting paginated ingestion of {}'s {} for job_id={}", integrationType, dataType, jobContext.getJobId());
        List<GcsDataResult> storageResults = new ArrayList<>();
        List<IngestionFailure> ingestionFailures = new ArrayList<>();

        String fileNamePrefix = generateFileNamePrefix();

        StreamUtils.forEachPage(streamData(query), outputPageSize, page -> page.forEach(collector -> {
            List<D> data = collector.getRecords();
            ingestionFailures.addAll(collector.getIngestionFailures());

            // don't store page if it's empty
            if (skipEmptyResults && (data.isEmpty()  || customEmptyPagePredicate.test(data))) {
                return;
            }
            int pageNumber = storageResults.size();
            StorageResult storageResult;
            try {
                storageResult = storageStrategy.storeOnePage(
                        integrationKey,
                        getIntegrationType(),
                        getDataType(),
                        jobContext.getJobId(),
                        ListResponse.of(data),
                        pageNumber,
                        fileNamePrefix);
            } catch (IngestException e) {
                throw new RuntimeIngestException(e);
            }
            storageResults.addAll(storageResult.getRecords());

            log.info("Ingested page {} of {}'s {} for job_id={}", pageNumber, integrationType, dataType, jobContext.getJobId());
        }));

        return ImmutablePair.of(storageResults, ingestionFailures);
    }

    private Stream<DataCollector<D>> streamData(Q query) throws RuntimeIngestException {
        return IntStream.iterate(0, i -> i + 1)
                .mapToObj(pageNumber -> {
                    try {
                        return DataCollector.collect(pageDataSupplier.fetchPage(query, Page.builder().pageNumber(pageNumber).build()));
                    } catch (FetchException e) {
                        throw new RuntimeIngestException(e);
                    }
                })
                .takeWhile(d -> CollectionUtils.isNotEmpty(d.getRecords()));
    }

    private static class RuntimeIngestException extends RuntimeException {
        public RuntimeIngestException(Throwable cause) {
            super(cause);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Page.PageBuilder.class)
    public static class Page {
        int pageNumber;
        // TODO add other pagination mechanisms
    }

}