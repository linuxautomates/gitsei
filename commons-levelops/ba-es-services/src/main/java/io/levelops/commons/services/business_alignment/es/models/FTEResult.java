package io.levelops.commons.services.business_alignment.es.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.services.business_alignment.models.JiraAcross;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = FTEResult.FTEResultBuilder.class)
public class FTEResult {
    @JsonProperty("ticket_category")
    private String ticketCategory;
    @JsonProperty("assignee_id")
    private String assigneeId;
    @JsonProperty("assignee_name")
    private String assigneeName;
    @JsonProperty("interval")
    private Long interval;
    @JsonProperty("interval_as_string")
    private String intervalAsString;
    @JsonProperty("effort")
    private Long effort;
    @JsonProperty("total_effort")
    private Long totalEffort;
    @JsonProperty("fte")
    private Double fte;

    //region Merge
    public static List<FTEResult> merge (FTEResultMergeRequest mergeRequest) {
        List<FTEResult> fteResults = new ArrayList<>();
        Map<String, Map<Long, FTEResultBuilder>> map = new HashMap<>();
        for(FTEPartial p : CollectionUtils.emptyIfNull(mergeRequest.getForCategory())) {
            if (StringUtils.isEmpty(p.getAssigneeId()) || "null".equals(p.getAssigneeId())) {
                continue;
            }
            map.computeIfAbsent(p.getAssigneeId(), k -> new HashMap<>())
                    .put(p.getInterval(), FTEResult.builder()
                            .ticketCategory(p.getTicketCategory())
                            .assigneeId(p.getAssigneeId())
                            .assigneeName(p.getAssigneeName())
                            .interval(p.getInterval())
                            .intervalAsString(p.getIntervalAsString())
                            .effort(p.getEffortOrTotalEffort()));
        }
        for(FTEPartial p : CollectionUtils.emptyIfNull(mergeRequest.getAllTickets())) {
            if (StringUtils.isEmpty(p.getAssigneeId()) || "null".equals(p.getAssigneeId())) {
                continue;
            }
            if (!map.containsKey(p.getAssigneeId())) {
                fteResults.add(FTEResult.builder()
                        .ticketCategory(mergeRequest.getTicketCategory())
                        .assigneeId(p.getAssigneeId())
                        .assigneeName(p.getAssigneeName())
                        .interval(p.getInterval())
                        .intervalAsString(p.getIntervalAsString())
                        .totalEffort(p.getEffortOrTotalEffort())
                        .effort(0l)
                        .fte(calculateFTE(0l, p.getEffortOrTotalEffort()))
                        .build());
                continue;
            }
            if (!map.get(p.getAssigneeId()).containsKey(p.getInterval())) {
                fteResults.add(FTEResult.builder()
                        .ticketCategory(mergeRequest.getTicketCategory())
                        .assigneeId(p.getAssigneeId())
                        .assigneeName(p.getAssigneeName())
                        .interval(p.getInterval())
                        .intervalAsString(p.getIntervalAsString())
                        .totalEffort(p.getEffortOrTotalEffort())
                        .effort(0l)
                        .fte(calculateFTE(0l, p.getEffortOrTotalEffort()))
                        .build());
                continue;
            }
            FTEResult.FTEResultBuilder bldr = map.get(p.getAssigneeId()).get(p.getInterval());
            map.get(p.getAssigneeId()).put(p.getInterval(), bldr.totalEffort(p.getEffortOrTotalEffort()));
        }

        for(Map.Entry<String, Map<Long, FTEResultBuilder>> e : map.entrySet()) {
            for(Map.Entry<Long, FTEResultBuilder> f : e.getValue().entrySet()) {
                FTEResultBuilder bldr = f.getValue();
                Double fte = calculateFTE(bldr.effort, bldr.totalEffort);
                fteResults.add(bldr.fte(fte).build());
            }
        }
        Collections.sort(fteResults, (a,b) -> {
            int r = a.getInterval().compareTo(b.getInterval());
            if(r != 0) {
                return r;
            }
            r = a.getAssigneeId().compareTo(b.getAssigneeId());
            if(r != 0) {
                return r;
            }
            r = a.getFte().compareTo(b.getFte());
            return r;

        });
        return fteResults;
    }
    //endregion

    //region Calculate FTE
    public static Double calculateFTE(Long effort, Long totalEffort) {
        Double fte = (totalEffort == null || totalEffort == 0l) ? 0.0 : (double)ObjectUtils.firstNonNull(effort, 0l) / (double)totalEffort;
        return fte;
    }
    //endregion

    //region Merge Weekly Data Into BiWeekly
    private static FTEResult mergeConsecutiveIntervals(FTEResult first, FTEResult second) {
        Long effort = first.getEffort() + second.getEffort();
        Long totalEffort = ObjectUtils.firstNonNull(first.getTotalEffort(), 0l) + ObjectUtils.firstNonNull(second.getTotalEffort(), 0l);

        return FTEResult.builder()
                .ticketCategory(first.getTicketCategory())
                .assigneeId(first.getAssigneeId())
                .assigneeName(first.getAssigneeName())
                .interval(first.getInterval())
                .intervalAsString(first.getIntervalAsString())
                .effort(effort)
                .totalEffort(totalEffort)
                .fte(calculateFTE(effort, totalEffort))
                .build();
    }

    private static List<FTEResult> mergeDataForOneAssigneeAndCategory(List<FTEResult> weekly, Map<Long, Long> intervalToBiWeekStart, Map<Long, String> intervalToIntervalStringMap) {
        //If empty, no merge needed
        if (CollectionUtils.isEmpty(weekly)) {
            return weekly;
        }
        //Sort weekly data by
        Collections.sort(weekly, Comparator.comparingLong(FTEResult::getInterval));
        List<FTEResult> mergedBiWeekly = new ArrayList<>();

        Queue<FTEResult> queue = new LinkedList<>(weekly);
        while (!queue.isEmpty()) {
            FTEResult first = queue.remove();
            FTEResult second = (queue.isEmpty()) ? null : queue.peek();

            if( (second != null) && (intervalToBiWeekStart.get(first.getInterval()) == intervalToBiWeekStart.get(second.getInterval()))) {
                //First and Second are in same bi-week
                //DeQueue Second
                second = queue.remove();
                FTEResult merged = mergeConsecutiveIntervals(first, second);
                mergedBiWeekly.add(merged);
            } else {
                //First and Second are not in same bi-week
                //Before saving first make sure it allign to start of bi-week
                Long firstBiWeekStart = intervalToBiWeekStart.get(first.getInterval());
                mergedBiWeekly.add(first.toBuilder()
                        .interval(firstBiWeekStart)
                        .intervalAsString(intervalToIntervalStringMap.get(firstBiWeekStart))
                        .build());
            }
        }
        return mergedBiWeekly;
    }

    private static final Long SECONDS_IN_ONE_WEEK = TimeUnit.DAYS.toSeconds(7l);

    public static List<FTEResult> mergeWeeklyDataIntoBiWeekly(List<FTEResult> weekly){
        //If there are less than two objects, no merge needed
        if (CollectionUtils.size(weekly) < 2) {
            return weekly;
        }

        //Populate Interval (Long) -> Interval String Format mapping
        Map<Long, String> intervalToIntervalStringMap = weekly.stream().collect(Collectors.toMap(w-> w.getInterval(), w-> w.getIntervalAsString(), (first, second) -> first));

        Long minInterval = weekly.stream().mapToLong(w -> w.getInterval()).min().orElseThrow(NoSuchElementException::new);
        Long maxInterval = weekly.stream().mapToLong(w -> w.getInterval()).max().orElseThrow(NoSuchElementException::new);
        Map<Long, Long> intervalToBiWeekStart = new HashMap<>();
        int i=0;
        Long currentInterval = minInterval;
        Long intervalStart = null;
        while (currentInterval <= maxInterval) {
            if(i%2 == 0) {
                intervalStart = currentInterval;
            }
            intervalToBiWeekStart.put(currentInterval, intervalStart);
            intervalToIntervalStringMap.computeIfAbsent(currentInterval, DateUtils::getWeeklyFormat); //If data set does not have data for week start, we will not have String format, adding it manually
            i++;
            currentInterval += SECONDS_IN_ONE_WEEK;
        }

        //Used for O(1) search
        //Map<Long, FTEPartial> map = weekly.stream().collect(Collectors.toMap(FTEPartial::getInterval, data -> data));
        Map<String, Map<String, Map<Optional<String>, List<FTEResult>>>> map = weekly.stream()
                .collect(Collectors.groupingBy(FTEResult::getAssigneeId,
                                Collectors.groupingBy(FTEResult::getAssigneeName,
                                        Collectors.groupingBy(s -> Optional.ofNullable(s.getTicketCategory()),
                                                Collectors.toList())
                                )
                        )
                );

        List<FTEResult> mergedBiWeekly = new ArrayList<>();
        for(Map.Entry<String, Map<String, Map<Optional<String>, List<FTEResult>>>> e1 : map.entrySet()) {
            for(Map.Entry<String, Map<Optional<String>, List<FTEResult>>> e2 : e1.getValue().entrySet()) {
                for(Map.Entry<Optional<String>, List<FTEResult>> e3 : e2.getValue().entrySet()) {
                    mergedBiWeekly.addAll(mergeDataForOneAssigneeAndCategory(e3.getValue(), intervalToBiWeekStart, intervalToIntervalStringMap));
                }
            }
        }
        return mergedBiWeekly;
    }
    //endregion

    //region Group FTEResult By Across
    public static List<DbAggregationResult> groupFTEResultByAcross(JiraAcross across, List<FTEResult> allFTEResultWithAggInterval) {
        List<FTEResult> finalFTEResults = null;
        List<DbAggregationResult> dbAggregationResults = null;
        //https://stackoverflow.com/questions/26340688/group-by-and-sum-objects-like-in-sql-with-java-lambdas
        //https://stackoverflow.com/questions/26340688/group-by-and-sum-objects-like-in-sql-with-java-lambdas/26341146#26341146

        if (across == JiraAcross.ASSIGNEE) {
            finalFTEResults = allFTEResultWithAggInterval.stream()
                    .collect(
                            groupingBy(f -> f.getAssigneeId(),
                                    reducing((a,b)-> {
                                        Long effort = a.getEffort() + b.getEffort();
                                        Long totalEffort = a.getTotalEffort() + b.getTotalEffort();
                                        Double fte = FTEResult.calculateFTE(effort, totalEffort);
                                        return FTEResult.builder()
                                                .assigneeId(a.getAssigneeId()).assigneeName(a.getAssigneeName())
                                                .effort(effort).totalEffort(totalEffort).fte(fte)
                                                .build();
                                    })))
                    .values()
                    .stream()
                    .filter(result -> result.get() != null)
                    .map(result -> result.get())
                    .collect(Collectors.toList());
            dbAggregationResults = CollectionUtils.emptyIfNull(finalFTEResults).stream()
                    .sorted(Comparator.comparingDouble(FTEResult::getFte).reversed())
                    .map(f -> DbAggregationResult.builder()
                            .key(f.getAssigneeName())
                            .additionalKey(f.getAssigneeId())
                            .effort(f.getEffort()).total(f.getTotalEffort()).fte(f.getFte().floatValue())
                            .build())
                    .collect(Collectors.toList());
        } else if (across == JiraAcross.ISSUE_RESOLVED_AT) {
            finalFTEResults = allFTEResultWithAggInterval.stream()
                    .collect(
                            groupingBy(f -> f.getIntervalAsString(),
                                    reducing((a,b)-> {
                                        Long effort = a.getEffort() + b.getEffort();
                                        Long totalEffort = a.getTotalEffort() + b.getTotalEffort();
                                        Double fte = FTEResult.calculateFTE(effort, totalEffort);
                                        return FTEResult.builder()
                                                .interval(a.getInterval())
                                                .intervalAsString(a.getIntervalAsString())
                                                .effort(effort).totalEffort(totalEffort).fte(fte)
                                                .build();
                                    })))
                    .values()
                    .stream()
                    .filter(result -> result.get() != null)
                    .map(result -> result.get())
                    .collect(Collectors.toList());
            dbAggregationResults = CollectionUtils.emptyIfNull(finalFTEResults).stream()
                    .sorted(Comparator.comparingDouble(FTEResult::getInterval))
                    .map(f -> DbAggregationResult.builder()
                            .key(String.valueOf(f.getInterval()))
                            .additionalKey(f.getIntervalAsString())
                            .effort(f.getEffort()).total(f.getTotalEffort()).fte(f.getFte().floatValue())
                            .build())
                    .collect(Collectors.toList());
        } else if (across == JiraAcross.TICKET_CATEGORY) {
            finalFTEResults = allFTEResultWithAggInterval.stream()
                    .collect(
                            groupingBy(f -> f.getTicketCategory(),
                                    reducing((a,b)-> {
                                        Long effort = a.getEffort() + b.getEffort();
                                        Long totalEffort = a.getTotalEffort() + b.getTotalEffort();
                                        Double fte = FTEResult.calculateFTE(effort, totalEffort);
                                        return FTEResult.builder()
                                                .ticketCategory(a.getTicketCategory())
                                                .effort(effort).totalEffort(totalEffort).fte(fte)
                                                .build();
                                    })))
                    .values()
                    .stream()
                    .filter(result -> result.get() != null)
                    .map(result -> result.get())
                    .collect(Collectors.toList());
            dbAggregationResults = CollectionUtils.emptyIfNull(finalFTEResults).stream()
                    .sorted(Comparator.comparingDouble(FTEResult::getFte).reversed())
                    .map(f -> DbAggregationResult.builder()
                            .key(f.getTicketCategory())
                            .additionalKey(null)
                            .effort(f.getEffort()).total(f.getTotalEffort()).fte(f.getFte().floatValue())
                            .build())
                    .collect(Collectors.toList());

        } else {
            throw new RuntimeException("Across " + across.toString() + " is not supported!");
        }
        return dbAggregationResults;
    }
    //endregion

}
