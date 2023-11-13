package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GetTestRunResponse.GetTestRunResponseBuilder.class)
public class GetTestRunResponse {

    @JsonProperty("offset")
    Integer offset;

    @JsonProperty("limit")
    Integer limit;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("_links")
    Link links;

    @JsonProperty("runs")
    List<TestRun> runs;
}
