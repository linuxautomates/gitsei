package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.controllers.EmptyIngestionResult;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.controllers.generic.ControllerIngestionResultList;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.jira.models.JiraIterativeScanQuery;
import io.levelops.ingestion.models.JobContext;
import io.levelops.ingestion.sinks.StorageDataSink;
import io.levelops.ingestion.strategies.pagination.NumberedPaginationStrategy;
import io.levelops.ingestion.strategies.pagination.PaginationStrategy;
import io.levelops.ingestion.strategies.pagination.SinglePageStrategy;
import io.levelops.ingestion.strategies.pagination.StreamedPaginationStrategy;
import io.levelops.integrations.jira.models.JiraField;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraIssueFields.JiraStatus;
import io.levelops.integrations.jira.models.JiraProject;
import io.levelops.integrations.jira.models.JiraSprint;
import io.levelops.integrations.jira.models.JiraUser;
import io.levelops.integrations.jira.sources.JiraFieldDataSource;
import io.levelops.integrations.jira.sources.JiraIssueDataSource;
import io.levelops.integrations.jira.sources.JiraProjectDataSource;
import io.levelops.integrations.jira.sources.JiraSprintDataSource;
import io.levelops.integrations.jira.sources.JiraStatusDataSource;
import io.levelops.integrations.jira.sources.JiraUserDataSource;
import io.levelops.integrations.jira.sources.JiraUserDataSource.JiraUserQuery;
import io.levelops.integrations.storage.models.StorageResult;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@Log4j2
public class JiraIterativeScanController implements DataController<JiraIterativeScanQuery> {

    private static final String JQL_METADATA_FIELD = "jql";
    private static final String TIMEZONE_METADATA_FIELD = "timezone";
    private static final Integer PAGE_SIZE = 50;

    private final int onboardingScanInDays;
    private final ObjectMapper objectMapper;
    private final InventoryService inventoryService;
    @Nullable
    private final String jiraProject;

    private final PaginationStrategy<JiraIssue, JiraIssueDataSource.JiraIssueQuery> issuePaginationStrategy;
    private final PaginationStrategy<JiraProject, JiraProjectDataSource.JiraProjectQuery> projectPaginationStrategy;
    private final PaginationStrategy<JiraSprint, JiraSprintDataSource.JiraSprintQuery> sprintPaginationStrategy;
    private final PaginationStrategy<JiraField, BaseIntegrationQuery> fieldPaginationStrategy;
    private final PaginationStrategy<JiraUser, JiraUserQuery> userPaginationStrategy;
    private final PaginationStrategy<JiraStatus, BaseIntegrationQuery> statusPaginationStrategy;

    @Builder
    public JiraIterativeScanController(
            Integer onboardingScanInDays,
            ObjectMapper objectMapper,
            StorageDataSink storageDataSink,
            JiraIssueDataSource issueDataSource,
            JiraProjectDataSource projectDataSource,
            JiraFieldDataSource fieldDataSource,
            JiraUserDataSource jiraUserDataSource,
            JiraStatusDataSource jiraStatusDataSource,
            JiraSprintDataSource jiraSprintDataSource,
            InventoryService inventoryService,
            @Nullable Integer outputPageSize,
            @Nullable String jiraProject) {
        this.onboardingScanInDays = MoreObjects.firstNonNull(onboardingScanInDays, 90);
        this.objectMapper = objectMapper;
        this.inventoryService = inventoryService;
        this.jiraProject = jiraProject;

        issuePaginationStrategy = NumberedPaginationStrategy.<JiraIssue, JiraIssueDataSource.JiraIssueQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .integrationType("jira")
                .dataType("issues")
                .pageDataSupplier((query, page) -> {
                    int pageSize = MoreObjects.firstNonNull(query.getLimit(), PAGE_SIZE);
                    return issueDataSource.fetchMany(JiraIssueDataSource.JiraIssueQuery.builder()
                            .integrationKey(query.getIntegrationKey())
                            .jql(query.getJql())
                            .skip(page.getPageNumber() * pageSize)
                            .limit(pageSize)
                            .build());
                })
                .skipEmptyResults(true)
                .outputPageSize(outputPageSize)
                .build();

        projectPaginationStrategy = SinglePageStrategy.<JiraProject, JiraProjectDataSource.JiraProjectQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(projectDataSource)
                .integrationType("jira")
                .dataType("projects")
                .build();

        fieldPaginationStrategy = SinglePageStrategy.<JiraField, BaseIntegrationQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(fieldDataSource)
                .integrationType("jira")
                .dataType("fields")
                .build();

        userPaginationStrategy = StreamedPaginationStrategy.<JiraUser, JiraUserQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(jiraUserDataSource)
                .integrationType("jira")
                .dataType("users")
                .build();

        statusPaginationStrategy = SinglePageStrategy.<JiraStatus, BaseIntegrationQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(jiraStatusDataSource)
                .integrationType("jira")
                .dataType("statuses")
                .build();

        sprintPaginationStrategy = SinglePageStrategy.<JiraSprint, JiraSprintDataSource.JiraSprintQuery>builder()
                .objectMapper(objectMapper)
                .storageDataSink(storageDataSink)
                .dataSource(jiraSprintDataSource)
                .integrationType("jira")
                .dataType("sprints")
                .build();

    }

    /**
     * Get date format using given timezone.
     * Formats supported:
     * - abbreviation such as "PST",
     * - a full name such as "America/Los_Angeles",
     * - or a customID such as "GMT-8:00"
     */
    private SimpleDateFormat getJiraDateFormat(@Nullable String timezone) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        // TODO research if we can get timezone from customer's jira server config!
        if (StringUtils.isNotBlank(timezone)) {
            dateFormat.setTimeZone(TimeZone.getTimeZone(timezone.trim()));
        } else {
            // Jira Cloud uses PST!
            dateFormat.setTimeZone(TimeZone.getTimeZone("PST"));
        }

        return dateFormat;
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, JiraIterativeScanQuery query) throws IngestException {

        // get integration metadata
        String customJql = null;
        String timezone = null;
        try {
            Integration integration = inventoryService.getIntegration(query.getIntegrationKey());
            Map<String, Object> metadata = MapUtils.emptyIfNull(integration.getMetadata());
            customJql = (String) metadata.get(JQL_METADATA_FIELD);
            timezone = (String) metadata.get(TIMEZONE_METADATA_FIELD);
        } catch (InventoryException e) {
            throw new IngestException("Failed to get integration for key: " + query.getIntegrationKey(), e);
        }

        // ---- ISSUES ----

        SimpleDateFormat jiraDateFormat = getJiraDateFormat(timezone);

        Date from = query.getFrom();
        Date to = (query.getTo() != null) ? query.getTo() : new Date();

        if (from == null) {
            // onboarding
            // only onboarding last ONBOARDING_SCAN_IN_DAYS days
            from = Date.from(Instant.now().minus(onboardingScanInDays, ChronoUnit.DAYS));
        }
        String jql = String.format("updated > '%s' AND updated <= '%s'", jiraDateFormat.format(from), jiraDateFormat.format(to));
        if (Strings.isNotEmpty(jiraProject)) {
            jql += String.format(" AND project = \"%s\"", jiraProject);
        }
        if (StringUtils.isNotBlank(customJql)) {
            jql = String.format("( %s ) AND ( %s )", jql, customJql);
        }

        log.info("JQL={}", jql);

        if (query.getIssuesPageSize() != null) {
            log.info("jira_issues_page_size={}", query.getIssuesPageSize());
        }

        StorageResult issueResult = issuePaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), JiraIssueDataSource.JiraIssueQuery.builder()
                .integrationKey(query.getIntegrationKey())
                .jql(jql)
                .limit(query.getIssuesPageSize())
                .build());

        // if we don't need to fetch projects & sprints AND issues are empty - then return empty result
        boolean fetchProject = Boolean.TRUE.equals(query.getFetchProjects());
        boolean fetchSprints = Boolean.TRUE.equals(query.getFetchSprints());
        if (!fetchProject && !fetchSprints && CollectionUtils.isEmpty(issueResult.getRecords())) {
            return new EmptyIngestionResult();
        }

        List<ControllerIngestionResult> results = new ArrayList<>();
        results.add(issueResult);
        // ---- PROJECTS ----

        StorageResult projectResult;
        try {
            projectResult = projectPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), JiraProjectDataSource.JiraProjectQuery.builder()
                    .integrationKey(query.getIntegrationKey())
                    .build());
            results.add(projectResult);
        } catch (FetchException e) {
            log.error("Failed to fetch projects... ignoring.", e);
        }

        // ---- FIELDS ----
        try {
            StorageResult fieldResult = fieldPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), BaseIntegrationQuery.builder()
                    .integrationKey(query.getIntegrationKey())
                    .build());
            results.add(fieldResult);
        } catch (Exception e) {
            log.warn("Failed to fetch fields... ignoring.", e);
        }

        // ---- USERS ----
        try {
            StorageResult userResult = userPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), JiraUserQuery.builder()
                    .integrationKey(query.getIntegrationKey())
                    .build());
            results.add(userResult);
        } catch (Exception e) {
            log.warn("Failed to fetch users... ignoring.");
            log.debug("Failed to fetch users: ", e);
        }

        // ---- STATUSES ----
        try {
            StorageResult statusesResult = statusPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(), BaseIntegrationQuery.builder()
                    .integrationKey(query.getIntegrationKey())
                    .build());
            results.add(statusesResult);
        } catch (Exception e) {
            log.warn("Failed to fetch statuses... ignoring.", e);
        }

        // ---- SPRINTS ----
        if (Boolean.TRUE.equals(query.getFetchSprints())) {
            try {
                StorageResult sprintResult = sprintPaginationStrategy.ingestAllPages(jobContext, query.getIntegrationKey(),
                        JiraSprintDataSource.JiraSprintQuery.builder()
                                .integrationKey(query.getIntegrationKey())
                                .build());
                results.add(sprintResult);
            } catch (FetchException e) {
                log.warn("Failed to fetch sprints... ignoring.", e);
            }
        }

        return new ControllerIngestionResultList(results);
    }

    @Override
    public JiraIterativeScanQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, JiraIterativeScanQuery.class);
    }

}
