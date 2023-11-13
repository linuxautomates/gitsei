package io.levelops.commons.databases.models.database.pagerduty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbPagerDutyAlert.DbPagerDutyAlertBuilder.class)
public class DbPagerDutyAlert {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("pd_service_id")
    private UUID pdServiceId;
    @JsonProperty("pd_id")
    private String pdId;
    @JsonProperty("incident_id")
    private UUID incidentId;
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("severity")
    private String severity;
    @JsonProperty("status")
    private String status;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
    @JsonProperty("last_status_at")
    private Instant lastStatusAt;
    @JsonProperty("details")
    private Map<String, Object> details;
}