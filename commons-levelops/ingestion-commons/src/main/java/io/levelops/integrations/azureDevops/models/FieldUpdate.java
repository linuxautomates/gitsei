package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FieldUpdate.FieldUpdateBuilder.class)
public class FieldUpdate {

    @JsonProperty("System.AssignedTo")
    FieldUpdateAuthorizationDetail assignee;

    @JsonProperty("System.State")
    FieldUpdateString state;

    @JsonProperty("System.RevisedDate")
    FieldUpdateDate revisedDate;

    @JsonProperty("System.Rev")
    FieldUpdateString rev;

    @JsonProperty("System.AuthorizedDate")
    FieldUpdateDate authorizedDate;

    @JsonProperty("System.IterationId")
    FieldUpdateString iterationId;

    @JsonProperty("System.IterationLevel2")
    FieldUpdateString iterationLevel2;

    @JsonProperty("System.ChangedDate")
    FieldUpdateDate changedDate;

    @JsonProperty("System.Watermark")
    FieldUpdateString watermark;

    @JsonProperty("System.IterationPath")
    FieldUpdateString iterationPath;

    @JsonProperty("Microsoft.VSTS.Scheduling.RemainingWork")
    FieldUpdateString remainingWork;

    @JsonProperty("Microsoft.VSTS.Scheduling.CompletedWork")
    FieldUpdateString completedWork;

    @JsonProperty("Microsoft.VSTS.Scheduling.OriginalEstimate")
    FieldUpdateString originalEstimate;

    @JsonProperty("Microsoft.VSTS.Common.StateChangeDate")
    FieldUpdateDate stateChangeDate;

    @JsonProperty("Microsoft.VSTS.Common.Priority")
    FieldUpdateString priority;

    @JsonProperty("System.Title")
    FieldUpdateString title;

    @JsonProperty("System.AreaPath")
    FieldUpdateString areaPath;

    @JsonProperty("System.TeamProject")
    FieldUpdateString teamProject;

    @JsonProperty("System.CommentCount")
    FieldUpdateString commentCount;

    @JsonProperty("Microsoft.VSTS.Scheduling.StoryPoints")
    FieldUpdateFloat storyPoints;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldUpdate.FieldUpdateString.FieldUpdateStringBuilder.class)
    public static class FieldUpdateString {

        @JsonProperty("oldValue")
        String oldValue;

        @JsonProperty("newValue")
        String newValue;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldUpdate.FieldUpdateFloat.FieldUpdateFloatBuilder.class)
    public static class FieldUpdateFloat {

        @JsonProperty("oldValue")
        Float oldValue;

        @JsonProperty("newValue")
        Float newValue;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldUpdate.FieldUpdateDate.FieldUpdateDateBuilder.class)
    public static class FieldUpdateDate {

        @JsonProperty("oldValue")
        String oldValue;

        @JsonProperty("newValue")
        String newValue;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldUpdate.FieldUpdateAuthorizationDetail.FieldUpdateAuthorizationDetailBuilder.class)
    public static class FieldUpdateAuthorizationDetail {

        @JsonProperty("oldValue")
        AuthorizationDetail oldValue;

        @JsonProperty("newValue")
        AuthorizationDetail newValue;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldUpdate.AuthorizationDetail.AuthorizationDetailBuilder.class)
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
