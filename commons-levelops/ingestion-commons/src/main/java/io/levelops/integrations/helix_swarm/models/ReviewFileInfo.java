package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = ReviewFileInfo.ReviewFileInfoBuilder.class)
public class ReviewFileInfo {
    @JsonProperty("depotFile")
    String depotFile;
    @JsonProperty("action")
    String action;
    @JsonProperty("type")
    String type;
    @JsonProperty("rev")
    String rev;
    @JsonProperty("fileSize")
    String fileSize;
    @JsonProperty("digest")
    String digest;
}
