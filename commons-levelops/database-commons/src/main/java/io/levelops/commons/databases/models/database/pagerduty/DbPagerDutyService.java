package io.levelops.commons.databases.models.database.pagerduty;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbPagerDutyAlert.DbPagerDutyAlertBuilder.class)
public class DbPagerDutyService {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("service_id")
    private UUID serviceId;
    @JsonProperty("integration_id")
    private Integer integrationId;
    @JsonProperty("pd_id")
    private String pdId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("escalation_policies")
    private Set<UUID> escalationPolicies;
    @JsonProperty("created_at")
    private Instant createdAt;
    @JsonProperty("updated_at")
    private Instant updatedAt;
}