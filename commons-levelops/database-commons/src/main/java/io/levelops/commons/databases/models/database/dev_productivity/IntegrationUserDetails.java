package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = IntegrationUserDetails.IntegrationUserDetailsBuilder.class)
public class IntegrationUserDetails {

    @JsonProperty("integration_id")
    private int integrationId;

    @JsonProperty("integration_type")
    private IntegrationType integrationType;

    @JsonProperty("integration_user_id")
    private UUID integrationUserId;

    @JsonProperty("cloud_id")
    private String cloudId;

    @JsonProperty("display_name")
    private String displayName;
}
