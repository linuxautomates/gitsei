package io.levelops.commons.databases.models.dora;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DoraSingleStateDTO.DoraSingleStateDTOBuilder.class)
public class DoraSingleStateDTO {

    @JsonProperty("count_per_day")
    Double countPerDay;

    @JsonProperty("count_per_week")
    Double countPerWeek;

    @JsonProperty("count_per_month")
    Double countPerMonth;

    @JsonProperty("failure_rate")
    Double failureRate;

    @JsonProperty("band")
    Band band;

    @JsonProperty("total_deployment")
    Integer totalDeployment;

    @JsonProperty("is_absolute")
    Boolean isAbsolute;

    @Getter
    public enum Band {
        ELITE,
        MEDIUM,
        HIGH,
        LOW
    }
}
