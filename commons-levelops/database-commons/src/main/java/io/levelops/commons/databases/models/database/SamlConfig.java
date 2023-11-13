package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SamlConfig {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "enabled")
    private Boolean enabled;

    @JsonProperty(value = "idp_id")
    private String idpId;

    @JsonProperty(value = "idp_sso_url")
    private String idpSsoUrl;

    //this is actually the base64 encoded version of the actual certificate. because - newlines.
    @JsonProperty(value = "idp_cert")
    private String idpCert;

    @JsonProperty(value = "created_at")
    private Long createdAt;
}
