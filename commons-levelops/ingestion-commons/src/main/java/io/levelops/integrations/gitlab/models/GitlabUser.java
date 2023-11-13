package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabUser.GitlabUserBuilder.class)
public class GitlabUser {
    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("username")
    String username;
    @JsonProperty("state")
    String state;
    @JsonProperty("avatar_url")
    String avatarUrl;
    @JsonProperty("web_url")
    String webUrl;
    @JsonProperty("email")
    String email;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("is_admin")
    boolean isAdmin;
    @JsonProperty("bio")
    String bio;
    @JsonProperty("bio_html")
    String bioHtml;
    @JsonProperty("location")
    String location;
    @JsonProperty("skype")
    String skype;
    @JsonProperty("linkedin")
    String linkedin;
    @JsonProperty("twitter")
    String twitter;
    @JsonProperty("website_url")
    String websiteUrl;
    @JsonProperty("organization")
    String organization;
    @JsonProperty("job_title")
    String jobTitle;
    @JsonProperty("last_sign_in_at")
    Date lastSignInAt;
    @JsonProperty("confirmed_at")
    Date confirmedAt;
    @JsonProperty("theme_id")
    int themeId;
    @JsonProperty("last_activity_on")
    Date lastActivityOn;
    @JsonProperty("color_scheme_id")
    int colorSchemeId;
    @JsonProperty("projects_limit")
    int projectsLimit;
    @JsonProperty("current_sign_in_at")
    Date currentSignInAt;
    @JsonProperty("note")
    String note;
    @JsonProperty("identities")
    List<Identity> identities;
    @JsonProperty("can_create_group")
    boolean canCreateGroup;
    @JsonProperty("can_create_project")
    boolean canCreateProject;
    @JsonProperty("two_factor_enabled")
    boolean twoFactorEnabled;
    @JsonProperty("external")
    boolean external;
    @JsonProperty("private_profile")
    boolean privateProfile;
    @JsonProperty("current_sign_in_ip")
    String currentSignInIP;
    @JsonProperty("last_sign_in_ip")
    String lastSignInIP;
    @JsonProperty("public_email")
    String publicEmail;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Identity.IdentityBuilder.class)
    public static class Identity {
        @JsonProperty("provider")
        String provider;
        @JsonProperty("extern_uid")
        String externUid;
        @JsonProperty("saml_provider_id")
        int samlProviderId;
    }

    public GitlabEvent.GitlabEventAuthor toGitlabEventAuthor() {
        return GitlabEvent.GitlabEventAuthor.builder()
                .id(id)
                .authorName(name)
                .username(username)
                .state(state)
                .build();
    }
}
