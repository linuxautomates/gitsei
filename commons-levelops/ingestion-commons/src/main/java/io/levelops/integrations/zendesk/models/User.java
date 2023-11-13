package io.levelops.integrations.zendesk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Bean describing a user from https://developer.zendesk.com/rest_api/docs/support/users#content
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = User.UserBuilder.class)
public class User {

    @JsonProperty
    Long id;

    @JsonProperty
    String email;

    @JsonProperty
    String name;

    @JsonProperty
    Boolean active;

    @JsonProperty
    String alias;

    @JsonProperty("chat_only")
    Boolean chatOnly;

    @JsonProperty("created_at")
    Date createdAt;

    @JsonProperty("custom_role_id")
    Long customRoleId;

    @JsonProperty("role_type")
    Long roleType;

    @JsonProperty
    String details;

    @JsonProperty("external_id")
    String externalId;

    @JsonProperty("last_login_at")
    Date lastLoginAt;

    @JsonProperty
    String locale;

    @JsonProperty("locale_id")
    Long localeId;

    @JsonProperty
    Boolean moderator;

    @JsonProperty
    String notes;

    @JsonProperty("only_private_comments")
    Boolean onlyPrivateComments;

    @JsonProperty("organization_id")
    Long organizationId;

    @JsonProperty("default_group_id")
    Long defaultGroupId;

    @JsonProperty
    String phone;

    @JsonProperty("shared_phone_number")
    Boolean sharedPhoneNumber;

    @JsonProperty
    RequestComment.Attachment photo;

    @JsonProperty("restricted_agent")
    Boolean restrictedAgent;

    @JsonProperty
    String role;

    @JsonProperty
    Boolean shared;

    @JsonProperty("shared_agent")
    Boolean sharedAgent;

    @JsonProperty
    String signature;

    @JsonProperty
    Boolean suspended;

    @JsonProperty
    List<String> tags;

    @JsonProperty("ticket_restriction")
    String ticketRestriction;

    @JsonProperty("time_zone")
    String timeZone;

    @JsonProperty("two_factor_auth_enabled")
    Boolean twoFactorAuthEnabled;

    @JsonProperty("updated_at")
    Date updatedAt;

    @JsonProperty
    String url;

    @JsonProperty("user_fields")
    Map<String, String> userFields;

    @JsonProperty
    Boolean verified;

    @JsonProperty("report_csv")
    Boolean reportCsv;
}
