package io.levelops.integrations.jira.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = JiraUser.JiraUserBuilder.class)
public class JiraUser {

    @JsonProperty("self")
    URI self;

    @JsonProperty("accountId")
    String accountId;

    @JsonProperty("accountType")
    String accountType;

    @JsonProperty("name")
    String name; // JIRA SERVER

    @JsonProperty("displayName")
    String displayName;

    @JsonProperty("active")
    Boolean active;

    @JsonProperty("emailAddress")
    String emailAddress;

    public String getAccountId() {
        if (StringUtils.isNotEmpty(accountId)) {
            return accountId;
        }
        if (self == null) {
            return null;
        }
        return URLEncodedUtils.parse(self, StandardCharsets.UTF_8.toString())
                .stream()
                .filter(kv -> kv.getName().equals("accountId"))
                .map(NameValuePair::getValue)
                .findAny()
                .orElse(null);
    }
}
