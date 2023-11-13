package io.levelops.commons.databases.models.database.pagerduty;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class DbPagerDutyStatus {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("pd_incident_id")
    private UUID pdIncidentId;
    @JsonProperty("pd_user_id")
    private UUID pdUserId;
    @JsonProperty("status")
    private String status;
    @JsonProperty("timestamp")
    private Instant timestamp;
    private DbPagerDutyUser user;
}