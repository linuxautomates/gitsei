package io.levelops.commons.databases.models.database;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.databases.models.database.access.RoleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Builder.Default;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {
    @JsonProperty(value = "id")
    private String id;

    @JsonProperty(value = "first_name")
    private String firstName;

    @JsonProperty(value = "last_name")
    private String lastName;

    @JsonProperty(value = "bcrypt_password")
    private String bcryptPassword;

    @JsonProperty(value = "user_type")
    private RoleType userType;

    @JsonProperty(value = "saml_auth_enabled")
    private Boolean samlAuthEnabled;

    @JsonProperty(value = "password_auth_enabled")
    private Boolean passwordAuthEnabled;

    @JsonProperty(value = "mfa_enforced")
    private Boolean mfaEnforced;

    // This property is only set to true after a user has successfully enrolled in MFA
    @JsonProperty(value = "mfa_enabled")
    private Boolean mfaEnabled;

    @JsonProperty(value = "mfa_enrollment_end")
    private Instant mfaEnrollmentEndAt;

    @JsonGetter("mfa_enrollment_end")
    public Long getMfaEnrollmentEndAtJson() {
        return mfaEnrollmentEndAt != null ? mfaEnrollmentEndAt.getEpochSecond() : null;
    }

    @JsonProperty(value = "scopes")
    private Map<String, List<String>> scopes;

    @JsonProperty(value = "metadata")
    private  Map<String, Object> metadata;

    @JsonProperty(value = "mfa_reset_at")
    private Instant mfaResetAt;

    @JsonGetter("mfa_reset_at")
    public Long getMfaResetAtJson() {
        return mfaResetAt != null ? mfaResetAt.getEpochSecond() : null;
    }

    @JsonProperty(value = "password_reset")
    private PasswordReset passwordResetDetails;

    @JsonProperty(value = "email")
    private String email;

    @JsonProperty(value = "updated_at")
    private Long updatedAt;

    @JsonProperty(value = "created_at")
    private Long createdAt;

    @JsonProperty(value = "company")
    private String company;

    @JsonProperty("managed_ou_ref_ids")
    private List<Integer> managedOURefIds;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    @ToString
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PasswordReset {
        @JsonProperty(value = "token")
        private String token;

        @JsonProperty(value = "expiry")
        private Long expiry;
    }
}
