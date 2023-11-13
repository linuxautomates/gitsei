package io.levelops.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.SinglePageStrategy;
import io.levelops.integrations.checkmarx.models.CxScaScan;
import io.levelops.integrations.checkmarx.sources.cxsca.CxScaScanDataSource;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class CxScaScanController implements DataController<CxScaScanDataSource.CxScaScanQuery> {

    public static final String SCANS = "scans";
    private final SinglePageStrategy<CxScaScan, CxScaScanDataSource.CxScaScanQuery> cxScaScanStrategy;
    private final ObjectMapper objectMapper;
    private final int onboardingScanInDays;

    @Builder
    public CxScaScanController(ObjectMapper mapper, CxScaScanDataSource dataSource, StorageDataSink sink,
                               int onboardingScanInDays) {
        this.objectMapper = mapper;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.cxScaScanStrategy = SinglePageStrategy.<CxScaScan, CxScaScanDataSource.CxScaScanQuery>builder()
                .objectMapper(objectMapper)
                .dataType(SCANS)
                .skipEmptyResults(true)
                .dataSource(dataSource)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, CxScaScanDataSource.CxScaScanQuery query) throws IngestException {
        query = fillNullValuesWithDefaults(query);
        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(cxScaScanStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query));
        return new ControllerIngestionResultList(results);
    }

    @Override
    public CxScaScanDataSource.CxScaScanQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        CxScaScanDataSource.CxScaScanQuery query = objectMapper.convertValue(arg, CxScaScanDataSource.CxScaScanQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    private CxScaScanDataSource.CxScaScanQuery fillNullValuesWithDefaults(CxScaScanDataSource.CxScaScanQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        return CxScaScanDataSource.CxScaScanQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .build();
    }
}
