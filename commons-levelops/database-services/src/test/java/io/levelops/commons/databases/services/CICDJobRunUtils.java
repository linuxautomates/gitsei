package io.levelops.commons.databases.services;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.CICDJobTrigger;
import io.levelops.commons.databases.models.filters.CICD_AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.platform.commons.util.StringUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.models.filters.CiCdJobRunsFilter.getIntegrationIds;
import static io.levelops.commons.databases.models.filters.CiCdJobRunsFilter.parseCiCdJobRunParameters;
import static io.levelops.commons.databases.models.filters.CiCdJobRunsFilter.parseCiCdQualifiedJobNames;
import static io.levelops.commons.databases.models.filters.CiCdJobRunsFilter.parseCiCdStacks;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOfObjectOrDefault;
import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getListOrDefault;

public class CICDJobRunUtils {
    private final static Random random = new Random();

    public static List<CICDJobRun.JobRunParam> contructIncorrectJobRunParams(){
        List<CICDJobRun.JobRunParam> params = new ArrayList<>();
        for(int j=0; j<3; j++){
            CICDJobRun.JobRunParam param = CICDJobRun.JobRunParam.builder()
                    .type("string")
                    .name("name-" + ((j*100) + random.nextInt()))
                    .value("value-" + ((j*100) + random.nextInt()))
                    .build();
            params.add(param);
        }
        return params;
    }

    public static List<CICDJobRun.JobRunParam> contructJobRunParams(){
        List<CICDJobRun.JobRunParam> params = new ArrayList<>();
        for(int j=0; j<3; j++){
            CICDJobRun.JobRunParam param = CICDJobRun.JobRunParam.builder()
                    .type("string")
                    .name("name-" + j)
                    .value("value-" + j)
                    .build();
            params.add(param);
        }
        return params;
    }
    public static CICDJobRun createCICDJobRun(CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService, CICDJob cicdJob,
                                              String company, int i, Instant startTime, Integer duration, String parentJobFullName,
                                              Long parentJobRunNumber) throws SQLException {
        return createCICDJobRun(ciCdJobRunsDatabaseService, cicdJob, company, i, startTime, Instant.ofEpochSecond(1614499200), duration, parentJobFullName, parentJobRunNumber);
    }

    public static CICDJobRun createCICDJobRun(CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService, CICDJob cicdJob,
                                              String company, int i, Instant startTime, Instant endTime, Integer duration, String parentJobFullName,
                                              Long parentJobRunNumber) throws SQLException {
        List<CICDJobRun.JobRunParam> params = contructJobRunParams();
        CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .jobRunNumber(Long.valueOf(i))
                .status("SUCCESS")
                .startTime((startTime != null) ? startTime : Instant.now())
                .endTime(endTime)
                .duration((duration == null) ? random.nextInt(1000) : duration)
                .cicdUserId("user-jenkins-" + i)
                .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                .referenceId(UUID.randomUUID().toString())
                .scmCommitIds(List.of("commit-id-1","commit-id-2","commit-id-3"))
                .params(params);

        if((StringUtils.isNotBlank(parentJobFullName) && (parentJobRunNumber != null))){
            bldr.triggers(Set.of(CICDJobTrigger.builder()
                    .id(parentJobFullName)
                    .type("UpstreamCause")
                    .buildNumber(String.valueOf(parentJobRunNumber))
                    .build()
            ));
        }
        CICDJobRun cicdJobRun = bldr.build();
        String id = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        Assert.assertNotNull(id);
        CICDJobRun expected = cicdJobRun.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }
    public static CICDJobRun createCICDJobRunWithMetadata(CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService, CICDJob cicdJob,
                                              String company, int i, Instant startTime, Integer duration, String parentJobFullName,
                                              Long parentJobRunNumber, Map<String, Object>  metadata) throws SQLException {
        return createCICDJobRunWithMetadata(ciCdJobRunsDatabaseService, cicdJob, company, i, startTime, Instant.ofEpochSecond(1614499200), duration, parentJobFullName, parentJobRunNumber, metadata);
    }

    public static CICDJobRun createCICDJobRunWithMetadata(CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService, CICDJob cicdJob,
                                                          String company, int i, Instant startTime, Instant endTime, Integer duration, String parentJobFullName,
                                                          Long parentJobRunNumber, Map<String, Object>  metadata) throws SQLException {
        List<CICDJobRun.JobRunParam> params = contructJobRunParams();
        CICDJobRun.CICDJobRunBuilder bldr = CICDJobRun.builder()
                .cicdJobId(cicdJob.getId())
                .jobRunNumber(Long.valueOf(i))
                .status("SUCCESS")
                .startTime((startTime != null) ? startTime : Instant.now())
                .endTime(endTime)
                .duration((duration == null) ? random.nextInt(1000) : duration)
                .cicdUserId("user-jenkins-" + i)
                .source(CICDJobRun.Source.ANALYTICS_PERIODIC_PUSH)
                .referenceId(UUID.randomUUID().toString())
                .scmCommitIds(List.of("commit-id-1","commit-id-2","commit-id-3"))
                .metadata(metadata)
                .params(params);

        if((StringUtils.isNotBlank(parentJobFullName) && (parentJobRunNumber != null))){
            bldr.triggers(Set.of(CICDJobTrigger.builder()
                    .id(parentJobFullName)
                    .type("UpstreamCause")
                    .buildNumber(String.valueOf(parentJobRunNumber))
                    .build()
            ));
        }
        CICDJobRun cicdJobRun = bldr.build();
        String id = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        Assert.assertNotNull(id);
        CICDJobRun expected = cicdJobRun.toBuilder().id(UUID.fromString(id)).build();
        return expected;
    }

    public static CiCdJobRunsFilter.CiCdJobRunsFilterBuilder createCiCdJobRunsFilter(DefaultListRequest request) throws BadRequestException {
        Map<String, String> endTimeRange = request.getFilterValue("end_time", Map.class).orElse(Map.of());
        Map<String, String> startTimeRange = request.getFilterValue("start_time", Map.class).orElse(Map.of());
        final Long endTimeStart = endTimeRange.get("$gt") != null ? Long.valueOf(endTimeRange.get("$gt")) : null;
        final Long endTimeEnd = endTimeRange.get("$lt") != null ? Long.valueOf(endTimeRange.get("$lt")) : null;
        final Long startTimeStart = startTimeRange.get("$gt") != null ? Long.valueOf(startTimeRange.get("$gt")) : null;
        final Long startTimeEnd = startTimeRange.get("$lt") != null ? Long.valueOf(startTimeRange.get("$lt")) : null;
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) request.getFilter().get("partial_match"));
        Map<String, Object> excludeFields = (Map<String, Object>) request.getFilter().getOrDefault("exclude", Map.of());
        return CiCdJobRunsFilter.builder()
                .across(MoreObjects.firstNonNull(
                        CiCdJobRunsFilter.DISTINCT.fromString(
                                request.getAcross()),
                        CiCdJobRunsFilter.DISTINCT.trend))
                .calculation(CiCdJobRunsFilter.CALCULATION.count)
                .stacks(parseCiCdStacks(request.getStacks(), CiCdJobRunsFilter.DISTINCT.class))
                .cicdUserIds(getListOrDefault(request.getFilter(), "cicd_user_ids"))
                .jobNames(getListOrDefault(request.getFilter(), "job_names"))
                .jobNormalizedFullNames(getListOrDefault(request.getFilter(), "job_normalized_full_names"))
                .jobStatuses(getListOrDefault(request.getFilter(), "job_statuses"))
                .instanceNames(getListOrDefault(request.getFilter(), "instance_names"))
                .integrationIds(getIntegrationIds(request))
                .types(CICD_TYPE.parseFromFilter(request))
                .projects(getListOrDefault(request.getFilter(), "projects"))
                .excludeJobNames(getListOrDefault(excludeFields, "job_names"))
                .excludeJobNormalizedFullNames(getListOrDefault(excludeFields, "job_normalized_full_names"))
                .excludeJobStatuses(getListOrDefault(excludeFields, "job_statuses"))
                .excludeInstanceNames(getListOrDefault(excludeFields, "instance_names"))
                .excludeProjects(getListOrDefault(excludeFields, "projects"))
                .excludeTypes(CICD_TYPE.parseFromFilter(excludeFields))
                .excludeCiCdUserIds(getListOrDefault(excludeFields, "cicd_user_ids"))
                .excludeQualifiedJobNames(parseCiCdQualifiedJobNames(DefaultObjectMapper.get(), getListOfObjectOrDefault(excludeFields, "qualified_job_names")))
                .partialMatch(partialMatchMap)
                .parameters(parseCiCdJobRunParameters(DefaultObjectMapper.get(), getListOfObjectOrDefault(request.getFilter(), "parameters")))
                .qualifiedJobNames(parseCiCdQualifiedJobNames(DefaultObjectMapper.get(), getListOfObjectOrDefault(request.getFilter(), "qualified_job_names")))
                .endTimeRange(ImmutablePair.of(endTimeStart, endTimeEnd))
                .startTimeRange(ImmutablePair.of(startTimeStart, startTimeEnd))
                .orgProductsIds(getListOrDefault(request.getFilter(), "org_product_ids").stream()
                        .map(UUID::fromString).collect(Collectors.toSet()))
                .aggInterval(MoreObjects.firstNonNull(CICD_AGG_INTERVAL.fromString(request.getAggInterval()),
                        CICD_AGG_INTERVAL.day))
                .sortBy(SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), List.of())));
    }
}
