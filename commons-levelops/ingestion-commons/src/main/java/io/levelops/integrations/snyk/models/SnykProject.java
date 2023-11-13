package io.levelops.integrations.snyk.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = SnykProject.SnykProjectBuilder.class)
public class SnykProject {
    @JsonProperty("org_id")
    private final String orgId;
    @JsonProperty("name")
    private final String name;
    @JsonProperty("id")
    private final String id;
    @JsonProperty("created")
    private final Date created;
    @JsonProperty("origin")
    private final String origin;
    @JsonProperty("type")
    private final String type;
    @JsonProperty("readOnly")
    private final String readOnly;
    @JsonProperty("testFrequency")
    private final String testFrequency;
    @JsonProperty("totalDependencies")
    private final Integer totalDependencies;
    @JsonProperty("issueCountsBySeverity")
    private final IssueCountsBySeverity issueCountsBySeverity;
    @JsonProperty("imageId")
    private final String imageId;
    @JsonProperty("imageTag")
    private final String imageTag;
    @JsonProperty("remoteRepoUrl")
    private final String remoteRepoUrl;
    @JsonProperty("lastTestedDate")
    private final Date lastTestedDate;
    @JsonProperty("browseUrl")
    private final String browseUrl;
    @JsonProperty("importingUser")
    private final User importingUser;
    @JsonProperty("owner")
    private final User owner;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = IssueCountsBySeverity.IssueCountsBySeverityBuilder.class)
    public static final class IssueCountsBySeverity{
        @JsonProperty("low")
        private final Integer low;
        @JsonProperty("high")
        private final Integer high;
        @JsonProperty("medium")
        private final Integer medium;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = User.UserBuilder.class)
    public static final class User{
        @JsonProperty("id")
        private final String id;
        @JsonProperty("name")
        private final String name;
        @JsonProperty("username")
        private final String username;
        @JsonProperty("email")
        private final String email;
    }
}