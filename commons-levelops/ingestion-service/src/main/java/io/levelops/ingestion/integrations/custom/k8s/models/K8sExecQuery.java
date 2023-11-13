package io.levelops.ingestion.integrations.custom.k8s.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = K8sExecQuery.K8sExecQueryBuilder.class)
public class K8sExecQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("namespace")
    String namespace;
    @JsonProperty("pod_name")
    String podName;
    @JsonProperty("container")
    String container; // optional
    @JsonProperty("command")
    String command;
    @JsonProperty("timeout_in_minutes")
    Integer timeoutInMinutes;


}
