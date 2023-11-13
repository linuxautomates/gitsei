package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdJobRunParameters;
import static io.levelops.commons.databases.models.filters.CiCdUtils.parseCiCdQualifiedJobNames;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Value
@Builder(toBuilder = true)
public class CiCdScmFilter {
    DISTINCT across;
    List<DISTINCT> stacks;
    CALCULATION calculation;
    List<String> authors;
    List<String> cicdUserIds;
    List<String> jobNames;
    List<String> jobStatuses;
    List<String> instanceNames;
    List<String> jobNormalizedFullNames;
    List<String> integrationIds;
    List<String> cicdIntegrationIds;
    List<String> repos;
    List<CICD_TYPE> types;
    Long jobStartTime;
    Long jobEndTime;
    ImmutablePair<Long, Long> endTimeRange;
    ImmutablePair<Long, Long> startTimeRange;
    List<CiCdJobRunParameter> parameters;
    @JsonProperty("qualified_job_names")
    List<CiCdJobQualifiedName> qualifiedJobNames;
    List<String> projects;
    Map<String, Map<String, String>> partialMatch;
    Set<UUID> orgProductsIds;
    Map<String, SortingOrder> sortBy;

    List<String> excludeAuthors;
    List<String> excludeJobNames;
    List<String> excludeJobStatuses;
    List<String> excludeInstanceNames;
    List<String> excludeJobNormalizedFullNames;
    List<String> excludeRepos;
    List<String> excludeProjects;
    List<String> excludeCiCdUserIds;
    List<CICD_TYPE> excludeTypes;
    List<CiCdJobQualifiedName> excludeQualifiedJobNames;
    @JsonProperty("across_count")
    Integer acrossCount;

    CICD_AGG_INTERVAL aggInterval;

    public enum DISTINCT {
        job_status,
        author,
        job_name,
        qualified_job_name,
        job_normalized_full_name,
        cicd_user_id,
        instance_name,
        repo,
        project_name,
        //these are time based
        job_end,
        job_start,
        trend;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        lead_time,
        change_volume,
        count; // just a count of rows

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public static CiCdScmFilter.CALCULATION getSanitizedCalculation(CiCdScmFilter filter) {
        return (filter.getCalculation() != null) ? filter.getCalculation() : CiCdScmFilter.CALCULATION.change_volume;
    }

    @SuppressWarnings("unchecked")
    public static CiCdScmFilter parseCiCdScmFilter(DefaultListRequest filter, ObjectMapper objectMapper) {
        Map<String, String> endTimeRange = filter.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = filter.getFilterValue("start_time", Map.class)
                .orElse(Map.of());

        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        Map<String, Object> excludeFields =
                (Map<String, Object>) filter.getFilter().getOrDefault("exclude", Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        return CiCdScmFilter.builder()
                .across(MoreObjects.firstNonNull(
                        CiCdScmFilter.DISTINCT.fromString(
                                filter.getAcross()),
                        CiCdScmFilter.DISTINCT.trend))
                .calculation(CiCdScmFilter.CALCULATION.change_volume)
                .stacks(parseCiCdStacks(filter.getStacks(), CiCdScmFilter.DISTINCT.class))
                .authors(getListOrDefault(filter.getFilter(), "authors"))
                .cicdUserIds(getListOrDefault(filter.getFilter(), "cicd_user_ids"))
                .jobNames(getListOrDefault(filter.getFilter(), "job_names"))
                .jobNormalizedFullNames(getListOrDefault(filter.getFilter(), "job_normalized_full_names"))
                .jobStatuses(getListOrDefault(filter.getFilter(), "job_statuses"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .cicdIntegrationIds(getListOrDefault(filter.getFilter(), "cicd_integration_ids"))
                .types(CICD_TYPE.parseFromFilter(filter))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .instanceNames(getListOrDefault(filter.getFilter(), "instance_names"))
                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(filter.getFilter(), "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(filter.getFilter(), "qualified_job_names")))
                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                .orgProductsIds(getListOrDefault(filter.getFilter(), "org_product_ids").stream()
                        .map(UUID::fromString).collect(Collectors.toSet()))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(filter.getAggInterval()),
                        CICD_AGG_INTERVAL.day))
                .repos(getListOrDefault(filter.getFilter(), "repos"))
                .excludeRepos(getListOrDefault(excludeFields, "repos"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeAuthors(getListOrDefault(excludeFields, "authors"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .partialMatch(partialMatchMap)
                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())))
                .build();
    }

    public static <T extends Enum<T>> List<T> parseCiCdStacks(List<String> stackStrings, Class<T> t) {
        if (CollectionUtils.isEmpty(stackStrings)) {
            return Collections.emptyList();
        }
        List<T> results = stackStrings.stream().map(x -> EnumUtils.getEnumIgnoreCase(t, x)).collect(Collectors.toList());
        return results;
    }
}
