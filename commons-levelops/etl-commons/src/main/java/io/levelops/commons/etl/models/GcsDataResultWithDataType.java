package io.levelops.commons.etl.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.gcs.models.GcsDataResult;
import lombok.Builder;
import lombok.Value;

// This corresponds to 1 file in GCS
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GcsDataResultWithDataType.GcsDataResultWithDataTypeBuilder.class)
public class GcsDataResultWithDataType {
    @JsonProperty("gcs_data_result")
    GcsDataResult gcsDataResult;

    @JsonProperty("data_type_name")
    String dataTypeName;

    @JsonProperty("ingestion_job_id")
    String ingestionJobId;

    @JsonProperty("index")
    Integer index;
}
