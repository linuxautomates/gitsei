package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.integrations.circleci.models.CircleCIIngestionQuery;
import io.levelops.integrations.circleci.source.CircleCIBuildDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * CircleCI's implementation of the {@link DataController}
 * {@link StorageResult} for Projects and Builds are ingested.
 */
@Log4j2
public class CircleCIController implements DataController<CircleCIIngestionQuery> {

    private static final String BUILDS_DATATYPE = "builds";
    private static final String INTEGRATION_TYPE = "circleci";
    private static final String CIRCLECI_REPOS_METADATA_FIELD = "repos";

    private static final String CIRCLECI_FETCH_LOGS_METADATA_FIELD = "fetch_action_logs";

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final int onboardingScanInDays;
    private final PaginationStrategy<CircleCIBuild, CircleCIIngestionQuery> buildPaginationStrategy;

    @Builder
    public CircleCIController(ObjectMapper objectMapper,
                              CircleCIBuildDataSource buildDataSource, StorageDataSink storageDataSink,
                              InventoryService inventoryService,
                              int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.buildPaginationStrategy = StreamedPaginationStrategy.<CircleCIBuild, CircleCIIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(BUILDS_DATATYPE)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(buildDataSource)
                .skipEmptyResults(true)
                .outputPageSize(1)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, CircleCIIngestionQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        query = updateQuery(query);
        List<ControllerIngestionResult> result = new ArrayList<>();
        result.add(buildPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query));
        return new ControllerIngestionResultList(result);
    }

    @Override
    public CircleCIIngestionQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        CircleCIIngestionQuery query = objectMapper.convertValue(arg, CircleCIIngestionQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    // TODO: Move from to trigger

    private CircleCIIngestionQuery updateQuery(CircleCIIngestionQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        List<String> repos = List.of();
        boolean fetchActionLogs = false;
        try{
            Integration integration = inventoryService.getIntegration(query.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            if(metadata.containsKey(CIRCLECI_REPOS_METADATA_FIELD)){
                String reposCommaList = (String) metadata.get(CIRCLECI_REPOS_METADATA_FIELD);
                if (StringUtils.isNotBlank(reposCommaList)) {
                    repos = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(reposCommaList);
                    log.info("Scanning specific CircleCI repos: {}", repos);
                }
            }
            if(metadata.containsKey(CIRCLECI_FETCH_LOGS_METADATA_FIELD)){
                fetchActionLogs = BooleanUtils.isTrue((Boolean) metadata.get(CIRCLECI_FETCH_LOGS_METADATA_FIELD));
            }
        }
        catch (InventoryException e){
            log.warn("Failed to get integration for key: " + query.getIntegrationKey(), e);
        }
        return CircleCIIngestionQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .to(query.getTo())
                .repositories(repos)
                .shouldFetchActionLogs(fetchActionLogs)
                .build();
    }
}
