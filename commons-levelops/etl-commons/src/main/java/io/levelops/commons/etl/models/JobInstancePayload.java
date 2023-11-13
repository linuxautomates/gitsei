package io.levelops.commons.etl.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JobInstancePayload.JobInstancePayloadBuilder.class)
public class JobInstancePayload {
    @JsonProperty("gcs_records")
    List<GcsDataResultWithDataType> gcsRecords;

    @JsonProperty("ingestion_job_status_map")
    Map<String, IngestionJobStatus> ingestionJobStatusMap;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IngestionJobStatus.IngestionJobStatusBuilder.class)
    public static class IngestionJobStatus {
        @JsonProperty("ingestion_job_id")
        String ingestionJobId;

        // Denotes whether this ingestion job's results were fully included in the payload
        // For some cases we may want to only process a subset of an ingestion job's
        // result. And this helps us identify these cases for the purposes of
        // incremental payload determination
        @JsonProperty("is_complete")
        Boolean isComplete;
    }
}

