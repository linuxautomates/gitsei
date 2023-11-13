package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CiCdInstanceConfig.CiCdInstanceConfigBuilder.class)
public class CiCdInstanceConfig {

        @JsonProperty("heartbeat_duration")
        Integer heartbeatDuration;

        @JsonProperty("bullseye_report_paths")
        String bullseyeReportPaths;
}
