package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
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

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;

@Log4j2
@Value
@Builder(toBuilder = true)
public class CiCdJobRunsFilter {
    DISTINCT across;
    Boolean isCiJob;
    Boolean isCdJob;
    List<DISTINCT> stacks;
    CALCULATION calculation;
    List<String> cicdUserIds;
    List<String> services;
    List<String> environments;
    List<String> infrastructures;
    List<String> repositories;
    List<String> branches;
    List<String> deploymentTypes;
    Boolean rollback;
    List<String> tags;
    List<String> jobNames;
    List<String> jobNormalizedFullNames;
    List<String> jobStatuses;
    List<String> instanceNames;
    List<String> integrationIds;
    List<String> triageRuleNames;
    List<UUID> triageRuleIds;
    List<CICD_TYPE> types;
    ImmutablePair<Long, Long> startTimeRange;
    ImmutablePair<Long, Long> endTimeRange;
    List<CiCdJobRunParameter> parameters;
    @JsonProperty("qualified_job_names")
    List<CiCdJobQualifiedName> qualifiedJobNames;
    Map<String, Map<String, String>> partialMatch;
    List<String> projects;
    Set<UUID> orgProductsIds;
    Map<String, SortingOrder> sortBy;
    List<String> stageNames;
    List<String> stageStatuses;
    List<String> stepNames;
    List<String> stepStatuses;

    List<String> excludeServices;
    List<String> excludeEnvironments;
    List<String> excludeInfrastructures;
    List<String> excludeRepositories;
    List<String> excludeBranches;
    List<String> excludeDeploymentTypes;
    Boolean excludeRollback;
    List<String> excludeTags;
    List<String> excludeJobNames;
    List<String> excludeJobNormalizedFullNames;
    List<String> excludeJobStatuses;
    List<String> excludeInstanceNames;
    List<String> excludeProjects;
    List<String> excludeCiCdUserIds;
    List<CICD_TYPE> excludeTypes;
    List<CiCdJobQualifiedName> excludeQualifiedJobNames;
    List<String> excludeTriageRuleNames;
    List<UUID> excludeTriageRuleIds;
    List<String> excludeStageNames;
    List<String> excludeStageStatuses;
    List<String> excludeStepNames;
    List<String> excludeStepStatuses;

    @JsonProperty("across_count")
    Integer acrossCount;

    CICD_AGG_INTERVAL aggInterval;

    public enum DISTINCT {
        job_status,
        job_name,
        qualified_job_name,
        instance_name,
        cicd_user_id,
        job_normalized_full_name,
        service,
        environment,
        repository,
        infrastructure,
        deployment_type,
        rollback,
        branch,
        tag,
        job_end,
        project_name,
        cicd_job_id,
        triage_rule,
        stage_name,
        step_name,
        stage_status,
        step_status,
        //these are time based
        trend;

        public static DISTINCT fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(DISTINCT.class, st);
        }
    }

    public enum CALCULATION {
        duration,
        count; // just a count of rows

        public static CALCULATION fromString(String st) {
            return EnumUtils.getEnumIgnoreCase(CALCULATION.class, st);
        }
    }

    public static CALCULATION getSanitizedCalculation(CiCdJobRunsFilter filter) {
        return (filter.getCalculation() != null) ? filter.getCalculation() : CALCULATION.count;
    }

    public static List<CiCdJobRunParameter> parseCiCdJobRunParameters(ObjectMapper objectMapper, List<Object> parameterObjects) throws BadRequestException {
        log.debug("parameterObjects = {}", parameterObjects);
        if(CollectionUtils.isEmpty(parameterObjects)){
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(parameterObjects);
            log.debug("serialized = {}", serialized);
            List<CiCdJobRunParameter> parameters = objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, CiCdJobRunParameter.class));
            log.debug("parameters = {}", parameters);
            return parameters;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid filter parameter: parameters");
        }
    }

    public static List<CiCdJobQualifiedName> parseCiCdQualifiedJobNames(ObjectMapper objectMapper, List<Object> qualifiedJobNameObjects) throws BadRequestException {
        log.debug("qualifiedJobNameObjects = {}", qualifiedJobNameObjects);
        if(CollectionUtils.isEmpty(qualifiedJobNameObjects)){
            return Collections.emptyList();
        }
        try {
            String serialized = objectMapper.writeValueAsString(qualifiedJobNameObjects);
            log.debug("serialized = {}", serialized);
            List<CiCdJobQualifiedName> qualifiedJobNames = objectMapper.readValue(serialized, objectMapper.getTypeFactory().constructCollectionType(List.class, CiCdJobQualifiedName.class));
            log.debug("qualifiedJobNames = {}", qualifiedJobNames);
            return qualifiedJobNames;
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid filter parameter: qualified_job_names");
        }
    }

    @SuppressWarnings("unchecked")
    public static CiCdJobRunsFilter fromDefaultListRequest(DefaultListRequest filter, DISTINCT across, CALCULATION calculation, ObjectMapper objectMapper) throws BadRequestException {
        Map<String, Object> excludeFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        CiCdJobRunsFilter.CiCdJobRunsFilterBuilder bldr = CiCdJobRunsFilter.builder()
                .cicdUserIds(getListOrDefault(filter, "cicd_user_ids"))
                .services(getListOrDefault(filter, "services"))
                .environments(getListOrDefault(filter, "environments"))
                .infrastructures(getListOrDefault(filter, "infrastructures"))
                .repositories(getListOrDefault(filter, "repositories"))
                .branches(getListOrDefault(filter, "branches"))
                .deploymentTypes(getListOrDefault(filter, "deployment_types"))
                .rollback((Boolean) MapUtils.emptyIfNull(filter.getFilter()).getOrDefault("rollback", null))
                .tags(getListOrDefault(filter, "tags"))
                .jobNames(getListOrDefault(filter, "cicd_job_names"))
                .jobNormalizedFullNames(getListOrDefault(filter, "cicd_job_normalized_full_names"))
                .jobStatuses(getListOrDefault(filter, "cicd_job_statuses"))
                .instanceNames(getListOrDefault(filter, "cicd_instance_names"))
                .startTimeRange(getTimeRange(filter, "cicd_job_run_start_time"))
                .endTimeRange(getTimeRange(filter, "cicd_job_run_end_time"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .types(CICD_TYPE.parseFromFilter(filter))
                .projects(getListOrDefault(filter, "projects"))
                .excludeServices(getListOrDefault(excludeFields, "services"))
                .excludeEnvironments(getListOrDefault(excludeFields, "environments"))
                .excludeInfrastructures(getListOrDefault(excludeFields, "infrastructures"))
                .excludeRepositories(getListOrDefault(excludeFields, "repositories"))
                .excludeBranches(getListOrDefault(excludeFields, "branches"))
                .excludeDeploymentTypes(getListOrDefault(excludeFields, "deployment_types"))
                .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                .excludeTags(getListOrDefault(excludeFields, "tags"))
                .excludeJobNames(getListOrDefault(excludeFields, "cicd_job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "cicd_job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "cicd_job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "cicd_instance_names"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .partialMatch(partialMatchMap)
                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(filter, "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(filter, "qualified_job_names")))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(filter.getAggInterval()), CICD_AGG_INTERVAL.day));

        if(across != null) {
            bldr.across(across);
        }
        if (calculation != null) {
            bldr.calculation(calculation);
        }

        CiCdJobRunsFilter cicdJobRunsFilter = bldr.build();
        log.info("cicdJobRunsFilter = {}", cicdJobRunsFilter);
        return cicdJobRunsFilter;
    }

    @SuppressWarnings("unchecked")
    public static CiCdJobRunsFilter parseCiCdJobRunsFilter(DefaultListRequest filter, ObjectMapper objectMapper) throws BadRequestException {
        Map<String, String> endTimeRange = filter.getFilterValue("end_time", Map.class)
                .orElse(Map.of());
        Map<String, String> startTimeRange = filter.getFilterValue("start_time", Map.class)
                .orElse(Map.of());

        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        Map<String, Object> excludeFields = (Map<String, Object>) filter.getFilter()
                .getOrDefault("exclude", Map.of());
        return CiCdJobRunsFilter.builder()
                .across(MoreObjects.firstNonNull(
                        CiCdJobRunsFilter.DISTINCT.fromString(
                                filter.getAcross()),
                        CiCdJobRunsFilter.DISTINCT.trend))
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .stacks(parseCiCdStacks(filter.getStacks(), CiCdJobRunsFilter.DISTINCT.class))
                .cicdUserIds(getListOrDefault(filter.getFilter(), "cicd_user_ids"))
                .services(getListOrDefault(filter, "services"))
                .environments(getListOrDefault(filter, "environments"))
                .infrastructures(getListOrDefault(filter, "infrastructures"))
                .repositories(getListOrDefault(filter, "repositories"))
                .branches(getListOrDefault(filter, "branches"))
                .deploymentTypes(getListOrDefault(filter, "deployment_types"))
                .rollback((Boolean) MapUtils.emptyIfNull(filter.getFilter()).getOrDefault("rollback", null))
                .tags(getListOrDefault(filter, "tags"))
                .jobNames(getListOrDefault(filter.getFilter(), "job_names"))
                .jobNormalizedFullNames(getListOrDefault(filter.getFilter(), "job_normalized_full_names"))
                .jobStatuses(getListOrDefault(filter.getFilter(), "job_statuses"))
                .instanceNames(getListOrDefault(filter.getFilter(), "instance_names"))
                .integrationIds(getIntegrationIds(filter))
                .types(CICD_TYPE.parseFromFilter(filter))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .excludeServices(getListOrDefault(excludeFields, "services"))
                .excludeEnvironments(getListOrDefault(excludeFields, "environments"))
                .excludeInfrastructures(getListOrDefault(excludeFields, "infrastructures"))
                .excludeRepositories(getListOrDefault(excludeFields, "repositories"))
                .excludeBranches(getListOrDefault(excludeFields, "branches"))
                .excludeDeploymentTypes(getListOrDefault(excludeFields, "deployment_types"))
                .excludeRollback((Boolean) MapUtils.emptyIfNull(excludeFields).getOrDefault("rollback", null))
                .excludeTags(getListOrDefault(excludeFields, "tags"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(DefaultListRequestUtils.getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .partialMatch(partialMatchMap)
                .parameters(parseCiCdJobRunParameters(objectMapper, getListOfObjectOrDefault(filter.getFilter(), "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(objectMapper, getListOfObjectOrDefault(filter.getFilter(), "qualified_job_names")))
                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                .orgProductsIds(getListOrDefault(filter.getFilter(), "org_product_ids").stream()
                        .map(UUID::fromString).collect(Collectors.toSet()))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(filter.getAggInterval()),
                        CICD_AGG_INTERVAL.day))
                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())))
                .build();
    }

    public static List<String> getIntegrationIds(DefaultListRequest filter) {
        return CollectionUtils.isNotEmpty(getListOrDefault(filter.getFilter(), "cicd_integration_ids")) ?
                getListOrDefault(filter.getFilter(), "cicd_integration_ids") : getListOrDefault(filter.getFilter(), "integration_ids");
    }

    public static <T extends Enum<T>> List<T> parseCiCdStacks(List<String> stackStrings, Class<T> t) {
        if (CollectionUtils.isEmpty(stackStrings)) {
            return Collections.emptyList();
        }
        List<T> results = stackStrings.stream().map(x -> EnumUtils.getEnumIgnoreCase(t, x)).collect(Collectors.toList());
        log.info("stacks = {}", results);
        return results;
    }
}
