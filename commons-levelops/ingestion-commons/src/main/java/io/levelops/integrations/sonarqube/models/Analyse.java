package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;


@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = Analyse.AnalyseBuilder.class)
public class Analyse {

    @JsonProperty("key")
    String key;

    @JsonProperty("date")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date date;

    @JsonProperty("projectVersion")
    String projectVersion;

    @JsonProperty("buildString")
    String buildString;

    @JsonProperty("manualNewCodePeriodBaseline")
    String manualNewCodePeriodBaseline;

    @JsonProperty("revision")
    String revision;

    @JsonProperty("events")
    List<Event> events;


}