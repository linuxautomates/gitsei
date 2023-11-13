package io.levelops.commons.databases.models.database.dev_productivity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = OrgUserDetails.OrgUserDetailsBuilder.class)
public class OrgUserDetails {

    @JsonProperty("org_user_id")
    private UUID orgUserId;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("custom_fields")
    Map<String, Object> customFields;

    @JsonProperty("dev_productivity_profiles")
    private List<DevProductivityProfileInfo> devProductivityProfiles;

    @JsonProperty("integration_user_details")
    private List<IntegrationUserDetails> IntegrationUserDetailsList;

}
