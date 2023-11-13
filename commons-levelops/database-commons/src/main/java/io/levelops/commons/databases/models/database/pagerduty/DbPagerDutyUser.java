package io.levelops.commons.databases.models.database.pagerduty;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder(toBuilder = true)
public class DbPagerDutyUser {
    @JsonProperty("id")
    private UUID id;
    @JsonProperty("pd_id")
    private String pdId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("email")
    private String email;
    @JsonProperty("time_zone")
    private String timeZone;
}