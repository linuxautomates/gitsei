package io.levelops.commons.databases.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.base.Stopwatch;
import io.levelops.commons.databases.converters.CountQueryConverter;
import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.converters.StringQueryConverter;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmFileCommit;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabelLite;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmContributorsFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.filters.ScmIssueFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.ScmReposFilter;
import io.levelops.commons.databases.models.filters.VCS_TYPE;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.ScmCommitsFilterParser;
import io.levelops.commons.databases.services.parsers.ScmContributorsFilterParser;
import io.levelops.commons.databases.services.parsers.ScmFilesFilterParser;
import io.levelops.commons.databases.services.parsers.ScmFilterParserCommons;
import io.levelops.commons.databases.services.parsers.ScmIssuesFilterParser;
import io.levelops.commons.databases.services.parsers.ScmPrsFilterParser;
import io.levelops.commons.databases.services.parsers.ScmReposFilterParser;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.CriteriaUtils;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.databases.utils.TransactionCallback;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.UUIDUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.server.ResponseStatusException;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_APPROVER_COUNT;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkApprovalStatus;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkApproversFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCollaborationState;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommentDensityFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCommitsTableFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkCreatorsFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkFileTableFilters;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkPRReviewedJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkReviewTypeFilters;
import static io.levelops.commons.databases.services.ScmQueryUtils.checkReviewersFiltersJoin;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCodeChangeSql;
import static io.levelops.commons.databases.services.ScmQueryUtils.getCommitsFilterForTrendStack;
import static io.levelops.commons.databases.services.ScmQueryUtils.getFileCommitsSelect;
import static io.levelops.commons.databases.services.ScmQueryUtils.getFilesChangeSql;
import static io.levelops.commons.databases.services.ScmQueryUtils.getFilterForTrendStack;
import static io.levelops.commons.databases.services.ScmQueryUtils.getFilterWithConfig;
import static io.levelops.commons.databases.services.ScmQueryUtils.getScmCommitMetricsQuery;
import static io.levelops.commons.databases.services.ScmQueryUtils.getScmSortOrder;
import static io.levelops.commons.helper.organization.OrgUnitHelper.newOUConfigForStacks;
import static java.util.stream.Collectors.toList;
import static org.joda.time.DateTimeConstants.SECONDS_PER_DAY;

@Log4j2
@Service
public class ScmAggService extends DatabaseService<DbScmPullRequest> {

    public static final Set<String> FILES_PARTIAL_MATCH_COLUMNS = Set.of("repo_id", "project", "filename");
    public static final Set<String> ISSUES_PARTIAL_MATCH_COLUMNS = Set.of("title", "issue_id", "creator", "state", "project");
    public static final Set<String> PRS_PARTIAL_MATCH_COLUMNS = Set.of("title", "state", "creator", "source_branch", "target_branch", "project");
    public static final Set<String> PRS_PARTIAL_MATCH_ARRAY_COLUMNS = Set.of("assignees", "labels", "repo_id", "reviewer", "approver");
    public static final Set<String> COMMITS_PARTIAL_MATCH_COLUMNS = Set.of("author", "committer", "message", "project", "commit_title", "commit_branch");
    public static final Set<String> COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS = Set.of("repo_id");
    public static final Set<String> CONTRIBUTORS_PARTIAL_MATCH_COLUMNS = Set.of("author", "committer", "message");
    private static final Set<String> PR_LABELS_PARTIAL_MATCH_COLUMNS = Set.of("name");
    private static final Set<String> PR_LABELS_PARTIAL_MATCH_ARRAY_COLUMNS = Set.of();
    private static final Set<String> PR_LABELS_SORTABLE_COLUMNS = Set.of("name");
    private static final int DEFAULT_STACK_PARALLELISM = 30;
    private static final int TOP_N_RECORDS = 30;
    private static final int STACK_COUNT_THRESHOLD = 50;
    private static final String TAGS_TABLE = "scm_tags";
    private static final String FILES_TABLE = "scm_files";
    private static final String ISSUES_TABLE = "scm_issues";
    public static final String COMMITS_TABLE = "scm_commits";
    public static final String PRS_TABLE = "scm_pullrequests";
    private static final String FILE_COMMITS_TABLE = "scm_file_commits";
    public static final String REVIEWS_TABLE = "scm_pullrequest_reviews";
    public static final String PR_LABELS_TABLE = "scm_pullrequest_labels";
    public static final String COMMIT_WORKITEM_TABLE = "scm_commit_workitem_mappings";
    public static final String PULLREQUESTS_WORKITEM_TABLE = "scm_pullrequests_workitem_mappings";
    public static final String PULLREQUESTS_JIRA_TABLE = "scm_pullrequests_jira_mappings";
    public static final String COMMIT_JIRA_TABLE = "scm_commit_jira_mappings";
    private static final String USERS_TABLE = "integration_users";
    private static final String FINAL_TABLE = "final_table";
    private static final String FILES_UNIT = "files";
    public static final Set<String> COMMIT_SORTABLE_COLUMNS = Set.of("committed_at", "created_at");
    public static final Set<String> ISSUE_SORTABLE_COLUMNS = Set.of("issue_updated_at");
    public static final Set<String> FILE_SORTABLE_COLUMNS = Set.of("num_commits", "changes", "deletions", "additions");
    public static final Set<String> COLLAB_REPORT_SORTABLE_COLUMNS = Set.of("collab_state", "creator", "approvers");
    public static final Set<String> COMMITTERS_SORTABLE_COLUMNS =
            Set.of("num_repos", "num_commits", "num_additions", "num_deletions", "num_changes", "committer", "num_prs");
    private static final List<String> SCM_APPLICATIONS = List.of("helix", "bitbucket", "github", "gitlab", "azure_devops");
    public static final Set<String> REPOS_SORTABLE_COLUMNS =
            Set.of("num_commits", "num_additions", "num_deletions", "num_changes", "repo", "num_prs");

    //region Commits
    private static final String UPDATE_COMMIT_DIRECT_MERGE_SQL_FORMAT = "UPDATE %s." + COMMITS_TABLE + " SET direct_merge = :direct_merge WHERE id = :id";
    private static final String UPDATE_COMMIT_BRANCH_SQL = "UPDATE %s." + COMMITS_TABLE + " SET commit_branch = :commit_branch WHERE id = :id";
    private static final String UPDATE_COMMIT_CHANGES_SQL_FORMAT = "UPDATE %s." + COMMITS_TABLE + " SET changes = :changes WHERE id = :id";
    private static final String UPDATE_COMMIT_CHANGE_VOLUME_SQL_FORMAT = "UPDATE %s." + COMMITS_TABLE + " SET additions = :additions, deletions = :deletions, changes = :changes WHERE id = :id";
    private static final String UPDATE_COMMIT_INGESTED_AT_SQL_FORMAT = "UPDATE %s." + COMMITS_TABLE + " SET ingested_at = :ingested_at WHERE id = :id";
    private static final String UPDATE_COMMIT_USERS_ID_SQL_FORMAT = "UPDATE %s." + COMMITS_TABLE + " SET committer_id = :committer_id, author_id = :author_id WHERE id = :id";
    //end region

    //region PRs
    private static final String PR_INSERT_SQL_FORMAT = "INSERT INTO %s." + PRS_TABLE +
            " (repo_id,project,integration_id,state,number,creator,creator_id,merge_sha,source_branch,target_branch,merged,assignees,assignee_ids,labels,commit_shas,title,pr_updated_at,pr_merged_at,pr_closed_at,pr_created_at,author_response_time,reviewer_response_time,metadata)" +
            " VALUES(:repo_id,:project,:integration_id,:state,:number,:creator,:creator_id,:merge_sha,:source_branch,:target_branch,:merged,:assignees,:assignee_ids::uuid[],:labels,:commit_shas,:title,:pr_updated_at,:pr_merged_at,:pr_closed_at,:pr_created_at,:author_response_time,:reviewer_response_time,:metadata::jsonb) ON CONFLICT (number,repo_id,project,integration_id)" +
            " DO UPDATE SET (state,creator,creator_id,merge_sha,source_branch,target_branch,merged,assignees,assignee_ids,labels,commit_shas,title,pr_updated_at,pr_merged_at,pr_closed_at,pr_created_at,author_response_time,reviewer_response_time,metadata)" +
            " = (EXCLUDED.state,EXCLUDED.creator,EXCLUDED.creator_id,EXCLUDED.merge_sha,EXCLUDED.source_branch,EXCLUDED.target_branch,EXCLUDED.merged,EXCLUDED.assignees,EXCLUDED.assignee_ids,EXCLUDED.labels,EXCLUDED.commit_shas,EXCLUDED.title,EXCLUDED.pr_updated_at,EXCLUDED.pr_merged_at,EXCLUDED.pr_closed_at,EXCLUDED.pr_created_at,EXCLUDED.author_response_time,EXCLUDED.reviewer_response_time,EXCLUDED.metadata)" +
            " RETURNING id";
    private static final String UPDATE_PR_METADATA_SQL_FORMAT = "UPDATE %s." + PRS_TABLE + " SET metadata = :metadata::jsonb WHERE id = :pr_id::uuid ";
    private static final String PR_WORKITEM_INSERT_SQL_FORMAT = "INSERT INTO %s." + PULLREQUESTS_WORKITEM_TABLE + " (scm_integration_id,project,pr_id,workitem_id,pr_uuid) VALUES(:scm_integration_id,:project,:pr_id,:workitem_id,:pr_uuid) " +
            " ON CONFLICT ON CONSTRAINT scm_pullrequests_workitem_mappings_unique_pr_workitem DO NOTHING";
    private static final String PR_JIRA_KEY_INSERT_SQL_FORMAT = "INSERT INTO %s." + PULLREQUESTS_JIRA_TABLE + " (scm_integration_id,project,pr_id,issue_key,pr_uuid) VALUES(:scm_integration_id,:project,:pr_id,:issue_key,:pr_uuid) " +
            " ON CONFLICT ON CONSTRAINT scm_pullrequests_jira_mappings_unique_pr_jira DO NOTHING";
    private static final String PR_DELETE_REVIEWS_SQL_FORMAT = "DELETE FROM %s." + REVIEWS_TABLE + " WHERE pr_id = :pr_id";
    private static final String PR_INSERT_REVIEW_SQL_FORMAT = "INSERT INTO %s." + REVIEWS_TABLE + " (reviewer,reviewer_id,review_id,state,pr_id,reviewed_at) VALUES (:reviewer,:reviewer_id,:review_id,:state,:pr_id,:reviewed_at) ON CONFLICT (review_id,pr_id) DO NOTHING";
    private static final String PR_DELETE_REMOVED_LABELS = "DELETE from %s." + PR_LABELS_TABLE + " WHERE scm_pullrequest_id = :scm_pullrequest_id AND cloud_id NOT IN (:cloud_ids)";
    private static final String PR_DELETE_ALL_LABELS = "DELETE from %s." + PR_LABELS_TABLE + " WHERE scm_pullrequest_id = :scm_pullrequest_id";
    private static final String PR_INSERT_LABEL_SQL_FORMAT = "INSERT INTO %s." + PR_LABELS_TABLE + " (scm_pullrequest_id,cloud_id,name,description,label_added_at) VALUES (:scm_pullrequest_id,:cloud_id,:name,:description,:label_added_at) ON CONFLICT (scm_pullrequest_id,cloud_id) DO UPDATE SET(name,description) = (EXCLUDED.name,EXCLUDED.description)";

    //end region

    //region tags
    private static final String INSERT_TAG_SQL_FORMAT = "INSERT INTO %s.scm_tags (integration_id,project,repo,tag,commit_sha)" +
            " VALUES(:integration_id,:project,:repo,:tag,:commit_sha) " +
            " ON CONFLICT (repo,project,integration_id,tag) DO UPDATE SET commit_sha = EXCLUDED.commit_sha" +
            " RETURNING id";
    private static final String UPDATE_TAG_SQL_FORMAT = "UPDATE %s.scm_tags SET integration_id = :integration_id, project = " +
            ":project, repo = :repo, tag = :tag, commit_sha = :commit_sha , updated_at = now() WHERE id = :id";
    private static final String DELETE_TAG_SQL_FORMAT = "DELETE FROM %s.scm_tags WHERE id = :id";
    //end region

    private final Set<ScmPrFilter.DISTINCT> stackSupported = Set.of(
            ScmPrFilter.DISTINCT.reviewer,
            ScmPrFilter.DISTINCT.approval_status,
            ScmPrFilter.DISTINCT.approver,
            ScmPrFilter.DISTINCT.assignee,
            ScmPrFilter.DISTINCT.repo_id,
            ScmPrFilter.DISTINCT.reviewer_count,
            ScmPrFilter.DISTINCT.approver_count,
            ScmPrFilter.DISTINCT.code_change,
            ScmPrFilter.DISTINCT.comment_density,
            ScmPrFilter.DISTINCT.project,
            ScmPrFilter.DISTINCT.label,
            ScmPrFilter.DISTINCT.branch,
            ScmPrFilter.DISTINCT.source_branch,
            ScmPrFilter.DISTINCT.target_branch,
            ScmPrFilter.DISTINCT.creator,
            ScmPrFilter.DISTINCT.collab_state,
            ScmPrFilter.DISTINCT.state,
            ScmPrFilter.DISTINCT.review_type,
            ScmPrFilter.DISTINCT.pr_closed,
            ScmPrFilter.DISTINCT.pr_merged,
            ScmPrFilter.DISTINCT.pr_created);
    private final Set<ScmCommitFilter.DISTINCT> stackSupportedForCommits = Set.of(
            ScmCommitFilter.DISTINCT.code_change,
            ScmCommitFilter.DISTINCT.project,
            ScmCommitFilter.DISTINCT.author,
            ScmCommitFilter.DISTINCT.file_type,
            ScmCommitFilter.DISTINCT.code_category,
            ScmCommitFilter.DISTINCT.repo_id,
            ScmCommitFilter.DISTINCT.committer,
            ScmCommitFilter.DISTINCT.vcs_type
    );
    private final NamedParameterJdbcTemplate template;
    private final PlatformTransactionManager transactionManager;
    private ProductsDatabaseService productsDatabaseService;
    private ScmPrsFilterParser scmPrsFilterParser;
    private ScmCommitsFilterParser scmCommitsFilterParser;
    private ScmFilesFilterParser scmFilesFilterParser;
    private ScmIssuesFilterParser scmIssuesFilterParser;
    private ScmReposFilterParser scmReposFilterParser;
    private ScmContributorsFilterParser scmContributorsFilterParser;
    private ScmFilterParserCommons scmFilterParserCommons;
    private UserIdentityService userIdentityService;
    private Integer scmCommitInsertV2SyncFileCommitsThreadCount;
    private ObjectMapper objectMapper = DefaultObjectMapper.get();

    public ScmAggService(final DataSource dataSource, final UserIdentityService userIdentityService) {
        this(dataSource, userIdentityService, 1);
    }
    @Autowired
    public ScmAggService(final DataSource dataSource, final UserIdentityService userIdentityService, @org.springframework.beans.factory.annotation.Value("${SCM_COMMIT_INSERT_V2_SYNC_FILE_COMMITS_THREAD_COUNT:1}") Integer scmCommitInsertV2SyncFileCommitsThreadCount) {
        super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.transactionManager = new DataSourceTransactionManager(dataSource);
        productsDatabaseService = new ProductsDatabaseService(dataSource, objectMapper);
        scmFilterParserCommons = new ScmFilterParserCommons(productsDatabaseService);
        scmPrsFilterParser = new ScmPrsFilterParser();
        scmIssuesFilterParser = new ScmIssuesFilterParser();
        scmCommitsFilterParser = new ScmCommitsFilterParser();
        scmFilesFilterParser = new ScmFilesFilterParser();
        scmReposFilterParser = new ScmReposFilterParser();
        scmContributorsFilterParser = new ScmContributorsFilterParser();
        this.userIdentityService = userIdentityService;
        this.scmCommitInsertV2SyncFileCommitsThreadCount = scmCommitInsertV2SyncFileCommitsThreadCount;
    }

    @Override
    public Set<Class<? extends DatabaseService<?>>> getReferences() {
        return Set.of(IntegrationService.class, UserIdentityService.class);
    }

    //region PR Insert Complete
    //region Insert PR Only
    private MapSqlParameterSource constructPRParameterSource(DbScmPullRequest pr, UUID creatorUserId) {
        //Sort Repo Ids
        List<String> repoIds = new ArrayList<>(pr.getRepoIds());
        Collections.sort(repoIds);

        String[] assignees = new String[0];
        String[] assigneeIds = new String[0];
        if (CollectionUtils.isNotEmpty(pr.getAssigneesInfo())) {
            assignees = pr.getAssigneesInfo().stream().map(DbScmUser::getDisplayName).toArray(String[]::new);
            assigneeIds = pr.getAssigneesInfo().stream().map(DbScmUser::getId).toArray(String[]::new);
        }

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("repo_id", repoIds.toArray(new String[0]));
        params.addValue("project", pr.getProject());
        params.addValue("integration_id", NumberUtils.toInt(pr.getIntegrationId()));
        params.addValue("state", pr.getState());
        params.addValue("number", pr.getNumber());
        params.addValue("creator", pr.getCreator());
        params.addValue("creator_id", creatorUserId);
        params.addValue("merge_sha", pr.getMergeSha());
        params.addValue("source_branch", pr.getSourceBranch());
        params.addValue("target_branch", pr.getTargetBranch());
        params.addValue("metadata", ParsingUtils.serialize(objectMapper, "metadata", pr.getMetadata(), "{}"));
        params.addValue("merged", pr.getMerged());
        params.addValue("assignees", assignees);
        params.addValue("assignee_ids", assigneeIds);
        //params.addValue("labels", pr.getLabels());
        params.addValue("labels", pr.getLabels().toArray(new String[0]));
        params.addValue("commit_shas", pr.getCommitShas().toArray(new String[0]));
        params.addValue("title", pr.getTitle());
        params.addValue("pr_updated_at", LocalDateTime.ofEpochSecond(pr.getPrUpdatedAt(), 0, ZoneOffset.UTC));
        params.addValue("pr_merged_at", pr.getPrMergedAt() != null ? LocalDateTime.ofEpochSecond(pr.getPrMergedAt(), 0, ZoneOffset.UTC) : null);
        params.addValue("pr_closed_at", pr.getPrClosedAt() != null ? LocalDateTime.ofEpochSecond(pr.getPrClosedAt(), 0, ZoneOffset.UTC) : null);
        params.addValue("pr_created_at", LocalDateTime.ofEpochSecond(pr.getPrCreatedAt(), 0, ZoneOffset.UTC));
        params.addValue("author_response_time", computeAuthorResponseTime(pr));
        params.addValue("reviewer_response_time", computeReviewerResponseTime(pr));

        return params;
    }

    private UUID insertPR(String company, DbScmPullRequest pr, UUID creatorUserId) throws SQLException {
        if (CollectionUtils.isNotEmpty(pr.getAssigneesInfo())) {
            List<DbScmUser> assigneesInfo = pr.getAssigneesInfo().stream().map(assignee -> {
                try {
                    return assignee.toBuilder().id(userIdentityService.upsertIgnoreEmail(company, assignee)).build();
                } catch (SQLException e) {
                    log.error("Error while inserting user: {}", e.getMessage(), e);
                }
                return null;
            }).collect(toList());
            pr = pr.toBuilder().assigneesInfo(assigneesInfo).build();
        }
        MapSqlParameterSource params = constructPRParameterSource(pr, creatorUserId);
        String insertConfigSql = String.format(PR_INSERT_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(insertConfigSql, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert velocity config");
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id;
    }
    //endregion

    private SqlParameterSource constructPRWorkitemParameterSource(String workitem, DbScmPullRequest pr, UUID prId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scm_integration_id", NumberUtils.toInt(pr.getIntegrationId()));
        params.addValue("project", pr.getProject());
        params.addValue("pr_id", pr.getNumber());
        params.addValue("workitem_id", workitem);
        params.addValue("pr_uuid", prId);
        return params;
    }

    private SqlParameterSource constructPRIssueKeyParameterSource(String issueKey, DbScmPullRequest pr, UUID prId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scm_integration_id", NumberUtils.toInt(pr.getIntegrationId()));
        params.addValue("project", pr.getProject());
        params.addValue("pr_id", pr.getNumber());
        params.addValue("issue_key", issueKey);
        params.addValue("pr_uuid", prId);
        return params;
    }

    private void insertPRWorkitems(String company, DbScmPullRequest pr, UUID prId) {
        if (CollectionUtils.isEmpty(pr.getWorkitemIds())) {
            return;
        }
        String insertPrWorkitemSql = String.format(PR_WORKITEM_INSERT_SQL_FORMAT, company);
        List<SqlParameterSource> parameterSources = pr.getWorkitemIds().stream()
                .map(workitem -> constructPRWorkitemParameterSource(workitem, pr, prId)).collect(toList());
        int[] updateCounts = template.batchUpdate(insertPrWorkitemSql, parameterSources.toArray(new SqlParameterSource[0]));
        log.debug("Insert PR Workitem Mapping response {}", updateCounts);
    }

    private void insertPRIssueKeys(String company, DbScmPullRequest pr, UUID prId) {
        if (CollectionUtils.isEmpty(pr.getIssueKeys())) {
            return;
        }
        String insertPrIssueKeySql = String.format(PR_JIRA_KEY_INSERT_SQL_FORMAT, company);
        List<SqlParameterSource> parameterSources = pr.getIssueKeys().stream()
                .map(issueKey -> constructPRIssueKeyParameterSource(issueKey, pr, prId)).collect(toList());
        int[] updateCounts = template.batchUpdate(insertPrIssueKeySql, parameterSources.toArray(new SqlParameterSource[0]));
        log.debug("Insert PR Issue Key Mapping response {}", updateCounts);
    }

    //region PR Delete Reviews
    private Boolean deleteAllReviewsForPr(String company, UUID prId) {
        String deleteSql = String.format(PR_DELETE_REVIEWS_SQL_FORMAT, company);
        return template.update(deleteSql, Map.of("pr_id", prId)) > 0;
    }
    //endregion

    //region PR Insert Reviews
    private SqlParameterSource constructPRReviewParameterSource(DbScmReview review, UUID prId, UUID userId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("reviewer", review.getReviewer());
        params.addValue("reviewer_id", userId);
        params.addValue("review_id", review.getReviewId());
        params.addValue("state", review.getState());
        params.addValue("pr_id", prId);
        params.addValue("reviewed_at", LocalDateTime.ofEpochSecond(review.getReviewedAt(), 0, ZoneOffset.UTC));
        return params;
    }

    public void insertPRReviews(String company, DbScmPullRequest pr, UUID prId) throws SQLException {
        if (CollectionUtils.isEmpty(pr.getReviews())) {
            return;
        }
        String integrationId = pr.getIntegrationId();
        List<SqlParameterSource> parameterSources = pr.getReviews().stream().map(r ->
                constructPRReviewParameterSource(r, prId, insertAndGetUserId(company, integrationId, r))).collect(Collectors.toList());
        String insertConfigSql = String.format(PR_INSERT_REVIEW_SQL_FORMAT, company);
        int[] updateCounts = template.batchUpdate(insertConfigSql, parameterSources.toArray(new SqlParameterSource[0]));
        log.debug("Insert PR reviews response {}", updateCounts);
    }

    @Nullable
    private UUID insertAndGetUserId(String company, String integrationId, DbScmReview r) {
        if(StringUtils.isBlank(r.getReviewerInfo().getCloudId()) && StringUtils.isBlank(r.getReviewerInfo().getDisplayName())){
            return null;
        }
        UUID userId = null;
        try {
            String userIdString = userIdentityService.upsertIgnoreEmail(company, DbScmUser.builder()
                    .integrationId(integrationId)
                    .cloudId(StringUtils.isNotBlank(r.getReviewerInfo().getCloudId()) ? r.getReviewerInfo().getCloudId() : r.getReviewerInfo().getDisplayName())
                    .displayName(StringUtils.isNotBlank(r.getReviewerInfo().getDisplayName()) ? r.getReviewerInfo().getDisplayName() : r.getReviewerInfo().getCloudId())
                    .originalDisplayName(StringUtils.isNotBlank(r.getReviewerInfo().getDisplayName()) ? r.getReviewerInfo().getDisplayName() : r.getReviewerInfo().getCloudId())
                    .build());
            userId = UUID.fromString(userIdString);
        } catch (SQLException e) {
            log.error("Error while inserting user: {}", e.getMessage(), e);
        }
        return userId;
    }
    //endregion

    //region PR Sync Labels
    //region PR Delete Removed Labels
    private Boolean deleteRemovedLabelsFromPrs(String company, DbScmPullRequest pr, UUID prId) {
        List<String> currentCloudIds = CollectionUtils.emptyIfNull(pr.getPrLabels()).stream().map(DbScmPRLabel::getCloudId).collect(Collectors.toList());
        Map<String, Object> params = new HashMap<>();
        params.put("scm_pullrequest_id", prId);

        String deleteSql = null;
        if (CollectionUtils.isEmpty(currentCloudIds)) {
            deleteSql = String.format(PR_DELETE_ALL_LABELS, company);
        } else {
            deleteSql = String.format(PR_DELETE_REMOVED_LABELS, company);
            params.put("cloud_ids", currentCloudIds);
        }
        return template.update(deleteSql, params) > 0;
    }
    //endregion

    //region PR Insert Labels
    private SqlParameterSource constructPRLabelParameterSource(DbScmPRLabel label, UUID prId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("scm_pullrequest_id", prId);
        params.addValue("cloud_id", label.getCloudId());
        params.addValue("name", label.getName());
        params.addValue("description", label.getDescription());//Timestamp.from
        params.addValue("label_added_at", Timestamp.from(label.getLabelAddedAt()));
        return params;
    }

    private void insertPRLabels(String company, DbScmPullRequest pr, UUID prId) {
        if (CollectionUtils.isEmpty(pr.getPrLabels())) {
            return;
        }
        List<SqlParameterSource> parameterSources = pr.getPrLabels().stream().map(l -> constructPRLabelParameterSource(l, prId)).collect(Collectors.toList());
        String insertConfigSql = String.format(PR_INSERT_LABEL_SQL_FORMAT, company);
        int[] updateCounts = template.batchUpdate(insertConfigSql, parameterSources.toArray(new SqlParameterSource[0]));
        log.debug("Insert PR Labels response {}", updateCounts);
    }
    //endregion

    public void syncPRLabels(String company, DbScmPullRequest pr, UUID prId) {
        //Sync PR Labels - Step 1 - Delete the deleted labels
        deleteRemovedLabelsFromPrs(company, pr, prId);
        //Sync PR Labels - Step 2 - Insert the new labels
        insertPRLabels(company, pr, prId);
    }
    //endregion

    // LEV-3954: Need to update the commit_shas column after changes made for LEV-3564
    // ToDo: Need to remove after some time...
    public void updatePrCommitShas(String company, UUID prId, DbScmPullRequest pr) {
        if (CollectionUtils.isEmpty(pr.getCommitShas())) {
            return;
        }
        String sql = "UPDATE " + company + "." + PRS_TABLE + " SET commit_shas = :commit_shas WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", prId);
        params.addValue("commit_shas", pr.getCommitShas().toArray(new String[0]));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        template.update(sql, params);
    }

    // LEV-3954: Need to update the target branch column after changes made for LEV-4559
    public void updatePrTargetBranch(String company, UUID prId, DbScmPullRequest pr) {
        if (StringUtils.isEmpty(pr.getTargetBranch())) {
            return;
        }
        String sql = "UPDATE " + company + "." + PRS_TABLE + " SET target_branch = :target_branch WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", prId);
        params.addValue("target_branch", pr.getTargetBranch());
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        template.update(sql, params);
    }
    //endregion

    // LEV-5240: Need to update the project column.
    public void updatePrProject(String company, UUID prId, DbScmPullRequest pr) {
        if (StringUtils.isEmpty(pr.getProject())) {
            return;
        }
        String sql = "UPDATE " + company + "." + PRS_TABLE + " SET project = :project WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", prId);
        params.addValue("project", pr.getProject());
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        template.update(sql, params);
    }
    //endregion

    @Override
    public String insert(String company, DbScmPullRequest pr) throws SQLException {
        DbScmUser creator = DbScmUser.builder()
                .integrationId(pr.getIntegrationId())
                .cloudId(StringUtils.isNotBlank(pr.getCreatorInfo().getCloudId()) ? pr.getCreatorInfo().getCloudId() : pr.getCreatorInfo().getDisplayName())
                .displayName(StringUtils.isNotBlank(pr.getCreatorInfo().getDisplayName()) ? pr.getCreatorInfo().getDisplayName() : pr.getCreatorInfo().getCloudId())
                .originalDisplayName(StringUtils.isNotBlank(pr.getCreatorInfo().getDisplayName()) ? pr.getCreatorInfo().getDisplayName() : pr.getCreatorInfo().getCloudId())
                .build();
        String userIdString = userIdentityService.upsertIgnoreEmail(company, creator);
        UUID userId = UUID.fromString(userIdString);
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            UUID id = insertPR(company, pr, userId);
            deleteAllReviewsForPr(company, id);
            insertPRReviews(company, pr, id);
            insertPRWorkitems(company, pr, id);
            insertPRIssueKeys(company, pr, id);
            //Sync PR Labels
            syncPRLabels(company, pr, id);
            transactionManager.commit(txStatus);
            return id.toString();
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            throw e;
        }

    }
    //endregion

    public String updateScmMetadata(String company, String prId, Map<String, Object> metadata) throws SQLException {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("metadata", ParsingUtils.serialize(objectMapper, "metadata", metadata, "{}"));
        params.addValue("pr_id", prId);
        String updatePrMetadata = String.format(UPDATE_PR_METADATA_SQL_FORMAT, company);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updatedRows = template.update(updatePrMetadata, params, keyHolder);
        if (updatedRows <= 0 || keyHolder.getKeys() == null) {
            throw new SQLException("Failed to insert velocity config");
        }
        UUID id = (UUID) keyHolder.getKeys().get("id");
        return id.toString();
    }

    long computeAuthorResponseTime(DbScmPullRequest pr) {
        List<DbScmReview> reviews = new ArrayList<>(MoreObjects.firstNonNull(pr.getReviews(), Collections.emptyList()));
        reviews.sort(Comparator.comparing(DbScmReview::getReviewedAt));

        Long reviewerReviewTime = null;
        long authorResponseTime = 0;

        String creator = pr.getCreator();
        for (DbScmReview review : reviews) {
            if (reviewerReviewTime == null && !review.getReviewer().equalsIgnoreCase(creator)) {
                reviewerReviewTime = review.getReviewedAt();
            } else if (reviewerReviewTime != null && review.getReviewer().equalsIgnoreCase(creator)) {
                authorResponseTime += (review.getReviewedAt() - reviewerReviewTime);
                reviewerReviewTime = null;
            }
        }

        if (reviewerReviewTime != null) {
            authorResponseTime += (MoreObjects.firstNonNull(pr.getPrMergedAt(),
                    MoreObjects.firstNonNull(pr.getPrClosedAt(),
                            TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))) - reviewerReviewTime);
        }


        return authorResponseTime;
    }

    long computeReviewerResponseTime(DbScmPullRequest pr) {
        List<DbScmReview> reviews = new ArrayList<>(MoreObjects.firstNonNull(pr.getReviews(), Collections.emptyList()));
        reviews.sort(Comparator.comparing(DbScmReview::getReviewedAt));

        Long authorReviewTime = pr.getPrCreatedAt();
        long reviewerResponseTime = 0;

        String creator = pr.getCreator();
        for (DbScmReview review : reviews) {
            if (authorReviewTime != null && !review.getReviewer().equalsIgnoreCase(creator)) {
                reviewerResponseTime += review.getReviewedAt() - authorReviewTime;
                authorReviewTime = null;
            } else if (authorReviewTime == null && review.getReviewer().equalsIgnoreCase(creator)) {
                authorReviewTime = review.getReviewedAt();
            }
        }

        if (authorReviewTime != null) {
            reviewerResponseTime += (MoreObjects.firstNonNull(pr.getPrMergedAt(),
                    MoreObjects.firstNonNull(pr.getPrClosedAt(),
                            TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()))) - authorReviewTime);
        }

        return reviewerResponseTime;
    }


    //region PR Update - Unsupported
    //we dont support updates because the insert does all the work
    @Override
    public Boolean update(String company, DbScmPullRequest pr) {
        throw new UnsupportedOperationException();
    }
    //endregion

    //region PR Gets
    //region PR Get by ID - Unsupported
    //we dont support this get because the filter requires: integration_id + key + ingestedAt
    @Override
    public Optional<DbScmPullRequest> get(String company, String param) throws SQLException {
        throw new UnsupportedOperationException();
    }
    //endregion

    //region PR Get
    public Optional<DbScmPullRequest> getPr(String company, String number, String repoId, String integrationId)
            throws SQLException {
        Validate.notBlank(number, "Missing number.");
        Validate.notBlank(repoId, "Missing repo_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT *, scm_pullrequests.metadata->>'pr_link' as pr_link FROM " + company + "." + PRS_TABLE
                + " WHERE number = :number AND repo_id && ARRAY[ :repo_id ]::varchar[] AND integration_id = :integid";
        Map<String, Object> params = Map.of("number", number, "repo_id", repoId, "integid", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmPullRequest> data = template.query(sql, params, DbScmConverters.prRowMapper());
        return data.stream().findFirst();
    }
    //endregion

    //region PR Get
    public Optional<DbScmPullRequest> getPr(String company, String number, List<String> repoIds, String integrationId) {
        Validate.notBlank(number, "Missing number.");
        Validate.notEmpty(repoIds, "Missing repo_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * , scm_pullrequests.metadata->>'pr_link' as pr_link FROM " + company + "." + PRS_TABLE
                + " WHERE number = :number AND repo_id && ARRAY[ :repo_id ]::varchar[] AND integration_id = :integid";
        Map<String, Object> params = Map.of("number", number, "repo_id", repoIds, "integid", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmPullRequest> data = template.query(sql, params, DbScmConverters.prRowMapper());
        return data.stream().findFirst();
    }
    //endregion
    //endregion

    //region PR List



    @Override
    public DbListResponse<DbScmPullRequest> list(String company, Integer pageNumber,
                                                 Integer pageSize) throws SQLException {
        return list(company, ScmPrFilter.builder().build(), Collections.emptyMap(), null, pageNumber, pageSize);
    }


    public DbListResponse<DbScmPullRequest> list(String company,
                                                 ScmPrFilter filter,
                                                 Map<String, SortingOrder> sortBy,
                                                 OUConfiguration ouConfig,
                                                 Integer pageNumber,
                                                 Integer pageSize)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String prsWhere = "";
        String filterByProductSQL = "";
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmPrSorting.PR_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "pr_updated_at";
                })
                .orElse("pr_updated_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForPrsList(company, filter, params, ouConfig);
        }
        ScmPrFilter.CALCULATION CALC = filter.getCalculation();
        Map<String, List<String>> conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig, true);
        if (conditions.get(PRS_TABLE).size() > 0) {
            if (CALC != null && CALC.equals(ScmPrFilter.CALCULATION.merge_time)) {
                conditions.get(PRS_TABLE).add("merged");
            }
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String creatorJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String reviewerJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER);
        String approverJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER);
        String commenterJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER);
        String commitsPRsJoin = ScmQueryUtils.getCommitsPRsJoin(company, params, filter, "", "");
        String prJiraMappingJoin = ScmQueryUtils.getSqlForPRJiraMappingTable(company);
        String prWorkItemIdsMappingJoin = ScmQueryUtils.getSqlForPRWorkItemMappingTable(company);
        String prsReviewedAtJoin = ScmQueryUtils.getSqlForPRReviewedAtJoin(company);
        String approvalStatusSelect = ScmQueryUtils.getApprovalStatusSqlStmt();
        String codeChangeSql = getCodeChangeSql(filter.getCodeChangeSizeConfig(), true,filter.getCodeChangeUnit());
        String fileChangeSql = getFilesChangeSql(filter.getCodeChangeSizeConfig());
        String commentDensitySql = ScmQueryUtils.getCommentDensitySql(filter);
        String collaborationStateSql = ScmQueryUtils.getCollaborationStateSql();
        String issueKeysSql = ScmQueryUtils.getIssueKeysSql();
        String jiraWorkitemPrsMappingSelect = ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT;
        String prApprovalTimeSql = ScmQueryUtils.PR_APPROVE_TIME_SQL;
        String prCommentTimeSql =  ScmQueryUtils.PR_COMMENT_TIME_SQL;

        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM ( SELECT " + prsSelect + creatorsSelect
                    + ScmQueryUtils.COMMITS_PRS_SELECT + ScmQueryUtils.REVIEW_PRS_SELECT_LIST
                    + ScmQueryUtils.APPROVERS_PRS_LIST + ScmQueryUtils.COMMENTERS_SELECT_SQL
                    + jiraWorkitemPrsMappingSelect + ScmQueryUtils.PRS_REVIEWED_AT_SELECT
                    + prApprovalTimeSql + prCommentTimeSql
                    + approvalStatusSelect + codeChangeSql + collaborationStateSql + ScmQueryUtils.getReviewType() + ScmQueryUtils.PRS_REVIEWER_COUNT
                    + PRS_APPROVER_COUNT + fileChangeSql + commentDensitySql + issueKeysSql
                    + ScmQueryUtils.RESOLUTION_TIME_SQL + " FROM "
                    + company + "." + PRS_TABLE
                    + (filter.getHasComments() != null && filter.getHasComments() ? " INNER JOIN ( SELECT MIN(reviewed_at) as pr_reviewed_at,pr_id FROM "
                    + company + "." + REVIEWS_TABLE + " GROUP BY pr_id ) AS first_reviews ON scm_pullrequests.id = first_reviews.pr_id" : StringUtils.EMPTY)
                    + creatorJoin
                    + reviewerJoin
                    + approverJoin
                    + commenterJoin
                    + commitsPRsJoin
                    + prJiraMappingJoin
                    + prWorkItemIdsMappingJoin
                    + prsReviewedAtJoin
                    + " ) a " + prsWhere;
        }

        List<DbScmPullRequest> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT *"
                    + " FROM (" + filterByProductSQL + ") x ORDER BY " + sortByKey + " " + sortOrder
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.prRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + filterByProductSQL + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrs(String company,
                                                                      ScmPrFilter filter,
                                                                      OUConfiguration ouConfig) throws SQLException {
        return groupByAndCalculatePrs(company, filter, false, ouConfig);
    }

    public DbListResponse<DbAggregationResult> getCollaborationReport(String company, ScmPrFilter filter,
                                                                      OUConfiguration ouConfig,
                                                                      Boolean fromStack) {
        return getCollaborationReport(company, filter, ouConfig, fromStack, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> getCollaborationReport(String company, ScmPrFilter filter,
                                                                      OUConfiguration ouConfig,
                                                                      Boolean fromStack, Integer pageNumber, Integer pageSize){
        Map<String, List<String>> conditions;
        String prsWhere = "";
        Map<String, Object> params = new HashMap<>();
        String orderByString = getCollabReportOrderByString(filter.getSort());
        conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig);
        String key = fromStack ? "approver_ids" : "creator_id";
        String additionalKey = fromStack ? "approvers" : "creator";
        String creatorJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);
        String reviewerJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER);
        String approverJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER);
        String commenterJoin = ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER);
        String collaborationStateSql = ScmQueryUtils.getCollaborationStateSql();
        String reviewsTableSelect = ScmQueryUtils.REVIEW_PRS_SELECT_LIST;
        String approversTableSelect = ScmQueryUtils.APPROVERS_PRS_LIST;
        String commentersSelect = ScmQueryUtils.COMMENTERS_SELECT_SQL;
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }

        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String selectDistinctString = fromStack ? "  UNNEST(approver_id) AS approver_ids, unnest(approver) as approvers " : " creator_id, creator, collab_state ";
            String intrSql = "SELECT " + selectDistinctString + " ,count(DISTINCT id) as ct FROM " +
                    "(SELECT *  FROM (SELECT  scm_pullrequests.repo_id AS repo_ids, " + prsSelect + creatorsSelect
                    + reviewsTableSelect + approversTableSelect + commentersSelect + collaborationStateSql
                    + " FROM " + company + "." + PRS_TABLE
                    + creatorJoin
                    + reviewerJoin
                    + approverJoin
                    + commenterJoin
                    + " ) a" + prsWhere
                    + " ) y GROUP BY " + (fromStack ? "approver_ids, approvers " : " creator_id, creator, collab_state");
            String sql = intrSql + " ORDER BY " + orderByString
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.collaborationReportMapper(
                    key, additionalKey));
            String countSql = "SELECT COUNT(*) FROM ( " + intrSql + " ) i";
            log.info("countSql = " + countSql);
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }

    public DbListResponse<DbAggregationResult> getStackedCollaborationReport(String company, ScmPrFilter filter,
                                                                             OUConfiguration ouConfig) throws SQLException {
        return getStackedCollaborationReport(company, filter, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> getStackedCollaborationReport(String company, ScmPrFilter filter,
                                                                             OUConfiguration ouConfig, Integer pageNumber, Integer pageSize) throws SQLException {
        DbListResponse<DbAggregationResult> result = getCollaborationReport(company, filter, ouConfig, false, pageNumber, pageSize);
        ForkJoinPool threadPool = null;
        try {
            OUConfiguration ouConfigForStacks = newOUConfigForStacks(ouConfig, "approvers");
            final var finalOuConfigForStacks = newOUConfigForStacks(ouConfigForStacks, "creators");

            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                if (Objects.isNull(row.getKey())) {
                    return row.toBuilder().stacks(List.of()).build();
                }
                final ScmPrFilter.ScmPrFilterBuilder newFilterBuilder = filter.toBuilder();
                ScmPrFilter newFilter = getFilterWithConfig(newFilterBuilder, filter)
                        .creators(List.of(row.getKey()))
                        .collabStates(List.of(row.getCollabState()))
                        .sort(Map.of("approvers", SortingOrder.ASC))
                        .build();

                List<DbAggregationResult> currentStackResults = getCollaborationReport(company, newFilter, finalOuConfigForStacks, true).getRecords();
                return row.toBuilder().stacks(currentStackResults).build();
            });
            threadPool = new ForkJoinPool(DEFAULT_STACK_PARALLELISM);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            return DbListResponse.of(finalList, result.getTotalCount());
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new SQLException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }

    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrs(String company,
                                                                      ScmPrFilter filter,
                                                                      boolean valuesOnly,
                                                                      OUConfiguration ouConfig) throws SQLException {
        return groupByAndCalculatePrs(company, filter, valuesOnly, ouConfig, 0, Integer.MAX_VALUE);

    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrs(String company,
                                                                      ScmPrFilter filter,
                                                                      boolean valuesOnly,
                                                                      OUConfiguration ouConfig,
                                                                      Integer pageNumber,
                                                                      Integer pageSize) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        ScmPrFilter.CALCULATION calculation = filter.getCalculation();
        Map<String, Object> params = new HashMap<>();
        String filterByProductSQL = "";
        boolean acrossFilesChanged = false;
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        String prsWhere = "";
        boolean needIssueKeys = false;
        String calculationCountComponent;
        Map<String, List<String>> conditions;
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (calculation == null) {
            calculation = ScmPrFilter.CALCULATION.count;
        }
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForPrsGroupBy(company, filter, params,
                    filter.getAcross() == ScmPrFilter.DISTINCT.technology, ouConfig);
        }
        if (StringUtils.isNotEmpty(filter.getHasIssueKeys())) {
            needIssueKeys = true;
        }
        boolean needCommitsTable = checkCommitsTableFiltersJoin(filter, calculation, valuesOnly);
        boolean needCommentors = checkCommentDensityFiltersJoin(filter, calculation, valuesOnly);
        boolean needReviewedAt = checkPRReviewedJoin(filter);
        boolean needCreators = checkCreatorsFiltersJoin(filter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(filter, calculation, valuesOnly);
        boolean needApprovers = checkApproversFiltersJoin(filter, calculation, valuesOnly);
        boolean needApprovalStatuses = checkApprovalStatus(filter);
        boolean needCollabState = checkCollaborationState(filter);
        boolean needReviewTypes = checkReviewTypeFilters(filter);
        conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig, needCommitsTable);
        switch (calculation) {
            case author_response_time:
                calculationCountComponent = "COUNT(id) AS ct";
                calculationComponent = "MIN(author_response_time) AS mn,MAX(author_response_time) AS mx, " + calculationCountComponent + ",PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY author_response_time) AS median, SUM(author_response_time) as sum, SUM(author_response_time)/COUNT(id) as mean";
                break;
            case reviewer_response_time:
                calculationCountComponent = "COUNT(id) AS ct";
                calculationComponent = calculationCountComponent + ",MIN(reviewer_response_time) AS mn,MAX(reviewer_response_time) AS mx,PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY reviewer_response_time) AS median, SUM(author_response_time) as sum, SUM(reviewer_response_time)/COUNT(id) as mean";
                break;
            case reviewer_approve_time:
                calculationCountComponent = "COUNT(id) AS ct";
                calculationComponent = calculationCountComponent + ",MIN(reviewer_approve_time) AS mn,MAX(reviewer_approve_time) AS mx,PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY reviewer_approve_time) AS median, SUM(reviewer_approve_time) as sum, SUM(reviewer_approve_time)/COUNT(id) as mean";
                break;
            case reviewer_comment_time:
                calculationCountComponent = "COUNT(id) AS ct";
                calculationComponent = calculationCountComponent + ",MIN(reviewer_comment_time) AS mn,MAX(reviewer_comment_time) AS mx,PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY reviewer_comment_time) AS median, SUM(reviewer_comment_time) as sum, SUM(reviewer_comment_time)/COUNT(id) as mean";
                break;
            case merge_time:
                calculationCountComponent = "COUNT(id) AS ct";
                conditions.get(PRS_TABLE).add("merged");
                calculationComponent = calculationCountComponent + ",MIN(cycle_time) AS mn,MAX(cycle_time) as mx,PERCENTILE_DISC(0.5) " +
                        "WITHIN GROUP(ORDER BY cycle_time) as median,SUM(cycle_time) as sum,SUM(cycle_time)/COUNT(id) as mean ";
                break;
            case count:
                calculationCountComponent = "COUNT(DISTINCT pr_id) as ct";
                calculationComponent = calculationCountComponent + ", sum(COALESCE(array_length(commenter ,1),0)) as total_comments," +
                        "sum(lines_added) as lines_added,sum(lines_deleted) as lines_deleted," +
                        "sum(lines_changed) as lines_changed,ROUND(avg(lines_changed),5) as avg_lines_changed," +
                        "sum(files_ct) as total_files_changed,ROUND(avg(files_ct),5)  as avg_files_changed," +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY lines_changed) AS median_lines_changed,"
                        + "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY files_ct) AS median_files_changed";
                break;
            default:
                throw new SQLException("Invalid calculation field provided for this agg.");
        }

        String sortByKey = getPrsCountSortByKey(filter.getSort(), filter.getAcross().toString(), valuesOnly);
        orderByString = getPrsCountOrderByString(filter.getSort(), sortByKey);

        ScmPrFilter.DISTINCT DISTINCT = filter.getAcross();
        String intervalColumn = "";
        Optional<String> key = Optional.empty();
        boolean sortAscending = true;
        SortingOrder sortOrder = getScmSortOrder(filter.getSort());
        if(DISTINCT == ScmPrFilter.DISTINCT.none) {
            DISTINCT = ScmPrFilter.DISTINCT.repo_id;
        }
        switch (DISTINCT) {
            case reviewer:
                needReviewers = true;
                groupByString = "reviewer_ids, reviewers";
                selectDistinctString = "DISTINCT UNNEST(" + filter.getAcross() + " ) AS reviewers, UNNEST(reviewer_id) AS reviewer_ids";
                orderByString = " ct " + getScmSortOrder(filter.getSort());
                break;
            case approver_count:
                needApprovers = true;
                selectDistinctString = "approver_count";
                groupByString = filter.getAcross().toString();
                orderByString = "approver_count " + getScmSortOrder(filter.getSort());
                break;
            case collab_state:
                needCollabState = true;
                needApprovers = true;
                needCommentors = true;
                selectDistinctString = "collab_state";
                groupByString = filter.getAcross().toString();
                orderByString = "collab_state " + getScmSortOrder(filter.getSort());
                break;
            case reviewer_count:
                needReviewers = true;
                selectDistinctString = "reviewer_count";
                groupByString = filter.getAcross().toString();
                orderByString = "reviewer_count " + getScmSortOrder(filter.getSort());
                break;
            case code_change:
                selectDistinctString = "code_change as code_changes";
                groupByString = filter.getAcross().toString();
                orderByString = filter.getAcross() + " " + getScmSortOrder(filter.getSort());
                if (StringUtils.isNotEmpty(filter.getCodeChangeUnit()) && filter.getCodeChangeUnit().equals(FILES_UNIT)) {
                    acrossFilesChanged = true;
                    selectDistinctString = "files_changed as files_changed";
                    groupByString = "files_changed";
                    orderByString = " files_changed " + getScmSortOrder(filter.getSort());
                }
                needCommitsTable = true;
                break;
            case comment_density:
                selectDistinctString = "comment_density";
                groupByString = filter.getAcross().toString();
                needCommentors = true;
                break;
            case approver:
                needApprovers = true;
                groupByString = "approver_ids, approvers";
                selectDistinctString = "DISTINCT UNNEST(" + filter.getAcross() + " ) AS approvers, UNNEST(approver_id) AS approver_ids";
                orderByString = " ct " + getScmSortOrder(filter.getSort());
                break;
            case approval_status:
                needReviewers = true;
                needApprovers = true;
                needApprovalStatuses = true;
                selectDistinctString = "coalesce(approval_status, 'NONE') as approval_status";
                groupByString = filter.getAcross().toString();
                break;
            case review_type:
                needReviewers = true;
                needReviewTypes = true;
                selectDistinctString = "coalesce(review_type, 'NONE') as review_type";
                groupByString = filter.getAcross().toString();
                break;
            case assignee:
                groupByString = "assignee_id, " + filter.getAcross().name();
                selectDistinctString = "UNNEST(assignee_ids) AS assignee_id, UNNEST(" + filter.getAcross() + "s) AS " + filter.getAcross();
                break;
            case label:
                groupByString = filter.getAcross().toString();
                selectDistinctString = "UNNEST(" + filter.getAcross() + "s) AS "
                        + filter.getAcross();
                break;
            case creator:
                needCreators = true;
                groupByString = "creator_id, " + filter.getAcross().name();
                selectDistinctString = "creator_id, " + filter.getAcross().name();
                break;
            case branch:
                groupByString = "source_branch";
                selectDistinctString = "source_branch";
                orderByString = sortByKey.equals("ct") ? orderByString : " lower(source_branch) " + getScmSortOrder(filter.getSort());
                break;
            case source_branch:
            case target_branch:
            case state:
            case technology:
            case project:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                break;
            case repo_id:
                groupByString = "repo_ids";
                selectDistinctString = "repo_ids";
                break;
            case pr_merged:
                conditions.get(PRS_TABLE).add("merged = true");
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }

                    AggTimeQueryHelper.AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery
                            (DISTINCT + "_at", DISTINCT.toString(), filter.getAggInterval() != null ?
                                    filter.getAggInterval().toString() : null, false, sortAscending);
                    selectDistinctString = aggTimeQuery.getSelect();
                    intervalColumn = aggTimeQuery.getHelperColumn().replaceFirst(",", "") + ",";
                    groupByString = aggTimeQuery.getGroupBy();
                    if (MapUtils.isEmpty(filter.getSort()) || ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                        orderByString = aggTimeQuery.getOrderBy();
                    }
                    key = Optional.of(aggTimeQuery.getIntervalKey());
                break;
            case pr_closed:
                conditions.get(PRS_TABLE).add("pr_closed_at IS NOT NULL");
            case pr_reviewed:
            case pr_created:
            case pr_updated:
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }
                String columnName = DISTINCT + "_at";
                if (DISTINCT.equals(ScmPrFilter.DISTINCT.pr_reviewed)) {
                    conditions.get(PRS_TABLE).add("pr_reviewed_interval is NOT NULL");
                    columnName = "unnest(" + columnName + ")";
                }
                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        (columnName, DISTINCT.toString(), filter.getAggInterval() != null ?
                                filter.getAggInterval().toString() : null, false, sortAscending);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = ticketModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                groupByString = ticketModAggQuery.getGroupBy();
                if (MapUtils.isEmpty(filter.getSort()) || ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = ticketModAggQuery.getOrderBy();
                }
                key = Optional.of(ticketModAggQuery.getIntervalKey());
                break;
            default:
                Validate.notNull(DISTINCT == ScmPrFilter.DISTINCT.commenter ? DISTINCT: null , "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }

        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        String creatorJoin = needCreators ? ScmQueryUtils.sqlForCreatorTableJoin(company) : StringUtils.EMPTY;
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String commitsPRsJoin = needCommitsTable ? ScmQueryUtils.getCommitsPRsJoin(company, params, filter, "", "") : StringUtils.EMPTY;
        String prJiraMappingJoin = needIssueKeys ? ScmQueryUtils.getSqlForPRJiraMappingTable(company) : StringUtils.EMPTY;
        String prWorkItemIdsMappingJoin = needIssueKeys ? ScmQueryUtils.getSqlForPRWorkItemMappingTable(company) : StringUtils.EMPTY;
        String prsReviewedAtJoin = needReviewedAt ? ScmQueryUtils.getSqlForPRReviewedAtJoin(company) : StringUtils.EMPTY;
        String approvalStatusSelect = needApprovalStatuses ? ScmQueryUtils.getApprovalStatusSqlStmt() : StringUtils.EMPTY;
        String codeChangeSql = needCommitsTable ? getCodeChangeSql(filter.getCodeChangeSizeConfig(), true,filter.getCodeChangeUnit()) : StringUtils.EMPTY;
        String fileChangeSql = needCommitsTable ? getFilesChangeSql(filter.getCodeChangeSizeConfig()) : StringUtils.EMPTY;
        String commentDensitySql = needCommentors ? ScmQueryUtils.getCommentDensitySql(filter) : StringUtils.EMPTY;
        String issueKeysSql = needIssueKeys ? ScmQueryUtils.getIssueKeysSql() : StringUtils.EMPTY;
        String resolutionTimeSql = ScmQueryUtils.RESOLUTION_TIME_SQL;
        String prApprovalTimeSql = needApprovers ? ScmQueryUtils.PR_APPROVE_TIME_SQL : StringUtils.EMPTY;
        String prCommentTimeSql = needCommentors ? ScmQueryUtils.PR_COMMENT_TIME_SQL : StringUtils.EMPTY;
        String collaborationStateSql = (needCollabState || (needApprovers && needCommentors)) ?
                ScmQueryUtils.getCollaborationStateSql() : StringUtils.EMPTY;
        String reviewsTableSelect = needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY;
        String approversTableSelect = needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY;
        String commentersSelect = needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY;
        String commitsTableSelect = needCommitsTable ? ScmQueryUtils.COMMITS_PRS_SELECT : StringUtils.EMPTY;
        String jiraWorkItemSelect = needIssueKeys ? ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT : StringUtils.EMPTY;
        String prReviewerCountSelect = needReviewers ? ScmQueryUtils.PRS_REVIEWER_COUNT : StringUtils.EMPTY;
        String prApproversCountSelect = needApprovers ? PRS_APPROVER_COUNT : StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            if (prsWhere.equals("")) {
                prsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                prsWhere = prsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String daysOfWeekSelect = getDaysOfweekSelect(filter) ;
        String creatorsSelect = needCreators ? ScmQueryUtils.CREATORS_SQL : StringUtils.EMPTY;
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT *  FROM (SELECT " + intervalColumn + " unnest(scm_pullrequests.repo_id) AS repo_ids, " + prsSelect + creatorsSelect
                    + commitsTableSelect + reviewsTableSelect + approversTableSelect + commentersSelect + prReviewerCountSelect + prApproversCountSelect + jiraWorkItemSelect
                    + approvalStatusSelect + codeChangeSql + collaborationStateSql + ((needReviewTypes || needReviewers) ? ScmQueryUtils.getReviewType() : StringUtils.EMPTY) +
                    fileChangeSql + commentDensitySql + issueKeysSql + resolutionTimeSql + prApprovalTimeSql + prCommentTimeSql + daysOfWeekSelect
                    + " FROM " + company + "." + PRS_TABLE
                    + creatorJoin
                    + reviewerJoin
                    + approverJoin
                    + commenterJoin
                    + commitsPRsJoin
                    + prJiraMappingJoin
                    + prWorkItemIdsMappingJoin
                    + prsReviewedAtJoin
                    + " ) a" + prsWhere;
            if (filter.getAcross() == ScmPrFilter.DISTINCT.technology) {
                filterByProductSQL = "SELECT * FROM ( SELECT * FROM ( SELECT unnest(scm_pullrequests.repo_id) AS repo_ids,"
                        + prsSelect + creatorsSelect + commentersSelect + commitsTableSelect + collaborationStateSql + reviewsTableSelect + approversTableSelect +
                        prReviewerCountSelect + jiraWorkItemSelect +
                        approvalStatusSelect + resolutionTimeSql + daysOfWeekSelect +
                        " FROM " + company + "." + PRS_TABLE
                        + creatorJoin
                        + reviewerJoin
                        + approverJoin
                        + commenterJoin
                        + commitsPRsJoin
                        + prJiraMappingJoin
                        + prWorkItemIdsMappingJoin
                        + prsReviewedAtJoin
                        + " ) pr INNER JOIN ("
                        + " SELECT name as technology,repo_id as tr_id,integration_id as ti_id FROM "
                        + company + ".gittechnologies ) x ON x.tr_id = pr.repo_ids AND pr.integration_id = x.ti_id"
                        + " ) a" + prsWhere;
            }
        }
        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String intrSql = "SELECT " + selectDistinctString + (valuesOnly ? "," + calculationCountComponent :
                    (StringUtils.isNotEmpty(selectDistinctString) ? "," : "")
                            + calculationComponent)
                    + " FROM (" + filterByProductSQL + " ) y GROUP BY " + groupByString;
            String sql = intrSql + " ORDER BY " + orderByString
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.distinctPrRowMapper(
                    key, filter.getAcross(), calculation, acrossFilesChanged));
            String countSql = "SELECT COUNT(*) FROM ( " + intrSql + " ) i";
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }

    private String getDaysOfweekSelect(ScmPrFilter filter) {

        String daysOfWeekSelect = "";

        if(CollectionUtils.isNotEmpty(filter.getPrCreatedDaysOfWeek()))
            daysOfWeekSelect = ", Rtrim(To_char(Date(pr_created_at), 'Day')) as pr_created_day_of_week ";
        if(CollectionUtils.isNotEmpty(filter.getPrMergedDaysOfWeek()))
            daysOfWeekSelect += ", Rtrim(To_char(Date(pr_merged_at), 'Day')) as pr_merged_day_of_week ";
        if(CollectionUtils.isNotEmpty(filter.getPrClosedDaysOfWeek()))
            daysOfWeekSelect += ", Rtrim(To_char(Date(pr_closed_at), 'Day')) as pr_closed_day_of_week ";

        return daysOfWeekSelect;
    }
    //endregion

    //region PR Create Where Clause and Params
    public Map<String, List<String>> createPrWhereClauseAndUpdateParams(String company, Map<String, Object> params,
                                                                        ScmPrFilter filter, String paramSuffix,
                                                                        String prTblQualifier, OUConfiguration ouConfig) {
        return createPrWhereClauseAndUpdateParams(company, params, filter, paramSuffix, prTblQualifier, ouConfig, false);
    }

    public Map<String, List<String>> createPrWhereClauseAndUpdateParams(String company, Map<String, Object> params,
                                                                        ScmPrFilter filter, String paramSuffix,
                                                                        String prTblQualifier, OUConfiguration ouConfig,
                                                                        boolean needCommitLocFilter) {
        List<String> prTableConditions = new ArrayList<>();
        List<String> reviewTableConditions = new ArrayList<>();
        List<String> prJiraIssueTableConditions = new ArrayList<>();
        List<String> prWorkItemTableConditions = new ArrayList<>();

        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            if(filter.getIsApplyOuOnVelocityReport() == null || filter.getIsApplyOuOnVelocityReport()) {
                prTableConditions.add("(" + prTblQualifier + "id is NULL OR " + prTblQualifier + "repo_id && ARRAY[ :repo_ids" + paramSuffixString + " ]::varchar[] )");
            }else{
                prTableConditions.add(prTblQualifier + "repo_id && ARRAY[ :repo_ids" + paramSuffixString + " ]::varchar[]");
            }
            params.put("repo_ids" + paramSuffixString, filter.getRepoIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeRepoIds())) {
            prTableConditions.add("NOT " + prTblQualifier + "repo_id && ARRAY[ :exclude_repo_ids" + paramSuffixString + " ]::varchar[]");
            params.put("exclude_repo_ids" + paramSuffixString, filter.getExcludeRepoIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            if(filter.getIsApplyOuOnVelocityReport() == null || filter.getIsApplyOuOnVelocityReport()) {
                prTableConditions.add("(" + prTblQualifier + "id is NULL OR " + prTblQualifier + "project IN (:projects" + paramSuffixString + ") )");
            }else {
                prTableConditions.add(prTblQualifier + "project IN (:projects" + paramSuffixString + ")");
            }
            params.put("projects" + paramSuffixString, filter.getProjects());
        }
        if (StringUtils.isNotEmpty(filter.getHasIssueKeys())) {
            prTableConditions.add(prTblQualifier + "has_issue_keys = :has_issue_keys" + paramSuffixString);
            params.put("has_issue_keys" + paramSuffixString, filter.getHasIssueKeys().equals(String.valueOf(true)) ? 1 : 0);
        }
        if (CollectionUtils.isNotEmpty(filter.getCodeChanges()) && (filter.getCodeChangeUnit() == null || "lines".equals(filter.getCodeChangeUnit()))) {
            prTableConditions.add(prTblQualifier + "code_change IN (:code_change" + paramSuffixString + ")");
            params.put("code_change" + paramSuffixString, filter.getCodeChanges());
        }
        if (CollectionUtils.isNotEmpty(filter.getCodeChanges()) && "files".equals(filter.getCodeChangeUnit())) {
            prTableConditions.add(prTblQualifier + "files_changed IN (:code_change" + paramSuffixString + ")");
            params.put("code_change" + paramSuffixString, filter.getCodeChanges());
        }
        if (CollectionUtils.isNotEmpty(filter.getCommentDensities())) {
            prTableConditions.add(prTblQualifier + "comment_density IN (:comment_density" + paramSuffixString + ")");
            params.put("comment_density" + paramSuffixString, filter.getCommentDensities());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCodeChanges()) && (filter.getCodeChangeUnit() == null || "lines".equals(filter.getCodeChangeUnit())) ) {
            prTableConditions.add(prTblQualifier + "code_change NOT IN (:exclude_code_change" + paramSuffixString + ")");
            params.put("exclude_code_change" + paramSuffixString, filter.getExcludeCodeChanges());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCodeChanges()) && "files".equals(filter.getCodeChangeUnit())) {
            prTableConditions.add(prTblQualifier + "files_changed NOT IN (:exclude_code_change" + paramSuffixString + ")");
            params.put("exclude_code_change" + paramSuffixString, filter.getExcludeCodeChanges());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommentDensities())) {
            prTableConditions.add(prTblQualifier + "comment_density NOT IN (:exclude_comment_density" + paramSuffixString + ")");
            params.put("exclude_comment_density" + paramSuffixString, filter.getExcludeCommentDensities());
        }
        if (CollectionUtils.isNotEmpty(filter.getApprovalStatuses())) {
            prTableConditions.add(prTblQualifier + "approval_status IN (:approval_status" + paramSuffixString + ")");
            params.put("approval_status" + paramSuffixString, filter.getApprovalStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getCollabStates())) {
            prTableConditions.add(prTblQualifier + "collab_state IN (:collab_state" + paramSuffixString + ")");
            params.put("collab_state" + paramSuffixString, filter.getCollabStates());
        }
        if (CollectionUtils.isNotEmpty(filter.getPrCreatedDaysOfWeek())) {
            prTableConditions.add(prTblQualifier + "pr_created_day_of_week IN (:pr_created_day_of_week" + paramSuffixString + ")");
            params.put("pr_created_day_of_week" + paramSuffixString, filter.getPrCreatedDaysOfWeek());
        }
        if (CollectionUtils.isNotEmpty(filter.getPrMergedDaysOfWeek())) {
            prTableConditions.add(prTblQualifier + "pr_merged_day_of_week IN (:pr_merged_day_of_week" + paramSuffixString + ")");
            params.put("pr_merged_day_of_week" + paramSuffixString, filter.getPrMergedDaysOfWeek());
        }
        if (CollectionUtils.isNotEmpty(filter.getPrClosedDaysOfWeek())) {
            prTableConditions.add(prTblQualifier + "pr_closed_day_of_week IN (:pr_closed_day_of_week" + paramSuffixString + ")");
            params.put("pr_closed_day_of_week" + paramSuffixString, filter.getPrClosedDaysOfWeek());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCollabStates())) {
            prTableConditions.add(prTblQualifier + "collab_state NOT IN (:exclude_collab_state" + paramSuffixString + ")");
            params.put("exclude_collab_state" + paramSuffixString, filter.getExcludeCollabStates());
        }
        if (MapUtils.isNotEmpty(filter.getMissingFields())) {
            Map<ScmPrFilter.MISSING_BUILTIN_FIELD, Boolean> missingBuiltinFields = new EnumMap<>(
                    ScmPrFilter.MISSING_BUILTIN_FIELD.class);
            filter.getMissingFields().forEach((field, shouldBeMissing) -> {
                Optional.ofNullable(ScmPrFilter.MISSING_BUILTIN_FIELD.fromString(field))
                        .ifPresent(builtinField -> missingBuiltinFields.put(builtinField, shouldBeMissing));
            });
            prTableConditions.addAll(ScmQueryUtils.getMissingFieldsClause(missingBuiltinFields, params));
        }

        if (CollectionUtils.isNotEmpty(filter.getExcludeApprovalStatuses())) {
            prTableConditions.add(prTblQualifier + "approval_status NOT IN (:exclude_approval_status" + paramSuffixString + ")");
            params.put("exclude_approval_status" + paramSuffixString, filter.getExcludeApprovalStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getReviewTypes())) {
            prTableConditions.add(prTblQualifier + "review_type IN (:review_type" + paramSuffixString + ")");
            params.put("review_type" + paramSuffixString, filter.getReviewTypes().stream()
                    .map(reviewType -> reviewType.toUpperCase(Locale.ROOT)).collect(toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeReviewTypes())) {
            prTableConditions.add(prTblQualifier + "review_type NOT IN (:exclude_review_type" + paramSuffixString + ")");
            params.put("exclude_review_type" + paramSuffixString, filter.getExcludeReviewTypes()
                    .stream().map(reviewType -> reviewType.toUpperCase(Locale.ROOT)).collect(toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            prTableConditions.add(prTblQualifier + "project NOT IN (:exclude_projects" + paramSuffixString + ")");
            params.put("exclude_projects" + paramSuffixString, filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getApprovers()) || (OrgUnitHelper.doesOUConfigHavePRApprover(ouConfig))) { // OU: approvers
            var columnName = prTblQualifier + "approver_id";
            var columnNameParam = columnName + paramSuffixString;
            // OU first since it takes precedence over the original approvers
            if (OrgUnitHelper.doesOUConfigHavePRApprover(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    prTableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            }
            // only if OU is not configured for approvers
            else if (CollectionUtils.isNotEmpty(filter.getApprovers())) {
                TeamUtils.addUsersCondition(company, prTableConditions, params, prTblQualifier + "approver_id", columnNameParam, true,
                        filter.getApprovers(), SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeApprovers())) {
            String columnNameParam = prTblQualifier + "exclude_approver_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prTableConditions, params, "approver_id", columnNameParam, true,
                    filter.getExcludeApprovers(), SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getCommenters())) { // OU: commenter?
            String columnNameParam = prTblQualifier + "commenter_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prTableConditions, params, prTblQualifier + "commenter_id", columnNameParam, true,
                    filter.getCommenters(), SCM_APPLICATIONS);
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCommenters())) {
            String columnNameParam = prTblQualifier + "exclude_commenter_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prTableConditions, params, "commenter_id", columnNameParam, true,
                    filter.getExcludeCommenters(), SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getCreators()) || (OrgUnitHelper.doesOUConfigHavePRCreator(ouConfig))) { // OU: creators
            var columnName = prTblQualifier + "creator_id";
            var columnNameParam = columnName + paramSuffixString;
            // OU first since it takes precedence over the original approvers
            if (OrgUnitHelper.doesOUConfigHavePRCreator(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    prTableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(filter.getCreators())) {
                TeamUtils.addUsersCondition(company, prTableConditions, params, prTblQualifier + "creator_id", columnNameParam, false,
                        filter.getCreators(), SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCreators())) {
            String columnNameParam = prTblQualifier + "exclude_creator_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prTableConditions, params, "creator_id", columnNameParam, false,
                    filter.getExcludeCreators(), SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getSourceBranches())) {
            prTableConditions.add(prTblQualifier + "source_branch IN (:source_branches" + paramSuffixString + ")");
            params.put("source_branches" + paramSuffixString, filter.getSourceBranches());
        }
        if (CollectionUtils.isNotEmpty(filter.getTargetBranches())) {
            prTableConditions.add(prTblQualifier + "target_branch IN (:target_branches" + paramSuffixString + ")");
            params.put("target_branches" + paramSuffixString, filter.getTargetBranches());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeSourceBranches())) {
            prTableConditions.add(prTblQualifier + "source_branch NOT IN (:exclude_source_branches" + paramSuffixString + ")");
            params.put("exclude_source_branches" + paramSuffixString, filter.getExcludeSourceBranches());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTargetBranches())) {
            prTableConditions.add(prTblQualifier + "( target_branch NOT IN (:exclude_target_branches" + paramSuffixString + ") OR target_branch IS NULL ) ");
            params.put("exclude_target_branches" + paramSuffixString, filter.getExcludeTargetBranches());
        }
        if (CollectionUtils.isNotEmpty(filter.getStates())) {
            if(filter.getIsApplyOuOnVelocityReport() == null || filter.getIsApplyOuOnVelocityReport()) {
                prTableConditions.add("(" + prTblQualifier + "id is NULL OR " + prTblQualifier + "state IN (:states" + paramSuffixString + ") )");
            }else {
                prTableConditions.add(prTblQualifier + "state IN (:states" + paramSuffixString + ")");
            }
            params.put("states" + paramSuffixString, filter.getStates());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeStates())) {
            prTableConditions.add(prTblQualifier + "state NOT IN (:exclude_states" + paramSuffixString + ")");
            params.put("exclude_states" + paramSuffixString, filter.getExcludeStates());
        }
        if (CollectionUtils.isNotEmpty(filter.getTitles())) {
            prTableConditions.add(prTblQualifier + "title IN (:titles" + paramSuffixString + ")");
            params.put("titles" + paramSuffixString, filter.getTitles());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTitles())) {
            prTableConditions.add(prTblQualifier + "title NOT IN (:exclude_titles" + paramSuffixString + ")");
            params.put("exclude_titles" + paramSuffixString, filter.getExcludeTitles());
        }
        if (CollectionUtils.isNotEmpty(filter.getAssignees()) || (OrgUnitHelper.doesOUConfigHavePRAssignee(ouConfig))) { // OU: assignees
            var columnName = prTblQualifier + "assignee_ids";
            var columnNameParam = columnName + paramSuffixString;
            if (OrgUnitHelper.doesOUConfigHavePRAssignee(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    prTableConditions.add(MessageFormat.format("{0} && (SELECT ARRAY(SELECT id FROM ({1}) l) g) ", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(filter.getAssignees())) {
                TeamUtils.addUsersCondition(company, prTableConditions, params, prTblQualifier + "assignee_ids", columnNameParam, true,
                        filter.getAssignees(), SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeAssignees())) {
            String columnNameParam = prTblQualifier + "exclude_assignee_ids" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prTableConditions, params, "assignee_ids", columnNameParam, true,
                    filter.getExcludeAssignees(), SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getLabels())) {
            if(filter.getIsApplyOuOnVelocityReport() == null || filter.getIsApplyOuOnVelocityReport()) {
                prTableConditions.add("(" + prTblQualifier + "id is NULL OR " + prTblQualifier + "labels && ARRAY[ :labels" + paramSuffixString + " ]::varchar[] )");
            }else {
                prTableConditions.add(prTblQualifier + "labels && ARRAY[ :labels" + paramSuffixString + " ]::varchar[]");
            }
            params.put("labels" + paramSuffixString, filter.getLabels());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeLabels())) {
            prTableConditions.add("NOT " + prTblQualifier + "labels && ARRAY[ :exclude_labels" + paramSuffixString + " ]::varchar[]");
            params.put("exclude_labels" + paramSuffixString, filter.getExcludeLabels());
        }
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            prTableConditions.add(prTblQualifier + "integration_id IN (:integration_ids" + paramSuffixString + ")");
            prWorkItemTableConditions.add(prTblQualifier + "scm_integration_id IN (:integration_ids" + paramSuffixString + ")");
            prJiraIssueTableConditions.add(prTblQualifier + "scm_integration_id IN (:integration_ids" + paramSuffixString + ")");
            params.put("integration_ids" + paramSuffixString,
                    filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getReviewers()) || (OrgUnitHelper.doesOUConfigHavePRReviewer(ouConfig))) { // OU: reviewers
            var columnName = prTblQualifier + "reviewer_id" + paramSuffixString;
            var columnNameParam = columnName + paramSuffixString;
            if (OrgUnitHelper.doesOUConfigHavePRReviewer(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    prTableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(filter.getReviewers())) {
                TeamUtils.addUsersCondition(company, prTableConditions, params, prTblQualifier + "reviewer_id", columnNameParam, true,
                        filter.getReviewers(), SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeReviewers())) {
            String columnNameParam = prTblQualifier + "exclude_reviewer_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prTableConditions, params, "reviewer_id", columnNameParam, true,
                    filter.getExcludeReviewers(), SCM_APPLICATIONS, true);
        }

        if (MapUtils.isNotEmpty(filter.getPartialMatch())) {
            ScmQueryUtils.createPartialMatchFilter(filter.getPartialMatch(), prTableConditions, PRS_PARTIAL_MATCH_COLUMNS, PRS_PARTIAL_MATCH_ARRAY_COLUMNS, params, prTblQualifier, true);
        }
        if (MapUtils.isNotEmpty(filter.getExcludePartialMatch())) {
            ScmQueryUtils.createPartialMatchFilter(filter.getExcludePartialMatch(), prTableConditions, PRS_PARTIAL_MATCH_COLUMNS, PRS_PARTIAL_MATCH_ARRAY_COLUMNS, params, prTblQualifier, false);
        }
        if (filter.getPrCreatedRange() != null) {
            if (filter.getPrCreatedRange().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "pr_created_at > TO_TIMESTAMP(" + filter.getPrCreatedRange().getLeft() + ")");
            }
            if (filter.getPrCreatedRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "pr_created_at < TO_TIMESTAMP(" + filter.getPrCreatedRange().getRight() + ")");
            }
        }
        if (filter.getPrClosedRange() != null) {
            if (filter.getPrClosedRange().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "pr_closed_at > TO_TIMESTAMP(" + filter.getPrClosedRange().getLeft() + ")");
            }
            if (filter.getPrClosedRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "pr_closed_at < TO_TIMESTAMP(" + filter.getPrClosedRange().getRight() + ")");
            }
        }
        if (filter.getPrUpdatedRange() != null) {
            if (filter.getPrUpdatedRange().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "pr_updated_at > TO_TIMESTAMP(" + filter.getPrUpdatedRange().getLeft() + ")");
            }
            if (filter.getPrUpdatedRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "pr_updated_at < TO_TIMESTAMP(" + filter.getPrUpdatedRange().getRight() + ")");
            }
        }
        if (filter.getReviewerCount() != null) {
            if (filter.getReviewerCount().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "reviewer_count > " + filter.getReviewerCount().getLeft());
            }
            if (filter.getReviewerCount().getRight() != null) {
                prTableConditions.add(prTblQualifier + "reviewer_count < " + filter.getReviewerCount().getRight());
            }
        }
        if (filter.getApproverCount() != null) {
            if (filter.getApproverCount().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "approver_count > " + filter.getApproverCount().getLeft());
            }
            if (filter.getApproverCount().getRight() != null) {
                prTableConditions.add(prTblQualifier + "approver_count < " + filter.getApproverCount().getRight());
            }
        }
        if (filter.getPrMergedRange() != null) {
            if (filter.getPrMergedRange().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "pr_merged_at > TO_TIMESTAMP(" + filter.getPrMergedRange().getLeft() + ")");
            }
            if (filter.getPrMergedRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "pr_merged_at < TO_TIMESTAMP(" + filter.getPrMergedRange().getRight() + ")");
            }
        }
        if (needCommitLocFilter && filter.getLocRange() != null) {
            if (filter.getLocRange().getLeft() != null) {
                prTableConditions.add(prTblQualifier + "loc > " + filter.getLocRange().getLeft());
            }
            if (filter.getLocRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "loc < " + filter.getLocRange().getRight());
            }
        }
        if (needCommitLocFilter && filter.getExcludeLocRange() != null) {
            if (filter.getExcludeLocRange().getLeft() != null && filter.getExcludeLocRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "NOT ( loc > " + filter.getExcludeLocRange().getLeft() + " AND loc < " + filter.getExcludeLocRange().getRight() + ")");
            }
            if (filter.getExcludeLocRange().getLeft() != null && filter.getExcludeLocRange().getRight() == null) {
                prTableConditions.add(prTblQualifier + "loc < " + filter.getExcludeLocRange().getLeft());
            }
            if (filter.getExcludeLocRange().getLeft() == null && filter.getExcludeLocRange().getRight() != null) {
                prTableConditions.add(prTblQualifier + "loc > " + filter.getExcludeLocRange().getRight());
            }
        }
        if (filter.getReviewerIds() != null) {
            reviewTableConditions.add(prTblQualifier + "reviewer_id IN (:reviewer_id" + paramSuffixString + ")");
            params.put("reviewer_id" + paramSuffixString, filter.getReviewerIds().stream().map(UUID::fromString).collect(toList()));
        }

        if(filter.getPrReviewedRange() != null) {
            if (filter.getPrReviewedRange().getLeft() != null) {
                reviewTableConditions.add(prTblQualifier + "reviewed_at > TO_TIMESTAMP(" + filter.getPrReviewedRange().getLeft() + ")");
            }
            if (filter.getPrReviewedRange().getRight() != null) {
                reviewTableConditions.add(prTblQualifier + "reviewed_at < TO_TIMESTAMP(" + filter.getPrReviewedRange().getRight() + ")");
            }

        }
        if (CollectionUtils.isNotEmpty(filter.getIds())) {
            prTableConditions.add(prTblQualifier + "id IN (:ids" + paramSuffixString + ")");
            params.put("ids" + paramSuffixString, filter.getIds());
        }
        return Map.of(PRS_TABLE, prTableConditions,
                REVIEWS_TABLE, reviewTableConditions,
                PULLREQUESTS_JIRA_TABLE, prJiraIssueTableConditions,
                PULLREQUESTS_WORKITEM_TABLE, prWorkItemTableConditions);
    }

    //endregion

    //region PR Union Sql
    public String getUnionSqlForPrsList(String company, ScmPrFilter reqFilter, Map<String, Object> params, OUConfiguration ouConfig) {
        String unionSql = "";
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}", company);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmPrFilter scmPrFilter = scmPrsFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createPrWhereClauseAndUpdateParams(company, params, scmPrFilter, String.valueOf(paramSuffix++), "", ouConfig);
            listOfUnionSqls.add(scmPrsFilterParser.getSqlStmt(company, conditions, scmPrFilter));
        }
        return String.join(" UNION ", listOfUnionSqls);
    }

    public String getUnionSqlForPrsGroupBy(String company, ScmPrFilter reqFilter, Map<String, Object> params,
                                           boolean isAcrossTechnology, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        String unionSql = "";
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        integFiltersMap.values().removeAll(Collections.singleton(null));
        for (Integer integ : integFiltersMap.keySet()) {
            ScmPrFilter scmPrFilter = scmPrsFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createPrWhereClauseAndUpdateParams(company, params, scmPrFilter, String.valueOf(paramSuffix), "", ouConfig);
            listOfUnionSqls.add(scmPrsFilterParser.getSqlStmtForGroupByCount(company, conditions, scmPrFilter,
                    isAcrossTechnology, paramSuffix));
            paramSuffix++;
        }
        return String.join(" UNION ", listOfUnionSqls);
    }

    public String getUnionSqlForPrsDuration(String company, ScmPrFilter reqFilter,
                                            Map<String, Object> params, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        List<String> listOfUnionSqls = new ArrayList<>();
        String unionSql = "";
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmPrFilter scmPrFilter = scmPrsFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createPrWhereClauseAndUpdateParams(company, params, scmPrFilter, String.valueOf(paramSuffix), "", ouConfig);
            listOfUnionSqls.add(scmPrsFilterParser.getSqlStmtForGroupByDuration(company, conditions, scmPrFilter, paramSuffix));
            paramSuffix++;
        }
        return String.join(" UNION ", listOfUnionSqls);
    }
    //endregion

    public DbListResponse<DbAggregationResult> stackedPrsGroupBy(String company,
                                                                 ScmPrFilter filter,
                                                                 List<ScmPrFilter.DISTINCT> stacks,
                                                                 OUConfiguration ouConfig, final boolean valueOnly) throws SQLException {
        return stackedPrsGroupBy(company, filter, stacks, ouConfig, valueOnly, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> stackedPrsGroupBy(String company,
                                                                 ScmPrFilter filter,
                                                                 List<ScmPrFilter.DISTINCT> stacks,
                                                                 OUConfiguration ouConfig,
                                                                 final boolean valueOnly,
                                                                 Integer pageNumber,
                                                                 Integer pageSize) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        DbListResponse<DbAggregationResult> result = groupByAndCalculatePrs(company, filter, valueOnly, ouConfig, pageNumber, pageSize);
        log.info("[{}] Scm Agg: done across '{}' - results={}", company, filter.getAcross(), result.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !stackSupported.contains(stacks.get(0))) {
            return result;
        }
        ScmPrFilter.DISTINCT stack = stacks.get(0);
        ForkJoinPool threadPool = null;
        try {
            log.info("[{}] Scm Agg: started processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                try {
                    log.info("[{}] Scm Agg: --- currently processing stack for '{}' across '{}' - buckets={}, current='{}'", company, stack, filter.getAcross(), result.getCount(), row.getKey());
                    ScmPrFilter newFilter;
                    final ScmPrFilter.ScmPrFilterBuilder newFilterBuilder = filter.toBuilder();
                    OUConfiguration ouConfigForStacks = ouConfig;
                    switch (filter.getAcross()) {
                        case assignee:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).assignees(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "assignees");
                            break;
                        case reviewer:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).reviewers(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "reviewers");
                            break;
                        case approver:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).approvers(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "approvers");
                            break;
                        case creator:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).creators(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "creators");
                            break;
                        case state:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).states(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case project:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).projects(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case label:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).labels(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case branch:
                        case source_branch:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).sourceBranches(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case target_branch:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).targetBranches(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case repo_id:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).repoIds(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case collab_state:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).collabStates(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case comment_density:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).commentDensities(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case code_change:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).codeChanges(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case review_type:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).reviewTypes(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case approval_status:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).approvalStatuses(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case pr_closed:
                        case pr_created:
                        case pr_updated:
                        case pr_merged:
                            newFilter = getFilterForTrendStack(
                                    getFilterWithConfig(newFilterBuilder, filter), row, filter.getAcross(), stack,
                                    MoreObjects.firstNonNull(filter.getAggInterval().toString(), ""))
                                    .build();
                            break;
                        case approver_count:
                        case reviewer_count:
                            newFilter = getFilterWithConfig(newFilterBuilder, filter).across(stack)
                                    .build();
                            break;
                        default:
                            throw new SQLException("This stack is not available for scm queries." + stack);
                    }

                    newFilter = newFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build();
                    List<DbAggregationResult> currentStackResults = groupByAndCalculatePrs(company, newFilter, valueOnly, ouConfigForStacks, pageNumber, pageSize).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            });
            // -- collecting parallel stream with custom pool
            // (note: the toList collector preserves the encountered order)
            threadPool = new ForkJoinPool(DEFAULT_STACK_PARALLELISM);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            log.info("[{}] Scm Agg: done processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            return DbListResponse.of(finalList, finalList.size());
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new SQLException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }
    }

    //region PR Aggs
    public DbListResponse<DbAggregationResult> groupByAndCalculatePrsDuration(String company,
                                                                              ScmPrFilter filter, OUConfiguration ouConfig) throws SQLException {
        return groupByAndCalculatePrsDuration(company, filter, ouConfig, 0, Integer.MAX_VALUE);
    }
    public DbListResponse<DbAggregationResult> groupByAndCalculatePrsDuration(String company,
                                                                              ScmPrFilter filter, OUConfiguration ouConfig,
                                                                              Integer pageNumber, Integer pageSize) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        ScmPrFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = ScmPrFilter.CALCULATION.merge_time;
        }
        Map<String, Object> params = new HashMap<>();
        String prsWhere = "";
        boolean needIssueKeys = false;
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        String filterByProductSQL = "";
        String sortByKey = getPrsDurationSortByKey(filter.getSort(), filter.getAcross().toString());
        Map<String, List<String>> conditions;
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForPrsDuration(company, filter, params, ouConfig);
        }
        conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig);
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            if (prsWhere.equals("")) {
                prsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                prsWhere = prsWhere + " AND  repo_ids IN (:repo_ids) ";
            }
        }
        if (StringUtils.isNotEmpty(filter.getHasIssueKeys())) {
            needIssueKeys = true;
        }
        orderByString = getPrsDurationOrderByString(filter.getSort(), filter.getAcross().toString());
        calculationComponent = "MIN(calc) AS mn,MAX(calc) as mx,COUNT(id) AS ct,PERCENTILE_DISC(0.5) " +
                "WITHIN GROUP(ORDER BY calc) as md,SUM(calc) as sm";
        String intervalColumn;
        boolean sortAscending = true;
        SortingOrder sortOrder = getScmSortOrder(filter.getSort());
        Optional<String> key;
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        switch (calculation) {
            case merge_time:
            case first_review_to_merge_time:
                conditions.get(PRS_TABLE).add("merged");
                break;
            case first_review_time:
                break;
            default:
                throw new SQLException("Invalid calculation field provided for this agg: " + calculation);
        }
        boolean needCommitsTable = checkCommitsTableFiltersJoin(filter, calculation, false);
        boolean needCommentors = checkCommentDensityFiltersJoin(filter, calculation, false);
        boolean needReviewedAt = checkPRReviewedJoin(filter);
        boolean needCreators = checkCreatorsFiltersJoin(filter, ouConfig);
        boolean needReviewers = checkReviewersFiltersJoin(filter, calculation, false);
        boolean needApprovers = checkApproversFiltersJoin(filter, calculation, false);
        boolean needApprovalStatuses = checkApprovalStatus(filter);
        boolean needReviewTypes = checkReviewTypeFilters(filter);

        ScmPrFilter.DISTINCT DISTINCT = filter.getAcross();
        switch (DISTINCT) {
            case pr_merged:
                conditions.get(PRS_TABLE).add("merged");
                AggTimeQueryHelper.AggTimeQuery aggTimeQuery = AggTimeQueryHelper.getAggTimeQuery
                        (DISTINCT + "_at", DISTINCT.toString(), filter.getAggInterval() != null ?
                                filter.getAggInterval().toString() : null, false, sortAscending);
                selectDistinctString = aggTimeQuery.getSelect();
                intervalColumn = aggTimeQuery.getHelperColumn().replaceFirst(",", "") + ",";
                groupByString = aggTimeQuery.getGroupBy();
                if (MapUtils.isEmpty(filter.getSort()) || ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = aggTimeQuery.getOrderBy();
                }
                key = Optional.of(aggTimeQuery.getIntervalKey());
                break;
            case pr_closed:
                conditions.get(PRS_TABLE).add("pr_closed_at IS NOT NULL");
            case pr_created:
            case pr_updated:
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }
                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        (DISTINCT + "_at", DISTINCT.toString(), filter.getAggInterval() != null ?
                                filter.getAggInterval().toString() : null, false, sortAscending);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = ticketModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                groupByString = ticketModAggQuery.getGroupBy();
                if (MapUtils.isEmpty(filter.getSort()) || ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = ticketModAggQuery.getOrderBy();
                }
                key = Optional.of(ticketModAggQuery.getIntervalKey());
                break;
            default:
                throw new SQLException("Invalid Across field provided for this agg: " + filter.getAcross().toString());
        }
        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        String creatorJoin = needCreators ? ScmQueryUtils.sqlForCreatorTableJoin(company) : StringUtils.EMPTY;
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = needCreators ? ScmQueryUtils.CREATORS_SQL : StringUtils.EMPTY;
        String reviewerJoin = needReviewers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER) : StringUtils.EMPTY;
        String approverJoin = needApprovers ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER) : StringUtils.EMPTY;
        String commenterJoin = needCommentors ? ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER) : StringUtils.EMPTY;
        String commitsPRsJoin = needCommitsTable ? ScmQueryUtils.getCommitsPRsJoin(company, params, filter, "", "") : StringUtils.EMPTY;
        String prJiraMappingJoin = needIssueKeys ? ScmQueryUtils.getSqlForPRJiraMappingTable(company) : StringUtils.EMPTY;
        String prWorkItemIdsMappingJoin = needIssueKeys ? ScmQueryUtils.getSqlForPRWorkItemMappingTable(company) : StringUtils.EMPTY;
        String prsReviewedAtJoin = needReviewedAt ? ScmQueryUtils.getSqlForPRReviewedAtJoin(company) : StringUtils.EMPTY;
        String approvalStatusSelect = needApprovalStatuses ? ScmQueryUtils.getApprovalStatusSqlStmt() : StringUtils.EMPTY;
        String codeChangeSql = needCommitsTable ? getCodeChangeSql(filter.getCodeChangeSizeConfig(), true,filter.getCodeChangeUnit()) : StringUtils.EMPTY;
        String fileChangeSql = needCommitsTable ? getFilesChangeSql(filter.getCodeChangeSizeConfig()) : StringUtils.EMPTY;
        String commentDensitySql = needCommentors ? ScmQueryUtils.getCommentDensitySql(filter) : StringUtils.EMPTY;
        String issueKeysSql = needIssueKeys ? ScmQueryUtils.getIssueKeysSql() : StringUtils.EMPTY;
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM ( SELECT " + intervalColumn + " unnest(repo_id) AS repo_ids, " + prsSelect + creatorsSelect
                    + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql + ScmQueryUtils.RESOLUTION_TIME_SQL
                    + (needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY)
                    + (needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY) + (needCommentors ?
                    ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY) + (needCommitsTable ? ScmQueryUtils.COMMITS_PRS_SELECT :
                    StringUtils.EMPTY) + ((needReviewTypes || needReviewers) ? ScmQueryUtils.getReviewType() : StringUtils.EMPTY) +
                    (needIssueKeys ? ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT : StringUtils.EMPTY) +
                    (needReviewers ? ScmQueryUtils.PRS_REVIEWER_COUNT : StringUtils.EMPTY) + (needApprovers ? PRS_APPROVER_COUNT : StringUtils.EMPTY)
                    + " ,EXTRACT(EPOCH FROM (pr_merged_at - pr_created_at)) as calc"
                    + " FROM " + company + "." + PRS_TABLE
                    + creatorJoin + reviewerJoin + approverJoin + commenterJoin + commitsPRsJoin + prJiraMappingJoin + prWorkItemIdsMappingJoin
                    + prsReviewedAtJoin
                    + " ) a" + prsWhere;
            if (calculation == ScmPrFilter.CALCULATION.first_review_time) {
                filterByProductSQL = "SELECT * FROM ( SELECT " + intervalColumn + " unnest(repo_id) AS repo_ids," + prsSelect + creatorsSelect
                        + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql + ScmQueryUtils.RESOLUTION_TIME_SQL
                        + (needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY)
                        + (needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY) + (needCommentors ?
                        ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY)
                        + (needCommitsTable ? ScmQueryUtils.COMMITS_PRS_SELECT :
                        StringUtils.EMPTY) + ((needReviewTypes || needReviewers) ? ScmQueryUtils.getReviewType() : StringUtils.EMPTY) +
                        (needIssueKeys ? ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT : StringUtils.EMPTY) +
                        (needReviewers ? ScmQueryUtils.PRS_REVIEWER_COUNT : StringUtils.EMPTY) + (needApprovers ? PRS_APPROVER_COUNT : StringUtils.EMPTY)
                        + " ,EXTRACT(EPOCH FROM (reviews.pr_reviewed_at - scm_pullrequests.pr_created_at)) as calc FROM  "
                        + company + "." + PRS_TABLE + " INNER JOIN ( SELECT MIN(reviewed_at) as pr_reviewed_at,pr_id FROM "
                        + company + "." + REVIEWS_TABLE + " GROUP BY pr_id ) AS reviews ON scm_pullrequests.id = reviews.pr_id"
                        + creatorJoin + reviewerJoin + approverJoin + commenterJoin + commitsPRsJoin + prJiraMappingJoin + prWorkItemIdsMappingJoin
                        + " ) a" + prsWhere;
            }
            if (calculation == ScmPrFilter.CALCULATION.first_review_to_merge_time) {
                filterByProductSQL = "SELECT * FROM (SELECT " + intervalColumn + " unnest(repo_id) AS repo_ids," + prsSelect + creatorsSelect
                        + approvalStatusSelect + codeChangeSql + fileChangeSql + commentDensitySql + issueKeysSql + ScmQueryUtils.RESOLUTION_TIME_SQL
                        + (needReviewers ? ScmQueryUtils.REVIEW_PRS_SELECT_LIST : StringUtils.EMPTY)
                        + (needApprovers ? ScmQueryUtils.APPROVERS_PRS_LIST : StringUtils.EMPTY)
                        + (needCommentors ? ScmQueryUtils.COMMENTERS_SELECT_SQL : StringUtils.EMPTY)
                        + (needCommitsTable ? ScmQueryUtils.COMMITS_PRS_SELECT : StringUtils.EMPTY) + ((needReviewTypes || needReviewers) ? ScmQueryUtils.getReviewType() : StringUtils.EMPTY) +
                        (needIssueKeys ? ScmQueryUtils.JIRA_WORKITEM_PRS_MAPPING_SELECT :
                                StringUtils.EMPTY) +
                        (needReviewers ? ScmQueryUtils.PRS_REVIEWER_COUNT : StringUtils.EMPTY) + (needApprovers ? PRS_APPROVER_COUNT : StringUtils.EMPTY)
                        + " ,EXTRACT(EPOCH FROM (scm_pullrequests.pr_merged_at - reviews.pr_reviewed_at)) as calc FROM "
                        + company + "." + PRS_TABLE + " INNER JOIN ( SELECT MIN(reviewed_at) as pr_reviewed_at,pr_id FROM "
                        + company + "." + REVIEWS_TABLE + " GROUP BY pr_id ) AS reviews ON scm_pullrequests.id = reviews.pr_id"
                        + creatorJoin + reviewerJoin + approverJoin + commenterJoin + commitsPRsJoin + prJiraMappingJoin + prWorkItemIdsMappingJoin
                        + " ) a" + prsWhere;
            }
        }
        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String intrSql = "SELECT " + selectDistinctString + "," + calculationComponent
                    + " FROM (" + filterByProductSQL + " )x GROUP BY " + groupByString;
            String sql = intrSql + " ORDER BY " + orderByString
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.distinctPrRowDurationMapper(
                    key, filter.getAcross()));
            String countSql = "SELECT COUNT(*) FROM ( " + intrSql + " ) i";
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }
    //endregion

    //insert commitshas and woritemIds.

    //region Commit Insert
    public String insertCommit(String company, DbScmCommit commit) throws SQLException {
        String authorId = userIdentityService.upsertIgnoreEmail(company, DbScmUser.builder()
                .integrationId(commit.getIntegrationId())
                .cloudId(StringUtils.isNotBlank(commit.getAuthorInfo().getCloudId()) ? commit.getAuthorInfo().getCloudId() : commit.getAuthorInfo().getDisplayName())
                .displayName(StringUtils.isNotBlank(commit.getAuthorInfo().getDisplayName()) ? commit.getAuthorInfo().getDisplayName() : commit.getAuthorInfo().getCloudId())
                .originalDisplayName(StringUtils.isNotBlank(commit.getAuthorInfo().getDisplayName()) ? commit.getAuthorInfo().getDisplayName() : commit.getAuthorInfo().getCloudId())
                .build());
        String committerId = userIdentityService.upsertIgnoreEmail(company, DbScmUser.builder()
                .integrationId(commit.getIntegrationId())
                .cloudId(StringUtils.isNotBlank(commit.getCommitterInfo().getCloudId()) ? commit.getCommitterInfo().getCloudId() : commit.getCommitterInfo().getDisplayName())
                .displayName(StringUtils.isNotBlank(commit.getCommitterInfo().getDisplayName()) ? commit.getCommitterInfo().getDisplayName() : commit.getCommitterInfo().getCloudId())
                .originalDisplayName(StringUtils.isNotBlank(commit.getCommitterInfo().getDisplayName()) ? commit.getCommitterInfo().getDisplayName() : commit.getCommitterInfo().getCloudId())
                .build());

        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            //insert new issue
            String commitSql = "INSERT INTO " + company + "." + COMMITS_TABLE +
                    " (repo_id,project,integration_id,author,author_id,committer,committer_id,commit_sha,commit_url,message,files_ct," +
                    "additions,deletions,changes,file_types,commit_branch,direct_merge,committed_at,commit_pushed_at" +
                    ",ingested_at,vcs_type) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) " +
                    "ON CONFLICT (commit_sha,repo_id,project,integration_id) DO NOTHING";

            String commitJiraSql = "INSERT INTO " + company + "." + COMMIT_JIRA_TABLE +
                    " (commit_sha,scm_integ_id,issue_key) VALUES(?,?,?) ON CONFLICT " +
                    "(commit_sha,scm_integ_id,issue_key) DO NOTHING";

            String commitWorkitemSql = "INSERT INTO " + company + "." + COMMIT_WORKITEM_TABLE +
                    " (scm_integration_id,commit_sha,workitem_id) VALUES(?,?,?) " +
                    " ON CONFLICT (scm_integration_id,commit_sha,workitem_id) DO NOTHING";

            try (PreparedStatement insertCommit = conn.prepareStatement(
                    commitSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement insertJiraMapping = conn.prepareStatement(
                         commitJiraSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement insertWorkitemMapping = conn.prepareStatement(
                         commitWorkitemSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                List<String> repoIds = new ArrayList<>(commit.getRepoIds());
                Collections.sort(repoIds);
                List<String> fileTypes;
                fileTypes = commit.getFileTypes() != null ? commit.getFileTypes() : List.of();
                insertCommit.setObject(i++, conn.createArrayOf("varchar", repoIds.toArray()));
                insertCommit.setObject(i++, commit.getProject());
                insertCommit.setObject(i++, NumberUtils.toInt(commit.getIntegrationId()));
                insertCommit.setObject(i++, commit.getAuthor());
                insertCommit.setObject(i++, UUID.fromString(authorId));
                insertCommit.setObject(i++, commit.getCommitter());
                insertCommit.setObject(i++, UUID.fromString(committerId));
                insertCommit.setObject(i++, commit.getCommitSha());
                insertCommit.setObject(i++, commit.getCommitUrl());
                insertCommit.setObject(i++, commit.getMessage());
                insertCommit.setObject(i++, commit.getFilesCt());
                insertCommit.setObject(i++, commit.getAdditions());
                insertCommit.setObject(i++, commit.getDeletions());
                insertCommit.setObject(i++, commit.getChanges());
                insertCommit.setObject(i++, conn.createArrayOf("varchar", fileTypes.toArray()));
                insertCommit.setObject(i++, commit.getBranch());
                insertCommit.setObject(i++, commit.getDirectMerge() != null ? commit.getDirectMerge() : false);
                insertCommit.setObject(i++, LocalDateTime.ofEpochSecond(
                        commit.getCommittedAt(), 0, ZoneOffset.UTC));
                insertCommit.setObject(i++, commit.getCommitPushedAt() != null ? LocalDateTime.ofEpochSecond(
                        commit.getCommitPushedAt(), 0, ZoneOffset.UTC) : null);
                insertCommit.setObject(i++, commit.getIngestedAt());
                insertCommit.setString(i, commit.getVcsType().toString());

                int insertedRows = insertCommit.executeUpdate();

                if (CollectionUtils.isNotEmpty(commit.getIssueKeys())) {
                    for (String issueKey : commit.getIssueKeys()) {
                        insertJiraMapping.setObject(1, commit.getCommitSha());
                        insertJiraMapping.setObject(2, NumberUtils.toInt(commit.getIntegrationId()));
                        insertJiraMapping.setObject(3, issueKey.toUpperCase());
                        insertJiraMapping.addBatch();
                        insertJiraMapping.clearParameters();
                    }
                    insertJiraMapping.executeBatch();
                }
                if (CollectionUtils.isNotEmpty(commit.getWorkitemIds())) {
                    for (String workitemId : commit.getWorkitemIds()) {
                        insertWorkitemMapping.setObject(1, NumberUtils.toInt(commit.getIntegrationId()));
                        insertWorkitemMapping.setObject(2, commit.getCommitSha());
                        insertWorkitemMapping.setObject(3, workitemId);
                        insertWorkitemMapping.addBatch();
                        insertWorkitemMapping.clearParameters();
                    }
                    insertWorkitemMapping.executeBatch();
                }

                if (insertedRows == 0) {
                    log.debug("commit insert attempt failed. the commit: {}", commit.toString());
                    return null;
                }
                String insertedRowId = null;
                try (ResultSet rs = insertCommit.getGeneratedKeys()) {
                    if (rs.next()) {
                        insertedRowId = rs.getString(1);
                    }
                }
                if (insertedRowId == null) {
                    throw new SQLException("Failed to get inserted rowid.");
                }
                return insertedRowId;
            }
        }));
    }
    //endregion

    //region Commit Update Changes Count
    public Boolean updateCommitChangesCount(String company, UUID id, int changes) throws SQLException {
        String sql = String.format(UPDATE_COMMIT_CHANGES_SQL_FORMAT, company);
        Map<String, Object> params = Map.of("id", id, "changes", changes);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update changes for company %s, id %s", company, id));
        }
        return true;
    }
    //endregion

    //region Commit Update Commit users ids
    public Boolean updateCommitUsersId(String company, UUID id, UUID committerId, UUID authorId) throws SQLException {
        String sql = String.format(UPDATE_COMMIT_USERS_ID_SQL_FORMAT, company);
        Map<String, Object> params = Map.of("committer_id", committerId, "author_id", authorId, "id", id);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update commit users for company %s, id %s", company, id));
        }
        return true;
    }
    //endregion

    // LEV-4751: Need to update the change volume stats column...
    //ToDo: Need to remove after some time...
    public Boolean updateCommitChangeVolumeStats(String company, UUID id, int additions, int deletions, int changes) throws SQLException {
        String sql = String.format(UPDATE_COMMIT_CHANGE_VOLUME_SQL_FORMAT, company);
        Map<String, Object> params = Map.of("id", id, "additions", additions, "deletions", deletions, "changes", changes);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update change volume stats for company %s, id %s", company, id));
        }
        return true;
    }

    public Boolean updateCommitIngestedAt(String company, UUID id, Long ingestedAt) throws SQLException {
        String sql = String.format(UPDATE_COMMIT_INGESTED_AT_SQL_FORMAT, company);
        Map<String, Object> params = Map.of("id", id, "ingested_at", ingestedAt);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update ingested_at for company %s, id %s", company, id));
        }
        return true;
    }
    //endregion

    //region Commit Update Files Count
    //LEV-4196: Need to update the files_ct column...
    //ToDo: Need to remove after some time...
    public Boolean updateCommitFilesCount(String company, UUID id, int filesCount) throws SQLException {
        String updateFilesCountSql = "UPDATE %s." + COMMITS_TABLE + " SET files_ct = :files_ct WHERE id = :id";
        String sql = String.format(updateFilesCountSql, company);
        Map<String, Object> params = Map.of("id", id, "files_ct", filesCount);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update files_ct for company %s, id %s", company, id));
        }
        return true;
    }
    //endregion

    //region Commit Get
    public Optional<DbScmCommit> getCommit(String company, String commitSha, String repoId, String integrationId) {
        Validate.notBlank(commitSha, "Missing commitSha.");
        Validate.notBlank(repoId, "Missing repo_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + COMMITS_TABLE
                + " WHERE commit_sha = :commit_sha AND repo_id && ARRAY[ :repo_id ]::varchar[] AND integration_id = :integid";
        Map<String, Object> params = Map.of("commit_sha", commitSha,
                "repo_id", repoId,
                "integid", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmCommit> data = template.query(sql, params, DbScmConverters.commitRowMapper());
        return data.stream().findFirst();
    }

    //region check for direct merge commit
    public Optional<DbScmPullRequest> checkCommitForDirectMerge(String company, String commitSha, String repoId, String integrationId) {
        Validate.notBlank(commitSha, "Missing commitSha.");
        Validate.notBlank(repoId, "Missing repo_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * , scm_pullrequests.metadata->>'pr_link' as pr_link FROM " + company + "." + PRS_TABLE
                + " WHERE commit_shas && ARRAY[ :commit_shas ]::varchar[] AND repo_id && ARRAY[ :repo_id ]::varchar[] AND integration_id = :integid";
        Map<String, Object> params = Map.of("commit_shas", commitSha,
                "repo_id", repoId,
                "integid", NumberUtils.toInt(integrationId));
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmPullRequest> data = template.query(sql, params, DbScmConverters.prRowMapper());
        return data.stream().findFirst();
    }

    public boolean updateDirectMergeForCommit(String company, UUID id, Boolean directMerge) throws SQLException {
        String sql = String.format(UPDATE_COMMIT_DIRECT_MERGE_SQL_FORMAT, company);
        Map<String, Object> params = Map.of("id", id, "direct_merge", directMerge);
        log.debug("sql = " + sql);
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update direct_merge for company %s, id %s", company, id));
        }
        return true;
    }

    public boolean updateCommitBranchForCommit(String company, UUID id, String branch) throws SQLException {
        String sql = String.format(UPDATE_COMMIT_BRANCH_SQL, company);
        Map<String, Object> params = Map.of("id", id, "commit_branch", branch);
        log.debug("sql = " + sql);
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update commit_branch for company %s, id %s", company, id));
        }
        return true;
    }

    public Optional<DbScmCommit> getCommit(String company, String commitSha, List<String> repoIds, String integrationId) {
        Validate.notBlank(commitSha, "Missing commitSha.");
        Validate.notEmpty(repoIds, "Missing repo_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        List<DbScmCommit> data = template.query(
                "SELECT * FROM " + company + "." + COMMITS_TABLE
                        + " WHERE commit_sha = :commit_sha AND repo_id && ARRAY[ :repo_id ]::varchar[] AND integration_id = :integid",
                Map.of("commit_sha", commitSha,
                        "repo_id", repoIds,
                        "integid", NumberUtils.toInt(integrationId)),
                DbScmConverters.commitRowMapper());
        return data.stream().findFirst();
    }
    //endregion

    //Commit Workitems Get
    List<String> getCommitWorkitems(String company, List<String> commitShas, String integrationId) {
        Validate.notNull(commitShas, "Missing commitShas.");
        Validate.notBlank(integrationId, "Missing scm_integration_id.");
        String sql = "SELECT workitem_id FROM " + company + "." + COMMIT_WORKITEM_TABLE
                + " WHERE commit_sha IN (:commit_shas) AND scm_integration_id = :scm_integration_ids";
        Map<String, Object> params = Map.of("commit_shas", commitShas,
                "scm_integration_ids", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<String> scmCommitWorkitems = template.query(sql, params, DbScmConverters.getWorkitemRowMapper());
        return scmCommitWorkitems;
    }

    //PR Workitems Get
    List<String> getPrWorkitems(String company, List<String> prIds, String integrationId) {
        Validate.notNull(prIds, "Missing prIds.");
        Validate.notBlank(integrationId, "Missing scm_integration_id.");
        String sql = "SELECT workitem_id FROM " + company + "." + PULLREQUESTS_WORKITEM_TABLE
                + " WHERE pr_id IN (:pr_ids) AND scm_integration_id = :scm_integration_ids";
        Map<String, Object> params = Map.of("pr_ids", prIds,
                "scm_integration_ids", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<String> scmPrWorkitems = template.query(sql, params, DbScmConverters.getWorkitemRowMapper());
        return scmPrWorkitems;
    }

    //region Commit List
    public List<DbScmCommit> getCommits(String company, List<String> commitShas, String integrationId) {
        Validate.notNull(commitShas, "Missing commitShas.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + COMMITS_TABLE
                + " WHERE commit_sha IN (:commit_shas) AND integration_id = :integid";
        Map<String, Object> params = Map.of("commit_shas", commitShas,
                "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmCommit> scmCommits = template.query(sql, params, DbScmConverters.commitRowMapper());
        return scmCommits;
    }

    public DbListResponse<DbScmCommit> listCommits(String company,
                                                   ScmCommitFilter filter,
                                                   Map<String, SortingOrder> sortBy,
                                                   OUConfiguration ouConfig,
                                                   Integer pageNumber,
                                                   Integer pageSize) {
        String filterByProductSQL = "";
        String commitsWhere = "";
        String integIdCondition = "";
        Map<String, List<String>> conditions;
        Map<String, Object> params = new HashMap<>();
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForCommits(company, filter, params, true, ouConfig);
        }
        conditions = createCommitsWhereClauseAndUpdateParams(company, params, filter.getCommitShas(),
                filter.getRepoIds(), filter.getVcsTypes(), filter.getProjects(), filter.getCommitters(), filter.getAuthors(), filter.getDaysOfWeek(),
                filter.getIntegrationIds(), filter.getCommitBranches(), filter.getExcludeCommitShas(), filter.getExcludeRepoIds(),
                filter.getExcludeProjects(), filter.getExcludeCommitters(), filter.getExcludeCommitBranches(), filter.getExcludeAuthors(), filter.getExcludeDaysOfWeek(),
                filter.getCommittedAtRange(), filter.getLocRange(), filter.getExcludeLocRange(), filter.getPartialMatch(), filter.getExcludePartialMatch(), filter.getFileTypes(), filter.getExcludeFileTypes(), filter.getCodeChangeSize(), filter.getCodeChanges(),
                filter.getCodeChangeUnit(), filter.getTechnologies(), filter.getExcludeTechnologies(),
                null, "", true, ouConfig, filter.getIds(), filter.getIsApplyOuOnVelocityReport(), filter.getCreatedAtRange());
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT;
        if (CollectionUtils.isNotEmpty(filter.getDaysOfWeek()) || CollectionUtils.isNotEmpty(filter.getExcludeDaysOfWeek())) {
            String dayColumn = " , rtrim(to_char(date (scm_commits.committed_at), 'Day'))::VARCHAR AS day ";
            commitsSelect += dayColumn;
        }
        if (conditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        }
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (COMMIT_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "committed_at";
                })
                .orElse("committed_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        boolean hasTechFilters = ScmQueryUtils.checkTechnologiesFilter(filter);
        String technologyJoinSql = hasTechFilters ? ScmQueryUtils.getTechnologyJoinSqlStmt(company)
                : StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            List<Integer> integsList = filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList());
            integIdCondition = " AND scm_files.integration_id IN (" + StringUtils.join(integsList, ",") + ")";
        }

        params.put("metric_previous_committed_at", filter.getLegacyCodeConfig() != null ? filter.getLegacyCodeConfig() :
                Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());

        String fileTableJoinStmt = "";
        String fileTableJoinSelect = "";
        String codeChangeSelect = "";
        String issuesTableJoin = "";
        String issuesTableSelect = "";

        if (filter.getReturnHasIssueKeys() != null && filter.getReturnHasIssueKeys()) {
            issuesTableJoin = " LEFT JOIN (\n" +
                    " SELECT\n" +
                    " commit_sha as issue_commit_sha,\n" +
                    " scm_integ_id as issue_integration_id\n" +
                    " FROM " + company + "."+ COMMIT_JIRA_TABLE +
                    " GROUP BY commit_sha,scm_integ_id "+
                    " ) AS issues ON scm_commits.integration_id = issues.issue_integration_id\n" +
                    " AND scm_commits.commit_sha = issues.issue_commit_sha\n";
            issuesTableSelect = ",CASE WHEN issue_commit_sha IS NULL THEN false ELSE true END has_issue_keys ";
        }

        if (filter.getIgnoreFilesJoin() == null || !filter.getIgnoreFilesJoin()) {
            fileTableJoinStmt = getScmCommitMetricsQuery(company, integIdCondition);
            fileTableJoinSelect = " ,tot_addition,tot_deletion,tot_change,pct_legacy_refactored_lines,pct_refactored_lines,pct_new_lines,total_legacy_code_lines,total_refactored_code_lines ";
            codeChangeSelect = getCodeChangeSql(filter.getCodeChangeSizeConfig(), false,filter.getCodeChangeUnit()) + getFilesChangeSql(filter.getCodeChangeSizeConfig());
        }

        if (StringUtils.isEmpty(codeChangeSelect) && CollectionUtils.isNotEmpty(filter.getCodeChanges())) {
            codeChangeSelect = getCodeChangeSql(filter.getCodeChangeSizeConfig(), false,filter.getCodeChangeUnit()) + getFilesChangeSql(filter.getCodeChangeSizeConfig());
        }

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = " SELECT * FROM ( SELECT " + commitsSelect +
                    issuesTableSelect +
                    ",committer, committer_id::varchar as committer_id" +
                    ",author, author_id::varchar as author_id" +
                    fileTableJoinSelect + (hasTechFilters ? " , technology " : StringUtils.EMPTY) + codeChangeSelect +
                    " FROM " + company + "." + COMMITS_TABLE +
                    fileTableJoinStmt +
                    technologyJoinSql +
                    issuesTableJoin +
                    " ) fin " +
                    commitsWhere;
        }
        List<DbScmCommit> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM ( " + filterByProductSQL + ")x ORDER BY " + sortByKey + " " + sortOrder.toString() + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.commitRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM (" + filterByProductSQL + ") a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    //region LatestUpdatedScmCommit
    private static final String SELECT_LATEST_CREATED_AT_SCM_COMMIT = "SELECT * FROM %s.scm_commits ORDER BY created_at DESC LIMIT 1";
    public Optional<DbScmCommit> getLatestCreatedAtScmCommit (final String company) {
        String sql = String.format(SELECT_LATEST_CREATED_AT_SCM_COMMIT, company);
        List<DbScmCommit> scmCommits = template.query(sql, Map.of(), DbScmConverters.commitRowMapper());
        if(CollectionUtils.isEmpty(scmCommits)) {
            return Optional.empty();
        }
        return Optional.ofNullable(scmCommits.get(0));
    }
    //endregion

    public List<String> findCommitShasMatchingPartialShas(final String company, final Integer integrationId, final String partialCommitSha) {
        Validate.notBlank(company, "Missing company.");
        Validate.notNull(integrationId, "Missing integrationId.");
        Validate.notBlank(partialCommitSha, "Missing partialCommitSha.");

        String sql = "SELECT commit_sha FROM " + company + "." + COMMITS_TABLE
                + " WHERE integration_id = :integration_id AND commit_sha LIKE :partial_commit_sha";
        Map<String, Object> params = Map.of("integration_id", integrationId,
                "partial_commit_sha", partialCommitSha + "%");
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<String> commitShas = template.query(sql, params, StringQueryConverter.stringMapper("commit_sha"));
        return commitShas;
    }

    /**
     * This function has been restricted to bitbucket on purpose
     *
     * @param company
     * @param integrationId
     * @param pageSize
     * @return
     */
    public List<String> findPartialShasInBitbucketPRs(final String company, final Integer integrationId, final Integer pageSize) {
        Validate.notBlank(company, "Missing company.");
        Validate.notNull(integrationId, "Missing integrationId.");
        Validate.notNull(pageSize, "Missing page size.");
        String sql = "SELECT DISTINCT(pr.merge_sha) as partial_sha \n" +
                "FROM " + company + "." + PRS_TABLE + " as pr \n" +
                "JOIN " + company + ".integrations as i on pr.integration_id = i.id \n" +
                "WHERE i.application = 'bitbucket' AND i.id = :integration_id AND pr.merge_sha is not null and char_length(pr.merge_sha) <=12\n" +
                "ORDER by partial_sha \n" +
                "limit :limit";
        Map<String, Object> params = Map.of("integration_id", integrationId, "limit", pageSize);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<String> commitShas = template.query(sql, params, StringQueryConverter.stringMapper("partial_sha"));
        return commitShas;
    }

    /**
     * This function has been restricted to bitbucket on purpose
     *
     * @param company
     * @param integrationId
     * @param partialSha
     * @param completeSha
     * @return
     */
    public int updatePartialShasInBitbucketPRs(final String company, final Integer integrationId, final String partialSha, final String completeSha) {
        Validate.notBlank(company, "Missing company.");
        Validate.notNull(integrationId, "Missing integrationId.");
        Validate.notBlank(partialSha, "Missing partialSha.");
        Validate.notBlank(completeSha, "Missing completeSha.");

        String sql = "UPDATE " + company + "." + PRS_TABLE + " SET merge_sha = :merge_sha, commit_shas = :commit_shas WHERE merge_sha = :partial_merge_sha AND integration_id = :integration_id AND integration_id IN (SELECT id FROM " + company + ".integrations WHERE application = 'bitbucket')";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("partial_merge_sha", partialSha);
        params.addValue("integration_id", integrationId);
        params.addValue("merge_sha", completeSha);
        params.addValue("commit_shas", List.of(completeSha).toArray(new String[0]));

        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int affectedRows = template.update(sql, params);
        log.debug("updatePartialShasInBitbucketPRs, company = {}, integrationId = {}, partialSha = {}, completeSha = {}, affectedRows = {}", company, integrationId, partialSha, completeSha, affectedRows);
        return affectedRows;
    }
    //endregion

    //region Commit Create Where Clause and Params
    public Map<String, List<String>> createCommitsWhereClauseAndUpdateParams(String company, Map<String, Object> params, ScmCommitFilter scmCommitFilter, String paramSuffix, String commitTblQualifier, boolean isFileTypeList, OUConfiguration ouConfig) {
        return createCommitsWhereClauseAndUpdateParams(company, params,
                scmCommitFilter.getCommitShas(), scmCommitFilter.getRepoIds(), scmCommitFilter.getVcsTypes(), scmCommitFilter.getProjects(),
                scmCommitFilter.getCommitters(), scmCommitFilter.getAuthors(), scmCommitFilter.getDaysOfWeek(), scmCommitFilter.getIntegrationIds(),
                scmCommitFilter.getCommitBranches(), scmCommitFilter.getExcludeCommitShas(), scmCommitFilter.getExcludeRepoIds(), scmCommitFilter.getExcludeProjects(),
                scmCommitFilter.getExcludeCommitters(), scmCommitFilter.getExcludeCommitBranches(), scmCommitFilter.getExcludeAuthors(), scmCommitFilter.getExcludeDaysOfWeek(), scmCommitFilter.getCommittedAtRange(),
                scmCommitFilter.getLocRange(), scmCommitFilter.getExcludeLocRange(), scmCommitFilter.getPartialMatch(), scmCommitFilter.getExcludePartialMatch(), scmCommitFilter.getFileTypes(), scmCommitFilter.getExcludeFileTypes(),
                scmCommitFilter.getCodeChangeSize(), scmCommitFilter.getCodeChanges(), scmCommitFilter.getCodeChangeUnit(), scmCommitFilter.getTechnologies(), scmCommitFilter.getExcludeTechnologies(), paramSuffix, commitTblQualifier, isFileTypeList, ouConfig, scmCommitFilter.getIds(), scmCommitFilter.getIsApplyOuOnVelocityReport(),
                scmCommitFilter.getCreatedAtRange());
    }

    /**
     * @deprecated Use {@link ScmAggService#createCommitsWhereClauseAndUpdateParams(java.lang.String, java.util.Map, io.levelops.commons.databases.models.filters.ScmCommitFilter, java.lang.String, java.lang.String, boolean, io.levelops.commons.databases.models.organization.OUConfiguration)}
     */
    @Deprecated
    protected Map<String, List<String>> createCommitsWhereClauseAndUpdateParams(String company, Map<String, Object> params,
                                                                                List<String> commitShas,
                                                                                List<String> repoIds,
                                                                                List<VCS_TYPE> vcsTypes,
                                                                                List<String> projects,
                                                                                List<String> committers,
                                                                                List<String> authors,
                                                                                List<String> daysOfWeek,
                                                                                List<String> integrationIds,
                                                                                List<String> commitBranches,
                                                                                List<String> excludeCommitShas,
                                                                                List<String> excludeRepoIds,
                                                                                List<String> excludeProjects,
                                                                                List<String> excludeCommitters,
                                                                                List<String> excludeCommitBranches,
                                                                                List<String> excludeAuthors,
                                                                                List<String> excludeDaysOfWeek,
                                                                                ImmutablePair<Long, Long> committedAtRange,
                                                                                ImmutablePair<Long, Long> locRange,
                                                                                ImmutablePair<Long, Long> excludeLocRange,
                                                                                Map<String, Map<String, String>> partialMatch,
                                                                                Map<String, Map<String, String>> excludePartialMatch,
                                                                                List<String> fileTypes,
                                                                                List<String> excludeFileTypes,
                                                                                Map<String, String> codeChangeSize,
                                                                                List<String> codeChanges,
                                                                                String codeChangeUnit,
                                                                                List<String> technologies,
                                                                                List<String> excludeTechnologies,
                                                                                String paramSuffix,
                                                                                String commitTblQualifier,
                                                                                boolean isFileTypeList,
                                                                                OUConfiguration ouConfig,
                                                                                List<UUID> ids,
                                                                                Boolean isApplyOuOnVelocityReport,
                                                                                ImmutablePair<Long, Long> createdAtRange) {
        List<String> commitsTableConditions = new ArrayList<>();
        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        if (CollectionUtils.isNotEmpty(repoIds)) {
            if(isApplyOuOnVelocityReport == null || isApplyOuOnVelocityReport ){
                commitsTableConditions.add("(" + commitTblQualifier + "id is NULL OR " + commitTblQualifier + "repo_id && ARRAY[:repo_ids" + paramSuffixString + " ]::varchar[] )");
            }else {
                commitsTableConditions.add(commitTblQualifier + "repo_id && ARRAY[:repo_ids" + paramSuffixString + " ]::varchar[]");
            }
            params.put("repo_ids" + paramSuffixString, repoIds);
        }
        if (CollectionUtils.isNotEmpty(vcsTypes)) {
            commitsTableConditions.add(commitTblQualifier + "vcs_type IN (:vcs_types" + paramSuffixString + ")");
            params.put("vcs_types" + paramSuffixString, vcsTypes.stream()
                    .map(Enum::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(codeChanges)) {
            String column = "lines".equals(codeChangeUnit) ? "code_change" : "files_changed";
            commitsTableConditions.add(commitTblQualifier + column + " IN (:" + column + paramSuffixString + ")");
            params.put(column + paramSuffixString, codeChanges);
        }
        if (MapUtils.isNotEmpty(codeChangeSize)) {
            String column = "lines".equals(codeChangeUnit) ? "lines_changed" : "files_ct";
            if (codeChangeSize.get("$gt") != null) {
                commitsTableConditions.add(commitTblQualifier + column + " > :gt_" + column + paramSuffixString);
                params.put("gt_" + column + paramSuffixString, Integer.valueOf(codeChangeSize.get("$gt")));
            }
            if (codeChangeSize.get("$lt") != null) {
                commitsTableConditions.add(commitTblQualifier + column + " < :lt_" + column + paramSuffixString);
                params.put("lt_" + column + paramSuffixString, Integer.valueOf(codeChangeSize.get("$lt")));
            }
        }
        if (CollectionUtils.isNotEmpty(fileTypes)) {
            if (isFileTypeList) {
                commitsTableConditions.add(commitTblQualifier + "file_types && ARRAY[:file_types" + paramSuffixString + " ]::varchar[]");
            } else {
                commitsTableConditions.add(commitTblQualifier + "file_type IN (:file_types" + paramSuffixString + ")");
            }
            params.put("file_types" + paramSuffixString, fileTypes);

        }
        if (CollectionUtils.isNotEmpty(excludeFileTypes)) {
            if (isFileTypeList) {
                commitsTableConditions.add("NOT " + commitTblQualifier + "file_types && ARRAY[:exclude_file_types" + paramSuffixString + " ]::varchar[]");
            } else {
                commitsTableConditions.add(commitTblQualifier + "file_type NOT IN (:exclude_file_types" + paramSuffixString + ")");
            }
            params.put("exclude_file_types" + paramSuffixString, excludeFileTypes);
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            commitsTableConditions.add("NOT " + commitTblQualifier + "repo_id && ARRAY[:exclude_repo_ids" + paramSuffixString + " ]::varchar[]");
            params.put("exclude_repo_ids" + paramSuffixString, excludeRepoIds);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
                if(isApplyOuOnVelocityReport == null || isApplyOuOnVelocityReport ){
                    commitsTableConditions.add("(" + commitTblQualifier + "id is NULL OR " + commitTblQualifier + "project IN (:projects" + paramSuffixString + ") )");
                }else {
                    commitsTableConditions.add(commitTblQualifier + "project IN (:projects" + paramSuffixString + ")");
                }
            params.put("projects" + paramSuffixString, projects);
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            commitsTableConditions.add(commitTblQualifier + "project NOT IN (:exclude_projects" + paramSuffixString + ")");
            params.put("exclude_projects" + paramSuffixString, excludeProjects);
        }
        if (CollectionUtils.isNotEmpty(technologies)) {
            commitsTableConditions.add(commitTblQualifier + "technology  IN (:technologies" + paramSuffixString + ")");
            params.put("technologies" + paramSuffixString, technologies);
        }
        if (CollectionUtils.isNotEmpty(excludeTechnologies)) {
            commitsTableConditions.add(commitTblQualifier + "technology NOT IN (:exclude_technologies" + paramSuffixString + ")");
            params.put("exclude_technologies" + paramSuffixString, excludeTechnologies);
        }
        if (CollectionUtils.isNotEmpty(commitShas)) {
            commitsTableConditions.add(commitTblQualifier + "commit_sha IN (:commit_shas" + paramSuffixString + ")");
            params.put("commit_shas" + paramSuffixString, commitShas);
        }
        if (CollectionUtils.isNotEmpty(excludeCommitShas)) {
            commitsTableConditions.add(commitTblQualifier + "commit_sha NOT IN (:exclude_commit_shas" + paramSuffixString + ")");
            params.put("exclude_commit_shas" + paramSuffixString, excludeCommitShas);
        }
        if (CollectionUtils.isNotEmpty(committers) || OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig)) { // OU: committers
            String columnName = commitTblQualifier + "committer_id";
            String columnNameParam = columnName + paramSuffixString;
            if (OrgUnitHelper.doesOuConfigHaveCommitCommitters(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    commitsTableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(committers)) {
                TeamUtils.addUsersCondition(company, commitsTableConditions, params, columnName, columnNameParam,
                        false, committers, SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(excludeCommitters)) {
            String columnNameParam = commitTblQualifier + "exclude_committer_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, commitsTableConditions, params, commitTblQualifier + "committer_id", columnNameParam,
                    false, excludeCommitters, SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(excludeCommitBranches)) {
            String columnNameParam = commitTblQualifier + "exclude_commit_branch" + paramSuffixString;
            TeamUtils.addUsersCondition(company, commitsTableConditions, params, commitTblQualifier + "commit_branch", columnNameParam,
                    false, excludeCommitBranches, SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(authors) || OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig)) { // OU: authors
            String columnName = commitTblQualifier + "author_id";
            String columnNameParam = columnName + paramSuffixString;
            if (OrgUnitHelper.doesOuConfigHaveCommitAuthors(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    commitsTableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(authors)) {
                TeamUtils.addUsersCondition(company, commitsTableConditions, params, commitTblQualifier + "author_id", columnNameParam,
                        false, authors, SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(excludeAuthors)) {
            String columnNameParam = commitTblQualifier + "exclude_author_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, commitsTableConditions, params, commitTblQualifier + "author_id", columnNameParam,
                    false, excludeAuthors, SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(daysOfWeek)) {
            commitsTableConditions.add(commitTblQualifier + "day IN (:days" + paramSuffixString + ")");
            params.put("days" + paramSuffixString, daysOfWeek);
        }
        if (CollectionUtils.isNotEmpty(excludeDaysOfWeek)) {
            commitsTableConditions.add(commitTblQualifier + "day NOT IN (:exclude_days" + paramSuffixString + ")");
            params.put("exclude_days" + paramSuffixString, excludeDaysOfWeek);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            commitsTableConditions.add(commitTblQualifier + "integration_id IN (:integration_ids" + paramSuffixString + ")");
            params.put("integration_ids" + paramSuffixString,
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(commitBranches)) {
            commitsTableConditions.add(commitTblQualifier + "commit_branch IN (:commit_branches" + paramSuffixString + ")");
            params.put("commit_branches" + paramSuffixString,
                    commitBranches.stream().filter(StringUtils::isNotBlank).collect(Collectors.toList()));
        }
        if (committedAtRange != null) {
            if (committedAtRange.getLeft() != null) {
                commitsTableConditions.add(commitTblQualifier + "committed_at > TO_TIMESTAMP(" + committedAtRange.getLeft() + ")");
            }
            if (committedAtRange.getRight() != null) {
                commitsTableConditions.add(commitTblQualifier + "committed_at < TO_TIMESTAMP(" + committedAtRange.getRight() + ")");
            }
        }
        if (createdAtRange != null) {
            if (createdAtRange.getLeft() != null) {
                commitsTableConditions.add(commitTblQualifier + "created_at > " + createdAtRange.getLeft() );
            }
            if (createdAtRange.getRight() != null) {
                commitsTableConditions.add(commitTblQualifier + "created_at < " + createdAtRange.getRight() );
            }
        }
        if (locRange != null) {
            if (locRange.getLeft() != null) {
                commitsTableConditions.add(commitTblQualifier + "loc > " + locRange.getLeft());
            }
            if (locRange.getRight() != null) {
                commitsTableConditions.add(commitTblQualifier + "loc < " + locRange.getRight());
            }
        }
        if (excludeLocRange != null) {
            if (excludeLocRange.getLeft() != null && excludeLocRange.getRight() != null) {
                commitsTableConditions.add(commitTblQualifier + "NOT ( loc  > " + excludeLocRange.getLeft() + " AND loc < " + excludeLocRange.getRight() + ")");
            }
            if (excludeLocRange.getLeft() != null && excludeLocRange.getRight() == null) {
                commitsTableConditions.add(commitTblQualifier + "loc < " + excludeLocRange.getLeft());
            }
            if (excludeLocRange.getLeft() == null && excludeLocRange.getRight() != null) {
                commitsTableConditions.add(commitTblQualifier + "loc > " + excludeLocRange.getRight());
            }
        }
        if (CollectionUtils.isNotEmpty(ids)) {
            commitsTableConditions.add(commitTblQualifier + "id IN (:ids" + paramSuffixString + ")");
            params.put("ids" + paramSuffixString, ids);
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            ScmQueryUtils.createPartialMatchFilter(partialMatch, commitsTableConditions, COMMITS_PARTIAL_MATCH_COLUMNS, COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS, params, commitTblQualifier, true);
        }
        if (MapUtils.isNotEmpty(excludePartialMatch)) {
            ScmQueryUtils.createPartialMatchFilter(excludePartialMatch, commitsTableConditions, COMMITS_PARTIAL_MATCH_COLUMNS, COMMITS_PARTIAL_MATCH_ARRAY_COLUMNS, params, commitTblQualifier, false);
        }
        return Map.of(COMMITS_TABLE, commitsTableConditions);
    }
    //endregion

    //region Commit Union Sql
    public String getUnionSqlForCommits(String company, ScmCommitFilter reqFilter, Map<String, Object> params,
                                        boolean isListQuery, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        String unionSql = "";
        int paramSuffix = 1;
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmCommitFilter scmCommitFilter = scmCommitsFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createCommitsWhereClauseAndUpdateParams(company, params, scmCommitFilter.getCommitShas(),
                    scmCommitFilter.getRepoIds(), scmCommitFilter.getVcsTypes(), scmCommitFilter.getProjects(), scmCommitFilter.getCommitters(),
                    scmCommitFilter.getAuthors(), scmCommitFilter.getDaysOfWeek(), scmCommitFilter.getIntegrationIds(), scmCommitFilter.getCommitBranches(), scmCommitFilter.getExcludeCommitShas(),
                    scmCommitFilter.getExcludeRepoIds(), scmCommitFilter.getExcludeProjects(), scmCommitFilter.getExcludeCommitters(), scmCommitFilter.getExcludeCommitBranches(),
                    scmCommitFilter.getExcludeAuthors(), scmCommitFilter.getExcludeDaysOfWeek(), scmCommitFilter.getCommittedAtRange(),
                    scmCommitFilter.getLocRange(), scmCommitFilter.getExcludeLocRange(), scmCommitFilter.getPartialMatch(), scmCommitFilter.getExcludePartialMatch(), scmCommitFilter.getFileTypes(), scmCommitFilter.getExcludeFileTypes(),
                    scmCommitFilter.getCodeChangeSize(), scmCommitFilter.getCodeChanges(), scmCommitFilter.getCodeChangeUnit(),
                    scmCommitFilter.getTechnologies(), scmCommitFilter.getExcludeTechnologies(), String.valueOf(paramSuffix), "", isListQuery, ouConfig, scmCommitFilter.getIds(),scmCommitFilter.getIsApplyOuOnVelocityReport(),
                    scmCommitFilter.getCreatedAtRange());
            listOfUnionSqls.add(scmCommitsFilterParser.getSqlStmt(company, conditions,
                    isListQuery, scmCommitFilter, paramSuffix));
            paramSuffix++;
        }
        return String.join(" UNION ", listOfUnionSqls);
    }
    //endregion

    public DbListResponse<DbAggregationResult> stackedCommitsGroupBy(String company,
                                                                     ScmCommitFilter filter,
                                                                     List<ScmCommitFilter.DISTINCT> stacks,
                                                                     OUConfiguration ouConfig) throws SQLException {
        return stackedCommitsGroupBy(company, filter, stacks, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> stackedCommitsGroupBy(String company,
                                                                     ScmCommitFilter filter,
                                                                     List<ScmCommitFilter.DISTINCT> stacks,
                                                                     OUConfiguration ouConfig,
                                                                     Integer pageNumber,
                                                                     Integer pageSize) throws SQLException {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        DbListResponse<DbAggregationResult> tempResult = groupByAndCalculateCommits(company, filter, false, ouConfig, pageNumber, pageSize);
        log.info("[{}] Scm Agg: done across '{}' - results={}", company, filter.getAcross(), tempResult.getCount());
        if (stacks == null
                || stacks.size() == 0
                || !stackSupportedForCommits.contains(stacks.get(0))) {
            return tempResult;
        }

        if (tempResult.getCount() > STACK_COUNT_THRESHOLD) {
            log.info("[{}] Scm Agg: Total number of buckets to be processed are {}, will pick only top {} records ", company, tempResult.getCount(), TOP_N_RECORDS);
            tempResult = getTopNResults(tempResult.getRecords());
        }

        DbListResponse<DbAggregationResult> result = tempResult;

        ScmCommitFilter.DISTINCT stack = stacks.get(0);
        ForkJoinPool threadPool = null;
        try {
            log.info("[{}] Scm Agg: started processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            Stream<DbAggregationResult> stream = result.getRecords().parallelStream().map(row -> {
                try {
                    log.info("[{}] Scm Agg: --- currently processing stack for '{}' across '{}' - buckets={}, current='{}'", company, stack, filter.getAcross(), result.getCount(), row.getKey());
                    ScmCommitFilter newFilter;
                    OUConfiguration ouConfigForStacks = ouConfig;
                    final ScmCommitFilter.ScmCommitFilterBuilder newFilterBuilder = filter.toBuilder();
                    switch (filter.getAcross()) {
                        case committer:
                            newFilter = newFilterBuilder.committers(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "committers");
                            break;
                        case author:
                            newFilter = newFilterBuilder.authors(List.of(row.getKey()))
                                    .across(stack).build();
                            ouConfigForStacks = newOUConfigForStacks(ouConfig, "authors");
                            break;
                        case repo_id:
                            newFilter = newFilterBuilder.repoIds(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case project:
                            newFilter = newFilterBuilder.projects(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case vcs_type:
                            newFilter = newFilterBuilder.vcsTypes(List.of(VCS_TYPE.fromString(row.getKey())))
                                    .across(stack).build();
                            break;
                        case file_type:
                            newFilter = newFilterBuilder.fileTypes(List.of(row.getKey()))
                                    .across(stack).build();
                            break;
                        case trend:
                            newFilter = getCommitsFilterForTrendStack(
                                    newFilterBuilder, row, filter.getAcross(), stack,
                                    MoreObjects.firstNonNull(filter.getAggInterval().toString(), "")).build();
                            break;
                        default:
                            throw new SQLException("This stack is not available for scm queries." + stack);
                    }

                    newFilter = newFilter.toBuilder().sort(Map.of(stack.toString(), SortingOrder.ASC)).build();
                    List<DbAggregationResult> currentStackResults = groupByAndCalculateCommits(company, newFilter, false, ouConfigForStacks).getRecords();
                    return row.toBuilder().stacks(currentStackResults).build();
                } catch (SQLException e) {
                    throw new RuntimeStreamException(e);
                }
            });
            // -- collecting parallel stream with custom pool
            // (note: the toList collector preserves the encountered order)
            threadPool = new ForkJoinPool(DEFAULT_STACK_PARALLELISM);
            List<DbAggregationResult> finalList = threadPool.submit(() -> stream.collect(Collectors.toList())).join();
            log.info("[{}] Scm Agg: done processing stacks for '{}' across '{}' - buckets={}", company, stack, filter.getAcross(), result.getCount());
            return DbListResponse.of(finalList, result.getTotalCount());
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof SQLException) {
                throw (SQLException) e.getCause();
            }
            throw new SQLException("Failed to execute stack query", e);
        } finally {
            if (threadPool != null) {
                threadPool.shutdown(); // -- Very important: threads in the pool are not GC'ed automatically.
            }
        }
    }

    private DbListResponse<DbAggregationResult> getTopNResults(List<DbAggregationResult> result) {

        List<DbAggregationResult> topNResults = result.stream().
                sorted(Comparator.comparingLong(dbAgg -> dbAgg.getLinesAddedCount() + dbAgg.getLinesRemovedCount() + dbAgg.getLinesChangedCount()))
                .collect(toList());
        int topNRecords = TOP_N_RECORDS / 2;
        topNResults = Stream.concat(topNResults.subList((topNResults.size() - topNRecords), topNResults.size()).stream(),
                        topNResults.subList(0, topNRecords).stream())
                .distinct()
                .collect(toList());

        return DbListResponse.of(topNResults, topNResults.size());

    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCommits(String company,
                                                                          ScmCommitFilter filter,
                                                                          OUConfiguration ouConfig) {
        return groupByAndCalculateCommits(company, filter, false, ouConfig);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCommits(String company,
                                                                          ScmCommitFilter filter,
                                                                          boolean valuesOnly,
                                                                          OUConfiguration ouConfig) {
        return groupByAndCalculateCommits(company, filter, valuesOnly, ouConfig, 0, Integer.MAX_VALUE);
    }

    //region Commit Aggs
    //Empty cases
    public DbListResponse<DbAggregationResult> groupByAndCalculateCommits(String company,
                                                                          ScmCommitFilter filter,
                                                                          boolean valuesOnly,
                                                                          OUConfiguration ouConfig,
                                                                          Integer pageNumber,
                                                                          Integer pageSize) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        ScmCommitFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = ScmCommitFilter.CALCULATION.count;
        }
        String sortByKey = getCommitsSortByKey(filter.getSort(), filter.getAcross().toString(), calculation);
        if (calculation.equals(ScmCommitFilter.CALCULATION.commit_days)) {
            Validate.isTrue((filter.getCommittedAtRange().getLeft() != null || filter.getCommittedAtRange().getRight() != null), "Committed at time range should be specified.");
        }
        String filterByProductSQL = "";
        String commitsWhere = "";
        String commitGroupBy = "";
        String linesChanged;
        String outerCalculationComponent = "";
        String countCalculationComponent = "";
        boolean needAuthors = ScmQueryUtils.checkAuthors(filter, ouConfig);
        boolean needCommitters = ScmQueryUtils.checkCommitters(filter, ouConfig);
        Map<String, Object> params = new HashMap<>();
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForCommits(company, filter, params, false, ouConfig);
        }
        long codingDays = 0;
        if ((filter.getCommittedAtRange() != null) &&
                (filter.getCommittedAtRange().getRight() != null || filter.getCommittedAtRange().getLeft() != null)) {
            codingDays = getTimeDifferenceInDays(filter.getCommittedAtRange());
        }
        boolean isTechnology = filter.getAcross() == ScmCommitFilter.DISTINCT.technology;
        boolean isCount = filter.getCalculation() == null || filter.getCalculation() == ScmCommitFilter.CALCULATION.count;
        Map<String, List<String>> conditions = createCommitsWhereClauseAndUpdateParams(company, params, filter, null, "", false, ouConfig);
        if (conditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        }
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            if (commitsWhere.equals("")) {
                commitsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                commitsWhere = commitsWhere + " AND repo_ids IN (:repo_ids) ";
            }
        }
        String intervalColumn = "";
        String column = "";
        String avgColumn = "";
        String integIdCondition = "";
        String innerGroupBy = "";
        Optional<String> additionalKey = Optional.empty();
        boolean sortAscending = true;
        SortingOrder sortOrder = getScmSortOrder(filter.getSort());
        switch (calculation) {
            case commit_count:
                linesChanged = "lines_changed";
                outerCalculationComponent = "SUM(no_of_commits)/SUM(coding_days)AS mean, PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY no_of_commits)AS median, day ,SUM(commit_size)AS commit_size";
                calculationComponent = " COUNT(*) no_of_commits, COUNT(DISTINCT(date(committed_at))) AS coding_days, day, week_number, SUM(additions+deletions+changes)AS commit_size";
                orderByString = (sortByKey.isEmpty()) ? "median ASC" : getCommitsOrderByString(filter.getSort(), sortByKey);
                intervalColumn = (filter.getAcross().equals(ScmCommitFilter.DISTINCT.file_type)) ? " to_char(date (scm_commits.committed_at), 'Day') AS day, Date_part('WEEK', scm_commits.committed_at) AS week_number," :
                        " to_char(date (committed_at), 'Day') AS day, Date_part('WEEK', committed_at) AS week_number,";
                innerGroupBy = "day, week_number,";
                commitGroupBy = ",day";
                break;
            case commit_days:
                linesChanged = "lines_changed";
                outerCalculationComponent = " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY coding_days) AS median, sum(commit_size)AS commit_size, " +
                        "ROUND(" + ((filter.getAggInterval().equals(AGG_INTERVAL.month)) ? "(" : "") + "((SUM(coding_days)/" + codingDays + ")*7)" +
                        ((filter.getAggInterval().equals(AGG_INTERVAL.biweekly)) ? "*2 " : ((filter.getAggInterval().equals(AGG_INTERVAL.month)) ? "/7)*30" : "")) + ",7) AS mean ";
                calculationComponent = " COUNT(DISTINCT(date(committed_at))) as coding_days, SUM(additions + deletions + changes) as commit_size";
                orderByString = (sortByKey.isEmpty()) ? "median ASC" : getCommitsOrderByString(filter.getSort(), sortByKey);
                break;
            case commit_count_only:
                linesChanged = "additions+deletions+changes";
                orderByString = (sortByKey.isEmpty()) ? " ct DESC " : getCommitsOrderByString(filter.getSort(), sortByKey);
                column = "lines".equals(filter.getCodeChangeUnit()) ? linesChanged : "files_ct";
                avgColumn = "lines".equals(filter.getCodeChangeUnit()) ? " sum(" + linesChanged + ") * 1.0 /count(distinct(id)) " :
                        " count(*) * 1.0 /count(distinct(id))  ";
                countCalculationComponent = "COUNT(distinct(id)) as ct";
                calculationComponent = " COUNT(*) as tot_files_ct, " +
                        " " + countCalculationComponent + (isTechnology ? "" : ",sum(changes) as tot_lines_changed," +
                        " sum(deletions) as tot_lines_removed, sum(additions) as tot_lines_added," + avgColumn + " as avg_change_size, " +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY " + column + ") as median_change_size");
                break;
            default:
                linesChanged = "lines_changed";
                orderByString = (sortByKey.isEmpty()) ? " ct DESC " : getCommitsOrderByString(filter.getSort(), sortByKey);
                column = "lines".equals(filter.getCodeChangeUnit()) ? "lines_changed" : "files_ct";
                avgColumn = "lines".equals(filter.getCodeChangeUnit()) ? " sum(lines_changed) * 1.0 /count(distinct(id)) " :
                        " count(*) * 1.0 /count(distinct(id))  ";
                countCalculationComponent = "COUNT(distinct(id)) as ct";
                calculationComponent = " COUNT(*) as tot_files_ct, " +
                        " " + countCalculationComponent + (isTechnology ? "" : ",sum(change) as tot_lines_changed," +
                        " sum(deletion) as tot_lines_removed, sum(addition) as tot_lines_added," + avgColumn + " as avg_change_size, " +
                        " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY " + column + ") as median_change_size," +
                        " sum(total_legacy_code_lines) * 100.0/ NULLIF(sum(lines_total),0) as pct_legacy_refactored_lines," +
                        " sum(total_refactored_code_lines) * 100.0/ NULLIF(sum(lines_total),0) as pct_refactored_lines," +
                        " sum(total_new_lines) * 100.0/ NULLIF(sum(lines_total),0) as pct_new_lines ");
                break;
        }
        boolean needFiles = checkFileTableFilters(filter);
        String metricAcrossString = "";
        String fileCommitsSelect = "";
        ScmCommitFilter.DISTINCT DISTINCT = filter.getAcross();
        switch (DISTINCT) {
            case committer:
                needCommitters = true;
                groupByString = "committer_id, " + filter.getAcross().name();
                selectDistinctString = "committer_id, " + filter.getAcross().name();
                additionalKey = Optional.of("committer_id");
                metricAcrossString = "committer_id";
                break;
            case author:
                needAuthors = true;
                groupByString = "author_id, " + filter.getAcross().name();
                selectDistinctString = "author_id, " + filter.getAcross().name();
                additionalKey = Optional.of("author_id");
                metricAcrossString = "author_id";
                break;
            case commit_branch:
                needAuthors = true;
                groupByString = filter.getAcross().name();
                selectDistinctString = filter.getAcross().name();
                additionalKey = Optional.of("commit_branch");
                metricAcrossString = "commit_branch";
                break;
            case vcs_type:
                groupByString = (calculation.equals(ScmCommitFilter.CALCULATION.count)) ? "vcs_type, " + filter.getAcross().name() : filter.getAcross().name();
                selectDistinctString = (calculation.equals(ScmCommitFilter.CALCULATION.count)) ? "vcs_type, " + filter.getAcross().name() : filter.getAcross().name();
                additionalKey = (calculation.equals(ScmCommitFilter.CALCULATION.count)) ? (Optional.of("vcs_type")) : Optional.empty();
                metricAcrossString = "vcs_type";
                break;
            case code_category:
                needFiles = true;
            case technology:
            case project:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                metricAcrossString = filter.getAcross().toString();
                break;
            case repo_id:
                groupByString = "repo_ids";
                selectDistinctString = "repo_ids";
                metricAcrossString = "repo_ids";
                break;
            case file_type:
                needFiles = true;
                groupByString = "file_type";
                selectDistinctString = filter.getAcross().toString();
                metricAcrossString = "file_type";
                break;
            case code_change:
                needFiles = true;
                if ("lines".equals(filter.getCodeChangeUnit())) {
                    metricAcrossString = filter.getAcross().toString();
                    groupByString = filter.getAcross().toString();
                    selectDistinctString = filter.getAcross().toString();
                } else {
                    metricAcrossString = "files_changed";
                    groupByString = "files_changed";
                    selectDistinctString = "files_changed as code_change";
                }
                break;
            case trend:
                if (AGG_INTERVAL.day_of_week == filter.getAggInterval() && isCount) {
                    groupByString = "trend_interval";
                    selectDistinctString = "trend_interval as trend";
                    intervalColumn = " RTRIM(To_char(Date(committed_at), 'Day')) AS trend_interval,";
                    metricAcrossString = "trend_interval";
                    orderByString = "trend_interval";
                    break;
                }
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmCommitFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }
                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        ("committed_at", DISTINCT.toString(), filter.getAggInterval() != null ?
                                filter.getAggInterval().toString() : null, false, sortAscending);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = intervalColumn + ticketModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                groupByString = ticketModAggQuery.getGroupBy();
                metricAcrossString = groupByString;
                if (MapUtils.isEmpty(filter.getSort()) || ScmCommitFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = ticketModAggQuery.getOrderBy();
                }
                additionalKey = Optional.of(ticketModAggQuery.getIntervalKey());
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        if (StringUtils.isEmpty(commitsWhere)) {
            commitsWhere = " WHERE " + metricAcrossString + " IS NOT NULL ";
        } else {
            commitsWhere += " AND " + metricAcrossString + " IS NOT NULL ";
        }
        String fileSelect = (needFiles || (!valuesOnly && calculation != ScmCommitFilter.CALCULATION.commit_count_only)) ? " ,commit_files.file_type " : StringUtils.EMPTY;
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(filter.getDaysOfWeek()) || CollectionUtils.isNotEmpty(filter.getExcludeDaysOfWeek())) {
            String dayColumn = " , rtrim(to_char(date (scm_commits.committed_at), 'Day'))::VARCHAR AS day ";
            commitsSelect += dayColumn;
        }
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds()) && (needFiles || (!valuesOnly && calculation != ScmCommitFilter.CALCULATION.commit_count_only))) {
            List<Integer> integsList = filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList());
            integIdCondition = " AND scm_files.integration_id IN (" + StringUtils.join(integsList, ",") + ")";
        }
        String fileTableJoinStmt = " LEFT JOIN (SELECT commit_sha,filetype AS file_type,addition,file_id,addition+deletion+change as lines_changed," +
                "deletion,change,previous_committed_at,integration_id AS file_integ_id FROM " + company + ".scm_files scm_files INNER JOIN " +
                company + ".scm_file_commits scm_file_commits ON scm_file_commits.file_id = scm_files.id " +
                integIdCondition + " ) commit_files ON commit_files.commit_sha = scm_commits.commit_sha AND commit_files.file_integ_id = scm_commits.integration_id";
        String fileTableJoin = needFiles || (!valuesOnly && calculation != ScmCommitFilter.CALCULATION.commit_count_only) ? fileTableJoinStmt : StringUtils.EMPTY;
        fileCommitsSelect += needFiles || !valuesOnly ? getCodeChangeSql(filter.getCodeChangeSizeConfig(), linesChanged) : StringUtils.EMPTY;
        fileCommitsSelect += needFiles || !valuesOnly ? getFilesChangeSql(filter.getCodeChangeSizeConfig()) : StringUtils.EMPTY;
        fileCommitsSelect += (needFiles || (!valuesOnly && calculation != ScmCommitFilter.CALCULATION.commit_count_only)) ? getFileCommitsSelect() : StringUtils.EMPTY;

        if (needFiles || (!valuesOnly && calculation != ScmCommitFilter.CALCULATION.commit_count_only)) {
            params.put("metric_previous_committed_at", filter.getLegacyCodeConfig() != null ? filter.getLegacyCodeConfig() :
                    Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());
        }

        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM ( SELECT " + intervalColumn + " unnest(scm_commits.repo_id) as repo_ids, "
                    + commitsSelect + authorsSelect + committersSelect
                    + fileSelect + fileCommitsSelect + " FROM " + company + "."
                    + COMMITS_TABLE + authorTableJoin + committerTableJoin + fileTableJoin + " ) a " + commitsWhere;
            if (isTechnology) {
                filterByProductSQL = "SELECT * FROM ( SELECT * FROM ( "
                        + " SELECT " + intervalColumn + " unnest(repo_id) as repo_ids, " + commitsSelect
                        + authorsSelect + committersSelect + fileSelect + fileCommitsSelect
                        + " FROM " + company + "." + COMMITS_TABLE + authorTableJoin + committerTableJoin + fileTableJoin
                        + " ) c INNER JOIN ("
                        + " SELECT name as technology,repo_id as tr_id,integration_id as ti_id FROM "
                        + company + ".gittechnologies ) x ON x.tr_id = c.repo_ids AND c.integration_id = x.ti_id "
                        + " ) a " + commitsWhere;
            }
        }
        String outerJoin = "";
        String endJoin = "";
        String selectString = "";
        if (List.of(ScmCommitFilter.CALCULATION.commit_days, ScmCommitFilter.CALCULATION.commit_count).contains(calculation)) {
            outerJoin = " SELECT " + selectDistinctString + "," + outerCalculationComponent + " FROM (";
            endJoin = " ) b GROUP BY " + groupByString + commitGroupBy;
            groupByString = innerGroupBy + groupByString;
            if (filter.getAcross().equals(ScmCommitFilter.DISTINCT.trend)) {
                selectString = ", trend_interval";
            }
        }
        String scmMetricSelect = ",sum(lines_changed) " +
                "             filter (where code_category = 'legacy_refactored_lines' )" +
                "             over (partition by " + metricAcrossString + ") as total_legacy_code_lines ,\n" +
                "             sum(lines_changed) filter (where code_category = 'refactored_lines') " +
                "             over (partition by " + metricAcrossString + ") as total_refactored_code_lines ,\n" +
                "             sum(lines_changed) filter (where code_category = 'new_lines') over (partition by " + metricAcrossString + ") as total_new_lines,\n" +
                "             sum(lines_changed) over (partition by " + metricAcrossString + ") as lines_total," +
                "             sum(files_ct) over (partition by " + metricAcrossString + ") as files_total";
        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0){
            String intrSql = outerJoin +
                    "SELECT " + selectDistinctString +
                    (valuesOnly ? (StringUtils.isNotEmpty(countCalculationComponent) ? "," + countCalculationComponent : StringUtils.EMPTY) :
                            (StringUtils.isNotEmpty(selectDistinctString) ? "," : "") + calculationComponent)
                    + selectString
                    + " FROM ( select * "
                    + (!(isTechnology || !isCount || valuesOnly || calculation == ScmCommitFilter.CALCULATION.commit_count_only) ? scmMetricSelect : StringUtils.EMPTY)
                    + " from ( " + filterByProductSQL + " ) innr "
                    + " ) x GROUP BY " + groupByString + " "
                    + endJoin;
            String sql = intrSql
                    + " ORDER BY " + orderByString + " NULLS LAST"
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            String countSql = "SELECT COUNT(*) FROM (" +
                    intrSql
                    + " ) i";
            count = template.queryForObject(countSql, params, Integer.class);
            results = template.query(sql, params, DbScmConverters.distinctCommitRowMapper(
                additionalKey, filter.getAcross(), calculation, valuesOnly));
        }
        return DbListResponse.of(results, count);
    }
    //endregion

    public DbListResponse<DbAggregationResult> groupByAndCalculateCodingDays(String company,
                                                                             ScmCommitFilter filter,
                                                                             OUConfiguration ouConfig) {
        return groupByAndCalculateCodingDays(company, filter, ouConfig, 0, Integer.MAX_VALUE);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCodingDays(String company,
                                                                             ScmCommitFilter filter,
                                                                             OUConfiguration ouConfig,
                                                                             Integer pageNumber,
                                                                             Integer pageSize) {
        return groupByAndCalculateCodingDays(company, filter, false, ouConfig, pageNumber, pageSize);
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculateCodingDays(String company,
                                                                             ScmCommitFilter filter,
                                                                             boolean valuesOnly,
                                                                             OUConfiguration ouConfig,
                                                                             Integer pageNumber,
                                                                             Integer pageSize) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        ScmCommitFilter.CALCULATION calculation = filter.getCalculation();
        ScmCommitFilter.DISTINCT DISTINCT = filter.getAcross();
        if (calculation == null
                || !List.of(ScmCommitFilter.CALCULATION.commit_days, ScmCommitFilter.CALCULATION.commit_count).contains(calculation)
                || Set.of(ScmCommitFilter.DISTINCT.code_change,
                ScmCommitFilter.DISTINCT.file_type, ScmCommitFilter.DISTINCT.code_category).contains(DISTINCT)) {
            return groupByAndCalculateCommits(company, filter, false, ouConfig);
        }
        String sortByKey = getCommitsSortByKey(filter.getSort(), filter.getAcross().toString(), calculation);
        if (calculation.equals(ScmCommitFilter.CALCULATION.commit_days)) {
            Validate.isTrue((filter.getCommittedAtRange().getLeft() != null || filter.getCommittedAtRange().getRight() != null), "Committed at time range should be specified.");
        }
        String filterByProductSQL = "";
        String commitsWhere = "";
        String commitGroupBy = "";
        String outterCalculationComponent;
        String countCalculationComponent = "";
        boolean needAuthors = ScmQueryUtils.checkAuthors(filter, ouConfig);
        boolean needCommitters = ScmQueryUtils.checkCommitters(filter, ouConfig);
        Map<String, Object> params = new HashMap<>();
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForCommits(company, filter, params, false, ouConfig);
        }
        long codingDays = 0;
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if ((filter.getCommittedAtRange() != null) &&
                (filter.getCommittedAtRange().getRight() != null || filter.getCommittedAtRange().getLeft() != null)) {
            codingDays = getTimeDifferenceInDays(filter.getCommittedAtRange());
        }
        boolean isTechnology = filter.getAcross() == ScmCommitFilter.DISTINCT.technology;
        Map<String, List<String>> conditions = createCommitsWhereClauseAndUpdateParams(company, params, filter, null, "", false, ouConfig);
        if (conditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        }
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            if (commitsWhere.equals("")) {
                commitsWhere = " WHERE repo_ids IN (:repo_ids) ";
            } else {
                commitsWhere = commitsWhere + " AND repo_ids IN (:repo_ids) ";
            }
        }
        String intervalColumn = "";
        String innerGroupBy = "";
        Optional<String> additionalKey = Optional.empty();
        boolean sortAscending = true;
        SortingOrder sortOrder = getScmSortOrder(filter.getSort());
        switch (calculation) {
            case commit_count:
                outterCalculationComponent = "SUM(no_of_commits)/SUM(coding_days)AS mean, PERCENTILE_DISC(0.5) WITHIN GROUP (ORDER BY no_of_commits)AS median, day ,SUM(commit_size)AS commit_size";
                calculationComponent = " COUNT(*) no_of_commits, COUNT(DISTINCT(date(committed_at))) AS coding_days, day, week_number, SUM(additions+deletions+changes)AS commit_size";
                orderByString = (sortByKey.isEmpty()) ? "median ASC" : getCommitsOrderByString(filter.getSort(), sortByKey);
                intervalColumn = " to_char(date (committed_at), 'Day') AS day, Date_part('WEEK', committed_at) AS week_number,";
                innerGroupBy = "day, week_number,";
                commitGroupBy = ",day";
                break;
            case commit_days:
                outterCalculationComponent = " PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY coding_days) AS median, sum(commit_size)AS commit_size, " +
                        "ROUND(" + ((filter.getAggInterval().equals(AGG_INTERVAL.month)) ? "(" : "") + "((SUM(coding_days)/" + codingDays + ")*7)" +
                        ((filter.getAggInterval().equals(AGG_INTERVAL.biweekly)) ? "*2 " : ((filter.getAggInterval().equals(AGG_INTERVAL.month)) ? "/7)*30" : "")) + ",7) AS mean ";
                calculationComponent = " COUNT(DISTINCT(date(committed_at))) as coding_days, SUM(additions + deletions + changes) as commit_size";
                orderByString = (sortByKey.isEmpty()) ? "median ASC" : getCommitsOrderByString(filter.getSort(), sortByKey);
                break;
            default:
                return groupByAndCalculateCommits(company, filter, false, ouConfig);
        }
        boolean needFiles = checkFileTableFilters(filter);
        switch (DISTINCT) {
            case committer:
                needCommitters = true;
                groupByString = "committer_id, " + filter.getAcross().name();
                selectDistinctString = "committer_id, " + filter.getAcross().name();
                additionalKey = Optional.of("committer_id");
                break;
            case author:
                needAuthors = true;
                groupByString = "author_id, " + filter.getAcross().name();
                selectDistinctString = "author_id, " + filter.getAcross().name();
                additionalKey = Optional.of("author_id");
                break;
            case vcs_type:
                groupByString = filter.getAcross().name();
                selectDistinctString = filter.getAcross().name();
                additionalKey = Optional.empty();
                break;
            case technology:
            case project:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                break;
            case repo_id:
                groupByString = "repo_ids";
                selectDistinctString = "repo_ids";
                break;
            case trend:
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmCommitFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }
                AggTimeQueryHelper.AggTimeQuery ticketModAggQuery = AggTimeQueryHelper.getAggTimeQuery
                        ("committed_at", DISTINCT.toString(), filter.getAggInterval() != null ?
                                filter.getAggInterval().toString() : null, false, sortAscending);
                selectDistinctString = ticketModAggQuery.getSelect();
                intervalColumn = intervalColumn + ticketModAggQuery.getHelperColumn().replaceFirst(",", "") + ",";
                groupByString = ticketModAggQuery.getGroupBy();
                if (MapUtils.isEmpty(filter.getSort()) || ScmCommitFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = ticketModAggQuery.getOrderBy();
                }
                additionalKey = Optional.of(ticketModAggQuery.getIntervalKey());
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        if (CollectionUtils.isNotEmpty(filter.getDaysOfWeek()) || CollectionUtils.isNotEmpty(filter.getExcludeDaysOfWeek())) {
            String dayColumn = " , rtrim(to_char(date (scm_commits.committed_at), 'Day'))::VARCHAR AS day ";
            commitsSelect += dayColumn;
        }
        String authorTableJoin = needAuthors ? ScmQueryUtils.sqlForAuthorTableJoin(company) : StringUtils.EMPTY;
        String committerTableJoin = needCommitters ? ScmQueryUtils.sqlForCommitterTableJoin(company) : StringUtils.EMPTY;
        String fileTableJoin = " LEFT JOIN (SELECT commit_sha, filetype AS file_type, file_id FROM " + company + ".scm_files scm_files "
                + "INNER JOIN " + company + ".scm_file_commits scm_file_commits ON scm_file_commits.file_id = scm_files.id) commit_files "
                + "on commit_files.commit_sha = scm_commits.commit_sha ";
        fileTableJoin = needFiles || !valuesOnly ? fileTableJoin : StringUtils.EMPTY;

        if (needFiles || !valuesOnly) {
            params.put("metric_previous_committed_at", filter.getLegacyCodeConfig() != null ? filter.getLegacyCodeConfig() :
                    Instant.now().minus(60, ChronoUnit.DAYS).getEpochSecond());
        }

        if (StringUtils.isEmpty(filterByProductSQL)) {
            String innerSelect = " SELECT " + intervalColumn + " Unnest(scm_commits.repo_id) AS repo_ids,"
                    + commitsSelect + authorsSelect + committersSelect + ", commit_files.file_type, file_id";
            filterByProductSQL = "SELECT * FROM (" + innerSelect + " FROM " + company + "." + COMMITS_TABLE
                    + authorTableJoin + committerTableJoin + fileTableJoin + " ) a " + commitsWhere;
            if (isTechnology) {
                filterByProductSQL = "SELECT * FROM ( SELECT * FROM ( " + innerSelect
                        + " FROM " + company + "." + COMMITS_TABLE + authorTableJoin + committerTableJoin + fileTableJoin
                        + " ) c INNER JOIN ("
                        + " SELECT name as technology,repo_id as tr_id,integration_id as ti_id FROM "
                        + company + ".gittechnologies ) x ON x.tr_id = c.repo_ids AND c.integration_id = x.ti_id "
                        + " ) a " + commitsWhere;
            }
        }

        String whereClauseForDaysRange = "";
        if (filter.getDaysCountRange() != null && !ImmutablePair.nullPair().equals(filter.getDaysCountRange())) {
            if (filter.getDaysCountRange().getRight() != null && filter.getDaysCountRange().getLeft() == null) {
                whereClauseForDaysRange = " where coding_days <" + filter.getDaysCountRange().getRight();
            } else if (filter.getDaysCountRange().getLeft() != null && filter.getDaysCountRange().getRight() == null) {
                whereClauseForDaysRange = " where coding_days >" + filter.getDaysCountRange().getLeft();
            } else {
                whereClauseForDaysRange = " where coding_days >"
                        + filter.getDaysCountRange().getLeft() + " AND  coding_days <" + filter.getDaysCountRange().getRight();
            }
        }

        String outterJoin = "";
        String endJoin = "";
        String selectString = "";
        if (List.of(ScmCommitFilter.CALCULATION.commit_days, ScmCommitFilter.CALCULATION.commit_count).contains(calculation)) {
            outterJoin = " SELECT " + selectDistinctString + "," + outterCalculationComponent + " FROM (";
            endJoin = " ) b " + whereClauseForDaysRange + " GROUP BY " + groupByString + commitGroupBy;
            groupByString = innerGroupBy + groupByString;
            if (filter.getAcross().equals(ScmCommitFilter.DISTINCT.trend)) {
                selectString = ", trend_interval";
            }
        }
        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String intrSql = outterJoin +
                    "SELECT " + selectDistinctString +
                    (valuesOnly ? StringUtils.isNotEmpty(countCalculationComponent) ? "," + countCalculationComponent : StringUtils.EMPTY :
                            (StringUtils.isNotEmpty(selectDistinctString) ? "," : "") + calculationComponent)
                    + selectString
                    + " FROM ( WITH final_commit_files  as ( select * from ( " + filterByProductSQL + " ) innr )" +
                    " select * from final_commit_files "
                    + " ) x GROUP BY " + groupByString + " "
                    + endJoin;
            String sql = intrSql + " ORDER BY " + orderByString + " NULLS LAST"
                    + " OFFSET :skip LIMIT :limit";

            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.distinctCommitRowMapper(
                    additionalKey, filter.getAcross(), calculation, valuesOnly));
            String countSql = "SELECT COUNT(*) FROM ( " + intrSql + " ) i";
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }

    // LEV-5240: Need to update the project column.
    public void updateCommitProject(String company, UUID commitId, DbScmCommit commit) {
        if (StringUtils.isEmpty(commit.getProject())) {
            return;
        }
        String sql = "UPDATE " + company + "." + COMMITS_TABLE + " SET project = :project WHERE id = :id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", commitId);
        params.addValue("project", commit.getProject());
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        template.update(sql, params);
    }
    //endregion

    public void insert(String company, DbScmCommit dbScmCommit, List<DbScmFile> dbScmFiles) throws SQLException {
        List<String> fileExtns = dbScmFiles.stream()
                .map(DbScmFile::getFiletype)
                .distinct()
                .collect(Collectors.toList());
        DbScmCommit dbScmCommitWithFileTypes = dbScmCommit.toBuilder().fileTypes(fileExtns).build();
        String rowId = insertCommit(company, dbScmCommitWithFileTypes);
        dbScmFiles.forEach(scmFile -> insertFile(company, scmFile));
        log.debug("inserted commits: company {}, commit {}," +
                " workitem ids {}, issue keys {}, row id {}", company, dbScmCommit.getCommitSha(), dbScmCommit.getWorkitemIds(), dbScmCommit.getIssueKeys(), rowId);
    }

    public String insertV2(String company, DbScmCommit dbScmCommit, List<DbScmFile> dbScmFiles) throws SQLException {
        List<String> fileExtns = dbScmFiles.stream()
                .map(DbScmFile::getFiletype)
                .distinct().collect(Collectors.toList());

        DbScmCommit dbScmCommitWithFileTypes = dbScmCommit.toBuilder().fileTypes(fileExtns).build();

        boolean containsValidFiles = insertFilesV2(company, dbScmCommitWithFileTypes.getIntegrationId(), dbScmCommitWithFileTypes.getCommitSha(), dbScmFiles);
        if(! containsValidFiles) {
            log.info("insertFilesV2 insertFiles company {}, integrationId {}, commitSha {}, skipped doesn't have valid files", company, dbScmCommitWithFileTypes.getIntegrationId(), dbScmCommitWithFileTypes.getCommitSha());
            return null;
        }
        String rowId = insertCommit(company, dbScmCommitWithFileTypes);
        log.debug("inserted commits: company {}, commit {}," +
                " workitem ids {}, issue keys {}, row id {}", company, dbScmCommit.getCommitSha(), dbScmCommit.getWorkitemIds(), dbScmCommit.getIssueKeys(), rowId);
        return rowId;
    }

    //region File Insert V2

    private SqlParameterSource constructFileSqlSource(DbScmFile file) {
        return new MapSqlParameterSource().addValue("repo_id", file.getRepoId())
                .addValue("project", file.getProject())
                .addValue("integration_id", NumberUtils.toInt(file.getIntegrationId()))
                .addValue("filename", file.getFilename())
                .addValue("filetype", file.getFiletype());
    }

    @Value
    @Builder(toBuilder = true)
    public static class DbScmFileLite {
        private final UUID id;
        private final String repoId;
        private final String project;
        private final Integer integrationId;
        private final String filename;
    }
    private List<DbScmFile> enrichScmFiles(String company, String integrationId, String commitSha, List<DbScmFile> files) {
        Stopwatch st = Stopwatch.createStarted();
        List<DbScmFile> enrichedFiles = new ArrayList<>();
        for (List<DbScmFile> partition : ListUtils.partition(files, 300)) {
            if(CollectionUtils.isEmpty(partition)) {
                continue;
            }
            List<String> conditions = new ArrayList<>();
            Map<String, Object> params = new HashMap<>();
            for(int i=0; i< partition.size(); i++) {
                DbScmFile file = partition.get(i);
                String condition = String.format("( repo_id = :repo_id%d AND project = :project%d AND integration_id = :integration_id%d AND filename = :filename%d )", i, i, i, i);
                conditions.add(condition);
                params.put("repo_id" + i, file.getRepoId());
                params.put("project" + i, file.getProject());
                params.put("integration_id" + i, NumberUtils.toInt(file.getIntegrationId()));
                params.put("filename" + i, file.getFilename());
            }
            String whereCondition = " WHERE ( " + String.join(" OR ", conditions) + " )";
            String sql = "SELECT id, repo_id, integration_id, project, filename  FROM " + company + "." + FILES_TABLE + whereCondition;
            log.debug("sql = {}", sql);
            log.debug("params = {}", params);
            List<DbScmFileLite> fileLiteList = template.query(sql, params, (rs, rowNumber) -> {
                return DbScmFileLite.builder()
                        .id((UUID) rs.getObject("id"))
                        .integrationId(rs.getInt("integration_id"))
                        .project(rs.getString("project"))
                        .repoId(rs.getString("repo_id"))
                        .filename(rs.getString("filename"))
                        .build();
            });
            Map<String, Map<String, Map<String, Map<String, UUID>>>> map = new HashMap<>();
            for(DbScmFileLite fl : fileLiteList) {
                map.computeIfAbsent(fl.getIntegrationId().toString(), k -> new HashMap<>())
                        .computeIfAbsent(fl.getProject(), k -> new HashMap<>())
                        .computeIfAbsent(fl.getRepoId(), k -> new HashMap<>())
                        .put(fl.getFilename(), fl.getId());
            }
            for(DbScmFile f : partition) {
                UUID id = map.getOrDefault(f.getIntegrationId(), Map.of())
                        .getOrDefault(f.getProject(), Map.of())
                        .getOrDefault(f.getRepoId(), Map.of())
                        .getOrDefault(f.getFilename(), null);
                if (id == null) {
                    continue;
                }
                enrichedFiles.add(f.toBuilder().id(id.toString()).build());
            }
        }
        log.info("insertFilesV2 enrichScmFiles company {}, integrationId {}, commitSha {}, filesCount {}, enrichedFilesCount {}, time {}", company, integrationId, commitSha, files.size(), enrichedFiles.size(), st.elapsed(TimeUnit.MINUTES));
        return enrichedFiles;
    }

    private int insertFileCommits(String company, final UUID fileId, List<DbScmFileCommit> fileCommits) {
        String fileCommitSql = "WITH cte AS (SELECT committed_at FROM " + company + "." + FILE_COMMITS_TABLE
                + " WHERE committed_at < :current_committed_at AND file_id = :file_id ORDER BY committed_at DESC LIMIT 1)"
                + " INSERT INTO " + company + "." + FILE_COMMITS_TABLE
                + " (file_id,commit_sha,change,addition,deletion,committed_at,previous_committed_at)"
                + " VALUES(:file_id,:commit_sha,:change,:addition,:deletion,:committed_at,(SELECT cte.committed_at from cte)) ON CONFLICT (file_id,commit_sha)"
                + " DO NOTHING;";
        List<SqlParameterSource> params = fileCommits.stream()
                .map(fc -> {
                    LocalDateTime fcCommittedAt = LocalDateTime.ofEpochSecond(fc.getCommittedAt(), 0, ZoneOffset.UTC);
                    return new MapSqlParameterSource()
                            .addValue("current_committed_at", fcCommittedAt)
                            .addValue("file_id", fileId)
                            .addValue("commit_sha", fc.getCommitSha())
                            .addValue("change", fc.getChange())
                            .addValue("addition", fc.getAddition())
                            .addValue("deletion", fc.getDeletion())
                            .addValue("committed_at", fcCommittedAt);
                })
                .collect(Collectors.toList());
        int[] count = this.template.batchUpdate(fileCommitSql,params.toArray(new SqlParameterSource[]{}));
        int totalCount = (count == null) ? 0 : Arrays.stream(count).sum();
        return totalCount;
    }

    private int updatePrevCommittedForNextFileCommits(String company, final UUID fileId, List<DbScmFileCommit> fileCommits) {
        String updateFileCommitSql = "WITH cte AS (Select * FROM " + company + "." + FILE_COMMITS_TABLE
                + " WHERE committed_at > :current_committed_at AND file_id = :file_id ORDER BY committed_at ASC LIMIT 1)"
                + " UPDATE " + company + "." + FILE_COMMITS_TABLE + " sfc SET previous_committed_at = :current_committed_at FROM "
                + " cte WHERE sfc.id = cte.id";

        List<SqlParameterSource> params = fileCommits.stream()
                .map(fc -> {
                    LocalDateTime fcCommittedAt = LocalDateTime.ofEpochSecond(fc.getCommittedAt(), 0, ZoneOffset.UTC);
                    return new MapSqlParameterSource()
                            .addValue("current_committed_at", fcCommittedAt)
                            .addValue("file_id", fileId);
                })
                .collect(Collectors.toList());
        int[] count = this.template.batchUpdate(updateFileCommitSql,params.toArray(new SqlParameterSource[]{}));
        int totalCount = (count == null) ? 0 : Arrays.stream(count).sum();
        return totalCount;
    }

    private ImmutablePair<Integer, Integer> syncFileCommitsForSingleFile(final String company, DbScmFile file) {
        int insertFileCommitsCount = insertFileCommits(company, UUID.fromString(file.getId()), file.getFileCommits());
        int updatePrevFileCommitCount = updatePrevCommittedForNextFileCommits(company, UUID.fromString(file.getId()), file.getFileCommits());
        return ImmutablePair.of(insertFileCommitsCount, updatePrevFileCommitCount);
    }
    private ImmutablePair<Integer, Integer> syncFileCommitsForAllFiles(final String company, final String integrationId, final String commitSha, List<DbScmFile> files) {
        ImmutablePair<Integer, Integer> result = ImmutablePair.of(0,0);
        if(CollectionUtils.isEmpty(files)) {
            return result;
        }

        ExecutorService executorService = Executors.newFixedThreadPool(scmCommitInsertV2SyncFileCommitsThreadCount);
        Stopwatch st = Stopwatch.createStarted();
        int insertFileCommitsCount = 0;
        int updatePrevFileCommitCount = 0;
        try {
            List<Future<ImmutablePair<Integer, Integer>>> futures = new ArrayList<>();
            for(DbScmFile f : files) {
                futures.add(executorService.submit(() -> syncFileCommitsForSingleFile(company, f)));
            }
            for (int i = 0; i < futures.size(); i++) {
                try {
                    ImmutablePair<Integer, Integer> res = futures.get(i).get();
                    insertFileCommitsCount += res.getLeft();
                    updatePrevFileCommitCount += res.getRight();
                } catch (ExecutionException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            return ImmutablePair.of(insertFileCommitsCount,updatePrevFileCommitCount);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            log.info("insertFilesV2 Completed File Commits Sync for all Files, company {}, integrationId {}, commitSha {}, filesCount {}, insertFileCommitsCount {}, updatePrevFileCommitCount {}, time {}",
                    company, integrationId, commitSha, files.size(), insertFileCommitsCount, updatePrevFileCommitCount, st.elapsed(TimeUnit.MINUTES));
            if (executorService != null) {
                executorService.shutdown();
            }
        }
    }

    private boolean scmFileHasChanges(DbScmFile file) {
        long allChanges = ObjectUtils.firstNonNull(file.getTotalAdditions(), 0l)
                + ObjectUtils.firstNonNull(file.getTotalDeletions(), 0l)
                + ObjectUtils.firstNonNull(file.getTotalChanges(), 0l);
        return allChanges > 0l;
    }

    /**
     * Returns true if input contained atleast one valid file
     * Returns true if input contained zero valid files
     * @param company
     * @param integrationId
     * @param commitSha
     * @param files
     * @return
     */
    public boolean insertFilesV2(String company, String integrationId, String commitSha, List<DbScmFile> files) {
        List<DbScmFile> filteredFiles = CollectionUtils.emptyIfNull(files).stream()
                .filter(f -> scmFileHasChanges(f))
                .collect(toList());
        if (CollectionUtils.isEmpty(filteredFiles)) {
            return false;
        }

        Stopwatch st = Stopwatch.createStarted();
        String fileSql = "INSERT INTO " + company + "." + FILES_TABLE +
                " (repo_id,project,integration_id,filename, filetype) VALUES(:repo_id,:project,:integration_id,:filename, :filetype)" +
                " ON CONFLICT (filename,integration_id,repo_id,project)" +
                " DO NOTHING";
        List<SqlParameterSource> params = filteredFiles.stream()
                .map(f -> constructFileSqlSource(f))
                .collect(Collectors.toList());
        int[] count = this.template.batchUpdate(fileSql,params.toArray(new SqlParameterSource[]{}));
        log.info("insertFilesV2 insertFiles company {}, integrationId {}, commitSha {}, time {}", company, integrationId, commitSha, st.elapsed(TimeUnit.MINUTES));
        int insertFilesCount = (count == null) ? 0 : Arrays.stream(count).sum();


        List<DbScmFile> filesWithIds = enrichScmFiles(company, integrationId, commitSha, filteredFiles);

        ImmutablePair<Integer, Integer> syncFileCommitsResult = syncFileCommitsForAllFiles(company, integrationId, commitSha, filesWithIds);
        int insertFileCommitsCount = syncFileCommitsResult.getLeft();
        int updatePrevFileCommitCount = syncFileCommitsResult.getRight();
        log.info("insertFilesV2 company {}, integrationId {}, commitSha {}, insertFilesCount {}, insertFileCommitsCount {}, updatePrevFileCommitCount {}", company, integrationId, commitSha, insertFilesCount, insertFileCommitsCount, updatePrevFileCommitCount);
        return true;
    }
    //endregion

    //region File Insert
    public String insertFile(String company, DbScmFile file) {
        String fileId = template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            //insert new issue
            String fileSql = "INSERT INTO " + company + "." + FILES_TABLE +
                    " (repo_id,project,integration_id,filename, filetype) VALUES(?,?,?,?,?)" +
                    " ON CONFLICT (filename,integration_id,repo_id,project)" +
                    " DO NOTHING";

            try (PreparedStatement insertFilePstmt = conn.prepareStatement(
                    fileSql, Statement.RETURN_GENERATED_KEYS)) {
                int i = 1;
                insertFilePstmt.setObject(i++, file.getRepoId());
                insertFilePstmt.setObject(i++, file.getProject());
                insertFilePstmt.setObject(i++, NumberUtils.toInt(file.getIntegrationId()));
                insertFilePstmt.setObject(i++, file.getFilename());
                insertFilePstmt.setObject(i, file.getFiletype());

                int insertedRows = insertFilePstmt.executeUpdate();
                String insertedRowId = null;

                if (insertedRows == 0) {
                    return null;
                }
                try (ResultSet rs = insertFilePstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        insertedRowId = rs.getString(1);
                    }
                }
                if (insertedRowId == null) {
                    throw new SQLException("Failed to get inserted rowid.");
                }
                return insertedRowId;
            }
        }));
        if (fileId == null) {
            String subSql = "SELECT id FROM " + company + "." + FILES_TABLE +
                    " WHERE repo_id = :repo_id AND project = :project AND integration_id = :integration_id AND filename = :filename";
            Map<String, Object> subParams = Map.of("repo_id", file.getRepoId(),
                    "project", file.getProject(),
                    "integration_id", NumberUtils.toInt(file.getIntegrationId()),
                    "filename", file.getFilename());
            log.debug("subSql = {}", subSql);
            log.debug("subParams = {}", subParams);
            fileId = template.query(subSql, subParams, DbScmConverters.getIdRowMapper()).get(0);
        }
        final String returnVal = fileId;
        template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            String fileCommitSql = "WITH cte AS (SELECT committed_at FROM " + company + "." + FILE_COMMITS_TABLE
                    + " WHERE committed_at < ? AND file_id = ? ORDER BY committed_at DESC LIMIT 1)"
                    + " INSERT INTO " + company + "." + FILE_COMMITS_TABLE
                    + " (file_id,commit_sha,change,addition,deletion,committed_at,previous_committed_at)"
                    + " VALUES(?,?,?,?,?,?,(SELECT cte.committed_at from cte)) ON CONFLICT (file_id,commit_sha)"
                    + " DO NOTHING;";
            String updateFileCommitSql = "WITH cte AS (Select * FROM " + company + "." + FILE_COMMITS_TABLE
                    + " WHERE committed_at > ? AND file_id = ? ORDER BY committed_at ASC LIMIT 1)"
                    + " UPDATE " + company + "." + FILE_COMMITS_TABLE + " sfc SET previous_committed_at = ? FROM "
                    + " cte WHERE sfc.id = cte.id";

            try (PreparedStatement insertFileCommit = conn.prepareStatement(
                    fileCommitSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement updateFileCommit = conn.prepareStatement(
                         updateFileCommitSql, Statement.RETURN_GENERATED_KEYS)) {
                for (DbScmFileCommit fileCommit : file.getFileCommits()) {
                    int i = 1;
                    insertFileCommit.setObject(i++, LocalDateTime.ofEpochSecond(
                            fileCommit.getCommittedAt(), 0, ZoneOffset.UTC));
                    insertFileCommit.setObject(i++, UUID.fromString(returnVal));
                    insertFileCommit.setObject(i++, UUID.fromString(returnVal));
                    insertFileCommit.setObject(i++, fileCommit.getCommitSha());
                    insertFileCommit.setObject(i++, fileCommit.getChange());
                    insertFileCommit.setObject(i++, fileCommit.getAddition());
                    insertFileCommit.setObject(i++, fileCommit.getDeletion());
                    insertFileCommit.setObject(i, LocalDateTime.ofEpochSecond(
                            fileCommit.getCommittedAt(), 0, ZoneOffset.UTC));
                    insertFileCommit.addBatch();
                    insertFileCommit.clearParameters();

                    i = 1;
                    updateFileCommit.setObject(i++, LocalDateTime.ofEpochSecond(
                            fileCommit.getCommittedAt(), 0, ZoneOffset.UTC));
                    updateFileCommit.setObject(i++, UUID.fromString(returnVal));
                    updateFileCommit.setObject(i, LocalDateTime.ofEpochSecond(
                            fileCommit.getCommittedAt(), 0, ZoneOffset.UTC));
                    updateFileCommit.executeUpdate();
                }
                insertFileCommit.executeBatch();
                return null;
            }
        }));
        return returnVal;
    }
    //endregion

    //region File List
    public DbListResponse<DbScmFile> list(String company,
                                          ScmFilesFilter filter,
                                          Map<String, SortingOrder> sortBy,
                                          Integer pageNumber,
                                          Integer pageSize)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String filesWhere = "";
        String fileCommitsWhere = " WHERE file_id = files.id ";
        String filterByProductSQL = "";
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForFiles(company, filter, params, true);
        }
        Map<String, List<String>> conditions = createFilesWhereClauseAndUpdateParams(params,
                filter.getRepoIds(), filter.getProjects(), filter.getIntegrationIds(),
                filter.getExcludeRepoIds(), filter.getExcludeProjects(), filter.getFilename(),
                filter.getModule(), filter.getPartialMatch(), filter.getCommitStartTime(),
                filter.getCommitEndTime(), null);
        if (conditions.get(FILES_TABLE).size() > 0) {
            filesWhere = " WHERE " + String.join(" AND ", conditions.get(FILES_TABLE));
        }
        if (conditions.get(FILE_COMMITS_TABLE).size() > 0) {
            fileCommitsWhere = fileCommitsWhere + " AND " + String.join(" AND ", conditions.get(FILE_COMMITS_TABLE));
        }

        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (FILE_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "num_commits";
                })
                .orElse("num_commits");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT files.*,"
                    + "(SELECT COUNT(*) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS num_commits,"
                    + "(SELECT SUM(change) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS changes,"
                    + "(SELECT SUM(addition) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS additions,"
                    + "(SELECT SUM(deletion) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS deletions"
                    + " FROM " + company + "." + FILES_TABLE + " AS files"
                    + filesWhere;
        }
        List<DbScmFile> results = List.of();
        if (pageSize > 0) {
            String sql = "SELECT * FROM ( " + filterByProductSQL
                    + " ) a ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.filesRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM ( " + filterByProductSQL
                + " ) a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }
    //endregion

    //region File Commit Update File Stats
    //Need to update the file stats...
    //ToDo: Need to remove after some time...
    public Boolean updateFileCommitStats(String company, UUID id, int addition, int deletion, int change) throws SQLException {
        String updateFilesCountSql = "UPDATE %s." + FILE_COMMITS_TABLE + " SET addition = :addition, deletion = :deletion, change = :change WHERE id = :id";
        String sql = String.format(updateFilesCountSql, company);
        Map<String, Object> params = Map.of("id", id, "addition", addition, "deletion", deletion, "change", change);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        int updatedRows = template.update(sql, params);
        if (updatedRows != 1) {
            throw new SQLException(String.format("Failed to update file stats for company %s, id %s", company, id));
        }
        return true;
    }
    //endregion

    //region File Get
    public Optional<DbScmFile> getFile(String company, String fileName, String repoId, String project, String integrationId) {
        Validate.notBlank(fileName, "Missing fileName.");
        Validate.notBlank(repoId, "Missing repo_id.");
        Validate.notBlank(project, "Missing project.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + FILES_TABLE
                + " WHERE filename = :filename AND repo_id = :repo_id AND project = :project AND integration_id = :integid";
        Map<String, Object> params = Map.of("filename", fileName,
                "repo_id", repoId,
                "project", project,
                "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmFile> data = template.query(sql, params, DbScmConverters.commitFilesRowMapper());
        return data.stream().findFirst();
    }
    //endregion

    //region File Commit Get
    public Optional<DbScmFileCommit> getFileCommit(String company, String commitSha, String fileId) {
        Validate.notBlank(commitSha, "Missing commitSha.");
        Validate.notBlank(fileId, "Missing file_id.");
        String sql = "SELECT * FROM " + company + "." + FILE_COMMITS_TABLE
                + " WHERE commit_sha = :commit_sha AND file_id = :file_id::uuid";
        Map<String, Object> params = Map.of("commit_sha", commitSha,
                "file_id", fileId);
        log.debug("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.debug("params = {}", params);
        List<DbScmFileCommit> data = template.query(sql, params, DbScmConverters.filesCommitRowMapper());
        return data.stream().findFirst();
    }
    //endregion

    //only for unit test
    public List<DbScmFileCommit> getFileCommits(String company, String fileId) {
        Validate.notBlank(fileId, "Missing file id.");
        String sql = "SELECT * FROM " + company + "." + FILE_COMMITS_TABLE + " WHERE file_id = :file_id order by committed_at";
        Map<String, Object> params = Map.of(
                "file_id", UUID.fromString(fileId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(sql, params, DbScmConverters.filesCommitRowMapper());
    }

    //region File Union Sql
    public String getUnionSqlForFiles(String company, ScmFilesFilter reqFilter, Map<String, Object> params,
                                      boolean isListQuery) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        String unionSql = "";
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filters for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmFilesFilter scmFilesFilter = scmFilesFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createFilesWhereClauseAndUpdateParams(params,
                    scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                    scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                    scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                    scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), String.valueOf(paramSuffix++));
            listOfUnionSqls.add(scmFilesFilterParser.getSqlStmt(company, conditions, isListQuery));
        }
        return String.join(" UNION ", listOfUnionSqls);
    }
    //endregion

    //region Issue Insert
    public String insertIssue(String company, DbScmIssue issue) {
        return template.getJdbcOperations().execute(TransactionCallback.of(conn -> {
            //cleanup existing issue. also cleans up related tables.
            String deleteSql = "DELETE FROM " + company + "." + ISSUES_TABLE
                    + " WHERE issue_id = ? AND repo_id = ? AND project = ? AND integration_id = ?";
            //insert new issue
            String commitSql = "INSERT INTO " + company + "." + ISSUES_TABLE +
                    " (repo_id,project,integration_id,issue_id,number,assignees,creator,creator_id,labels,state,title,url," +
                    "num_comments,issue_created_at,issue_updated_at,issue_closed_at,first_comment_at)" +
                    " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT (issue_id,repo_id,project,integration_id)" +
                    " DO NOTHING";

            try (PreparedStatement del = conn.prepareStatement(deleteSql, Statement.RETURN_GENERATED_KEYS);
                 PreparedStatement insertPstmt = conn.prepareStatement(commitSql, Statement.RETURN_GENERATED_KEYS)) {
                del.setString(1, issue.getIssueId());
                del.setString(2, issue.getRepoId());
                del.setString(3, issue.getProject());
                del.setInt(4, NumberUtils.toInt(issue.getIntegrationId()));
                del.executeUpdate();

                int i = 1;
                insertPstmt.setObject(i++, issue.getRepoId());
                insertPstmt.setObject(i++, issue.getProject());
                insertPstmt.setObject(i++, NumberUtils.toInt(issue.getIntegrationId()));
                insertPstmt.setObject(i++, issue.getIssueId());
                insertPstmt.setObject(i++, issue.getNumber());
                insertPstmt.setObject(i++, conn.createArrayOf("varchar", issue.getAssignees().toArray()));
                insertPstmt.setObject(i++, issue.getCreator());
                insertPstmt.setObject(i++, insertAndGetUserId(company, issue));
                insertPstmt.setObject(i++, conn.createArrayOf("varchar", issue.getLabels().toArray()));
                insertPstmt.setObject(i++, issue.getState());
                insertPstmt.setObject(i++, issue.getTitle());
                insertPstmt.setObject(i++, issue.getUrl());
                insertPstmt.setObject(i++, issue.getNumComments());
                insertPstmt.setObject(i++, LocalDateTime.ofEpochSecond(
                        issue.getIssueCreatedAt(), 0, ZoneOffset.UTC));
                insertPstmt.setObject(i++, LocalDateTime.ofEpochSecond(
                        issue.getIssueUpdatedAt(), 0, ZoneOffset.UTC));
                insertPstmt.setObject(i++, issue.getIssueClosedAt() != null ?
                        LocalDateTime.ofEpochSecond(
                                issue.getIssueClosedAt(), 0, ZoneOffset.UTC) : null);
                insertPstmt.setObject(i, issue.getFirstCommentAt() != null ?
                        LocalDateTime.ofEpochSecond(
                                issue.getFirstCommentAt(), 0, ZoneOffset.UTC) : null);

                int insertedRows = insertPstmt.executeUpdate();
                if (insertedRows == 0) {
                    log.debug("issue insert attempt failed. the issue: {}", issue.toString());
                    return null;
                }
                String insertedRowId = null;
                try (ResultSet rs = insertPstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        insertedRowId = rs.getString(1);
                    }
                }
                if (insertedRowId == null) {
                    throw new SQLException("Failed to get inserted rowid.");
                }
                return insertedRowId;
            }
        }));
    }

    @Nullable
    private UUID insertAndGetUserId(String company, DbScmIssue issue) {
        if(StringUtils.isBlank(issue.getCreatorInfo().getCloudId()) && StringUtils.isBlank(issue.getCreatorInfo().getDisplayName())){
            return null;
        }
        UUID userId = null;
        try {
            String userIdString = userIdentityService.upsertIgnoreEmail(company,
                    DbScmUser.builder()
                            .integrationId(issue.getIntegrationId())
                            .cloudId(StringUtils.isNotBlank(issue.getCreatorInfo().getCloudId()) ? issue.getCreatorInfo().getCloudId() : issue.getCreatorInfo().getDisplayName())
                            .displayName(StringUtils.isNotBlank(issue.getCreatorInfo().getDisplayName()) ? issue.getCreatorInfo().getDisplayName() : issue.getCreatorInfo().getCloudId())
                            .originalDisplayName(StringUtils.isNotBlank(issue.getCreatorInfo().getDisplayName()) ? issue.getCreatorInfo().getDisplayName() : issue.getCreatorInfo().getCloudId())
                            .build());
            userId = UUID.fromString(userIdString);
        } catch (SQLException e) {
            log.error("Error while inserting user: {}", e.getMessage(), e);
        }
        return userId;
    }
    //endregion

    //region Issue Get
    public Optional<DbScmIssue> getIssue(String company, String issueId, String repoId, String integrationId) {
        Validate.notBlank(issueId, "Missing issue_id.");
        Validate.notBlank(repoId, "Missing repo_id.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "SELECT * FROM " + company + "." + ISSUES_TABLE
                + " WHERE issue_id = :issue_id AND repo_id = :repo_id AND integration_id = :integid";
        Map<String, Object> params = Map.of("issue_id", issueId,
                "repo_id", repoId,
                "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmIssue> data = template.query(sql, params, DbScmConverters.issueRowMapper());
        return data.stream().findFirst();
    }
    //endregion

    //region Issue List
    public DbListResponse<DbScmIssue> list(String company,
                                           ScmIssueFilter filter,
                                           Map<String, SortingOrder> sortBy,
                                           OUConfiguration ouConfig,
                                           Integer pageNumber,
                                           Integer pageSize)
            throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String filterByProductSQL = "";
        String issuesWhere = "";
        String finalWhere = "";
        long currentTime = (new Date()).toInstant().getEpochSecond();
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForIssues(company, filter, params, true, ouConfig);
        }
        Map<String, List<String>> conditions = createIssueWhereClauseAndUpdateParams(company, params,
                filter.getExtraCriteria(), filter.getRepoIds(), filter.getProjects(), filter.getCreators(),
                filter.getAssignees(), filter.getStates(), filter.getLabels(), filter.getIntegrationIds(),
                filter.getExcludeRepoIds(), filter.getExcludeProjects(), filter.getExcludeCreators(),
                filter.getExcludeAssignees(), filter.getExcludeStates(), filter.getExcludeLabels(),
                filter.getIssueCreatedRange(), filter.getTitle(), filter.getPartialMatch(),
                filter.getIssueClosedRange(), filter.getIssueUpdatedRange(), filter.getFirstCommentAtRange(),
                currentTime, null, ouConfig);
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }
        if (conditions.get(FINAL_TABLE).size() > 0) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
        }
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ISSUE_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "issue_updated_at";
                })
                .orElse("issue_updated_at");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        List<DbScmIssue> results = List.of();
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM (SELECT issues.*,"
                    + "(extract(epoch FROM COALESCE(first_comment_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM issue_created_at)) AS resp_time FROM "
                    + company + "." + ISSUES_TABLE + " AS issues"
                    + issuesWhere + " ) a" + finalWhere;
        }
        if (pageSize > 0) {
            String sql = "SELECT * FROM (" + filterByProductSQL
                    + " ) x ORDER BY " + sortByKey + " " + sortOrder.toString()
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.issueRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM "
                + (StringUtils.isNotEmpty(finalWhere) ? "( SELECT * FROM " : "")
                + "(" + filterByProductSQL
                + " ) a " + (StringUtils.isNotEmpty(finalWhere) ? finalWhere + " ) b" : "");
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }
    //endregion

    //region Issue Create Where Clause and Params
    private Map<String, List<String>> createIssueWhereClauseAndUpdateParams(String company, Map<String, Object> params,
                                                                            List<ScmIssueFilter.EXTRA_CRITERIA> criteria,
                                                                            List<String> repoIds,
                                                                            List<String> projects,
                                                                            List<String> creators,
                                                                            List<String> assignees,
                                                                            List<String> states,
                                                                            List<String> labels,
                                                                            List<String> integrationIds,
                                                                            List<String> excludeRepoIds,
                                                                            List<String> excludeProjects,
                                                                            List<String> excludeCreators,
                                                                            List<String> excludeAssignees,
                                                                            List<String> excludeStates,
                                                                            List<String> excludeLabels,
                                                                            ImmutablePair<Long, Long> issueCreatedRange,
                                                                            String title,
                                                                            Map<String, Map<String, String>> partialMatch,
                                                                            ImmutablePair<Long, Long> issueClosedRange,
                                                                            ImmutablePair<Long, Long> issueUpdatedRange,
                                                                            ImmutablePair<Long, Long> firstCommentAtRange,
                                                                            Long currentTime,
                                                                            String paramSuffix,
                                                                            OUConfiguration ouConfig) {
        List<String> issueTableConditions = new ArrayList<>();
        List<String> finalTableConditions = new ArrayList<>();
        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        if (CollectionUtils.isNotEmpty(repoIds)) {
            issueTableConditions.add("repo_id IN (:repo_ids" + paramSuffixString + ")");
            params.put("repo_ids" + paramSuffixString, repoIds);
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            issueTableConditions.add("repo_id NOT IN (:exclude_repo_ids" + paramSuffixString + ")");
            params.put("exclude_repo_ids" + paramSuffixString, excludeRepoIds);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            issueTableConditions.add("project IN (:projects" + paramSuffixString + ")");
            params.put("projects" + paramSuffixString, projects);
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            issueTableConditions.add("project NOT IN (:exclude_projects" + paramSuffixString + ")");
            params.put("exclude_projects" + paramSuffixString, excludeProjects);
        }
        if (CollectionUtils.isNotEmpty(creators) || OrgUnitHelper.doesOuConfigHaveIssueCreators(ouConfig)) { // OU: creator
            String columnName = "creator_id";
            String columnNameParam = "creator_id" + paramSuffixString;
            if (OrgUnitHelper.doesOuConfigHaveIssueCreators(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    issueTableConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(creators)) {
                TeamUtils.addUsersCondition(company, issueTableConditions, params, columnName + "::varchar", columnNameParam, false,
                        creators, SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(excludeCreators)) {
            String columnNameParam = "exclude_creator_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, issueTableConditions, params, "creator_id::varchar", columnNameParam, false,
                    excludeCreators, SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(assignees) || OrgUnitHelper.doesOuConfigHaveIssueAssignees(ouConfig)) { // OU: assignee
            if (OrgUnitHelper.doesOuConfigHaveIssueAssignees(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    issueTableConditions.add(MessageFormat.format("{0} && (SELECT ARRAY (SELECT id FROM ({1}) l) g)", "assignees", usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(assignees)) {
                issueTableConditions.add("assignees && ARRAY[ :assignees" + paramSuffixString + " ]::varchar[]");
                params.put("assignees" + paramSuffixString, assignees);
            }
        }
        if (CollectionUtils.isNotEmpty(excludeAssignees)) {
            issueTableConditions.add("NOT assignees && ARRAY[ :exclude_assignees" + paramSuffixString + " ]::varchar[]");
            params.put("exclude_assignees" + paramSuffixString, excludeAssignees);
        }
        if (CollectionUtils.isNotEmpty(states)) {
            issueTableConditions.add("state IN (:states" + paramSuffixString + ")");
            params.put("states" + paramSuffixString, states);
        }
        if (CollectionUtils.isNotEmpty(excludeStates)) {
            issueTableConditions.add("state NOT IN (:exclude_states" + paramSuffixString + ")");
            params.put("exclude_states" + paramSuffixString, excludeStates);
        }
        if (CollectionUtils.isNotEmpty(labels)) {
            issueTableConditions.add("labels && ARRAY[ :labels" + paramSuffixString + " ]::varchar[]");
            params.put("labels" + paramSuffixString, labels);
        }
        if (CollectionUtils.isNotEmpty(excludeLabels)) {
            issueTableConditions.add("NOT labels && ARRAY[ :exclude_labels" + paramSuffixString + " ]::varchar[]");
            params.put("exclude_labels" + paramSuffixString, excludeLabels);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            issueTableConditions.add("integration_id IN (:integration_ids" + paramSuffixString + ")");
            params.put("integration_ids" + paramSuffixString,
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(title)) {
            issueTableConditions.add("title LIKE (:title" + paramSuffixString + ")");
            params.put("title" + paramSuffixString, "%" + title + "%");
        }
        if (issueCreatedRange != null) {
            if (issueCreatedRange.getLeft() != null) {
                issueTableConditions.add("issue_created_at > TO_TIMESTAMP(" + issueCreatedRange.getLeft() + ")");
            }
            if (issueCreatedRange.getRight() != null) {
                issueTableConditions.add("issue_created_at < TO_TIMESTAMP(" + issueCreatedRange.getRight() + ")");
            }
        }
        if (issueClosedRange != null) {
            if (issueClosedRange.getLeft() != null) {
                issueTableConditions.add("issue_closed_at > TO_TIMESTAMP(:issue_closed_at_start)");
                params.put("issue_closed_at_start", issueClosedRange.getLeft());
            }
            if (issueClosedRange.getRight() != null) {
                issueTableConditions.add("issue_closed_at < TO_TIMESTAMP(:issue_closed_at_end)");
                params.put("issue_closed_at_end", issueClosedRange.getRight());
            }
        }
        if (issueUpdatedRange != null) {
            if (issueUpdatedRange.getLeft() != null) {
                issueTableConditions.add("issue_updated_at > TO_TIMESTAMP(:issue_updated_at_start)");
                params.put("issue_updated_at_start", issueUpdatedRange.getLeft());
            }
            if (issueUpdatedRange.getRight() != null) {
                issueTableConditions.add("issue_updated_at < TO_TIMESTAMP(:issue_updated_at_end)");
                params.put("issue_updated_at_end", issueUpdatedRange.getRight());
            }
        }
        if (firstCommentAtRange != null) {
            if (firstCommentAtRange.getLeft() != null) {
                issueTableConditions.add("first_comment_at > TO_TIMESTAMP(:first_comment_at_start)");
                params.put("first_comment_at_start", firstCommentAtRange.getLeft());
            }
            if (firstCommentAtRange.getRight() != null) {
                issueTableConditions.add("first_comment_at < TO_TIMESTAMP(:first_comment_at_end)");
                params.put("first_comment_at_end", firstCommentAtRange.getRight());
            }
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            ScmQueryUtils.createPartialMatchFilter(partialMatch, issueTableConditions, ISSUES_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), params, "", true);
        }

        for (ScmIssueFilter.EXTRA_CRITERIA criterion : criteria) {
            switch (criterion) {
                case no_assignees:
                    issueTableConditions.add("assignees = '{}'");
                    break;
                case idle:
                    issueTableConditions.add("issue_updated_at < :idle7days" + paramSuffixString);
                    params.put("idle7days" + paramSuffixString, LocalDateTime.ofEpochSecond(
                            (currentTime - 7 * 86400), 0, ZoneOffset.UTC));
                    break;
                case no_labels:
                    issueTableConditions.add("labels = '{}'");
                    break;
                case no_response:
                    issueTableConditions.add("first_comment_at IS NULL");
                    break;
                case missed_response_time:
                    finalTableConditions.add("resp_time > :resp_sla" + paramSuffixString);
                    params.put("resp_sla" + paramSuffixString, 86400);
                    break;
            }
        }
        return Map.of(ISSUES_TABLE, issueTableConditions, FINAL_TABLE, finalTableConditions);
    }
    //endregion

    //region Issue Union Sql
    public String getUnionSqlForIssues(String company, ScmIssueFilter reqFilter, Map<String, Object> params,
                                       boolean isListQuery, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        String unionSql = "";
        List<String> listOfUnionSqls = new ArrayList<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmIssueFilter scmIssueFilter = scmIssuesFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createIssueWhereClauseAndUpdateParams(company, params,
                    scmIssueFilter.getExtraCriteria(), scmIssueFilter.getRepoIds(), scmIssueFilter.getProjects(), scmIssueFilter.getCreators(),
                    scmIssueFilter.getAssignees(), scmIssueFilter.getStates(), scmIssueFilter.getLabels(), scmIssueFilter.getIntegrationIds(),
                    scmIssueFilter.getExcludeRepoIds(), scmIssueFilter.getExcludeProjects(), scmIssueFilter.getExcludeCreators(),
                    scmIssueFilter.getExcludeAssignees(), scmIssueFilter.getExcludeStates(), scmIssueFilter.getExcludeLabels(),
                    scmIssueFilter.getIssueCreatedRange(), scmIssueFilter.getTitle(), scmIssueFilter.getPartialMatch(),
                    scmIssueFilter.getIssueClosedRange(), scmIssueFilter.getIssueUpdatedRange(), scmIssueFilter.getFirstCommentAtRange()
                    , currentTime, String.valueOf(paramSuffix++), ouConfig);
            listOfUnionSqls.add(scmIssuesFilterParser.getSqlStmt(company, conditions, scmIssueFilter, currentTime, isListQuery));
        }
        return String.join(" UNION ", listOfUnionSqls);
    }
    //endregion

    public DbListResponse<DbAggregationResult> groupByAndCalculateIssues(String company, ScmIssueFilter filter, OUConfiguration ouConfig) {
        return groupByAndCalculateIssues(company, filter, ouConfig, 0, Integer.MAX_VALUE);
    }

    //region Issue Aggs
    public DbListResponse<DbAggregationResult> groupByAndCalculateIssues(String company, ScmIssueFilter filter, OUConfiguration ouConfig, Integer pageNumber, Integer pageSize) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        Map<String, Object> params = new HashMap<>();
        long currentTime = (new Date()).toInstant().getEpochSecond();
        String filterByProductSQL = "";
        String finalWhere = "";
        String issuesWhere = "";
        boolean needResolutionTimeReport = false;
        Map<String, List<String>> conditions;
        ScmIssueFilter.CALCULATION calculation = filter.getCalculation();
        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (calculation == null) {
            calculation = ScmIssueFilter.CALCULATION.count;
        }
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForIssues(company, filter, params, false, ouConfig);
        }
        conditions = createIssueWhereClauseAndUpdateParams(company, params,
                filter.getExtraCriteria(), filter.getRepoIds(), filter.getProjects(), filter.getCreators(),
                filter.getAssignees(), filter.getStates(), filter.getLabels(), filter.getIntegrationIds(),
                filter.getExcludeRepoIds(), filter.getExcludeProjects(), filter.getExcludeCreators(),
                filter.getExcludeAssignees(), filter.getExcludeStates(), filter.getExcludeLabels(),
                filter.getIssueCreatedRange(), filter.getTitle(), filter.getPartialMatch(),
                filter.getIssueClosedRange(), filter.getIssueUpdatedRange(), filter.getFirstCommentAtRange(), currentTime, null, ouConfig);
        String sortByKey = getIssuesSortByKey(filter.getSort(), calculation, filter.getAcross().toString());
        SortingOrder sortOrder = getScmSortOrder(filter.getSort());
        orderByString = getIssuesOrderByString(sortByKey, sortOrder);
        switch (calculation) {
            case response_time:
                calculationComponent = "MIN(resp_time) AS mn,MAX(resp_time) as mx,COUNT(id) AS ct," +
                        "PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY resp_time) as md,SUM(resp_time) as sm";
                log.debug("response_time orderByString = {}", orderByString);
                break;
            case resolution_time:
                calculationComponent = "COUNT(id) AS ct, PERCENTILE_DISC(0.5) WITHIN GROUP(ORDER BY solve_time) as md";
                needResolutionTimeReport = true;
                log.debug("resolution_time orderByString = {}", orderByString);
                break;
            default:
                calculationComponent = "COUNT(*) AS ct";
                log.debug("default orderByString = {}", orderByString);
                break;
        }
        String intervalColumn = "";
        AggTimeQueryHelper.AggTimeQuery issueModAggQuery;
        AGG_INTERVAL aggInterval = filter.getAggInterval();
        if (aggInterval == null) {
            aggInterval = AGG_INTERVAL.day;
        }
        Optional<String> additionalKey = Optional.empty();
        boolean sortAscending = true;
        switch (filter.getAcross()) {
            case label:
                groupByString = filter.getAcross().toString();
                selectDistinctString = "UNNEST(labels) AS label"; //unnest for components as that is an array
                break;
            case assignee:
                groupByString = filter.getAcross().toString();
                selectDistinctString = "UNNEST(assignees) AS assignee"; //unnest for components as that is an array
                break;
            case creator:
                groupByString = "creator_id, " + filter.getAcross().name();
                selectDistinctString = "creator_id, " + filter.getAcross().name();
                additionalKey = Optional.of(filter.getAcross().name());
                break;
            case repo_id:
            case project:
            case state:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                break;
            case issue_closed:
                conditions.get(ISSUES_TABLE).add("issue_closed_at IS NOT NULL");
            case issue_created:
            case issue_updated:
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmIssueFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }
                issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(filter.getAcross().toString() +
                                "_at", filter.getAcross().toString(), aggInterval.toString(), false, sortAscending);
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = issueModAggQuery.getGroupBy();
                if (MapUtils.isEmpty(filter.getSort()) || ScmIssueFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = issueModAggQuery.getOrderBy();
                }
                selectDistinctString = issueModAggQuery.getSelect();
                additionalKey = Optional.of(issueModAggQuery.getIntervalKey());
                log.debug("issue_updated orderByString = {}", orderByString);
                break;
            case first_comment:
                conditions.get(ISSUES_TABLE).add("first_comment_at IS NOT NULL");
                if (MapUtils.isNotEmpty(filter.getSort()) && ScmIssueFilter.DISTINCT.fromString(sortByKey) != null) {
                    sortAscending = sortOrder.equals(SortingOrder.ASC);
                }
                issueModAggQuery =
                        AggTimeQueryHelper.getAggTimeQuery(filter.getAcross().toString() +
                                "_at", filter.getAcross().toString(), aggInterval.toString(), false, sortAscending);
                intervalColumn = issueModAggQuery.getHelperColumn();
                groupByString = issueModAggQuery.getGroupBy();
                if (MapUtils.isEmpty(filter.getSort()) || ScmIssueFilter.DISTINCT.fromString(sortByKey) != null) {
                    orderByString = issueModAggQuery.getOrderBy();
                }
                selectDistinctString = issueModAggQuery.getSelect();
                additionalKey = Optional.of(issueModAggQuery.getIntervalKey());
                log.debug("first_comment orderByString = {}", orderByString);
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        if (conditions.get(ISSUES_TABLE).size() > 0) {
            issuesWhere = " WHERE " + String.join(" AND ", conditions.get(ISSUES_TABLE));
        }
        if (conditions.get(FINAL_TABLE).size() > 0) {
            finalWhere = " WHERE " + String.join(" AND ", conditions.get(FINAL_TABLE));
        }
        String resolutionTimeColumn = "";
        if (needResolutionTimeReport) {
            resolutionTimeColumn = ",(extract(epoch FROM COALESCE(issue_closed_at,TO_TIMESTAMP(" +
                    currentTime + ")))-extract(epoch FROM issue_created_at)) AS solve_time";
        }
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM (SELECT issues.*,"
                    + "(extract(epoch FROM COALESCE(first_comment_at,TO_TIMESTAMP(" + currentTime + ")))"
                    + "-extract(epoch FROM issue_created_at)) AS resp_time"
                    + (needResolutionTimeReport ? resolutionTimeColumn : "")
                    + intervalColumn
                    + " FROM " + company + "." + ISSUES_TABLE + " AS issues"
                    + issuesWhere
                    + " ) a" + finalWhere;
        }
        log.debug("before query orderByString = {}", orderByString);
        List<DbAggregationResult> results = List.of();
        Integer count = 0;
        if (pageSize > 0) {
            String intrSql = " SELECT " + selectDistinctString + "," + calculationComponent
                    + " FROM ( " + filterByProductSQL + ")x GROUP BY " + groupByString;
            String sql = intrSql + " ORDER BY " + orderByString
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.distinctIssueRowMapper(
                    filter.getAcross(), calculation, additionalKey));
            String countSql = "SELECT COUNT (*) FROM ("+
                    intrSql
                    + " ) i";
            count = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, count);
    }
    //endregion


    //region Contributor List
    public DbListResponse<DbScmContributorAgg> list(String company,
                                                    ScmContributorsFilter filter,
                                                    Map<String, SortingOrder> sortBy,
                                                    OUConfiguration ouConfig,
                                                    Integer pageNumber,
                                                    Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        boolean includeIssues = filter.isIncludeIssues();
        String filterByProductSQL = "";
        String integIdCondition = "";
        String prsWhere;
        String commitsWhere;
        String sql;
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (COMMITTERS_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "num_commits";
                })
                .orElse("num_commits");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        if (sortByKey.equalsIgnoreCase("committer")) {
            sortByKey = "COALESCE(committer, creator)";
        }

        //If across is author groupBy is author
        //If across is null or committer groupBy is committer
        boolean hasAuthor = ScmQueryUtils.checkAuthorsFilters(filter, ouConfig);
        boolean hasCommitter = ScmQueryUtils.checkCommittersFilters(filter, ouConfig);
        String commitsSelectAndGroupBy = (ScmContributorsFilter.DISTINCT.author == filter.getAcross()) ?
                "author, author_id" : "committer, committer_id";
        String commitsSelectDistinctString = (ScmContributorsFilter.DISTINCT.author == filter.getAcross()) ?
                "author" : "committer";
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForContributors(company, filter, params, ouConfig);
        }
        Map<String, List<String>> conditions = createCommitterAndRepoWhereClauseAndUpdateParams(company, params,
                filter.getRepoIds(), filter.getProjects(), filter.getAuthors(), filter.getCommitters(),
                filter.getIntegrationIds(), null, filter.getExcludeRepoIds(), filter.getExcludeProjects(),
                filter.getExcludeAuthors(), filter.getExcludeCommitters(), null, filter.getPartialMatch(),
                filter.getExcludePartialMatch(), filter.getDataTimeRange(), filter.getLocRange(), filter.getExcludeLocRange(), null, ouConfig);
        prsWhere = CollectionUtils.isEmpty(conditions.get(PRS_TABLE)) ? "" :
                " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        commitsWhere = CollectionUtils.isEmpty(conditions.get(COMMITS_TABLE)) ? "" :
                " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        String commitJiraIssuesMappingTableJoin = includeIssues ? ScmQueryUtils.sqlForCommitJiraIssuesMappingTableJoin(company, filter.getIntegrationIds(), null)
                : StringUtils.EMPTY;
        String commitWorkitemsMappingTableJoin = includeIssues ? ScmQueryUtils.sqlForCommitWorkItemsMappingTableJoin(company, filter.getIntegrationIds(), null)
                : StringUtils.EMPTY;
        String prJiraIssuesMappingTableJoin = includeIssues ? ScmQueryUtils.sqlForPrJiraIssuesMappingTableJoin(company, filter.getIntegrationIds(), null)
                : StringUtils.EMPTY;
        String prWorkitemsMappingTableJoin = includeIssues ? ScmQueryUtils.sqlForPrWorkitemsMappingTableJoin(company, filter.getIntegrationIds(), null) :
                StringUtils.EMPTY;

        List<DbScmContributorAgg> results = List.of();
        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT;
        String authorsSelect = hasAuthor ? ",scm_commits.author, scm_commits.author_id::varchar as author_id " : StringUtils.EMPTY;
        String committersSelect = hasCommitter  ? ",scm_commits.committer,scm_commits.committer_id::varchar as committer_id ": StringUtils.EMPTY;
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ",scm_pullrequests.creator, scm_pullrequests.creator_id::varchar as creator_id";
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            integIdCondition = " WHERE scm_files.integration_id IN (:integList) ";
            params.put("integList", filter.getIntegrationIds().stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        String fileTableJoin = " LEFT JOIN ( Select commit_sha,array_agg(DISTINCT(filetype)) AS file_type FROM " + company
                + ".scm_files scm_files INNER JOIN " + company + ".scm_file_commits scm_file_commits ON scm_file_commits.file_id = scm_files.id "
                + integIdCondition + " GROUP BY commit_sha) commit_files on commit_files.commit_sha = scm_commits.commit_sha ";
        String commitsSQL = "( SELECT " + commitsSelect + authorsSelect + committersSelect + ",commit_files.file_type FROM " + company + "." + COMMITS_TABLE +
                 fileTableJoin + ") scm_commits ";
        String prsSQL = "( SELECT" + prsSelect + creatorsSelect + " FROM " + company + "." + PRS_TABLE + ") scm_pullrequests ";
        String commitsIssuesWorkitemsSelect = includeIssues ? " , array_cat_agg(cjissues) AS cjissues, array_cat_agg(cworkitems) AS cworkitems" : StringUtils.EMPTY;
        String prsIssuesWorkItemsSelect = includeIssues ? " , array_cat_agg(prjissues) AS prjissues, array_cat_agg(prworkitems) AS prworkitems" : StringUtils.EMPTY;
        String finalCommitsPrsIssueWorkitemsSelect = includeIssues ? "prjissues,prworkitems,cjissues,cworkitems," : StringUtils.EMPTY;
        String prTableJoin = includeIssues ? " FULL OUTER JOIN pulls ON comms." + ((ScmContributorsFilter.DISTINCT.author == filter.getAcross()) ? "author_id" : "committer_id")
                + "= pulls.creator_id" : StringUtils.EMPTY;
        String pullsTableSelect = includeIssues ? ", pulls AS ( SELECT COUNT(*) AS num_prs,creator,creator_id "
                + prsIssuesWorkItemsSelect + " FROM "
                + prsSQL
                + prJiraIssuesMappingTableJoin
                + prWorkitemsMappingTableJoin
                + prsWhere
                + " GROUP BY creator,creator_id )" : StringUtils.EMPTY;
        String pullsTableFinalStmt = includeIssues ? "creator,creator_id,num_prs," : StringUtils.EMPTY;
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT num_commits, " +
                    " num_additions, num_deletions, num_changes, files, repos, " + finalCommitsPrsIssueWorkitemsSelect + pullsTableFinalStmt + commitsSelectDistinctString
                    + "," + ((ScmContributorsFilter.DISTINCT.author == filter.getAcross()) ? "author_id" : "committer_id") +
                    ",Array_length(anyarray_uniq(repos),1) AS num_repos FROM ( WITH comms AS ( "
                    + "SELECT array_cat_agg(file_type) AS files,array_cat_agg(repo_id) AS repos,COUNT(*) AS num_commits, SUM(additions) AS num_additions,"
                    + " SUM(deletions) AS num_deletions , SUM(changes) AS num_changes, "
                    + commitsSelectAndGroupBy + commitsIssuesWorkitemsSelect + " FROM "
                    + commitsSQL
                    + commitJiraIssuesMappingTableJoin
                    + commitWorkitemsMappingTableJoin
                    + commitsWhere
                    + " GROUP BY " + commitsSelectAndGroupBy + " )" + pullsTableSelect + " SELECT files,repos,num_commits,num_additions,num_deletions,num_changes," + pullsTableFinalStmt
                    + finalCommitsPrsIssueWorkitemsSelect + commitsSelectDistinctString + "," + ((ScmContributorsFilter.DISTINCT.author == filter.getAcross()) ? "author_id" : "committer_id") + " FROM ("
                    + " SELECT * FROM comms " + prTableJoin + " )"
                    + " x ) a ";
        }
        if (pageSize > 0) {
            String nullsPosition = SortingOrder.getNullsPosition(sortOrder);
            sql = filterByProductSQL + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS " + nullsPosition
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.committerAggRowMapper(filter, includeIssues));
        }
        String countSql = "SELECT COUNT(*) FROM ( " + filterByProductSQL + " ) a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }
    //endregion

    //region Contributor Union Sql
    public String getUnionSqlForContributors(String company, ScmContributorsFilter reqFilter, Map<String, Object> params, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return null;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmContributorsFilter scmContributorsFilter = scmContributorsFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createCommitterAndRepoWhereClauseAndUpdateParams(company, params,
                    scmContributorsFilter.getRepoIds(), scmContributorsFilter.getProjects(), scmContributorsFilter.getAuthors(),
                    scmContributorsFilter.getCommitters(), scmContributorsFilter.getIntegrationIds(), null,
                    scmContributorsFilter.getExcludeRepoIds(), scmContributorsFilter.getExcludeProjects(),
                    scmContributorsFilter.getExcludeAuthors(), scmContributorsFilter.getExcludeCommitters(), null,
                    scmContributorsFilter.getPartialMatch(), scmContributorsFilter.getExcludePartialMatch(), scmContributorsFilter.getDataTimeRange(),
                    scmContributorsFilter.getLocRange(), scmContributorsFilter.getExcludeLocRange(), String.valueOf(paramSuffix), ouConfig);
            listOfUnionSqls.add(scmContributorsFilterParser.getSqlStmt(company, conditions, scmContributorsFilter, paramSuffix));
            paramSuffix++;
        }
        return String.join(" UNION ", listOfUnionSqls);
    }

    //endregion
    public DbListResponse<DbScmRepoAgg> listFileTypes(String company,
                                                      ScmReposFilter filter,
                                                      Map<String, SortingOrder> sortBy,
                                                      OUConfiguration ouConfig,
                                                      Integer pageNumber,
                                                      Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        String commitsWhere = "";
        String prsWhere = "";
        String filterByProductSQL = "";
        String reposWhere = "";
        String integIdCondition = "";
        boolean needAuthors = ScmQueryUtils.checkAuthors(filter, ouConfig);
        boolean needCommitters = ScmQueryUtils.checkCommitters(filter, ouConfig);
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForRepos(company, filter, params, false, ouConfig);
        }
        Map<String, List<String>> conditions = createCommitterAndRepoWhereClauseAndUpdateParams(company, params,
                filter.getRepoIds(), filter.getProjects(), filter.getAuthors(), filter.getCommitters(),
                filter.getIntegrationIds(), filter.getFileTypes(), filter.getExcludeRepoIds(), filter.getExcludeProjects(),
                filter.getExcludeAuthors(), filter.getExcludeCommitters(), filter.getExcludeFileTypes(), filter.getPartialMatch(),
                Map.of(), filter.getDataTimeRange(), ImmutablePair.nullPair(), ImmutablePair.nullPair(), null, ouConfig);
        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        if (conditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        }
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (REPOS_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "num_commits";
                })
                .orElse("num_commits");
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            List<Integer> integsList = filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInt).collect(Collectors.toList());
            integIdCondition = " AND scm_files.integration_id IN (" + StringUtils.join(integsList, ",") + ")";
        }
        String fileTableJoinStmt = "WITH commit_files AS ( Select commit_sha,filetype AS file_type FROM " + company + ".scm_files scm_files INNER JOIN " +
                company + ".scm_file_commits scm_file_commits ON scm_file_commits.file_id = scm_files.id " +
                integIdCondition + " GROUP BY commit_sha, file_type)";
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String commitAuthorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String commitCommitterTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);
        String pullCreatorTableJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);

        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT;
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String authorsSelect = needAuthors ? ScmQueryUtils.AUTHORS_SELECT : StringUtils.EMPTY;
        String committersSelect = needCommitters ? ScmQueryUtils.COMMITTERS_SELECT : StringUtils.EMPTY;
        String filesSelect = " ,commit_files.file_type ";
        String scmPRFileTypeJoin = ScmQueryUtils.sqlForFileTableJoin(true);
        String commitsSQL = "( SELECT " + commitsSelect + authorsSelect + committersSelect + ", unnest(scm_commits.file_types) as file_type FROM " + company + "." + COMMITS_TABLE + commitAuthorTableJoin +
                commitCommitterTableJoin + ") scm_commits ";
        String prsSQL = "(" + fileTableJoinStmt + " SELECT  " + prsSelect + creatorsSelect + filesSelect + " FROM " + company + "." + PRS_TABLE +
                pullCreatorTableJoin + scmPRFileTypeJoin + ") scm_pullrequests ";

        String commitJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForCommitJiraIssuesMappingTableJoin(company, filter.getIntegrationIds(), null);
        String commitWorkitemsMappingTableJoin = ScmQueryUtils.sqlForCommitWorkItemsMappingTableJoin(company, filter.getIntegrationIds(), null);
        String prJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForPrJiraIssuesMappingTableJoin(company, filter.getIntegrationIds(), null);
        String prWorkitemsMappingTableJoin = ScmQueryUtils.sqlForPrWorkitemsMappingTableJoin(company, filter.getIntegrationIds(), null);

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM ( WITH comms AS ( SELECT COUNT(*) AS num_commits, SUM(additions) AS num_additions,"
                    + " SUM(deletions) AS num_deletions , SUM(changes) AS num_changes,file_type as file_type,"
                    + " array_cat_agg(cjissues) AS cjissues, array_cat_agg(cworkitems) AS cworkitems FROM "
                    + commitsSQL
                    + commitJiraIssuesMappingTableJoin
                    + commitWorkitemsMappingTableJoin
                    + commitsWhere
                    + " GROUP BY file_type ), pulls AS ( SELECT COUNT(*) AS num_prs,file_type as prsfile,"
                    + " array_cat_agg(prjissues) AS prjissues, array_cat_agg(prworkitems) AS prworkitems FROM "
                    + prsSQL
                    + prJiraIssuesMappingTableJoin
                    + prWorkitemsMappingTableJoin
                    + prsWhere
                    + " GROUP BY file_type ) SELECT file_type,num_commits,num_additions,"
                    + "num_deletions,num_changes,num_prs,prjissues,prworkitems,cjissues,cworkitems FROM ("
                    + " SELECT * FROM comms FULL OUTER JOIN pulls ON pulls.prsfile = comms.file_type )"
                    + " x" + reposWhere + " ) a ";
        }
        List<DbScmRepoAgg> results = List.of();
        if (pageSize > 0) {
            String nullsPosition = SortingOrder.getNullsPosition(sortOrder);
            String sql = filterByProductSQL + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS " + nullsPosition
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.fileTypesAggRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM ( " + filterByProductSQL
                + " ) a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }

    //region Repo List
    public DbListResponse<DbScmRepoAgg> list(String company,
                                             ScmReposFilter filter,
                                             Map<String, SortingOrder> sortBy,
                                             OUConfiguration ouConfig,
                                             Integer pageNumber,
                                             Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        String commitsWhere = "";
        String prsWhere = "";
        String filterByProductSQL = "";
        String reposWhere = "";
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForRepos(company, filter, params, true, ouConfig);
        }
        Map<String, List<String>> conditions = createCommitterAndRepoWhereClauseAndUpdateParams(company, params,
                filter.getRepoIds(), filter.getProjects(), filter.getAuthors(), filter.getCommitters(),
                filter.getIntegrationIds(), null, filter.getExcludeRepoIds(), filter.getExcludeProjects(),
                filter.getExcludeAuthors(), filter.getExcludeCommitters(), null, filter.getPartialMatch(),
                Map.of(), filter.getDataTimeRange(), ImmutablePair.nullPair(), ImmutablePair.nullPair(), null, ouConfig);
        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        if (conditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        }
        if (CollectionUtils.isNotEmpty(filter.getRepoIds())) {
            reposWhere = " WHERE cr IN (:scm_repo_ids) ";
        }
        String sortByKey = sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (REPOS_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        return entry.getKey();
                    }
                    return "num_commits";
                })
                .orElse("num_commits");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String commitAuthorTableJoin = ScmQueryUtils.sqlForAuthorTableJoin(company);
        String commitCommitterTableJoin = ScmQueryUtils.sqlForCommitterTableJoin(company);
        String pullCreatorTableJoin = ScmQueryUtils.sqlForCreatorTableJoin(company);

        String commitJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForCommitJiraIssuesMappingTableJoin(company, filter.getIntegrationIds(), null);
        String commitWorkitemsMappingTableJoin = ScmQueryUtils.sqlForCommitWorkItemsMappingTableJoin(company, filter.getIntegrationIds(), null);
        String prJiraIssuesMappingTableJoin = ScmQueryUtils.sqlForPrJiraIssuesMappingTableJoin(company, filter.getIntegrationIds(), null);
        String prWorkitemsMappingTableJoin = ScmQueryUtils.sqlForPrWorkitemsMappingTableJoin(company, filter.getIntegrationIds(), null);

        String commitsSelect = ScmQueryUtils.SCM_COMMITS_SELECT;
        String authorsSelect = ScmQueryUtils.AUTHORS_SELECT;
        String committersSelect = ScmQueryUtils.COMMITTERS_SELECT;
        String prsSelect = ScmQueryUtils.PRS_SELECT;
        String creatorsSelect = ScmQueryUtils.CREATORS_SQL;
        String commitsSQL = "( SELECT" + commitsSelect + authorsSelect + committersSelect + "FROM " + company + "." + COMMITS_TABLE + commitAuthorTableJoin +
                commitCommitterTableJoin + ") scm_commits ";
        String prsSQL = "( SELECT" + prsSelect + creatorsSelect + " FROM " + company + "." + PRS_TABLE + pullCreatorTableJoin + ") scm_pullrequests ";

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM ( WITH comms AS ( SELECT COUNT(*) AS num_commits, SUM(additions) AS num_additions,"
                    + " SUM(deletions) AS num_deletions , SUM(changes) AS num_changes, unnest(repo_id) AS cr,"
                    + " array_cat_agg(cjissues) AS cjissues, array_cat_agg(cworkitems) AS cworkitems FROM "
                    + commitsSQL
                    + commitJiraIssuesMappingTableJoin
                    + commitWorkitemsMappingTableJoin
                    + commitsWhere
                    + " GROUP BY unnest(repo_id) ), pulls AS ( SELECT COUNT(*) AS num_prs,unnest(repo_id) AS repo,"
                    + " array_cat_agg(prjissues) AS prjissues, array_cat_agg(prworkitems) AS prworkitems FROM "
                    + prsSQL
                    + prJiraIssuesMappingTableJoin
                    + prWorkitemsMappingTableJoin
                    + prsWhere
                    + " GROUP BY unnest(repo_id ) ) SELECT num_commits,num_additions,num_deletions,"
                    + " num_changes,cr,repo,num_prs,prjissues,prworkitems,cjissues,cworkitems FROM ("
                    + " SELECT * FROM comms FULL OUTER JOIN pulls ON pulls.repo = comms.cr )"
                    + " x" + reposWhere + " ) a ";
        }
        List<DbScmRepoAgg> results = List.of();
        if (pageSize > 0) {
            String nullsPosition = SortingOrder.getNullsPosition(sortOrder);
            String sql = filterByProductSQL + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS " + nullsPosition
                    + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params, DbScmConverters.repoAggRowMapper());
        }
        String countSql = "SELECT COUNT(*) FROM ( " + filterByProductSQL
                + " ) a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }
    //endregion

    //region Repo names List
    //TODO : Query "gitrepositories" table once the aggregation is done for all integration types
    public DbListResponse<String> listAllRepoNames(String company,
                                             ScmReposFilter filter,
                                             Map<String, SortingOrder> sortBy,
                                             OUConfiguration ouConfig,
                                             Integer pageNumber,
                                             Integer pageSize) {
        Map<String, Object> params = new HashMap<>();
        String commitsWhere = "";
        String prsWhere = "";

        String sortByKey = "repo";
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);

        Map<String, List<String>> conditions = createCommitterAndRepoWhereClauseAndUpdateParams(company, params,
                filter.getRepoIds(), filter.getProjects(), filter.getAuthors(), filter.getCommitters(),
                filter.getIntegrationIds(), null, filter.getExcludeRepoIds(), filter.getExcludeProjects(),
                filter.getExcludeAuthors(), filter.getExcludeCommitters(), null, filter.getPartialMatch(),
                Map.of(), filter.getDataTimeRange(), ImmutablePair.nullPair(), ImmutablePair.nullPair(), null, ouConfig);
        if (conditions.get(PRS_TABLE).size() > 0) {
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        }
        if (conditions.get(COMMITS_TABLE).size() > 0) {
            commitsWhere = " WHERE " + String.join(" AND ", conditions.get(COMMITS_TABLE));
        }

        String commitsSQL = "select unnest(repo_id) as repo from "+company+".scm_commits "+commitsWhere;
        String prsSQL = "select unnest(repo_id) as repo from "+company+".scm_pullrequests "+prsWhere;

        params.put("skip", pageNumber * pageSize);
        params.put("limit", pageSize);

        List<String> results = List.of();
        String sqlWithoutPagntn = "";
        if (pageSize > 0) {
            String nullsPosition = SortingOrder.getNullsPosition(sortOrder);
            sqlWithoutPagntn = "(" + commitsSQL + ")\n"
                    +"union\n"
                    +"(" + prsSQL + ")\n"
                    + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " NULLS " + nullsPosition;
            String sql = sqlWithoutPagntn + " OFFSET :skip LIMIT :limit";
            log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
            log.info("params = {}", params);
            results = template.query(sql, params,  (rs, rowNumber) -> rs.getString("repo"));
        }
        String countSql = "SELECT COUNT(*) FROM ( " + sqlWithoutPagntn
                + " ) a";
        log.info("countSql = {}", countSql);
        log.info("params = {}", params);
        Integer count = template.queryForObject(countSql, params, Integer.class);
        return DbListResponse.of(results, count);
    }
    //endregion

    //region Repo Union Sql
    public String getUnionSqlForRepos(String company, ScmReposFilter reqFilter, Map<String, Object> params,
                                      boolean isRepoType, OUConfiguration ouConfig) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return null;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmReposFilter scmReposFilter = scmReposFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createCommitterAndRepoWhereClauseAndUpdateParams(company, params,
                    scmReposFilter.getRepoIds(), scmReposFilter.getProjects(), scmReposFilter.getAuthors(),
                    scmReposFilter.getCommitters(), scmReposFilter.getIntegrationIds(), scmReposFilter.getFileTypes(),
                    scmReposFilter.getExcludeRepoIds(), scmReposFilter.getExcludeProjects(),
                    scmReposFilter.getExcludeAuthors(), scmReposFilter.getExcludeCommitters(), scmReposFilter.getExcludeFileTypes(),
                    scmReposFilter.getPartialMatch(), Map.of(), scmReposFilter.getDataTimeRange(), ImmutablePair.nullPair(), ImmutablePair.nullPair(), String.valueOf(paramSuffix), ouConfig);
            listOfUnionSqls.add(scmReposFilterParser.getSqlStmt(company, conditions, scmReposFilter, paramSuffix, isRepoType));
            paramSuffix++;
        }
        return String.join(" UNION ", listOfUnionSqls);
    }
    //endregion

    //region Misc

    private String getPrsCountSortByKey(Map<String, SortingOrder> sortBy, String across, boolean valuesOnly) {
        if (MapUtils.isEmpty(sortBy)) {
            return "ct";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmPrFilter.DISTINCT.fromString(entry.getKey()) != null) {
                        if (!across.equals(entry.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return entry.getKey();
                    }
                    if (ScmPrFilter.CALCULATION.count.toString().equalsIgnoreCase(entry.getKey())) {
                        return "ct";
                    }
                    if (!valuesOnly) {
                        if (ScmPrSorting.MEAN_AUTHOR_RESPONSE_TIME.equalsIgnoreCase(entry.getKey())) {
                            return "mean";
                        }
                    if (ScmPrSorting.MEDIAN_AUTHOR_RESPONSE_TIME.equalsIgnoreCase(entry.getKey())) {
                            return "median";
                        }
                    }
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                })
                .orElse("ct");
    }

    private String getPrsDurationSortByKey(Map<String, SortingOrder> sortBy, String across) {
        if (MapUtils.isEmpty(sortBy)) {
            return "ct";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                            if (ScmPrFilter.DISTINCT.fromString(entry.getKey()) != null) {
                                if (!across.equals(entry.getKey())) {
                                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                                }
                                return entry.getKey();
                            }
                            return "ct";
                        }
                )
                .orElse("ct");
    }

    private String getPrsCountOrderByString(Map<String, SortingOrder> sortBy, String sortByKey) {
        SortingOrder sortOrder = getScmSortOrder(sortBy);
        if (ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
            String[] acrossArrayFields = {"repo_id", "label", "assignee"};
            if (Arrays.stream(acrossArrayFields).anyMatch(across -> across.equalsIgnoreCase(sortByKey))) {
                if ("repo_id".equalsIgnoreCase(sortByKey)) {
                    return "lower(repo_ids) " + sortOrder;
                }
                return "lower(unnest(" + sortByKey + "s)) " + sortOrder;
            }
            return "lower(" + sortByKey + ") " + sortOrder;
        }
        return sortByKey + " " + sortOrder;
    }

    private String getPrsCountTrendOrderByString(Map<String, SortingOrder> sortBy, String sortByKey) {
        SortingOrder sortOrder = getScmSortOrder(sortBy);
        if (MapUtils.isNotEmpty(sortBy)) {
            if (ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                return "trend " + sortOrder;
            } else if (sortByKey.equals("ct")) {
                return sortByKey + " " + sortOrder;
            }
        }
        return "trend ASC";
    }

    private String getPrsDurationOrderByString(Map<String, SortingOrder> sortBy, String across) {
        if (MapUtils.isNotEmpty(sortBy)) {
            String sortByKey = sortBy.entrySet()
                    .stream().findFirst()
                    .map(entry -> {
                        if (ScmPrFilter.DISTINCT.fromString(entry.getKey()) != null) {
                            if (!across.equals(entry.getKey())) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                            }
                            return entry.getKey();
                        }
                        if (Objects.isNull(ScmPrFilter.CALCULATION.fromString(entry.getKey()))) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return "md";
                    })
                    .orElse("md");
            SortingOrder sortOrder = sortBy.entrySet()
                    .stream().findFirst()
                    .map(entry -> {
                        if (ScmPrFilter.DISTINCT.fromString(entry.getKey()) != null) {
                            return sortBy.getOrDefault(entry.getKey(), SortingOrder.ASC);
                        }
                        return sortBy.getOrDefault(entry.getKey(), SortingOrder.DESC);
                    })
                    .orElse(SortingOrder.DESC);
            if (ScmPrFilter.DISTINCT.fromString(sortByKey) != null) {
                return "trend " + sortOrder;
            }
            return sortByKey + " " + sortOrder;
        }
        return "trend ASC";
    }

    private String getCommitsTrendOrderByString(Map<String, SortingOrder> sortBy, String sortByKey) {
        SortingOrder sortOrder = getScmSortOrder(sortBy);
        if (MapUtils.isNotEmpty(sortBy)) {
            if (ScmCommitFilter.DISTINCT.fromString(sortByKey) != null) {
                return "trend " + sortOrder;
            } else if (sortByKey.equals("ct")) {
                return sortByKey + " " + sortOrder;
            }
        }
        return "trend ASC";
    }

    private String getCommitsOrderByString(Map<String, SortingOrder> sortBy, String sortByKey) {
        SortingOrder sortOrder = getScmSortOrder(sortBy);
        if (ScmCommitFilter.DISTINCT.fromString(sortByKey) != null) {
            if ("repo_id".equalsIgnoreCase(sortByKey)) {
                return "lower(repo_ids) " + sortOrder;
            }
            return "lower(" + sortByKey + ") " + sortOrder;
        }
        return sortByKey + " " + sortOrder;
    }

    private String getCollabReportOrderByString(Map<String, SortingOrder> sortBy) {
        SortingOrder sortOrder = getScmSortOrder(sortBy);
        if (MapUtils.isEmpty(sortBy)) {
            return " ct " + sortOrder;
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (!COLLAB_REPORT_SORTABLE_COLUMNS.contains(entry.getKey())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                    }
                    return entry.getKey() + " " + sortOrder;
                })
                .orElse(" ct " + sortOrder);
    }

    private String getCommitsSortByKey(Map<String, SortingOrder> sortBy, String across, ScmCommitFilter.CALCULATION calculation) {
        if (MapUtils.isEmpty(sortBy)) {
            return "";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmCommitFilter.DISTINCT.fromString(entry.getKey()) != null) {
                        if (!across.equals(entry.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return entry.getKey();
                    }
                    if (Objects.isNull(ScmCommitFilter.CALCULATION.fromString(entry.getKey())) || !entry.getKey().equals(calculation.toString())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                    } else {
                        if (calculation.equals(ScmCommitFilter.CALCULATION.count)) {
                            return "ct";
                        } else if (List.of(ScmCommitFilter.CALCULATION.commit_count, ScmCommitFilter.CALCULATION.commit_days).contains(calculation)) {
                            return "median";
                        }
                        return "";
                    }
                })
                .orElse("");
    }

    private String getIssuesOrderByString(String sortByKey, SortingOrder sortOrder) {
        if (ScmIssueFilter.DISTINCT.fromString(sortByKey) != null) {
            String[] acrossArrayFields = {"label", "assignee"};
            if (Arrays.stream(acrossArrayFields).anyMatch(across -> across.equalsIgnoreCase(sortByKey))) {
                return "lower(unnest(" + sortByKey + "s)) " + sortOrder;
            }
            return "lower(" + sortByKey + ") " + sortOrder;
        }
        return sortByKey + " " + sortOrder;
    }

    private String getIssuesSortByKey(Map<String, SortingOrder> sortBy,
                                      ScmIssueFilter.CALCULATION calculation,
                                      String across) {
        if (MapUtils.isEmpty(sortBy)) {
            switch (calculation) {
                case response_time:
                case resolution_time:
                    return "md";
                default:
                    return "ct";
            }
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmIssueFilter.DISTINCT.fromString(entry.getKey()) != null) {
                        if (!across.equals(entry.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return entry.getKey();
                    }
                    if (!calculation.toString().equalsIgnoreCase(entry.getKey())) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                    }
                    switch (calculation) {
                        case response_time:
                        case resolution_time:
                            return "md";
                        default:
                            return "ct";
                    }
                })
                .orElse("ct");
    }

    protected Map<String, List<String>> createFilesWhereClauseAndUpdateParams(Map<String, Object> params,
                                                                              List<String> repoIds,
                                                                              List<String> projects,
                                                                              List<String> integrationIds,
                                                                              List<String> excludeRepoIds,
                                                                              List<String> excludeProjects,
                                                                              String filename,
                                                                              String module,
                                                                              Map<String, Map<String, String>> partialMatch,
                                                                              Long commitStartTime,
                                                                              Long commitEndTime,
                                                                              String paramSuffix) {
        List<String> fileTableConditions = new ArrayList<>();
        List<String> fileCommitsTableConditions = new ArrayList<>();
        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        List<String> scmCommitJiraMappingsTableConditions = new ArrayList<>();
        List<String> scmCommitWorkItemMappingsTableConditions = new ArrayList<>();

        List<String> scmPRsTableConditions = new ArrayList<>();
        List<String> scmPRJiraMappingsTableConditions = new ArrayList<>();
        List<String> scmPRWorkItemMappingsTableConditions = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(repoIds)) {
            fileTableConditions.add("repo_id IN (:scf_repo_ids" + paramSuffixString + ")");
            params.put("scf_repo_ids" + paramSuffixString, repoIds);
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            fileTableConditions.add("repo_id NOT IN (:exclude_scf_repo_ids" + paramSuffixString + ")");
            params.put("exclude_scf_repo_ids" + paramSuffixString, excludeRepoIds);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            fileTableConditions.add("project IN (:projects" + paramSuffixString + ")");
            params.put("projects" + paramSuffixString, projects);
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            fileTableConditions.add("project NOT IN (:exclude_projects" + paramSuffixString + ")");
            params.put("exclude_projects" + paramSuffixString, excludeProjects);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            fileTableConditions.add("integration_id IN (:scf_integration_ids" + paramSuffixString + ")");
            scmCommitJiraMappingsTableConditions.add("scm_integ_id IN (:scf_integration_ids" + paramSuffixString + ")");
            scmCommitWorkItemMappingsTableConditions.add("scm_integration_id IN (:scf_integration_ids" + paramSuffixString + ")");

            scmPRsTableConditions.add("integration_id IN (:scf_integration_ids" + paramSuffixString + ")");
            scmPRJiraMappingsTableConditions.add("scm_integration_id IN (:scf_integration_ids" + paramSuffixString + ")");
            scmPRWorkItemMappingsTableConditions.add("scm_integration_id IN (:scf_integration_ids" + paramSuffixString + ")");

            params.put("scf_integration_ids" + paramSuffixString,
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (StringUtils.isNotEmpty(filename)) {
            fileTableConditions.add("filename LIKE :scf_filename" + paramSuffixString + "");
            params.put("scf_filename" + paramSuffixString, "%" + filename + "%");
        }

        if (StringUtils.isNotEmpty(module)) {
            fileTableConditions.add("filename LIKE :scf_filename" + paramSuffixString + "");
            params.put("scf_filename" + paramSuffixString, module + "/%");
        }

        if (MapUtils.isNotEmpty(partialMatch)) {
            ScmQueryUtils.createPartialMatchFilter(partialMatch, fileTableConditions, FILES_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), params, "", true);
        }

        if (commitStartTime != null) {
            fileCommitsTableConditions.add("committed_at >= TO_TIMESTAMP(" + commitStartTime + ")");
        }
        if (commitEndTime != null) {
            fileCommitsTableConditions.add("committed_at <= TO_TIMESTAMP(" + commitEndTime + ")");
        }
        return Map.of(FILES_TABLE, fileTableConditions,
                FILE_COMMITS_TABLE, fileCommitsTableConditions,
                COMMIT_JIRA_TABLE, scmCommitJiraMappingsTableConditions,
                COMMIT_WORKITEM_TABLE, scmCommitWorkItemMappingsTableConditions,
                PRS_TABLE, scmPRsTableConditions,
                PULLREQUESTS_JIRA_TABLE, scmPRJiraMappingsTableConditions,
                PULLREQUESTS_WORKITEM_TABLE, scmPRWorkItemMappingsTableConditions);
    }

    public DbListResponse<DbAggregationResult> listModules(String company,
                                                           ScmFilesFilter filter,
                                                           Map<String, SortingOrder> sortBy) {
        return listModules(company, filter, sortBy, null, null);
    }


    public DbListResponse<DbAggregationResult> listModules(String company,
                                                           ScmFilesFilter filter,
                                                           Map<String, SortingOrder> sortBy,
                                                           Integer page,
                                                           Integer pageSize) {

        Map<String, Object> params = new HashMap<>();
        String sortByKey = "no_of_commits";
        String filterByProductSQL = "";
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.DESC);
        String filesWhere = "";
        String fileCondition = "";
        String fileCommitsWhere = " WHERE file_id = files.id ";
        String path;
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForModules(company, filter, params);
        }
        Map<String, List<String>> conditions = createFilesWhereClauseAndUpdateParams(params,
                filter.getRepoIds(), filter.getProjects(), filter.getIntegrationIds(),
                filter.getExcludeRepoIds(), filter.getExcludeProjects(), filter.getFilename(),
                filter.getModule(), filter.getPartialMatch(), filter.getCommitStartTime(),
                filter.getCommitEndTime(), null);
        path = StringUtils.isEmpty(filter.getModule())
                ? "filename" : "substring(filename from " + (filter.getModule().length() + 2) + ")";
        if (filter.getListFiles() != null && !filter.getListFiles()) {
            fileCondition = " where position('/' IN " + path + " ) > 0 ";
        }
        if (conditions.get(FILES_TABLE).size() > 0) {
            filesWhere = " WHERE " + String.join(" AND ", conditions.get(FILES_TABLE));
        }
        if (conditions.get(FILE_COMMITS_TABLE).size() > 0) {
            fileCommitsWhere = fileCommitsWhere + " AND " + String.join(" AND ", conditions.get(FILE_COMMITS_TABLE));
        }
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM (" + "SELECT split_part(" + path + ", '/', 1) as root_module, repo_id, project, SUM(num_commits) " +
                    "as no_of_commits FROM ( " + " SELECT files.*,"
                    + "(SELECT COUNT(*) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS num_commits,"
                    + "(SELECT SUM(change) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS changes,"
                    + "(SELECT SUM(addition) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS additions,"
                    + "(SELECT SUM(deletion) FROM " + company + ".scm_file_commits " + fileCommitsWhere + " ) AS deletions"
                    + " FROM " + company + "." + FILES_TABLE + " AS files"
                    + filesWhere + " ) s_files" + fileCondition + " GROUP BY repo_id, project, root_module" + ") a ";
        }
        String baseSql = filterByProductSQL;
        String sql = baseSql + " ORDER BY " + sortByKey + " " + sortOrder.toString() + " , root_module asc";
        var countSql = "";
        if (page != null && pageSize != null) {
            page = page >= 0 ? page : 0;
            pageSize = pageSize > 0 ? pageSize : 100;
            params.put("skip", page * pageSize);
            params.put("limit", pageSize);
            var limit =" LIMIT :limit OFFSET :skip ";
            sql = sql + limit;
            countSql = MessageFormat.format("SELECT COUNT(*) FROM ({0}) AS t_count", baseSql);
        }
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.moduleRowMapper("root_module"));
        var totalCount = results.size();
        if (StringUtils.isNotEmpty(countSql)) {
            totalCount = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, totalCount);
    }

    public String getUnionSqlForModules(String company, ScmFilesFilter reqFilter, Map<String, Object> params) {
        Map<String, List<String>> conditions;
        int paramSuffix = 1;
        String unionSql = "";
        List<String> listOfUnionSqls = new ArrayList<>();
        Map<Integer, Map<String, Object>> integFiltersMap = null;
        try {
            integFiltersMap = (reqFilter.getOrgProductIds() == null) ? Map.of() :
                    scmFilterParserCommons.getProductFilters(company, reqFilter.getOrgProductIds());
        } catch (SQLException e) {
            log.warn("Cannot fetch list of filter for company {}, {}", company, e);
        }
        if (MapUtils.isEmpty(integFiltersMap) || integFiltersMap.values().stream().allMatch(Objects::isNull)) {
            return unionSql;
        }
        for (Integer integ : integFiltersMap.keySet()) {
            ScmFilesFilter scmFilesFilter = scmFilesFilterParser.merge(integ, reqFilter, integFiltersMap.get(integ));
            conditions = createFilesWhereClauseAndUpdateParams(params,
                    scmFilesFilter.getRepoIds(), scmFilesFilter.getProjects(), scmFilesFilter.getIntegrationIds(),
                    scmFilesFilter.getExcludeRepoIds(), scmFilesFilter.getExcludeProjects(),
                    scmFilesFilter.getFilename(), scmFilesFilter.getModule(), scmFilesFilter.getPartialMatch(),
                    scmFilesFilter.getCommitStartTime(), scmFilesFilter.getCommitEndTime(), String.valueOf(paramSuffix++));
            listOfUnionSqls.add(scmFilesFilterParser.getSqlStmtForModules(company, conditions, scmFilesFilter));
        }
        return String.join(" UNION ", listOfUnionSqls);
    }

    /**
     * Returns all the files
     */
    public DbListResponse<DbAggregationResult> groupByAndCalculateFiles(String company, ScmFilesFilter filter) {
        return groupByAndCalculateFiles(company, filter, null, null);
    }

    /**
     * Returns only the specified page of the results
     */
    public DbListResponse<DbAggregationResult> groupByAndCalculateFiles(String company, ScmFilesFilter filter, Integer page, Integer pageSize) {
        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");
        Map<String, Object> params = new HashMap<>();
        String calculationComponent, selectDistinctString, groupByString, orderByString;
        String filesWhere = "";
        String fileCommitsWhere = "";
        String filterByProductSQL = "";
        ScmFilesFilter.CALCULATION calculation = filter.getCalculation();
        if (calculation == null) {
            calculation = ScmFilesFilter.CALCULATION.count;
        }
        if (CollectionUtils.isNotEmpty(filter.getOrgProductIds())) {
            filterByProductSQL = getUnionSqlForFiles(company, filter, params, false);
        }
        Map<String, List<String>> conditions = createFilesWhereClauseAndUpdateParams(params,
                filter.getRepoIds(), filter.getProjects(), filter.getIntegrationIds(),
                filter.getExcludeRepoIds(), filter.getExcludeProjects(), filter.getFilename(),
                filter.getModule(), filter.getPartialMatch(), filter.getCommitStartTime(),
                filter.getCommitEndTime(), null);
        if (conditions.get(FILES_TABLE).size() > 0) {
            filesWhere = " WHERE " + String.join(" AND ", conditions.get(FILES_TABLE));
        }
        if (conditions.get(FILE_COMMITS_TABLE).size() > 0) {
            fileCommitsWhere = " WHERE " + String.join(" AND ", conditions.get(FILE_COMMITS_TABLE));
        }

        switch (calculation) {
            default:
                orderByString = getFilesOrderByString(filter.getSort(), filter.getAcross().toString());
                calculationComponent = "COUNT(*) as ct";
                break;
        }
        switch (filter.getAcross()) {
            case repo_id:
            case project:
                groupByString = filter.getAcross().toString();
                selectDistinctString = filter.getAcross().toString();
                break;
            default:
                Validate.notNull(null, "Invalid across field provided.");
                return DbListResponse.of(List.of(), 0);
        }
        if (StringUtils.isEmpty(filterByProductSQL)) {
            filterByProductSQL = "SELECT * FROM (SELECT files.id,files.repo_id,files.project,commits.* FROM "
                    + company + "." + FILES_TABLE + " AS files"
                    + " INNER JOIN ( SELECT COUNT(*) as num_commits,file_id FROM "
                    + company + "." + FILE_COMMITS_TABLE
                    + fileCommitsWhere + " GROUP BY file_id ) AS commits"
                    + " ON files.id = commits.file_id "
                    + filesWhere + ") a";
        }
        var countSql = "";
        var baseSql = "SELECT " + selectDistinctString + "," + calculationComponent
                + " FROM ( " + filterByProductSQL
                + " ) a GROUP BY " + groupByString;
        String sql = baseSql + " ORDER BY " + orderByString;
        if (page != null || pageSize != null) {
            page = page >= 0 ? page : 0;
            pageSize = pageSize > 0 ? pageSize : 100;
            params.put("skip", page * pageSize);
            params.put("limit", pageSize);
            String limit =" LIMIT :limit OFFSET :skip ";
            sql = sql + limit;
            countSql = MessageFormat.format("SELECT COUNT(*) FROM ({0}) as t_count", baseSql);
        }
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbAggregationResult> results = template.query(sql, params, DbScmConverters.distinctFilesRowMapper(
                filter.getAcross()));
        var totalCount = results.size();
        if (StringUtils.isNotEmpty(countSql)) {
            totalCount = template.queryForObject(countSql, params, Integer.class);
        }
        return DbListResponse.of(results, totalCount);
    }

    private String getFilesOrderByString(Map<String, SortingOrder> sortBy, String across) {
        String sortByKey = getFilesSortByKey(sortBy, across);
        SortingOrder sortOrder = getScmSortOrder(sortBy);
        if (ScmFilesFilter.DISTINCT.fromString(sortByKey) != null) {
            return "lower(" + sortByKey + ") " + sortOrder;
        }
        return sortByKey + " " + sortOrder;
    }

    private String getFilesSortByKey(Map<String, SortingOrder> sortBy, String across) {
        if (MapUtils.isEmpty(sortBy)) {
            return "ct";
        }
        return sortBy.entrySet()
                .stream().findFirst()
                .map(entry -> {
                    if (ScmFilesFilter.DISTINCT.fromString(entry.getKey()) != null) {
                        if (!across.equals(entry.getKey())) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                        }
                        return entry.getKey();
                    }
                    if (Objects.isNull(ScmFilesFilter.CALCULATION.fromString(entry.getKey()))) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid sort field " + entry.getKey());
                    }
                    return "ct";
                })
                .orElse("ct");
    }

    protected Map<String, List<String>> createCommitterAndRepoWhereClauseAndUpdateParams(String company, Map<String, Object> params,
                                                                                         List<String> repoIds,
                                                                                         List<String> projects,
                                                                                         List<String> authors,
                                                                                         List<String> committers,
                                                                                         List<String> integrationIds,
                                                                                         List<String> fileTypes,
                                                                                         List<String> excludeRepoIds,
                                                                                         List<String> excludeProjects,
                                                                                         List<String> excludeAuthors,
                                                                                         List<String> excludeCommitters,
                                                                                         List<String> excludeFileTypes,
                                                                                         Map<String, Map<String, String>> partialMatch,
                                                                                         Map<String, Map<String, String>> excludePartialMatch,
                                                                                         ImmutablePair<Long, Long> dataTimeRange,
                                                                                         ImmutablePair<Long, Long> locRange,
                                                                                         ImmutablePair<Long, Long> excludeLocRange,
                                                                                         String paramSuffix,
                                                                                         OUConfiguration ouConfig) {
        List<String> prConditions = new ArrayList<>();
        List<String> commitConditions = new ArrayList<>();
        String paramSuffixString = StringUtils.isEmpty(paramSuffix) ? "" : "_" + paramSuffix;
        if (CollectionUtils.isNotEmpty(repoIds)) {
            commitConditions.add("repo_id && ARRAY[ :scm_repo_ids" + paramSuffixString + " ]::varchar[]");
            prConditions.add("repo_id && ARRAY[ :scm_repo_ids" + paramSuffixString + " ]::varchar[]");
            params.put("scm_repo_ids" + paramSuffixString, repoIds);
        }
        if (CollectionUtils.isNotEmpty(excludeRepoIds)) {
            commitConditions.add("NOT repo_id && ARRAY[ :exclude_scm_repo_ids" + paramSuffixString + " ]::varchar[]");
            prConditions.add("NOT repo_id && ARRAY[ :exclude_scm_repo_ids" + paramSuffixString + " ]::varchar[]");
            params.put("exclude_scm_repo_ids" + paramSuffixString, excludeRepoIds);
        }
        if (CollectionUtils.isNotEmpty(fileTypes)) {
            commitConditions.add("file_type IN (:file_types" + paramSuffixString + ")");
            prConditions.add("file_type IN (:file_types" + paramSuffixString + ")");
            params.put("file_types" + paramSuffixString, fileTypes);
        }
        if (CollectionUtils.isNotEmpty(excludeFileTypes)) {
            commitConditions.add("file_type NOT IN (:exclude_file_types" + paramSuffixString + ")");
            prConditions.add("file_type NOT IN (:exclude_file_types" + paramSuffixString + ")");
            params.put("exclude_file_types" + paramSuffixString, excludeFileTypes);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            commitConditions.add("project IN (:scm_projects" + paramSuffixString + ")");
            prConditions.add("project IN (:scm_projects" + paramSuffixString + ")");
            params.put("scm_projects" + paramSuffixString, projects);
        }
        if (CollectionUtils.isNotEmpty(excludeProjects)) {
            commitConditions.add("project NOT IN (:exclude_scm_projects" + paramSuffixString + ")");
            prConditions.add("project NOT IN (:exclude_scm_projects" + paramSuffixString + ")");
            params.put("exclude_scm_projects" + paramSuffixString, excludeProjects);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            commitConditions.add("integration_id IN (:scm_integration_ids" + paramSuffixString + ")");
            prConditions.add("integration_id IN (:scm_integration_ids" + paramSuffixString + ")");
            params.put("scm_integration_ids" + paramSuffixString,
                    integrationIds.stream().map(NumberUtils::toInt).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(committers) || OrgUnitHelper.doesOuConfigHaveReposAndCommittersCreators(ouConfig)) { // OU: committer/creator
            var columnName = "committer_id";
            var columnNameParam = columnName + paramSuffixString;
            if (OrgUnitHelper.doesOuConfigHaveReposAndCommittersCreators(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    commitConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                    prConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", "creator_id", usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(committers)) {
                TeamUtils.addUsersCondition(company, commitConditions, params, "committer_id", columnNameParam,
                        false, committers, SCM_APPLICATIONS);
                columnNameParam = "creator_id" + paramSuffixString;
                TeamUtils.addUsersCondition(company, prConditions, params, "creator_id", columnNameParam,
                        false, committers, SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(excludeCommitters)) {
            String columnNameParam = "exclude_committer_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, commitConditions, params, "committer_id", columnNameParam,
                    false, excludeCommitters, SCM_APPLICATIONS, true);
            columnNameParam = "exclude_creator_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, prConditions, params, "creator_id", columnNameParam,
                    false, excludeCommitters, SCM_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(authors) || OrgUnitHelper.doesOuConfigHaveReposAndCommittersAuthors(ouConfig)) { // OU: author
            var columnName = "author_id";
            var columnNameParam = columnName + paramSuffixString;
            if (OrgUnitHelper.doesOuConfigHaveReposAndCommittersAuthors(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params, IntegrationType.getSCMIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    commitConditions.add(MessageFormat.format("{0}::uuid IN (SELECT id FROM ({1}) l)", columnName, usersSelect));
                }
            } else if (CollectionUtils.isNotEmpty(authors)) {
                TeamUtils.addUsersCondition(company, commitConditions, params, "author_id", columnNameParam,
                        false, authors, SCM_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(excludeAuthors)) {
            String columnNameParam = "exclude_author_id" + paramSuffixString;
            TeamUtils.addUsersCondition(company, commitConditions, params, "author_id", columnNameParam,
                    false, excludeAuthors, SCM_APPLICATIONS, true);
        }
        if (dataTimeRange != null) {
            if (dataTimeRange.getLeft() != null) {
                commitConditions.add("committed_at > TO_TIMESTAMP(" + dataTimeRange.getLeft() + ")");
                prConditions.add("pr_created_at > TO_TIMESTAMP(" + dataTimeRange.getLeft() + ")");
            }
            if (dataTimeRange.getRight() != null) {
                commitConditions.add("committed_at < TO_TIMESTAMP(" + dataTimeRange.getRight() + ")");
                prConditions.add("pr_created_at < TO_TIMESTAMP(" + dataTimeRange.getRight() + ")");
            }
        }
        if (locRange != null) {
            if (locRange.getLeft() != null) {
                commitConditions.add("loc > " + locRange.getLeft());
            }
            if (locRange.getRight() != null) {
                commitConditions.add("loc < " + locRange.getRight());
            }
        }
        if (excludeLocRange != null) {
            if (excludeLocRange.getLeft() != null && excludeLocRange.getRight() != null) {
                commitConditions.add("NOT ( loc  > " + excludeLocRange.getLeft() + " AND loc < " + excludeLocRange.getRight() + ")");
            }
            if (excludeLocRange.getLeft() != null && excludeLocRange.getRight() == null) {
                commitConditions.add("loc < " + excludeLocRange.getLeft());
            }
            if (excludeLocRange.getLeft() == null && excludeLocRange.getRight() != null) {
                commitConditions.add("loc > " + excludeLocRange.getRight());
            }
        }
        if (MapUtils.isNotEmpty(partialMatch)) {
            ScmQueryUtils.createPartialMatchFilter(partialMatch, commitConditions, CONTRIBUTORS_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), params, "", true);
        }
        if (MapUtils.isNotEmpty(excludePartialMatch)) {
            ScmQueryUtils.createPartialMatchFilter(partialMatch, commitConditions, CONTRIBUTORS_PARTIAL_MATCH_COLUMNS, Collections.emptySet(), params, "", false);
        }
        return Map.of(COMMITS_TABLE, commitConditions,
                PRS_TABLE, prConditions);
    }
    //endregion

    //region PR Labels
    private MapSqlParameterSource constructPRLabelsListParameterSource(final List<String> criterias, final List<Integer> integrationIds, final List<UUID> scmPRIds, final Map<String, Map<String, String>> partialMatchMap) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            criterias.add("pr.integration_id in (:integration_ids)");
            params.addValue("integration_ids", integrationIds);
        }
        if (CollectionUtils.isNotEmpty(scmPRIds)) {
            criterias.add("prl.scm_pullrequest_id in (:scm_pullrequest_ids)");
            params.addValue("scm_pullrequest_ids", scmPRIds);
        }
        CriteriaUtils.addPartialMatchClause(partialMatchMap, criterias, null, params, PR_LABELS_PARTIAL_MATCH_COLUMNS, PR_LABELS_PARTIAL_MATCH_ARRAY_COLUMNS, "prl");
        return params;
    }

    public DbListResponse<DbScmPRLabel> listPRLabelsByFilter(String company, Integer pageNumber, Integer pageSize, final List<Integer> integrationIds, final List<UUID> scmPRIds, final Map<String, Map<String, String>> partialMatchMap) {
        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = constructPRLabelsListParameterSource(criterias, integrationIds, scmPRIds, partialMatchMap);
        String baseWhereClause = (CollectionUtils.isEmpty(criterias)) ? "" : " WHERE " + String.join(" AND ", criterias) + "\n";

        String selectSqlBase = "SELECT prl.* \n" +
                "FROM " + company + "." + PR_LABELS_TABLE + " as prl \n" +
                "JOIN " + company + "." + PRS_TABLE + " as pr on pr.id = prl.scm_pullrequest_id \n" +
                baseWhereClause;
        String selectSql = selectSqlBase + " ORDER BY label_added_at desc" + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmPRLabel> scmPrLabels = template.query(selectSql, params, DbScmConverters.mapPRLabel());
        log.info("scmPrLabels.size() = {}", scmPrLabels.size());
        if (scmPrLabels.size() > 0) {
            totCount = scmPrLabels.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (scmPrLabels.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(scmPrLabels, totCount);
    }

    public DbListResponse<DbScmPRLabelLite> listUniquePRLabelsByFilter(String company, Integer pageNumber, Integer pageSize, final List<Integer> integrationIds, final List<UUID> scmPRIds, final Map<String, Map<String, String>> partialMatchMap, final Map<String, SortingOrder> sortBy) {
        String sortByKey = MapUtils.emptyIfNull(sortBy).entrySet()
                .stream().findFirst()
                .map(entry -> {
                    return (!PR_LABELS_SORTABLE_COLUMNS.contains(entry.getKey())) ? "name" : entry.getKey();
                })
                .orElse("name");
        SortingOrder sortOrder = sortBy.getOrDefault(sortByKey, SortingOrder.ASC);

        List<String> criterias = new ArrayList<>();
        MapSqlParameterSource params = constructPRLabelsListParameterSource(criterias, integrationIds, scmPRIds, partialMatchMap);
        String baseWhereClause = (CollectionUtils.isEmpty(criterias)) ? "" : " WHERE " + String.join(" AND ", criterias) + "\n";

        String selectSqlBase = "SELECT distinct(prl.name) \n" +
                "FROM " + company + "." + PR_LABELS_TABLE + " as prl \n" +
                "JOIN " + company + "." + PRS_TABLE + " as pr on pr.id = prl.scm_pullrequest_id \n" +
                baseWhereClause;
        String selectSql = selectSqlBase + " ORDER BY " + sortByKey + " " + sortOrder + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" + selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        List<DbScmPRLabelLite> scmPrLabels = template.query(selectSql, params, DbScmConverters.mapPRLabelLite());
        log.info("scmPrLabels.size() = {}", scmPrLabels.size());
        if (scmPrLabels.size() > 0) {
            totCount = scmPrLabels.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (scmPrLabels.size() == pageSize) {
                log.info("sql = " + countSQL); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(scmPrLabels, totCount);
    }
    //endregion

    //region Cleanup SCM Data
    public int cleanUpOldPrs(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + PRS_TABLE
                        + " WHERE created_at < :olderThanTime",
                Map.of("olderThanTime", currentTime - olderThanSeconds));
    }

    public int cleanUpOldCommits(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + COMMITS_TABLE
                        + " WHERE created_at < :olderThanTime",
                Map.of("olderThanTime", currentTime - olderThanSeconds));
    }

    public int cleanUpOldIssues(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + ISSUES_TABLE
                        + " WHERE created_at < :olderThanTime",
                Map.of("olderThanTime", currentTime - olderThanSeconds));
    }

    public int cleanUpOldFiles(String company, Long currentTime, Long olderThanSeconds) {
        return template.update("DELETE FROM " + company + "." + FILE_COMMITS_TABLE
                        + " WHERE created_at < :olderThanTime",
                Map.of("olderThanTime", currentTime - olderThanSeconds));
    }
    //endregion

    //region PR Delete
    /*
    For Helix Swarm Review
    1) we try to get repo id from review file info
    2) if not found, we look for review commits & see if we have it in db, if available we use commit's repo ids
    3) if commit not found in db, we use prs versions
    It is possible that during first ingestion the commit didn't exist in the db.
    In subsequent ingestion, the commit could exist in db. This would cause duplicate records in db
    record 1 => pr number n, repo id ['unknown'], integ id i
    record 2 => pr number n, repo id ['repo id from commit'], integ id i
    The delete function below would be used to delete, record 1. i.e. when pr with real repo id is available, we would delete any older prs.
     */
    public Boolean delete(String company, String number, String repoId, String project, String integrationId) throws SQLException {
        Validate.notBlank(number, "Missing number.");
        Validate.notBlank(repoId, "Missing repo_id.");
        Validate.notBlank(project, "Missing project.");
        Validate.notBlank(integrationId, "Missing integrationId.");
        String sql = "DELETE FROM " + company + "." + PRS_TABLE
                + " WHERE number = :number AND repo_id && ARRAY[ :repo_id ]::varchar[] AND project = :project AND integration_id = :integid";
        Map<String, Object> params = Map.of("number", number, "repo_id", repoId, "project", project, "integid", NumberUtils.toInt(integrationId));
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        int rowsDeleted = template.update(sql, params);
        return (rowsDeleted > 0);
    }

    //we dont support delete because insert does the work and cascade deletes do the other work
    @Override
    public Boolean delete(String company, String id) throws SQLException {
        throw new UnsupportedOperationException();
    }
    //endregion

    //region PR Review Get
    public List<DbScmReview> getPrReviews(String company, String prRowId) {
        Validate.notBlank(prRowId, "Missing pr_id.");
        String sql = "SELECT * FROM " + company + "." + REVIEWS_TABLE + " WHERE pr_id = :prId::uuid";
        UUID prId = UUIDUtils.fromString(prRowId);
        if (Objects.isNull(prId)) {
            return List.of();
        }
        Map<String, Object> params = Map.of("prId", prId);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(sql, params, DbScmConverters.prReviewRowMapper());
    }
    //endregion

    //region PR Label Get
    public List<DbScmPRLabel> getPrLabels(String company, String prRowId) {
        Validate.notBlank(prRowId, "Missing pr_id.");
        String sql = "SELECT * FROM " + company + "." + PR_LABELS_TABLE + " WHERE scm_pullrequest_id = :prId::uuid";
        UUID prId = UUIDUtils.fromString(prRowId);
        if (Objects.isNull(prId)) {
            return List.of();
        }
        Map<String, Object> params = Map.of("prId", prId);
        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(sql, params, DbScmConverters.mapPRLabel());
    }
    //endregion

    //region Ensure Table Existence
    @Override
    public Boolean ensureTableExistence(String company) throws SQLException {
        List<String> sqlList = List.of(
                "CREATE TABLE IF NOT EXISTS " + company + "." + PRS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    repo_id VARCHAR[] NOT NULL,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    title VARCHAR NOT NULL DEFAULT '',\n" +
                        "    state VARCHAR NOT NULL,\n" +
                        "    number VARCHAR NOT NULL,\n" +
                        "    creator VARCHAR NOT NULL,\n" +
                        "    merge_sha VARCHAR,\n" +
                        "    source_branch VARCHAR,\n" +
                        "    target_branch VARCHAR,\n" +
                        "    merged BOOLEAN NOT NULL,\n" +
                        "    assignees VARCHAR[] NOT NULL,\n" +
                        "    assignee_ids VARCHAR[] NOT NULL,\n" +
                        "    labels VARCHAR[] NOT NULL,\n" +
                        "    commit_shas VARCHAR[] NOT NULL,\n" +
                        "    creator_id UUID REFERENCES " +
                        company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    metadata JSONB NOT NULL DEFAULT '{}'::JSONB,\n" +
                        "    pr_updated_at TIMESTAMP NOT NULL,\n" +
                        "    pr_merged_at TIMESTAMP,\n" +
                        "    pr_closed_at TIMESTAMP,\n" +
                        "    pr_created_at TIMESTAMP NOT NULL,\n" +
                        "    author_response_time BIGINT,\n" +
                        "    reviewer_response_time BIGINT,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (number,repo_id,project,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + PRS_TABLE + "_number_repo_project_integration_compound_idx " +
                        "on " + company + "." + PRS_TABLE + "(number,repo_id,project,integration_id)",
                "CREATE INDEX IF NOT EXISTS " + PRS_TABLE + "_number_project_integration_id_idx ON " + company + "." + PRS_TABLE + "(number,project,integration_id)",
                "CREATE INDEX IF NOT EXISTS " + PRS_TABLE + "_integration_id_idx ON " + company + "." + PRS_TABLE + "(integration_id)",
                "CREATE INDEX IF NOT EXISTS " + PRS_TABLE + "_merge_sha_idx on " + company + "." + PRS_TABLE + "(merge_sha)",
                "CREATE INDEX IF NOT EXISTS " + PRS_TABLE + "_target_branch_idx on " + company + "." + PRS_TABLE + "(target_branch)",
                "CREATE INDEX IF NOT EXISTS " + PRS_TABLE + "_commit_shas_idx on " + company + "." + PRS_TABLE + " USING GIN(commit_shas)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + COMMITS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    repo_id VARCHAR[] NOT NULL,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    vcs_type VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    author VARCHAR NOT NULL,\n" +
                        "    committer VARCHAR NOT NULL,\n" +
                        "    commit_sha VARCHAR NOT NULL,\n" +
                        "    commit_url VARCHAR,\n" +
                        "    message VARCHAR,\n" +
                        "    author_id UUID REFERENCES " +
                        company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    committer_id UUID REFERENCES " +
                        company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    files_ct INTEGER NOT NULL,\n" +
                        "    additions INTEGER NOT NULL,\n" +
                        "    deletions INTEGER NOT NULL,\n" +
                        "    changes INTEGER NOT NULL,\n" +
                        "    file_types VARCHAR[] NOT NULL,\n" +
                        "    commit_branch VARCHAR,\n" +
                        "    direct_merge BOOLEAN NOT NULL DEFAULT false,\n" +
                        "    ingested_at BIGINT NOT NULL,\n" +
                        "    committed_at TIMESTAMP NOT NULL,\n" +
                        "    commit_pushed_at TIMESTAMP,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (commit_sha,repo_id,project,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + COMMITS_TABLE + "_commit_repo_project_integration_compound_idx " +
                        "on " + company + "." + COMMITS_TABLE + "(commit_sha,repo_id,project,integration_id)",
                "CREATE INDEX IF NOT EXISTS " + COMMITS_TABLE + "_created_at_idx " +
                        "on " + company + "." + COMMITS_TABLE + "(created_at)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + ISSUES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    repo_id VARCHAR NOT NULL,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    title VARCHAR NOT NULL,\n" +
                        "    issue_id VARCHAR NOT NULL,\n" +
                        "    creator VARCHAR NOT NULL,\n" +
                        "    creator_id UUID REFERENCES "
                        + company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE RESTRICT,\n" +
                        "    state VARCHAR NOT NULL,\n" +
                        "    assignees VARCHAR[] NOT NULL,\n" +
                        "    labels VARCHAR[] NOT NULL,\n" +
                        "    number VARCHAR,\n" +
                        "    url VARCHAR,\n" +
                        "    num_comments INTEGER NOT NULL,\n" +
                        "    issue_updated_at TIMESTAMP NOT NULL,\n" +
                        "    issue_created_at TIMESTAMP NOT NULL,\n" +
                        "    issue_closed_at TIMESTAMP,\n" +
                        "    first_comment_at TIMESTAMP,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (issue_id,repo_id,project,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + ISSUES_TABLE + "_issue_repo_project_integration_compound_idx " +
                        "on " + company + "." + ISSUES_TABLE + "(issue_id,repo_id,project,integration_id)",
                //scm files table
                "CREATE TABLE IF NOT EXISTS " + company + "." + FILES_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    repo_id VARCHAR NOT NULL,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    filename VARCHAR NOT NULL,\n" +
                        "    filetype VARCHAR DEFAULT '''NA''',\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (filename,repo_id,project,integration_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + FILES_TABLE + "_repo_id_integration_id_idx ON " + company + "." + FILES_TABLE + "(repo_id,integration_id)",
                "CREATE INDEX IF NOT EXISTS " + FILES_TABLE + "_integration_id_idx ON " + company + "." + FILES_TABLE + "(integration_id)",
                //scm file to commit table
                "CREATE TABLE IF NOT EXISTS " + company + "." + FILE_COMMITS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    commit_sha VARCHAR NOT NULL,\n" +
                        "    file_id UUID NOT NULL REFERENCES "
                        + company + "." + FILES_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    change INTEGER NOT NULL,\n" +
                        "    addition INTEGER NOT NULL,\n" +
                        "    deletion INTEGER NOT NULL,\n" +
                        "    committed_at TIMESTAMP NOT NULL,\n" +
                        "    previous_committed_at TIMESTAMP,\n" +
                        "    created_at BIGINT DEFAULT extract(epoch from now()),\n" +
                        "    UNIQUE (file_id,commit_sha)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + FILE_COMMITS_TABLE + "_commit_sha_idx ON " + company + "." + FILE_COMMITS_TABLE + "(commit_sha)",
                "CREATE INDEX IF NOT EXISTS " + FILE_COMMITS_TABLE + "_file_id_idx ON " + company + "." + FILE_COMMITS_TABLE + "(file_id)",
                "CREATE INDEX IF NOT EXISTS " + FILE_COMMITS_TABLE + "_committed_at_idx ON " + company + "." + FILE_COMMITS_TABLE + "(committed_at)",
                //supporting tables
                //scm jira mapping table
                "CREATE TABLE IF NOT EXISTS " + company + "." + COMMIT_JIRA_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    commit_sha VARCHAR NOT NULL,\n" +
                        "    scm_integ_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    issue_key VARCHAR NOT NULL,\n" +
                        "    UNIQUE (commit_sha,scm_integ_id,issue_key)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + COMMIT_JIRA_TABLE + "_scm_integ_id_idx on "
                        + company + "." + COMMIT_JIRA_TABLE + "(scm_integ_id)",
                "CREATE INDEX IF NOT EXISTS " + COMMIT_JIRA_TABLE + "_issue_key_idx ON " + company + "." + COMMIT_JIRA_TABLE + "(issue_key)",
                // scm commit workitem mapping table
                "CREATE TABLE IF NOT EXISTS " + company + "." + COMMIT_WORKITEM_TABLE + "(\n" +
                        "   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   scm_integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "   commit_sha VARCHAR NOT NULL,\n" +
                        "   workitem_id VARCHAR NOT NULL,\n" +
                        "   UNIQUE (commit_sha,scm_integration_id,workitem_id)\n" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS " + COMMIT_WORKITEM_TABLE + "_scm_integration_commit_sha_workitem_id_idx on "
                        + company + "." + COMMIT_WORKITEM_TABLE + "(scm_integration_id,commit_sha,workitem_id)",
                "CREATE INDEX IF NOT EXISTS " + COMMIT_WORKITEM_TABLE + "_workitem_id_idx ON " + company + "." + COMMIT_WORKITEM_TABLE + "(workitem_id)",
                //scm pullrequests jira mapping table
                "CREATE TABLE IF NOT EXISTS " + company + "." + PULLREQUESTS_JIRA_TABLE + "(\n" +
                        "   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   scm_integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "   project VARCHAR NOT NULL,\n" +
                        "   pr_id VARCHAR NOT NULL,\n" +
                        "   issue_key VARCHAR NOT NULL,\n" +
                        "   pr_uuid UUID REFERENCES " + company + "." + PRS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "   CONSTRAINT " + PULLREQUESTS_JIRA_TABLE + "_unique_pr_jira UNIQUE(issue_key,pr_uuid)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + PULLREQUESTS_JIRA_TABLE + "_issue_key_idx ON " + company + "." + PULLREQUESTS_JIRA_TABLE + "(issue_key)",
                // scm pullrequests workitem mapping table
                "CREATE TABLE IF NOT EXISTS " + company + "." + PULLREQUESTS_WORKITEM_TABLE + "(\n" +
                        "   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "   scm_integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "   project VARCHAR NOT NULL,\n" +
                        "   pr_id VARCHAR NOT NULL,\n" +
                        "   workitem_id VARCHAR NOT NULL,\n" +
                        "   pr_uuid UUID REFERENCES " + company + "." + PRS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "   CONSTRAINT " + PULLREQUESTS_WORKITEM_TABLE + "_unique_pr_workitem UNIQUE(workitem_id,pr_uuid)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + PULLREQUESTS_WORKITEM_TABLE + "_workitem_id_idx ON " + company + "." + PULLREQUESTS_WORKITEM_TABLE + "(workitem_id)",
                // tentative index, but subject to review queries since we already have the unique index for (commit_sha,issue_key)
                // "CREATE INDEX IF NOT EXISTS " + COMMIT_JIRA_TABLE + "_issue_key_idx on "
                //         + company + "." + COMMIT_JIRA_TABLE + "(issue_key)",

                //reviews table
                "CREATE TABLE IF NOT EXISTS " + company + "." + REVIEWS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    reviewer VARCHAR NOT NULL,\n" +
                        "    reviewer_id UUID REFERENCES "
                        + company + "." + UserIdentityService.USER_IDS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    review_id VARCHAR NOT NULL,\n" +
                        "    state VARCHAR NOT NULL,\n" +
                        "    pr_id UUID NOT NULL REFERENCES "
                        + company + "." + PRS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    reviewed_at TIMESTAMP NOT NULL,\n" +
                        "    UNIQUE (review_id,pr_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + REVIEWS_TABLE + "_compound_idx on "
                        + company + "." + REVIEWS_TABLE + "(review_id,pr_id)",

                "CREATE TABLE IF NOT EXISTS " + company + "." + PR_LABELS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    scm_pullrequest_id UUID NOT NULL REFERENCES " + company + "." + PRS_TABLE + "(id) ON DELETE CASCADE,\n" +
                        "    cloud_id VARCHAR NOT NULL,\n" +
                        "    name VARCHAR NOT NULL,\n" +
                        "    description VARCHAR,\n" +
                        "    label_added_at TIMESTAMP WITH TIME ZONE NOT NULL,\n" +
                        "    CONSTRAINT uniq_" + PR_LABELS_TABLE + "_scm_pullrequest_id_cloud_id_idx UNIQUE(scm_pullrequest_id,cloud_id)\n" +
                        ")",
                "CREATE INDEX IF NOT EXISTS " + PR_LABELS_TABLE + "_name_idx on " + company + "." + PR_LABELS_TABLE + "(name)\n",
                "CREATE INDEX IF NOT EXISTS " + PR_LABELS_TABLE + "_label_added_at_idx ON " + company + "." + PR_LABELS_TABLE + "(label_added_at DESC)",
                "CREATE TABLE IF NOT EXISTS " + company + "." + TAGS_TABLE + "(\n" +
                        "    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),\n" +
                        "    integration_id INTEGER NOT NULL REFERENCES "
                        + company + ".integrations(id) ON DELETE CASCADE,\n" +
                        "    project VARCHAR NOT NULL,\n" +
                        "    repo VARCHAR NOT NULL,\n" +
                        "    tag VARCHAR NOT NULL,\n" +
                        "    commit_sha VARCHAR NOT NULL,\n" +
                        "    created_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()," +
                        "    updated_at       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()" +
                        ")",
                "CREATE UNIQUE INDEX IF NOT EXISTS " + TAGS_TABLE + "_repo_project_integration_tag_compound_idx " +
                        "ON " + company + "." + TAGS_TABLE + " (repo,project,integration_id,tag)",
                "CREATE INDEX IF NOT EXISTS " + TAGS_TABLE + "_repo_project_integration_commit_sha_compound_idx " +
                        "ON " + company + "." + TAGS_TABLE + " (repo,project,integration_id,commit_sha)",
                "CREATE INDEX IF NOT EXISTS " + TAGS_TABLE + "integration_tag_commit_sha_compound_idx " +
                        "ON " + company + "." + TAGS_TABLE + " (integration_id,tag,commit_sha)"
        );

        sqlList.forEach(template.getJdbcTemplate()::execute);
        return true;
    }

    public UUID insertTag(String company, DbScmTag tag) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            MapSqlParameterSource params = constructParameterSourceTag(tag, null);
            String insertConfigSql = String.format(INSERT_TAG_SQL_FORMAT, company);
            KeyHolder keyHolder = new GeneratedKeyHolder();
            int updatedRows = template.update(insertConfigSql, params, keyHolder);
            if (updatedRows <= 0 || keyHolder.getKeys() == null) {
                throw new SQLException("Failed to insert scm tag");
            }
            UUID id = (UUID) keyHolder.getKeys().get("id");
            transactionManager.commit(txStatus);
            return id;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Error while inserting tag : {}", e.getMessage(), e);
            throw e;
        }
    }

    private MapSqlParameterSource constructParameterSourceTag(DbScmTag tag, final UUID existingId) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("integration_id", Integer.valueOf(tag.getIntegrationId()));
        params.addValue("project", tag.getProject());
        params.addValue("repo", tag.getRepo());
        params.addValue("tag", tag.getTag());
        params.addValue("commit_sha", tag.getCommitSha());
        if(existingId != null) {
            params.addValue("id", existingId);
        }
        return params;
    }

    public Boolean updateTag(String company, DbScmTag tag) throws SQLException {
        TransactionDefinition txDef = new DefaultTransactionDefinition();
        TransactionStatus txStatus = transactionManager.getTransaction(txDef);
        try {
            MapSqlParameterSource params = constructParameterSourceTag(tag, tag.getId());
            String updateConfigSql = String.format(UPDATE_TAG_SQL_FORMAT, company);
            int updatedRows = template.update(updateConfigSql, params);
            if (updatedRows != 1) {
                throw new SQLException("Failed to update scm tag, updatedRows = " + updatedRows);
            }
            transactionManager.commit(txStatus);
            return true;
        } catch (Exception e) {
            transactionManager.rollback(txStatus);
            log.error("Error while updating tag: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Boolean deleteTag(String company, String id) {
        String deleteSql = String.format(DELETE_TAG_SQL_FORMAT, company);
        log.info("Deleting Scm Tag for " + company);
        return template.update(deleteSql, Map.of("id", UUID.fromString(id))) > 0;
    }

    public Optional<DbScmTag> getTag(String company, String id) throws SQLException {
        var results = listByFilterTag(company, 0, 1, null, Collections.singletonList(UUID.fromString(id)), null, null, null, null).getRecords();
        return results.size() > 0 ? Optional.of(results.get(0)) : Optional.empty();
    }

    public Optional<DbScmTag> getTag(String company, String repoId, String project, String integrationId, String tag) {
        var results = listByFilterTag(company, 0, 1, List.of(integrationId), List.of(), List.of(project), List.of(repoId), List.of(tag), List.of());
        return results.getRecords().size() > 0 ? Optional.of(results.getRecords().get(0)) : Optional.empty();
    }

    public DbListResponse<DbScmTag> listTag(String company, Integer pageNumber, Integer pageSize) throws SQLException {
        return listByFilterTag(company, pageNumber, pageSize, null, null, null, null, null, null);
    }

    public DbListResponse<DbScmTag> listByFilterTag(String company, Integer pageNumber, Integer pageSize,
                                                    final List<String> integrationIds, final List<UUID> ids,
                                                    final List<String> projects, final List<String> repos,
                                                    final List<String> tags, final List<String> commitShas) {
        List<String> criteria = new ArrayList<>();
        MapSqlParameterSource params = new MapSqlParameterSource();

        parseCriteriaTag(criteria, params, integrationIds, ids, projects, repos, tags, commitShas);
        String baseWhereClause = (CollectionUtils.isEmpty(criteria)) ? "" : " WHERE " + String.join(" AND ", criteria);
        String selectSqlBase = "SELECT st.* FROM " + company + ".scm_tags as st "
                + baseWhereClause
                + " group by st.id";

        String selectSql = selectSqlBase + " ORDER BY updated_at desc" + " LIMIT " + pageSize + " OFFSET " + (pageNumber * pageSize);
        String countSQL = "SELECT COUNT(*) FROM (" +  selectSqlBase + ") AS counted";

        Integer totCount = 0;
        log.info("sql = {}", selectSql);
        log.info("params = {}", params);
        List<DbScmTag> scmTags = template.query(selectSql, params, DbScmConverters.mapScmTag());
        log.info("scmTags.size() = {}", scmTags.size());
        if (scmTags.size() > 0) {
            totCount = scmTags.size() + pageNumber * pageSize; // if its last page or total count is less than pageSize
            if (scmTags.size() == pageSize) {
                log.info("sql = {}", countSQL);
                log.info("params = {}", params);
                totCount = template.query(countSQL, params, CountQueryConverter.countMapper()).get(0);
            }
        }
        return DbListResponse.of(scmTags, totCount);
    }

    private void parseCriteriaTag(final List<String> criteria, final MapSqlParameterSource params,
                                  final List<String> integrationIds, final List<UUID> ids,
                                  final List<String> projects, final List<String> repos,
                                  final List<String> tags, final List<String> commitShas) {
        if (CollectionUtils.isNotEmpty(ids)) {
            criteria.add("st.id IN (:ids)");
            params.addValue("ids", ids);
        }
        if (CollectionUtils.isNotEmpty(integrationIds)) {
            criteria.add("st.integration_id IN (:integration_ids)");
            List<Integer> intIntegrationIds = new ArrayList<>();
            for(String integrationId : integrationIds) intIntegrationIds.add(Integer.valueOf(integrationId));
            params.addValue("integration_ids", intIntegrationIds);
        }
        if (CollectionUtils.isNotEmpty(projects)) {
            criteria.add("st.project IN (:projects)");
            params.addValue("projects", projects);
        }
        if (CollectionUtils.isNotEmpty(repos)) {
            criteria.add("st.repo IN (:repos)");
            params.addValue("repos", repos);
        }
        if (CollectionUtils.isNotEmpty(tags)) {
            criteria.add("st.tag IN (:tags)");
            params.addValue("tags", tags);
        }
        if (CollectionUtils.isNotEmpty(commitShas)) {
            criteria.add("st.commit_sha IN (:commit_shas)");
            params.addValue("commit_shas", commitShas);
        }
    }

    public DbListResponse<DbAggregationResult> groupByAndCalculatePrReviews(String company, ScmPrFilter filter, OUConfiguration ouConfig) throws SQLException {

        Validate.notNull(filter.getAcross(), "Across cant be missing for groupby query.");

        ScmPrFilter.DISTINCT across = filter.getAcross();
        ScmPrFilter.CALCULATION calculation = filter.getCalculation();
        if(calculation == null)
            calculation = ScmPrFilter.CALCULATION.count;

        Map<String, Object> params = new HashMap<>();

        String prsWhere = "";
        String prsReviewsWhere = "";

        Map<String, List<String>> conditions = createPrWhereClauseAndUpdateParams(company, params, filter, null, "", ouConfig);

        if(CollectionUtils.isNotEmpty(conditions.get(PRS_TABLE)))
            prsWhere = " WHERE " + String.join(" AND ", conditions.get(PRS_TABLE));
        if(CollectionUtils.isNotEmpty(conditions.get(REVIEWS_TABLE)))
            prsReviewsWhere = StringUtils.isEmpty(prsWhere) ? " WHERE " + String.join(" AND ", conditions.get(REVIEWS_TABLE)):" AND "+ String.join(" AND ", conditions.get(REVIEWS_TABLE));

        String selectBaseQuery = "";
        String commentStateCondition = useCommentCondition(filter) ? " WHERE state = 'COMMENTED' ": StringUtils.EMPTY;

        switch (calculation){

            case count:
                selectBaseQuery =  "WITH pr_reviews AS (\n" +
                        " SELECT pr_id, reviewer,  reviewer_id, RTRIM(To_char(Date(reviewed_at), 'Day')) AS reviewed_at_interval, count(*), reviewed_at " +
                        " FROM "+company+"."+REVIEWS_TABLE +
                        commentStateCondition+
                        " GROUP BY pr_id, reviewer, reviewer_id, reviewed_at_interval, reviewed_at\n" +
                        " ORDER BY reviewer) ";
                break;
            default:
                throw new SQLException("Invalid calculation field provided for this agg.");
        }

        String filterProductSql = "";
        String groupBy = "GROUP BY ";
        String orderBy = "ORDER BY ";
        String key = "";
        String additionalKey = "reviewed_at_interval";

        switch (across){
            case commenter:
                filterProductSql = "SELECT reviewer, reviewer_id, reviewed_at_interval, sum (count) AS count, integration_id FROM ( ";
                groupBy += " reviewer, reviewer_id , reviewed_at_interval, integration_id ";
                orderBy += "reviewer ";
                key = "reviewer_id";
                break;

            case repo_id:
                filterProductSql = "SELECT repo_ids, reviewed_at_interval, sum (count) AS count, integration_id FROM ( ";
                groupBy += " repo_ids, reviewed_at_interval, integration_id ";
                orderBy += " repo_ids ";
                key = "repo_ids";
                break;

            default:
                throw new SQLException("Invalid across field provided for this agg.");
        }

        String query = " SELECT pr_reviews.*,  unnest(scm_pullrequests.repo_id) AS repo_ids, "+ScmQueryUtils.PRS_SELECT+
                " FROM pr_reviews LEFT JOIN "+company+"."+PRS_TABLE+" on pr_reviews.pr_id = scm_pullrequests.id ) a ";

        String finalQuery = selectBaseQuery+
                filterProductSql+ query +
                prsWhere + prsReviewsWhere
                +groupBy+ orderBy;

        log.info("final query is {}",finalQuery);
        log.info("param {}",params);

        List<DbAggregationResult> res = template.query(finalQuery, params, DbScmConverters.scmActivityMapper(key, additionalKey));
        DbListResponse<DbAggregationResult> result = DbListResponse.of(res, res.size());

        return result;
    }

    private boolean useCommentCondition(ScmPrFilter filter) {
        return CollectionUtils.isNotEmpty(filter.getReviewerIds());
    }
    //endregion

    public static long getTimeDifferenceInDays(ImmutablePair<Long, Long> range) {
        LocalDateTime begin = Instant.ofEpochSecond((range.getLeft())).atZone(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime end = Instant.ofEpochSecond((range.getRight())).atZone(ZoneId.of("UTC")).toLocalDateTime();
        long diffINSeconds = java.time.Duration.between(begin, end).toSeconds();
        double dayDiff = (double) diffINSeconds / SECONDS_PER_DAY;
        return (long) Math.ceil(dayDiff);
    }
}
