package io.levelops.commons.etl.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Lists;
import io.levelops.commons.functional.IngestionFailure;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import io.levelops.ingestion.models.controlplane.JobDTO;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IngestionLogDTO.IngestionLogDTOBuilder.class)
public class EtlLogDTO {
    @JsonProperty("id")
    String id;
    @JsonProperty("status")
    String status;
    @JsonProperty("attempt_count")
    Integer attemptCount;
    @JsonProperty("created_at")
    Long createdAt;
    @JsonProperty("updated_at")
    Long updatedAt;
    @JsonProperty("elapsed")
    Long elapsed;
    @JsonProperty("agg_processor_name")
    String aggProcessorName;
    @JsonProperty("isFull")
    Boolean isFull;
    @JsonProperty("tags")
    List<String> tags;
    @Nullable
    @JsonProperty("ingestion_job_ids")
    List<String> ingestionJobIds;


    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd hh:mm", Locale.US);

    public static EtlLogDTO fromDbJobInstance(ObjectMapper objectMapper, DbJobInstance jobInstance) {
        List<String> ingestionIds = null;
        if (jobInstance.getPayload() != null) {
            ingestionIds = Lists.newArrayList(jobInstance.getPayload().getIngestionJobStatusMap().keySet());
        }
        return EtlLogDTO.builder()
                .id(jobInstance.getId().toString())
                .status(jobInstance.getStatus().toString())
                .attemptCount(jobInstance.getAttemptCount())
                .createdAt(jobInstance.getCreatedAt().toEpochMilli())
                .updatedAt(jobInstance.getUpdatedAt().toEpochMilli())
                .elapsed(jobInstance.getUpdatedAt().toEpochMilli() - jobInstance.getCreatedAt().toEpochMilli())
                .aggProcessorName(jobInstance.getAggProcessorName())
                .isFull(jobInstance.getIsFull())
                .tags(Lists.newArrayList(jobInstance.getTags()))
                .ingestionJobIds(ingestionIds)
                .build();
    }
}
