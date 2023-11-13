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
import io.levelops.integrations.okta.models.OktaGroup;
import io.levelops.integrations.okta.models.OktaScanQuery;
import io.levelops.integrations.okta.models.OktaUser;
import io.levelops.integrations.okta.sources.OktaGroupsDataSource;
import io.levelops.integrations.okta.sources.OktaUsersDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Okta's implementation of the {@link DataController}
 */
@Log4j2
public class OktaIngestionController implements DataController<OktaScanQuery> {

    private static final String INTEGRATION_TYPE = "okta";
    private static final String GROUPS_DATA_TYPE = "groups";
    private static final String USERS_DATA_TYPE = "users";
    private static final String USER_TYPE = "user type";
    private static final String LINKED_OBJECT_DEFINITIONS = "linked object definitions";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<OktaGroup, OktaScanQuery> groupPaginationStrategy;
    private final PaginationStrategy<OktaUser, OktaScanQuery> userPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public OktaIngestionController(ObjectMapper objectMapper, OktaGroupsDataSource groupsDataSource,
                                   OktaUsersDataSource usersDataSource,
                                   StorageDataSink storageDataSink, int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 90;
        this.groupPaginationStrategy = StreamedPaginationStrategy.<OktaGroup, OktaScanQuery>builder()
                .objectMapper(objectMapper)
                .dataType(GROUPS_DATA_TYPE)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(groupsDataSource)
                .skipEmptyResults(true)
                .build();
        this.userPaginationStrategy = StreamedPaginationStrategy.<OktaUser, OktaScanQuery>builder()
                .objectMapper(objectMapper)
                .dataType(USERS_DATA_TYPE)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(usersDataSource)
                .skipEmptyResults(true)
                .build();
    }


    @Override
    public OktaScanQuery parseQuery(Object arg) {
        log.info("parseQuery: received args: {}", arg);
        OktaScanQuery query = objectMapper.convertValue(arg, OktaScanQuery.class);
        log.info("parseQuery: parsed query successfully: {}", query);
        return query;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, OktaScanQuery query) throws IngestException {
        log.info("ingest: ingesting data for jobId: {} with query: {}", jobContext.getJobId(), query);
        OktaScanQuery scanQuery;
        if (query.getFrom() == null && query.getCursor() == null) {
            scanQuery = OktaScanQuery.builder()
                    .from(Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS)))
                    .integrationKey(query.getIntegrationKey())
                    .build();
        } else {
            scanQuery = query;
        }
        StorageResult groupsStorageResult = groupPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), scanQuery);
        StorageResult userStorageResult = userPaginationStrategy.ingestAllPages(jobContext,
                query.getIntegrationKey(), scanQuery);
        return new ControllerIngestionResultList(groupsStorageResult, userStorageResult);
    }
}
