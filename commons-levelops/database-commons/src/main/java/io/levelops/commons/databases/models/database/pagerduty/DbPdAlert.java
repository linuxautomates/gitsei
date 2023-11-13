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
import java.util.Map;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbPdAlert {
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
    @JsonProperty("alert_acknowledged_at")
    private Instant alertAcknowledgedAt;
    @JsonProperty("service_name")
    private String serviceName;
    @JsonProperty("integration_id")
    private String integrationId;
    @JsonProperty("details")
    private Map<String, Object> details;
}
