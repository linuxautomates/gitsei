package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxSastScanStatus.CxSastScanStatusBuilder.class)
public class CxSastScanStatus {

    @JsonProperty("id")
    String id;

    @JsonProperty("name")
    String name;

    @JsonProperty("details")
    Details details;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Details.DetailsBuilder.class)
    public static class Details {
        @JsonProperty("stage")
        String stage;

        @JsonProperty("step")
        String step;
    }
}
