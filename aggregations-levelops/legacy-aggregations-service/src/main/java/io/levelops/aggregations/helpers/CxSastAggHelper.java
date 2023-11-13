package io.levelops.aggregations.helpers;

import com.google.common.base.MoreObjects;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastIssue;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastProject;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastQuery;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastScan;
import io.levelops.commons.databases.services.CxSastAggService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.CxSastScan;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class for performing cxsast aggregations
 */
@Log4j2
@Service
public class CxSastAggHelper {
    private static final String PROJECTS_DATATYPE = "projects";
    private static final String SCANS_DATATYPE = "scans";

    private final JobDtoParser jobDtoParser;
    private final IntegrationTrackingService trackingService;
    private final CxSastAggService aggService;
    private final EventsClient eventsClient;

    @Autowired
    public CxSastAggHelper(JobDtoParser jobDtoParser,
                           CxSastAggService aggService,
                           IntegrationTrackingService trackingService,
                           EventsClient eventsClient) {
        this.jobDtoParser = jobDtoParser;
        this.aggService = aggService;
        this.trackingService = trackingService;
        this.eventsClient = eventsClient;
    }

    public boolean setupCxSastProject(String customer, String integrationId,
                                      MultipleTriggerResults triggerResults,
                                      Date currentTime) {
        Optional<TriggerResults> latestTriggerResult = triggerResults.getTriggerResults().stream().findFirst();
        boolean result = false;
        if (latestTriggerResult.isPresent()) {
            result = jobDtoParser.applyToResults(customer,
                    PROJECTS_DATATYPE,
                    CxSastProject.class,
                    triggerResults.getTriggerResults().get(0),
                    cxSastProject -> {
                        DbCxSastProject project = DbCxSastProject.fromProject(cxSastProject, integrationId);
                        try {
                            aggService.insert(customer, project);
                        } catch (SQLException e) {
                            log.error("setupCxSastDatabaseService: error inserting project with id: "
                                    + project.getProjectId(), e);
                        }
                    },
                    List.of());
            if (result) {
                trackingService.upsert(customer,
                        IntegrationTracker.builder()
                                .integrationId(integrationId)
                                .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime,
                                        Calendar.DATE))
                                .build());
            }
        }
        return result;
    }

    public boolean setupCxSastScan(String customer, String integrationId,
                                   MultipleTriggerResults triggerResults, Date currentTime, boolean isOnboarding) {
        Optional<TriggerResults> latestTriggerResult = triggerResults.getTriggerResults().stream().findFirst();
        boolean result = false;
        if (latestTriggerResult.isPresent()) {
            result = jobDtoParser.applyToResults(customer,
                    SCANS_DATATYPE,
                    CxSastScan.class,
                    triggerResults.getTriggerResults().get(0),
                    cxSastScan -> {
                        DbCxSastScan dbCxSastScan = DbCxSastScan.fromScan(cxSastScan, integrationId);
                        String scanUUID = aggService.insertScan(customer, dbCxSastScan);
                        cxSastScan.getReport().getQueries()
                                .forEach(cxQuery -> {
                                    Optional<String> query = aggService.getQuery(customer, cxQuery.getId(), scanUUID,
                                            Integer.parseInt(integrationId));
                                    DbCxSastQuery dbCxSastQuery = DbCxSastQuery.fromQuery(cxQuery, integrationId, currentTime);
                                    List<DbCxSastIssue> dbCxSastIssues = dbCxSastQuery.getIssues();
                                    dbCxSastIssues.forEach(dbCxSastIssue ->
                                            checkAndGenerateEventForIssue(customer, integrationId, isOnboarding, query, dbCxSastIssue, dbCxSastScan));
                                    aggService.insertQuery(customer, dbCxSastQuery, cxSastScan.getId(),
                                            cxSastScan.getProject().getId());
                                });
                    },
                    List.of());
            if (result) {
                trackingService.upsert(customer,
                        IntegrationTracker.builder()
                                .integrationId(integrationId)
                                .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(currentTime,
                                        Calendar.DATE))
                                .build());
            }
        }
        return result;
    }

    public void checkAndGenerateEventForIssue(String customer, String integrationId, boolean isOnboarding,
                                              Optional<String> existingQueryUUID, DbCxSastIssue dbCxSastIssue,
                                              DbCxSastScan dbCxSastScan) {
        if (isOnboarding) {
            return;
        }
        if (existingQueryUUID.isEmpty()) {
            emitEventForNewIssue(customer, dbCxSastIssue, dbCxSastScan);
        } else {
            Optional<String> existingIssue = aggService.getIssue(customer, Integer.parseInt(integrationId), existingQueryUUID.get(),
                    dbCxSastIssue.getNodeId());
            if (existingIssue.isEmpty()) {
                emitEventForNewIssue(customer, dbCxSastIssue, dbCxSastScan);
            }
        }
    }

    private void emitEventForNewIssue(String customer, DbCxSastIssue dbCxSastIssue,
                                      DbCxSastScan dbCxSastScan) {
        Map<String, Object> eventData = buildEventData(dbCxSastIssue, dbCxSastScan);
        try {
            eventsClient.emitEvent(customer, EventType.CHECKMARX_SAST_NEW_ISSUE, eventData);
        } catch (EventsClientException e) {
            log.error("Error sending event {}, {}, {}", customer, EventType.CHECKMARX_SAST_NEW_ISSUE, eventData, e);
        }
    }

    private Map<String, Object> buildEventData(DbCxSastIssue issue, DbCxSastScan scan) {
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("node_id", issue.getNodeId());
        eventData.put("query_id", issue.getQueryId());
        eventData.put("assignee", MoreObjects.firstNonNull(issue.getAssignee(), ""));
        eventData.put("severity", MoreObjects.firstNonNull(issue.getSeverity(), ""));
        eventData.put("file_name", MoreObjects.firstNonNull(issue.getFileName(), ""));
        eventData.put("line", MoreObjects.firstNonNull(issue.getLine(), 0));
        eventData.put("column", MoreObjects.firstNonNull(issue.getColumn(), 0));
        eventData.put("state", MoreObjects.firstNonNull(issue.getState(), ""));
        eventData.put("status", MoreObjects.firstNonNull(issue.getStatus(), ""));
        eventData.put("detection_date", MoreObjects.firstNonNull(issue.getDetectionDate(), new Date()));
        eventData.put("false_positive", MoreObjects.firstNonNull(issue.isFalsePositive(), false));
        eventData.put("scan_type", MoreObjects.firstNonNull(scan.getScanType(), ""));
        eventData.put("project_id", MoreObjects.firstNonNull(scan.getProjectId(), ""));
        eventData.put("initiator_name", MoreObjects.firstNonNull(scan.getInitiatorName(), ""));
        return eventData;
    }
}

