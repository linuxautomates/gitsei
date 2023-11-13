package io.levelops.contributor.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SEIOrgContributor.SEIOrgContributorBuilder.class)
public class SEIOrgContributor {

    @JsonProperty("org_user_id")
    private UUID orgUserId;

    @JsonProperty("org_user_ref_id")
    private String orgUserRefId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("integration_user_details")
    private List<SEIIntegrationContributor> seiIntegrationContributor;
}
