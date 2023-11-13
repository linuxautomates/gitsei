package io.levelops.aggregations_shared.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.aggregations_shared.services.AutomationRulesEngine;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.database.github.DbGithubProject;
import io.levelops.commons.databases.models.database.github.DbGithubProjectCard;
import io.levelops.commons.databases.models.database.github.DbGithubProjectColumn;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmIssue;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.DbScmTag;
import io.levelops.commons.databases.services.GithubAggService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.utils.CommaListSplitter;
import io.levelops.controlplane.trigger.strategies.JobTags;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.integrations.github.models.GithubIterativeScanQuery;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubEvent;
import io.levelops.integrations.github.models.GithubIssue;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubTag;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.models.database.scm.DbScmPullRequest.SEPARATE_APPROVAL_AND_COMMENT;

@Log4j2
@Service
public class GithubAggHelperService {
    public static final int TOTAL_LINES_OF_CHANGE = 5000;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");

    private final ScmAggService scmAggService;
    private final GithubAggService githubAggService;
    private final EventsClient eventsClient;
    private final AutomationRulesEngine automationRulesEngine;
    private final ObjectMapper mapper;
    private final List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist;
    private final Set<String> prSeparateApprovalAndCommentTenantsWhitelist;

    private final IntegrationService integrationService;


    @Autowired
    public GithubAggHelperService(ScmAggService aggService, GithubAggService githubAggService, IntegrationService integrationService,
                                  EventsClient eventsClient, ObjectMapper mapper, AutomationRulesEngine automationRulesEngine,
                                  @Qualifier("scmCommitsInsertV2integrationIdWhitelist") List<IntegrationWhitelistEntry> scmCommitsInsertV2integrationIdWhitelist,
                                  @Value("${GITHUB_PR_SEPARATE_APPROVAL_AND_COMMENT_TENANTS_WHITELIST:}") String prSeparateApprovalAndCommentTenantsWhitelistString) {
        this.scmAggService = aggService;
        this.githubAggService = githubAggService;
        this.integrationService = integrationService;
        this.eventsClient = eventsClient;
        this.automationRulesEngine = automationRulesEngine;
        this.mapper = mapper;
        this.scmCommitsInsertV2integrationIdWhitelist = scmCommitsInsertV2integrationIdWhitelist;
        this.prSeparateApprovalAndCommentTenantsWhitelist = CommaListSplitter.splitToStream(prSeparateApprovalAndCommentTenantsWhitelistString)
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    public void processRepositoryCommits(GithubRepository repository, String customer, String integrationId) {
        AtomicInteger commitCount = new AtomicInteger();
        repository.getEvents().stream()
                .filter(ev -> "PushEvent".equals(ev.getType()))
                .forEach(ev -> ev.getCommits()
                        .forEach(commit -> {
                            processCommit(commit, ev, repository, customer, integrationId);
                            commitCount.getAndIncrement();
                        }));
        log.info("Total commit count for repo: {} is {}", repository.getName(), commitCount);
    }

    public void processCommit(
            GithubCommit commit,
            GithubEvent ev,
            GithubRepository repository,
            String customer,
            String integrationId) {
        boolean useScmCommitInsertV2 = ScmAggUtils.useScmCommitsInsertV2(scmCommitsInsertV2integrationIdWhitelist, customer, integrationId);
        log.info("company {}, integrationId {}, useScmCommitInsertV2 {}", customer, integrationId, useScmCommitInsertV2);

        // This if condition is a temporary measure for PROP-1760. We should not silently
        // fail if the gitCommitter field is null under normal situations because this is how
        // we determine the time of commit
        if (commit.getGitCommitter() != null) {
            //Build DB Git Commit
            long committedAt = TimeUnit.MILLISECONDS.toSeconds(commit.getGitCommitter().getDate().getTime());
            Long pushEventTime = ev.getCreatedAt().toInstant().getEpochSecond();
            DbScmCommit dbScmCommitFromGCS = DbScmCommit.fromGithubCommit(
                    commit, repository.getId(), integrationId, committedAt, pushEventTime);
            Optional<DbScmCommit> opt = scmAggService.getCommit(customer, dbScmCommitFromGCS.getCommitSha(), repository.getId(), integrationId);
            if (opt.isEmpty()) {
                log.info("Process Git Commit Customer {}, integrationId {}, commitSha {} commit saved to db", customer, integrationId, dbScmCommitFromGCS.getCommitSha());
                List<DbScmFile> dbScmFiles = DbScmFile.fromGithubCommit(commit, repository.getId(), integrationId, committedAt);
                try {
                    if (ScmAggUtils.isRelevant(customer, RELEVANT_TENANT_IDS)) {
                        if (ScmAggUtils.isChangeVolumeLessThanXLines(dbScmCommitFromGCS, TOTAL_LINES_OF_CHANGE)) {
                            if (useScmCommitInsertV2) {
                                scmAggService.insertV2(customer, dbScmCommitFromGCS, dbScmFiles);
                            } else {
                                scmAggService.insert(customer, dbScmCommitFromGCS, dbScmFiles);
                            }
                        } else {
                            log.info("Commit not inserted as lines of change greater than {} for Customer {}, integrationId {}," +
                                    " commitSha {} commit ", TOTAL_LINES_OF_CHANGE, customer, integrationId, dbScmCommitFromGCS.getCommitSha());
                        }
                    } else {
                        if (useScmCommitInsertV2) {
                            scmAggService.insertV2(customer, dbScmCommitFromGCS, dbScmFiles);
                        } else {
                            scmAggService.insert(customer, dbScmCommitFromGCS, dbScmFiles);
                        }
                    }
                } catch (SQLException e) {
                    log.error("Failed to insert SCM commits and files for customer={}, integrationId={}", customer, integrationId, e);
                }
            } else {
                updateCommitBranch(customer, integrationId, pushEventTime, dbScmCommitFromGCS, opt.get());
            }
        } else {
            log.warn("Ignoring commit because the gitCommitter field was not found.");
        }
    }

    private void updateCommitBranch(String customer, String integrationId,
                                    Long eventTime, DbScmCommit scmCommitFromGCS,
                                    DbScmCommit scmCommitFromDb) {
        if (scmCommitFromDb.getCommitPushedAt() != null && scmCommitFromDb.getCommitPushedAt() > eventTime) {
            if (StringUtils.isEmpty(scmCommitFromDb.getId()) || StringUtils.isEmpty(scmCommitFromGCS.getBranch())) {
                log.warn("Not updating SCM commit branch for commitSha={}: missing id or branch", scmCommitFromGCS.getCommitSha());
                return;
            }
            try {
                scmAggService.updateCommitBranchForCommit(customer, UUID.fromString(scmCommitFromDb.getId()), scmCommitFromGCS.getBranch());
            } catch (SQLException e) {
                log.error("Failed to update SCM commit branch for customer={}, integrationId={}", customer, integrationId, e);
            }
        }
    }

    private ImmutablePair<DbScmPullRequest, Boolean> persistGitPrToDb(String customer, String integrationId, String repositoryId, GithubPullRequest gitPr, Integration it) {
        DbScmPullRequest dbGitPr = null;
        boolean shouldSeparatePRApprovalAndComment = prSeparateApprovalAndCommentTenantsWhitelist.contains(customer);
        if (shouldSeparatePRApprovalAndComment) {
            log.info("GitPRReviews Sync - customer {}, integrationId {}, SEPARATE_APPROVAL_AND_COMMENT = true", customer, integrationId);
            dbGitPr = DbScmPullRequest.fromGithubPullRequest(gitPr, repositoryId, integrationId, SEPARATE_APPROVAL_AND_COMMENT, it);
        } else {
            dbGitPr = DbScmPullRequest.fromGithubPullRequest(gitPr, repositoryId, integrationId, it);
        }
        try {
            DbScmPullRequest existingDbGitPr = scmAggService.getPr(customer, dbGitPr.getNumber(), repositoryId, integrationId)
                    .orElse(null);
            log.debug("automationRule Id existingDbGitPr {}", existingDbGitPr != null ? existingDbGitPr.getId() : null);
            if (existingDbGitPr == null || existingDbGitPr.getPrUpdatedAt() < dbGitPr.getPrUpdatedAt()) {
                String id = scmAggService.insert(customer, dbGitPr);
                dbGitPr = dbGitPr.toBuilder().id(id).build();
                log.debug("automationRule Id after {}", id);
                return ImmutablePair.of(dbGitPr, false);
            } else {
                String id = existingDbGitPr.getId();
                log.info("GitPRLabel Sync - Error Existing not in cache and Equal id {} updatedAt {} existingDb {} existingDbLabelsSize {} newLabelsSize {}", id, dbGitPr.getPrUpdatedAt(), existingDbGitPr.getPrUpdatedAt(), existingDbGitPr.getLabels().size(), dbGitPr.getPrLabels().size());
                if (existingDbGitPr.getPrUpdatedAt().equals(dbGitPr.getPrUpdatedAt())) {
                    scmAggService.syncPRLabels(customer, dbGitPr, UUID.fromString(existingDbGitPr.getId()));
                }
                if (shouldSeparatePRApprovalAndComment) {
                    int existingReviewsCount = CollectionUtils.size(existingDbGitPr.getReviews());
                    int newReviewsCount = CollectionUtils.size(dbGitPr.getReviews());

                    boolean isLatestVersion = existingDbGitPr.getPrUpdatedAt().equals(dbGitPr.getPrUpdatedAt());
                    boolean moreReviewsSeen = newReviewsCount > existingReviewsCount;
                    boolean updatePr = isLatestVersion && moreReviewsSeen;
                    log.info("GitPRReviews Sync - customer {}, integrationId {}, prId {}, updatePr {}, existingPRUpdatedAt {}, newPRUpdatedAt {}, existingReviewsCount {}, newReviewsCount {}",
                            customer, integrationId, id, updatePr, existingDbGitPr.getPrUpdatedAt(), dbGitPr.getPrUpdatedAt(), existingReviewsCount, newReviewsCount);

                    if (updatePr) {
                        scmAggService.insert(customer, dbGitPr);
                    }
                }
                return ImmutablePair.of(existingDbGitPr, true);
            }
        } catch (SQLException throwables) {
            log.error("Error persisting DbScmPullRequest {}", dbGitPr, throwables);
            return null;
        }
    }

    private Map<String, Object> buildEventData(GithubPullRequest pullRequest, List<String> productIds, String prDBId, String repoOwner) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", pullRequest.getId());
        data.put("pr_number", MoreObjects.firstNonNull(pullRequest.getNumber(), ""));
        data.put("created_at", MoreObjects.firstNonNull(pullRequest.getCreatedAt(), ""));
        data.put("closed_at", MoreObjects.firstNonNull(pullRequest.getClosedAt(), ""));
        data.put("updated_at", MoreObjects.firstNonNull(pullRequest.getClosedAt(), ""));
        data.put("merged_at", MoreObjects.firstNonNull(pullRequest.getMergedAt(), ""));
        data.put("state", MoreObjects.firstNonNull(pullRequest.getState(), ""));
        data.put("title", MoreObjects.firstNonNull(pullRequest.getTitle(), ""));
        data.put("requester", MoreObjects.firstNonNull(pullRequest.getUser().getLogin(), ""));
        data.put("branch", MoreObjects.firstNonNull(pullRequest.getBase().getRef(), ""));
        data.put("repo_owner", MoreObjects.firstNonNull(repoOwner, ""));
        data.put("repo", MoreObjects.firstNonNull(pullRequest.getBase().getRepo().getName(), ""));
        data.put("is_locked", Boolean.TRUE.equals(pullRequest.getLocked()));
        data.put("body", MoreObjects.firstNonNull(pullRequest.getBody(), ""));
        data.put("products", productIds);
        data.put("levelops_id", prDBId);
        data.put("object_type", ObjectType.GIT_PULL_REQUEST);
        data.put("patches", CollectionUtils.emptyIfNull(pullRequest.getPatches()));
        if (CollectionUtils.isNotEmpty(pullRequest.getPatches())) {
            log.debug("automationRule Id pr db id {}, mergeCommitSha {}, patch not found", prDBId, pullRequest.getMergeCommitSha());
        } else {
            log.debug("automationRule Id pr db id {}, mergeCommitSha {}, patch found", prDBId, pullRequest.getMergeCommitSha());
        }
        return data;
    }

    @Nullable
    private EventType determineEventType(GithubPullRequest pullRequest, GithubIterativeScanQuery query, Set<String> jobTags) {
        if (query.getFrom() == null || SetUtils.emptyIfNull(jobTags).contains(JobTags.BACKWARD_SCAN_TAG)) {
            // ignore historic scan
            return null;
        }

        // if this is a new PR send the event right away.
        if (pullRequest.getCreatedAt() != null && pullRequest.getCreatedAt().toInstant().isAfter(query.getFrom().toInstant())) {
            return EventType.GITHUB_PULL_REQUEST_CREATED;
        }
        // check if another event needs to be sent
        // if the PR is closed then check if it was closed after the query time
        if ("closed".equalsIgnoreCase(pullRequest.getState()) && pullRequest.getClosedAt().toInstant().isAfter(query.getFrom().toInstant())) {
            // check if it has been merged and if so, check if it happened after the query time
            if (pullRequest.getMergedAt() != null && pullRequest.getMergedAt().toInstant().isAfter(query.getFrom().toInstant())) {
                return EventType.GITHUB_PULL_REQUEST_MERGED;
            }
            return EventType.GITHUB_PULL_REQUEST_CLOSED;
        }
        return EventType.GITHUB_PULL_REQUEST_UPDATED;
    }

    private void processGitPr(String customer, String integrationId, String repositoryId, String repoOwner, List<String> productIds, GithubIterativeScanQuery query, Set<String> jobTags, GithubPullRequest gitPr, Integration it) {
        boolean useScmCommitInsertV2 = ScmAggUtils.useScmCommitsInsertV2(scmCommitsInsertV2integrationIdWhitelist, customer, integrationId);
        log.info("company {}, integrationId {}, useScmCommitInsertV2 {}", customer, integrationId, useScmCommitInsertV2);

        //Persist PR to DB
        ImmutablePair<DbScmPullRequest, Boolean> persistResult = persistGitPrToDb(customer, integrationId, repositoryId, gitPr, it);
        if (persistResult == null) {
            return;
        }

        // persist merge commit if there is one
        try {
            processMergeCommit(customer, integrationId, repositoryId, gitPr, useScmCommitInsertV2);
        } catch (Exception e) {
            // TODO replace with actual exception
            log.warn("Failed to process merge commit for repoId={}, prId={}", repositoryId, gitPr.getId(), e);
        }

        DbScmPullRequest dbGitPr = persistResult.getLeft();
        log.debug("automationRule Id dbGitPr {}", dbGitPr);
        Boolean existing = persistResult.getRight();
        log.debug("automationRule Id existing {}", existing);

        if ((dbGitPr == null) || (existing)) {
            log.debug("automationRule Id dbGitPr is null or existing is true i.e. duplicate event, will not send event!");
            return;
        }

        //Build Event Data
        Map<String, Object> eventData = buildEventData(gitPr, productIds, dbGitPr.getId(), repoOwner);

        //Scan with Automation Rules
        automationRulesEngine.scanWithRules(customer, ObjectType.GIT_PULL_REQUEST, dbGitPr.getId(), eventData);

        //Determine Event Type
        EventType eventType = determineEventType(gitPr, query, jobTags);

        //Send Event
        if (eventType != null) {
            try {
                eventsClient.emitEvent(customer, eventType, eventData);
            } catch (EventsClientException e) {
                log.error("Error sending event for tenant={}, eventType={}, eventData={}", customer, eventType, eventData, e);
            }
        }
    }

    public void processRepositoryPrs(GithubRepository repository, String customer, String integrationId, JobDTO jobDTO, List<String> productIds) {
        AtomicInteger prCount = new AtomicInteger();
        String repoOwner = repository.getOwner().getLogin();
        GithubIterativeScanQuery query = mapper.convertValue(jobDTO.getQuery(), GithubIterativeScanQuery.class);
        Integration it = integrationService.get(customer, integrationId).orElse(null);
        repository.getPullRequests()
                .forEach(pr -> {
                    processGitPr(customer, integrationId, repository.getId(), repoOwner, productIds, query, jobDTO.getTags(), pr, it);
                    prCount.getAndIncrement();
                });
        log.info("PR count for repo {} = {}", repository.getName(), prCount);
    }

    public void emitEvent(String customer, String integrationId, Long startTime){
        try {
            log.info("Emitting GITHUB_NEW_AGGREGATION event for company {}", customer);

            IntegrationKey integrationKey = IntegrationKey.builder()
                    .tenantId(customer)
                    .integrationId(integrationId)
                    .build();

            eventsClient.emitEvent(customer, EventType.GITHUB_NEW_AGGREGATION, Map.of("integration_key", integrationKey, "start_time", startTime));

        } catch (EventsClientException e) {
            log.error("Error sending event for tenant={}, eventType={}", customer, EventType.GITHUB_NEW_AGGREGATION, e);
        }
    }


    public void updateRepositoryCommitsForDirectMerge(GithubRepository repository, String customer, String integrationId) {
        AtomicInteger commitCount = new AtomicInteger();
        repository.getEvents().stream()
                .filter(ev -> "PushEvent".equals(ev.getType()))
                .forEach(ev -> ev.getCommits()
                        .forEach(commit -> {
                            Optional<DbScmPullRequest> optionalDbScmPullRequest = scmAggService.checkCommitForDirectMerge(customer, commit.getSha(), repository.getId(), integrationId);
                            boolean directMerge = optionalDbScmPullRequest.isEmpty();
                            Optional<DbScmCommit> opt = scmAggService.getCommit(customer, commit.getSha(), repository.getId(), integrationId);
                            commitCount.getAndIncrement();
                            if (opt.isPresent()) {
                                try {
                                    scmAggService.updateDirectMergeForCommit(customer, UUID.fromString(opt.get().getId()), directMerge);
                                } catch (SQLException e) {
                                    log.error("Failed to update direct_merge of SCM commits for customer={}, integrationId={}", customer, integrationId, e);
                                }
                            }
                        }));
        log.info("Total commit count for direct merge for repo {} = {}", repository.getName(), commitCount);
    }

    public void insertRepositoryIssues(GithubRepository repository, String customer, String integrationId) {
        repository.getIssues()
                .forEach(issue -> {
                    insertGitIssue(issue, repository, customer, integrationId);
                });
        log.info("Issue count for repo {} = {}", repository.getName(), repository.getIssues().size());
    }

    public void insertGitIssue(GithubIssue issue, GithubRepository repository, String customer, String integrationId) {
        DbScmIssue gitIssue = DbScmIssue.fromGithubIssue(
                issue, repository.getId(), integrationId);
        DbScmIssue oldIssue = scmAggService.getIssue(
                        customer, gitIssue.getIssueId(), repository.getId(), integrationId)
                .orElse(null);
        if (oldIssue == null || oldIssue.getIssueUpdatedAt() < gitIssue.getIssueUpdatedAt()) {
            scmAggService.insertIssue(customer, gitIssue);
        }
    }

    public void insertRepositoryTags(GithubRepository repository, String customer, String integrationId) {
        repository.getTags()
                .forEach(tag -> {
                    insertGitTag(tag, repository, customer, integrationId);
                });
        log.info("Tag count for repo {} = {}", repository.getName(), repository.getTags().size());
    }

    public void insertGitTag(GithubTag tag, GithubRepository repository, String customer, String integrationId) {
        if (repository.getId() != null) {
            DbScmTag gitTag = DbScmTag.fromGithubTag(tag, repository.getId(), integrationId);
            try {
                scmAggService.insertTag(customer, gitTag);
            } catch (SQLException e) {
                log.error("Failed to insert the tag for customer:{}, integrationId:{}, repo:{}, tag:{}", customer, integrationId, repository.getId(), tag.getName());
            }
        } else {
            log.warn("Could not insert the tag for customer:{}, integrationId:{}, tag:{} for null repo ", customer, integrationId, tag.getName());
        }
    }

    public void processGitProject(String customer, String integrationId, GithubProject project) {
        DbGithubProject gitProject = DbGithubProject.fromProject(project, integrationId);
        DbGithubProject oldProject = githubAggService.getProject(customer, project.getId(), integrationId).orElse(null);
        if (oldProject == null || oldProject.getProjectUpdatedAt() < gitProject.getProjectUpdatedAt()) {
            String projectId = githubAggService.insert(customer, gitProject);
            if (projectId != null && project.getColumns() != null) {
                project.getColumns().forEach(column -> processGitColumn(customer, integrationId, projectId, column));
            }
        }
    }

    private void processGitColumn(String customer, String integrationId, String projectId, GithubProjectColumn column) {
        DbGithubProjectColumn gitColumn = DbGithubProjectColumn.fromProjectColumn(column, projectId);
        DbGithubProjectColumn oldColumn = githubAggService.getColumn(customer, projectId, column.getId()).orElse(null);
        if (oldColumn == null || oldColumn.getColumnUpdatedAt() < gitColumn.getColumnUpdatedAt()) {
            String columnId = githubAggService.insertColumn(customer, gitColumn);
            if (columnId != null && column.getCards() != null) {
                column.getCards().forEach(card -> processGitCard(customer, integrationId, columnId, card));
            }
        }
    }

    private void processGitCard(String customer, String integrationId, String columnId, GithubProjectCard card) {
        DbGithubProjectCard gitCard = DbGithubProjectCard.fromProjectCard(card, columnId);
        DbGithubProjectCard oldCard = githubAggService.getCard(customer, columnId, card.getId()).orElse(null);
        if (oldCard == null || oldCard.getCardUpdatedAt() < gitCard.getCardUpdatedAt()) {
            githubAggService.insertCard(customer, integrationId, gitCard);
        }
    }

    public void linkIssuesAndProjectCards(String customer, String integrationId) {
        Stream<DbGithubProjectCard> cardStream = githubAggService.streamCardsWithNullIssueId(customer, integrationId);
        cardStream.forEach(card -> linkIssueAndProjectCard(customer, integrationId, card));
    }

    private void linkIssueAndProjectCard(String customer, String integrationId, DbGithubProjectCard card) {
        String contentUrl = card.getContentUrl();
        if (contentUrl == null || contentUrl.lastIndexOf("repos") == -1 || contentUrl.lastIndexOf("/") == -1) {
            return;
        }
        String number = contentUrl.substring(contentUrl.lastIndexOf("/") + 1);
        String repo = contentUrl.substring(contentUrl.lastIndexOf("repos") + 6, contentUrl.lastIndexOf("/") - 7);
        card = card.toBuilder().number(number).repoId(repo).build();
        Optional<DbScmIssue> existing = githubAggService.getIssue(customer, integrationId, card.getRepoId(), card.getNumber());
        if (existing.isPresent()) {
            githubAggService.updateCardIssueId(customer, card.getId(), existing.get().getIssueId());
            log.info("GithubProjectCard : " + card.getId() + " is enriched with issue_id: " + existing.get().getIssueId());
        }
    }

    private void processMergeCommit(String customer, String integrationId, String repositoryId, GithubPullRequest gitPr, boolean useScmCommitInsertV2) {
        if (gitPr == null || gitPr.getMergeCommit() == null) {
            return;
        }
        GithubCommit mergeCommit = gitPr.getMergeCommit();
        String mergeCommitSha = StringUtils.firstNonBlank(mergeCommit.getSha(), gitPr.getMergeCommitSha(), "");
        if (StringUtils.isBlank(mergeCommitSha)) {
            log.warn("Skipping merge commit of repoId={}, PR={}: sha was blank", repositoryId, gitPr.getNumber());
            return;
        }

        log.info("Processing the merge commit of repoId={}, PR={}, sha={}", repositoryId, gitPr.getNumber(), gitPr.getMergeCommitSha());

        // parse dates
        Long committedAtSeconds = null;
        if (mergeCommit.getGitCommitter() != null && mergeCommit.getGitCommitter().getDate() != null) {
            committedAtSeconds = TimeUnit.MILLISECONDS.toSeconds(mergeCommit.getGitCommitter().getDate().getTime());
        }
        Long eventTimeSeconds = null;
        if (mergeCommit.getGitAuthor() != null && mergeCommit.getGitAuthor().getDate() != null) {
            eventTimeSeconds = TimeUnit.MILLISECONDS.toSeconds(mergeCommit.getGitAuthor().getDate().getTime());
        }
        committedAtSeconds = ObjectUtils.firstNonNull(committedAtSeconds, eventTimeSeconds, Instant.now().getEpochSecond());
        eventTimeSeconds = ObjectUtils.firstNonNull(eventTimeSeconds, committedAtSeconds, Instant.now().getEpochSecond());

        // sanitize commit (add missing values)
        mergeCommit = mergeCommit.toBuilder()
                .sha(mergeCommitSha)
                .message(StringUtils.defaultString(mergeCommit.getMessage()))
                .build();

        DbScmCommit dbScmCommitFromGCS = DbScmCommit.fromGithubCommit(mergeCommit, repositoryId, integrationId, committedAtSeconds, eventTimeSeconds);

        // check if commit already exists, if so, update commit branch if event is more recent
        Optional<DbScmCommit> opt = scmAggService.getCommit(customer, dbScmCommitFromGCS.getCommitSha(), repositoryId, integrationId);
        if (opt.isPresent()) {
            updateCommitBranch(customer, integrationId, eventTimeSeconds, dbScmCommitFromGCS, opt.get());
            if (doesCommitHaveEmptyLOC(opt.get())) {
                try {
                    scmAggService.updateCommitChangeVolumeStats(customer, UUID.fromString(opt.get().getId()), dbScmCommitFromGCS.getAdditions(),
                            dbScmCommitFromGCS.getDeletions(), dbScmCommitFromGCS.getChanges());
                } catch (SQLException e) {
                    log.error("Error updating merge commit volume stats for commit_sha {} ", dbScmCommitFromGCS.getCommitSha(), e);
                }
            }
            return;
        }

        log.info("Process Git Merge Commit Customer {}, integrationId {}, commitSha {} commit saved to db", customer, integrationId, dbScmCommitFromGCS.getCommitSha());
        List<DbScmFile> dbScmFiles = DbScmFile.fromGithubCommit(mergeCommit, repositoryId, integrationId, eventTimeSeconds);
        try {
            if (ScmAggUtils.isRelevant(customer, RELEVANT_TENANT_IDS)) {
                if (ScmAggUtils.isChangeVolumeLessThanXLines(dbScmCommitFromGCS, TOTAL_LINES_OF_CHANGE)) {
                    if (useScmCommitInsertV2) {
                        scmAggService.insertV2(customer, dbScmCommitFromGCS, dbScmFiles);
                    } else {
                        scmAggService.insert(customer, dbScmCommitFromGCS, dbScmFiles);
                    }
                    log.info("Merge commit inserted for customer" + customer + "---integrationId--" + integrationId + "---CommitSha---" + dbScmCommitFromGCS.getCommitSha());
                } else {
                    log.info("Merge Commit not inserted as lines of change greater than {} for Customer {}, integrationId {}," +
                            " commitSha {} commit ", TOTAL_LINES_OF_CHANGE, customer, integrationId, dbScmCommitFromGCS.getCommitSha());
                }
            } else {
                log.info("Insert of merge commit started");
                if (useScmCommitInsertV2) {
                    scmAggService.insertV2(customer, dbScmCommitFromGCS, dbScmFiles);
                } else {
                    scmAggService.insert(customer, dbScmCommitFromGCS, dbScmFiles);
                }
                log.info("Merge commit inserted for customer" + customer + "---integrationId--" + integrationId + "---CommitSha---" + dbScmCommitFromGCS.getCommitSha());
            }
        } catch (SQLException e) {
            log.error("Failed to insert SCM Merge commits and files for customer={}, integrationId={}", customer, integrationId, e);
        }
    }

    private boolean doesCommitHaveEmptyLOC(DbScmCommit dbScmCommit) {
        return dbScmCommit.getChanges() == 0 && dbScmCommit.getAdditions() == 0 && dbScmCommit.getDeletions() == 0;
    }
}

