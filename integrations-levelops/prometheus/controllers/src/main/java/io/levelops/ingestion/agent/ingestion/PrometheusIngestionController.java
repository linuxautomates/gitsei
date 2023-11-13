package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.StorageStrategy;
import io.levelops.integrations.prometheus.client.PrometheusClient;
import io.levelops.integrations.prometheus.client.PrometheusClientFactory;
import io.levelops.integrations.prometheus.models.PrometheusIngestionQuery;
import io.levelops.integrations.prometheus.models.PrometheusQueryResponse;
import io.levelops.integrations.prometheus.sources.PrometheusQueryDataSource;
import io.levelops.integrations.storage.models.StorageMetadata;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.util.List;

@Log4j2
public class PrometheusIngestionController implements DataController<PrometheusIngestionQuery> {

    private final ObjectMapper objectMapper;
    private final PrometheusClientFactory clientFactory;
    private final PrometheusQueryDataSource prometheusQueryDataSource;
    private final StorageDataSink storageDataSink;
    private final StorageStrategy storageStrategy;
    private static final String INTEGRATION_TYPE = "prometheus";
    private static final String DATATYPE = "prometheus_response";

    @Builder
    public PrometheusIngestionController(ObjectMapper objectMapper, PrometheusClientFactory clientFactory, PrometheusQueryDataSource prometheusQueryDataSource, StorageDataSink storageDataSink) {
        this.objectMapper = objectMapper;
        this.clientFactory = clientFactory;
        this.prometheusQueryDataSource = prometheusQueryDataSource;
        this.storageDataSink = storageDataSink;
        this.storageStrategy = new StorageStrategy(objectMapper, storageDataSink);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, PrometheusIngestionQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        IntegrationKey integrationKey = query.getIntegrationKey();
        PrometheusClient client = clientFactory.get(integrationKey);
        validateQuery(query);
        log.debug("ingest: Querying for {} with query {}", integrationKey, query.getQueryRequest().getQueryString());
        Data<PrometheusQueryResponse> prometheusQueryData = prometheusQueryDataSource.fetchOne(query);
        StorageResult storageResult = storageStrategy.storeOnePage(integrationKey, INTEGRATION_TYPE, DATATYPE, jobContext.getJobId(), ListResponse.of(List.of(prometheusQueryData)), null, null);
        return StorageResult.builder()
                .storageMetadata(StorageMetadata.builder()
                        .key(integrationKey)
                        .integrationType(INTEGRATION_TYPE)
                        .dataType(DATATYPE)
                        .build())
                .prefixUri(StorageStrategy.getRelativePathPrefix(DATATYPE))
                .records(storageResult.getRecords())
                .build();
    }

    private void validateQuery(PrometheusIngestionQuery query) throws IngestException {
        if (query.getQueryRequest().getQueryString() == null) {
            log.error("ingest: QueryString cannot be null with prometheus query: {}", query);
            throw new IngestException("QueryString cannot be null with prometheus query: " + query);
        }
        if (query.getQueryRequest().getIsInstant() == null) {
            log.error("ingest: IsInstant cannot be null with prometheus query: {}", query);
            throw new IngestException("IsInstant cannot be null with prometheus query: " + query);
        }
        if (!query.getQueryRequest().getIsInstant()) {
            if (query.getQueryRequest().getStartTime() == null) {
                log.error("ingest: StartTime cannot be null with prometheus query: {}", query);
                throw new IngestException("StartTime cannot be null with prometheus query: " + query);
            } else if (query.getQueryRequest().getEndTime() == null) {
                log.error("ingest: EndTime cannot be null with prometheus query: {}", query);
                throw new IngestException("EndTime cannot be null with prometheus query: " + query);
            } else if (query.getQueryRequest().getStep() == null) {
                log.error("ingest: Step cannot be null with prometheus query: {}", query);
                throw new IngestException("Step cannot be null with prometheus query: " + query);
            }
        }
    }

    @Override
    public PrometheusIngestionQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        PrometheusIngestionQuery query = objectMapper.convertValue(arg, PrometheusIngestionQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }
}
