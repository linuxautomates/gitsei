package io.levelops.integrations.sonarqube.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.Set;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SonarQubeIterativeScanQuery.SonarQubeIterativeScanQueryBuilder.class)
public class SonarQubeIterativeScanQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("from")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date from;

    @JsonProperty("to")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date to;

    @JsonProperty("fetchOnce")
    Boolean fetchOnce;

    /**
     * To restrict which projects will be ingested. Lowercase.
     */
    @JsonProperty("project_keys")
    Set<String> projectKeys;

    @JsonProperty("use_privileged_APIs")
    Boolean usePrivilegedAPIs;
}