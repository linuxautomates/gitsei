package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Comment.CommentBuilder.class)
public class Comment {

    @JsonProperty("workItemId")
    Integer workItemId;

    @JsonProperty("createdDate")
    String createdDate;

    @JsonProperty("modifiedDate")
    String modifiedDate;

    @JsonProperty("text")
    String text;

    @JsonProperty("url")
    String url;

    @JsonProperty("version")
    String version;

    @JsonProperty("modifiedBy")
    IdentityRef modifiedBy;

    @JsonProperty("createdBy")
    IdentityRef createdBy;

    @JsonProperty("id")
    Integer id;

    @JsonProperty("isDeleted")
    Boolean isDeleted;
}
