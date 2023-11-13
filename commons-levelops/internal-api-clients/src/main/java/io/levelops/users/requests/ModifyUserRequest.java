package io.levelops.users.requests;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.dates.DateUtils;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ModifyUserRequest {
    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "first_name")
    private String firstName;

    @JsonProperty(value = "last_name")
    private String lastName;

    @JsonProperty(value = "user_type")
    private String userType;

    @JsonProperty(value = "saml_auth_enabled")
    private Boolean samlAuthEnabled;

    @JsonProperty(value = "password_auth_enabled")
    private Boolean passwordAuthEnabled;

    @JsonProperty(value = "notify_user") //used when creating user.
    private Boolean notifyUser;

    @JsonProperty(value = "mfa_enabled")
    private Boolean mfaEnabled;

    @JsonProperty(value = "metadata")
    private Map<String, Object> metadata;

    @JsonProperty(value = "mfa_enrollment_end")
    private Long mfaEnrollmentWindowExpiry; // Note: manually parsing Epoch as there was some regression in deserializing Instant (secs/ms format)

    @JsonProperty(value = "mfa_reset_at") // Note: manually parsing Epoch as there was some regression in deserializing Instant (secs/ms format)
    private Long mfaResetAt;

    @JsonProperty("managed_ou_ref_ids")
    private List<Integer> managedOURefIds;

    @JsonIgnore
    public Instant getMfaEnrollmentWindowExpiryInstant() {
        return DateUtils.fromEpochSecond(mfaEnrollmentWindowExpiry);
    }

    @JsonIgnore
    public Instant getMfaResetAtInstant() {
        return DateUtils.fromEpochSecond(mfaResetAt);
    }
}
