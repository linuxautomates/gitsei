package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OktaLinkedObject.OktaLinkedObjectBuilder.class)
public class OktaLinkedObject {

    @JsonProperty
    ObjectValue primary;

    @JsonProperty
    ObjectValue associated;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ObjectValue.ObjectValueBuilder.class)
    public static class ObjectValue {

        @JsonProperty("name")
        String name;

        @JsonProperty("title")
        String title;

        @JsonProperty("description")
        String description;

        @JsonProperty("type")
        String type;
    }
}
