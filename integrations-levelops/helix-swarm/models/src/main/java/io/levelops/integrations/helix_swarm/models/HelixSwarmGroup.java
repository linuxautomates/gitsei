package io.levelops.integrations.helix_swarm.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = HelixSwarmGroup.HelixSwarmGroupBuilder.class)
public class HelixSwarmGroup {

    @JsonProperty("Group")
    String group;

    @JsonProperty("maxLockTime")
    Long maxLockTime;

    @JsonProperty("MaxResults")
    Long maxResults;

    @JsonProperty("MaxScanRows")
    Long maxScanRows;

    @JsonProperty("Owners")
    List<String> owners;

    @JsonProperty("PasswordTimeout")
    Long passwordTimeout;

    @JsonProperty("Subgroups")
    List<String> subgroups;

    @JsonProperty("Timeout")
    Long timeout;

    @JsonProperty("Users")
    List<String> users;

    @JsonProperty("config")
    Config config;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Config.ConfigBuilder.class)
    public static class Config {

        @JsonProperty("description")
        String description;

        @JsonProperty("emailAddress")
        String emailAddress;

        @JsonProperty("emailFlags")
        List<String> emailFlags;

        @JsonProperty("name")
        String name;

        @JsonProperty("useMailingList")
        Boolean useMailingList;

    }
}
