package io.levelops.integrations.okta.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OktaLinkedUserValues.OktaLinkedUserValuesBuilder.class)
public class OktaLinkedUserValues {

    @JsonProperty("_links")
    Links links;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Links.LinksBuilder.class)
    public static class Links {

        @JsonProperty
        OktaHref self;
    }
}
