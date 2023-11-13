package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonDeserialize(builder = DbAggregationResultStacksWrapper.DbAggregationResultStacksWrapperBuilder.class)
public class DbAggregationResultStacksWrapper {
    @JsonProperty("across_key")
    String acrossKey;

    @JsonProperty("across_additional_key")
    String acrossAdditionalKey;

    @JsonProperty("across_cicd_job_id")
    String acrossCiCdJobId;

    @JsonProperty("aggregation_result")
    DbAggregationResult dbAggregationResult;
}
