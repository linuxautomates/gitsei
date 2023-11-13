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
import io.levelops.integrations.harnessng.models.HarnessNGIngestionQuery;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.source.HarnessNGEnrichPipelineDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HarnessNG's implementation of the {@link DataController}
 * {@link StorageResult} for Repos and its enrichments are ingested.
 */
@Log4j2
public class HarnessNGController implements DataController<HarnessNGIngestionQuery> {

    private static final String PIPELINE_DATATYPE = "pipeline";
    private static final String ORGANIZATIONS_METADATA_FIELD = "organization";
    private static final String PROJECTS_METADATA_FIELD = "project";
    private static final String ACCOUNT_IDENTIFIER = "accountId";
    private static final String INTEGRATION_TYPE = "harnessng";
    private static final long DEFAULT_EXECUTED_PIPELINES_TIME_WINDOW_IN_MILLIS = TimeUnit.DAYS.toMillis(7);

    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    private final int onboardingScanInDays;
    private final long executedPipelinesTimeWindowInMillis;
    private final PaginationStrategy<HarnessNGPipelineExecution, HarnessNGIngestionQuery> repoPaginationStrategy;

    @Builder
    public HarnessNGController(ObjectMapper objectMapper, HarnessNGEnrichPipelineDataSource pipelineDataSource,
                               InventoryService inventoryService,
                               StorageDataSink storageDataSink,
                               int onboardingScanInDays,
                               Long executedPipelinesTimeWindowInMillis) {
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 30;
        this.repoPaginationStrategy = StreamedPaginationStrategy.<HarnessNGPipelineExecution, HarnessNGIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(PIPELINE_DATATYPE)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(pipelineDataSource)
                .skipEmptyResults(true)
                .outputPageSize(1000)
                .build();
        this.executedPipelinesTimeWindowInMillis = ObjectUtils.firstNonNull(executedPipelinesTimeWindowInMillis, DEFAULT_EXECUTED_PIPELINES_TIME_WINDOW_IN_MILLIS);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, HarnessNGIngestionQuery query) throws IngestException {
        query = updateQuery(query);
        List<ControllerIngestionResult> result = new ArrayList<>();
        result.add(repoPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), query));
        return new ControllerIngestionResultList(result);
    }

    @Override
    public HarnessNGIngestionQuery parseQuery(Object arg) {
        log.debug("parseQuery: received args: {}", arg);
        HarnessNGIngestionQuery query = objectMapper.convertValue(arg, HarnessNGIngestionQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    private HarnessNGIngestionQuery updateQuery(HarnessNGIngestionQuery query) {
        Date from = query.getFrom() == null ? Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)) :
                Date.from(Instant.ofEpochMilli(query.getFrom().getTime() - executedPipelinesTimeWindowInMillis));
        List<String> orgs = List.of();
        List<String> projects = List.of();
        String accountId = "";
        try{
            Integration integration = inventoryService.getIntegration(query.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            if(metadata.containsKey(ORGANIZATIONS_METADATA_FIELD)){
                String orgsCommaList = (String) metadata.get(ORGANIZATIONS_METADATA_FIELD);
                if (StringUtils.isNotBlank(orgsCommaList)) {
                    orgs = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(orgsCommaList);
                    log.info("Scanning specific HarnessNG orgs: {}", orgs);
                }
            }
            if(metadata.containsKey(PROJECTS_METADATA_FIELD)){
                String projectsCommaList = (String) metadata.get(PROJECTS_METADATA_FIELD);
                if (StringUtils.isNotBlank(projectsCommaList)) {
                    projects = Splitter.on(",").omitEmptyStrings().trimResults().splitToList(projectsCommaList);
                    log.info("Scanning specific HarnessNG projects: {}", projects);
                }
            }
            if(metadata.containsKey(ACCOUNT_IDENTIFIER)){
                accountId = metadata.get(ACCOUNT_IDENTIFIER).toString();
            }
        }
        catch (InventoryException e){
            log.warn("Failed to get integration for key: " + query.getIntegrationKey(), e);
        }
        return HarnessNGIngestionQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .from(from)
                .accountIdentifier(accountId)
                .organizations(orgs)
                .projects(projects)
                .to(query.getTo())
                .build();
    }
}
