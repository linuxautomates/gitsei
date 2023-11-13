package io.levelops.integrations.gcs.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.sinks.SinkIngestionResult;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GcsDataResult.GcsDataResultBuilder.class)
public class GcsDataResult implements SinkIngestionResult, Serializable {
    @JsonProperty("uri")
    private String uri;

    @JsonProperty("html_uri")
    private String htmlUri;

    @JsonProperty("blob_id")
    private BlobId blobId;

}