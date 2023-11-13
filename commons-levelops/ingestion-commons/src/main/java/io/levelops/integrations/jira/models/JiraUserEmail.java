package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraUserEmail.JiraUserEmailBuilder.class)
public class JiraUserEmail {
    @JsonProperty("accountId")
    String accountId;

    @JsonProperty("email")
    String email;
}
