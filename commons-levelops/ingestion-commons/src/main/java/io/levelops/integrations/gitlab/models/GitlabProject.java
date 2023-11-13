package io.levelops.integrations.gitlab.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GitlabProject.GitlabProjectBuilder.class)
public class GitlabProject {
    @JsonProperty("id")
    String id;
    @JsonProperty("description")
    String description;
    @JsonProperty("name")
    String name;
    @JsonProperty("name_with_namespace")
    String nameWithNamespace;
    @JsonProperty("path")
    String path;
    @JsonProperty("path_with_namespace")
    String pathWithNamespace;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("default_branch")
    String defaultBranch;
    @JsonProperty("tag_list")
    List<String> tagList;
    @JsonProperty("ssh_url_to_repo")
    String sshUrlToRepo;
    @JsonProperty("http_url_to_repo")
    String httpUrlToRepo;
    @JsonProperty("web_url")
    String webUrl;
    @JsonProperty("readme_url")
    String readmeUrl;
    @JsonProperty("avatar_url")
    String avatarUrl;
    @JsonProperty("forks_count")
    long forksCount;
    @JsonProperty("star_count")
    long starCount;
    @JsonProperty("last_activity_at")
    Date lastActivityAt;
    @JsonProperty("namespace")
    Namespace namespace;
    @JsonProperty("_links")
    Links links;
    @JsonProperty("container_expiration_policy")
    ContainerExpirationPolicy policy;
    @JsonProperty("permissions")
    Permissions permissions;
    @JsonProperty("packages_enabled")
    boolean packagesEnabled;
    @JsonProperty("empty_repo")
    boolean emptyRepo;
    @JsonProperty("archived")
    boolean archived;
    @JsonProperty("visibility")
    String visibility;
    @JsonProperty("resolve_outdated_diff_discussions")
    boolean resolveOutdatedDiffDiscussions;
    @JsonProperty("container_registry_enabled")
    boolean containerRegistryEnabled;
    @JsonProperty("issues_enabled")
    boolean issuesEnabled;
    @JsonProperty("merge_requests_enabled")
    boolean mergeRequestsEnabled;
    @JsonProperty("wiki_enabled")
    boolean wikiEnabled;
    @JsonProperty("jobs_enabled")
    boolean jobsEnabled;
    @JsonProperty("builds_access_level")
    String buildsAccessLevel;
    @JsonProperty("snippets_enabled")
    boolean snippetsEnabled;
    @JsonProperty("service_desk_enabled")
    boolean serviceDeskEnabled;
    @JsonProperty("service_desk_address")
    String serviceDeskAddress;
    @JsonProperty("can_create_merge_request_in")
    boolean canCreateMergeRequestIn;
    @JsonProperty("issues_access_level")
    String issuesAccessLevel;
    @JsonProperty("repository_access_level")
    String repositoryAccessLevel;
    @JsonProperty("merge_requests_access_level")
    String mergeRequestsAccessLevel;
    @JsonProperty("forking_access_level")
    String forkingAccessLevel;

    @JsonProperty("commits")
    List<GitlabCommit> commits; //enriched

    @JsonProperty("merge_requests")
    List<GitlabMergeRequest> mergeRequests; // enriched

    @JsonProperty("branches")
    List<GitlabBranch> branches; // enriched

    @JsonProperty("users")
    List<GitlabUser> users; //enriched

    @JsonProperty("milestones")
    List<GitlabMilestone> milestones; //enriched

    @JsonProperty("pipelines")
    List<GitlabPipeline> pipelines; //enriched

    @JsonProperty("tags")
    List<GitlabTag> tags; //enriched

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Namespace.NamespaceBuilder.class)
    public static class Namespace {
        @JsonProperty("id")
        String id;
        @JsonProperty("name")
        String name;
        @JsonProperty("path")
        String path;
        @JsonProperty("kind")
        String kind;
        @JsonProperty("full_path")
        String fullPath;
        @JsonProperty("parent_id")
        String parentId;
        @JsonProperty("avatar_url")
        String avatarUrl;
        @JsonProperty("web_url")
        String webUrl;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Links.LinksBuilder.class)
    public static class Links {
        @JsonProperty("self")
        String self;
        @JsonProperty("issues")
        String issues;
        @JsonProperty("merge_requests")
        String merge_requests;
        @JsonProperty("repo_branches")
        String repo_branches;
        @JsonProperty("labels")
        String labels;
        @JsonProperty("events")
        String events;
        @JsonProperty("members")
        String members;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ContainerExpirationPolicy.ContainerExpirationPolicyBuilder.class)
    public static class ContainerExpirationPolicy {
        @JsonProperty("cadence")
        String cadence;
        @JsonProperty("enabled")
        boolean enabled;
        @JsonProperty("keep_n")
        int keepN;
        @JsonProperty("older_than")
        String olderThan;
        @JsonProperty("name_regex")
        String nameRegex;
        @JsonProperty("name_regex_keep")
        String nameRegexKeep;
        @JsonProperty("next_run_at")
        String nextRunAt;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Permissions.PermissionsBuilder.class)
    public static class Permissions {
        @JsonProperty("project_access")
        AccessLevel projectAccess;
        @JsonProperty("group_access")
        AccessLevel groupAccess;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = AccessLevel.AccessLevelBuilder.class)
        public static class AccessLevel {
            @JsonProperty("access_level")
            int accessLevel;
            @JsonProperty("notification_level")
            int notificationLevel;
        }
    }
}
