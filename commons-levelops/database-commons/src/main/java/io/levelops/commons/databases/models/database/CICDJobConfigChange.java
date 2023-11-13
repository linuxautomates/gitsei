package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CICDJobConfigChange.CICDJobConfigChangeBuilder.class)
public class CICDJobConfigChange {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("cicd_job_id")
    private final UUID cicdJobId;

    @JsonProperty("change_time")
    private final Instant changeTime;

    @JsonProperty("change_type")
    private final String changeType;

    @JsonProperty("cicd_user_id")
    private final String cicdUserId;
}
