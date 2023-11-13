package io.levelops.aggregations.helpers;

import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmPullRequest;
import io.levelops.commons.databases.models.database.scm.RepoConfigEntryMatcher;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.helix_swarm.models.HelixSwarmReview;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
@Service
public class HelixSwarmAggHelper {
    public static final String UNKNOWN_REPO = "unknown";
    public static final String UNKNOWN_PROJECT = "unknown";
    private static final Set<String> UNKNOWN_REPO_IDS = Set.of(UNKNOWN_REPO);
    public static final String REVIEWS_DATA_TYPE = "reviews";
    private final JobDtoParser jobDtoParser;
    private final IntegrationTrackingService trackingService;
    private final ScmAggService scmAggService;

    @Autowired
    public HelixSwarmAggHelper(JobDtoParser jobDtoParser, IntegrationTrackingService trackingService,
                               ScmAggService scmAggService) {
        this.jobDtoParser = jobDtoParser;
        this.trackingService = trackingService;
        this.scmAggService = scmAggService;
    }

    public boolean setupHelixSwarmReviews(String messageId, String customer,
                                          String integrationId,
                                          List<IntegrationConfig.RepoConfigEntry> configEntries,
                                          MultipleTriggerResults triggerResults,
                                          Date currentTime) {
        final AtomicInteger prsCount = new AtomicInteger();
        final AtomicInteger prDBErrorsCount = new AtomicInteger();

        final RepoConfigEntryMatcher repoConfigEntryMatcher = new RepoConfigEntryMatcher(configEntries);
        Date truncatedDate = DateUtils.truncate(currentTime, Calendar.DATE);
        boolean success = jobDtoParser.applyToResults(customer, REVIEWS_DATA_TYPE, HelixSwarmReview.class,
                triggerResults.getTriggerResults().get(0),
                review -> processHelixSwarmReview(messageId, customer, integrationId, truncatedDate, repoConfigEntryMatcher, review,
                        prsCount, prDBErrorsCount),
                List.of());
        log.info("messageId {}, customer {}, integrationId {}, prsCount {}, prDBErrorsCount {}", messageId, customer, integrationId, prsCount.get(), prDBErrorsCount.get());
        if (success) {
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(truncatedDate.toInstant().getEpochSecond())
                            .build());
        }
        return success;
    }

    private boolean prIsForNonUnknownRepo(Set<String> repoIds) {
        //Repo Ids is empty, cannot say it is from non unknown repos
        if(CollectionUtils.isEmpty(repoIds)) {
            return false;
        }
        //We have more than one repo ids, atleast one of them is from non unknown repos
        if(repoIds.size() >1) {
            return true;
        }
        //If set has only one element & it is not unknown then it is from non unknown repos
        return (!repoIds.contains(UNKNOWN_REPO));
    }

    void processHelixSwarmReview(final String messageId, final String customer, final String integrationId, final Date truncatedDate, final RepoConfigEntryMatcher repoConfigEntryMatcher, final HelixSwarmReview review,
                                 final AtomicInteger prsCount, final AtomicInteger prDBErrorsCount){
        //parseHelixReviewGetRepoIds currently returns atleast SingletonSet. If it starts returning emptySet add check
        Set<String> repoIds = parseHelixReviewGetRepoIds(review, customer, integrationId, repoConfigEntryMatcher);
        DbScmPullRequest dbScmPullRequest = DbScmPullRequest.fromHelixSwarmReview(review, repoIds, integrationId);
        try {
            String prId = scmAggService.insert(customer, dbScmPullRequest);
            log.info("messageId {}, customer {}, integrationId {}, pr count {}, pr id {}, pr no {}", messageId, customer, integrationId, prsCount.incrementAndGet(), prId, dbScmPullRequest.getNumber());
            log.info("messageId {}, customer {}, integrationId {}, pr id {}, pr no {}, repoIds {}", messageId, customer, integrationId, prId, dbScmPullRequest.getNumber(), repoIds);
            if(prIsForNonUnknownRepo(repoIds)) {
                log.info("messageId {}, customer {}, integrationId {}, pr id {} is for non unknown repo", messageId, customer, integrationId, prId);
                try {
                    Boolean deleteDuplicatePr = scmAggService.delete(customer, dbScmPullRequest.getNumber(), UNKNOWN_REPO, UNKNOWN_PROJECT, integrationId);
                    log.info("messageId {}, customer {}, integrationId {}, pr id {}, duplicate prs deleted {}", messageId, customer, integrationId, prId, deleteDuplicatePr);
                } catch (RuntimeException | SQLException e) {
                    log.error("Error deleting duplicate pr for unknown repo id", e);
                }
            }
        } catch (SQLException e) {
            log.error("messageId {} setupHelixSwarmReviews: error inserting review: {}", messageId, review, e);
            prDBErrorsCount.incrementAndGet();
        }
    }

    Set<String> parseHelixReviewGetRepoIds(final HelixSwarmReview review, final String customer, final String integrationId, RepoConfigEntryMatcher repoConfigEntryMatcher) {
        Set<String> repoIdsSet = parseHelixReviewGetRepoIdsInternal(review, customer, integrationId, repoConfigEntryMatcher);
        repoIdsSet = (CollectionUtils.isNotEmpty(repoIdsSet)) ? repoIdsSet : UNKNOWN_REPO_IDS;
        log.debug("repoIdsSet = {}", repoIdsSet);
        return repoIdsSet;
    }

    Set<String> parseHelixReviewGetRepoIdsInternal(final HelixSwarmReview review, final String customer, final String integrationId, RepoConfigEntryMatcher repoConfigEntryMatcher) {
        //First check if PR has File Info
        if(CollectionUtils.isNotEmpty(review.getFileInfos())) {
            return review.getFileInfos().stream()
                    .map(reviewFileInfo -> repoConfigEntryMatcher.matchPrefix(reviewFileInfo.getDepotFile()))
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toSet());
        }
        //Second check if PR has commit_shas & if they exist in db
        List<String> commitShas = CollectionUtils.emptyIfNull(review.getCommits()).stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .distinct()
                .collect(Collectors.toList());
        if(CollectionUtils.isNotEmpty(commitShas)) {
            List<DbScmCommit> scmCommits = null;
            try {
                scmCommits = scmAggService.getCommits(customer, commitShas, integrationId);
            } catch (RuntimeException e) {
                log.warn("Error fetching commits for integrationId {}, commitShas {}", integrationId, commitShas, e);
            }
            //If db has scm commits for the commitShas use it
            if(CollectionUtils.isNotEmpty(scmCommits)) {
                return scmCommits.stream()
                        .filter(commit -> CollectionUtils.isNotEmpty(commit.getRepoIds()))
                        .map(DbScmCommit::getRepoIds)
                        .flatMap(Collection::stream)
                        .filter(r -> !UNKNOWN_REPO.equals(r))
                        .collect(Collectors.toSet());
            }
        }

        //Third parse review versions
        return CollectionUtils.emptyIfNull(review.getVersions()).stream()
                .map(version -> repoConfigEntryMatcher.matchPrefix(version.getStream()))
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toSet());
    }
}
