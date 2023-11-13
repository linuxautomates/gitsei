package io.levelops.integrations.checkmarx.models.cxsast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.integrations.checkmarx.models.Link;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxSastReportAckResponse.CxSastReportAckResponseBuilder.class)
public class CxSastReportAckResponse {
    @JsonProperty("reportId")
    String reportId;
    @JsonProperty("links")
    Map<String, Link> links;
}
