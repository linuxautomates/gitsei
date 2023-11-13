package io.levelops.integrations.azureDevops.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = TeamSetting.TeamSettingBuilder.class)
public class TeamSetting {

    @JsonProperty("field")
    Field field;

    @JsonProperty("defaultValue")
    String defaultValue;

    @JsonProperty("values")
    List<FieldValue> values;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Field.FieldBuilder.class)
    public static class Field {

        @JsonProperty("referenceName")
        String referenceName;

        @JsonProperty("url")
        String url;

    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = FieldValue.FieldValueBuilder.class)
    public static class FieldValue {

        @JsonProperty("value")
        String value;

        @JsonProperty("includeChildren")
        Boolean includeChildren;

    }
}

