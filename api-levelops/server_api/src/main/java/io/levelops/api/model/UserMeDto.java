package io.levelops.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.User;
import lombok.Builder;
import lombok.Value;
import java.util.Set;

@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
@JsonDeserialize(builder = UserMeDto.UserMeDtoBuilder.class)
public class UserMeDto {
    @JsonUnwrapped
    User user;

    @JsonProperty("metadata")
    Object metadata;
    @JsonProperty("landing_page")
    String landingPage;
    @JsonProperty("license")
    String license;
    @JsonProperty("entitlements")
    Set<String> entitlements;
}