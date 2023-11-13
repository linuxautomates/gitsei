package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.gerrit.models.AccountInfo;
import io.levelops.integrations.gerrit.models.GerritQuery;
import io.levelops.integrations.gerrit.models.GroupInfo;
import io.levelops.integrations.gerrit.sources.GerritAccountsDataSource;
import io.levelops.integrations.gerrit.sources.GerritGroupDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Log4j2
public class GerritIngestionController implements DataController<GerritQuery> {

    private static final String GERRIT = "gerrit";
    private static final String GROUPS = "groups";
    private static final String ACCOUNTS = "accounts";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<GroupInfo, GerritQuery> groupPaginationStrategy;
    private final PaginationStrategy<AccountInfo, GerritQuery> accountPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public GerritIngestionController(ObjectMapper objectMapper,
                                     GerritGroupDataSource gerritGroupDataSource,
                                     GerritAccountsDataSource gerritAccountsDataSource,
                                     StorageDataSink storageDataSink, int onboardingScanInDays) {
        this.objectMapper = objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.groupPaginationStrategy = StreamedPaginationStrategy.<GroupInfo, GerritQuery>builder()
                .objectMapper(objectMapper)
                .dataType(GROUPS)
                .integrationType(GERRIT)
                .storageDataSink(storageDataSink)
                .dataSource(gerritGroupDataSource)
                .skipEmptyResults(true)
                .build();
        this.accountPaginationStrategy = StreamedPaginationStrategy.<AccountInfo, GerritQuery>builder()
                .objectMapper(objectMapper)
                .dataType(ACCOUNTS)
                .integrationType(GERRIT)
                .storageDataSink(storageDataSink)
                .dataSource(gerritAccountsDataSource)
                .skipEmptyResults(true)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, GerritQuery query) throws IngestException {
        log.debug("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        if (query.getAfter() == null) {
            query = query.toBuilder()
                    .after(Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)))
                    .build();
        }
        StorageResult groupsStorageResult = groupPaginationStrategy
                .ingestAllPages(jobContext, query.getIntegrationKey(), query);
        StorageResult accountsStorageResult = accountPaginationStrategy
                .ingestAllPages(jobContext, query.getIntegrationKey(), query);
        return new ControllerIngestionResultList(groupsStorageResult, accountsStorageResult);
    }

    @Override
    public GerritQuery parseQuery(Object arg) {
        log.debug("parseQuery: received args: {}", arg);
        GerritQuery query = objectMapper.convertValue(arg, GerritQuery.class);
        log.debug("parseQuery: parsed query successfully: {}", query);
        return query;
    }
}
