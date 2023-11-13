package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CoverityAttributes.CoverityAttributesBuilder.class)
public class CoverityAttributes {

    @JsonProperty("attribute_definition_id")
    DefectsAttributes attributeDefinitionId;

    @JsonProperty("attribute_value_id")
    DefectsAttributes attributeValueId;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = DefectsAttributes.DefectsAttributesBuilder.class)
    public static class DefectsAttributes {

        @JsonProperty("name")
        String name;
    }
}
