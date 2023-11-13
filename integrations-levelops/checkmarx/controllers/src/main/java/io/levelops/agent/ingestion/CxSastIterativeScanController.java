package io.levelops.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.checkmarx.models.cxsast.CxSastIterativeScanQuery;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastProjectDataSource;
import io.levelops.integrations.checkmarx.sources.cxsast.CxSastScanDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * CxSast's implementation of the {@link DataController}
 * {@link StorageResult} for Projects and Builds are ingested.
 */
@Log4j2
public class CxSastIterativeScanController implements DataController<CxSastIterativeScanQuery> {

    private static final int CXSAST_ONBOARDING_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final IntegrationController<CxSastProjectDataSource.CxSastProjectQuery> projectController;
    private final IntegrationController<CxSastScanDataSource.CxSastScanQuery> scanController;
    private final InventoryService inventoryService;
    private final int onboardingInDays;

    @Builder
    public CxSastIterativeScanController(ObjectMapper objectMapper,
                                         IntegrationController<CxSastProjectDataSource.CxSastProjectQuery> projectController,
                                         IntegrationController<CxSastScanDataSource.CxSastScanQuery> scanController,
                                         InventoryService inventoryService,
                                         Integer onboardingInDays) {
        this.inventoryService = inventoryService;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, CXSAST_ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.projectController = projectController;
        this.scanController = scanController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, CxSastIterativeScanQuery iterativeScanQuery)
            throws IngestException {
        boolean onboarding = iterativeScanQuery.getFrom() == null;
        Date from = onboarding ? Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays))) :
                iterativeScanQuery.getFrom();
        Date to = iterativeScanQuery.getTo();
        log.info("CxSast iterative scan: integration={}, from={}, to={}, job_id={}",
                iterativeScanQuery.getIntegrationKey(), from, to, jobContext.getJobId());

        CxSastProjectDataSource.CxSastProjectQuery projectQuery = CxSastProjectDataSource.CxSastProjectQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .build();

        CxSastScanDataSource.CxSastScanQuery scanQuery = CxSastScanDataSource.CxSastScanQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .last(iterativeScanQuery.getLast())
                .projectId(iterativeScanQuery.getProjectId())
                .scanStatus(iterativeScanQuery.getScanStatus())
                .build();

        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(projectController.ingest(jobContext, projectQuery));
        results.add(scanController.ingest(jobContext, scanQuery));
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
    public CxSastIterativeScanQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        CxSastIterativeScanQuery query = objectMapper.convertValue(arg, CxSastIterativeScanQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;

    }
}
