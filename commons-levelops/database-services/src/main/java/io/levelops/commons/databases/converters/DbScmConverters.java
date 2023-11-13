package io.levelops.commons.databases.converters;

import com.google.api.client.util.Lists;
import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmCommitStats;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommitDetails;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabelLite;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Map.entry;


@Log4j2
public class DbScmConverters {

    public static final Map<String, String> LANGUAGE_FILE_MAP = Map.ofEntries(
            entry("py", "PYTHON"),
            entry("py3", "PYTHON"),
            entry("java", "JAVA"),
            entry("c", "C/C++"),
            entry("cpp", "C/C++"),
            entry("go", "Golang"),
            entry("cs", "C#"),
            entry("sh", "Shell scripts"),
            entry("bash", "Shell scripts"),
            entry("bsh", "Shell scripts"),
            entry("csh", "Shell scripts"),
            entry("zsh", "Shell scripts"),
            entry("bat", "Batch files"),
            entry("kt", "Kotlin"),
            entry("js", "Javascript/Typescript"),
            entry("jsx", "Javascript/Typescript"),
            entry("ts", "Javascript/Typescript"),
            entry("tsx", "Javascript/Typescript"),
            entry("php", "PHP"),
            entry("pl", "Perl/Ruby"),
            entry("rb", "Perl/Ruby"),
            entry("r", "R"),
            entry("ps1", "Powershell"),
            entry("json", "JSON"),
            entry("html", "HTML/CSS"),
            entry("css", "HTML/CSS"),
            entry("less", "HTML/CSS"),
            entry("scss", "HTML/CSS"),
            entry("sass", "HTML/CSS"),
            entry("svg", "HTML/CSS"),
            entry("sql", "SQL"),
            entry("jar", "JAVA"));

    public static RowMapper<DbScmPullRequest> prRowMapper() {
        return (rs, rowNumber) -> DbScmPullRequest.builder()
                .id(rs.getString("id"))
                .repoIds((rs.getArray("repo_id") != null &&
                        rs.getArray("repo_id").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("repo_id").getArray()) : List.of())
                .project(rs.getString("project"))
                .integrationId(rs.getString("integration_id"))
                .state(rs.getString("state"))
                .number(rs.getString("number"))
                .creator(columnPresent(rs, "creator") ? rs.getString("creator") : null)
                .creatorId(columnPresent(rs, "creator_id") ? rs.getString("creator_id") : null)
                .mergeSha(rs.getString("merge_sha"))
                .sourceBranch(rs.getString("source_branch"))
                .targetBranch(rs.getString("target_branch"))
                .prLink(columnPresent(rs, "pr_link") ? rs.getString("pr_link") : null)
                .merged(rs.getBoolean("merged"))
                .title(rs.getString("title"))
                .linesAdded(columnPresent(rs, "lines_added") ? rs.getString("lines_added") : null)
                .linesDeleted(columnPresent(rs, "lines_deleted") ? rs.getString("lines_deleted") : null)
                .linesChanged(columnPresent(rs, "lines_changed") ? rs.getString("lines_changed") : null)
                .filesChanged(columnPresent(rs, "files_ct") ? rs.getString("files_ct") : null)
                .codeChange(columnPresent(rs, "code_change") ? rs.getString("code_change") : null)
                .commentDensity(columnPresent(rs, "comment_density") ? rs.getString("comment_density") : null)
                .hasIssueKeys(columnPresent(rs, "has_issue_keys") ? rs.getBoolean("has_issue_keys") : null)
                .reviewType(columnPresent(rs, "review_type") ? rs.getString("review_type") : null)
                .assignees((rs.getArray("assignees") != null &&
                        rs.getArray("assignees").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("assignees").getArray()) : List.of())
                .assigneeIds((rs.getArray("assignee_ids") != null &&
                        rs.getArray("assignee_ids").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("assignee_ids").getArray()) : List.of())
                .reviewers(columnPresent(rs, "reviewer") ? Arrays.asList((String[]) rs.getArray("reviewer").getArray()) : List.of())
                .reviewerIds(columnPresent(rs, "reviewer_id") ? Arrays.asList((String[]) rs.getArray("reviewer_id").getArray()) : List.of())
                .commenters(columnPresent(rs, "commenter") ? Arrays.asList((String[]) rs.getArray("commenter").getArray()) : List.of())
                .commenterIds(columnPresent(rs, "commenter_id") ? Arrays.asList((String[]) rs.getArray("commenter_id").getArray()) : List.of())
                .collabState(columnPresent(rs, "collab_state") ? rs.getString("collab_state") : null)
                .approvers(columnPresent(rs, "approver") ? Arrays.asList((String[]) rs.getArray("approver").getArray()) : List.of())
                .approverIds(columnPresent(rs, "approver_id") ? Arrays.asList((String[]) rs.getArray("approver_id").getArray()) : List.of())
                .labels((rs.getArray("labels") != null &&
                        rs.getArray("labels").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("labels").getArray()) : List.of())
                .commitShas((rs.getArray("commit_shas") != null &&
                        rs.getArray("commit_shas").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("commit_shas").getArray()) : List.of())
                .prUpdatedAt(rs.getTimestamp("pr_updated_at").toInstant().getEpochSecond())
                .prMergedAt(rs.getTimestamp("pr_merged_at") != null ?
                        rs.getTimestamp("pr_merged_at").toInstant().getEpochSecond() : null)
                .prCreatedAt(rs.getTimestamp("pr_created_at").toInstant().getEpochSecond())
                .prClosedAt(rs.getTimestamp("pr_closed_at") != null ?
                        rs.getTimestamp("pr_closed_at").toInstant().getEpochSecond() : null)
                .createdAt(rs.getLong("created_at"))
                .avgAuthorResponseTime(columnPresent(rs, "author_response_time") ? rs.getLong("author_response_time") : null)
                .avgReviewerResponseTime(columnPresent(rs, "reviewer_response_time") ? rs.getLong("reviewer_response_time") : null)
                .approvalTime(columnPresent(rs, "reviewer_approve_time") ? rs.getLong("reviewer_approve_time") : null)
                .commentTime(columnPresent(rs, "reviewer_comment_time") ? rs.getLong("reviewer_comment_time") : null)
                .approvalStatus(columnPresent(rs, "approval_status") ? rs.getString("approval_status") : null)
                .approverCount(columnPresent(rs, "approver_count") ? rs.getInt("approver_count") : null)
                .reviewerCount(columnPresent(rs, "reviewer_count") ? rs.getInt("reviewer_count") : null)
                .build();
    }

    public static RowMapper<DbScmReview> prReviewRowMapper() {
        return (rs, rowNumber) -> DbScmReview.builder()
                .id(rs.getString("id"))
                .reviewer(rs.getString("reviewer"))
                .reviewerId(rs.getString("reviewer_id"))
                .reviewId(rs.getString("review_id"))
                .state(rs.getString("state"))
                .prId(rs.getString("pr_id"))
                .reviewedAt(rs.getTimestamp("reviewed_at").toInstant().getEpochSecond())
                .build();
    }

    public static RowMapper<String> getIdRowMapper() {
        return (rs, rowNumber) -> rs.getString("id");
    }

    public static RowMapper<String> getWorkitemRowMapper() {
        return (rs, rowNumber) -> rs.getString("workitem_id");
    }

    public static RowMapper<DbScmCommit> commitRowMapper() {

        return (rs, rowNumber) -> {
            return DbScmCommit.builder()
                    .id(rs.getString("id"))
                    .repoIds((rs.getArray("repo_id") != null &&
                            rs.getArray("repo_id").getArray() != null) ?
                            Arrays.asList((String[]) rs.getArray("repo_id").getArray()) : List.of())
                    .project(rs.getString("project"))
                    .fileTypes(columnPresent(rs, "file_types") ? Arrays.asList((String[]) rs.getArray("file_types").getArray())
                            : List.of())
                    .integrationId(rs.getString("integration_id"))
                    .author(rs.getString("author"))
                    .authorId(rs.getString("author_id"))
                    .committer(rs.getString("committer"))
                    .committerId(rs.getString("committer_id"))
                    .commitSha(rs.getString("commit_sha"))
                    .commitUrl(rs.getString("commit_url"))
                    .message(rs.getString("message"))
                    .technology(columnPresent(rs, "technology") ? rs.getString("technology") : null)
                    .filesCt(rs.getInt("files_ct"))
                    .additions(rs.getInt("additions"))
                    .deletions(rs.getInt("deletions"))
                    .changes(rs.getInt("changes"))
                    .vcsType(VCS_TYPE.valueOf(rs.getString("vcs_type")))
                    .ingestedAt(rs.getLong("ingested_at"))
                    .createdAt(rs.getLong("created_at"))
                    .branch(columnPresent(rs, "commit_branch") ? rs.getString("commit_branch") : null)
                    .commitPushedAt(columnPresent(rs, "commit_pushed_at") ? rs.getTimestamp("commit_pushed_at").toInstant().getEpochSecond()
                            : null)
                    .committedAt(rs.getTimestamp("committed_at").toInstant().getEpochSecond())
                    .totalLinesAdded(columnPresent(rs, "tot_addition") ? rs.getLong("tot_addition") : 0)
                    .totalLinesChanged(columnPresent(rs, "tot_change") ? rs.getLong("tot_change") : 0)
                    .totalLinesRemoved(columnPresent(rs, "tot_deletion") ? rs.getLong("tot_deletion") : 0)
                    .pctLegacyLines(columnPresent(rs, "pct_legacy_refactored_lines") ? Double.parseDouble(String.format("%.2f", rs.getDouble("pct_legacy_refactored_lines"))) : 0)
                    .pctNewLines(columnPresent(rs, "pct_new_lines") ? Double.parseDouble(String.format("%.2f", rs.getDouble("pct_new_lines"))) : 0)
                    .pctRefactoredLines(columnPresent(rs, "pct_refactored_lines") ? Double.parseDouble(String.format("%.2f", rs.getDouble("pct_refactored_lines"))) : 0)
                    .pctNewLines(columnPresent(rs, "pct_new_lines") ? Double.parseDouble(String.format("%.2f", rs.getDouble("pct_new_lines"))) : 0)
                    .pctRefactoredLines(columnPresent(rs, "pct_refactored_lines") ? Double.parseDouble(String.format("%.2f", rs.getDouble("pct_refactored_lines"))) : 0)
                    .legacyLinesCount(columnPresent(rs, "total_legacy_code_lines") ? rs.getLong("total_legacy_code_lines") : 0)
                    .linesRefactoredCount(columnPresent(rs, "total_refactored_code_lines") ? rs.getLong("total_refactored_code_lines") : 0)
                    .hasIssueKeys(columnPresent(rs, "has_issue_keys") ? rs.getBoolean("has_issue_keys") : null)
                    .build();
        };
    }

    private static boolean columnPresent(ResultSet rs, String column) {
        boolean isColumnPresent = false;
        try {
            rs.findColumn(column);
            if (ObjectUtils.isNotEmpty(rs.getObject(column))) {
                isColumnPresent = true;
            }
        } catch (SQLException e) {
            isColumnPresent = false;
        }
        return isColumnPresent;
    }


    public static RowMapper<DbScmContributorAgg> committerAggRowMapper(ScmContributorsFilter filter, boolean includeIssues) {
        return (rs, rowNumber) -> {
            String creator = includeIssues ? rs.getString("creator") : StringUtils.EMPTY;
            String name = (ScmContributorsFilter.DISTINCT.author == filter.getAcross()) ?
                    StringUtils.firstNonBlank(rs.getString("author"), StringUtils.isEmpty(creator) ? "NA" : creator) :
                    StringUtils.firstNonBlank(rs.getString("committer"), StringUtils.isEmpty(creator) ? "NA" : creator);
            List<String> workitems = includeIssues ? getIssues(rs, "cworkitems", "prworkitems") : List.of();
            List<String> jiraIssues = includeIssues ? getIssues(rs, "cjissues", "prjissues") : List.of();
            Set<String> filesValues = getUniqueValues(rs, "files");
            return DbScmContributorAgg.builder()
                    .id(getId(rs))
                    .name(name)
                    .fileTypes(CollectionUtils.isEmpty(filesValues) ? List.of() : Lists.newArrayList(filesValues))
                    .techBreadth(getBreadthList(rs.getArray("files"), false))
                    .repoBreadth(getBreadthList(rs.getArray("repos"), true))
                    .numCommits(rs.getInt("num_commits"))
                    .numRepos(rs.getInt("num_repos"))
                    .numPrs(columnPresent(rs, "num_prs") ? rs.getInt("num_prs") : 0)
                    .numAdditions(rs.getInt("num_additions"))
                    .numDeletions(rs.getInt("num_deletions"))
                    .numChanges(rs.getInt("num_changes"))
                    .numJiraIssues(jiraIssues.size())
                    .numWorkitems(workitems.size())
                    .build();
        };
    }


    private static Set<String> getUniqueValues(ResultSet rs, String columnName) throws SQLException {
        Array files = rs.getArray(columnName);
        if (files == null) {
            return Set.of();
        }
        return new HashSet<>(Arrays.asList((String[]) files.getArray()));
    }

    private static String getId(ResultSet rs) throws SQLException {
        if (columnPresent(rs, "creator_id")) {
            return rs.getString("creator_id");
        }
        if (columnPresent(rs, "author_id")) {
            return rs.getString("author_id");
        }
        if (columnPresent(rs, "committer_id")) {
            return rs.getString("committer_id");
        }
        return null;
    }

    private static Map<String, Integer> getFileValues(Array files) throws SQLException {
        HashMap<String, Integer> result = new HashMap<>();
        if (files == null) {
            return result;
        }
        for (String file : (String[]) files.getArray())
            result.put(LANGUAGE_FILE_MAP.get(file), result.getOrDefault(LANGUAGE_FILE_MAP.get(file), 0) + 1);
        return result;
    }

    private static Map<String, Integer> getRepoValues(Array repos) throws SQLException {
        HashMap<String, Integer> result = new HashMap<>();
        if (repos == null) {
            return result;
        }
        for (String repo : (String[]) repos.getArray())
            result.put(repo, result.getOrDefault(repo, 0) + 1);
        return result;
    }

    private static List<String> getBreadthList(Array repos, Boolean isRepo) throws SQLException {
        Map<String, Integer> values = (isRepo) ? getRepoValues(repos) : getFileValues(repos);
        return values.keySet().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static RowMapper<DbScmRepoAgg> repoAggRowMapper() {
        return (rs, rowNumber) -> {
            List<String> workitems = getIssues(rs, "cworkitems", "prworkitems");
            List<String> jiraIssues = getIssues(rs, "cjissues", "prjissues");
            return DbScmRepoAgg.builder()
                    .id(StringUtils.firstNonEmpty(rs.getString("repo"),
                            rs.getString("cr")))
                    .name(StringUtils.firstNonEmpty(rs.getString("repo"),
                            rs.getString("cr")))
                    .numCommits(rs.getInt("num_commits"))
                    .numPrs(rs.getInt("num_prs"))
                    .numAdditions(rs.getInt("num_additions"))
                    .numDeletions(rs.getInt("num_deletions"))
                    .numChanges(rs.getInt("num_changes"))
                    .numJiraIssues(jiraIssues.size())
                    .numWorkitems(workitems.size())
                    .build();
        };
    }

    public static RowMapper<DbScmRepoAgg> fileTypesAggRowMapper() {
        return (rs, rowNumber) -> {
            List<String> workitems = getIssues(rs, "cworkitems", "prworkitems");
            List<String> jiraIssues = getIssues(rs, "cjissues", "prjissues");
            return DbScmRepoAgg.builder()
                    .name(rs.getString("file_type"))
                    .numCommits(rs.getInt("num_commits"))
                    .numPrs(rs.getInt("num_prs"))
                    .numAdditions(rs.getInt("num_additions"))
                    .numDeletions(rs.getInt("num_deletions"))
                    .numChanges(rs.getInt("num_changes"))
                    .numJiraIssues(jiraIssues.size())
                    .numWorkitems(workitems.size())
                    .build();
        };
    }

    @NotNull
    public static List<String> getIssues(ResultSet rs, String commitIssuesColumnLabel, String prIssuesColumnLabel) throws SQLException {
        Set<String> issues = new HashSet<>();
        List<String> commitIssues = (rs.getArray(commitIssuesColumnLabel) != null && rs.getArray(commitIssuesColumnLabel).getArray() != null) ?
                Arrays.asList((String[]) rs.getArray(commitIssuesColumnLabel).getArray()) : List.of();
        List<String> prIssues = (rs.getArray(prIssuesColumnLabel) != null && rs.getArray(prIssuesColumnLabel).getArray() != null) ?
                Arrays.asList((String[]) rs.getArray(prIssuesColumnLabel).getArray()) : List.of();
        issues.addAll(commitIssues);
        issues.addAll(prIssues);
        return new ArrayList<>(issues);
    }

    public static RowMapper<DbScmFile> filesRowMapper() {
        return (rs, rowNumber) -> DbScmFile.builder()
                .id(rs.getString("id"))
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .integrationId(rs.getString("integration_id"))
                .filename(rs.getString("filename"))
                .numCommits(rs.getLong("num_commits"))
                .totalAdditions(rs.getLong("additions"))
                .totalDeletions(rs.getLong("deletions"))
                .totalChanges(rs.getLong("changes"))
                .createdAt(rs.getLong("created_at"))
//                .commitShas((rs.getArray("commit_shas") != null &&
//                        rs.getArray("commit_shas").getArray() != null) ?
//                        Arrays.asList((String[]) rs.getArray("commit_shas").getArray()) : List.of())
                .build();
    }

    public static RowMapper<DbScmFile> commitFilesRowMapper() {
        return (rs, rowNumber) -> DbScmFile.builder()
                .id(rs.getString("id"))
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .integrationId(rs.getString("integration_id"))
                .filename(rs.getString("filename"))
                .createdAt(rs.getLong("created_at"))
                .build();
    }

    public static RowMapper<DbScmFileCommit> filesCommitRowMapper() {
        return (rs, rowNumber) -> DbScmFileCommit.builder()
                .id(rs.getString("id"))
                .commitSha(rs.getString("commit_sha"))
                .fileId(rs.getString("file_id"))
                .change(rs.getInt("change"))
                .addition(rs.getInt("addition"))
                .deletion(rs.getInt("deletion"))
                .committedAt(rs.getTimestamp("committed_at").toInstant().getEpochSecond())
                .previousCommittedAt(rs.getTimestamp("previous_committed_at") != null ? rs.getTimestamp("previous_committed_at").toInstant().getEpochSecond() : null)
                .createdAt(rs.getLong("created_at"))
                .build();
    }

    public static RowMapper<DbScmFileCommitDetails> filesCommitDetailsRowMapper() {
        return (rs, rowNumber) -> DbScmFileCommitDetails.builder()
                .id(rs.getString("id"))
                .commitSha(rs.getString("commit_sha"))
                .fileId(rs.getString("file_id"))
                .integrationId(rs.getString("integration_id"))
                .repo(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .fileName(rs.getString("filename"))
                .fileType(rs.getString("filetype"))
                .change(rs.getInt("change"))
                .addition(rs.getInt("addition"))
                .deletion(rs.getInt("deletion"))
                .committedAt(rs.getTimestamp("committed_at").toInstant().getEpochSecond())
                .previousCommittedAt(rs.getTimestamp("previous_committed_at") != null ? rs.getTimestamp("previous_committed_at").toInstant().getEpochSecond() : null)
                .createdAt(rs.getLong("created_at"))
                .fileCreatedAt(rs.getLong("file_created_at"))
                .build();
    }

    public static RowMapper<DbScmFile> filesJiraRowMapper() {
        return (rs, rowNumber) -> DbScmFile.builder()
                .id(rs.getString("id"))
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .filename(rs.getString("filename"))
                .numIssues(rs.getLong("num_issues"))
                .integrationId(rs.getString("integration_id"))
                .jiraIssueKeys((rs.getArray("keys") != null &&
                        rs.getArray("keys").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("keys").getArray()) : List.of())
                .build();
    }

    public static RowMapper<DbAggregationResult> modulesJiraRowMapper(String key) {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .key(rs.getString(key))
                .totalIssues(rs.getLong("num_issues"))
                .build();
    }

    public static RowMapper<DbScmFile> filesSalesforceRowMapper() {
        return (rs, rowNumber) -> DbScmFile.builder()
                .id(rs.getString("id"))
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .filename(rs.getString("filename"))
                .numCases(rs.getLong("num_cases"))
                .integrationId(rs.getString("integration_id"))
                .salesforceCaseNumbers((rs.getArray("cases") != null &&
                        rs.getArray("cases").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("cases").getArray()) : List.of())
                .numCommits(rs.getLong("num_commits"))
                .totalAdditions(rs.getLong("additions"))
                .totalChanges(rs.getLong("changes"))
                .totalDeletions(rs.getLong("deletions"))
                .build();
    }

    public static RowMapper<DbScmFile> filesZendeskRowMapper() {
        return (rs, rowNumber) -> DbScmFile.builder()
                .id(rs.getString("id"))
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .filename(rs.getString("filename"))
                .numTickets(rs.getLong("num_tickets"))
                .integrationId(rs.getString("integration_id"))
                .zendeskIntegrationId(rs.getString("zendesk_integration_id"))
                .zendeskTicketIds((rs.getArray("tickets") != null &&
                        rs.getArray("tickets").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("tickets").getArray()) : List.of())
                .numCommits(rs.getLong("num_commits"))
                .totalAdditions(rs.getLong("additions"))
                .totalChanges(rs.getLong("changes"))
                .totalDeletions(rs.getLong("deletions"))
                .build();
    }

    public static RowMapper<DbScmIssue> issueRowMapper() {
        return (rs, rowNumber) -> DbScmIssue.builder()
                .id(rs.getString("id"))
                .repoId(rs.getString("repo_id"))
                .project(rs.getString("project"))
                .integrationId(rs.getString("integration_id"))
                .issueId(rs.getString("issue_id"))
                .creator(rs.getString("creator"))
                .assignees((rs.getArray("assignees") != null &&
                        rs.getArray("assignees").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("assignees").getArray()) : List.of())
                .labels((rs.getArray("labels") != null &&
                        rs.getArray("labels").getArray() != null) ?
                        Arrays.asList((String[]) rs.getArray("labels").getArray()) : List.of())
                .number(rs.getString("number"))
                .numComments(rs.getInt("num_comments"))
                .state(rs.getString("state"))
                .title(rs.getString("title"))
                .url(rs.getString("url"))
                .issueCreatedAt(rs.getTimestamp("issue_created_at").toInstant().getEpochSecond())
                .issueUpdatedAt(rs.getTimestamp("issue_updated_at").toInstant().getEpochSecond())
                .issueClosedAt(rs.getTimestamp("issue_closed_at") != null ?
                        rs.getTimestamp("issue_closed_at").toInstant().getEpochSecond() : null)
                .firstCommentAt(rs.getTimestamp("first_comment_at") != null ?
                        rs.getTimestamp("first_comment_at").toInstant().getEpochSecond() : null)
                .createdAt(rs.getLong("created_at"))
                .creatorId(columnPresent(rs, "creator_id") ? rs.getString("creator_id") : null)
                .build();
    }

    public static RowMapper<DbAggregationResult> collaborationReportMapper(String key, String additionalKey) {

        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(StringUtils.isNotEmpty(key) ? rs.getString(key) : null)
                .additionalKey(StringUtils.isNotEmpty(additionalKey) ? rs.getString(additionalKey) : null)
                .count(rs.getLong("ct"))
                .collabState(columnPresent(rs, "collab_state") ? rs.getString("collab_state") : null)
                .build();
    }

    public static RowMapper<DbAggregationResult> doraMetricsRowMapper(String metric, ScmPrFilter.DISTINCT across) {
        switch (across) {
            case project:
                return getRowMapper(metric, across.name(), null);
            case repo_id:
                return getRowMapper(metric, "repo_ids", null);
            case creator:
                return getRowMapper(metric, "creator_id", across.name());
            default:
                return getRowMapper(metric, null, null);
        }
    }

    @NotNull
    private static RowMapper<DbAggregationResult> getRowMapper(String metric, String key, String additionalKey) {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(StringUtils.isNotEmpty(key) ? rs.getString(key) : null)
                .additionalKey(StringUtils.isNotEmpty(additionalKey) ? rs.getString(additionalKey) : null)
                .band(StringUtils.isNotEmpty(metric) ? rs.getString(metric) : null)
                .leadTime(columnPresent(rs, "lead_time") ? rs.getLong("lead_time") : null)
                .recoverTime(columnPresent(rs, "recover_time") ? rs.getLong("recover_time") : null)
                .deploymentFrequency(columnPresent(rs, "deployment_frequency") ? Double.valueOf(rs.getString("deployment_frequency")) : null)
                .failureRate(columnPresent(rs, "failure_rate") ? Double.valueOf(rs.getString("failure_rate")) : null)
                .count(rs.getLong("ct"))
                .build();
    }

    public static RowMapper<DbAggregationResult> distinctPrRowMapper(Optional<String> additionalKey,
                                                                     ScmPrFilter.DISTINCT key,
                                                                     ScmPrFilter.CALCULATION calculation,
                                                                     boolean acrossFilesChanged) {
        return (rs, rowNumber) -> {
            switch (key) {
                case pr_reviewed:
                case pr_created:
                case pr_updated:
                case pr_merged:
                case pr_closed:
                    return setCalculationComponent(rs, calculation, String.valueOf(key),
                            additionalKey.orElse(null), true);
                case reviewer:
                    return setCalculationComponent(rs, calculation, "reviewer_ids", "reviewers", false);
                case approver:
                    return setCalculationComponent(rs, calculation, "approver_ids", "approvers", false);
                case assignee:
                    return setCalculationComponent(rs, calculation, "assignee_id", "assignee", false);
                case repo_id:
                case none:
                    return setCalculationComponent(rs, calculation, "repo_ids", null, false);
                case creator:
                    return setCalculationComponent(rs, calculation, "creator_id", key.toString(), false);
                case branch:
                    return setCalculationComponent(rs, calculation, "source_branch", null, false);
                case code_change:
                    if (acrossFilesChanged) {
                        return setCalculationComponent(rs, calculation, "files_changed", null, false);
                    }
                    return setCalculationComponent(rs, calculation, "code_changes", null, false);
                default:
                    return setCalculationComponent(rs, calculation, key.toString(), null, false);
            }
        };
    }

    private static DbAggregationResult setCalculationComponent(ResultSet rs, ScmPrFilter.CALCULATION calculation,
                                                               String key, String additionalKey, Boolean isTimeBased) throws SQLException {

        DbAggregationResult.DbAggregationResultBuilder dbAggregationResultBuilder = DbAggregationResult.builder();
        Boolean isFloatType = rs.getMetaData().getColumnTypeName(1).equals("float8");
        switch (calculation) {
            case author_response_time:
            case reviewer_response_time:
            case reviewer_approve_time:
            case reviewer_comment_time:
            case merge_time:
                return dbAggregationResultBuilder
                        .key(StringUtils.isNotEmpty(key) ? (isTimeBased && isFloatType ? String.valueOf(rs.getBigDecimal(key).longValue()) : rs.getString(key)) : null)
                        .additionalKey(StringUtils.isNotEmpty(additionalKey) ? rs.getString(additionalKey) : null)
                        .max(rs.getLong("mx"))
                        .min(rs.getLong("mn"))
                        .median(rs.getLong("median"))
                        .count(rs.getLong("ct"))
                        .mean(rs.getDouble("mean"))
                        .sum(rs.getLong("sum"))
                        .build();
            case count:
            default:
                return dbAggregationResultBuilder
                        .key(StringUtils.isNotEmpty(key) ? (isTimeBased && isFloatType ? String.valueOf(rs.getBigDecimal(key).longValue()) : rs.getString(key)) : null)
                        .additionalKey(StringUtils.isNotEmpty(additionalKey) ? rs.getString(additionalKey) : null)
                        .count(Long.valueOf(rs.getString("ct")))
                        .totalComments(columnPresent(rs, "total_comments") ? Long.valueOf(rs.getString("total_comments")) : null)
                        .linesAddedCount(columnPresent(rs, "lines_added") ? Long.valueOf(rs.getString("lines_added")) : null)
                        .linesRemovedCount(columnPresent(rs, "lines_deleted") ? Long.parseLong(rs.getString("lines_deleted")) : null)
                        .linesChangedCount(columnPresent(rs, "lines_changed") ? Long.parseLong(rs.getString("lines_changed")) : null)
                        .filesChangedCount(columnPresent(rs, "total_files_changed") ? Long.valueOf(rs.getString("total_files_changed")) : null)
                        .avgFilesChanged(columnPresent(rs, "avg_files_changed") ? Double.valueOf(rs.getString("avg_files_changed")) : null)
                        .avgLinesChanged(columnPresent(rs, "avg_lines_changed") ? Double.valueOf(rs.getString("avg_lines_changed")) : null)
                        .medianFilesChanged(columnPresent(rs, "median_files_changed") ? Double.valueOf(rs.getString("median_files_changed")) : null)
                        .medianLinesChanged(columnPresent(rs, "median_lines_changed") ? Double.valueOf(rs.getString("median_lines_changed")) : null)
                        .build();
        }
    }

    public static RowMapper<DbAggregationResult> distinctPrRowDurationMapper(Optional<String> additionalKey,
                                                                             ScmPrFilter.DISTINCT key) {
        return (rs, rowNumber) -> {
            switch (key) {
                case pr_created:
                case pr_updated:
                case pr_merged:
                case pr_closed:
                    return DbAggregationResult.builder()
                            .key(rs.getString(String.valueOf(key)))
                            .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                            .count(rs.getLong("ct"))
                            .sum(rs.getLong("sm"))
                            .max(rs.getLong("mx"))
                            .min(rs.getLong("mn"))
                            .median(rs.getLong("md"))
                            .build();
                default:
                    throw new SQLException("Unsupported query.");
            }
        };
    }

    public static RowMapper<DbAggregationResult> distinctFilesRowMapper(ScmFilesFilter.DISTINCT key) {
        return (rs, rowNumber) -> {
            switch (key) {
                default:
                    return DbAggregationResult.builder()
                            .key(rs.getString(key.toString()))
                            .count(rs.getLong("ct"))
                            .build();
            }
        };
    }


    public static RowMapper<DbAggregationResult> distinctIssueRowMapper(ScmIssueFilter.DISTINCT key,
                                                                        ScmIssueFilter.CALCULATION calc,
                                                                        Optional<String> additionalKey) {
        return (rs, rowNumber) -> {
            String stringKey = key.toString();
            if (key == ScmIssueFilter.DISTINCT.creator) {
                stringKey = "creator_id";
            }
            if (calc == ScmIssueFilter.CALCULATION.count) {
                return DbAggregationResult.builder()
                        .key(rs.getString(stringKey))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .build();
            } else if (calc == ScmIssueFilter.CALCULATION.response_time) {
                return DbAggregationResult.builder()
                        .key(rs.getString(stringKey))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .median(rs.getLong("md"))
                        .min(rs.getLong("mn"))
                        .max(rs.getLong("mx"))
                        .sum(rs.getLong("sm"))
                        .build();
            } else if (calc == ScmIssueFilter.CALCULATION.resolution_time) {
                return DbAggregationResult.builder()
                        .key(rs.getString(stringKey))
                        .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                        .count(rs.getLong("ct"))
                        .median(rs.getLong("md"))
                        .build();
            }
            throw new SQLException("Unsupported query.");
        };
    }

    public static RowMapper<DbAggregationResult> distinctCommitRowMapper(Optional<String> additionalKey,
                                                                         ScmCommitFilter.DISTINCT key,
                                                                         ScmCommitFilter.CALCULATION calculation,
                                                                         boolean valuesOnly) {
        return (rs, rowNumber) -> {
            switch (calculation) {
                case commit_days:
                    if (List.of(ScmCommitFilter.DISTINCT.author, ScmCommitFilter.DISTINCT.committer).contains(key)) {
                        return setCommitDaysCalculationComponentForCommits(rs, additionalKey.orElse(null), Optional.of(key.toString()));
                    }
                    return setCommitDaysCalculationComponentForCommits(rs, key.toString(), additionalKey);
                case commit_count:
                    if (List.of(ScmCommitFilter.DISTINCT.author, ScmCommitFilter.DISTINCT.committer).contains(key)) {
                        return setCommitCountCalculationComponentForCommits(rs, additionalKey.orElse(null), Optional.of(key.toString()));
                    }
                    return setCommitCountCalculationComponentForCommits(rs, key.toString(), additionalKey);
                case commit_count_only:
                    if (List.of(ScmCommitFilter.DISTINCT.author, ScmCommitFilter.DISTINCT.committer).contains(key)) {
                        return setCommitCountOnlyCalculationComponentForCommits(rs, additionalKey.orElse(null), Optional.of(key.toString()));
                    }
                    return setCommitCountOnlyCalculationComponentForCommits(rs, key.toString(), additionalKey);
                default:
                    if (valuesOnly) {
                        if (List.of(ScmCommitFilter.DISTINCT.repo_id, ScmCommitFilter.DISTINCT.author, ScmCommitFilter.DISTINCT.committer).contains(key)) {
                            return DbAggregationResult.builder()
                                    .key((key.equals(ScmCommitFilter.DISTINCT.repo_id)) ? rs.getString("repo_ids") : rs.getString(additionalKey.get()))
                                    .additionalKey(additionalKey.isPresent() ? rs.getString(key.toString()) : null)
                                    .count(rs.getLong("ct"))
                                    .build();
                        }
                        return DbAggregationResult.builder()
                                .key((key.equals(ScmCommitFilter.DISTINCT.repo_id)) ? rs.getString("repo_ids") : rs.getString(key.toString()))
                                .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                                .count(rs.getLong("ct"))
                                .build();
                    }
                    if (List.of(ScmCommitFilter.DISTINCT.repo_id, ScmCommitFilter.DISTINCT.author, ScmCommitFilter.DISTINCT.committer).contains(key)) {
                        return DbAggregationResult.builder()
                                .key((key.equals(ScmCommitFilter.DISTINCT.repo_id)) ? rs.getString("repo_ids") : rs.getString(additionalKey.get()))
                                .additionalKey(additionalKey.isPresent() ? rs.getString(key.toString()) : null)
                                .count(rs.getLong("ct"))
                                .filesChangedCount(columnPresent(rs, "tot_files_ct") ? rs.getLong("tot_files_ct") : null)
                                .linesAddedCount(rs.getLong("tot_lines_added"))
                                .linesChangedCount(rs.getLong("tot_lines_changed"))
                                .linesRemovedCount(rs.getLong("tot_lines_removed"))
                                .avgChangeSize(Float.valueOf(String.format("%.3f", rs.getFloat("avg_change_size"))))
                                .medianChangeSize(rs.getLong("median_change_size"))
                                .pctLegacyRefactoredLines(Double.valueOf(String.format("%.2f", rs.getDouble("pct_legacy_refactored_lines"))))
                                .pctNewLines(Double.valueOf(String.format("%.2f", rs.getDouble("pct_new_lines"))))
                                .pctRefactoredLines(Double.valueOf(String.format("%.2f", rs.getDouble("pct_refactored_lines"))))
                                .build();
                    } else if (ScmCommitFilter.DISTINCT.technology == key) {
                        return DbAggregationResult.builder()
                                .key(rs.getString(key.toString()))
                                .additionalKey(additionalKey.isPresent() ? rs.getString(key.toString()) : null)
                                .count(rs.getLong("ct"))
                                .build();
                    } else {
                        return DbAggregationResult.builder()
                                .key(rs.getString(String.valueOf(key)))
                                .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                                .count(columnPresent(rs, "ct") ? rs.getLong("ct") : null)
                                .filesChangedCount(columnPresent(rs, "tot_files_ct") ? rs.getLong("tot_files_ct") : null)
                                .linesAddedCount(rs.getLong("tot_lines_added"))
                                .linesChangedCount(rs.getLong("tot_lines_changed"))
                                .linesRemovedCount(rs.getLong("tot_lines_removed"))
                                .avgChangeSize(Float.valueOf(String.format("%.3f", rs.getFloat("avg_change_size"))))
                                .medianChangeSize(rs.getLong("median_change_size"))
                                .pctLegacyRefactoredLines(Double.valueOf(String.format("%.2f", rs.getDouble("pct_legacy_refactored_lines"))))
                                .pctNewLines(Double.valueOf(String.format("%.2f", rs.getDouble("pct_new_lines"))))
                                .pctRefactoredLines(Double.valueOf(String.format("%.2f", rs.getDouble("pct_refactored_lines"))))
                                .build();
                    }
            }
        };
    }

    private static DbAggregationResult setCommitCountOnlyCalculationComponentForCommits(ResultSet rs, String key, Optional<String> additionalKey) throws SQLException {
        return DbAggregationResult.builder()
                .key(StringUtils.isNotBlank(key) ? ((key.equals(ScmCommitFilter.DISTINCT.repo_id.name())) ? rs.getString("repo_ids") : rs.getString(key)) : null)
                .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                .count(columnPresent(rs, "ct") ? rs.getLong("ct") : null)
                .filesChangedCount(columnPresent(rs, "tot_files_ct") ? rs.getLong("tot_files_ct") : null)
                .linesAddedCount(rs.getLong("tot_lines_added"))
                .linesChangedCount(rs.getLong("tot_lines_changed"))
                .linesRemovedCount(rs.getLong("tot_lines_removed"))
                .avgChangeSize(Float.valueOf(String.format("%.3f", rs.getFloat("avg_change_size"))))
                .medianChangeSize(rs.getLong("median_change_size"))
                .build();
    }

    private static DbAggregationResult setCommitDaysCalculationComponentForCommits(ResultSet rs, String key, Optional<String> additionalKey) throws SQLException {
        return DbAggregationResult.builder()
                .key(StringUtils.isNotBlank(key) ? ((key.equals(ScmCommitFilter.DISTINCT.repo_id.name())) ? rs.getString("repo_ids") : rs.getString(key)) : null)
                .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                .commitSize(rs.getLong("commit_size"))
                .mean(rs.getDouble("mean"))
                .median(rs.getLong("median"))
                .build();
    }

    private static DbAggregationResult setCommitCountCalculationComponentForCommits(ResultSet rs, String key, Optional<String> additionalKey) throws SQLException {
        return DbAggregationResult.builder()
                .key(StringUtils.isNotBlank(key) ? ((key.equals(ScmCommitFilter.DISTINCT.repo_id.name())) ? rs.getString("repo_ids") : rs.getString(key)) : null)
                .additionalKey(additionalKey.isPresent() ? rs.getString(additionalKey.get()) : null)
                .median(rs.getLong("median"))
                .commitSize(rs.getLong("commit_size"))
                .dayOfWeek(rs.getString("day").trim())
                .mean(rs.getDouble("mean"))
                .build();
    }

    public static RowMapper<DbAggregationResult> commitFileRowMapper() {
        return (rs, rowNumber) -> DbAggregationResult.builder()
                .key(rs.getString("filename"))
                .additionalKey(rs.getString("author"))
                .count(rs.getLong("no_of_commits"))
                .build();
    }

    public static RowMapper<DbScmReview> reviewRowMapper() {
        return (rs, rowNumber) -> DbScmReview.builder()
                .id(rs.getString("id"))
                .prId(rs.getString("pr_id"))
                .state(rs.getString("state"))
                .reviewer(rs.getString("reviewer"))
                .reviewId(rs.getString("review_id"))
                .reviewedAt(rs.getDate("reviewed_at").getTime() / 1000)
                .build();
    }

    public static RowMapper<DbAggregationResult> escalatedSFCasesMapper(String key) {
        return (rs, rowNumber) -> {
            return DbAggregationResult.builder()
                    .repoId(rs.getString("repo_id"))
                    .project(rs.getString("project"))
                    .key(rs.getString(key))
                    .totalCases(rs.getLong("ct"))
                    .build();
        };
    }

    public static RowMapper<DbAggregationResult> escalatedZDTicketMapper(String key) {
        return (rs, rowNumber) -> {
            return DbAggregationResult.builder()
                    .repoId(rs.getString("repo_id"))
                    .project(rs.getString("project"))
                    .key(rs.getString(key))
                    .totalTickets(rs.getLong("ct"))
                    .build();
        };
    }

    public static RowMapper<DbAggregationResult> moduleRowMapper(String key) {
        return (rs, rowNumber) -> {
            return DbAggregationResult.builder()
                    .repoId(rs.getString("repo_id"))
                    .project(rs.getString("project"))
                    .key(rs.getString(key))
                    .count(rs.getLong("no_of_commits"))
                    .build();
        };
    }

    public static RowMapper<DbScmUser> userRowMapper() {
        return (rs, rowNumber) -> DbScmUser.builder()
                .id(rs.getString("id"))
                .integrationId(rs.getString("integration_id"))
                .cloudId(rs.getString("cloud_id"))
                .displayName(rs.getString("display_name"))
                .build();
    }

    public static RowMapper<DbScmPRLabel> mapPRLabel() {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            UUID scmPullRequestId = (UUID) rs.getObject("scm_pullrequest_id");
            String cloudId = rs.getString("cloud_id");
            String name = rs.getString("name");
            String description = rs.getString("description");
            Instant labelAddedAt = DateUtils.toInstant(rs.getTimestamp("label_added_at"));

            DbScmPRLabel scmPRLabel = DbScmPRLabel.builder()
                    .id(id)
                    .scmPullRequestId(scmPullRequestId)
                    .cloudId(cloudId).name(name).description(description)
                    .labelAddedAt(labelAddedAt)
                    .build();

            return scmPRLabel;
        };
    }

    public static RowMapper<DbScmPRLabelLite> mapPRLabelLite() {
        return (rs, rowNumber) -> {
            String name = rs.getString("name");
            DbScmPRLabelLite scmPRLabel = DbScmPRLabelLite.builder().name(name).build();
            return scmPRLabel;
        };
    }

    public static RowMapper<DbAggregationResult> scmActivityMapper(String key, String additionalKey) {
        return (rs, rowNumber) -> {
            return DbAggregationResult.builder()
                    .key(rs.getString(key))
                    .additionalKey(rs.getString(additionalKey))
                    .count(rs.getLong("count"))
                    .build();
        };
    }

    public static RowMapper<DbScmTag> mapScmTag() {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            String integrationId = rs.getString("integration_id");
            String project = rs.getString("project");
            String repo = rs.getString("repo");
            String tag = rs.getString("tag");
            String commitSha = rs.getString("commit_sha");
            Long createdAt = rs.getTimestamp("created_at").toInstant().getEpochSecond();
            Long updatedAt = rs.getTimestamp("updated_at").toInstant().getEpochSecond();
            return DbScmTag.builder()
                    .id(id)
                    .integrationId(integrationId)
                    .project(project)
                    .repo(repo)
                    .tag(tag)
                    .commitSha(commitSha)
                    .createdAt(createdAt)
                    .updatedAt(updatedAt)
                    .build();
        };
    }

    public static RowMapper<GitTechnology> mapScmTech() {
        return (rs, rowNumber) -> {
            String id = rs.getString("id");
            String integrationId = rs.getString("integration_id");
            String name = rs.getString("name");
            String repo = rs.getString("repo_id");
            Long createdAt = rs.getLong("created_at");
            return GitTechnology.builder()
                    .id(id)
                    .integrationId(integrationId)
                    .repoId(repo)
                    .name(name)
                    .createdAt(createdAt)
                    .build();
        };
    }

    public static RowMapper<DbScmCommit> commitESRowMapper() {

        return (rs, rowNumber) -> {
            return DbScmCommit.builder()
                    .id(rs.getString("id"))
                    .repoIds((rs.getArray("repo_id") != null &&
                            rs.getArray("repo_id").getArray() != null) ?
                            Arrays.asList((String[]) rs.getArray("repo_id").getArray()) : List.of())
                    .project(rs.getString("project"))
                    .fileTypes(columnPresent(rs, "file_types") ? Arrays.asList((String[]) rs.getArray("file_types").getArray())
                            : List.of())
                    .integrationId(rs.getString("integration_id"))
                    .author(rs.getString("author"))
                    .authorId(rs.getString("author_id"))
                    .committer(rs.getString("committer"))
                    .committerId(rs.getString("committer_id"))
                    .commitSha(rs.getString("commit_sha"))
                    .commitUrl(rs.getString("commit_url"))
                    .message(rs.getString("message"))
                    .filesCt(rs.getInt("files_ct"))
                    .additions(rs.getInt("additions"))
                    .deletions(rs.getInt("deletions"))
                    .changes(rs.getInt("changes"))
                    .vcsType(VCS_TYPE.valueOf(rs.getString("vcs_type")))
                    .ingestedAt(rs.getLong("ingested_at"))
                    .createdAt(rs.getLong("created_at"))
                    .branch(columnPresent(rs, "commit_branch") ? rs.getString("commit_branch") : null)
                    .commitPushedAt(columnPresent(rs, "commit_pushed_at") ? rs.getTimestamp("commit_pushed_at").toInstant().getEpochSecond()
                            : null)
                    .committedAt(rs.getTimestamp("committed_at").toInstant().getEpochSecond())
                    .directMerge(rs.getBoolean("direct_merge"))
                    .build();
        };
    }

    public static RowMapper<DbScmPRLabel> mapESPRLabel() {
        return (rs, rowNumber) -> {
            UUID id = (UUID) rs.getObject("id");
            UUID scmPullRequestId = (UUID) rs.getObject("scm_pullrequest_id");
            String cloudId = rs.getString("cloud_id");
            String name = rs.getString("name");
            String description = rs.getString("description");
            Long labelAddedAt = DateUtils.toInstant(rs.getTimestamp("label_added_at")).getEpochSecond();

            DbScmPRLabel scmPRLabel = DbScmPRLabel.builder()
                    .id(id)
                    .scmPullRequestId(scmPullRequestId)
                    .cloudId(cloudId).name(name).description(description)
                    .labelAddedTs(labelAddedAt)
                    .build();

            return scmPRLabel;
        };
    }

    public static RowMapper<DbScmCommitStats> prCommitStatRowMapper() {
        return (rs, rowNumber) -> DbScmCommitStats.builder()
                .integrationId(rs.getString("integration_id"))
                .commitSha(rs.getString("commit_sha"))
                .additions(rs.getInt("additions"))
                .deletions(rs.getInt("deletions"))
                .changes(rs.getInt("changes"))
                .fileCount(rs.getInt("files_ct"))
                .committedAt(rs.getTimestamp("committed_at").toInstant().toEpochMilli())
                .build();
    }
}