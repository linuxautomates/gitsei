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
import io.levelops.integrations.salesforce.models.Case;
import io.levelops.integrations.salesforce.models.CaseComment;
import io.levelops.integrations.salesforce.models.CaseHistory;
import io.levelops.integrations.salesforce.models.Contract;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import io.levelops.integrations.salesforce.models.Solution;
import io.levelops.integrations.salesforce.sources.SalesforceCaseDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceCaseHistoryDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceContractDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceSolutionDataSource;
import io.levelops.integrations.salesforce.sources.SalesforceCaseCommentDataSource;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * SalesForce's implementation of the {@link DataController}. Responsible for fetching Salesforce entities upon
 * receiving ingest request
 */
public class SalesforceIngestionController implements DataController<SalesforceIngestionQuery> {

    private static final String CASES = "cases";
    private static final String SOLUTIONS = "solutions";
    private static final String CONTRACTS = "contracts";
    private static final String CASE_HISTORIES = "case_histories";
    private static final String CASE_COMMENTS = "case_comments";
    private static final String INTEGRATION_TYPE = "salesforce";

    private final ObjectMapper objectMapper;
    private final PaginationStrategy<Case, SalesforceIngestionQuery> casePaginationStrategy;
    private final PaginationStrategy<Solution, SalesforceIngestionQuery> solutionPaginationStrategy; // dont need this for now
    private final PaginationStrategy<Contract, SalesforceIngestionQuery> contractPaginationStrategy; // dont need this for now
    private final PaginationStrategy<CaseHistory, SalesforceIngestionQuery> caseHistoryPaginationStrategy;
    private final PaginationStrategy<CaseComment, SalesforceIngestionQuery> caseCommentPaginationStrategy;
    private final int onboardingScanInDays;

    @Builder
    public SalesforceIngestionController(ObjectMapper objectMapper, SalesforceCaseDataSource salesForceCaseDataSource,
                                         SalesforceSolutionDataSource salesForceSolutionDataSource,
                                         SalesforceContractDataSource salesForceContractDataSource,
                                         SalesforceCaseHistoryDataSource salesForceCaseHistoryDataSource,
                                         SalesforceCaseCommentDataSource salesforceCaseCommentDataSource,
                                         StorageDataSink storageDataSink,
                                         int onboardingScanInDays) {
        this.objectMapper = objectMapper;
        this.casePaginationStrategy = StreamedPaginationStrategy.<Case, SalesforceIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(CASES)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(salesForceCaseDataSource)
                .skipEmptyResults(true)
                .build();
        this.solutionPaginationStrategy = StreamedPaginationStrategy.<Solution, SalesforceIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(SOLUTIONS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(salesForceSolutionDataSource)
                .skipEmptyResults(true)
                .build();
        this.contractPaginationStrategy = StreamedPaginationStrategy.<Contract, SalesforceIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(CONTRACTS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(salesForceContractDataSource)
                .skipEmptyResults(true)
                .build();
        this.caseHistoryPaginationStrategy = StreamedPaginationStrategy.<CaseHistory, SalesforceIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(CASE_HISTORIES)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(salesForceCaseHistoryDataSource)
                .skipEmptyResults(true)
                .build();
        this.caseCommentPaginationStrategy = StreamedPaginationStrategy.<CaseComment, SalesforceIngestionQuery>builder()
                .objectMapper(objectMapper)
                .dataType(CASE_COMMENTS)
                .integrationType(INTEGRATION_TYPE)
                .storageDataSink(storageDataSink)
                .dataSource(salesforceCaseCommentDataSource)
                .skipEmptyResults(true)
                .build();
        this.onboardingScanInDays = onboardingScanInDays != 0 ? onboardingScanInDays : 30;
    }

    /**
     * Ingests the data for given job {@param jobId} with the query {@param query}.
     * It calls the {@link SalesforceCaseDataSource} for fetching case entities, {@link SalesforceSolutionDataSource} to
     * fetch Solution entities, {@link SalesforceCaseHistoryDataSource} to fetch case histories, and {@link SalesforceContractDataSource}
     * to fetch Service contract histories
     *
     * @param jobId id of the job for which data needs to be ingested.
     * @param query describing the job
     * @return {@link ControllerIngestionResult} for the executed job
     * @throws IngestException for any exception during the ingestion process
     */
    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, SalesforceIngestionQuery query) throws IngestException {
        SalesforceIngestionQuery salesForceIngestionQuery;
        if (query.getFrom() == null) {
            salesForceIngestionQuery = SalesforceIngestionQuery.builder()
                    .from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS).getEpochSecond())
                    .partial(query.getPartial())
                    .integrationKey(query.getIntegrationKey())
                    .build();
        } else {
            salesForceIngestionQuery = query;
        }

        StorageResult caseStorageResult = casePaginationStrategy.ingestAllPages(jobContext,
                salesForceIngestionQuery.getIntegrationKey(), salesForceIngestionQuery);
//        StorageResult solutionStorageResult = solutionPaginationStrategy.ingestAllPages(jobContext,
//                salesForceIngestionQuery.getIntegrationKey(), salesForceIngestionQuery);
        StorageResult caseHistoryStorageResult = caseHistoryPaginationStrategy.ingestAllPages(jobContext,
                salesForceIngestionQuery.getIntegrationKey(), salesForceIngestionQuery);
//        StorageResult contractStorageResult = contractPaginationStrategy.ingestAllPages(jobContext,
//                salesForceIngestionQuery.getIntegrationKey(), salesForceIngestionQuery);
        StorageResult caseCommentsStorageResult = caseCommentPaginationStrategy.ingestAllPages(jobContext,
                salesForceIngestionQuery.getIntegrationKey(), salesForceIngestionQuery);
        return new ControllerIngestionResultList(caseStorageResult,
//                solutionStorageResult,
//                contractStorageResult,
                caseHistoryStorageResult,
                caseCommentsStorageResult);
    }

    /**
     * Parses the received query
     *
     * @param arg: query object corresponding to the required {@link SalesforceIngestionQuery}
     * @return {@link SalesforceIngestionQuery} for the job
     */
    @Override
    public SalesforceIngestionQuery parseQuery(Object arg) {
        SalesforceIngestionQuery query = objectMapper.convertValue(arg, SalesforceIngestionQuery.class);
        return query;
    }
}
