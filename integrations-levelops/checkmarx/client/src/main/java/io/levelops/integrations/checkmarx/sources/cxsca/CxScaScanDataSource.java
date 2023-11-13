package io.levelops.integrations.checkmarx.sources.cxsca;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClient;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClientFactory;
import io.levelops.integrations.checkmarx.models.CxScaScan;
import io.levelops.integrations.checkmarx.services.cxsca.CxScaScanEnrichmentService;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CxScaScanDataSource implements DataSource<CxScaScan, CxScaScanDataSource.CxScaScanQuery> {

    private final CxScaClientFactory clientFactory;
    private final CxScaScanEnrichmentService enrichmentService;

    public CxScaScanDataSource(CxScaClientFactory factory, CxScaScanEnrichmentService enrichmentService) {
        this.clientFactory = factory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<CxScaScan> fetchOne(CxScaScanQuery cxScaScanQuery) throws FetchException {
        throw new UnsupportedOperationException("Fetch one is not supported");
    }

    @Override
    public Stream<Data<CxScaScan>> fetchMany(CxScaScanQuery cxScaScanQuery) throws FetchException {
        CxScaClient client = clientFactory.get(cxScaScanQuery.getIntegrationKey());
        List<CxScaScan> scans = client.getScans(cxScaScanQuery.getProjectId());
        scans = enrichScans(client, scans);
        Stream<Data<CxScaScan>> stream = scans.stream()
                .map(BasicData.mapper(CxScaScan.class));
        return stream.filter(Objects::nonNull);
    }

    public List<CxScaScan> enrichScans(CxScaClient client, List<CxScaScan> scans) {
        return enrichmentService.enrichScans(client, scans);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CxScaScanDataSource.CxScaScanQuery.CxScaScanQueryBuilder.class)
    public static class CxScaScanQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("project_id")
        String projectId;

        @JsonProperty("from")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        Date from;

        @JsonProperty("to")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
        Date to;

    }
}
