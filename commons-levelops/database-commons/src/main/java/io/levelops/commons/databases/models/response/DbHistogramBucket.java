package io.levelops.commons.databases.models.response;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbHistogramBucket.DbHistogramBucketBuilder.class)
public class DbHistogramBucket {
    @JsonProperty("bucket_number")
    Integer bucketNumber;
    @JsonProperty("interval_lower")
    Double intervalLower;
    @JsonProperty("interval_upper")
    Double intervalUpper;
    @JsonProperty("frequency")
    Long frequency;
}
