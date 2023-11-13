package io.levelops.bullseye_converter_commons.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = ConversionRequest.ConversionRequestBuilder.class)
public class ConversionRequest {
    @JsonProperty("customer")
    private final String customer;
    @JsonProperty("reference_id")
    private final String referenceId;
    @JsonProperty("job_run_id")
    private final UUID jobRunId;
    @JsonProperty("file_name")
    private final String fileName;
}
