package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyLogEntry.PagerDutyLogEntryBuilder.class)
public class PagerDutyLogEntry implements PagerDutyEntity {
    @JsonProperty("id")
    private String id;
    @JsonProperty("type")
    private String type;
    @JsonProperty("incident")
    private PagerDutyIncident incident;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", shape = JsonFormat.Shape.STRING)
    @JsonProperty("created_at")
    private Date createdAt;

    @Override
    public PagerDutyIngestionDataType getIngestionDataType() {
        return PagerDutyIngestionDataType.LOG_ENTRY;
    }

    @Override
    @JsonIgnore
    public Long getUpdatedAt() {
        if(this.incident != null){
            return MoreObjects.firstNonNull(this.incident.getLastStatusChangeAt(), this.createdAt).toInstant().getEpochSecond();
        }
        return this.createdAt.toInstant().getEpochSecond();
    }

    @JsonIgnore
    public Long getIncidentDate() {
        if(this.incident == null){
            return null;
        }
        return MoreObjects.firstNonNull(this.incident.getLastStatusChangeAt(), this.createdAt).toInstant().getEpochSecond();
    }
}