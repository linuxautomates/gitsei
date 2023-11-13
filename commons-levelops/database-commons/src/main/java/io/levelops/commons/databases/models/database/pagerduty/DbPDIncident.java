package io.levelops.commons.databases.models.database.pagerduty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbPDIncident {
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
    @JsonProperty("user_names")
    private List<String> userNames;
    @JsonProperty("user_ids")
    private List<UUID> userids;
    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("integration_id")
    private String integrationId;
    @JsonProperty("incident_resolved_at")
    private Instant incidentResolvedAt;
    @JsonProperty("incident_acknowledged_at")
    private Instant incidentAcknowledgedAt;
    @JsonProperty("incident_statuses")
    private List<String> incidentStatuses;
    @JsonProperty("details")
    private Map<String, Object> details;
    private Set<DbPagerDutyStatus> statuses;
}
