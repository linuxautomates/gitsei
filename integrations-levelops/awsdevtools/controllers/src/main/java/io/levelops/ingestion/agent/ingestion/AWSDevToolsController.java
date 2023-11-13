package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.awsdevtools.models.AWSDevToolsQuery;
import io.levelops.integrations.awsdevtools.models.CBBuild;
import io.levelops.integrations.awsdevtools.models.CBBuildBatch;
import io.levelops.integrations.awsdevtools.models.CBProject;
import io.levelops.integrations.awsdevtools.sources.AWSDevToolsBuildBatchDataSource;
import io.levelops.integrations.awsdevtools.sources.AWSDevToolsBuildDataSource;
import io.levelops.integrations.awsdevtools.sources.AWSDevToolsProjectDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * AWSDevTools's implementation of the {@link DataController}
 */
@Log4j2
public class AWSDevToolsController implements DataController<AWSDevToolsQuery> {

    private static final String INTEGRATION_TYPE = "awsdevtools";
    private static final String DATATYPE_PROJECTS = "projects";
    private static final String DATATYPE_BUILDS = "builds";
    private static final String DATATYPE_BUILD_BATCHES = "build_batches";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<CBProject, AWSDevToolsQuery> projectPaginationStrategy;
    private final PaginationStrategy<CBBuild, AWSDevToolsQuery> buildPaginationStrategy;
    private final PaginationStrategy<CBBuildBatch, AWSDevToolsQuery> buildBatchPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public AWSDevToolsController(ObjectMapper objectMapper, AWSDevToolsProjectDataSource projectDataSource,
                                 AWSDevToolsBuildDataSource buildDataSource, AWSDevToolsBuildBatchDataSource buildBatchDataSource,
                                 StorageDataSink storageDataSink, int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.projectPaginationStrategy = StreamedPaginationStrategy.<CBProject, AWSDevToolsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_PROJECTS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(projectDataSource)
                .skipEmptyResults(true)
                .build();
        this.buildPaginationStrategy = StreamedPaginationStrategy.<CBBuild, AWSDevToolsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_BUILDS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(buildDataSource)
                .skipEmptyResults(true)
                .build();
        this.buildBatchPaginationStrategy = StreamedPaginationStrategy.<CBBuildBatch, AWSDevToolsQuery>builder()
                .objectMapper(objectMapper)
                .dataType(DATATYPE_BUILD_BATCHES)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(buildBatchDataSource)
                .skipEmptyResults(true)
                .build();
    }

    /**
     * Ingests the data for {@code jobId} with the {@code query}.
     * It calls the {@link AWSDevToolsProjectDataSource} for fetching the projects.
     * It calls the {@link AWSDevToolsBuildDataSource} for fetching the builds.
     * It calls the {@link AWSDevToolsBuildBatchDataSource} for fetching the build batches.
     *
     * @param jobId {@link String} id of the job for which the data needs to be ingested
     * @param query {@link AWSDevToolsQuery} describing the job
     * @return {@link ControllerIngestionResult} for the executed job
     * @throws IngestException for any exception during the ingestion process
     */
    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, AWSDevToolsQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        AWSDevToolsQuery awsDevToolsQuery = query.getFrom() == null ? AWSDevToolsQuery.builder()
                .from(Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)))
                .to(query.getTo())
                .regionIntegrationKey(query.getRegionIntegrationKey())
                .build() : query;
        StorageResult projectStorageResult = projectPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), awsDevToolsQuery);
        StorageResult buildStorageResult = buildPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), awsDevToolsQuery);
        StorageResult buildBatchStorageResult = buildBatchPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), awsDevToolsQuery);
        return new ControllerIngestionResultList(projectStorageResult, buildStorageResult, buildBatchStorageResult);
    }

    /**
     * parses the {@code arg}
     *
     * @param arg {@link Object} corresponding to the required {@link AWSDevToolsQuery}
     * @return {@link AWSDevToolsQuery} for the job
     */
    @Override
    public AWSDevToolsQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        AWSDevToolsQuery query = objectMapper.convertValue(arg, AWSDevToolsQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }
}
