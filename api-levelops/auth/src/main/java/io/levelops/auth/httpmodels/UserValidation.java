package io.levelops.auth.httpmodels;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class UserValidation {

    @JsonProperty(value = "is_valid_email")
    Boolean isValidEmail;

    @JsonProperty(value = "is_valid_company")
    Boolean isValidComapny;

    @JsonProperty(value = "is_multi_tenant")
    Boolean isMultiTenant;

    @JsonProperty(value = "is_first_time_user")
    Boolean isFirstTimeUser;

    @JsonProperty(value = "is_sso_enabled")
    Boolean isSSOEnabled;

    @JsonProperty(value = "is_password_enabled")
    Boolean isPasswordEnabled;

    @JsonProperty(value = "status")
    Boolean status;

    @JsonProperty(value = "company")
    String company;

    @JsonProperty(value = "first_name")
    String firstName;

    @JsonProperty(value = "error_message")
    String errorMessage;

}
