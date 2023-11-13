package io.levelops.commons.faceted_search.db.services;

import io.levelops.commons.databases.converters.DbScmConverters;
import io.levelops.commons.databases.models.database.GitTechnology;
import io.levelops.commons.databases.models.database.scm.DbScmCommitStats;
import io.levelops.commons.databases.models.database.scm.DbScmPRLabel;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmReview;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.ScmQueryUtils;
import io.levelops.commons.faceted_search.db.converters.ScmPRConverter;
import io.levelops.commons.faceted_search.db.models.ScmPROrCommitJiraWIMapping;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Objects;
import static io.levelops.commons.databases.services.ScmAggService.PRS_TABLE;
import static io.levelops.commons.databases.services.ScmAggService.PULLREQUESTS_JIRA_TABLE;
import static io.levelops.commons.databases.services.ScmAggService.PULLREQUESTS_WORKITEM_TABLE;
import static io.levelops.commons.databases.services.ScmQueryUtils.APPROVERS_PRS_LIST;
import static io.levelops.commons.databases.services.ScmQueryUtils.COMMENTERS_SELECT_SQL;
import static io.levelops.commons.databases.services.ScmQueryUtils.CREATORS_SQL;
import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_APPROVER_COUNT;
import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_REVIEWED_AT_SELECT;
import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_REVIEWER_COUNT;
import static io.levelops.commons.databases.services.ScmQueryUtils.PRS_SELECT;
import static io.levelops.commons.databases.services.ScmQueryUtils.PR_APPROVE_TIME_SQL;
import static io.levelops.commons.databases.services.ScmQueryUtils.PR_COMMENT_TIME_SQL;
import static io.levelops.commons.databases.services.ScmQueryUtils.REVIEW_PRS_SELECT_LIST;

@Log4j2
@Service
public class ScmPRDBService {
    private static final Integer CHUNK_SIZE = 1000;
    private static final Integer BATCH_SIZE = 30000;
    private static final List<String> APPROVED_STATES = List.of("APPROVED","approved","looks good to me, approved","approved with suggestions");
    private static final List<String> COMMENTED_STATES = List.of("commented", "i would prefer this is not merged as is", "this shall not be merged","looks good to me, but someone else must approve", "no score", "no vote", "changes_requested");

    private static final String PRS_LIST_BY_JIRA_ISSUE_KEY = "SELECT DISTINCT(pr.id) as id, pr.pr_created_at, ARRAY_AGG(issue_key) as workitem_ids FROM %s." + PRS_TABLE + " AS pr JOIN %s." + PULLREQUESTS_JIRA_TABLE + " AS m ON pr.integration_id=m.scm_integration_id AND pr.project=m.project and pr.number = m.pr_id ";
    private static final String PRS_LIST_BY_WORKITEM_ID = "SELECT DISTINCT(pr.id) as id, pr.pr_created_at, ARRAY_AGG(workitem_id) as workitem_ids FROM %s." + PRS_TABLE + " AS pr JOIN %s." + PULLREQUESTS_WORKITEM_TABLE + " AS m ON pr.integration_id=m.scm_integration_id AND pr.project=m.project and pr.number = m.pr_id ";
    private static final String PR_LABEL_LIST = "SELECT * FROM %s.scm_pullrequest_labels AS prl WHERE prl.scm_pullrequest_id IN (:pr_id) ORDER BY prl.scm_pullrequest_id";
    private static final String PR_REVIEWS_LIST = "SELECT * FROM %s.scm_pullrequest_reviews AS prr WHERE prr.pr_id IN (:pr_id) ORDER BY prr.pr_id";
    private static final String PR_COMMITS_STATS = "SELECT sc.commit_sha, sc.integration_id, sc.files_ct, sc.additions, sc.deletions, sc.changes, sc.committed_at" +
            " from %s.scm_commits sc where sc.commit_sha in (:commit_sha)";
    private static final String SCM_TECH_LIST = "SELECT * FROM %s.gittechnologies AS gt WHERE gt.repo_id IN (:repo_id) ORDER BY gt.repo_id";

    private final NamedParameterJdbcTemplate template;
    private final ScmAggService scmAggService;

    @Autowired
    public ScmPRDBService(final DataSource dataSource, ScmAggService scmAggService) {
        //super(dataSource);
        template = new NamedParameterJdbcTemplate(dataSource);
        this.scmAggService = scmAggService;
    }

    //region PRS
    private Map<UUID, DbScmPullRequest> fetchPRsByIds(String company, List<UUID> prIds) throws SQLException {
        Map<UUID, DbScmPullRequest> pullRequestsMap = new HashMap<>();
        for(List<UUID> currentPRIds : ListUtils.partition(prIds, CHUNK_SIZE)) {
            DbListResponse<DbScmPullRequest> scmPRsListResponse = scmAggService.list(company, ScmPrFilter.builder().ids(currentPRIds).build(), Collections.emptyMap(), null, 0, CHUNK_SIZE);
            CollectionUtils.emptyIfNull(scmPRsListResponse.getRecords()).stream()
                    .forEach(pr -> pullRequestsMap.put(UUID.fromString(pr.getId()), pr));
        }
        return pullRequestsMap;
    }
    //endregion

    //region PRs List by Jira & WI Ids
    private Map<String, List<DbScmPullRequest>> listByJiraIssuesOrWorkItems(String company, Set<String> jiraIssueKeysOrWorkItemIds, boolean jiraIssues) throws SQLException {

        List<ScmPROrCommitJiraWIMapping> scmPrJiraWIMappings = getScmPRJiraWIMappings(company, jiraIssueKeysOrWorkItemIds, null, null, jiraIssues);
        List<UUID> prUUIDs = CollectionUtils.emptyIfNull(scmPrJiraWIMappings).stream().map(ScmPROrCommitJiraWIMapping::getPrOrCommitId).collect(Collectors.toList());
        Map<UUID, DbScmPullRequest> pullRequestsMap = fetchPRsByIds(company, prUUIDs);

        Map<String, List<DbScmPullRequest>> issueKeyToPRIdMap = new HashMap<>();
        CollectionUtils.emptyIfNull(scmPrJiraWIMappings).stream()
                .filter(x -> pullRequestsMap.containsKey(x.getPrOrCommitId()))
                .forEach(m -> {
                    CollectionUtils.emptyIfNull(m.getWorkItemIds()).stream()
                            .forEach(issueKey -> {
                                issueKeyToPRIdMap.computeIfAbsent(issueKey, k -> new ArrayList<>()).add(pullRequestsMap.get(m.getPrOrCommitId()));
                                Collections.sort(issueKeyToPRIdMap.get(issueKey), (a,b) -> (int) (a.getPrCreatedAt() - b.getPrCreatedAt()));
                            });
                });

        return issueKeyToPRIdMap;
    }

    public List<ScmPROrCommitJiraWIMapping> getScmPRJiraWIMappings(String company, Set<String> jiraIssueKeysOrWorkItemIds, Set<String> prNumbers, Set<String> prIds, boolean jiraIssues){
        MapSqlParameterSource params = new MapSqlParameterSource();
        List<String> conditions = new ArrayList<>();
        String groupBy = " GROUP BY pr.id ";
        String orderBy = " ORDER BY pr.pr_created_at";

        if (CollectionUtils.isNotEmpty(jiraIssueKeysOrWorkItemIds)) {
            if (jiraIssues)
                conditions.add("m.issue_key in (:workitem_ids)");
            else
                conditions.add("m.workitem_id in (:workitem_ids)");

            params.addValue("workitem_ids", jiraIssueKeysOrWorkItemIds);
        }
        if (CollectionUtils.isNotEmpty(prNumbers)) {
            conditions.add("pr.number in (:number)");
            params.addValue("number", prNumbers);
        }
        if (CollectionUtils.isNotEmpty(prIds)) {
            conditions.add("pr.id in (:id)");
            params.addValue("id", prIds.stream().map(pr -> UUID.fromString(pr)).collect(Collectors.toList()));
        }
        String whereClause = "";
        if (CollectionUtils.isNotEmpty(conditions))
            whereClause = " WHERE " + String.join(" AND ", conditions);

        String selectSql = String.format((jiraIssues) ? PRS_LIST_BY_JIRA_ISSUE_KEY : PRS_LIST_BY_WORKITEM_ID, company, company)
                + whereClause
                + groupBy
                + orderBy;

        log.info("sql = " + selectSql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);
        return template.query(selectSql, params, ScmPRConverter.prJiraWIMapper());

    }

    public Map<String, List<DbScmPullRequest>> listByJiraTickets(String company, Set<String> jiraIssueKeys) throws SQLException {
        return listByJiraIssuesOrWorkItems(company, jiraIssueKeys, true);
    }

    public Map<String, List<DbScmPullRequest>> listByWorkItems(String company, Set<String> workItemIds) throws SQLException {
        return listByJiraIssuesOrWorkItems(company, workItemIds, false);
    }
    //endregion

    public List<DbScmPullRequest> listPRsWithoutEnrichment(String company, ScmPrFilter filter, int pageNumber, Integer pageSize) {
        String baseQuery = getPRQuery(company);
        String limit  = " offset :offset limit :limit";
        String orderBy = " order by pr_created_at";

        List<String> prTableConditions = new ArrayList<>();
        ImmutablePair<Long, Long> prUpdatedRange = filter.getPrUpdatedRange();
        if (prUpdatedRange!= null) {
            if (prUpdatedRange.getLeft() != null) {
                prTableConditions.add("updated_at > TO_TIMESTAMP(" + prUpdatedRange.getLeft() + ")");
            }
            if (filter.getPrUpdatedRange().getRight() != null) {
                prTableConditions.add("updated_at < TO_TIMESTAMP(" + prUpdatedRange.getRight() + ")");
            }
        }

        String whereClause = "";
        if (CollectionUtils.isNotEmpty(prTableConditions)) {
            whereClause = " WHERE " + String.join(" AND ", prTableConditions);
        }

        String query = "SELECT * FROM ( "+baseQuery +") a"
                +whereClause
                +orderBy
                +limit;
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("offset", pageNumber * pageSize);
        params.addValue("limit", pageSize);

        log.info("query {}", query);
        log.info("param {}", params);

        return template.query(query, params, DbScmConverters.prRowMapper());
    }

    public List<DbScmPullRequest> readFromDB(String company, ScmPrFilter filter, int pageNumber, Integer pageSize, boolean enrichPR, boolean useMergeSha) {

        List<DbScmPullRequest> prList = listPRsWithoutEnrichment(company, filter, pageNumber, pageSize);
        if (CollectionUtils.isEmpty(prList)) {
            return Collections.EMPTY_LIST;            
        }
        if(!enrichPR) {
            return prList;
        }

        Map<UUID, List<DbScmPRLabel>> prLabelList = listLabels(company, prList);
        Set<String> pr_numbers = prList.stream().map(pr -> pr.getNumber()).collect(Collectors.toSet());
        List<ScmPROrCommitJiraWIMapping> jiraMappings = getScmPRJiraWIMappings(company, null, pr_numbers, null, true);
        Map<String, ScmPROrCommitJiraWIMapping> jiraMap = new HashMap<>();
        CollectionUtils.emptyIfNull(jiraMappings).stream().forEach(p -> jiraMap.put(p.getPrOrCommitId().toString(), p));
        List<ScmPROrCommitJiraWIMapping> wiMappings = getScmPRJiraWIMappings(company, null, pr_numbers, null,false);
        Map<String, ScmPROrCommitJiraWIMapping> wiMap = new HashMap<>();
        CollectionUtils.emptyIfNull(wiMappings).stream().forEach(p -> wiMap.put(p.getPrOrCommitId().toString(), p));
        Map<String, List<DbScmReview>> reviewMap = listPrReviews(company, prList.stream().map(p -> UUID.fromString(p.getId())).collect(Collectors.toList()));
        List<String> commitShaList = new ArrayList<>();
        if(useMergeSha){
            log.info("getting merge_shas for company {}",company);
            commitShaList = prList.stream().flatMap(
                            p -> Objects.isNull(p.getMergeSha())? p.getCommitShas().stream() : Stream.of(p.getMergeSha()))
                    .collect(Collectors.toList());
        }else {
            log.info("getting commit_shas for company {}",company);
            commitShaList = prList.stream().flatMap(p -> p.getCommitShas().stream()).collect(Collectors.toList());
        }
        Map<String, DbScmCommitStats> commitsMap = new HashMap<>();
        for (List<String> currentBatch : ListUtils.partition(commitShaList, BATCH_SIZE)){
            commitsMap.putAll(listCommitStats(company, currentBatch));
        }
        Map<String, List<GitTechnology>> techMap = getScmTechnologies(company, prList.stream().flatMap(p -> p.getRepoIds().stream()).distinct().collect(Collectors.toList()));

        return prList.stream().map(pr -> {
            String integrationId = pr.getIntegrationId();

            Long minReviewedAt = null;
            if(reviewMap.containsKey(pr.getId())){
                List<DbScmReview> reviewList = reviewMap.get(pr.getId());
                minReviewedAt = getMinReviewedTime(reviewList);
                pr  = populateReviewDetails(pr, reviewList);
            }

            if (prLabelList.containsKey(UUID.fromString(pr.getId()))) {
                pr = pr.toBuilder()
                        .prLabels(prLabelList.get(UUID.fromString(pr.getId())))
                        .build();
            }
            if (jiraMap.containsKey(pr.getId())) {
                pr = pr.toBuilder()
                        .workitemIds(jiraMap.get(pr.getId()).getWorkItemIds())
                        .build();
            }
            if (wiMap.containsKey(pr.getId())) {
                List<String> items = new ArrayList<>();
                items.addAll(wiMap.get(pr.getId()).getWorkItemIds());
                if(CollectionUtils.isNotEmpty(pr.getWorkitemIds())){
                    items.addAll(pr.getWorkitemIds());
                }
                pr = pr.toBuilder()
                        .workitemIds(items)
                        .build();
            }

            if(useMergeSha && !Objects.isNull(pr.getMergeSha())){
                if(commitsMap.containsKey(pr.getMergeSha()+"_"+integrationId)){
                    DbScmCommitStats dbScmCommitStats = commitsMap.get(pr.getMergeSha()+"_"+integrationId);
                    pr = populateCommitStats(pr, List.of(dbScmCommitStats));
                }
            }else if(pr.getCommitShas().stream().anyMatch(r -> commitsMap.containsKey(r+"_"+integrationId))) {

                List<DbScmCommitStats> commitList = pr.getCommitShas().stream()
                        .filter(r -> commitsMap.containsKey(r+"_"+integrationId))
                        .map(c -> commitsMap.get(c+"_"+integrationId))
                        .collect(Collectors.toList());

                if(CollectionUtils.isNotEmpty(commitList)) {
                    pr = populateCommitStats(pr, commitList);
                }
            }

            if(techMap.containsKey(pr.getRepoIds().get(0)+"_"+integrationId)){
                List<String> techList = techMap.get(pr.getRepoIds().get(0)+"_"+integrationId).stream()
                        .map(p -> p.getName())
                        .collect(Collectors.toList());
                pr = pr.toBuilder()
                        .technology(techList)
                        .build();
            }

            String createdDay = new SimpleDateFormat("EEEE").format(new Date(TimeUnit.SECONDS.toMillis(pr.getCreatedAt())));
            String mergedDay = pr.getPrMergedAt() == null ? null : new SimpleDateFormat("EEEE").format(new Date(TimeUnit.SECONDS.toMillis(pr.getPrMergedAt())));
            String closedDay = pr.getPrClosedAt() == null ? null : new SimpleDateFormat("EEEE").format(new Date(TimeUnit.SECONDS.toMillis(pr.getPrClosedAt())));

            if(StringUtils.isNotEmpty(mergedDay)){
                pr = pr.toBuilder()
                        .mergedDay(mergedDay)
                        .cycleTime(pr.getPrMergedAt() - pr.getPrCreatedAt())
                        .reviewMergeCycleTime(minReviewedAt == null ? null : pr.getPrMergedAt() - minReviewedAt)
                        .prMergedAt(TimeUnit.SECONDS.toMillis(pr.getPrMergedAt()))
                        .build();
            }
            if(StringUtils.isNotEmpty(closedDay)){
                pr = pr.toBuilder()
                        .closedDay(closedDay)
                        .prClosedAt(TimeUnit.SECONDS.toMillis(pr.getPrClosedAt()))
                        .build();
            }

            pr = pr.toBuilder()
                    .createdDay(createdDay)
                    .reviewCycleTime((minReviewedAt == null ? null :  minReviewedAt - pr.getPrCreatedAt()))
                    .createdAt(TimeUnit.SECONDS.toMillis(pr.getCreatedAt()))
                    .prCreatedAt(TimeUnit.SECONDS.toMillis(pr.getPrCreatedAt()))
                    .prUpdatedAt(TimeUnit.SECONDS.toMillis(pr.getPrUpdatedAt()))
                    .commentCount(pr.getCommenters().size())
                    .merged(pr.getMerged() ? true : null)
                    .build();

            return pr;
        }).collect(Collectors.toList());
    }

    //region PR Reviews
    public  Map<String, List<DbScmReview>> listPrReviews(String company, List<UUID> prIdList){

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pr_id", prIdList);
        String selectSql = String.format(PR_REVIEWS_LIST, company);

        log.info("reviews sql = " + selectSql);

        List<DbScmReview> reviewList = template.query(selectSql, params, DbScmConverters.prReviewRowMapper());
        Map<String, List<DbScmReview>> reviewMap = new HashMap<>();
        CollectionUtils.emptyIfNull(reviewList).stream()
                .forEach(r -> {
                    List<DbScmReview> list = reviewMap.getOrDefault(r.getPrId(), new ArrayList<DbScmReview>());
                    list.add(r);
                    reviewMap.put(r.getPrId(), list);
                });

        return reviewMap;
    }
    private DbScmPullRequest populateReviewDetails(DbScmPullRequest pr, List<DbScmReview> reviewList) {

        List<DbScmReview> approveReviewList = new ArrayList<>();
        List<DbScmReview> commentReviewList = new ArrayList<>();

        reviewList.forEach(review ->{
            review = review.toBuilder()
                    .reviewedAt(TimeUnit.SECONDS.toMillis(review.getReviewedAt()))
                    .build();
            if(StringUtils.isNotEmpty(review.getState()) && APPROVED_STATES.contains(review.getState().toLowerCase())){
                approveReviewList.add(review);
            }else if(StringUtils.isNotEmpty(review.getState()) && COMMENTED_STATES.contains(review.getState().toLowerCase())){
                commentReviewList.add(review);
            }
        });

        if(CollectionUtils.isNotEmpty(approveReviewList)) {
            Long minApprovalTime = getMinReviewedTime(approveReviewList);
            pr = pr.toBuilder()
                    .approverInfo(approveReviewList)
                    .approvalTime(TimeUnit.MILLISECONDS.toSeconds(minApprovalTime) - pr.getPrCreatedAt())
                    .build();
        }

        if(CollectionUtils.isNotEmpty(commentReviewList)) {
            Long minCommentTime = getMinReviewedTime(commentReviewList);;
            pr = pr.toBuilder()
                    .commenterInfo(commentReviewList)
                    .commentTime(TimeUnit.MILLISECONDS.toSeconds(minCommentTime) - pr.getPrCreatedAt())
                    .build();
        }

        return pr;
    }
    //endregion

    //region PR Commit Stats
    public  Map<String, DbScmCommitStats> listCommitStats(String company, List<String> commitSha){

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("commit_sha", commitSha);
        String selectSql = String.format(PR_COMMITS_STATS, company);

        log.info("commits stat sql = " + selectSql);
        log.info("commits/merge sha = " + commitSha);
        List<DbScmCommitStats> commitStatsList = template.query(selectSql, params, DbScmConverters.prCommitStatRowMapper());
        Map<String, DbScmCommitStats> commitStatMap = new HashMap<>();
        CollectionUtils.emptyIfNull(commitStatsList).stream()
                .forEach(r -> {
                    commitStatMap.put(r.getCommitSha()+"_"+r.getIntegrationId(), r);
                });

        return commitStatMap;
    }
    private DbScmPullRequest populateCommitStats(DbScmPullRequest pr, List<DbScmCommitStats> commitList) {

        int additions = 0;
        int deletions = 0;
        int changes = 0;
        int filesCt = 0;
        Long firstCommittedAt = null;

        for (DbScmCommitStats c : commitList) {
            if( c == null)
                continue;

            additions += c.getAdditions();
            deletions += c.getDeletions();
            changes += c.getChanges();
            filesCt += c.getFileCount();
            if(firstCommittedAt != null){
                firstCommittedAt = c.getCommittedAt() < firstCommittedAt ? c.getCommittedAt() : firstCommittedAt;
            }else {
                firstCommittedAt = c.getCommittedAt();
            }
        }

        return pr.toBuilder()
                .additions(additions)
                .deletions(deletions)
                .change(changes)
                .loc(additions+changes)
                .filesCount(filesCt)
                .firstCommittedAt(firstCommittedAt)
                .build();
    }
    //endregion

    //region Scm Technologies
    public Map<String, List<GitTechnology>> getScmTechnologies(String company, List<String> repoIds) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("repo_id",repoIds);
        String selectSql = String.format(SCM_TECH_LIST, company);

        log.info("technology sql = " + selectSql);

        List<GitTechnology> technologyList = template.query(selectSql, params, DbScmConverters.mapScmTech());
        Map<String, List<GitTechnology>> techMap = new HashMap<>();
        CollectionUtils.emptyIfNull(technologyList).stream()
                .forEach(c -> {
                    List<GitTechnology> list = techMap.getOrDefault(c.getRepoId()+"_"+c.getIntegrationId(), new ArrayList<GitTechnology>());
                    list.add(c);
                    techMap.put(c.getRepoId()+"_"+c.getIntegrationId(), list);
                });
        return techMap;
    }
    //endregion

    private String getPRQuery(String company) {

        return "SELECT  "+ PRS_SELECT + CREATORS_SQL
                + REVIEW_PRS_SELECT_LIST + APPROVERS_PRS_LIST
                + COMMENTERS_SELECT_SQL + PRS_REVIEWED_AT_SELECT
                + PR_APPROVE_TIME_SQL + PR_COMMENT_TIME_SQL
                + ScmQueryUtils.getApprovalStatusSqlStmt() + ScmQueryUtils.getReviewType()
                + PRS_REVIEWER_COUNT + PRS_APPROVER_COUNT
                + ScmQueryUtils.getCollaborationStateSql()
                + ScmQueryUtils.getReviewType()
                + ScmQueryUtils.getApprovalStatusSqlStmt()
                + ScmQueryUtils.RESOLUTION_TIME_SQL + " FROM "
                + company+"." + PRS_TABLE
                + ScmQueryUtils.sqlForCreatorTableJoin(company)
                + ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.REVIEWER)
                + ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.APPROVER)
                + ScmQueryUtils.getSqlForReviewTableJoin(company, ScmQueryUtils.REVIEWS_TABLE_TYPE.COMMENTER)
                + ScmQueryUtils.getSqlForPRReviewedAtJoin(company);
    }

    //region PR Labels
    public Map<UUID, List<DbScmPRLabel>> listLabels(String company, List<DbScmPullRequest> prList) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("pr_id", prList.stream().map(c -> UUID.fromString(c.getId())).collect(Collectors.toList()));
        String selectSql = String.format(PR_LABEL_LIST, company);

        log.info("sql = " + selectSql);
        log.info("params = {}", params);

        List<DbScmPRLabel> labelList = template.query(selectSql, params, DbScmConverters.mapESPRLabel());
        Map<UUID, List<DbScmPRLabel>> labelMap = new HashMap<>();
        CollectionUtils.emptyIfNull(labelList).stream()
                .forEach(c -> {
                    List<DbScmPRLabel> list = labelMap.getOrDefault(c.getScmPullRequestId(), new ArrayList<DbScmPRLabel>());
                    list.add(c);
                    labelMap.put(c.getScmPullRequestId(), list);
                });

        return labelMap;

    }
    //endregion

    private Long getMinReviewedTime(List<DbScmReview> reviewList) {

        OptionalLong approvalTime = reviewList.stream()
                .mapToLong(r -> r.getReviewedAt())
                .min();

        return approvalTime.isPresent() ? approvalTime.getAsLong() : null;

    }
}
