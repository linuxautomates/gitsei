package io.levelops.aggregations_shared.helpers.harnessng;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HarnessNGArtifactsOutcome.HarnessNGArtifactsOutcomeBuilder.class)
public class HarnessNGArtifactsOutcome {

    @JsonProperty("artifacts")
    Artifacts artifacts;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Artifacts.ArtifactsBuilder.class)
    public static class Artifacts {

        @JsonProperty("primary")
        Primary primary;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Primary.PrimaryBuilder.class)
        public static class Primary {
            @JsonProperty("image")
            String image;

        }
    }

}
