package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabGroup.GitlabGroupBuilder.class)
public class GitlabGroup {
    @JsonProperty("id")
    String id;
    @JsonProperty("name")
    String name;
    @JsonProperty("web_url")
    String webUrl;
    @JsonProperty("description")
    String description;
    @JsonProperty("path")
    String path;
    @JsonProperty("visibility")
    String visibility;
    @JsonProperty("share_with_group_lock")
    boolean shareWithGroupLock;
    @JsonProperty("require_two_factor_authentication")
    boolean requireTwoFactorAuthentication;
    @JsonProperty("two_factor_grace_period")
    int twoFactorGracePeriod;
    @JsonProperty("project_creation_level")
    String projectCreationLevel;
    @JsonProperty("auto_devops_enabled")
    String autoDevopsEnabled;
    @JsonProperty("subgroup_creation_level")
    String subgroupCreationLevel;
    @JsonProperty("emails_disabled")
    String emailsDisabled;
    @JsonProperty("mentions_disabled")
    String mentionsDisabled;
    @JsonProperty("lfs_enabled")
    boolean lfsEnabled;
    @JsonProperty("default_branch_protection")
    int defaultBranchProtection;
    @JsonProperty("avatar_url")
    String avatarUrl;
    @JsonProperty("request_access_enabled")
    boolean requestAccessEnabled;
    @JsonProperty("full_name")
    String fullName;
    @JsonProperty("full_path")
    String fullPath;
    @JsonProperty("file_template_project_id")
    int fileTemplateProjectId;
    @JsonProperty("parent_id")
    String parent_id;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("statistics")
    Statistics statistics;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Statistics.StatisticsBuilder.class)
    public static class Statistics {
        @JsonProperty("storage_size")
        int storageSize;
        @JsonProperty("repository_size")
        int repositorySize;
        @JsonProperty("wiki_size")
        int wiki_size;
        @JsonProperty("lfs_objects_size")
        int lfsObjectsSize;
        @JsonProperty("job_artifacts_size")
        int jobArtifactsSize;
        @JsonProperty("packages_size")
        int packagesSize;
        @JsonProperty("snippets_size")
        int snippetsSize;
    }
}
