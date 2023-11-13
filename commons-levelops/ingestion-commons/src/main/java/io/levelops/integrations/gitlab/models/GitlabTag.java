package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabTag.GitlabTagBuilder.class)
public class GitlabTag {

    @JsonProperty("name")
    String name;

    @JsonProperty("message")
    String message;

    @JsonProperty("target")
    String target;

    @JsonProperty("commit")
    GitlabCommitInfo commit;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabTag.GitlabCommitInfo.GitlabCommitInfoBuilder.class)
    public static class GitlabCommitInfo {

        @JsonProperty("id")
        String id;

        @JsonProperty("short_id")
        String shortId;

        @JsonProperty("created_at")
        Date createdAt;

        @JsonProperty("title")
        String title;

        @JsonProperty("web_url")
        String webUrl;
    }

    @JsonProperty("release")
    GitlabRelease release;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GitlabTag.GitlabRelease.GitlabReleaseBuilder.class)
    public static class GitlabRelease {

        @JsonProperty("tag_name")
        String tagName;

        @JsonProperty("description")
        String description;
    }

    @JsonProperty("protected")
    Boolean protectedTag;
}
