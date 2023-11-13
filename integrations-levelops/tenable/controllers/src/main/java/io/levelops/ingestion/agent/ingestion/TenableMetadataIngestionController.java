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
import io.levelops.integrations.storage.models.StorageResult;
import io.levelops.integrations.tenable.models.NetworkResponse;
import io.levelops.integrations.tenable.models.ScannerPoolResponse;
import io.levelops.integrations.tenable.models.ScannerResponse;
import io.levelops.integrations.tenable.models.TenableMetadataQuery;
import io.levelops.integrations.tenable.sources.TenableNetworkDataSource;
import io.levelops.integrations.tenable.sources.TenableScannerDataSource;
import io.levelops.integrations.tenable.sources.TenableScannerPoolDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

/**
 * Tenable's implementation of the {@link DataController}. Responsible for fetching details about
 * networks, scanners and scanner groups.
 */
@Log4j2
public class TenableMetadataIngestionController implements DataController<TenableMetadataQuery> {

    private static final String NETWORKS = "networks";
    private static final String SCANNERS = "scanners";
    private static final String SCANNER_GROUPS = "scanner_groups";
    private static final String INTEGRATION_TYPE = "tenable";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<NetworkResponse.Network, TenableMetadataQuery> networkPaginationStrategy;
    private final PaginationStrategy<ScannerResponse.Scanner, TenableMetadataQuery> scannerPaginationStrategy;
    private final PaginationStrategy<ScannerPoolResponse.ScannerPool, TenableMetadataQuery> scannerPoolPaginationStrategy;

    @Builder
    public TenableMetadataIngestionController(ObjectMapper objectMapper, TenableNetworkDataSource tenableNetworkDataSource,
                                              TenableScannerDataSource tenableScannerDataSource,
                                              TenableScannerPoolDataSource tenableScannerPoolDataSource,
                                              StorageDataSink storageDataSink) {
        this.objectMapper = objectMapper;
        this.networkPaginationStrategy = StreamedPaginationStrategy.<NetworkResponse.Network, TenableMetadataQuery>builder()
                .objectMapper(objectMapper)
                .dataType(NETWORKS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(tenableNetworkDataSource)
                .skipEmptyResults(true)
                .build();
        this.scannerPaginationStrategy = StreamedPaginationStrategy.<ScannerResponse.Scanner, TenableMetadataQuery>builder()
                .objectMapper(objectMapper)
                .dataType(SCANNERS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(tenableScannerDataSource)
                .skipEmptyResults(true)
                .build();
        this.scannerPoolPaginationStrategy = StreamedPaginationStrategy.<ScannerPoolResponse.ScannerPool, TenableMetadataQuery>builder()
                .objectMapper(objectMapper)
                .dataType(SCANNER_GROUPS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(tenableScannerPoolDataSource)
                .skipEmptyResults(true)
                .build();
    }

    /**
     * Ingests the data for given job {@param jobId} with the query {@param query}. It calls the
     * {@link TenableNetworkDataSource} for fetching the networks, {@link TenableScannerDataSource}
     * for fetching the scanners detail and {@link TenableScannerPoolDataSource} for fetching detail
     * about scanner pools
     *
     * @param jobId id of the job for which data needs to be ingested.
     * @param query describing the job
     * @return {@link ControllerIngestionResult} for the executed job
     * @throws IngestException for any exception during the ingestion process.
     */
    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, TenableMetadataQuery query) throws IngestException {
        log.debug("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        StorageResult networkStorageResult = networkPaginationStrategy
                .ingestAllPages(jobContext, query.getIntegrationKey(), query);
        StorageResult scannerStorageResult = scannerPaginationStrategy
                .ingestAllPages(jobContext, query.getIntegrationKey(), query);
        StorageResult scannerPoolStorageResult = scannerPoolPaginationStrategy
                .ingestAllPages(jobContext, query.getIntegrationKey(), query);
        return new ControllerIngestionResultList(networkStorageResult, scannerStorageResult, scannerPoolStorageResult);
    }

    /**
     * Parses the received query
     *
     * @param arg: query object corresponding to the required {@link TenableMetadataQuery}
     * @return {@link TenableMetadataQuery} for the job
     */
    @Override
    public TenableMetadataQuery parseQuery(Object arg) {

        log.debug("parseQuery: received args: {}", arg);
        TenableMetadataQuery query = objectMapper.convertValue(arg, TenableMetadataQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }
}
