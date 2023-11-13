package io.levelops.integrations.pagerduty.models;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.utils.ResourceUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.annotation.Nullable;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor(onConstructor = @__(@JsonIgnore))
@RequiredArgsConstructor
@SuppressWarnings("unchecked")
public class PagerDutyAlert implements PagerDutyEntity {
    @JsonProperty("id")
    private String id;
    @JsonProperty("summary")
    private String summary;
    @JsonProperty("status")
    private String status;
    @JsonProperty("alert_key")
    private String alertKey;
    @JsonProperty("severity")
    private Severity severity;
    // @Setter(AccessLevel.PRIVATE)
    // @JsonUnwrapped(prefix = "body_")
    @JsonProperty("body_details")
    private String body;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", shape = JsonFormat.Shape.STRING)
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", shape = JsonFormat.Shape.STRING)
    @JsonProperty("resolved_at")
    private Date resolvedAt;

    @JsonProperty("service")
    private PagerDutyService service;
    @JsonProperty("incident")
    private PagerDutyIncident incident;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Body.BodyBuilder.class)
    public static class Body {
        @JsonProperty("details")
        private Map<String, Object> details;
    }

    public static enum Status {
        TRIGGERED, RESOLVED;

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

    static final class PagerDutyAlertBuilderImpl extends PagerDutyAlertBuilder {
        // public PagerDutyAlertBuilder bodyDetails(final Object value){
        // super.body(Body.builder().details((Map<String, Object>)value).build());
        // return this;
        // }
    }

    @Override
    public PagerDutyIngestionDataType getIngestionDataType() {
        return PagerDutyIngestionDataType.ALERT;
    }

    @Override
    @JsonIgnore
    public Long getUpdatedAt() {
        return MoreObjects.firstNonNull(this.resolvedAt, this.createdAt).toInstant().getEpochSecond();
    }

    @JsonProperty("body")
    public void setBodyDetails(Object value) {
        System.out.println(value);
        if (value instanceof Map) {
            value = ((Map<String, Object>) value).get("details");
            if (value != null && value instanceof Map) {
                try {
                    this.body = DefaultObjectMapper.get().writeValueAsString(value);
                    return;
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
        
        if (value instanceof String) {
            this.body = (String) value;
        }
    }

    // @JsonProperty("body")
    // public void setBodyDetails(final String value){
    //     this.body = value;
    // }

    // @JsonAnySetter()
    // public void setBodyDetails(final String key, final Object value){
    //     if("body_details".equals(key)) this.body = Body.builder().details((Map<String, Object>)value).build();
    // }

    public enum Severity {
        info, warning, error, critical;

        @JsonCreator
        public Severity fromString(String value){
            return EnumUtils.getEnumIgnoreCase(Severity.class, value);
        }
    }
}