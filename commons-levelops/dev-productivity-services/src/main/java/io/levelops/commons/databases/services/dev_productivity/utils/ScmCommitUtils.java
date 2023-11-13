package io.levelops.commons.databases.services.dev_productivity.utils;

import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.web.exceptions.BadRequestException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

@Log4j2
public class ScmCommitUtils {

    public static Pair<Map<String, Map<String, String>>, Map<String, Map<String, String>>> checkPartialMatchConditions(Map<String, Object> profileSettings) {
        Map<String, Object> excludedFields = MapUtils.emptyIfNull((Map<String, Object>) profileSettings.get("exclude"));
        Map<String, Map<String, String>> excludePartialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) excludedFields.get("partial_match"));
        Map<String, Map<String, String>> partialMatchMap = MapUtils.emptyIfNull((Map<String, Map<String, String>>) profileSettings.get("partial_match"));
        return Pair.of(partialMatchMap, excludePartialMatchMap);
    }

    public static ScmCommitFilter.ScmCommitFilterBuilder getCommitFiltersBuilder(Map<String, Object> profileSettings) {
        ImmutablePair<Long, Long> loc = null;
        List<String> commitTitles = null;
        try {
            commitTitles = profileSettings.get("commit_title") != null ? (List<String>) profileSettings.get("commit_title") : null;
            loc = profileSettings.get("loc") != null ?
                    getRangeImmutablePair("loc", (Map<String, Object>) profileSettings.get("loc")) : ImmutablePair.nullPair();
        } catch (BadRequestException e) {
            log.error("Failed to parse loc range filter" + e);
        }
        Map<String, Object> excludedFields = MapUtils.emptyIfNull((Map<String, Object>) profileSettings.get("exclude"));
        ImmutablePair<Long, Long> excludeLoc = ImmutablePair.nullPair();
        List<String> excludeTitles = null;
        if (MapUtils.isNotEmpty(excludedFields)) {
            try {
                excludeTitles = excludedFields.get("commit_title") != null ?  (List<String>) excludedFields.get("commit_title") : null;
                excludeLoc = excludedFields.get("loc") != null ?
                        getRangeImmutablePair("loc", (Map<String, Object>) excludedFields.get("loc")) : ImmutablePair.nullPair();
            } catch (BadRequestException e) {
                log.error("Failed to parse exclude loc range filter" + e);
            }
        }
        return ScmCommitFilter.builder()
                .locRange(loc)
                .excludeLocRange(excludeLoc)
                .commitTitles(commitTitles)
                .excludeCommitTitles(excludeTitles);
    }

    @NotNull
    public static ImmutablePair<Long, Long> getRangeImmutablePair(String field, Map<String, Object> range) throws BadRequestException {
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
