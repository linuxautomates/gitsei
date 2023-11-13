package io.levelops.commons.faceted_search.db.models.workitems;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsExtensibleField.EsExtensibleFieldBuilder.class)
public class EsExtensibleField {

    @JsonProperty("name")
    String name;

    @JsonProperty("int")
    Integer intValue;

    @JsonProperty("long")
    Long longValue;

    @JsonProperty("str")
    String strValue;

    @JsonProperty("date")
    Timestamp dateValue;

    @JsonProperty("arr")
    Object arrValue;

    @JsonProperty("bool")
    Boolean boolValue;

    @JsonProperty("float")
    Float floatValue;
}
