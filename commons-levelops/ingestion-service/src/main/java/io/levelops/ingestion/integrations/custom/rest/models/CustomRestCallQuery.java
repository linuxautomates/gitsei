package io.levelops.ingestion.integrations.custom.rest.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CustomRestCallQuery.CustomRestCallQueryBuilder.class)
public class CustomRestCallQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;
    @JsonProperty("url")
    String url;
    @JsonProperty("method")
    String method;
    @JsonProperty("headers")
    List<Header> headers;
    @JsonProperty("content_type")
    String contentType;
    @JsonProperty("body")
    String body;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Header.HeaderBuilder.class)
    public static class Header {
        @JsonProperty("key")
        String key;
        @JsonProperty("value")
        String value;
    }
}
