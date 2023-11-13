package io.levelops.commons.databases.models.database.signature.operators;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.signature.SignatureOperator;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RangeOperator.RangeOperatorBuilder.class)
public class RangeOperator implements SignatureOperator {

    public static final String TYPE = "range";

    @JsonProperty("id")
    String id;

    @JsonProperty("type")
    String type = TYPE;

    @JsonProperty("field")
    String field;

    @JsonProperty("lower_bound")
    Long lowerBound;

    @JsonProperty("upper_bound")
    Long upperBound;
}
