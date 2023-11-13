package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.NumberedPaginationStrategy;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.pagerduty.client.PagerDutyClientFactory;
import io.levelops.integrations.pagerduty.models.PagerDutyAlertsPage;
import io.levelops.integrations.pagerduty.models.PagerDutyIncidentsPage;
import io.levelops.integrations.pagerduty.models.PagerDutyIngestionDataType;
import io.levelops.integrations.pagerduty.models.PagerDutyIterativeScanQuery;
import io.levelops.integrations.pagerduty.models.PagerDutyLogEntriesPage;
import io.levelops.integrations.pagerduty.models.PagerDutyServicesPage;
import io.levelops.integrations.pagerduty.models.PagerDutyUsersPage;
import io.levelops.integrations.pagerduty.sources.PagerDutyApiDataSource;
import io.levelops.integrations.pagerduty.sources.PagerDutyIncidentsDataSource;
import io.levelops.integrations.pagerduty.sources.PagerDutyLogEntriesDataSource;
import io.levelops.integrations.pagerduty.utils.PagerDutyUtils;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Map;

@Log4j2
@SuppressWarnings("rawtypes")
public class PagerDutyIterativeScanController implements DataController<PagerDutyIterativeScanQuery> {

    private final static int PAGE_SIZE = 100;

    private final ObjectMapper objectMapper;
    private final int onboardingInDays;
    private final PaginationStrategy<Map, PagerDutyIncidentsPage.Query> incidentsIngestor;
    private final PaginationStrategy<Map, PagerDutyAlertsPage.Query> alertsIngestor;
    private final PaginationStrategy<Map, PagerDutyLogEntriesPage.Query> logEntriesIngestor;
    private final PaginationStrategy<Map, PagerDutyServicesPage.Query> servicesIngestor;
    private final PaginationStrategy<Map, PagerDutyUsersPage.Query> usersIngestor;

    @Builder
    public PagerDutyIterativeScanController(
            final ObjectMapper objectMapper,
            final PagerDutyClientFactory clientFactory,
            final StorageDataSink storageDataSink,
            PagerDutyLogEntriesDataSource logEntriesDataSource,
            PagerDutyIncidentsDataSource incidentsDataSource,
            @Nullable Integer onboardingInDays,
            @Nullable Integer outputPageSize) {
        this.objectMapper = objectMapper;
        this.onboardingInDays = MoreObjects.firstNonNull(onboardingInDays, 90);

        this.alertsIngestor = NumberedPaginationStrategy.<Map, PagerDutyAlertsPage.Query>builder()
                .integrationType("pagerduty")
                .dataType(PagerDutyIngestionDataType.ALERT.getIngestionPluralDataType())
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .pageDataSupplier((query, page) ->
                        new PagerDutyApiDataSource<PagerDutyAlertsPage, PagerDutyAlertsPage.Query>(clientFactory, PagerDutyAlertsPage.class)
                                .fetchMany(PagerDutyAlertsPage.Query.builder()
                                        .limit(PAGE_SIZE)
                                        .since(query.getSince())
                                        .until(query.getUntil())
                                        .offset(page.getPageNumber() * PAGE_SIZE)
                                        .integrationKey(query.getIntegrationKey())
                                        .build()))
                .skipEmptyResults(true)
                .outputPageSize(outputPageSize)
                .build();

        this.incidentsIngestor = StreamedPaginationStrategy.<Map, PagerDutyIncidentsPage.Query>builder()
                .integrationType("pagerduty")
                .dataType(PagerDutyIngestionDataType.INCIDENT.getIngestionPluralDataType())
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(incidentsDataSource)
                .skipEmptyResults(true)
                .build();

        this.logEntriesIngestor = StreamedPaginationStrategy.<Map, PagerDutyLogEntriesPage.Query>builder()
                .integrationType("pagerduty")
                .dataType(PagerDutyIngestionDataType.LOG_ENTRY.getIngestionPluralDataType())
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(logEntriesDataSource)
                .skipEmptyResults(true)
                .build();

        this.servicesIngestor = NumberedPaginationStrategy.<Map, PagerDutyServicesPage.Query>builder()
                .integrationType("pagerduty")
                .dataType(PagerDutyIngestionDataType.SERVICE.getIngestionPluralDataType())
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .pageDataSupplier((query, page) ->
                        new PagerDutyApiDataSource<PagerDutyServicesPage, PagerDutyServicesPage.Query>(clientFactory, PagerDutyServicesPage.class)
                                .fetchMany(PagerDutyServicesPage.Query.builder()
                                        .limit(PAGE_SIZE)
                                        .offset(page.getPageNumber() * PAGE_SIZE)
                                        .integrationKey(query.getIntegrationKey())
                                        .build()))
                .skipEmptyResults(true)
                .outputPageSize(outputPageSize)
                .build();

        this.usersIngestor = NumberedPaginationStrategy.<Map, PagerDutyUsersPage.Query>builder()
                .integrationType("pagerduty")
                .dataType(PagerDutyIngestionDataType.USER.getIngestionPluralDataType())
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .pageDataSupplier((query, page) ->
                        new PagerDutyApiDataSource<PagerDutyUsersPage, PagerDutyUsersPage.Query>(clientFactory, PagerDutyUsersPage.class)
                                .fetchMany(PagerDutyUsersPage.Query.builder()
                                        .limit(PAGE_SIZE)
                                        .offset(page.getPageNumber() * PAGE_SIZE)
                                        .integrationKey(query.getIntegrationKey())
                                        .build()))
                .skipEmptyResults(true)
                .outputPageSize(outputPageSize)
                .build();
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, PagerDutyIterativeScanQuery query) throws IngestException {
        log.info("Starting ingestion for the job '{}''", jobContext.getJobId());
        String to = (query.getTo() != null) ? PagerDutyUtils.formatDate(query.getTo()) : PagerDutyUtils.formatDate(Instant.now());
        // onboarding
        String from;
        boolean iterative = true;
        if (query.getFrom() == null) {
            log.info("Running onboarding....");
            // Onboarding
            // get all existing incidents in any state
            // get all existing alerts
            from = PagerDutyUtils.formatDate(Date.from(Instant.now().minus(onboardingInDays, ChronoUnit.DAYS)));
            iterative = false;
        } else {
            // Iterative
            // get new incidents
            // get new alerts
            // get logs with incidents
            from = PagerDutyUtils.formatDate(query.getFrom());
        }
        log.info("Iteration: from={}, to={}, iterative={}", from, to, iterative);

        // get all existing services
        StorageResult servicesResults = servicesIngestor.ingestAllPages(jobContext, query.getIntegrationKey(), PagerDutyServicesPage.Query.builder()
                .integrationKey(query.getIntegrationKey())
                .limit(PAGE_SIZE)
                .offset(0)
                .build());

        // get all existing users
        StorageResult usersResults = usersIngestor.ingestAllPages(jobContext, query.getIntegrationKey(), PagerDutyUsersPage.Query.builder()
                .integrationKey(query.getIntegrationKey())
                .since(from)
                .until(to)
                .limit(PAGE_SIZE)
                .offset(0)
                .build());

        // get incidents for the time range
        StorageResult incidentsResults = incidentsIngestor.ingestAllPages(jobContext, query.getIntegrationKey(), PagerDutyIncidentsPage.Query.builder()
                .integrationKey(query.getIntegrationKey())
                .since(from)
                .until(to)
                .limit(PAGE_SIZE)
                .offset(0)
                .build());

        // get alerts for the time range
        StorageResult alertsResults = alertsIngestor.ingestAllPages(jobContext, query.getIntegrationKey(), PagerDutyAlertsPage.Query.builder()
                .integrationKey(query.getIntegrationKey())
                .since(from)
                .until(to)
                .limit(PAGE_SIZE)
                .offset(0)
                .build());

        if (!iterative){
            log.info("[job={}] Completed onboarding ingestion. incidents={}, alerts={}, users={}",
                    jobContext.getJobId(),
                    incidentsResults == null ? 0: incidentsResults.getCount(),
                    alertsResults == null ? 0: alertsResults.getCount(),
                    usersResults == null ? 0: usersResults.getCount());
            if (CollectionUtils.isEmpty(incidentsResults.getRecords())
                    && CollectionUtils.isEmpty(alertsResults.getRecords())
                    && CollectionUtils.isEmpty(usersResults.getRecords())
                    && CollectionUtils.isEmpty(servicesResults.getRecords())) {
                return new EmptyIngestionResult();
            }
            return new ControllerIngestionResultList(servicesResults, incidentsResults, alertsResults, usersResults);
        }

        // get the changes for the time range
        StorageResult logResults = logEntriesIngestor.ingestAllPages(jobContext, query.getIntegrationKey(), PagerDutyLogEntriesPage.Query.builder()
                .integrationKey(query.getIntegrationKey())
                .limit(PAGE_SIZE)
                .offset(0)
                .since(from)
                .until(to)
                .build());

        log.info("[job={}] Completed iterative ingestion. incidents={}, alerts={}, users={}, logs={}",
                jobContext.getJobId(),
                incidentsResults == null ? 0: incidentsResults.getCount(),
                alertsResults == null ? 0: alertsResults.getCount(),
                usersResults == null ? 0: usersResults.getCount(),
                logResults == null ? 0: logResults.getCount());

        if (CollectionUtils.isEmpty(incidentsResults.getRecords())
                && CollectionUtils.isEmpty(alertsResults.getRecords())
                && CollectionUtils.isEmpty(usersResults.getRecords())
                && CollectionUtils.isEmpty(servicesResults.getRecords())
                && CollectionUtils.isEmpty(logResults.getRecords())) {
            return new EmptyIngestionResult();
        }

        return new ControllerIngestionResultList(servicesResults, incidentsResults, alertsResults, usersResults, logResults);
    }

    @Override
    public PagerDutyIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, PagerDutyIterativeScanQuery.class);
    }

}