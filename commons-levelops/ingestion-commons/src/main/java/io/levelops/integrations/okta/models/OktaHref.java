package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OktaHref.OktaHrefBuilder.class)
public class OktaHref {

    @JsonProperty
    String name;

    @JsonProperty
    String href;

    @JsonProperty
    String type;
}
