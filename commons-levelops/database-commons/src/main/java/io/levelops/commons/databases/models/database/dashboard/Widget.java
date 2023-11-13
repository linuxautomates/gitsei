package io.levelops.commons.databases.models.database.dashboard;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Widget {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "dashboard_id")
    private String dashboardId;

    @JsonProperty(value = "name")
    private String name;

    @JsonProperty(value = "type")
    private String type;

    @JsonProperty(value = "metadata")
    private Object metadata;

    @JsonProperty(value = "query")
    private Object query;

    @JsonProperty(value = "display_info")
    private Object displayInfo;

    @JsonProperty("precalculate")
    private Boolean precalculate;

    @JsonProperty("precalculate_frequency_in_mins")
    private Integer precalculateFrequencyInMins;
}