package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.aggregations.models.messages.AggregationMessage;
import lombok.Builder;
import lombok.Value;

@Builder
@Value
@JsonDeserialize(builder = CiCdArtifactMappingMessage.CiCdArtifactMappingMessageBuilder.class)
public class CiCdArtifactMappingMessage implements AggregationMessage {

    @JsonProperty("message_id")
    String messageId;

    @JsonProperty("customer")
    String customer;

    @JsonProperty("output_bucket")
    String outputBucket;

}
