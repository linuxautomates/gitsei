package io.levelops.integrations.sonarqube.models;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@ToString
@JsonDeserialize(builder = User.UserBuilder.class)

public class User {

    @JsonProperty("login")
    String login;

    @JsonProperty("name")
    String name;

    @JsonProperty("active")
    Boolean active;

    @JsonProperty("avatar")
    String avatar;

    @JsonProperty("email")
    String email;

    @JsonProperty("tokensCount")
    long tokensCount;

    @JsonProperty("local")
    Boolean local;

    @JsonProperty("externalIdentity")
    String externalIdentity;

    @JsonProperty("externalProvider")
    String externalProvider;

    @JsonProperty("groups")
    List<String> groups;

    @JsonProperty("lastConnectionDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssZZZZ")
    Date lastConnectionDate;



}
