package io.levelops.integrations.gcs.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlobId.BlobIdBuilder.class)
public class BlobId implements Serializable {
    @JsonProperty("bucket")
    String bucket;
    @JsonProperty("name")
    String name;
    @JsonProperty("generation")
    Long generation;
}