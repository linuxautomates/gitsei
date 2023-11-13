package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.integrations.azureDevops.models.Tag;
import io.levelops.integrations.bitbucket.models.BitbucketTag;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerTag;
import io.levelops.integrations.github.models.GithubTag;
import io.levelops.integrations.gitlab.models.GitlabTag;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = DbScmTag.DbScmTagBuilder.class)
public class DbScmTag {
    @JsonProperty("id")
    private final UUID id;

    @JsonProperty("integration_id")
    private final String integrationId;

    @JsonProperty("project")
    private final String project;

    @JsonProperty("repo")
    private final String repo;

    @JsonProperty("tag")
    private final String tag;

    @JsonProperty("commit_sha")
    private final String commitSha;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;

    public static DbScmTag fromGithubTag(GithubTag githubTag, String repoId, String integrationId) {
        String sha = null;
        if (githubTag.getCommit() != null) {
            sha = githubTag.getCommit().getSha();
        }
        long epochSecond = new Date().toInstant().getEpochSecond();
        return DbScmTag.builder()
                .repo(repoId)
                .project(MoreObjects.firstNonNull(repoId, ""))
                .integrationId(integrationId)
                .tag(githubTag.getName())
                .commitSha(sha)
                .updatedAt(epochSecond)
                .createdAt(epochSecond)
                .build();
    }

    public static DbScmTag fromGitLabTag(GitlabTag gitlabTag, String repoId, String integrationId) {
        String sha = null;
        if (gitlabTag.getCommit() != null) {
            sha = gitlabTag.getCommit().getId();
        }
        long epochSecond = new Date().toInstant().getEpochSecond();
        return DbScmTag.builder()
                .repo(repoId)
                .project(MoreObjects.firstNonNull(repoId, ""))
                .integrationId(integrationId)
                .tag(gitlabTag.getName())
                .commitSha(sha)
                .updatedAt(epochSecond)
                .createdAt(epochSecond)
                .build();
    }

    public static DbScmTag fromBitbucketServerTag(BitbucketServerTag bitbucketServerTag, String repoId, String integrationId) {
        long epochSecond = new Date().toInstant().getEpochSecond();
        return DbScmTag.builder()
                .repo(repoId)
                .project(MoreObjects.firstNonNull(repoId, ""))
                .integrationId(integrationId)
                .tag(bitbucketServerTag.getDisplayId())
                .commitSha(bitbucketServerTag.getLatestCommit())
                .updatedAt(epochSecond)
                .createdAt(epochSecond)
                .build();
    }

    public static DbScmTag fromBitbucketTag(BitbucketTag bitbucketTag, String repoId, String integrationId) {
        long epochSecond = new Date().toInstant().getEpochSecond();
        return DbScmTag.builder()
                .repo(repoId)
                .project(MoreObjects.firstNonNull(repoId, ""))
                .integrationId(integrationId)
                .tag(bitbucketTag.getName())
                .commitSha(bitbucketTag.getTarget().getHash())
                .updatedAt(epochSecond)
                .createdAt(epochSecond)
                .build();
    }

    public static DbScmTag fromAzureDevopsTag(Tag tag, String repoId, String integrationId) {
        long epochSecond = new Date().toInstant().getEpochSecond();
        return DbScmTag.builder()
                .repo(repoId)
                .project(MoreObjects.firstNonNull(repoId, ""))
                .integrationId(integrationId)
                .tag(tag.getName())
                .commitSha(tag.getTaggedObject().getObjectId())
                .updatedAt(epochSecond)
                .createdAt(epochSecond)
                .build();
    }
}