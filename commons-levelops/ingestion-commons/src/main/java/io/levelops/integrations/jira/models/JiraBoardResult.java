package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraBoardResult.JiraBoardResultBuilder.class)
public class JiraBoardResult {

    @JsonProperty("isLast")
    Boolean isLast;
    @JsonProperty("startAt")
    Integer startAt;
    @JsonProperty("maxResults")
    Integer maxResults;
    @JsonProperty("total")
    Integer total;
    @JsonProperty("values")
    List<JiraBoard> boards;

}
