package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraServerInfo.JiraServerInfoBuilder.class)
public class JiraServerInfo {

    @JsonProperty("baseUrl")
    String baseUrl;
    @JsonProperty("version")
    String version;
    @JsonProperty("versionNumbers")
    List<Integer> versionNumbers;
    @JsonProperty("deploymentType")
    JiraDeploymentType deploymentType;
    @JsonProperty("buildData")
    String buildData;
    @JsonProperty("serverTime")
    String serverTime;
    @JsonProperty("serverTitle")
    String serverTitle;

}
