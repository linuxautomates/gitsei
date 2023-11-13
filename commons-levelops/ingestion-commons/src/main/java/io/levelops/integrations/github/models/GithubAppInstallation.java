package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import javax.annotation.Nullable;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubAppInstallation.GithubAppInstallationBuilder.class)
public class GithubAppInstallation {
    @JsonProperty("id")
    String id;

    @JsonProperty("account")
    GithubAccount account;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubAccount.GithubAccountBuilder.class)
    public static class GithubAccount {
        @JsonProperty("type")
        String type;

        @JsonProperty("login")
        String login;

        @JsonProperty("id")
        Long id;

        @JsonProperty("node_id")
        String nodeId;

        @JsonProperty("url")
        String url;

        @JsonProperty("repos_url")
        String reposUrl;

        @JsonProperty("events_url")
        String eventsUrl;

        @JsonIgnore
        @Nullable
        public GithubOrganization toOrganization() {
            if (type == null || !type.equals("Organization")) {
                return null;
            }
            return GithubOrganization.builder()
                    .login(login)
                    .id(id)
                    .nodeId(nodeId)
                    .url(url)
                    .reposUrl(reposUrl)
                    .eventsUrl(eventsUrl)
                    .build();
        }
    }
}
