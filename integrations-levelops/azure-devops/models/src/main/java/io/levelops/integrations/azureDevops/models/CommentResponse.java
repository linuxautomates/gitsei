package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.azureDevops.models.Comment;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CommentResponse.CommentResponseBuilder.class)
public class CommentResponse {

    @JsonProperty("comments")
    List<Comment> comments;

    @JsonProperty("count")
    Integer count;

    @JsonProperty("totalCount")
    Integer totalCount;
}
