package io.levelops.commons.databases.services.dev_productivity.utils;

import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Log4j2
public class ScmPRUtils {

    public static Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> checkPartialMatchConditions(Map<String, Object> profileSettings) {
        Map<String, Object> excludedFields = MapUtils.emptyIfNull((Map<String, Object>) profileSettings.get("exclude"));
        Map<String, Map<String, String>> excludePartialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) excludedFields.get("partial_match"));
        Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) profileSettings.get("partial_match"));
        return Pair.of(partialMatchMap, excludePartialMatchMap);
    }

    public static ScmPrFilter.ScmPrFilterBuilder getPrFiltersBuilder(Map<String, Object> profileSettings) {
        ImmutablePair<Long, Long> loc = null;
        try {
            loc = profileSettings.get("loc") != null ?
                    getRangeImmutablePair("loc", (Map<String, Object>) profileSettings.get("loc")) : ImmutablePair.nullPair();
        } catch (BadRequestException e) {
            log.error("Failed to parse loc range filter" + e);
        }
        List<String> commitTitles = profileSettings.get("commit_titles") != null ?
                getListOrDefault(profileSettings, "commit_titles") : List.of();
        List<String> includeTitles = profileSettings.get("titles") != null ?
                getListOrDefault(profileSettings, "titles") : List.of();
        List<String> includeLabels = profileSettings.get("labels") != null ?
                getListOrDefault(profileSettings, "labels") : List.of();
        Map<String, Object> excludedFields = MapUtils.emptyIfNull((Map<String, Object>) profileSettings.get("exclude"));
        ImmutablePair<Long, Long> excludeLoc = ImmutablePair.nullPair();
        List<String> excludeCommitTitles = List.of();
        List<String> excludeTitles = List.of();
        List<String> excludeLabels = List.of();
        List<String> excludeTargetBranches = List.of();
        if (MapUtils.isNotEmpty(excludedFields)) {
            try {
                excludeLoc = excludedFields.get("loc") != null ?
                        getRangeImmutablePair("loc", (Map<String, Object>) excludedFields.get("loc")) : ImmutablePair.nullPair();
            } catch (BadRequestException e) {
                log.error("Failed to parse exclude loc range filter" + e);
            }
            excludeCommitTitles = excludedFields.get("commit_titles") != null ?
                    getListOrDefault(excludedFields, "commit_titles") : List.of();
            excludeLabels = excludedFields.get("labels") != null ?
                    getListOrDefault(excludedFields, "labels") : List.of();
            excludeTitles = excludedFields.get("titles") != null ?
                    getListOrDefault(excludedFields, "titles") : List.of();
            excludeTargetBranches = excludedFields.get("target_branches") != null ?
                    getListOrDefault(excludedFields, "target_branches") : List.of();

        }


        return ScmPrFilter.builder()
                .titles(includeTitles)
                .labels(includeLabels)
                .locRange(loc)
                .commitTitles(commitTitles)
                .excludeTitles(excludeTitles)
                .excludeLabels(excludeLabels)
                .targetBranches(profileSettings.get("target_branches") != null ?
                        getListOrDefault(profileSettings, "target_branches") : List.of())
                .excludeTargetBranches(excludeTargetBranches)
                .excludeLocRange(excludeLoc)
                .excludeCommitTitles(excludeCommitTitles);
    }

    @NotNull
    private static ImmutablePair<Long, Long> getRangeImmutablePair(String field, Map<String, Object> range) throws BadRequestException {
        try {
            Long start = range.get("$gte") != null ? Long.parseLong(range.get("$gte").toString()) - 1 : null;
            Long end = range.get("$lte") != null ? Long.parseLong(range.get("$lte").toString()) + 1 : null;
            start = range.get("$gt") != null ? Long.valueOf(range.get("$gt").toString()) : start;
            end = range.get("$lt") != null ? Long.valueOf(range.get("$lt").toString()) : end;
            return ImmutablePair.of(start, end);
        } catch (Exception e) {
            throw new BadRequestException("Could not parse " + field + " field", e);
        }
    }
}
