package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.Date;
import java.util.Set;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = PagerDutyIncident.PagerDutyIncidentBuilder.class)
public class PagerDutyIncident implements PagerDutyEntity {
    @JsonProperty("id")
    private String id;
    @JsonProperty("title")
    private String title;
    @JsonProperty("description")
    private String description;
    @JsonProperty("urgency")
    private Urgency urgency;
    @JsonProperty("status")
    private String status;
    @JsonUnwrapped(prefix = "priority_")
    @JsonProperty("priority")
    private Priority priority;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", shape = JsonFormat.Shape.STRING)
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", shape = JsonFormat.Shape.STRING)
    @JsonProperty("last_status_change_at")
    private Date lastStatusChangeAt;
    @JsonProperty("service")
    private PagerDutyService service;
    @JsonProperty("acknowledgements")
    private Set<Acknowledgement> acknowledgements;
    @JsonProperty("last_status_change_by")
    private Acknowledger lastStatusChangeBy;

    @Setter
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Priority {
        @JsonProperty("summary")
        private String summary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Acknowledgement {
        @JsonProperty("at")
        private String at;
        // @JsonUnwrapped(prefix = "")
        @JsonProperty("acknowledger")
        private Acknowledger acknowledger;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Acknowledger {
        @JsonProperty("id")
        private String id;
        @JsonProperty("type")
        private String type;
        @JsonProperty("summary")
        private String name;
    }

    public static enum Status {
        TRIGGERED,
        ACKNOWLEDGED,
        RESOLVED;

        @JsonCreator
        @Nullable
        public static Status fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(Status.class, value);
        }
        
        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

    @Override
    public PagerDutyIngestionDataType getIngestionDataType(){
        return PagerDutyIngestionDataType.INCIDENT;
    }

    @Override
    @JsonIgnore
    public Long getUpdatedAt() {
        try {
            return MoreObjects.firstNonNull(this.lastStatusChangeAt, this.createdAt).toInstant().getEpochSecond();
        }
        catch (NullPointerException e){
            throw new RuntimeException(String.format("Couldn't get a valid update time %s", toString()), e);
        }
    }

    public enum Urgency {
        high, low;

        @JsonCreator
        public Urgency fromString(String value){
            return EnumUtils.getEnumIgnoreCase(Urgency.class, value);
        }
    }
}