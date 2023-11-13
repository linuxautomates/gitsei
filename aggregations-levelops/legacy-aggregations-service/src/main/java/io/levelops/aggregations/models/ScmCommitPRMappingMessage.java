package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.aggregations.models.messages.AggregationMessage;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Builder
@Value
@JsonDeserialize(builder = ScmCommitPRMappingMessage.ScmCommitPRMappingMessageBuilder.class)
public class ScmCommitPRMappingMessage implements AggregationMessage {
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("customer")
    private String customer;

    @JsonProperty("output_bucket")
    private String outputBucket;

    @JsonProperty("created_at_from")
    private final Instant createdAtFrom;
}
