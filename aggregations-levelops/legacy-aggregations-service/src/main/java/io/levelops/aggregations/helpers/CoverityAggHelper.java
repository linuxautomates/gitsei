package io.levelops.aggregations.helpers;

import com.google.common.base.MoreObjects;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.coverity.DbCoverityDefect;
import io.levelops.commons.databases.models.database.coverity.DbCoveritySnapshot;
import io.levelops.commons.databases.models.database.coverity.DbCoverityStream;
import io.levelops.commons.databases.services.CoverityDatabaseService;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.coverity.models.Defect;
import io.levelops.integrations.coverity.models.EnrichedProjectData;
import io.levelops.integrations.coverity.models.Snapshot;
import io.levelops.integrations.coverity.models.Stream;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class CoverityAggHelper {

    private static final String DATA_TYPE = "defects";

    private final JobDtoParser jobDtoParser;
    private final CoverityDatabaseService coverityDatabaseService;
    private final EventsClient eventsClient;

    @Autowired
    public CoverityAggHelper(JobDtoParser jobDtoParser,
                             CoverityDatabaseService coverityDatabaseService,
                             EventsClient eventsClient) {
        this.jobDtoParser = jobDtoParser;
        this.coverityDatabaseService = coverityDatabaseService;
        this.eventsClient = eventsClient;
    }

    public boolean setupCoverityStreams(String customer, String integrationId,
                                        MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer, DATA_TYPE, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Stream stream = enrichedProjectData.getStream();
                    try {
                        DbCoverityStream dbCoverityStream = DbCoverityStream.fromStream(stream, integrationId);
                        coverityDatabaseService.upsertStream(customer, List.of(dbCoverityStream));
                    } catch (Exception e) {
                        log.warn("setupCoverityStream: error inserting streams: "
                                + " for integration id: " + integrationId, e);
                    }
                },
                List.of());
    }

    public boolean setupCoveritySnapshots(String customer, String integrationId,
                                          MultipleTriggerResults results) {
        List<DbCoveritySnapshot> dbCoveritySnapshots = new ArrayList<>();
        return jobDtoParser.applyToResults(customer, DATA_TYPE, EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                enrichedProjectData -> {
                    Stream stream = enrichedProjectData.getStream();
                    Optional<DbCoverityStream> streamId = coverityDatabaseService.getStream(customer, stream.getId().get("name"), stream.getPrimaryProjectId().get("name"), integrationId);
                    Snapshot snapshot = enrichedProjectData.getSnapshot();
                    dbCoveritySnapshots.add(DbCoveritySnapshot.fromSnapshot(snapshot, integrationId, streamId.get().getId()));
                    try {
                        coverityDatabaseService.upsertSnapshot(customer, dbCoveritySnapshots);
                    } catch (SQLException e) {
                        log.warn("setupCoverityStream: error inserting snapshot: "
                                + " for integration id: " + integrationId, e);
                    }
                },
                List.of());
    }

    public boolean setupCoverityDefects(String customer, String integrationId,
                                        MultipleTriggerResults results,
                                        boolean isOnboarding) {
        boolean result = jobDtoParser.applyToResults(
                customer,
                DATA_TYPE,
                EnrichedProjectData.class,
                results.getTriggerResults().get(0),
                (enrichedProjectData, jobDTO) -> processDefects(customer, integrationId, jobDTO, enrichedProjectData, isOnboarding),
                List.of());
        if (!result) {
            return false;
        }
        return result;
    }

    private void processDefects(String customer, String integrationId, JobDTO jobDTO, EnrichedProjectData enrichedProjectData, boolean isOnboarding) {
        Snapshot snapshot = enrichedProjectData.getSnapshot();
        Optional<DbCoveritySnapshot> snapshotId = coverityDatabaseService.getSnapshot(customer, String.valueOf(snapshot.getSnapshotId().get("id")), integrationId);
        List<Defect> defects = enrichedProjectData.getDefects();
        for (Defect defect : defects) {
            DbCoverityDefect dbCoverityDefect = DbCoverityDefect.fromDefect(defect, integrationId, snapshotId.get().getId());
            Optional<DbCoverityDefect> oldIssue = Optional.empty();
            boolean issueIsActuallyNew = false;
            boolean todayIssueIsNew = false;
            try {
                oldIssue = coverityDatabaseService.getDefect(customer, integrationId, dbCoverityDefect.getCid().toString());
                log.info("customer {}, oldIssue {}", customer, oldIssue);
                if (oldIssue.isEmpty() && isNewDefectAcrossSnapshot(dbCoverityDefect.getFirstDetectedAt(), dbCoverityDefect.getLastDetectedAt())) {
                    log.info("new defect found for snapshot{}", snapshot.getSnapshotId().get("id"));
                    issueIsActuallyNew = true;
                    todayIssueIsNew = true;
                    try {
                        coverityDatabaseService.insert(customer, dbCoverityDefect);
                    } catch (SQLException e) {
                        log.warn("setupCoverityStream: error inserting defects: "
                                + " for integration id: " + integrationId, e);
                    }
                }
                if (issueIsActuallyNew) {
                    EventType eventType = null;
                    Map<String, Object> eventData = null;
                    try {
                        eventData = convertDefectToEventData(integrationId, defect);
                        eventType = determineEventType(isOnboarding, todayIssueIsNew);
                        if (eventType != null) {
                            eventsClient.emitEvent(customer, eventType, eventData);
                        }
                    } catch (EventsClientException e) {
                        log.error("Error sending event for tenant={}, eventType={}, eventData={}", customer, eventType, eventData, e);
                    }
                }
            } catch (Exception e) {
                log.warn("Error updating issue with isEmpty={}", oldIssue.isEmpty(), e);
                return;
            }
        }
    }

    private Map<String, Object> convertDefectToEventData(String integrationId, Defect defect) {
        var data = new HashMap<String, Object>();
        data.put("cid", MoreObjects.firstNonNull(defect.getCid(), ""));
        data.put("checker_name", MoreObjects.firstNonNull(defect.getCheckerName(), ""));
        data.put("component_name", MoreObjects.firstNonNull(defect.getComponentName(), ""));
        data.put("integration_id", integrationId);
        data.put("cwe", MoreObjects.firstNonNull(defect.getCwe(), ""));
        data.put("domain", MoreObjects.firstNonNull(defect.getDomain(), ""));
        data.put("impact", MoreObjects.firstNonNull(defect.getDisplayImpact(), ""));
        data.put("kind", MoreObjects.firstNonNull(defect.getDisplayIssueKind(), ""));
        data.put("occurrence_count", MoreObjects.firstNonNull(defect.getOccurrenceCount(), ""));
        data.put("category", MoreObjects.firstNonNull(defect.getDisplayCategory(), ""));
        data.put("file_pathname", MoreObjects.firstNonNull(defect.getFilePathname(), ""));
        data.put("function_name", MoreObjects.firstNonNull(defect.getFunctionName(), ""));
        data.put("last_detected", MoreObjects.firstNonNull(defect.getLastDetected(), ""));
        data.put("first_detected", MoreObjects.firstNonNull(defect.getFirstDetected(), ""));
        data.put("first_detected_stream", MoreObjects.firstNonNull(defect.getFirstDetectedStream(), ""));
        data.put("last_detected_stream", MoreObjects.firstNonNull(defect.getLastDetectedStream(), ""));
        data.put("first_detected_snapshot_id", MoreObjects.firstNonNull(defect.getFirstDetectedSnapshotId(), ""));
        data.put("last_detected_snapshot_id", MoreObjects.firstNonNull(defect.getLastDetectedSnapshotId(), ""));
        return data;
    }

    private EventType determineEventType(boolean isOnboarding, boolean isNewDefect) {
        if (isOnboarding) {
            return null;
        }
        if (isNewDefect) {
            return EventType.COVERITY_DEFECT_CREATED;
        }
        return null;
    }

    private boolean isNewDefectAcrossSnapshot(Timestamp firstDetected, Timestamp lasDetected) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String firstDetectedTime = dateFormat.format(firstDetected);
        String lastDetectedTime = dateFormat.format(lasDetected);
        return firstDetectedTime.equals(lastDetectedTime);
    }
}
