package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.controllers.generic.IntegrationController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import io.levelops.integrations.sonarqube.sources.SonarQubeProjectDataSource.SonarQubeProjectQuery;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sonarqube's implementation of the {@link DataController}
 */
@Log4j2
public class SonarQubeIterativeScanController implements DataController<SonarQubeIterativeScanQuery> {

    private static final int SONARQUBE_ONBOARDING_IN_DAYS = 360;
    private static final String PROJECT_KEYS_METADATA_FIELD = "project_keys";
    public static final String USE_PRIVILEGED_APIS = "use_privileged_APIs";

    private final InventoryService inventoryService;
    private final int onboardingInDays;
    private final ObjectMapper objectMapper;

    private final IntegrationController<SonarQubeIterativeScanQuery> qualityGatesController;
    private final IntegrationController<SonarQubeIterativeScanQuery> userController;
    private final IntegrationController<SonarQubeIterativeScanQuery> userGroupController;
    private final IntegrationController<SonarQubeIterativeScanQuery> issueController;
    private final IntegrationController<SonarQubeProjectQuery> projectController;
    private final IntegrationController<SonarQubeProjectQuery> analysesController;
    private final IntegrationController<SonarQubeProjectQuery> prIssueController;
    private final IntegrationController<SonarQubeProjectQuery> branchController;

    @Builder
    public SonarQubeIterativeScanController(ObjectMapper objectMapper,
                                            InventoryService inventoryService,
                                            IntegrationController<SonarQubeProjectQuery>
                                                    analysesController,
                                            IntegrationController<SonarQubeProjectQuery>
                                                    prIssueController,
                                            IntegrationController<SonarQubeProjectQuery>
                                                    branchController,
                                            IntegrationController<SonarQubeProjectQuery>
                                                    projectController,
                                            IntegrationController<SonarQubeIterativeScanQuery> qualityGatesController,
                                            IntegrationController<SonarQubeIterativeScanQuery> userController,
                                            IntegrationController<SonarQubeIterativeScanQuery> userGroupController,
                                            IntegrationController<SonarQubeIterativeScanQuery> issueController,
                                            Integer onboardingInDays) {
        this.inventoryService = inventoryService;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, SONARQUBE_ONBOARDING_IN_DAYS);
        this.objectMapper = objectMapper;
        this.qualityGatesController = qualityGatesController;
        this.userController = userController;
        this.userGroupController = userGroupController;
        this.issueController = issueController;
        this.branchController = branchController;
        this.projectController = projectController;
        this.analysesController = analysesController;
        this.prIssueController = prIssueController;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext,
                                            SonarQubeIterativeScanQuery iterativeScanQuery) throws IngestException {

        // get integration metadata
        Set<String> projectKeys;
        Boolean usePrivilegedAPIs;
        try {
            Integration integration = inventoryService.getIntegration(iterativeScanQuery.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            projectKeys =  CommaListSplitter.splitToStream((String) metadata.get(PROJECT_KEYS_METADATA_FIELD))
                    .collect(Collectors.toSet());
            usePrivilegedAPIs = (Boolean) metadata.getOrDefault(USE_PRIVILEGED_APIS, false);
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " + iterativeScanQuery.getIntegrationKey(), e);
        }
        Date from = iterativeScanQuery.getFrom() != null ?
                iterativeScanQuery.getFrom() :
                Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));

        Date to = iterativeScanQuery.getTo() != null
                ? iterativeScanQuery.getTo()
                : new Date();

        SonarQubeProjectQuery query;
        query = SonarQubeProjectQuery.builder()
                .integrationKey(iterativeScanQuery.getIntegrationKey())
                .from(from)
                .to(to)
                .projectKeys(projectKeys)
                .usePrivilegedAPIs(usePrivilegedAPIs)
                .build();
        log.info("SonarQube iterative scan: integration={}, from={}, to={}, job_id={}, projects={}",
                iterativeScanQuery.getIntegrationKey(), from, to, jobContext.getJobId(), projectKeys);

        iterativeScanQuery = iterativeScanQuery.toBuilder()
                .projectKeys(projectKeys)
                .from(from)
                .to(to)
                .usePrivilegedAPIs(usePrivilegedAPIs)
                .build();

        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(projectController.ingest(jobContext, query));
        results.add(analysesController.ingest(jobContext, query));
        results.add(prIssueController.ingest(jobContext, query));
        results.add(branchController.ingest(jobContext, query));
        results.add(issueController.ingest(jobContext, iterativeScanQuery));
        if (iterativeScanQuery.getFetchOnce()) {
            results.add(qualityGatesController.ingest(jobContext, iterativeScanQuery));
            if (usePrivilegedAPIs) {
                results.add(userController.ingest(jobContext, iterativeScanQuery));
                results.add(userGroupController.ingest(jobContext, iterativeScanQuery));
            }
        }
        return new ControllerIngestionResultList(results);
    }

    @Override
    public SonarQubeIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, SonarQubeIterativeScanQuery.class);
    }

}