package io.levelops.aggregations.models.messages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.UUID;

import javax.annotation.Nonnull;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JenkinsLogTriagingMessage.JenkinsLogTriagingMessageBuilder.class)
public class JenkinsLogTriagingMessage implements AggregationMessage {
    @JsonProperty("message_id")
    private String messageId;
    @NonNull
    @JsonProperty("company")
    private String company;
    // db id
    @Nonnull
    @JsonProperty("instance_id")
    private UUID instanceId;
    @Nonnull
    @JsonProperty("instance_name")
    private String instanceName;
    @Nonnull
    @JsonProperty("job_name")
    private String jobName;
    @NonNull
    @JsonProperty("job_status")
    private String jobStatus;
    // db id
    @NonNull
    @JsonProperty("job_id")
    private UUID jobId;
    // db id
    @NonNull
    @JsonProperty("job_run_id")
    private UUID jobRunId;
    // db id
    @JsonProperty("stage_id")
    private UUID stageId;
    @JsonProperty("step_id")
    // db id
    private UUID stepId;
    @JsonProperty("url")
    private String url;
    @NonNull
    @JsonProperty("log_location")
    private String logLocation;
    @JsonProperty("logs_bucket")
    private String logBucket;

    @Override
    public String getCustomer() {
        return this.company;
    }

    @Override
    public String getOutputBucket() {
        return this.logBucket;
    }
}