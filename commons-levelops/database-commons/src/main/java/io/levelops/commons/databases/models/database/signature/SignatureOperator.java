package io.levelops.commons.databases.models.database.signature;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.levelops.commons.databases.models.database.signature.operators.EqualsOperator;
import io.levelops.commons.databases.models.database.signature.operators.RangeOperator;
import io.levelops.commons.databases.models.database.signature.operators.RegexOperator;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "type",
        visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = EqualsOperator.class, name = EqualsOperator.TYPE),
        @JsonSubTypes.Type(value = RegexOperator.class, name = RegexOperator.TYPE),
        @JsonSubTypes.Type(value = RangeOperator.class, name = RangeOperator.TYPE),
})
public interface SignatureOperator {

    @JsonProperty("id")
    String getId();

    @JsonProperty("type")
    String getType();

    @JsonProperty("field")
    String getField();
}