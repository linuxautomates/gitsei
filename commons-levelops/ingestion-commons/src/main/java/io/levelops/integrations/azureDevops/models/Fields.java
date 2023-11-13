package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.HashMap;
import java.util.Map;


@Value
@AllArgsConstructor
@Builder(toBuilder = true)
public class Fields {

    public Fields() {
        this(null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null,
                null,null,null, null, null, null, null, null, null, null, null, null);
    }

    @JsonProperty("System.Id")
    Integer id;

    @JsonProperty("System.AreaId")
    Integer areaId;

    @JsonProperty("System.AreaPath")
    String areaPath;

    @JsonProperty("System.TeamProject")
    String teamProject;

    @JsonProperty("System.NodeName")
    String nodeName;

    @JsonProperty("System.AreaLevel1")
    String areaLevel1;

    @JsonProperty("System.AreaLevel2")
    String areaLevel2;

    @JsonProperty("System.Rev")
    Integer rev;

    @JsonProperty("System.AuthorizedDate")
    String authorizedDate;

    @JsonProperty("System.RevisedDate")
    String revisedDate;

    @JsonProperty("System.IterationId")
    Integer iterationId;

    @JsonProperty("System.IterationPath")
    String iterationPath;

    @JsonProperty("System.IterationLevel1")
    String iterationLevel1;

    @JsonProperty("System.IterationLevel2")
    String iterationLevel2;

    @JsonProperty("System.WorkItemType")
    String workItemType;

    @JsonProperty("System.State")
    String state;

    @JsonProperty("System.Reason")
    String reason;

    @JsonProperty("System.AssignedTo")
    AuthorizationDetail assignedTo;

    @JsonProperty("System.CreatedDate")
    String createdDate;

    @JsonProperty("System.CreatedBy")
    AuthorizationDetail createdBy;

    @JsonProperty("System.ChangedBy")
    AuthorizationDetail changedBy;

    @JsonProperty("System.AuthorizedAs")
    AuthorizationDetail authorizedAs;

    @JsonProperty("System.PersonId")
    Long personId;

    @JsonProperty("System.Watermark")
    Integer watermark;

    @JsonProperty("System.CommentCount")
    Integer commentCount;

    @JsonProperty("System.Title")
    String title;

    @JsonProperty("Microsoft.VSTS.Scheduling.StoryPoints")
    Float storyPoints;

    @JsonProperty("Microsoft.VSTS.Scheduling.RemainingWork")
    String remainingWork;

    @JsonProperty("Microsoft.VSTS.Scheduling.OriginalEstimate")
    String originalEstimate;

    @JsonProperty("Microsoft.VSTS.Scheduling.CompletedWork")
    String completedWork;

    @JsonProperty("Microsoft.VSTS.Common.Activity")
    String activity;

    @JsonProperty("Microsoft.VSTS.Common.StateChangeDate")
    String stateChangeDate;

    @JsonProperty("Microsoft.VSTS.Common.ActivatedDate")
    String activatedDate;

    @JsonProperty("Microsoft.VSTS.Common.ActivatedBy")
    AuthorizationDetail activatedBy;

    @JsonProperty("Microsoft.VSTS.Common.ClosedDate")
    String closedDate;

    @JsonProperty("Microsoft.VSTS.Common.ClosedBy")
    AuthorizationDetail closedBy;

    @JsonProperty("Microsoft.VSTS.Common.AcceptanceCriteria")
    String acceptanceCriteria;

    @JsonProperty("System.Description")
    String description;

    @JsonProperty("System.History")
    String history;

    @JsonProperty("Microsoft.VSTS.Build.IntegrationBuild")
    String integrationBuild;

    @JsonProperty("System.ChangedDate")
    String updatedDate;

    @JsonProperty("Microsoft.VSTS.Common.ResolvedDate")
    String resolvedDate;

    @JsonProperty("Microsoft.VSTS.Common.ResolvedReason")
    String resolvedReason;

    @JsonProperty("System.BoardColumn")
    String boardColumn;

    @JsonProperty("System.BoardColumnDone")
    Boolean boardColumnDone;

    @JsonProperty("Microsoft.VSTS.Common.Priority")
    Integer priority;

    @JsonProperty("Microsoft.VSTS.Common.Severity")
    String severity;

    @JsonProperty("Microsoft.VSTS.Common.ValueArea")
    String valueArea;

    @JsonProperty("Custom.newfield")
    String newField;

    @JsonProperty("Microsoft.VSTS.Scheduling.Effort")
    Integer effort;

    @JsonProperty("System.Tags")
    String tags;

    @JsonProperty("System.Parent")
    Integer parent;

    Map<String, Object> customFields = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getCustomFields() {
        return customFields;
    }

    @JsonAnySetter
    public void addCustomField(String key, Object value) {
        if (value == null) {
            return;
        }
        customFields.put(key, value);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = AuthorizationDetail.AuthorizationDetailBuilder.class)
    public static class AuthorizationDetail {

        @JsonProperty("displayName")
        String displayName;

        @JsonProperty("url")
        String url;

        @JsonProperty("_links")
        Link _links;

        @JsonProperty("id")
        String id;

        @JsonProperty("uniqueName")
        String uniqueName;

        @JsonProperty("imageUrl")
        String imageUrl;

        @JsonProperty("descriptor")
        String descriptor;
    }
}
