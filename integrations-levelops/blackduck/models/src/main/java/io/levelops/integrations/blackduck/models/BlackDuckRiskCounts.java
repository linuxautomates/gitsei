package io.levelops.integrations.blackduck.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = BlackDuckRiskCounts.BlackDuckRiskCountsBuilder.class)
public class BlackDuckRiskCounts {

    @JsonProperty("counts")
    List<BlackDuckCountType> blackDuckCountTypeList;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BlackDuckCountType.BlackDuckCountTypeBuilder.class)
    public static class BlackDuckCountType {
        @JsonProperty("countType")
        String countType;

        @JsonProperty("count")
        String count;
    }
}
