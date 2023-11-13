package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.Signature;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.Fields;
import io.levelops.integrations.azureDevops.models.IdentityRef;
import io.levelops.integrations.azureDevops.models.PullRequest;
import io.levelops.integrations.azureDevops.models.WorkItem;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequestActivity;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketPullRequest;
import io.levelops.integrations.bitbucket.models.BitbucketUser;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPRActivity;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerPullRequest;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerUser;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubCommitUser;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubReview;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabEvent;
import io.levelops.integrations.gitlab.models.GitlabIssue;
import io.levelops.integrations.gitlab.models.GitlabMergeRequest;
import io.levelops.integrations.gitlab.models.GitlabUser;
import io.levelops.integrations.helix_swarm.models.HelixSwarmActivity;
import io.levelops.integrations.helix_swarm.models.HelixSwarmChange;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DbScmUser {

    public static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("id")
    private String id;

    @JsonProperty("integration_id")
    private String integrationId;

    @JsonProperty("display_name")
    private String displayName;

    @JsonProperty("original_display_name")
    private String originalDisplayName;

    @JsonProperty("cloudId")
    private String cloudId;

    @JsonProperty("email")
    private List<String> emails;

    @JsonProperty("mapping_status")
    private MappingStatus mappingStatus;

    @JsonProperty("created_at")
    private Long createdAt;

    @JsonProperty("updated_at")
    private Long updatedAt;


    public enum MappingStatus {
        AUTO, // eligible for auto mapping
        MANUAL; // manually overridden

        @JsonValue
        public String toString() {
            return super.toString().toLowerCase();
        }

        @JsonCreator
        public static MappingStatus fromString(@Nullable String value) {
            return EnumUtils.getEnumIgnoreCase(MappingStatus.class, value);
        }
    }


    // For github we get the display name and emails only through the new user ingestion
    // flow. These fields are not available when fetching PRs, commits, etc.
    public static DbScmUser fromGithubUser(GithubUser githubUser, String integrationId) {
        String login = Optional.ofNullable(githubUser.getLogin()).orElse(UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(login)
                .displayName(Optional.ofNullable(githubUser.getName()).orElse(login))
                .originalDisplayName(Optional.ofNullable(githubUser.getName()).orElse(login))
                .emails(githubUser.getOrgVerifiedDomainEmails())
                .build();
    }

    public static DbScmUser fromGithubPullRequestCreator(GithubPullRequest source,
                                                         String integrationId) {
        return fromGithubUser(source.getUser(), integrationId);
    }

    public static DbScmUser fromGithubPullRequestReviewer(GithubReview source,
                                                          String integrationId) {
        return fromGithubUser(source.getUser(), integrationId);
    }

    public static DbScmUser fromGithubCommitAuthor(GithubCommit source,
                                                   String integrationId) {
        String author = null;
        if (source.getAuthor() != null)
            author = Optional.of(source.getAuthor()).map(GithubUser::getLogin).orElse(UNKNOWN);
        else if (source.getGitAuthor() != null)
            author = Optional.of(source.getGitAuthor()).map(GithubCommitUser::getEmail).orElse(UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(author)
                .displayName(author)
                .originalDisplayName(author)
                .emails(null)
                .build();
    }

    public static DbScmUser fromGithubCommitCommitter(GithubCommit source,
                                                      String integrationId) {
        String committer = null;
        if (source.getCommitter() != null)
            committer = Optional.of(source.getCommitter()).map(GithubUser::getLogin).orElse(UNKNOWN);
        else if (source.getGitCommitter() != null)
            committer = Optional.of(source.getGitCommitter()).map(GithubCommitUser::getEmail).orElse(UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(committer)
                .displayName(committer)
                .originalDisplayName(committer)
                .emails(null)
                .build();
    }

    public static DbScmUser fromGithubIssueCreator(GithubIssue source,
                                                   String integrationId) {
        String cloudId = Optional.ofNullable(source.getUser()).map(GithubUser::getLogin).orElse(UNKNOWN);
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(cloudId)
                .displayName(cloudId)
                .originalDisplayName(cloudId)
                .emails(null)
                .build();
    }

    public static DbScmUser fromADWorkItemsAssignee(WorkItem source,
                                                    String integrationId) {
        Fields.AuthorizationDetail assignedTo = source.getFields().getAssignedTo();
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(assignedTo != null ? MoreObjects.firstNonNull(assignedTo.getUniqueName(), assignedTo.getDisplayName()) : UNKNOWN)
                .displayName(assignedTo != null ? assignedTo.getDisplayName() : UNKNOWN)
                .originalDisplayName(assignedTo != null ? assignedTo.getDisplayName() : UNKNOWN)
                .build();
    }

    public static DbScmUser fromADWorkItemsReporter(WorkItem source,
                                                    String integrationId) {
        Fields.AuthorizationDetail reportedBy = source.getFields().getCreatedBy();
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(reportedBy != null ? MoreObjects.firstNonNull(reportedBy.getUniqueName(), reportedBy.getDisplayName()) : UNKNOWN)
                .displayName(reportedBy != null ? reportedBy.getDisplayName() : UNKNOWN)
                .originalDisplayName(reportedBy != null ? reportedBy.getDisplayName() : UNKNOWN)
                .build();
    }

    public static DbScmUser fromAzureDevopsCommitterInfo(Commit commit,
                                                         String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(commit.getCommitter() != null ? commit.getCommitter().getEmail() : null)
                .displayName(commit.getCommitter() != null ? commit.getCommitter().getName() : null)
                .originalDisplayName(commit.getCommitter() != null ? commit.getCommitter().getName() : null)
                .build();
    }

    public static DbScmUser fromAzureDevopsCheckedInByInfo(ChangeSet changeSet,
                                                           String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(changeSet.getCheckedInBy() != null ? changeSet.getCheckedInBy().getUniqueName() : null)
                .displayName(changeSet.getCheckedInBy() != null ? changeSet.getCheckedInBy().getDisplayName() : null)
                .originalDisplayName(changeSet.getCheckedInBy() != null ? changeSet.getCheckedInBy().getDisplayName() : null)
                .build();
    }

    public static DbScmUser fromAzureDevopsCommitAuthorInfo(Commit commit,
                                                            String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(commit.getAuthor() != null ? commit.getAuthor().getEmail() : null)
                .displayName(commit.getAuthor() != null ? commit.getAuthor().getName() : null)
                .originalDisplayName(commit.getAuthor() != null ? commit.getAuthor().getName() : null)
                .build();
    }

    public static DbScmUser fromAzureDevopsCheckedInAuthor(ChangeSet changeSet,
                                                           String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(changeSet.getAuthor() != null ? changeSet.getAuthor().getUniqueName() : null)
                .displayName(changeSet.getAuthor() != null ? changeSet.getAuthor().getDisplayName() : null)
                .originalDisplayName(changeSet.getAuthor() != null ? changeSet.getAuthor().getDisplayName() : null)
                .build();
    }

    public static DbScmUser fromAzureDevopsPullRequestCreator(PullRequest pullRequest,
                                                              String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(pullRequest.getCreatedBy().getUniqueName(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(pullRequest.getCreatedBy().getDisplayName(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(pullRequest.getCreatedBy().getDisplayName(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromAzureDevopsPullRequestReviewer(IdentityRef identityRef,
                                                               String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(identityRef.getUniqueName(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(identityRef.getDisplayName(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(identityRef.getDisplayName(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromHelixSwarmReviewCreator(HelixSwarmReview source,
                                                        String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromHelixSwarmReviewAuthor(HelixSwarmChange source,
                                                       String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(source.getUser(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(source.getUser(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromHelixSwarmReviewCommitter(HelixSwarmChange source,
                                                          String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(source.getUser(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(source.getUser(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(source.getUser(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromHelixCoreChangeListAuthor(HelixCoreChangeList source,
                                                          String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromHelixCoreChangeListCommitter(HelixCoreChangeList source,
                                                             String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .displayName(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .originalDisplayName(MoreObjects.firstNonNull(source.getAuthor(), UNKNOWN))
                .build();
    }

    public static DbScmUser fromBitbucketServerCommitAuthor(BitbucketServerCommit source,
                                                            String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(String.valueOf(source.getAuthor().getId())).orElse(UNKNOWN))
                .displayName(Optional.of(source.getAuthor()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .originalDisplayName(Optional.of(source.getAuthor()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromBitbucketServerCommitCommitter(BitbucketServerCommit source,
                                                               String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(String.valueOf(source.getCommitter().getId())).orElse(UNKNOWN))
                .displayName(Optional.of(source.getCommitter()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .originalDisplayName(Optional.of(source.getCommitter()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromBitbucketServerPullRequestCreator(BitbucketServerPullRequest source,
                                                                  String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(String.valueOf(source.getAuthor().getUser().getId())).orElse(UNKNOWN))
                .displayName(Optional.of(source.getAuthor().getUser()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .originalDisplayName(Optional.of(source.getAuthor().getUser()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromBitbucketServerPullRequestReviewer(BitbucketServerPRActivity source,
                                                                   String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(String.valueOf(source.getUser().getId())).orElse(UNKNOWN))
                .displayName(Optional.of(source.getUser()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .originalDisplayName(Optional.of(source.getUser()).map(BitbucketServerUser::getDisplayName).orElse(UNKNOWN))
                .build();
    }


    public static DbScmUser fromGitlabPullRequestCreator(GitlabMergeRequest source,
                                                         String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(source.getAuthor())
                        .map(GitlabUser::getUsername)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN))
                .displayName(Optional.ofNullable(source.getAuthor())
                        .map(GitlabUser::getName)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(source.getAuthor())
                        .map(GitlabUser::getName)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromGitlabPullRequestReviewer(GitlabEvent source,
                                                          String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(source.getAuthor())
                        .map(GitlabEvent.GitlabEventAuthor::getUsername)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN))
                .displayName(Optional.ofNullable(source.getAuthor())
                        .map(GitlabEvent.GitlabEventAuthor::getAuthorName)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(source.getAuthor())
                        .map(GitlabEvent.GitlabEventAuthor::getAuthorName)
                        .filter(StringUtils::isNotBlank)
                        .orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromGitlabCommitCommitter(GitlabCommit source,
                                                      String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(source.getCommitterDetails()).map(GitlabUser::getUsername).orElse(UNKNOWN))
                .displayName(Optional.ofNullable(source.getCommitterDetails()).map(GitlabUser::getName).orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(source.getCommitterDetails()).map(GitlabUser::getName).orElse(UNKNOWN))
                .build();

    }

    public static DbScmUser fromGitlabCommitAuthor(GitlabCommit source,
                                                   String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(source.getAuthorDetails()).map(GitlabUser::getUsername).orElse(UNKNOWN))
                .displayName(Optional.ofNullable(source.getAuthorDetails()).map(GitlabUser::getName).orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(source.getAuthorDetails()).map(GitlabUser::getName).orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromGitlabIssueCreator(GitlabIssue source,
                                                   String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(source.getAuthor()).map(GitlabUser::getUsername).orElse(UNKNOWN))
                .displayName(Optional.ofNullable(source.getAuthor()).map(GitlabUser::getName).orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(source.getAuthor()).map(GitlabUser::getName).orElse(UNKNOWN))
                .build();
    }

    public static DbScmUser fromHelixSwarmReviewReviewer(HelixSwarmActivity source,
                                                         String integrationId) {
        return DbScmUser.builder()
                .integrationId(integrationId)
                .cloudId(Optional.ofNullable(source.getUser()).orElse(UNKNOWN))
                .displayName(Optional.ofNullable(source.getUserFullName()).orElse(UNKNOWN))
                .originalDisplayName(Optional.ofNullable(source.getUserFullName()).orElse(UNKNOWN))
                .build();
    }
}
