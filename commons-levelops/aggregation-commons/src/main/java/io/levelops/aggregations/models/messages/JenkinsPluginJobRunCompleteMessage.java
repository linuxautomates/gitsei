package io.levelops.aggregations.models.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Builder(toBuilder = true)
@Value
@JsonDeserialize(builder = JenkinsPluginJobRunCompleteMessage.JenkinsPluginJobRunCompleteMessageBuilder.class)
public class JenkinsPluginJobRunCompleteMessage implements AggregationMessage {
    @JsonProperty("message_id")
    private String messageId;

    @JsonProperty("customer")
    private String customer;

    @JsonProperty("output_bucket")
    private String outputBucket;

    @JsonProperty("json_file_path")
    private final String jsonFilePath;

    @JsonProperty("result_file_path")
    private final String resultFilePath;

    @JsonProperty("task_id")
    private final String taskId;
}
