package io.levelops.aggregations.parsers;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import io.levelops.aggregations.models.messages.AggregationMessage;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.UUID;

//ToDo: Move to commons. Keeping this here for quicker dev and testing. Will move to commons before release.
@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@ToString
@NoArgsConstructor
public class PluginAggMessage implements AggregationMessage {
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("customer")
    private String customer;

    @JsonProperty("output_bucket")
    private String outputBucket;

    @JsonProperty("product_ids")
    private List<String> productIds;

    @JsonProperty("plugin_result_id")
    private String pluginResultId;

    @Builder
    public PluginAggMessage(String customer, String outputBucket, List<String> productIds, String pluginResultId) {
        Preconditions.checkArgument(StringUtils.isNotEmpty(customer));
        Preconditions.checkArgument(StringUtils.isNotEmpty(outputBucket));
        Preconditions.checkArgument(CollectionUtils.isNotEmpty(productIds));
        Preconditions.checkArgument(StringUtils.isNotEmpty(pluginResultId));
        this.customer = customer;
        this.outputBucket = outputBucket;
        this.productIds = productIds;
        this.pluginResultId = pluginResultId;
        this.messageId = UUID.randomUUID().toString();
    }
}
