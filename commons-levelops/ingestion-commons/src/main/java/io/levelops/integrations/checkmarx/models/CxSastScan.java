package io.levelops.integrations.checkmarx.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = CxSastScan.CxSastScanBuilder.class)
public class CxSastScan {

    @JsonProperty("id")
    String id;

    @JsonProperty("project")
    CxSastProject project;

    @JsonProperty("status")
    CxSastScanStatus status;

    @JsonProperty("scanType")
    ScanType scanType;

    @JsonProperty("comment")
    String comment;

    @JsonProperty("dateAndTime")
    DateAndTime dateAndTime;

    @JsonProperty("resultStatistics")
    Map<String, Object> resultStatistics;

    @JsonProperty("scanState")
    ScanState scanState;

    @JsonProperty("owner")
    String owner;

    @JsonProperty("origin")
    String origin;

    @JsonProperty("originURL")
    String originUrl;

    @JsonProperty("initiatorName")
    String initiatorName;

    @JsonProperty("owningTeamId")
    String owningTeamId;

    @JsonProperty("isPublic")
    Boolean isPublic;

    @JsonProperty("isLocked")
    Boolean isLocked;

    @JsonProperty("isIncremental")
    Boolean isIncremental;

    @JsonProperty("scanRisk")
    Integer scanRisk;

    @JsonProperty("scanSeverity")
    Integer scanRiskSeverity;

    @JsonProperty("engineServer")
    EngineServer engineServer;

    @JsonProperty("finishedScanStatus")
    Map<String, Object> finishedScanStatus;

    @JsonProperty("partialScanReasons")
    String partialScanReasons;

    @JsonProperty("report")
    CxXmlResults report; // enriched
}
