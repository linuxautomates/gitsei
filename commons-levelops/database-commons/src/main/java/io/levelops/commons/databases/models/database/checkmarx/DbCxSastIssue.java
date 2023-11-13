package io.levelops.commons.databases.models.database.checkmarx;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.integrations.checkmarx.models.CxQuery;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.time.DateUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbCxSastIssue.DbCxSastIssueBuilder.class)
public class DbCxSastIssue {
    @JsonProperty("id")
    String id;
    @JsonProperty("integration_id")
    String integrationId;
    @JsonProperty("query_id")
    String queryId;
    @JsonProperty("node_id")
    String nodeId;
    @JsonProperty("file_name")
    String fileName;
    @JsonProperty("status")
    String status;
    @JsonProperty("line")
    int line;
    @JsonProperty("column")
    int column;
    @JsonProperty("false_positive")
    boolean falsePositive;
    @JsonProperty("severity")
    String severity;
    @JsonProperty("assignee")
    String assignee;
    @JsonProperty("state")
    String state;
    @JsonProperty("detection_date")
    Date detectionDate;
    @JsonProperty("ingested_at")
    Date ingestedAt;


    public static List<DbCxSastIssue> fromQuery(CxQuery source,
                                                String integrationId,
                                                Date ingestedAt) {
        Date truncatedDate = DateUtils.truncate(ingestedAt, Calendar.DATE);
        return IterableUtils.parseIterable(
                source.getResults(),
                issue -> DbCxSastIssue.builder()
                        .integrationId(integrationId)
                        .nodeId(MoreObjects.firstNonNull(issue.getNodeId(), ""))
                        .fileName(MoreObjects.firstNonNull(issue.getFileName(), ""))
                        .status(MoreObjects.firstNonNull(issue.getStatus(), ""))
                        .line(MoreObjects.firstNonNull(issue.getLine(), 0))
                        .column(MoreObjects.firstNonNull(issue.getColumn(), 0))
                        .falsePositive(MoreObjects.firstNonNull(issue.getFalsePositive(), false))
                        .severity(MoreObjects.firstNonNull(issue.getSeverity(), ""))
                        .assignee(MoreObjects.firstNonNull(issue.getAssignToUser(), ""))
                        .state(MoreObjects.firstNonNull(issue.getState(), ""))
                        .detectionDate(MoreObjects.firstNonNull(issue.getDetectionDate(), new Date()))
                        .ingestedAt(truncatedDate)
                        .build()
        );
    }
}
