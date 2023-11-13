package io.levelops.commons.utils.dora;

import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.services.ScmConditionBuilder;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.jdbc.core.RowMapper;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DoraCalculationUtils {

    public static RowMapper<DoraTimeSeriesDTO.TimeSeriesData> getTimeSeries(){
        return (rs, rowNumber) -> DoraTimeSeriesDTO.TimeSeriesData.builder()
                .key(rs.getLong("trend"))
                .count(rs.getInt("ct")).build();

    }

    public static RowMapper<JiraReleaseResponse> getDFForJiraRelease() {
        return (rs, rowNumber) -> JiraReleaseResponse.builder()
                .name(rs.getString("fix_version"))
                .project(rs.getString("project"))
                .releaseEndTime(rs.getLong("released_date"))
                .issueCount(rs.getInt("count"))
                .build();
    }

    public static RowMapper<DoraTimeSeriesDTO.TimeSeriesData> getTimeSeriesForStacks(String stackField) {
        return (rs, rowNumber) -> DoraTimeSeriesDTO.TimeSeriesData.builder()
                .key(rs.getLong("trend"))
                .count(0)
                .stacks((stackField != null ) ? getStacksValues((String[]) rs.getArray(stackField).getArray()) : null).build();
    }

    public static List<Map<String, Object>> getStacksValues(String[] stackData) {
        List<Map<String, Object>> stackList = new ArrayList<>();
        for (String element: stackData) {
            String fieldKey = element.split(":")[0];
            Integer fieldCount = Integer.parseInt(element.split(":")[1]);
            stackList.add(Map.of("key", fieldKey, "count", fieldCount));
        }
        return stackList;
    }

    public static List<DoraTimeSeriesDTO.TimeSeriesData> fillRemainingDates(Long startTime, Long endTime, List<DoraTimeSeriesDTO.TimeSeriesData> tempTimeSeriesResult) {

        LocalDate startDate = Instant.ofEpochSecond(startTime).atZone(ZoneId.of("UTC")).toLocalDate();
        LocalDate endDate = Instant.ofEpochSecond(endTime).atZone(ZoneId.of("UTC")).toLocalDate();
        List<LocalDate> allDates= startDate.datesUntil(endDate).collect(Collectors.toList());
        List<Long> epochList = allDates.stream().map(m->m.atStartOfDay(ZoneId.of("UTC")).toEpochSecond()).collect(Collectors.toList());
        if(!epochList.contains(endDate.toEpochDay()))
            epochList.add(endDate.atStartOfDay(ZoneId.of("UTC")).toEpochSecond());

        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeries = new ArrayList<>();
        epochList.forEach(key -> fillMissingDate(tempTimeSeriesResult, key, filledTimeSeries));
        return filledTimeSeries;

    }

    private static void fillMissingDate(List<DoraTimeSeriesDTO.TimeSeriesData> tempTimeSeriesResult, Long key, List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeries) {

        List<DoraTimeSeriesDTO.TimeSeriesData> matched = tempTimeSeriesResult.stream()
                .filter(timeSeries -> timeSeries.getKey().longValue() == key.longValue()).collect(Collectors.toList());

        DoraTimeSeriesDTO.TimeSeriesData.TimeSeriesDataBuilder timeSeriesDataBuilder =  DoraTimeSeriesDTO.TimeSeriesData.builder();

        if(matched.isEmpty()) {
            timeSeriesDataBuilder.key(key);
            timeSeriesDataBuilder.count(0);
            timeSeriesDataBuilder.additionalKey((Instant.ofEpochSecond(key).atZone(ZoneId.of("UTC")).toLocalDate()).toString());
        }
        else  {
            timeSeriesDataBuilder.key(matched.get(0).getKey());
            timeSeriesDataBuilder.count(matched.get(0).getCount());
            timeSeriesDataBuilder.additionalKey(matched.get(0).getAdditionalKey());
            timeSeriesDataBuilder.stacks(matched.get(0).getStacks());
        }
        filledTimeSeries.add(timeSeriesDataBuilder.build());
    }

    public static List<DoraTimeSeriesDTO.TimeSeriesData> convertTimeSeries(String by, List<DoraTimeSeriesDTO.TimeSeriesData> response) {
        Map<String, TemporalAdjuster> ADJUSTERS = new HashMap<>();
        ADJUSTERS.put("day", TemporalAdjusters.ofDateAdjuster(d -> d)); // identity
        ADJUSTERS.put("week", TemporalAdjusters.previousOrSame(DayOfWeek.of(1)));
        ADJUSTERS.put("month", TemporalAdjusters.firstDayOfMonth());

        Map<LocalDate, List<DoraTimeSeriesDTO.TimeSeriesData>> result = CollectionUtils.emptyIfNull(response).stream()
                .collect(Collectors.groupingBy(res -> Instant.ofEpochSecond((res.getKey())).atZone(ZoneId.of("UTC")).toLocalDate()
                        .with(ADJUSTERS.get(by))));

        List<DoraTimeSeriesDTO.TimeSeriesData> output = new ArrayList<>();
        result.entrySet().forEach(entry-> prepareOutputList(entry, output));

        List<DoraTimeSeriesDTO.TimeSeriesData> sortedResponse = CollectionUtils.emptyIfNull(output).stream().sorted((t1, t2) -> t1.getKey().compareTo(t2.getKey()))
                .collect(Collectors.toList());
        return sortedResponse;
    }

    private static void prepareOutputList(Map.Entry<LocalDate, List<DoraTimeSeriesDTO.TimeSeriesData>> entry, List<DoraTimeSeriesDTO.TimeSeriesData> output) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DoraTimeSeriesDTO.TimeSeriesData ts =  DoraTimeSeriesDTO.TimeSeriesData.builder().key(entry.getKey().atStartOfDay(ZoneId.of("UTC")).toEpochSecond())
                .count(entry.getValue().stream().map(m -> m.getCount()).reduce(0,(a, b) -> a+b))
                .additionalKey(entry.getKey().format(formatter))
                .stacks(getAccumulatedStacks(entry.getValue()))
                .build();
        output.add(ts);
    }

    private static List<Map<String, Object>> getAccumulatedStacks(List<DoraTimeSeriesDTO.TimeSeriesData> entry) {
        Map<String, Object> stackValuesWithCount = new HashMap<>();
        List<Map<String, Object>> finalStacks = new ArrayList<>();
        for (DoraTimeSeriesDTO.TimeSeriesData record: entry) {
            if (record.getStacks() != null)
                record.getStacks().forEach(map -> stackValuesWithCount.put(map.get("key").toString(), (Integer)map.get("count") + (Integer)stackValuesWithCount.getOrDefault(map.get("key"), 0)));
        }

        if (stackValuesWithCount.isEmpty())
            return null;

        for (String key: stackValuesWithCount.keySet()) {
            finalStacks.add(Map.of("key", key, "count", stackValuesWithCount.get(key)));
        }

        return  finalStacks;
    }
    public static DoraSingleStateDTO.Band calculateDeploymentFrequencyBand(double perDay) {
        if(perDay > 1)
            return DoraSingleStateDTO.Band.ELITE;

        if (perDay <= 1 && (perDay*7) >= 1)
            return DoraSingleStateDTO.Band.HIGH;

        if((perDay*7) < 1 && (perDay*30) >= 1)
            return  DoraSingleStateDTO.Band.MEDIUM;

        return DoraSingleStateDTO.Band.LOW;
    }

    public static DoraSingleStateDTO.Band calculateChangeFailureRateBand(double failureRate) {
        if(failureRate <= 15)
            return DoraSingleStateDTO.Band.ELITE;
        if (failureRate > 15 && failureRate <= 30)
            return DoraSingleStateDTO.Band.HIGH;
        if(failureRate > 30  && failureRate <= 45)
            return  DoraSingleStateDTO.Band.MEDIUM;
        return DoraSingleStateDTO.Band.LOW;
    }

    public static Double getTimeDifference(ImmutablePair<Long, Long> range) {
        LocalDateTime begin = Instant.ofEpochSecond((range.getLeft())).atZone(ZoneId.of("UTC")).toLocalDateTime();
        LocalDateTime end = Instant.ofEpochSecond((range.getRight())).atZone(ZoneId.of("UTC")).toLocalDateTime();
        long diffINSeconds = java.time.Duration.between(begin, end).toSeconds();
        int daySeconds = 24 * 60 * 60;
        double dayDiff = (double) diffINSeconds / daySeconds;
        return Math.ceil(dayDiff);
    }

    public static boolean isTagsJoinRequired(Map<String, Map<String, List<String>>> velocityConfigDTO) {

        for (Map.Entry<String, Map<String, List<String>>> partialMatchEntry : velocityConfigDTO.entrySet()) {
            String field = partialMatchEntry.getKey();
            if ("tags".equals(field))
                return true;
        }
        return false;
    }

    public static String addPushedAtCondition(String commitTblQualifier, ScmCommitFilter commitFilter) {

        String pushedAtPrCondition = StringUtils.EMPTY;
        ImmutablePair<Long, Long> commitPushedAtRange = commitFilter.getCommitPushedAtRange();
        if (commitPushedAtRange != null) {
            if (commitPushedAtRange.getLeft() != null) {
                pushedAtPrCondition = pushedAtPrCondition.concat("  AND " + commitTblQualifier + "commit_pushed_at > TO_TIMESTAMP(" + commitPushedAtRange.getLeft() + ")");
            }
            if (commitPushedAtRange.getRight() != null) {
                pushedAtPrCondition = pushedAtPrCondition.concat("  AND " + commitTblQualifier + "commit_pushed_at < TO_TIMESTAMP(" + commitPushedAtRange.getRight() + ")");
            }
        }
        return pushedAtPrCondition;
    }

    public static Map<String, List<String>> buildRegexMapWithNewProfile(Map<String, Map<String, List<String>>> velocityConfigDTO,
                                                                  Map<String, Object> params,
                                                                  String suffix,
                                                                  String tableName) {
        List<String> doraConditions = new ArrayList<>();
        if (velocityConfigDTO.size() == 0)
            return Map.of();
        return ScmConditionBuilder.buildNewRegexPatternMap(params, velocityConfigDTO, doraConditions, tableName, suffix);
    }
}
