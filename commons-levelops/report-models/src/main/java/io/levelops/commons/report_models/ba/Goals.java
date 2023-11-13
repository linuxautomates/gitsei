package io.levelops.commons.report_models.ba;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Goals.GoalsBuilder.class)
public class Goals {
    @JsonProperty("enabled")
    Boolean enabled;
    @JsonProperty("ideal_range")
    Goal idealRange;
    @JsonProperty("acceptable_range")
    Goal acceptableRange;

    @JsonIgnore
    public Goal getIdealRange() {
        return idealRange != null ? idealRange : Goal.builder().build();
    }

    @JsonIgnore
    public Goal getAcceptableRange() {
        return acceptableRange != null ? acceptableRange : Goal.builder().build();
    }
}
