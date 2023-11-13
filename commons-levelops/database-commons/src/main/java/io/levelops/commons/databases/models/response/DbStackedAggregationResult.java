package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

@Value
@Log4j2
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = DbStackedAggregationResult.DbStackedAggregationResultBuilder.class)
public class DbStackedAggregationResult {

    @JsonProperty("row_key")
    String rowKey;
    @JsonProperty("row_additional_key")
    String rowAdditionalKey;
    @JsonProperty("stacked_agg_result")
    DbAggregationResult stackedAggResult;
}
