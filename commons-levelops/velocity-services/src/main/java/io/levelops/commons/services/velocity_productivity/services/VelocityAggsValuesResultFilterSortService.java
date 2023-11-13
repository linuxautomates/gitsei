package io.levelops.commons.services.velocity_productivity.services;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.velocity.VelocityAggsQueryBuilder.TOTAL_LEAD_TIME_COLUMN_KEYS;

@Log4j2
@Service
public class VelocityAggsValuesResultFilterSortService {
    private static final boolean SORT_ASC = true;
    private static final boolean SORT_DESC = false;

    /*
    private static final Comparator<SortingObj> COMPARATOR_FOR_SORTING_OBJ_ASC = Comparator
            .nullsLast(Comparator.comparing(SortingObj::getValueForSorting))
            .thenComparing((s1, s2) -> s1.getResult().getKey().compareTo(s2.getResult().getKey()));
    private static final Comparator<SortingObj> COMPARATOR_FOR_SORTING_OBJ_DESC = Comparator
            .nullsLast(Comparator.comparing(SortingObj::getValueForSorting).reversed())
            .thenComparing((s1, s2) -> s1.getResult().getKey().compareTo(s2.getResult().getKey()));
     */

    private static final Comparator<SortingObj> COMPARATOR_FOR_SORTING_OBJ_ASC = Comparator
            .comparing(SortingObj::getValueForSorting, Comparator.nullsLast(Long::compareTo))
            .thenComparing((s1, s2) -> s1.getResult().getKey().compareTo(s2.getResult().getKey()));
    private static final Comparator<SortingObj> COMPARATOR_FOR_SORTING_OBJ_DESC = Comparator
            .comparing(SortingObj::getValueForSorting, Comparator.nullsFirst(Long::compareTo).reversed())
            .thenComparing((s1, s2) -> s2.getResult().getKey().compareTo(s1.getResult().getKey()));

    @Value
    @Builder(toBuilder = true)
    public static class SortingObj {
        private Long valueForSorting;
        private final DbAggregationResult result;
    }

    //region Common for Total & Single Stage
    private Long determineValueForSorting(DbAggregationResult dbAggregationResult, Map<String, SortingOrder> sort) {
        if(MapUtils.isEmpty(sort)) {
            //if no sorting column specified - sort by total
            return dbAggregationResult.getTotal();
        }
        Map.Entry<String, SortingOrder> entry = sort.entrySet().iterator().next();
        if(TOTAL_LEAD_TIME_COLUMN_KEYS.contains(entry.getKey())) {
            return dbAggregationResult.getTotal();
        }
        for(DbAggregationResult stageResult : CollectionUtils.emptyIfNull(dbAggregationResult.getData())) {
            if(StringUtils.equals(stageResult.getKey(), entry.getKey())) {
                return stageResult.getMedian();
            }
        }
        return null;
    }
    private boolean sortOrderIsAscending(Map<String, SortingOrder> sort) {
        if(MapUtils.isEmpty(sort)) {
            //if no sorting column specified - sort by total DESC
            return SORT_DESC;
        }
        Map.Entry<String, SortingOrder> entry = sort.entrySet().iterator().next();
        return (entry.getValue() == SortingOrder.ASC) ? SORT_ASC : SORT_DESC;
    }
    private SortingObj mapVelocityResultToSortingObj(DbAggregationResult velocityValue, Map<String, SortingOrder> sort) {
        return SortingObj.builder()
                .result(velocityValue)
                .valueForSorting(determineValueForSorting(velocityValue, sort))
                .build();
    }
    //endregion

    //region Total Specific
    private boolean filterVelocityValuesTotal(DbAggregationResult velocityValue, Set<VelocityConfigDTO.Rating> filterRatings) {
        if(filterRatings.contains(VelocityConfigDTO.Rating.MISSING)) {
            //we create two db results 1) for missing & 2) for all non missing
            //if filterRatings has missing, we use all values & do not filter out anything
            return true;
        }
        //if filterRatings is for non missing, since this is for total we check all stage ratings & see if any stage matches the filtered stage.
        for(DbAggregationResult stageResult: CollectionUtils.emptyIfNull(velocityValue.getData())) {
            if(stageResult.getVelocityStageResult() == null) {
                continue;
            }
            if(stageResult.getVelocityStageResult().getRating() == null) {
                continue;
            }
            if(filterRatings.contains(stageResult.getVelocityStageResult().getRating())) {
                return true;
            }
        }
        return false;
    }
    private DbListResponse<DbAggregationResult> filterAndSortVelocityValuesTotal(List<DbAggregationResult> velocityValues, VelocityFilter velocityFilter) {
        int page = velocityFilter.getPage();
        int pageSize = velocityFilter.getPageSize();

        Set<VelocityConfigDTO.Rating> filterRatings = CollectionUtils.emptyIfNull(velocityFilter.getRatings()).stream().collect(Collectors.toSet());
        Map<String, SortingOrder> sort = velocityFilter.getSort();

        List<SortingObj> fullResults = CollectionUtils.emptyIfNull(velocityValues).stream()
                .filter(v -> filterVelocityValuesTotal(v, filterRatings))
                .map(v -> mapVelocityResultToSortingObj(v, sort))
                .sorted((sortOrderIsAscending(sort) ? COMPARATOR_FOR_SORTING_OBJ_ASC : COMPARATOR_FOR_SORTING_OBJ_DESC))
                .collect(Collectors.toList());

        List<DbAggregationResult> paginatedResults = CollectionUtils.emptyIfNull(fullResults).stream()
                .skip(page * pageSize)
                .limit(pageSize)
                .map(so -> so.getResult())
                .collect(Collectors.toList());
        return DbListResponse.of(paginatedResults, CollectionUtils.size(fullResults));
    }
    //endregion

    //region Single Stage Specific
    private DbAggregationResult parseSelectedStageResult(DbAggregationResult velocityValue, String selectedStageName) {
        for(DbAggregationResult stageResult: CollectionUtils.emptyIfNull(velocityValue.getData())) {
            if(selectedStageName.equals(stageResult.getKey())) {
                return stageResult;
            }
        }
        return null;
    }
    private boolean filterVelocityValuesSingleStage(DbAggregationResult velocityValue, Set<VelocityConfigDTO.Rating> filterRatings, String selectedStageName) {
        DbAggregationResult stageResult = parseSelectedStageResult(velocityValue, selectedStageName);
        if(stageResult == null) {
            //Ideally we should never be hitting this case
            log.error("Velocity Value key {}, selectedStageName {}, filterRatings {}, stage result NOT found!!", velocityValue.getKey(), selectedStageName, filterRatings);
            return false;
        }
        if(stageResult.getVelocityStageResult() == null) {
            //Ideally we should never be hitting this case
            log.error("Velocity Value key {}, selectedStageName {}, filterRatings {}, stageResult.getVelocityStageResult is null!!", velocityValue.getKey(), selectedStageName, filterRatings);
            return false;
        }
        if(stageResult.getVelocityStageResult().getRating() == null) {
            //Ideally we should never be hitting this case
            log.error("Velocity Value key {}, selectedStageName {}, filterRatings {}, stageResult.getVelocityStageResult().getRating() is null!!", velocityValue.getKey(), selectedStageName, filterRatings);
            return false;
        }
        return filterRatings.contains(stageResult.getVelocityStageResult().getRating());
    }
    private DbListResponse<DbAggregationResult> filterAndSortVelocityValuesSingleStage(List<DbAggregationResult> velocityValues, VelocityFilter velocityFilter, String selectedStageName) {
        int page = velocityFilter.getPage();
        int pageSize = velocityFilter.getPageSize();

        Set<VelocityConfigDTO.Rating> filterRatings = CollectionUtils.emptyIfNull(velocityFilter.getRatings()).stream().collect(Collectors.toSet());
        Map<String, SortingOrder> sort = velocityFilter.getSort();

        List<SortingObj> fullResults = CollectionUtils.emptyIfNull(velocityValues).stream()
                .filter(v -> filterVelocityValuesSingleStage(v, filterRatings, selectedStageName))
                .map(v -> mapVelocityResultToSortingObj(v, sort))
                .sorted((sortOrderIsAscending(sort) ? COMPARATOR_FOR_SORTING_OBJ_ASC : COMPARATOR_FOR_SORTING_OBJ_DESC))
                .collect(Collectors.toList());

        List<DbAggregationResult> paginatedResults = CollectionUtils.emptyIfNull(fullResults).stream()
                .skip(page * pageSize)
                .limit(pageSize)
                .map(so -> so.getResult())
                .collect(Collectors.toList());
        return DbListResponse.of(paginatedResults, CollectionUtils.size(fullResults));
    }
    //endregion

    public DbListResponse<DbAggregationResult> filterAndSortVelocityValues(List<DbAggregationResult> velocityValues, DefaultListRequest defaultListRequest) {
        VelocityFilter velocityFilter = VelocityFilter.fromListRequest(defaultListRequest);

        if(StringUtils.isNotBlank(velocityFilter.getHistogramStageName())) {
            return filterAndSortVelocityValuesSingleStage(velocityValues, velocityFilter, velocityFilter.getHistogramStageName());
        } else {
            return filterAndSortVelocityValuesTotal(velocityValues, velocityFilter);
        }
    }
}
