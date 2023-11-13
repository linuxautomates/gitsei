package io.levelops.api.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import io.levelops.commons.databases.models.database.SamlConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SamlConfigResponse {
    @JsonUnwrapped
    SamlConfig samlConfig;

    @JsonProperty(value = "default_relay_state")
    String defaultRelayState;

    @JsonProperty(value = "acs_url")
    String acsUrl;

    @JsonProperty(value = "sp_id")
    String spId;
}
