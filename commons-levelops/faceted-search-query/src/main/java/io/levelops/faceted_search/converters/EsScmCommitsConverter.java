package io.levelops.faceted_search.converters;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.DateHistogramBucket;
import co.elastic.clients.elasticsearch._types.aggregations.MultiBucketBase;
import co.elastic.clients.elasticsearch._types.aggregations.MultiTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.SingleBucketAggregateBase;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import io.levelops.commons.databases.models.database.scm.DbScmCommit;
import io.levelops.commons.databases.models.database.scm.DbScmContributorAgg;
import io.levelops.commons.databases.models.database.scm.DbScmFile;
import io.levelops.commons.databases.models.database.scm.DbScmRepoAgg;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmFilesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.faceted_search.db.models.scm.EsScmCommit;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.converters.DbScmConverters.LANGUAGE_FILE_MAP;
import static io.levelops.faceted_search.utils.EsUtils.getCalenderInterval;

@Log4j2
public class EsScmCommitsConverter {

    public static List<ScmCommitFilter.DISTINCT> ACROSS_USERS = List.of(ScmCommitFilter.DISTINCT.committer, ScmCommitFilter.DISTINCT.author);

    public static List<EsScmCommit> getEsScmCommitList(List<DbScmCommit> dbScmCommitList){

        return dbScmCommitList.stream()
                .map(commit -> EsScmCommit.builder()
                        .id(commit.getId())
                        .additions(commit.getAdditions())
                        .author(commit.getAuthor())
                        .authorId(commit.getAuthorId())
                        .authorInfo(commit.getAuthorInfo())
                        .branch(commit.getBranch())
                        .commitSha(commit.getCommitSha())
                        .changes(commit.getChanges())
                        .committer(commit.getCommitter())
                        .committerId(commit.getCommitterId())
                        .committerInfo(commit.getCommitterInfo())
                        .commitUrl(commit.getCommitUrl())
                        .commitPushedAt(commit.getCommitPushedAt())
                        .committedAt(commit.getCommittedAt())
                        .createdAt(commit.getCreatedAt())
                        .dayOfWeek(commit.getDayOfWeek())
                        .deletions(commit.getDeletions())
                        .directMerge(commit.getDirectMerge())
                        .fileCommitList(commit.getFileCommitList())
                        .filesCt(commit.getFilesCt())
                        .fileTypes(commit.getFileTypes())
                        .hasIssueKeys(commit.getHasIssueKeys())
                        .integrationId(commit.getIntegrationId())
                        .ingestedAt(commit.getIngestedAt())
                        .issueKeys(commit.getIssueKeys())
                        .legacyLinesCount(commit.getLegacyLinesCount())
                        .linesRefactoredCount(commit.getLinesRefactoredCount())
                        .message(commit.getMessage())
                        .pctLegacyLines(commit.getPctLegacyLines())
                        .pctNewLines(commit.getPctNewLines())
                        .project(commit.getProject())
                        .pctRefactoredLines(commit.getPctRefactoredLines())
                        .repoIds(commit.getRepoIds())
                        .tags(commit.getTags())
                        .technologies(commit.getTechnologies())
                        .technology(commit.getTechnology())
                        .totalLinesAdded(commit.getTotalLinesAdded())
                        .totalLinesChanged(commit.getTotalLinesChanged())
                        .totalLinesRemoved(commit.getTotalLinesRemoved())
                        .vcsType(commit.getVcsType())
                        .workitemIds(commit.getWorkitemIds())
                        .prList(commit.getPrList())
                        .prCount(commit.getPrCount())
                        .committerPrList(commit.getCommitterPrList())
                        .committerPrCount(commit.getCommitterPrCount())
                        .loc((commit.getAdditions() == null ? 0 : commit.getAdditions()) + (commit.getChanges() == null ? 0 : commit.getChanges()))
                        .build()).collect(Collectors.toList());
    }

    public static List<DbAggregationResult> getAggResultFromSearchResponse(SearchResponse<Void> searchResponse, ScmCommitFilter filter, boolean valuesOnly, Map<String, String> userIdMap) {

        List<DbAggregationResult> dbAggregationResults = new ArrayList<>();
        ScmCommitFilter.DISTINCT across = filter.getAcross();
        ScmCommitFilter.CALCULATION calculation = filter.getCalculation();
        switch (across) {

            case author:
            case committer:
            case repo_id:
            case project:
            case file_type:
            case commit_branch:
            case vcs_type:
                List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_" + across)
                        .sterms().buckets().array();
                termsBuckets.forEach(term -> {
                    dbAggregationResults.add(getDbAggResponse(term, filter, valuesOnly, userIdMap));
                });
                break;

            case technology:
                List<StringTermsBucket> techBuckets = searchResponse.aggregations().get("across_" + across).nested()
                        .aggregations().get("technology").sterms().buckets().array();
                techBuckets.forEach(term -> {
                    String key = term.key();
                    long count = term.docCount();
                    DbAggregationResult res = DbAggregationResult.builder()
                            .key(key)
                            .count(count)
                            .build();
                    dbAggregationResults.add(res);
                });
                break;

            case trend:

                List<DateHistogramBucket> dateBucket = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                if(ScmCommitFilter.CALCULATION.commit_count_only.equals(calculation)){
                    dateBucket.forEach(term -> {
                        DbAggregationResult res = getDbAggResponseForCommitCount(term, filter, valuesOnly, userIdMap);
                        if (res.getCount() != 0) {
                            dbAggregationResults.add(res);
                        }
                    });
                } else {
                    dateBucket.forEach(term -> {
                        DbAggregationResult res = getDbAggResponse(term, filter, valuesOnly, userIdMap);
                        if (res.getCount() != 0) {
                            dbAggregationResults.add(res);
                        }
                    });
                }
                break;

            case code_change:
                Aggregate smallChange = searchResponse.aggregations().get("small_code");
                Aggregate mediumChange = searchResponse.aggregations().get("medium_code");
                Aggregate largeChange = searchResponse.aggregations().get("large_code");

                DbAggregationResult res = getDbAggResponseForCode("small", smallChange, across);
                if (res != null) {
                    dbAggregationResults.add(res);
                }
                res = getDbAggResponseForCode("medium", mediumChange, across);
                if (res != null) {
                    dbAggregationResults.add(res);
                }

                res = getDbAggResponseForCode("large", largeChange, across);
                if (res != null) {
                    dbAggregationResults.add(res);

                }

                break;

            case code_category:
                Aggregate legacyRefactoredLines = searchResponse.aggregations().get("total_legacy_refactored_lines");
                Aggregate newLines = searchResponse.aggregations().get("total_new_lines");
                Aggregate refactoredLines = searchResponse.aggregations().get("total_refactored_lines");

                res = getDbAggResponseForCode("legacy_refactored_lines", refactoredLines, across);
                if (res != null) {
                    dbAggregationResults.add(res);
                }

                res = getDbAggResponseForCode("new_lines", newLines, across);
                if (res != null) {
                    dbAggregationResults.add(res);
                }

                res = getDbAggResponseForCode("refactored_lines", legacyRefactoredLines, across);
                if (res != null) {
                    dbAggregationResults.add(res);
                }

                break;

            default:
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid across provided " + across);
        }
        return dbAggregationResults;
    }

    public static List<DbAggregationResult> getAggResultForCodingDays(SearchResponse<Void> searchResponse, ScmCommitFilter filter, Map<String, String> userIdMap) {

        List<DbAggregationResult> list = new ArrayList<>();
        ScmCommitFilter.DISTINCT across = filter.getAcross();

        double interval = getIntervalDays(filter);

        long duration = 6l;
        if (filter.getCommittedAtRange() != null && filter.getCommittedAtRange().getLeft() != null &&
                filter.getCommittedAtRange().getRight() != null) {
            Instant lowerBound = DateUtils.fromEpochSecond(filter.getCommittedAtRange().getLeft());
            Instant upperBound = DateUtils.fromEpochSecond(filter.getCommittedAtRange().getRight());
            duration = Duration.between(lowerBound, upperBound).toDays();
        }
        double finalDuration = duration;
        double finalInterval = interval;
        log.info("Looking commits for {} days duration", finalDuration);
        switch (across) {
            case committer:
            case author:

                List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();

                termsBuckets.forEach(term -> {
                    String key = term.key();
                    String additionalKey = userIdMap.getOrDefault(key, "NONE");
                    List<StringTermsBucket> sTermBuckets = term.aggregations().get("coding_days").sterms().buckets().array();
                    sTermBuckets.forEach(sterm -> {
                        String day = sterm.key();
                        long commitSize = (long) sterm.aggregations().get("commit_size_change").nested().aggregations().get("commit_size").sum().value();
                        int codingDays = sterm.aggregations().get("committed_at").lterms().buckets().array().size();
                        double mean = (codingDays / finalDuration) * finalInterval;

                        DbAggregationResult res = DbAggregationResult.builder()
                                .key(key)
                                .additionalKey(additionalKey)
                                .commitSize(commitSize)
                                .dayOfWeek(day)
                                .mean(mean)
                                .build();
                        list.add(res);
                    });
                });

                break;
            default:

                List<StringTermsBucket> buckets = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();

                buckets.forEach(term -> {
                    String key = term.key();
                    List<StringTermsBucket> sTermBuckets = term.aggregations().get("coding_days").sterms().buckets().array();
                    sTermBuckets.forEach(sterm -> {
                        String day = sterm.key();
                        long commitSize = (long) sterm.aggregations().get("commit_size_change").nested().aggregations().get("commit_size").sum().value();
                        int codingDays = sterm.aggregations().get("committed_at").lterms().buckets().array().size();
                        double mean = (codingDays / finalDuration) * finalInterval;
                        DbAggregationResult res = DbAggregationResult.builder()
                                .key(key)
                                .commitSize(commitSize)
                                .dayOfWeek(day)
                                .mean(mean)
                                .build();
                        list.add(res);
                    });
                });
        }

        return list;
    }

    private static double getIntervalDays(ScmCommitFilter filter){

        if(AGG_INTERVAL.day == filter.getAggInterval()) {
            return 1d;
        }
        if(AGG_INTERVAL.week == filter.getAggInterval()){
            return 7d;
        }
        if(AGG_INTERVAL.biweekly == filter.getAggInterval()){
            return 14d;
        }
        if(AGG_INTERVAL.month == filter.getAggInterval()){
            return 30d;
        }
        return 7d;
    }

    public static List<DbAggregationResult> getAggResultForCodingDaysReport(SearchResponse<Void> searchResponse, ScmCommitFilter filter, Map<String, String> userIdMap) {

        List<DbAggregationResult> list = new ArrayList<>();
        ScmCommitFilter.DISTINCT across = filter.getAcross();

        double intervalDays = getIntervalDays(filter);

        long duration = 6l;
        if (filter.getCommittedAtRange() != null && filter.getCommittedAtRange().getLeft() != null &&
                filter.getCommittedAtRange().getRight() != null) {
            Instant lowerBound = DateUtils.fromEpochSecond(filter.getCommittedAtRange().getLeft());
            Instant upperBound = DateUtils.fromEpochSecond(filter.getCommittedAtRange().getRight());
            duration = Duration.between(lowerBound, upperBound).toDays()+1;
        }
        double finalDuration = duration;
        double finalInterval = intervalDays;
        log.info("Looking commits for {} days duration", finalDuration);
        switch (across) {
            case committer:
            case author:

                List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();

                termsBuckets.forEach(term -> {
                    String key = term.key();
                    String additionalKey = userIdMap.getOrDefault(key, "NONE");
                    long codingDays = term.aggregations().get("coding_days").dateHistogram().buckets().array().size();
                    long commitSize = (long) term.aggregations().get("commit_size_change").nested().aggregations().get("commit_size").sum().value();
                    String medianValue = term.aggregations().get("commit_size_change").nested().aggregations().get("median_percentile").tdigestPercentiles().values().keyed().get("50.0");
                    double median = medianValue == null ? 0l : Double.valueOf(medianValue);
                    double mean = (codingDays / finalDuration) * finalInterval;

                    DbAggregationResult res = DbAggregationResult.builder()
                            .key(key)
                            .additionalKey(additionalKey)
                            .commitSize(commitSize)
                            .median((long) median)
                            .mean(Double.valueOf(String.format("%.2f",mean)))
                            .build();

                    list.add(res);
                });
                break;

            case trend:
                List<DateHistogramBucket> dateBucket = searchResponse.aggregations().get("across_" + across).dateHistogram().buckets().array();
                dateBucket.forEach(term -> {

                    long interval = term.key().toEpochMilli();
                    String key = Long.toString(TimeUnit.MILLISECONDS.toSeconds(interval));
                    String additionalKey =  term.keyAsString();
                    additionalKey = getCalenderInterval(filter.getAggInterval().name(), additionalKey, interval);
                    long codingDays = term.aggregations().get("coding_days").dateHistogram().buckets().array().size();
                    long commitSize = (long) term.aggregations().get("commit_size_change").nested().aggregations().get("commit_size").sum().value();
                    String medianValue = term.aggregations().get("commit_size_change").nested().aggregations().get("median_percentile").tdigestPercentiles().values().keyed().get("50.0");
                    double median = medianValue == null ? 0l : Double.valueOf(medianValue);
                    double mean = (codingDays / finalDuration) * finalInterval;

                    DbAggregationResult res = DbAggregationResult.builder()
                            .key(key)
                            .additionalKey(additionalKey)
                            .commitSize(commitSize)
                            .median((long) median)
                            .mean(mean)
                            .build();

                    list.add(res);
                });
                break;

            default:

                List<StringTermsBucket> stringTermsBuckets = searchResponse.aggregations().get("across_" + across).sterms().buckets().array();

                stringTermsBuckets.forEach(term -> {
                    String key = term.key();
                    long codingDays = term.aggregations().get("coding_days").dateHistogram().buckets().array().size();
                    long commitSize = (long) term.aggregations().get("commit_size_change").nested().aggregations().get("commit_size").sum().value();
                    String medianValue = term.aggregations().get("commit_size_change").nested().aggregations().get("median_percentile").tdigestPercentiles().values().keyed().get("50.0");
                    double median = medianValue == null ? 0l : Double.valueOf(medianValue);
                    double mean = (codingDays / finalDuration) * finalInterval;

                    DbAggregationResult res = DbAggregationResult.builder()
                            .key(key)
                            .commitSize(commitSize)
                            .median((long) median)
                            .mean(Double.valueOf(String.format("%.2f",mean)))
                            .build();

                    list.add(res);
                });
        }

        return list;
    }

    private static DbAggregationResult getDbAggResponseForCode(String key, Aggregate change, ScmCommitFilter.DISTINCT across) {

        SingleBucketAggregateBase term = null;
        long count;
        if(across.equals(ScmCommitFilter.DISTINCT.code_change)) {
            term = change.filter().aggregations().get("code_change_filter").nested();
            count =  change.filter().docCount();
        }
        else {
            term = change.nested().aggregations().get("code_change_filter").filter();
            count = (long) term.aggregations().get("files_ct").reverseNested().docCount();
        }

        if (term.docCount() == 0) {
            return null;
        }

        long ct = term.docCount();
        long addition_ct = (long) term.aggregations().get("addition_ct").sum().value();
        long deletion_ct = (long) term.aggregations().get("deletion_ct").sum().value();
        long changes_ct = (long) term.aggregations().get("changes_ct").sum().value();
        String pctValue = term.aggregations().get("pct").tdigestPercentiles().values().keyed().get("50.0");
        double medianChange = pctValue == null ? 0l :  Double.valueOf(pctValue);
        float avgChangeSize = ((float) (addition_ct + deletion_ct + changes_ct)) / count;

        double totalLines = term.aggregations().get("total_lines").sum().value();
        double legacyLines = term.aggregations().get("total_legacy_line").filter().aggregations().get("total_legacy_refactored_lines").sum().value();
        double refactoredLines = term.aggregations().get("total_refactored_line").filter().aggregations().get("total_refactored_lines").sum().value();
        double totalNewLines = term.aggregations().get("total_new_line").filter().aggregations().get("total_new_lines").sum().value();


        double pctNewLines = 0d;
        double pctRefactoredLine = 0d;
        double pctLegacyLine = 0d;

        if(totalLines != 0 ){
            pctNewLines = totalNewLines * 100 / totalLines;
            pctRefactoredLine = refactoredLines * 100 / totalLines;
            pctLegacyLine = legacyLines * 100 / totalLines;
        }
        return DbAggregationResult.builder()
                .key(key)
                .count(count)
                .filesChangedCount(ct)
                .linesAddedCount(addition_ct)
                .linesRemovedCount(deletion_ct)
                .linesChangedCount(changes_ct)
                .pctLegacyRefactoredLines(Double.valueOf(String.format("%.2f", pctRefactoredLine)))
                .pctNewLines(Double.valueOf(String.format("%.2f", pctNewLines)))
                .pctRefactoredLines(Double.valueOf(String.format("%.2f", pctLegacyLine)))
                .median((long) medianChange)
                .avgChangeSize(Float.valueOf(String.format("%.3f", avgChangeSize)))
                .build();

    }

    private static DbAggregationResult getDbAggResponse(MultiBucketBase term, ScmCommitFilter filter, boolean valuesOnly, Map<String, String> userIdMap) {

        String key = null;
        String additionalKey = null;

        if (term instanceof MultiTermsBucket) {
            additionalKey = ((MultiTermsBucket) term).key().get(0);
            key = ((MultiTermsBucket) term).key().get(1);
        } else if (term instanceof StringTermsBucket) {
            key = ((StringTermsBucket) term).key();
            if (ACROSS_USERS.contains(filter.getAcross())) {
                additionalKey = userIdMap.getOrDefault(key, "NONE");
            }
        } else if (term instanceof DateHistogramBucket) {
            long interval = (((DateHistogramBucket) term).key().toEpochMilli());
            key = Long.toString(TimeUnit.MILLISECONDS.toSeconds(interval));
            additionalKey = ((DateHistogramBucket) term).keyAsString();
            additionalKey = getCalenderInterval(filter.getAggInterval().name(), additionalKey, interval);
        }

        long count = term.docCount();

        if (valuesOnly) {
            return DbAggregationResult.builder()
                    .key(key)
                    .additionalKey(additionalKey)
                    .count(count)
                    .build();
        }

        long tot_files_ct = (long) term.aggregations().get("files_ct").sum().value();
        long addition_ct = (long) term.aggregations().get("addition_ct").sum().value();
        long deletion_ct = (long) term.aggregations().get("deletion_ct").sum().value();
        long changes_ct = (long) term.aggregations().get("changes_ct").sum().value();
        String pctValue = term.aggregations().get("pct").tdigestPercentiles().values().keyed().get("50.0");
        double medianChange = pctValue == null ? 0d : Double.valueOf(pctValue);

        double totalLines = term.aggregations().get("total_lines_changed").nested().aggregations().get("total_lines").sum().value();
        double legacyLines = term.aggregations().get("total_legacy_refactored_lines").nested().aggregations().get("legacy_filter").filter().aggregations().get("legacy_sum").sum().value();
        double refactoredLines = term.aggregations().get("total_refactored_lines").nested().aggregations().get("legacy_filter").filter().aggregations().get("refactored_sum").sum().value();
        double totalNewLines = term.aggregations().get("total_new_lines").nested().aggregations().get("legacy_filter").filter().aggregations().get("new_lines").sum().value();
        float avgChangeSize = (float) tot_files_ct / count;

        if ("lines".equals(filter.getCodeChangeUnit())) {
            avgChangeSize = ((float)addition_ct+deletion_ct+changes_ct)/count;
        }

        double pctNewLines = 0d;
        double pctRefactoredLine = 0d;
        double pctLegacyLine = 0d;

        if(totalLines != 0 ){
            pctNewLines = totalNewLines * 100 / totalLines;
            pctRefactoredLine = refactoredLines * 100 / totalLines;
            pctLegacyLine = legacyLines * 100 / totalLines;
        }

        return DbAggregationResult.builder()
                .key(key)
                .additionalKey(additionalKey)
                .count(count)
                .filesChangedCount(tot_files_ct)
                .linesAddedCount(addition_ct)
                .linesRemovedCount(deletion_ct)
                .linesChangedCount(changes_ct)
                .medianChangeSize((long) medianChange)
                .pctLegacyRefactoredLines(Double.valueOf(String.format("%.2f", pctRefactoredLine)))
                .pctNewLines(Double.valueOf(String.format("%.2f", pctNewLines)))
                .pctRefactoredLines(Double.valueOf(String.format("%.2f", pctLegacyLine)))
                .avgChangeSize(Float.valueOf(String.format("%.3f", avgChangeSize)))
                .build();
    }

    private static DbAggregationResult getDbAggResponseForCommitCount(MultiBucketBase term, ScmCommitFilter filter, boolean valuesOnly, Map<String, String> userIdMap) {

        String key = null;
        String additionalKey = null;

        if (term instanceof MultiTermsBucket) {
            additionalKey = ((MultiTermsBucket) term).key().get(0);
            key = ((MultiTermsBucket) term).key().get(1);
        } else if (term instanceof StringTermsBucket) {
            key = ((StringTermsBucket) term).key();
            if (ACROSS_USERS.contains(filter.getAcross())) {
                additionalKey = userIdMap.getOrDefault(key, "NONE");
            }
        } else if (term instanceof DateHistogramBucket) {
            long interval = (((DateHistogramBucket) term).key().toEpochMilli());
            key = Long.toString(TimeUnit.MILLISECONDS.toSeconds(interval));
            additionalKey = ((DateHistogramBucket) term).keyAsString();
            additionalKey = getCalenderInterval(filter.getAggInterval().name(), additionalKey, interval);
        }

        long count = term.docCount();

        if (valuesOnly) {
            return DbAggregationResult.builder()
                    .key(key)
                    .additionalKey(additionalKey)
                    .count(count)
                    .build();
        }

        long tot_files_ct = (long) term.aggregations().get("files_ct").sum().value();
        long addition_ct = (long) term.aggregations().get("commit_addition_ct").sum().value();
        long deletion_ct = (long) term.aggregations().get("commit_deletion_ct").sum().value();
        long changes_ct = (long) term.aggregations().get("commit_changes_ct").sum().value();
        String pctValue = term.aggregations().get("pct").tdigestPercentiles().values().keyed().get("50.0");
        double medianChange = pctValue == null ? 0d : Double.valueOf(pctValue);

        float avgChangeSize = (float) tot_files_ct / count;

        if ("lines".equals(filter.getCodeChangeUnit())) {
            avgChangeSize = ((float)addition_ct+deletion_ct+changes_ct)/count;
        }

        return DbAggregationResult.builder()
                .key(key)
                .additionalKey(additionalKey)
                .count(count)
                .filesChangedCount(tot_files_ct)
                .linesAddedCount(addition_ct)
                .linesRemovedCount(deletion_ct)
                .linesChangedCount(changes_ct)
                .medianChangeSize((long) medianChange)
                .avgChangeSize(Float.valueOf(String.format("%.3f", avgChangeSize)))
                .build();
    }

    public static List<DbScmFile> esScmFileMapper(SearchResponse<Void> searchResponse) {

        List<DbScmFile> list = new ArrayList<>();
        List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_files").nested().aggregations().get("across_file_list").sterms().buckets().array();

        termsBuckets.forEach(term -> {

            String [] temp = term.key().split("#");
            String key = temp[0];
            String fileName =  temp[1];
            String integrationId = temp[2];
            String repo =  temp[3];
            String project =  temp[4];
            long createdAt = Long.valueOf( temp[5]);

            createdAt = TimeUnit.MILLISECONDS.toSeconds(createdAt);
            long count = term.docCount();

            long additions = (long) term.aggregations().get("additions").sum().value();
            long deletions = (long) term.aggregations().get("deletions").sum().value();
            long change = (long) term.aggregations().get("changes").sum().value();

            DbScmFile res = DbScmFile.builder()
                    .id(key)
                    .integrationId(integrationId)
                    .numCommits(count)
                    .repoId(repo)
                    .project(project)
                    .filename(fileName)
                    .totalAdditions(additions)
                    .totalChanges(change)
                    .totalDeletions(deletions)
                    .createdAt(createdAt)
                    .build();
            list.add(res);
        });
        return list;
    }

    public static List<DbAggregationResult> esScmModuleMapper(SearchResponse<Void> searchResponse, ScmFilesFilter filter) {


        List<DbAggregationResult> list = new ArrayList<>();
        List<StringTermsBucket> termsBuckets;

        if (StringUtils.isNotEmpty(filter.getModule())) {
            termsBuckets = searchResponse.aggregations().get("across_files").nested().aggregations().get("wildcard_filter").filter().aggregations().get("across_module").sterms().buckets().array();
        } else {
            termsBuckets = searchResponse.aggregations().get("across_files").nested().aggregations().get("across_module").sterms().buckets().array();
        }

        termsBuckets.forEach(term -> {

            String aggKey = term.key();
            String module = aggKey.substring(0, aggKey.indexOf("#"));
            String repo = aggKey.substring(aggKey.indexOf("#") + 1, aggKey.lastIndexOf("#"));
            String project = aggKey.substring(aggKey.lastIndexOf("#") + 1);

            DbAggregationResult res = DbAggregationResult.builder()
                    .key(module)
                    .count(term.docCount())
                    .repoId(repo)
                    .project(project)
                    .build();

            list.add(res);
        });
        return list;
    }

    public static List<DbScmContributorAgg> esScmContributorMapper(SearchResponse<Void> searchResponse, String across) {

        List<StringTermsBucket> termsBuckets = searchResponse.aggregations().get("across_"+across).sterms().buckets().array();

        List<DbScmContributorAgg> list = new ArrayList<>();
        termsBuckets.forEach( term -> {

            String temp = term.key();
            String key = "";
            String additionalKey = "";

            if (StringUtils.isNotEmpty(temp) && temp.contains("#")){
                key = temp.substring(0, temp.indexOf("#"));
                additionalKey = temp.substring(temp.indexOf("#") + 1);
            }

            int commitCount = (int) term.aggregations().get("num_commits").cardinality().value();
            int repoCount = (int) term.aggregations().get("repo_count").cardinality().value();
            int prCount = (int) term.aggregations().get("num_prs").sum().value();
            List<StringTermsBucket> fileTypeBuckets = term.aggregations().get("file_types").sterms().buckets().array();
            List<String> fileTypes = fileTypeBuckets.stream().map(b -> b.key()).collect(Collectors.toList());
            List<StringTermsBucket> reposBuckets = term.aggregations().get("repos").sterms().buckets().array();
            List<String> repos = reposBuckets.stream().map(b -> b.key()).collect(Collectors.toList());
            int additions = (int) term.aggregations().get("additions").sum().value();
            int deletions = (int) term.aggregations().get("deletions").sum().value();
            int changes = (int) term.aggregations().get("changes").sum().value();
            int workItemCount = (int) term.aggregations().get("num_workitems").valueCount().value();
            int jiraCount = (int) term.aggregations().get("num_jiraissues").valueCount().value();
            List<String> technologies = getFileValues(fileTypes);

            DbScmContributorAgg res = DbScmContributorAgg.builder()
                    .id(key)
                    .name(additionalKey)
                    .numCommits(commitCount)
                    .numRepos(repoCount)
                    .numPrs(prCount)
                    .fileTypes(fileTypes)
                    .repoBreadth(repos)
                    .numAdditions(additions)
                    .numDeletions(deletions)
                    .numChanges(changes)
                    .numWorkitems(workItemCount)
                    .numJiraIssues(jiraCount)
                    .techBreadth(technologies)
                    .build();

            list.add(res);

        });

        return list;
    }

    public static List<DbScmRepoAgg> esScmFileTypeMapper(SearchResponse<Void> searchResponse) {

        List<StringTermsBucket> termBuckets = searchResponse.aggregations().get("file_type_agg").sterms().buckets().array();
        List<DbScmRepoAgg> list = new ArrayList<>();

        termBuckets.forEach(term -> {

            String fileType = term.key();

            int commitCount = (int) term.aggregations().get("num_commits").cardinality().value();
            int workItemCount = (int) term.aggregations().get("num_workitems").valueCount().value();

            int additions = (int) term.aggregations().get("additions").sum().value();
            int deletions = (int) term.aggregations().get("deletions").sum().value();
            int changes = (int) term.aggregations().get("changes").sum().value();
            int prCount = (int) term.aggregations().get("num_prs").sum().value();

            DbScmRepoAgg res = DbScmRepoAgg.builder()
                    .name(fileType)
                    .numCommits(commitCount)
                    .numAdditions(additions)
                    .numDeletions(deletions)
                    .numChanges(changes)
                    .numWorkitems(workItemCount)
                    .numPrs(prCount)
                    .build();

            list.add(res);
        });

        return list;
    }

    private static List<String> getFileValues(List<String> files){
        List<String> result = new ArrayList<>();
        if (files == null) {
            return result;
        }
        for (String file : files) {
            result.add(LANGUAGE_FILE_MAP.get(file));
        }
        return result.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }
}
