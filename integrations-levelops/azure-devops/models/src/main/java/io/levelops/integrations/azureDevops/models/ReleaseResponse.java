package io.levelops.integrations.azureDevops.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReleaseResponse.ReleaseResponseBuilder.class)
public class ReleaseResponse {

    @JsonProperty("value")
    List<AzureDevopsRelease> releases;

    @JsonProperty("count")
    Integer count;

    String continuationToken;
}
