package io.levelops.commons.databases.models.database.signature.operators;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.signature.SignatureOperator;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = EqualsOperator.EqualsOperatorBuilder.class)
public class EqualsOperator implements SignatureOperator {

    public static final String TYPE = "equals";

    @JsonProperty("id")
    String id;

    @JsonProperty("type")
    String type = TYPE;

    @JsonProperty("field")
    String field;

    @JsonProperty("value")
    String value;
}
