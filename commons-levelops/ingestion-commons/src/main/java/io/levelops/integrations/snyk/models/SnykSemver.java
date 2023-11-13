package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykSemver.SnykSemverBuilder.class)
public class SnykSemver {
    @JsonProperty("unaffected")
    private final String unaffected;
    @JsonProperty("vulnerable")
    private final List<String> vulnerable;
}
