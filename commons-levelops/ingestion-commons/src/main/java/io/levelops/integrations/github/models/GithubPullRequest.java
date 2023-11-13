package io.levelops.integrations.github.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;

import java.util.Date;
import java.util.List;

/**
 * NB: Also used by API Client.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = GithubPullRequest.GithubPullRequestBuilder.class)
public class GithubPullRequest {

    @JsonProperty("id")
    String id;
    @JsonProperty("number")
    Integer number;
    @JsonProperty("state")
    String state;
    @JsonProperty("locked")
    Boolean locked;
    @JsonProperty("title")
    String title;
    @JsonProperty("user")
    GithubUser user;
    @JsonProperty("body")
    String body;
    @JsonProperty("created_at")
    Date createdAt;
    @JsonProperty("updated_at")
    Date updatedAt;
    @JsonProperty("closed_at")
    Date closedAt;
    @JsonProperty("merged_at")
    Date mergedAt;
    @JsonProperty("merge_commit_sha")
    String mergeCommitSha;
    @JsonProperty("assignee")
    GithubUser assignee;
    @JsonProperty("assignees")
    List<GithubUser> assignees;
    @JsonProperty("requested_reviewers")
    List<GithubUser> requestedReviewers;
    @JsonProperty("requested_teams")
    List<Team> requestedTeams;
    @JsonProperty("labels")
    List<Label> labels;
    @JsonProperty("milestone")
    Milestone milestone;
    @JsonProperty("head")
    Ref head;
    @JsonProperty("base")
    Ref base;
    @JsonProperty("author_association")
    String authorAssociation;
    @JsonProperty("reviews")
    List<GithubReview> reviews; // enrichment
    @JsonProperty("pr_commits")
    List<GithubCommit> commits; // enrichment
    @JsonProperty("merge_commit")
    GithubCommit mergeCommit; // enrichment

    // LEV-3954: Need to update the commit_shas column after changes made for LEV-3564
    // For LEV-3564, enriched commits was renamed from commits to pr_commits
    // as commits is an integer field if we are fetching a single PR,
    // Hence, deserialization was the problem and due to which commit_shas in prs table are not showing up in the case of older data.
    // ToDo: Need to remove after some time...
    @JsonProperty("commits")
    Object olderCommits;
    // endregion

    @JsonProperty("patches")
    List<String> patches; // enrichment

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Label.LabelBuilder.class)
    public static class Label {
        @JsonProperty("id")
        String id;
        @JsonProperty("name")
        String name;
        @JsonProperty("description")
        String description;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Ref.RefBuilder.class)
    public static class Ref {
        @JsonProperty("label")
        String label;
        @JsonProperty("ref")
        String ref;
        @JsonProperty("sha")
        String sha;
        @JsonProperty("user")
        GithubUser user;
        @JsonProperty("repo")
        RepoId repo;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = RepoId.RepoIdBuilder.class)
    public static class RepoId {
        @JsonProperty("id")
        String id;
        @JsonProperty("name")
        String name;
        @JsonProperty("full_name")
        String fullName;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Team.TeamBuilder.class)
    public static class Team {
        @JsonProperty("id")
        String id;
        @JsonProperty("name")
        String name;
        @JsonProperty("parent")
        Team parent;
        @JsonProperty("permission")
        String permission;
        @JsonProperty("privacy")
        String privacy;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = Milestone.MilestoneBuilder.class)
    public static class Milestone {
        @JsonProperty("id")
        String id;
        @JsonProperty("number")
        Integer number;
        @JsonProperty("state")
        String state;
        @JsonProperty("title")
        String title;
        @JsonProperty("description")
        String description;
        @JsonProperty("creator")
        GithubUser creator;
        @JsonProperty("open_issues")
        Integer openIssues;
        @JsonProperty("closed_issues")
        Integer closedIssues;
        @JsonProperty("created_at")
        Date createdAt;
        @JsonProperty("updated_at")
        Date updatedAt;
        @JsonProperty("closed_at")
        Date closedAt;
        @JsonProperty("due_on")
        Date due_on;
    }

}
