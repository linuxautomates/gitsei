package io.levelops.commons.databases.models.filters;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.utils.ListUtils;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

@Value
@Builder(toBuilder = true)
public class CiCdPipelineJobRunsFilter {
    DISTINCT across;
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
    List<String> jobIds;
    List<String> instanceNames;
    String jobRunId;
    List<CICD_TYPE> types;
    List<String> projects;
    ImmutablePair<Long, Long> startTimeRange;
    ImmutablePair<Long, Long> endTimeRange;
    List<CiCdJobRunParameter> parameters;
    @JsonProperty("qualified_job_names")
    List<CiCdJobQualifiedName> qualifiedJobNames;
    Map<String, Map<String, String>> partialMatch;
    Set<UUID> orgProductsIds;
    Map<String, SortingOrder> sortBy;

    List<String> excludeServices;
    List<String> excludeEnvironments;
    List<String> excludeInfrastructures;
    List<String> excludeRepositories;
    List<String> excludeBranches;
    List<String> excludeDeploymentTypes;
    Boolean excludeRollback;
    List<String> excludeTags;
    List<String> excludeJobNames;
    List<String> excludeJobStatuses;
    List<String> excludeInstanceNames;
    List<String> excludeProjects;
    List<String> excludeJobNormalizedFullNames;
    List<String> excludeCiCdUserIds;
    List<String> excludeJobIds;
    List<String> excludeCiCdJobRunIds;
    List<CiCdJobQualifiedName> excludeQualifiedJobNames;
    List<CICD_TYPE> excludeTypes;
    @JsonProperty("parent_cicd_job_ids")
    List<String> parentCiCdJobIds;

    @JsonProperty("cicd_job_run_ids")
    List<String> ciCdJobRunIds;
    List<String> integrationIds;

    String jobNamePartial;

    @JsonProperty("across_count")
    Integer acrossCount;

    CICD_AGG_INTERVAL aggInterval;

    public enum DISTINCT {
        job_status,
        job_name,
        qualified_job_name,
        job_normalized_full_name,
        instance_name,
        cicd_user_id,
        cicd_job_id,
        job_end,
        project_name,
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

    public static CALCULATION getSanitizedCalculation(CiCdPipelineJobRunsFilter filter){
        return (filter.getCalculation() != null) ? filter.getCalculation() : CALCULATION.count;
    }

    public static CiCdJobRunsFilter parseToCiCdJobRunsFilter(CiCdPipelineJobRunsFilter filter) {
        return CiCdJobRunsFilter.builder()
                .across(filter.getAcross() == null ? CiCdJobRunsFilter.DISTINCT.trend :
                                CiCdJobRunsFilter.DISTINCT.fromString(filter.getAcross().toString()))
                .calculation(filter.getCalculation() == null ? CiCdJobRunsFilter.CALCULATION.count :
                        CiCdJobRunsFilter.CALCULATION.fromString(filter.getCalculation().toString()))
                .stacks(ListUtils.emptyIfNull(filter.getStacks()).stream()
                        .map(s -> CiCdJobRunsFilter.DISTINCT.fromString(s.toString())).collect(Collectors.toList()))
                .cicdUserIds(filter.getCicdUserIds())
                .services(filter.getServices())
                .environments(filter.getEnvironments())
                .infrastructures(filter.getInfrastructures())
                .repositories(filter.getRepositories())
                .branches(filter.getBranches())
                .deploymentTypes(filter.getDeploymentTypes())
                .rollback(filter.getRollback())
                .tags(filter.getTags())
                .jobNames(filter.getJobNames())
                .jobNormalizedFullNames(filter.getJobNormalizedFullNames())
                .jobStatuses(filter.getJobStatuses())
                .instanceNames(filter.getInstanceNames())
                .integrationIds(filter.getIntegrationIds())
                .types(filter.getTypes())
                .projects(filter.getProjects())
                .excludeServices(filter.getExcludeServices())
                .excludeEnvironments(filter.getExcludeEnvironments())
                .excludeInfrastructures(filter.getExcludeInfrastructures())
                .excludeRepositories(filter.getExcludeRepositories())
                .excludeBranches(filter.getExcludeBranches())
                .excludeDeploymentTypes(filter.getExcludeDeploymentTypes())
                .excludeRollback(filter.getExcludeRollback())
                .excludeTags(filter.getExcludeTags())
                .excludeJobNames(filter.getExcludeJobNames())
                .excludeJobNormalizedFullNames(filter.getExcludeJobNormalizedFullNames())
                .excludeJobStatuses(filter.getExcludeJobStatuses())
                .excludeInstanceNames(filter.getExcludeInstanceNames())
                .excludeProjects(filter.getExcludeProjects())
                .excludeTypes(filter.getExcludeTypes())
                .excludeCiCdUserIds(filter.getExcludeCiCdUserIds())
                .excludeQualifiedJobNames(filter.getExcludeQualifiedJobNames())
                .partialMatch(filter.getPartialMatch())
                .parameters(filter.getParameters())
                .qualifiedJobNames(filter.getQualifiedJobNames())
                .endTimeRange(filter.getEndTimeRange())
                .startTimeRange(filter.getStartTimeRange())
                .orgProductsIds(filter.getOrgProductsIds())
                .aggInterval(filter.getAggInterval())
                .sortBy(filter.getSortBy())
                .build();
    }
}
