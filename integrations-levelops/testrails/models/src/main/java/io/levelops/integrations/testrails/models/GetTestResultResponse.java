package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GetTestResultResponse.GetTestResultResponseBuilder.class)
public class GetTestResultResponse {

    @JsonProperty("offset")
    Integer offset;

    @JsonProperty("limit")
    Integer limit;

    @JsonProperty("size")
    Integer size;

    @JsonProperty("_links")
    Link links;

    @JsonProperty("results")
    List<Test.Result> results;
}
