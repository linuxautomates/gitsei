package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GetTestPlanResponse.GetTestPlanResponseBuilder.class)
public class GetTestPlanResponse {

    @JsonProperty("offset")
    Integer offset;

    @JsonProperty("limit")
    Integer limit;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("_links")
    Link links;

    @JsonProperty("plans")
    List<TestPlan> plans;
}
