package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbHistogramResult.DbHistogramResultBuilder.class)
public class DbHistogramResult {
    @JsonProperty("index")
    Integer index;
    @JsonProperty("name")
    String name;
    @JsonProperty("buckets")
    List<DbHistogramBucket> buckets;
}
