package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.github.actions.models.GithubActionsIngestionQuery;
import io.levelops.integrations.github.actions.sources.GithubActionsWorkflowDataSource;
import io.levelops.integrations.github_actions.models.GithubActionsEnrichedWorkflowRun;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Log4j2
public class GithubActionsController implements DataController<GithubActionsIngestionQuery> {

    private static final int GITHUB_ONBOARDING_IN_DAYS = 7;
    private static final String WORKFLOW_RUN_DATATYPE = "workflow_run";
    private static final String INTEGRATION_TYPE = "github_actions";
    private static final String GITHUB_APP_ID_METADATA_FIELD = "app_id";
    private static final String GITHUB_REPOS_METADATA_FIELD = "repos";
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final int defaultOnboardingInDays;
    private final PaginationStrategy<GithubActionsEnrichedWorkflowRun, GithubActionsIngestionQuery> workflowPaginationStrategy;

    @Builder
    public GithubActionsController(ObjectMapper objectMapper,
                                   GithubActionsWorkflowDataSource workflowDataSource,
                                   InventoryService inventoryService,
                                   StorageDataSink storageDataSink,
                                   Integer onboardingInDays) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.defaultOnboardingInDays = MoreObjects.firstNonNull(onboardingInDays, GITHUB_ONBOARDING_IN_DAYS);
        this.workflowPaginationStrategy = StreamedPaginationStrategy.<GithubActionsEnrichedWorkflowRun, GithubActionsIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(WORKFLOW_RUN_DATATYPE)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(workflowDataSource)
                .skipEmptyResults(true)
                .outputPageSize(10)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GithubActionsIngestionQuery query) throws IngestException {
        List<ControllerIngestionResult> result = new ArrayList<>();
        query = updateIngestionQuery(query);
        result.add(workflowPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query));

        return new ControllerIngestionResultList(result);
    }

    @Override
    public GithubActionsIngestionQuery parseQuery(Object arg) {
        log.debug("parseQuery: received args: {}", arg);
        GithubActionsIngestionQuery query = objectMapper.convertValue(arg, GithubActionsIngestionQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    private GithubActionsIngestionQuery updateIngestionQuery(GithubActionsIngestionQuery query) {
        List<String> repos = null;
        boolean isGithubApp = false;
        Date from = query.getFrom();
        Date to = query.getTo();
        boolean onboarding = query.getFrom() == null;;

        try {
            Integration integration = inventoryService.getIntegration(query.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());

            Integer onboardingInDays = (Integer) metadata.getOrDefault("onboarding", defaultOnboardingInDays);
            String appId = (String) metadata.get(GITHUB_APP_ID_METADATA_FIELD);
            if (StringUtils.isNotBlank(appId)) {
                log.info("github_app=true");
                isGithubApp = true;
            }
            // get comma separated repoIds from integration metadata
            String reposCommaList = (String) metadata.get(GITHUB_REPOS_METADATA_FIELD);
            if (StringUtils.isNotBlank(reposCommaList)) {
                repos = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(reposCommaList);
                log.info("Scanning specific Github repos: {}", repos);
            }

            from = query.getFrom() != null
                    ? query.getFrom()
                    : Date.from(Instant.now().minus(Duration.ofDays(onboardingInDays)));
            to = query.getTo() != null
                    ? query.getTo()
                    : new Date();
        } catch (InventoryException e) {
            throw new RuntimeException(e);
        }


        return GithubActionsIngestionQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .to(to)
                .onboarding(onboarding)
                .repos(repos)
                .githubApp(isGithubApp).build();
    }
}
