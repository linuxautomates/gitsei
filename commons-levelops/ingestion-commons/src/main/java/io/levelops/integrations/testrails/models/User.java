package io.levelops.integrations.testrails.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

/**
 * Bean definition for testrails user
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = User.UserBuilder.class)
public class User {

    @JsonProperty("id")
    Integer id;

    @JsonProperty("name")
    String name;

    @JsonProperty("email")
    String email;

    @JsonProperty("is_active")
    Boolean isActive;

    @JsonProperty("role_id")
    Integer roleId;

    @JsonProperty("role")
    String role;
}
