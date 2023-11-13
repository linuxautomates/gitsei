package io.levelops.commons.faceted_search.db.models.scm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommitDetails;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder(toBuilder = true)
@JsonDeserialize(builder = EsScmCommit.EsScmCommitBuilder.class)
public class EsScmCommit {

    static final String UNKNOWN = "_UNKNOWN_";

    @JsonProperty("c_id")
    private String id;

    @JsonProperty("c_integration_id")
    private String integrationId;

    @JsonProperty("c_repo_id")
    private List<String> repoIds; //repository id -- something like: "levelops/api-levelops"

    @JsonProperty("c_vcs_type")
    private VCS_TYPE vcsType;

    @JsonProperty("c_project")
    private String project;

    @JsonProperty("c_commit_sha")
    private String commitSha;

    @JsonProperty("c_branch")
    private String branch;

    @JsonProperty("c_committer")
    private String committer; //name from git committer field

    @JsonProperty("c_author")
    private String author; //name from git author field

    @JsonProperty("c_commit_url")
    private String commitUrl;

    @JsonProperty("c_message")
    private String message; //commit msg

    @JsonProperty("c_files_ct")
    private Integer filesCt;

    @JsonProperty("c_additions")
    private Integer additions;

    @JsonProperty("c_deletions")
    private Integer deletions;

    @JsonProperty("c_changes")
    private Integer changes;

    @JsonProperty("c_loc")
    private Integer loc;

    @JsonProperty("c_committed_at")
    private Long committedAt;

    @JsonProperty("c_ingested_at")
    private Long ingestedAt;

    @JsonProperty("c_commit_pushed_at")
    private Long commitPushedAt;

    @JsonProperty("c_created_at")
    private Long createdAt;

    @JsonProperty("c_technology")
    private String technology;

    @JsonProperty("c_author_info")
    private DbScmUser authorInfo; //contains DbScmUser object for author

    @JsonProperty("c_committer_info")
    private DbScmUser committerInfo; //contains DbScmUser object for committer

    @JsonProperty("c_author_id")
    private String authorId; //refers to unique row in scm_users

    @JsonProperty("c_committer_id")
    private String committerId; //refers to unique row in scm_users

    @JsonProperty("c_file_types")
    private List<String> fileTypes;

    //only used during insertion
    @JsonProperty("c_issue_keys")
    private List<String> issueKeys;

    @JsonProperty("c_workitem_ids")
    private List<String> workitemIds;

    @JsonProperty("c_tot_lines_changed")
    Long totalLinesChanged;

    @JsonProperty("c_tot_lines_removed")
    Long totalLinesRemoved;

    @JsonProperty("c_tot_lines_added")
    Long totalLinesAdded;

    @JsonProperty("c_pct_legacy_refactored_lines")
    private Double pctLegacyLines;

    @JsonProperty("c_pct_refactored_lines")
    private Double pctRefactoredLines;

    @JsonProperty("c_pct_new_lines")
    private Double pctNewLines;

    @JsonProperty("c_total_legacy_lines")
    Long legacyLinesCount;

    @JsonProperty("c_total_refactored_lines")
    Long linesRefactoredCount;

    @JsonProperty("c_has_issue_keys")
    private Boolean hasIssueKeys;

    @JsonProperty("c_files")
    private List<DbScmFileCommitDetails> fileCommitList;

    @JsonProperty("c_scm_tags")
    private List<DbScmTag> tags;

    @JsonProperty("c_technologies")
    private List<GitTechnology> technologies;

    @JsonProperty("c_day")
    String dayOfWeek;

    @JsonProperty("c_direct_merge")
    private Boolean directMerge;

    @JsonProperty("c_pr_list")
    private List<String> prList;

    @JsonProperty("c_pr_count")
    private Integer prCount;

    @JsonProperty("c_committer_pr_list")
    private List<String> committerPrList;

    @JsonProperty("c_committer_pr_count")
    private Integer committerPrCount;
}
