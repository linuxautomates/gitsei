package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SignatureLogEvent.SignatureLogEventBuilder.class)
public class SignatureLogEvent {

    @JsonProperty("id")
    String id;

    @JsonProperty("signature_id")
    String signatureId;

    @JsonProperty("product_id")
    String productId;

    @JsonProperty("successful")
    Boolean successful;

    @JsonProperty("metadata")
    Map<String, Object> metadata;

    @JsonProperty("results")
    Map<String, Object> results;

    @JsonProperty("labels")
    Map<String, Object> labels;

    @JsonProperty("timestamp")
    Long timestamp;

}
