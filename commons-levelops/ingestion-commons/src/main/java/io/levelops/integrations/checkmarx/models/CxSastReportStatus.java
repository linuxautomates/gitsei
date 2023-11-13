package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxSastReportStatus.CxSastReportStatusBuilder.class)
public class CxSastReportStatus {

    @JsonProperty("link")
    Link link;

    @JsonProperty("contentType")
    String contentType;

    @JsonProperty("status")
    Status status;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Status.StatusBuilder.class)
    public static class Status {

        @JsonProperty("id")
        String id;

        @JsonProperty("value")
        String value;

    }
}
