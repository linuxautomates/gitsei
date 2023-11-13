package io.levelops.commons.databases.models.database.pagerduty;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
public class DbPagerDutyIncident {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("pd_service_id")
    private UUID pdServiceId;
    @JsonProperty("pd_id")
    private String pdId;
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("urgency")
    private String urgency;
    @JsonProperty("priority")
    private String priority;
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
    private Set<DbPagerDutyStatus> statuses;
}