package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraMyself.JiraMyselfBuilder.class)
public class JiraMyself {
    @JsonProperty("self")
    String self;
    @JsonProperty("key")
    String key;
    @JsonProperty("accountId")
    String accountId;
    @JsonProperty("name")
    String name;
    @JsonProperty("emailAddress")
    String emailAddress;
    @JsonProperty("displayName")
    String displayName;
    @JsonProperty("active")
    Boolean active;
    @JsonProperty("timeZone")
    String timeZone;
    @JsonProperty("locale")
    String locale;

    /*
    {
        "groups": {
            "size": 6,
            "items": []
        },
        "applicationRoles": {
            "size": 2,
            "items": []
        },
        "expand": "groups,applicationRoles"
    }
     */
}
