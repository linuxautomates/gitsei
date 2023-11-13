package io.levelops.aggregations.models.messages;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@NoArgsConstructor
public class MultiAppAggMsg implements AggregationMessage{
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("customer")
    private String customer;

    @JsonProperty("output_bucket")
    private String outputBucket;

    @JsonProperty("product_id")
    private String productId;

    @JsonProperty("integration_ids")
    private List<String> integrationIds;

    @JsonProperty("integration_types")
    private List<String> integrationTypes;

    @Builder
    public MultiAppAggMsg(String customer, String outputBucket, List<String> integrationIds,
                          List<String> integrationTypes, String productId) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(customer));
        Preconditions.checkArgument(StringUtils.isNotEmpty(productId));
        Preconditions.checkArgument(StringUtils.isNotEmpty(outputBucket));
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(integrationIds));
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(integrationTypes));
        this.customer = customer;
        this.productId = productId;
        this.outputBucket = outputBucket;
        this.integrationIds = integrationIds;
        this.integrationTypes = integrationTypes;
        this.messageId = UUID.randomUUID().toString();
    }
}
