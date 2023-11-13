package io.levelops.aggregations.models.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@NoArgsConstructor
public class AppAggMessage implements AggregationMessage {
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("customer")
    private String customer;

    @JsonProperty("output_bucket")
    private String outputBucket;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("integration_type")
    private String integrationType;

    @Builder
    public AppAggMessage(String customer, String outputBucket, String integrationId,
                         String integrationType, String productId) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(customer));
        Preconditions.checkArgument(StringUtils.isNotEmpty(outputBucket));
        Preconditions.checkArgument(StringUtils.isNotEmpty(integrationId));
        Preconditions.checkArgument(StringUtils.isNotEmpty(integrationType));
        this.customer = customer;
        this.productId = productId;
        this.outputBucket = outputBucket;
        this.integrationId = integrationId;
        this.integrationType = integrationType;
        this.messageId = UUID.randomUUID().toString();
    }
}
