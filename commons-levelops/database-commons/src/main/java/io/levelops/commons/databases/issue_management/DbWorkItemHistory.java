package io.levelops.commons.databases.issue_management;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.dates.DateUtils;
import io.levelops.integrations.azureDevops.models.FieldUpdate;
import io.levelops.integrations.azureDevops.models.WorkItemHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.apache.commons.lang3.StringUtils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static io.levelops.integrations.azureDevops.utils.WorkItemUtils.getChangedDateFromHistoryAsTimestamp;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbWorkItemHistory {

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("field_type")
    private String fieldType;

    @JsonProperty(value = "field_value")
    private String fieldValue;

    @JsonIgnore
    private String oldValue;

    @JsonProperty(value = "workitem_id")
    private String workItemId;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty(value = "start_date")
    private Timestamp startDate;

    @JsonProperty(value = "end_date")
    private Timestamp endDate;

    @JsonProperty(value = "created_at")
    private Timestamp createdAt;

    @JsonProperty(value = "updated_at")
    private Timestamp updatedAt;

    public static List<DbWorkItemHistory> fromAzureDevopsWorkItemHistories(String integrationId, WorkItemHistory workItemHistory, Date currentTime) {
        FieldUpdate fields = workItemHistory.getFields();
        if (fields == null) {
            return List.of();
        }
        Timestamp changedDate = getChangedDateFromHistoryAsTimestamp(workItemHistory);
        Timestamp endDate = DateUtils.toTimestamp(new Date().toInstant());
        ArrayList<DbWorkItemHistory> workItemHistories = new ArrayList<>();
        if (fields.getState() != null) {
            workItemHistories.add(DbWorkItemHistory.builder()
                    .workItemId(String.valueOf(workItemHistory.getWorkItemId()))
                    .integrationId(integrationId)
                    .fieldType("status")
                    .fieldValue(fields.getState().getNewValue())
                    .oldValue(fields.getState().getOldValue())
                    .startDate(changedDate)
                    .endDate(endDate)
                    .build());
        }
        if (fields.getAssignee() != null) {
            workItemHistories.add(DbWorkItemHistory.builder()
                    .workItemId(String.valueOf(workItemHistory.getWorkItemId()))
                    .integrationId(integrationId)
                    .fieldType("assignee")
                    .fieldValue(StringUtils.defaultIfEmpty(fields.getAssignee().getNewValue() != null ? fields.getAssignee().getNewValue().getDisplayName() : null, "UNASSIGNED"))
                    .oldValue(fields.getAssignee().getOldValue() != null ? fields.getAssignee().getOldValue().getDisplayName() : null)
                    .startDate(changedDate)
                    .endDate(endDate)
                    .build());
        }
        if (fields.getIterationPath() != null) {
            endDate = DateUtils.toTimestamp(DateUtils.toEndOfDay(currentTime.toInstant()));
            workItemHistories.add(DbWorkItemHistory.builder()
                    .workItemId(String.valueOf(workItemHistory.getWorkItemId()))
                    .integrationId(integrationId)
                    .fieldType("sprint")
                    .fieldValue(fields.getIterationPath().getNewValue())
                    .oldValue(fields.getIterationPath().getOldValue())
                    .startDate(changedDate)
                    .endDate(endDate)
                    .build());
        }
        if (fields.getStoryPoints() != null) {
            workItemHistories.add(DbWorkItemHistory.builder()
                    .workItemId(String.valueOf(workItemHistory.getWorkItemId()))
                    .integrationId(integrationId)
                    .fieldType("story_points")
                    .fieldValue(String.valueOf(fields.getStoryPoints().getNewValue()))
                    .oldValue(String.valueOf(fields.getStoryPoints().getOldValue()))
                    .startDate(changedDate)
                    .endDate(endDate)
                    .build());
        }
        return workItemHistories;
    }
}
