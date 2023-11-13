package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Splitter;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.droneci.models.DroneCIIngestionQuery;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import io.levelops.integrations.droneci.source.DroneCIEnrichRepoDataSource;
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
 * DroneCI's implementation of the {@link DataController}
 * {@link StorageResult} for Repos and its enrichments are ingested.
 */
@Log4j2
public class DroneCIController implements DataController<DroneCIIngestionQuery> {

    private static final String REPOS_DATATYPE = "repository";
    private static final String DRONCI_REPOS_METADATA_FIELD = "repos";
    private static final String DRONCI_EXCLUDE_REPOS_METADATA_FIELD = "exclude_repos";

    private static final String DRONCI_FETCH_LOGS_METADATA_FIELD = "fetch_steplogs";
    private static final String INTEGRATION_TYPE = "droneci";

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final int onboardingScanInDays;
    private final PaginationStrategy<DroneCIEnrichRepoData, DroneCIIngestionQuery> repoPaginationStrategy;

    @Builder
    public DroneCIController(ObjectMapper objectMapper, DroneCIEnrichRepoDataSource repoDataSource,
                             InventoryService inventoryService,
                             StorageDataSink storageDataSink,
                             int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.repoPaginationStrategy = StreamedPaginationStrategy.<DroneCIEnrichRepoData, DroneCIIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(REPOS_DATATYPE)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(repoDataSource)
                .skipEmptyResults(true)
                .outputPageSize(1)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, DroneCIIngestionQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        query = updateQuery(query);
        List<ControllerIngestionResult> result = new ArrayList<>();
        result.add(repoPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query));
        return new ControllerIngestionResultList(result);
    }

    @Override
    public DroneCIIngestionQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        DroneCIIngestionQuery query = objectMapper.convertValue(arg, DroneCIIngestionQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    private DroneCIIngestionQuery updateQuery(DroneCIIngestionQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)) :
                query.getFrom();
        List<String> repos = List.of();
        List<String> excludeRepos = List.of();
        boolean fetchStepLogs = false;
        try {
            Integration integration = inventoryService.getIntegration(query.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            repos = parseListFromMetadata(DRONCI_REPOS_METADATA_FIELD, metadata);
            excludeRepos = parseListFromMetadata(DRONCI_EXCLUDE_REPOS_METADATA_FIELD, metadata);
            if (repos.size() > 0) {
                log.info("Scanning specific DroneCI repos: {}", repos);
            }
            if (excludeRepos.size() > 0) {
                log.info("Excluding specific DroneCI repos: {}", excludeRepos);
            }
            if (metadata.containsKey(DRONCI_FETCH_LOGS_METADATA_FIELD)) {
                fetchStepLogs = BooleanUtils.isTrue((Boolean) metadata.get(DRONCI_FETCH_LOGS_METADATA_FIELD));
            }
        } catch (InventoryException e) {
            log.warn("Failed to get integration for key: " + query.getIntegrationKey(), e);
        }
        return DroneCIIngestionQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .repositories(repos)
                .excludeRepositories(excludeRepos)
                .shouldFetchStepLogs(fetchStepLogs)
                .to(query.getTo())
                .build();
    }

    private static List<String> parseListFromMetadata(String fieldName, Map<String, Object> metadata) {
        List<String> parsedList = List.of();
        if (metadata.containsKey(fieldName)) {
            String reposCommaList = (String) metadata.get(fieldName);
            parsedList = CommaListSplitter.split(reposCommaList);
        }
        return parsedList;
    }
}
