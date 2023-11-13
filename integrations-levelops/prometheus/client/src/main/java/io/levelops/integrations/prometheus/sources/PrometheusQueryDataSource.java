package io.levelops.integrations.prometheus.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.prometheus.client.PrometheusClientException;
import io.levelops.integrations.prometheus.client.PrometheusClientFactory;
import io.levelops.integrations.prometheus.models.PrometheusIngestionQuery;
import io.levelops.integrations.prometheus.models.PrometheusQueryRequest;
import io.levelops.integrations.prometheus.models.PrometheusQueryResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.stream.Stream;

@Log4j2
public class PrometheusQueryDataSource implements DataSource<PrometheusQueryResponse, PrometheusIngestionQuery> {

    private final PrometheusClientFactory clientFactory;

    public PrometheusQueryDataSource(PrometheusClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<PrometheusQueryResponse> fetchOne(PrometheusIngestionQuery query) throws FetchException {
        PrometheusQueryRequest prometheusQueryRequest;
        Validate.notNull(query, "query ingestion cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "getIntegrationKey() cannot be null.");
        Validate.notNull(query.getQueryRequest().getQueryString(), "getQueryString() cannot be null.");
        Validate.notNull(query.getQueryRequest().getIsInstant(), "getIsInstant() cannot be null.");
        if (query.getQueryRequest().getIsInstant()) {
            prometheusQueryRequest = PrometheusQueryRequest.builder()
                    .integrationKey(query.getIntegrationKey())
                    .queryString(query.getQueryRequest().getQueryString())
                    .build();
        } else {
            Validate.notNull(query.getQueryRequest().getStartTime(), "query.getStartTime() cannot be null.");
            Validate.notNull(query.getQueryRequest().getEndTime(), "query.getEndTime() cannot be null.");
            Validate.notNull(query.getQueryRequest().getStep(), "query.getStep() cannot be null.");
            prometheusQueryRequest = PrometheusQueryRequest.builder()
                    .integrationKey(query.getIntegrationKey())
                    .queryString(query.getQueryRequest().getQueryString())
                    .startTime(query.getQueryRequest().getStartTime())
                    .endTime(query.getQueryRequest().getEndTime())
                    .step(query.getQueryRequest().getStep())
                    .build();
        }
        try {
            PrometheusQueryResponse prometheusQueryResponse = fetchOneProject(query.getIntegrationKey(), prometheusQueryRequest, query.getQueryRequest().getIsInstant());
            return BasicData.of(PrometheusQueryResponse.class, prometheusQueryResponse);
        } catch (PrometheusClientException e) {
            throw new FetchException("Could not fetch data from prometheus", e);
        }
    }

    @Override
    public Stream<Data<PrometheusQueryResponse>> fetchMany(PrometheusIngestionQuery prometheusIngestionQuery) throws FetchException {
        throw new UnsupportedOperationException("fetchMany not supported");
    }

    private PrometheusQueryResponse fetchOneProject(IntegrationKey integrationKey, PrometheusQueryRequest prometheusQueryRequest, boolean isInstant)
            throws PrometheusClientException {
        PrometheusQueryResponse prometheusQueryResponse;
        if (isInstant) {
            prometheusQueryResponse = clientFactory.get(integrationKey)
                    .runInstantQuery(prometheusQueryRequest);
        } else {
            prometheusQueryResponse = clientFactory.get(integrationKey)
                    .runRangeQuery(prometheusQueryRequest);
        }
        log.debug("Query Response Status={}", prometheusQueryResponse.getStatus());
        return prometheusQueryResponse;
    }
}
