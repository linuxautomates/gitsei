package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = WorkItemHistory.WorkItemHistoryBuilder.class)
public class WorkItemHistory {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("workItemId")
    Integer workItemId;

    @JsonProperty("rev")
    Integer rev;

    @JsonProperty("revisedBy")
    IdentityReference revisedBy;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IdentityReference.IdentityReferenceBuilder.class)
    public static class IdentityReference {

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

    @JsonProperty("revisedDate")
    String revisedDate;

    @JsonProperty("fields")
    FieldUpdate fields;

    @JsonProperty("url")
    String url;
}
