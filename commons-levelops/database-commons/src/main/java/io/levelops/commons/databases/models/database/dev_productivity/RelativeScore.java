package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = RelativeScore.RelativeScoreBuilder.class)
public class RelativeScore {

    @JsonProperty("key")
    private Long key;

    @JsonProperty("additional_key")
    private String additionalKey;

    @JsonProperty("interval")
    private ReportIntervalType interval;

    @JsonProperty("report")
    private List<RelativeDevProductivityReport> reportList;

}