package io.levelops.ingestion.integrations.custom.k8s.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = K8sCreatePodQuery.K8sCreatePodQueryBuilder.class)
public class K8sCreatePodQuery implements IntegrationQuery {

    @JsonProperty("integration_key")
    IntegrationKey integrationKey;

    @JsonProperty("namespace")
    String namespace;
    @JsonProperty("yaml_resource")
    String yamlResource;
    @JsonProperty("name_suffix")
    String nameSuffix; // optional
    @JsonProperty("timeout_in_minutes")
    Integer timeoutInMinutes;
    @JsonProperty("pod_deletion")
    PodDeletion podDeletion;

    public enum PodDeletion {
        ALWAYS,
        ONLY_IF_SUCCESSFUL,
        NEVER;

        @Nullable
        @JsonCreator
        public static PodDeletion fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(PodDeletion.class, value);
        }

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }
    }

}
