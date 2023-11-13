package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class HelixCoreIterativeScanController implements DataController<HelixCoreIterativeQuery> {

    private final int onboardingInDays;
    private final int pageSizeInDays;
    private final ObjectMapper objectMapper;

    private final IntegrationController<HelixCoreIterativeQuery> helixCoreChangeListController;
    private final IntegrationController<HelixCoreIterativeQuery> helixCoreDepotController;

    @Builder
    public HelixCoreIterativeScanController(int onboardingInDays,
                                            ObjectMapper objectMapper,
                                            IntegrationController<HelixCoreIterativeQuery> helixCoreChangeListController,
                                            IntegrationController<HelixCoreIterativeQuery> helixCoreDepotController,
                                            int pageSizeInDays) {
        this.onboardingInDays = onboardingInDays;
        this.objectMapper = objectMapper;
        this.helixCoreChangeListController = helixCoreChangeListController;
        this.helixCoreDepotController = helixCoreDepotController;
        this.pageSizeInDays = pageSizeInDays;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, HelixCoreIterativeQuery iterativeScanQuery) throws IngestException {
        Date from = iterativeScanQuery.getFrom() != null ?
                iterativeScanQuery.getFrom() :
                Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));
        log.info("Helixcore iterative scan: integration={}, from={}, job_id={}",
                iterativeScanQuery.getIntegrationKey(), from, jobContext.getJobId());
        HelixCoreIterativeQuery query = HelixCoreIterativeQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(iterativeScanQuery.getTo())
                .pageSizeInDays(pageSizeInDays)
                .build();
        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(helixCoreChangeListController.ingest(jobContext, query));
        boolean fetchDepots = Boolean.TRUE.equals(query.getFetchDepots());
        boolean isEverythingEmpty = results.stream()
                .map(StorageResult.class::cast)
                .map(StorageResult::getRecords)
                .allMatch(CollectionUtils::isEmpty);
        if (!fetchDepots && isEverythingEmpty) {
            return new EmptyIngestionResult();
        }
        results.add(helixCoreDepotController.ingest(jobContext, query));
        return new ControllerIngestionResultList(results);
    }

    @Override
    public HelixCoreIterativeQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, HelixCoreIterativeQuery.class);
    }
}
