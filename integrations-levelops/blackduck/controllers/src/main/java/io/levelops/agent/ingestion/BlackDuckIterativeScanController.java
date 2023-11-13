package io.levelops.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.blackduck.models.BlackDuckIterativeScanQuery;
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
public class BlackDuckIterativeScanController implements DataController<BlackDuckIterativeScanQuery> {

    private final ObjectMapper objectMapper;
    private final IntegrationController<BlackDuckIterativeScanQuery> projectController;
    private final int onBoardingInDays;

    @Builder
    public BlackDuckIterativeScanController(ObjectMapper objectMapper,
                                            IntegrationController<BlackDuckIterativeScanQuery> projectController,
                                            int onBoardingInDays) {
        this.objectMapper = objectMapper;
        this.projectController = projectController;
        this.onBoardingInDays = onBoardingInDays;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, BlackDuckIterativeScanQuery blackDuckIterativeScanQuery) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), blackDuckIterativeScanQuery);
        blackDuckIterativeScanQuery = fillNullValuesWithDefaults(blackDuckIterativeScanQuery);
        BlackDuckIterativeScanQuery scanQuery;
        if (blackDuckIterativeScanQuery.getFrom() == null) {
            scanQuery = BlackDuckIterativeScanQuery.builder()
                    .from(Date.from(Instant.now().minus(onBoardingInDays, ChronoUnit.DAYS)))
                    .integrationKey(blackDuckIterativeScanQuery.getIntegrationKey())
                    .build();
        } else {
            scanQuery = blackDuckIterativeScanQuery;
        }
        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(projectController.ingest(jobContext, scanQuery));
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
    public BlackDuckIterativeScanQuery parseQuery(Object o) {
        log.debug("parseQuery: received args: {}", o);
        BlackDuckIterativeScanQuery query = objectMapper.convertValue(o, BlackDuckIterativeScanQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    private BlackDuckIterativeScanQuery fillNullValuesWithDefaults(BlackDuckIterativeScanQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onBoardingInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        Date to = query.getTo() == null ? Date.from(Instant.now()) : query.getTo();
        return BlackDuckIterativeScanQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .to(to)
                .build();
    }
}
