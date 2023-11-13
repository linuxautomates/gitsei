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
import io.levelops.integrations.tenable.models.Asset;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import io.levelops.integrations.tenable.models.Vulnerability;
import io.levelops.integrations.tenable.models.WASResponse;
import io.levelops.integrations.tenable.sources.TenableAssetDataSource;
import io.levelops.integrations.tenable.sources.TenableVulnerabilityDataSource;
import io.levelops.integrations.tenable.sources.TenableWASDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Tenable's implementation of the {@link DataController}. Responsible for fetching assets and vulnerabilities
 * data upon receiving the ingest request.
 */
@Log4j2
public class TenableIngestionController implements DataController<TenableScanQuery> {
    private static final String DATATYPE_ASSET = "asset";
    private static final String DATATYPE_VULNERABILITY = "vulnerability";
    private static final String DATATYPE_WEB_VULNERABILITY = "web_vulnerability";
    private static final String INTEGRATION_TYPE = "tenable";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<Asset, TenableScanQuery> assetPaginationStrategy;
    private final PaginationStrategy<Vulnerability, TenableScanQuery> vulnerabilityPaginationStrategy;
    private final PaginationStrategy<WASResponse.Data, TenableScanQuery> wasPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public TenableIngestionController(ObjectMapper objectMapper,
                                      int onboardingScanInDays, StorageDataSink storageDataSink,
                                      TenableAssetDataSource tenableAssetDataSource,
                                      TenableWASDataSource tenableWASDataSource,
                                      TenableVulnerabilityDataSource tenableVulnerabilityDataSource) {
        this.objectMapper = objectMapper;
        this.assetPaginationStrategy = StreamedPaginationStrategy.<Asset, TenableScanQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_ASSET)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(tenableAssetDataSource)
                .skipEmptyResults(true)
                .build();
        this.vulnerabilityPaginationStrategy = StreamedPaginationStrategy.<Vulnerability, TenableScanQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_VULNERABILITY)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(tenableVulnerabilityDataSource)
                .skipEmptyResults(true)
                .build();
        this.wasPaginationStrategy = StreamedPaginationStrategy.<WASResponse.Data, TenableScanQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_WEB_VULNERABILITY)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(tenableWASDataSource)
                .skipEmptyResults(true)
                .build();
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 30;
    }

    /**
     * Ingests the data for given job {@param jobId} with the query {@param query}. It calls the
     * {@link TenableAssetDataSource} for fetching the assets, {@link TenableVulnerabilityDataSource}
     * for fetching the vulnerabilities and {@link TenableWASDataSource} for fetching the web application
     * scanning vulnerabilities.
     *
     * @param jobId id of the job for which data needs to be ingested.
     * @param query describing the job
     * @return {@link ControllerIngestionResult} for the executed job
     * @throws IngestException for any exception during the ingestion process.
     */
    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, TenableScanQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        TenableScanQuery tenableScanQuery;
        if (query.getSince() == null) {
            tenableScanQuery = TenableScanQuery.builder()
                    .since(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS).getEpochSecond())
                    .partial(query.getPartial())
                    .integrationKey(query.getIntegrationKey())
                    .build();
        } else {
            tenableScanQuery = query;
        }
        StorageResult assetStorageResult = assetPaginationStrategy.ingestAllPages(
                jobContext, query.getIntegrationKey(), tenableScanQuery);
        StorageResult vulnerabilityStorageResult = vulnerabilityPaginationStrategy.ingestAllPages(
                jobContext, query.getIntegrationKey(), tenableScanQuery);
        StorageResult wasStorageResult = wasPaginationStrategy.ingestAllPages(
                jobContext, query.getIntegrationKey(), tenableScanQuery);
        return new ControllerIngestionResultList(assetStorageResult, vulnerabilityStorageResult, wasStorageResult);
    }

    /**
     * Parses the received query
     *
     * @param arg: query object corresponding to the required {@link TenableScanQuery}
     * @return {@link TenableScanQuery} for the job
     */
    @Override
    public TenableScanQuery parseQuery(Object arg) {
        log.debug("parseQuery: received args: {}", arg);
        TenableScanQuery query = objectMapper.convertValue(arg, TenableScanQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }
}
