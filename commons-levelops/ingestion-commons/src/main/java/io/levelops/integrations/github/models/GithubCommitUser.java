package io.levelops.integrations.github.models;


import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;
import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubCommitUser.GithubCommitUserBuilder.class)
public class GithubCommitUser implements Serializable {

    @JsonProperty("email")
    String email;

    @JsonProperty("name")
    String name;

    @JsonProperty("date")
    Date date;

}
