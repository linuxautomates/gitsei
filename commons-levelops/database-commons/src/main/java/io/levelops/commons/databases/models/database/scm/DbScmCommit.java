package io.levelops.commons.databases.models.database.scm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.utils.ScmIssueCorrelationUtil;
import io.levelops.integrations.azureDevops.models.ChangeSet;
import io.levelops.integrations.azureDevops.models.ChangeSetWorkitem;
import io.levelops.integrations.azureDevops.models.Commit;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.bitbucket.models.BitbucketCommit;
import io.levelops.integrations.bitbucket.models.BitbucketCommitDiffStat;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerCommit;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubCommitFile;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabCommitStat;
import io.levelops.integrations.helix_swarm.models.HelixSwarmChange;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.levelops.commons.dates.DateUtils.parseDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DbScmCommit {
    public static final String UNKNOWN = "_UNKNOWN_";
    public static final int COMMIT_MESSAGE_MAX_LENGTH = 25;

    @JsonProperty("id")
    @JsonAlias({"c_id"})
    private String id;

    @JsonProperty("integration_id")
    @JsonAlias({"c_integration_id"})
    private String integrationId;

    @JsonProperty("repo_id")
    @JsonAlias({"c_repo_id"})
    private List<String> repoIds; //repository id -- something like: "levelops/api-levelops"

    @JsonProperty("vcs_type")
    @JsonAlias({"c_vcs_type"})
    private VCS_TYPE vcsType;

    @JsonProperty("project")
    @JsonAlias({"c_project"})
    private String project;

    @JsonProperty("commit_sha")
    @JsonAlias({"c_commit_sha"})
    private String commitSha;

    @JsonProperty("committer")
    @JsonAlias({"c_committer"})
    private String committer; //name from git committer field

    @JsonProperty("author")
    @JsonAlias({"c_author"})
    private String author; //name from git author field

    @JsonProperty("commit_url")
    @JsonAlias({"c_commit_url"})
    private String commitUrl;

    @JsonProperty("message")
    @JsonAlias({"c_message"})
    private String message; //commit msg

    @JsonProperty("files_ct")
    @JsonAlias({"c_files_ct"})
    private Integer filesCt;

    @JsonProperty("additions")
    @JsonAlias({"c_additions"})
    private Integer additions;

    @JsonProperty("deletions")
    @JsonAlias({"c_deletions"})
    private Integer deletions;

    @JsonProperty("changes")
    @JsonAlias({"c_changes"})
    private Integer changes;

    @JsonProperty("committed_at")
    @JsonAlias({"c_committed_at"})
    private Long committedAt;

    @JsonProperty("ingested_at")
    @JsonAlias({"c_ingested_at"})
    private Long ingestedAt;

    @JsonProperty("commit_pushed_at")
    @JsonAlias({"c_commit_pushed_at"})
    private Long commitPushedAt;

    @JsonProperty("created_at")
    @JsonAlias({"c_created_at"})
    private Long createdAt;

    @JsonProperty("technology")
    @JsonAlias({"c_technology"})
    private String technology;

    @JsonProperty("author_info")
    @JsonAlias({"c_author_info"})
    private DbScmUser authorInfo; //contains DbScmUser object for author

    @JsonProperty("committer_info")
    @JsonAlias({"c_committer_info"})
    private DbScmUser committerInfo; //contains DbScmUser object for committer

    @JsonProperty("author_id")
    @JsonAlias({"c_author_id"})
    private String authorId; //refers to unique row in scm_users

    @JsonProperty("committer_id")
    @JsonAlias({"c_committer_id"})
    private String committerId; //refers to unique row in scm_users

    @JsonProperty("file_types")
    @JsonAlias({"c_file_types"})
    private List<String> fileTypes;

    //only used during insertion
    @JsonProperty("issue_keys")
    @JsonAlias({"c_issue_keys"})
    private List<String> issueKeys;

    @JsonProperty("workitem_ids")
    @JsonAlias({"c_workitem_ids"})
    private List<String> workitemIds;

    @JsonProperty("tot_lines_changed")
    @JsonAlias({"c_tot_lines_changed"})
    Long totalLinesChanged;

    @JsonProperty("tot_lines_removed")
    @JsonAlias({"c_tot_lines_removed"})
    Long totalLinesRemoved;

    @JsonProperty("tot_lines_added")
    @JsonAlias({"c_tot_lines_added"})
    Long totalLinesAdded;

    @JsonProperty("pct_legacy_refactored_lines")
    @JsonAlias({"c_pct_legacy_refactored_lines"})
    private Double pctLegacyLines;

    @JsonProperty("pct_refactored_lines")
    @JsonAlias({"c_pct_refactored_lines"})
    private Double pctRefactoredLines;

    @JsonProperty("pct_new_lines")
    @JsonAlias({"c_pct_new_lines"})
    private Double pctNewLines;

    @JsonProperty("total_legacy_lines")
    @JsonAlias({"c_total_legacy_lines"})
    Long legacyLinesCount;

    @JsonProperty("total_refactored_lines")
    @JsonAlias({"c_total_refactored_lines"})
    Long linesRefactoredCount;

    @JsonProperty("has_issue_keys")
    @JsonAlias({"c_has_issue_keys"})
    private Boolean hasIssueKeys;

    @JsonProperty("branch")
    @JsonAlias({"c_branch"})
    private String branch;

    @JsonProperty("files")
    @JsonAlias({"c_files"})
    private List<DbScmFileCommitDetails> fileCommitList;

    @JsonProperty("scm_tags")
    @JsonAlias({"c_scm_tags"})
    private List<DbScmTag> tags;

    @JsonProperty("technologies")
    @JsonAlias({"c_technologies"})
    private List<GitTechnology> technologies;

    @JsonProperty("day")
    @JsonAlias({"c_day"})
    String dayOfWeek;

    @JsonProperty("direct_merge")
    @JsonAlias({"c_direct_merge"})
    private Boolean directMerge;

    @JsonProperty("pr_list")
    @JsonAlias({"c_pr_list"})
    private List<String> prList;

    @JsonProperty("pr_count")
    @JsonAlias({"c_pr_count"})
    private Integer prCount;

    @JsonProperty("committer_pr_list")
    @JsonAlias({"c_committer_pr_list"})
    private List<String> committerPrList;

    @JsonProperty("committer_pr_count")
    @JsonAlias({"c_committer_pr_count"})
    private Integer committerPrCount;

    @JsonProperty("loc")
    @JsonAlias({"c_loc"})
    private Integer loc;


    public static DbScmCommit fromGithubCommit(GithubCommit source,
                                               String repoId,
                                               String integrationId,
                                               Long committedAt,
                                               Long eventTime) {
        String committer = UNKNOWN;
        String author = UNKNOWN;
        if (source.getCommitter() != null) {
            committer = MoreObjects.firstNonNull(source.getCommitter().getLogin(), committer);
        } else if (source.getGitCommitter() != null) {
            committer = MoreObjects.firstNonNull(source.getGitCommitter().getEmail(), committer);
        }

        if (source.getAuthor() != null) {
            author = MoreObjects.firstNonNull(source.getAuthor().getLogin(), author);
        } else if (source.getGitAuthor() != null) {
            author = MoreObjects.firstNonNull(source.getGitAuthor().getEmail(), author);
        }
        Integer changes;
        changes = source.getFiles().orElse(List.of()).stream().map(GithubCommitFile::getChanges).reduce(0, Integer::sum);
        List<String> workitemIds = ScmIssueCorrelationUtil.extractWorkitems(source.getMessage());
        List<String> jiraIssueKeys = ScmIssueCorrelationUtil.extractJiraKeys(source.getMessage());
        return DbScmCommit.builder()
                .integrationId(integrationId)
                .commitSha(source.getSha())
                .vcsType(VCS_TYPE.GIT)
                .issueKeys(jiraIssueKeys)
                .workitemIds(workitemIds)
                .repoIds(List.of(repoId))
                .project(MoreObjects.firstNonNull(repoId, ""))
                .committer(committer)
                .committerInfo(DbScmUser.fromGithubCommitCommitter(source, integrationId))
                .author(author)
                .authorInfo(DbScmUser.fromGithubCommitAuthor(source, integrationId))
                .commitUrl(source.getUrl())
                .message(StringUtils.truncate(source.getMessage(), COMMIT_MESSAGE_MAX_LENGTH))
                .filesCt(source.getFiles().isPresent() ? CollectionUtils.size(source.getFiles().get()) : 0)
                .additions(source.getStats() != null ? source.getStats().getAdditions() : 0)
                .deletions(source.getStats() != null ? source.getStats().getDeletions() : 0)
                .changes(changes)
                .branch(source.getBranch())
                .directMerge(false)
                .commitPushedAt(eventTime)
                .committedAt(committedAt)
                .ingestedAt(DateUtils.truncate(new Date(TimeUnit.MILLISECONDS.toMicros(eventTime)), Calendar.DATE).toInstant().getEpochSecond())
                .build();
    }

    public static DbScmCommit fromAzureDevopsCommit(Commit commit,
                                                    Project project,
                                                    String repoId,
                                                    String integrationId,
                                                    Long eventTime) {
        List<String> workitemIds = ScmIssueCorrelationUtil.extractWorkitems(commit.getComment());
        List<String> jiraIssueKeys = ScmIssueCorrelationUtil.extractJiraKeys(commit.getComment());
        return DbScmCommit.builder()
                .repoIds(List.of(repoId))
                .integrationId(integrationId)
                .vcsType(VCS_TYPE.GIT) // find out Git or Tfvc
                .commitSha(commit.getCommitId())
                .filesCt(commit.getChanges().size())
                .author(commit.getAuthor() != null ? ObjectUtils.firstNonNull(commit.getAuthor().getName(), commit.getAuthor().getEmail(), UNKNOWN) : UNKNOWN)
                .project(project.getName())
                .committer(commit.getCommitter() != null ? ObjectUtils.firstNonNull(commit.getCommitter().getName(), commit.getCommitter().getEmail(), UNKNOWN) : UNKNOWN)
                .committerInfo(DbScmUser.fromAzureDevopsCommitterInfo(commit, integrationId))
                .authorInfo(DbScmUser.fromAzureDevopsCommitAuthorInfo(commit, integrationId))
                .commitUrl(commit.getRemoteUrl())
                .message(StringUtils.truncate(commit.getComment(), COMMIT_MESSAGE_MAX_LENGTH))
                .additions(commit.getChangeCounts().getAdd())
                .deletions(commit.getChangeCounts().getDelete())
                .changes(commit.getChangeCounts().getEdit())
                .committedAt(commit.getCommitter() != null ?
                        parseDateTime(commit.getCommitter().getDate()).getEpochSecond() : null)
                .ingestedAt(DateUtils.truncate(new Date(TimeUnit.MILLISECONDS.toMillis(eventTime)),
                        Calendar.DATE).toInstant().getEpochSecond())
                .workitemIds(workitemIds)
                .issueKeys(jiraIssueKeys)
                .directMerge(false)
                .build();
    }

    public static DbScmCommit fromAzureDevopsChangeSets(ChangeSet changeSet,
                                                        Project project,
                                                        String integrationId,
                                                        Long eventTime) {
        List<String> workitemIds = new ArrayList<>();
        Set<String> jiraIssueKeys = new HashSet<>();
        List<ChangeSetWorkitem> changeSetWorkitemList = changeSet.getChangeSetWorkitems();
        for (ChangeSetWorkitem changeSetWorkitem : changeSetWorkitemList) {
            workitemIds.add(changeSetWorkitem.getId().toString());
            jiraIssueKeys.addAll(ScmIssueCorrelationUtil.extractJiraKeys(changeSetWorkitem.getTitle()));
        }
        Instant createdDate = parseDateTime(changeSet.getCreatedDate());
        return DbScmCommit.builder()
                .repoIds(List.of(project.getName()))
                .integrationId(integrationId)
                .workitemIds(workitemIds)
                .issueKeys(new ArrayList<>(jiraIssueKeys))
                .commitSha(String.valueOf(changeSet.getChangesetId()))
                .filesCt(CollectionUtils.size(changeSet.getChangeSetChanges()))
                .author(changeSet.getAuthor() != null ? changeSet.getAuthor().getDisplayName() : UNKNOWN)
                .project(project.getName())
                .committer(changeSet.getCheckedInBy() != null ? changeSet.getCheckedInBy().getDisplayName() : UNKNOWN)
                .committerInfo(DbScmUser.fromAzureDevopsCheckedInByInfo(changeSet, integrationId))
                .authorInfo(DbScmUser.fromAzureDevopsCheckedInAuthor(changeSet, integrationId))
                .commitUrl(changeSet.getUrl())
                .message(StringUtils.truncate(changeSet.getComment(), COMMIT_MESSAGE_MAX_LENGTH))
                .additions(0)
                .deletions(0)
                .changes(0)
                .vcsType(VCS_TYPE.TFVC)
                .committedAt(createdDate != null ? createdDate.getEpochSecond() : null)
                .ingestedAt(DateUtils.truncate(new Date(TimeUnit.MILLISECONDS.toMillis(eventTime)),
                        Calendar.DATE).toInstant().getEpochSecond())
                .directMerge(false)
                .build();
    }


    public static DbScmCommit fromBitbucketServerCommit(BitbucketServerCommit source, String repoId, String integrationId, Long truncatedDate) {
        String author = source.getAuthor() != null ? MoreObjects.firstNonNull(source.getAuthor().getDisplayName(), source.getAuthor().getName()) : UNKNOWN;
        String committer = source.getCommitter() != null ? MoreObjects.firstNonNull(source.getCommitter().getDisplayName(), source.getCommitter().getName()) : UNKNOWN;
        return DbScmCommit.builder()
                .integrationId(integrationId)
                .repoIds(List.of(repoId))
                .vcsType(VCS_TYPE.GIT)
                .workitemIds(ScmIssueCorrelationUtil.extractWorkitems(source.getMessage()))
                .issueKeys(ScmIssueCorrelationUtil.extractJiraKeys(source.getMessage()))
                .project(MoreObjects.firstNonNull(source.getProjectName(), UNKNOWN))
                .commitUrl(MoreObjects.firstNonNull(source.getCommitUrl(), UNKNOWN))
                .committer(committer)
                .committerInfo(DbScmUser.fromBitbucketServerCommitCommitter(source, integrationId))
                .author(author)
                .authorInfo(DbScmUser.fromBitbucketServerCommitAuthor(source, integrationId))
                .message(StringUtils.truncate(source.getMessage(), COMMIT_MESSAGE_MAX_LENGTH))
                .committedAt(TimeUnit.MILLISECONDS.toSeconds(source.getCommitterTimestamp()))
                .ingestedAt(truncatedDate)
                .commitSha(source.getId())
                .filesCt(CollectionUtils.emptyIfNull(source.getFiles()).size())
                .additions(MoreObjects.firstNonNull(source.getAdditions(), 0))
                .deletions(MoreObjects.firstNonNull(source.getDeletions(), 0))
                .changes(0) //LEV-3306 - We don't get lines changed from bitbucket server
                .directMerge(false)
                .build();
    }

    public static DbScmCommit fromGitlabCommit(GitlabCommit source, String projectId, String integrationId) {
        List<String> workitemIds = ScmIssueCorrelationUtil.extractWorkitems(source.getMessage());
        List<String> jiraIssueKeys = ScmIssueCorrelationUtil.extractJiraKeys(source.getMessage());
        return DbScmCommit.builder()
                .integrationId(integrationId)
                .vcsType(VCS_TYPE.GIT)
                .issueKeys(jiraIssueKeys)
                .workitemIds(workitemIds)
                .repoIds(List.of(projectId))
                .project(MoreObjects.firstNonNull(projectId, ""))
                .id(source.getId())
                .commitSha(source.getId())
                .committer(MoreObjects.firstNonNull(source.getCommitterName(), UNKNOWN))
                .committerInfo(DbScmUser.fromGitlabCommitCommitter(source, integrationId))
                .commitUrl(source.getWebUrl())
                .author(MoreObjects.firstNonNull(source.getAuthorName(), UNKNOWN))
                .authorInfo(DbScmUser.fromGitlabCommitAuthor(source, integrationId))
                .createdAt(source.getCreatedAt().toInstant().getEpochSecond())
                .message(StringUtils.truncate(source.getMessage(), COMMIT_MESSAGE_MAX_LENGTH))
                .committedAt(source.getCommittedDate().toInstant().getEpochSecond())
                .additions(Optional.ofNullable(source.getStats()).map(GitlabCommitStat::getAdditions).orElse(0))
                .deletions(Optional.ofNullable(source.getStats()).map(GitlabCommitStat::getDeletions).orElse(0))
                .changes(0) //LEV-3306 - We don't get lines changed from Gitlab
                .filesCt(CollectionUtils.size(source.getChanges()))
                .branch(source.getRefBranch())
                .directMerge(false)
                .ingestedAt(DateUtils.truncate(source.getCommittedDate(), Calendar.DATE).toInstant().getEpochSecond())
                .build();
    }

    public static DbScmCommit fromHelixSwarmVersion(HelixSwarmChange source, List<String> repoIds, String integrationId,
                                                    Date ingestedAt) {
        return DbScmCommit.builder()
                .integrationId(integrationId)
                .vcsType(VCS_TYPE.PERFORCE)
                .issueKeys(List.of())
                .repoIds(repoIds)
                .project(repoIds.stream().findFirst().orElse(""))
                .author(source.getUser())
                .authorInfo(DbScmUser.fromHelixSwarmReviewAuthor(source, integrationId))
                .committer(source.getUser())
                .committerInfo(DbScmUser.fromHelixSwarmReviewCommitter(source, integrationId))
                .commitSha(String.valueOf(source.getChange()))
                .filesCt(0)
                .additions(0)
                .deletions(0)
                .changes(0)
                .ingestedAt(DateUtils.truncate(ingestedAt, Calendar.DATE).toInstant().getEpochSecond())
                .committedAt(source.getTime())
                .directMerge(false)
                .build();

    }

    public static DbScmCommit fromHelixCoreChangeList(HelixCoreChangeList source,
                                                      Set<String> repoIds, String integrationId) {
        List<String> workitemIds = ScmIssueCorrelationUtil.extractWorkitems(source.getDescription());
        List<String> jiraIssueKeys = ScmIssueCorrelationUtil.extractJiraKeys(source.getDescription());
        return DbScmCommit.builder()
                .integrationId(integrationId)
                .vcsType(VCS_TYPE.PERFORCE)
                .issueKeys(jiraIssueKeys)
                .workitemIds(workitemIds)
                .repoIds(new ArrayList<>(repoIds))
                .project(repoIds.stream().findFirst().orElse(""))
                .commitSha(String.valueOf(source.getId()))
                .committer(source.getAuthor())
                .committerInfo(DbScmUser.fromHelixCoreChangeListCommitter(source, integrationId))
                .author(source.getAuthor())
                .authorInfo(DbScmUser.fromHelixCoreChangeListAuthor(source, integrationId))
                .message(source.getDescription())
                .committedAt(source.getLastUpdatedAt().toInstant().getEpochSecond())
                .filesCt(source.getFilesCount())
                .additions(Optional.ofNullable(source.getAdditions()).orElse(0))
                .deletions(Optional.ofNullable(source.getDeletions()).orElse(0))
                .changes(Optional.ofNullable(source.getChanges()).orElse(0))
                .ingestedAt(DateUtils.truncate(source.getLastUpdatedAt(), Calendar.DATE).toInstant().getEpochSecond())
                .directMerge(false)
                .build();
    }
}
