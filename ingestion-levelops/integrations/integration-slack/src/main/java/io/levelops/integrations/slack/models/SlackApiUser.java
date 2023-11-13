package io.levelops.integrations.slack.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SlackApiUser.SlackApiUserBuilder.class)
public class SlackApiUser {

    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("team_id")
    String teamId;
    @JsonProperty("deleted")
    Boolean deleted;

    @JsonProperty("color")
    private String color;
    @JsonProperty("real_name")
    private String realName;
    @JsonProperty("tz")
    private String tz;
    @JsonProperty("tz_label")
    private String tzLabel;
    @JsonProperty("profile")
    private Profile profile;
    @JsonProperty("is_admin")
    private Boolean isAdmin;
    @JsonProperty("is_owner")
    private Boolean isOwner;
    @JsonProperty("is_primary_owner")
    private Boolean isPrimaryOwner;
    @JsonProperty("is_restricted")
    private Boolean isRestricted;
    @JsonProperty("is_ultra_restricted")
    private Boolean isUltraRestricted;
    @JsonProperty("is_bot")
    private Boolean isBot;
    @JsonProperty("is_app_user")
    private Boolean isAppUser;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Profile.ProfileBuilder.class)
    public static class Profile {
        @JsonProperty("title")
        private String title;
        @JsonProperty("phone")
        private String phone;
        @JsonProperty("real_name")
        private String realName;
        @JsonProperty("real_name_normalized")
        private String realNameNormalized;
        @JsonProperty("display_name")
        private String displayName;
        @JsonProperty("display_name_normalized")
        private String displayNameNormalized;
        @JsonProperty("email")
        private String email;
        @JsonProperty("team")
        private String team;
    }

}
