package io.levelops.commons.databases.services.dev_productivity.engine;


import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.dev_productivity.filters.ScmActivityFilter;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivities;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivity;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivityDetails;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivityType;
import io.levelops.commons.models.DbListResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Log4j2
@Service
public class ScmActivitiesEngine {
    private static final Long DEFAULT_TIMEOUT_IN_SECS = TimeUnit.MINUTES.toSeconds(5);
    private final ScmAggService scmAggService;

    @Autowired
    public ScmActivitiesEngine(ScmAggService scmAggService) {
        this.scmAggService = scmAggService;
    }

    private Future<List<List<ScmActivity>>> processPRsCount(final ExecutorService executorService, final String company, final List<Integer> integrationIds, List<UUID> integrationUserIds, ScmActivityFilter.DISTINCT across, ImmutablePair<Long, Long> timeRange, ScmActivityType activityType, final boolean valueOnly) {
        return executorService.submit(() -> {
            if (CollectionUtils.isEmpty(integrationIds))
                return null;

            List<String> userIds = integrationUserIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());

            ScmPrFilter.ScmPrFilterBuilder bldr = ScmPrFilter.builder()
                    .calculation(ScmPrFilter.CALCULATION.count)
                    .aggInterval(AGG_INTERVAL.day_of_week)
                    .integrationIds(integrationIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList()));
            boolean useCreator = true;
            ScmPrFilter.DISTINCT prAcross = ScmPrFilter.DISTINCT.pr_created;
            if (ScmActivityType.PRS_CREATED == activityType) {
                bldr.prCreatedRange(timeRange);
            } else if (ScmActivityType.PRS_MERGED == activityType) {
                prAcross = ScmPrFilter.DISTINCT.pr_merged;
                bldr.prMergedRange(timeRange);
            } else if (ScmActivityType.PRS_CLOSED == activityType) {
                prAcross = ScmPrFilter.DISTINCT.pr_closed;
                bldr.prClosedRange(timeRange);
            } else {
                throw new RuntimeException("scmActivityType not supported");
            }

            ScmPrFilter.DISTINCT stack = ScmPrFilter.DISTINCT.creator;
            if (ScmActivityFilter.DISTINCT.repo_id == across) {
                stack = ScmPrFilter.DISTINCT.repo_id;
                useCreator = false;
            }

            ScmPrFilter scmPrFilter = bldr
                    .across(prAcross)
                    .creators(useCreator ? userIds : null)
                    .build();

            DbListResponse<DbAggregationResult> aggregationResult = scmAggService.stackedPrsGroupBy(company, scmPrFilter, List.of(stack), null, valueOnly);

            List<List<ScmActivity>> result = Collections.EMPTY_LIST;

            if (aggregationResult.getTotalCount() > 0) {
                result = aggregationResult.getRecords().stream().filter(record -> CollectionUtils.isNotEmpty(record.getStacks()))
                        .map(record -> {
                            return record.getStacks().stream().map(rec -> {
                                return ScmActivity.builder()
                                        .key(rec.getKey())
                                        .additionalKey(rec.getAdditionalKey())
                                        .dayOfWeek(record.getKey())
                                        .scmActivityType(activityType)
                                        .result(Math.toIntExact(rec.getCount()))
                                        .build();
                            }).collect(Collectors.toList());
                        }).collect(Collectors.toList());
            }
            return result;
        });
    }

    private Future<List<List<ScmActivity>>> processCommitsCount(final ExecutorService executorService, final String company, List<Integer> integrationIds, List<UUID> integrationUserIds, ScmActivityFilter.DISTINCT across, ImmutablePair<Long, Long> timeRange, ScmActivityType activityType) {
        return executorService.submit(() -> {

            if (CollectionUtils.isEmpty(integrationIds))
                return null;

            List<String> userIds = integrationUserIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());

            ScmCommitFilter.DISTINCT commitAcross = ScmCommitFilter.DISTINCT.committer;
            boolean useAuthor = true;
            if (ScmActivityFilter.DISTINCT.repo_id == across) {
                commitAcross = ScmCommitFilter.DISTINCT.repo_id;
                useAuthor = false;
            }

            ScmCommitFilter commitsFilter = ScmCommitFilter.builder()
                    .calculation(ScmCommitFilter.CALCULATION.count)
                    .aggInterval(AGG_INTERVAL.day_of_week)
                    .integrationIds(integrationIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList()))
                    .authors(useAuthor ? userIds : null)
                    .across(ScmCommitFilter.DISTINCT.trend)
                    .committedAtRange(timeRange)
                    .build();

            DbListResponse<DbAggregationResult> aggregationResult = scmAggService.stackedCommitsGroupBy(company, commitsFilter, List.of(commitAcross), null);

            List<List<ScmActivity>> result = Collections.EMPTY_LIST;
            if (aggregationResult.getTotalCount() > 0) {
                result = aggregationResult.getRecords().stream().filter(record -> CollectionUtils.isNotEmpty(record.getStacks()))
                        .map(record -> {
                            return record.getStacks().stream().map(rec -> {
                                return ScmActivity.builder()
                                        .key(rec.getKey())
                                        .additionalKey(rec.getAdditionalKey())
                                        .dayOfWeek(record.getKey())
                                        .scmActivityType(activityType)
                                        .result(Math.toIntExact(rec.getCount()))
                                        .build();
                            }).collect(Collectors.toList());
                        }).collect(Collectors.toList());
            }
            return result;
        });
    }

    public List<ScmActivities> calculateScmActivities(final String company, final List<UUID> integrationUserIds, final List<Integer> integrationIds, ScmActivityFilter.DISTINCT across, ImmutablePair<Long, Long> timeRange, final Long timeOutInSeconds, final boolean valueOnly) throws Exception {
        Long effectiveTimeOutInSecs = MoreObjects.firstNonNull(timeOutInSeconds, DEFAULT_TIMEOUT_IN_SECS);

        ExecutorService executorService = Executors.newFixedThreadPool(ScmActivityType.values().length);
        List<Future<List<List<ScmActivity>>>> futures = new ArrayList<>();

        futures.add(processPRsCount(executorService, company, integrationIds, integrationUserIds, across, timeRange, ScmActivityType.PRS_CREATED, valueOnly));
        futures.add(processPRsCount(executorService, company, integrationIds, integrationUserIds, across, timeRange, ScmActivityType.PRS_MERGED, valueOnly));
        futures.add(processPRsCount(executorService, company, integrationIds, integrationUserIds, across, timeRange, ScmActivityType.PRS_CLOSED, valueOnly));
        futures.add(processPRsReviewsCount(executorService, company, integrationIds, integrationUserIds, across, timeRange, ScmActivityType.PRS_COMMENTS));
        futures.add(processCommitsCount(executorService, company, integrationIds, integrationUserIds, across, timeRange, ScmActivityType.COMMITS_CREATED));

        long endTime = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(effectiveTimeOutInSecs);

        List<List<List<ScmActivity>>> allActivities = futures.stream()
                .map(f -> {
                    try {
                        return f.get(Math.max(0, endTime - System.currentTimeMillis()), TimeUnit.MILLISECONDS);
                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        log.error("Scm Activities calculation failed for company {}, integrationUserIds {}, integrationIds {}, across {}, timeRange {}", company, integrationUserIds, integrationIds, across, timeRange, e);
                        f.cancel(true);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        Map<String, ScmActivities> map = Maps.newHashMap();

        allActivities.forEach(record -> {
            record.forEach(list -> {
                list.stream().forEach(rec -> {
                    ScmActivities activities = null;
                    ScmActivityType activityType = rec.getScmActivityType();
                    if (map.containsKey(rec.getKey())) {
                        activities = map.get(rec.getKey());
                        ScmActivityDetails detail = activities.getActivityDetails().stream().filter(r -> r.getDayOfWeek().equals(rec.getDayOfWeek())).findFirst().orElse(null);
                        List<ScmActivityDetails> stackList = new ArrayList<>(activities.getActivityDetails());
                        if (detail == null) {
                            detail = ScmActivityDetails.builder()
                                    .dayOfWeek(rec.getDayOfWeek())
                                    .numPrsCreated(ScmActivityType.PRS_CREATED == activityType ? rec.getResult() : null)
                                    .numPrsMerged(ScmActivityType.PRS_MERGED == activityType ? rec.getResult() : null)
                                    .numPrsClosed(ScmActivityType.PRS_CLOSED == activityType ? rec.getResult() : null)
                                    .numPrsComments(ScmActivityType.PRS_COMMENTS == activityType ? rec.getResult() : null)
                                    .numCommitsCreated(ScmActivityType.COMMITS_CREATED == activityType ? rec.getResult() : null)
                                    .build();
                            stackList.add(detail);
                            activities = activities.toBuilder()
                                    .userName(across == ScmActivityFilter.DISTINCT.integration_user && activities.getUserName() == null ? rec.getAdditionalKey() : activities.getUserName())
                                    .activityDetails(stackList)
                                    .build();
                        } else {
                            AtomicInteger i = new AtomicInteger();
                            int index = stackList.stream()
                                    .peek(v -> i.incrementAndGet())
                                    .anyMatch(r -> r.getDayOfWeek().equals(rec.getDayOfWeek())) ?
                                    i.get() - 1 : -1;

                            detail = detail.toBuilder()
                                    .numPrsCreated(ScmActivityType.PRS_CREATED == activityType ? rec.getResult() : detail.getNumPrsCreated())
                                    .numPrsMerged(ScmActivityType.PRS_MERGED == activityType ? rec.getResult() : detail.getNumPrsMerged())
                                    .numPrsClosed(ScmActivityType.PRS_CLOSED == activityType ? rec.getResult() : detail.getNumPrsClosed())
                                    .numPrsComments(ScmActivityType.PRS_COMMENTS == activityType ? rec.getResult() : detail.getNumPrsComments())
                                    .numCommitsCreated(ScmActivityType.COMMITS_CREATED == activityType ? rec.getResult() : detail.getNumCommitsCreated())
                                    .build();
                            stackList.set(index, detail);
                            activities = activities.toBuilder()
                                    .userName(across == ScmActivityFilter.DISTINCT.integration_user && activities.getUserName() == null ? rec.getAdditionalKey() : activities.getUserName())
                                    .activityDetails(stackList)
                                    .build();
                        }
                    } else {
                        ScmActivityDetails detail = ScmActivityDetails.builder()
                                .dayOfWeek(rec.getDayOfWeek())
                                .numPrsCreated(ScmActivityType.PRS_CREATED == activityType ? rec.getResult() : null)
                                .numPrsMerged(ScmActivityType.PRS_MERGED == activityType ? rec.getResult() : null)
                                .numPrsClosed(ScmActivityType.PRS_CLOSED == activityType ? rec.getResult() : null)
                                .numPrsComments(ScmActivityType.PRS_COMMENTS == activityType ? rec.getResult() : null)
                                .numCommitsCreated(ScmActivityType.COMMITS_CREATED == activityType ? rec.getResult() : null)
                                .build();
                        List<ScmActivityDetails> stackList = new ArrayList<>();
                        stackList.add(detail);
                        activities = ScmActivities.builder()
                                .integrationUserId(across == ScmActivityFilter.DISTINCT.integration_user ? rec.getKey() : null)
                                .userName(across == ScmActivityFilter.DISTINCT.integration_user ? rec.getAdditionalKey() : null)
                                .repoId(across == ScmActivityFilter.DISTINCT.repo_id ? rec.getKey() : null)
                                .activityDetails(stackList)
                                .build();
                    }
                    map.put(rec.getKey(), activities);
                });
            });
        });

        executorService.shutdownNow();

        List<ScmActivities> scmActivities = new ArrayList<>(map.values());
        log.info("scmActivities = {}", scmActivities);
        return scmActivities;
    }

    private Future<List<List<ScmActivity>>> processPRsReviewsCount(ExecutorService executorService, String company, List<Integer> integrationIds, List<UUID> integrationUserIds, ScmActivityFilter.DISTINCT across, ImmutablePair<Long, Long> timeRange, ScmActivityType activityType) {
        return executorService.submit(() -> {

            if (CollectionUtils.isEmpty(integrationIds))
                return null;

            if (ScmActivityType.PRS_COMMENTS != activityType)
                throw new RuntimeException("scmActivityType not supported");

            boolean useReviewer = true;
            ScmPrFilter.DISTINCT prAcross = ScmPrFilter.DISTINCT.commenter;
            if (ScmActivityFilter.DISTINCT.repo_id == across) {
                useReviewer = false;
                prAcross = ScmPrFilter.DISTINCT.repo_id;
            }

            List<String> userIds = integrationUserIds.stream()
                    .map(UUID::toString)
                    .collect(Collectors.toList());

            ScmPrFilter scmPrFilter = ScmPrFilter.builder()
                    .calculation(ScmPrFilter.CALCULATION.count)
                    .aggInterval(AGG_INTERVAL.day_of_week)
                    .integrationIds(integrationIds.stream()
                            .map(String::valueOf)
                            .collect(Collectors.toList()))
                    .across(prAcross)
                    .reviewerIds(useReviewer ? userIds : null)
                    .prReviewedRange(timeRange)
                    .build();

            DbListResponse<DbAggregationResult> aggregationResult = scmAggService.groupByAndCalculatePrReviews(company, scmPrFilter, null);

            List<List<ScmActivity>> result = Collections.EMPTY_LIST;

            if (aggregationResult.getTotalCount() > 0) {
                result = aggregationResult.getRecords().stream().map(record -> {

                    ScmActivity activity = ScmActivity.builder()
                            .key(record.getKey())
                            .dayOfWeek(record.getAdditionalKey())
                            .scmActivityType(activityType)
                            .result(Math.toIntExact(record.getCount()))
                            .build();
                    return List.of(activity);
                }).collect(Collectors.toList());
            }

            return result;
        });
    }
}
