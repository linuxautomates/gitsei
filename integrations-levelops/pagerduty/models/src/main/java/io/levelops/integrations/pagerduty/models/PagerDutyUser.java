package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.ingestion.models.IngestionDataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor(onConstructor = @__(@JsonIgnore))
@RequiredArgsConstructor
public class PagerDutyUser implements PagerDutyEntity {
    @JsonProperty("id")
    private String id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("email")
    private String email;
    @JsonProperty("time_zone")
    private String timeZone;
    @JsonProperty("role")
    private String role;
    @JsonProperty("type")
    private String type;

    @Override
    public Long getUpdatedAt() {
        return null;
    }

    @Override
    public IngestionDataType<PagerDutyEntity, PagerDutyResponse> getIngestionDataType() {
        return PagerDutyIngestionDataType.USER;
    }
}