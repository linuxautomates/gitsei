package io.levelops.commons.databases.services.dev_productivity.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class ScmActivity {
    @JsonProperty("key")
    private String key;

    @JsonProperty("additional_key")
    private String additionalKey;

    @JsonProperty("day_of_week")
    private String dayOfWeek;

    @JsonProperty("scm_activity_type")
    private ScmActivityType scmActivityType;

    @JsonProperty("result")
    private Integer result;
}
