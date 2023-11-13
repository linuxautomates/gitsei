package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class CoverityIterativeScanController implements DataController<CoverityIterativeScanQuery> {

    private final ObjectMapper objectMapper;
    private final IntegrationController<CoverityIterativeScanQuery> defectsController;
    private final int onBoardingInDays;

    @Builder
    public CoverityIterativeScanController(ObjectMapper objectMapper,
                                           IntegrationController<CoverityIterativeScanQuery> defectsController,
                                           int onBoardingInDays) {
        this.objectMapper = objectMapper;
        this.defectsController = defectsController;
        this.onBoardingInDays = onBoardingInDays;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, CoverityIterativeScanQuery coverityIterativeScanQuery) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), coverityIterativeScanQuery);
        coverityIterativeScanQuery = fillNullValuesWithDefaults(coverityIterativeScanQuery);
        CoverityIterativeScanQuery scanQuery;
        if (coverityIterativeScanQuery.getFrom() == null) {
            scanQuery = CoverityIterativeScanQuery.builder()
                    .from(Date.from(Instant.now().minus(onBoardingInDays, ChronoUnit.DAYS)))
                    .integrationKey(coverityIterativeScanQuery.getIntegrationKey())
                    .build();
        } else {
            scanQuery = coverityIterativeScanQuery;
        }
        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(defectsController.ingest(jobContext, scanQuery));
        boolean isEverythingEmpty = results.stream()
                .map(StorageResult.class::cast)
                .map(StorageResult::getRecords)
                .allMatch(CollectionUtils::isEmpty);
        if (isEverythingEmpty) {
            return new EmptyIngestionResult();
        }
        return new ControllerIngestionResultList(results);
    }

    @Override
    public CoverityIterativeScanQuery parseQuery(Object o) {
        log.debug("parseQuery: received args: {}", o);
        CoverityIterativeScanQuery query = objectMapper.convertValue(o, CoverityIterativeScanQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    private CoverityIterativeScanQuery fillNullValuesWithDefaults(CoverityIterativeScanQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onBoardingInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        Date to = query.getTo() == null ? Date.from(Instant.now()) : query.getTo();
        return CoverityIterativeScanQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .to(to)
                .build();
    }
}
