package io.levelops.commons.service.dora;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.dora.DoraResponseDTO;
import io.levelops.commons.databases.models.dora.DoraSingleStateDTO;
import io.levelops.commons.databases.models.dora.DoraTimeSeriesDTO;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CiCdMetadataConditionBuilder;
import io.levelops.commons.databases.services.CiCdPartialMatchConditionBuilder;
import io.levelops.commons.databases.services.CiCdStageStepConditionBuilder;
import io.levelops.commons.databases.services.organization.ProductsDatabaseService;
import io.levelops.commons.databases.services.parsers.CiCdJobRunsFilterParsers;
import io.levelops.commons.databases.utils.AggTimeQueryHelper;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.commons.utils.dora.DoraCalculationUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jetbrains.annotations.NotNull;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;


@Log4j2
@Service
public class CiCdDoraService {
    public static final Set<String> CICD_PARTIAL_MATCH_COLUMNS = Set.of("job_normalized_full_name");
    public static final List<String> CICD_APPLICATIONS = List.of("jenkins", "azure_devops", "gitlab");
    public static final String CICD_CONDITIONS = "cicd_conditions";
    public static final String TRIAGE_CONDITIONS = "triage_conditions";

    private final NamedParameterJdbcTemplate template;
    private final CiCdJobRunsFilterParsers ciCdJobRunsFilterParsers;

    private final CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;
    private final CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;
    private final CiCdStageStepConditionBuilder ciCdStageStepConditionBuilder;

    protected CiCdDoraService(DataSource dataSource) {
        template = new NamedParameterJdbcTemplate(dataSource);
        ProductsDatabaseService productsDatabaseService
                = new ProductsDatabaseService(dataSource, DefaultObjectMapper.get());
        this.ciCdJobRunsFilterParsers = new CiCdJobRunsFilterParsers(DefaultObjectMapper.get());
        this.ciCdMetadataConditionBuilder = new CiCdMetadataConditionBuilder();
        this.ciCdPartialMatchConditionBuilder = new CiCdPartialMatchConditionBuilder();
        this.ciCdStageStepConditionBuilder = new CiCdStageStepConditionBuilder();
    }

    public DoraResponseDTO calculateNewDeploymentFrequency(String company,
                                                           CiCdJobRunsFilter filter,
                                                           OUConfiguration ouConfig,
                                                           VelocityConfigDTO velocityConfigDTO,
                                                           String stackField) {

        List<DoraTimeSeriesDTO.TimeSeriesData> tempResults = getTimeSeriesData(
                company,
                filter,
                ouConfig,
                velocityConfigDTO.getDeploymentFrequency()
                        .getVelocityConfigFilters().getDeploymentFrequency(),
                isManualJobSelectionInDF(velocityConfigDTO),
                velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString(),
                stackField
        );
        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesResult = getFilledTimeSeriesData(
                filter, velocityConfigDTO.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString(), tempResults
        );

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries(
                "day", filledTimeSeriesResult
        );
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries(
                "week", filledTimeSeriesResult
        );
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries(
                "month", filledTimeSeriesResult
        );
        Integer total = CollectionUtils.emptyIfNull(filledTimeSeriesResult).stream()
                .map(m -> m.getCount()).reduce(0, (a, b) -> a + b);
        if (stackField != null)
            total = tempResults.stream()
                    .map(DoraTimeSeriesDTO.TimeSeriesData::getStacks)
                    .map(map -> map.stream().filter(m1 -> m1.containsKey("count")).map(s1 -> s1.get("count")).mapToInt(a -> (Integer) a).sum())
                    .mapToInt(Integer::intValue).sum();


        double perDay = total / getTimeDifference(filter, velocityConfigDTO);

        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder()
                .day(timeSeriesByDay)
                .month(timeSeriesByMonth)
                .week(timeSeriesByWeek).build();
        DoraSingleStateDTO stats = DoraSingleStateDTO.builder().totalDeployment(total).countPerDay(perDay)
                .band(DoraCalculationUtils.calculateDeploymentFrequencyBand(perDay)).build();

        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats).build();
    }

    public DoraResponseDTO calculateNewChangeFailureRate(String company,
                                                         CiCdJobRunsFilter failedDeploymentFilter,
                                                         CiCdJobRunsFilter totalDeploymentFilter,
                                                         OUConfiguration ouConfig,
                                                         VelocityConfigDTO velocityConfigDTO,
                                                         String stackField
    ) {
        DoraSingleStateDTO.DoraSingleStateDTOBuilder stats = DoraSingleStateDTO.builder()
                .isAbsolute(velocityConfigDTO.getChangeFailureRate().getIsAbsoulte());

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayFailedDeployment = getTimeSeriesData(
                company,
                failedDeploymentFilter,
                ouConfig,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment(),
                isManualJobSelectionInFailedDeployment(velocityConfigDTO),
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString(),
                stackField
        );

        int totalFailed = CollectionUtils.emptyIfNull(timeSeriesByDayFailedDeployment).stream()
                .map(m -> m.getCount()).reduce(0, (a, b) -> a + b);
        if (stackField != null)
            totalFailed = timeSeriesByDayFailedDeployment.stream()
                    .map(DoraTimeSeriesDTO.TimeSeriesData::getStacks)
                    .map(map -> map.stream().filter(m1 -> m1.containsKey("count")).map(s1 -> s1.get("count")).mapToInt(a -> (Integer) a).sum())
                    .mapToInt(Integer::intValue).sum();
        List<DoraTimeSeriesDTO.TimeSeriesData> filledTimeSeriesByDayFailedDeployment = getFilledTimeSeriesData(
                failedDeploymentFilter,
                velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getFailedDeployment().getCalculationField().toString(),
                timeSeriesByDayFailedDeployment
        );

        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDay = DoraCalculationUtils.convertTimeSeries(
                "day", filledTimeSeriesByDayFailedDeployment
        );
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByWeek = DoraCalculationUtils.convertTimeSeries(
                "week", filledTimeSeriesByDayFailedDeployment
        );
        List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByMonth = DoraCalculationUtils.convertTimeSeries(
                "month", filledTimeSeriesByDayFailedDeployment
        );

        DoraTimeSeriesDTO timeSeries = DoraTimeSeriesDTO.builder()
                .day(timeSeriesByDay)
                .month(timeSeriesByMonth)
                .week(timeSeriesByWeek).build();
        stats.totalDeployment(totalFailed);

        if (velocityConfigDTO.getChangeFailureRate().getIsAbsoulte() != null
                && !velocityConfigDTO.getChangeFailureRate().getIsAbsoulte()) {
            List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayTotalDeployment = getTimeSeriesData(
                    company,
                    totalDeploymentFilter,
                    ouConfig,
                    velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment(),
                    isManualJobSelectionInTotalDeployment(velocityConfigDTO),
                    velocityConfigDTO.getChangeFailureRate().getVelocityConfigFilters().getTotalDeployment().getCalculationField().toString(),
                    stackField
            );

            int totalDeployment = CollectionUtils.emptyIfNull(timeSeriesByDayTotalDeployment).stream()
                    .map(m -> m.getCount()).reduce(0, (a, b) -> a + b);
            if (stackField != null)
                totalDeployment = timeSeriesByDayTotalDeployment.stream().map(DoraTimeSeriesDTO.TimeSeriesData::getStacks)
                        .map(map -> map.stream().filter(m1 -> m1.containsKey("count")).map(s1 -> s1.get("count")).mapToInt(a -> (Integer) a).sum())
                        .mapToInt(Integer::intValue).sum();
            if (totalFailed > 0 && totalDeployment == 0) {
                throw new RuntimeException(
                        "Invalid configuration for Change Failure Rate for WorkFlow Profile: "
                                + velocityConfigDTO.getName()
                );
            }

            double failureRate = 0;
            if (totalFailed == 0)
                failureRate = 0;
            else
                failureRate = Double.valueOf(totalFailed) * 100 / totalDeployment;

            stats.failureRate(failureRate).totalDeployment(totalDeployment)
                    .band(DoraCalculationUtils.calculateChangeFailureRateBand(failureRate));
        }
        return DoraResponseDTO.builder().timeSeries(timeSeries).stats(stats.build()).build();
    }

    @NotNull
    private static List<DoraTimeSeriesDTO.TimeSeriesData> getFilledTimeSeriesData(
            CiCdJobRunsFilter filter,
            String calculationField,
            List<DoraTimeSeriesDTO.TimeSeriesData> timeSeriesByDayFailedDeployment
    ) {
        Long startTime = 0L;
        Long endTime = 0L;
        if ("end_time".equals(calculationField)) {
            startTime = filter.getEndTimeRange().getLeft();
            endTime = filter.getEndTimeRange().getRight();
        } else {
            startTime = filter.getStartTimeRange().getLeft();
            endTime = filter.getStartTimeRange().getRight();
        }
        return DoraCalculationUtils.fillRemainingDates(startTime, endTime, timeSeriesByDayFailedDeployment);
    }

    @NotNull
    private List<DoraTimeSeriesDTO.TimeSeriesData> getTimeSeriesData(String company,
                                                                     CiCdJobRunsFilter filter,
                                                                     OUConfiguration ouConfig,
                                                                     VelocityConfigDTO.FilterTypes velocityConfigFilter,
                                                                     boolean isCICDJobIdFilterNeeded,
                                                                     String calculationOnField,
                                                                     String stackField) {
        StringBuilder orderByStringBuilder = new StringBuilder();
        AggTimeQueryHelper.AggTimeQuery trendStartAggQuery =
                AggTimeQueryHelper.getAggTimeQuery(
                        "r."+calculationOnField + " ::TIMESTAMP WITHOUT TIME ZONE",
                        filter.getAcross().toString(),
                        filter.getAggInterval() != null ? filter.getAggInterval().toString() : null, false, false
                );

        String outerSelect = trendStartAggQuery.getSelect();
        String innerSelect = trendStartAggQuery.getHelperColumn().replaceFirst(",", "");
        String groupByString = trendStartAggQuery.getGroupBy();
        orderByStringBuilder.append(trendStartAggQuery.getOrderBy());

        Map<String, Object> params = new HashMap<>();
        String nullsPosition = SortingOrder.getNullsPosition(
                SortingOrder.fromString(orderByStringBuilder.toString().split(" ")[1])
        );
        if (stackField != null) {
            innerSelect += (", " + stackField);
            groupByString += (", " + stackField);
        }
        String subQuery = getSubQuery(
                company, params, filter, innerSelect, null, false, false, ouConfig
        );
        String outerSelectForStack = getOuterSelectForStack(stackField);
        String groupByForStack = getGroupByClauseForStack(stackField);

        if(isCICDJobIdFilterNeeded) {
            params.put("cicd_job_ids", CollectionUtils.emptyIfNull(velocityConfigFilter.getEvent().getValues()).stream().map(UUID::fromString).collect(Collectors.toList()));
            subQuery = subQuery + " AND cicd_job_id IN (:cicd_job_ids)";
        }

        String sql = outerSelectForStack + "SELECT " + outerSelect + ", COUNT(id) as ct " + ((stackField != null) ? (", " + stackField) : "") + " FROM (" + subQuery + " ) a" + " GROUP BY "
                + groupByString + " ORDER BY " + orderByStringBuilder + " NULLS " + nullsPosition + groupByForStack;
        log.info("sql = " + sql);
        log.info("params = {}", params);

        return template.query(
                sql, params, (stackField == null) ? DoraCalculationUtils.getTimeSeries() : DoraCalculationUtils.getTimeSeriesForStacks(stackField)
        );
    }

    private String getOuterSelectForStack(String stackField) {
        if (stackField == null)
            return "";
        return "SELECT trend, interval, array_agg(" + stackField + " || ':' || ct) as " + stackField + " FROM (";
    }

    private String getGroupByClauseForStack(String stackField) {
        if (stackField == null)
            return "";
        return ") b GROUP BY b.interval,b.trend";
    }

    private String getSubQuery(String company, Map<String, Object> params, CiCdJobRunsFilter filter, String innerSelect,
                               DbListResponse<DbAggregationResult> resultsWithoutStacks,
                               Boolean isList, Boolean hasStacks,
                               OUConfiguration ouConfig) {
        Map<String, List<String>> conditions = createWhereClauseAndUpdateParamsJobRuns(
                company, filter, params, null, isList, ouConfig
        );
        return ciCdJobRunsFilterParsers.getSqlStmt(company, conditions, innerSelect, isList, filter, false);
    }

    private Double getTimeDifference(CiCdJobRunsFilter ciCdJobRunsFilterilter,
                                     VelocityConfigDTO velocityConfig) {
        if ("end_time".equals(velocityConfig.getDeploymentFrequency().getVelocityConfigFilters().getDeploymentFrequency().getCalculationField().toString())) {
            return DoraCalculationUtils.getTimeDifference(ciCdJobRunsFilterilter.getEndTimeRange());
        } else {
            return DoraCalculationUtils.getTimeDifference(ciCdJobRunsFilterilter.getStartTimeRange());
        }
    }

    public Map<String, List<String>> createWhereClauseAndUpdateParamsJobRuns(String company, CiCdJobRunsFilter filter,
                                                                             Map<String, Object> params, String suffix,
                                                                             Boolean isList, OUConfiguration ouConfig) {
        List<String> criterias = new ArrayList<>();
        List<String> triageCriterias = new ArrayList<>();
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            criterias.add("i.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) { // OU: user
            var columnName = "r.cicd_user_id" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if (OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) {
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params,
                        IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    criterias.add(MessageFormat.format(
                            "{0} IN (SELECT cloud_id FROM ({1}) l)",
                            "r.cicd_user_id", usersSelect
                    ));
                }
            } else if (CollectionUtils.isNotEmpty(filter.getCicdUserIds())) {
                TeamUtils.addUsersCondition(
                        company, criterias, params, "r.cicd_user_id", columnNameParam,
                        false, filter.getCicdUserIds(), CICD_APPLICATIONS
                );
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds())) {
            String columnNameParam = "r.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, criterias, params, "r.cicd_user_id",
                    columnNameParam, false, filter.getExcludeCiCdUserIds(), CICD_APPLICATIONS, true);
        }
        if (CollectionUtils.isNotEmpty(filter.getTypes())) {
            criterias.add("i.type IN (:types" + paramSuffix + ")");
            params.put("types" + paramSuffix, filter.getTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTypes())) {
            criterias.add("i.type NOT IN (:excl_types" + paramSuffix + ")");
            params.put("excl_types" + paramSuffix, filter.getExcludeTypes().stream()
                    .map(CICD_TYPE::toString)
                    .collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            criterias.add("j.project_name IN (:projects" + paramSuffix + ")");
            params.put("projects" + paramSuffix, filter.getProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getTriageRuleNames())) {
            criterias.add("jen.triage_rule IN (:triage_rules" + paramSuffix + ")");
            triageCriterias.add("name IN (:triage_rules" + paramSuffix + ")");
            params.put("triage_rules" + paramSuffix, filter.getTriageRuleNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getTriageRuleIds())) {
            criterias.add("jen.triage_rule_id IN (:triage_rule_ids" + paramSuffix + ")");
            triageCriterias.add("t.id IN (:triage_rule_ids" + paramSuffix + ")");
            params.put("triage_rule_ids" + paramSuffix, filter.getTriageRuleIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTriageRuleNames())) {
            criterias.add("jen.triage_rule NOT IN (:excl_triage_rules" + paramSuffix + ")");
            triageCriterias.add("name NOT IN (:excl_triage_rules" + paramSuffix + ")");
            params.put("excl_triage_rules" + paramSuffix, filter.getExcludeTriageRuleNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeTriageRuleIds())) {
            criterias.add("jen.triage_rule_id NOT IN (:excl_triage_rule_ids" + paramSuffix + ")");
            triageCriterias.add("t.id NOT IN (:excl_triage_rule_ids" + paramSuffix + ")");
            params.put("excl_triage_rule_ids" + paramSuffix, filter.getExcludeTriageRuleIds());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeProjects())) {
            criterias.add("j.project_name NOT IN (:excl_projects" + paramSuffix + ")");
            params.put("excl_projects" + paramSuffix, filter.getExcludeProjects());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNames())) {
            criterias.add("j.job_name IN (:job_names" + paramSuffix + ")");
            params.put("job_names" + paramSuffix, filter.getJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNames())) {
            criterias.add("j.job_name NOT IN (:excl_job_names" + paramSuffix + ")");
            params.put("excl_job_names" + paramSuffix, filter.getExcludeJobNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getJobNormalizedFullNames())) {
            criterias.add("j.job_normalized_full_name IN (:job_normalized_full_names" + paramSuffix + ")");
            params.put("job_normalized_full_names" + paramSuffix, filter.getJobNormalizedFullNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobNormalizedFullNames())) {
            criterias.add("j.job_normalized_full_name NOT IN (:excl_job_normalized_full_names" + paramSuffix + ")");
            params.put("excl_job_normalized_full_names" + paramSuffix, filter.getExcludeJobNormalizedFullNames());
        }
        if (filter.getStartTimeRange() != null) {
            ImmutablePair<Long, Long> startTimeRange = filter.getStartTimeRange();
            if (startTimeRange.getLeft() != null) {
                criterias.add("r.start_time >= TO_TIMESTAMP(:start_time_start" + paramSuffix + ")");
                params.put("start_time_start" + paramSuffix, startTimeRange.getLeft());
            }
            if (startTimeRange.getRight() != null) {
                criterias.add("r.start_time <= TO_TIMESTAMP(:start_time_end" + paramSuffix + ")");
                params.put("start_time_end" + paramSuffix, startTimeRange.getRight());
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getQualifiedJobNames())) {
            List<String> qualifiedJobNamesCriterea = new ArrayList<>();
            for (int i = 0; i < filter.getQualifiedJobNames().size(); i++) {
                CiCdJobQualifiedName qualifiedJobName = filter.getQualifiedJobNames().get(i);
                if (StringUtils.isBlank(qualifiedJobName.getJobName())) {
                    log.warn("qualified job name, job name is null or empty skipping it!! {}", qualifiedJobName);
                    continue;
                }
                String instanceNameKey = "instance_name" + i;
                String jobNameKey = "job_name" + i;
                if (qualifiedJobName.getInstanceName() == null) {
                    qualifiedJobNamesCriterea.add("(i.name IS NULL AND job_name = :" + jobNameKey + paramSuffix + ")");
                    params.put(jobNameKey + paramSuffix, qualifiedJobName.getJobName());
                } else {
                    qualifiedJobNamesCriterea.add("(i.name = :" + instanceNameKey + paramSuffix + " AND job_name = :" + jobNameKey + paramSuffix + ")");
                    params.put(instanceNameKey + paramSuffix, qualifiedJobName.getInstanceName());
                    params.put(jobNameKey + paramSuffix, qualifiedJobName.getJobName());
                }
            }
            if (CollectionUtils.isNotEmpty(qualifiedJobNamesCriterea)) {
                criterias.add("( " + String.join(" OR ", qualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeQualifiedJobNames())) {
            List<String> excludeQualifiedJobNamesCriterea = new ArrayList<>();
            for (int i = 0; i < filter.getExcludeQualifiedJobNames().size(); i++) {
                CiCdJobQualifiedName excludeQualifiedJobName = filter.getExcludeQualifiedJobNames().get(i);
                if (StringUtils.isBlank(excludeQualifiedJobName.getJobName())) {
                    log.warn("exclude qualified job name, job name is null or empty skipping it!! {}", excludeQualifiedJobName);
                    continue;
                }
                String instanceNameKey = "excl_instance_name" + i;
                String jobNameKey = "excl_job_name" + i;
                if (excludeQualifiedJobName.getInstanceName() == null) {
                    excludeQualifiedJobNamesCriterea.add("(i.name IS NULL AND job_name != :" + jobNameKey + paramSuffix + ")");
                    params.put(jobNameKey + paramSuffix, excludeQualifiedJobName.getJobName());
                } else {
                    excludeQualifiedJobNamesCriterea.add("(i.name != :" + instanceNameKey + paramSuffix + " AND job_name != :" + jobNameKey + paramSuffix + ")");
                    params.put(instanceNameKey + paramSuffix, excludeQualifiedJobName.getInstanceName());
                    params.put(jobNameKey + paramSuffix, excludeQualifiedJobName.getJobName());
                }
            }
            if (CollectionUtils.isNotEmpty(excludeQualifiedJobNamesCriterea)) {
                criterias.add("( " + String.join(" OR ", excludeQualifiedJobNamesCriterea) + " )");
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getJobStatuses())) {
            criterias.add("r.status IN (:job_statuses" + paramSuffix + ")");
            params.put("job_statuses" + paramSuffix, filter.getJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobStatuses())) {
            criterias.add("r.status NOT IN (:excl_job_statuses" + paramSuffix + ")");
            params.put("excl_job_statuses" + paramSuffix, filter.getExcludeJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getInstanceNames())) {
            criterias.add("i.name IN (:instance_names" + paramSuffix + ")");
            params.put("instance_names" + paramSuffix, filter.getInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeInstanceNames())) {
            criterias.add("i.name NOT IN (:excl_instance_names" + paramSuffix + ")");
            params.put("excl_instance_names" + paramSuffix, filter.getExcludeInstanceNames());
        }
        if (CollectionUtils.isNotEmpty(filter.getParameters())) {
            List<String> parametersCriterea = new ArrayList<>();
            for (int i = 0; i < filter.getParameters().size(); i++) {
                CiCdJobRunParameter param = filter.getParameters().get(i);
                if (StringUtils.isBlank(param.getName())) {
                    log.warn("param name is null or empty skipping it!! {}", param);
                    continue;
                }
                if (CollectionUtils.isEmpty(param.getValues())) {
                    log.warn("param values is null or empty skipping it!! {}", param);
                    continue;
                }
                List<String> sanitizedValues = param.getValues().stream().filter(StringUtils::isNotBlank).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(sanitizedValues)) {
                    log.warn("sanitized param values is null or empty skipping it!! {}", sanitizedValues);
                    continue;
                }
                String nameKey = "name" + i;
                String valueKey = "values" + i;
                parametersCriterea.add("(p.name = :" + nameKey + paramSuffix + " AND p.value IN (:" + valueKey + paramSuffix + "))");
                params.put(nameKey + paramSuffix, param.getName());
                params.put(valueKey + paramSuffix, param.getValues());
            }
            if (CollectionUtils.isNotEmpty(parametersCriterea)) {
                criterias.add("( " + String.join(" OR ", parametersCriterea) + " )");
            }
        }
        if(filter.getIsCiJob() != null && filter.getIsCdJob() != null){
            criterias.add("(r.cd = :is_cd" + paramSuffix + " OR r.ci = :is_ci" + paramSuffix + ")");
            params.put("is_cd" + paramSuffix, filter.getIsCdJob());
            params.put("is_ci" + paramSuffix, filter.getIsCiJob());
        } else if (filter.getIsCiJob() != null){
            criterias.add("r.ci = :is_ci" + paramSuffix);
            params.put("is_ci" + paramSuffix, filter.getIsCiJob());
        } else if (filter.getIsCdJob() != null) {
            criterias.add("r.cd = :is_cd" + paramSuffix);
            params.put("is_cd" + paramSuffix, filter.getIsCdJob());
        }
        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                criterias.add("r.end_time >= to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                criterias.add("r.end_time <= to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }

        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
        if (isList && (CiCdJobRunsFilter.DISTINCT.trend == filter.getAcross() ||
                CollectionUtils.isNotEmpty(filter.getStacks())
                        && filter.getStacks().get(0).equals(CiCdJobRunsFilter.DISTINCT.trend))
                && filter.getEndTimeRange() != null && filter.getStartTimeRange() != null) {
            criterias.add("start_time >= to_timestamp(:start_time_from" + paramSuffix + ")");
            params.put("start_time_from" + paramSuffix, Instant.now().minus(acrossCount, ChronoUnit.DAYS).getEpochSecond());
        }
        if (CiCdJobRunsFilter.DISTINCT.instance_name == filter.getAcross()) {
            criterias.add("i.name IS NOT NULL");
        }
        if (CiCdJobRunsFilter.DISTINCT.job_normalized_full_name == filter.getAcross()) {
            criterias.add("j.job_normalized_full_name IS NOT NULL");
        }
        if (CiCdJobRunsFilter.DISTINCT.triage_rule == filter.getAcross() ||
                (CollectionUtils.isNotEmpty(filter.getStacks())
                        && filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.triage_rule))) {
            criterias.add("triage_rule IS NOT NULL");
        }

        ciCdMetadataConditionBuilder.prepareMetadataConditions(filter, params, null, criterias);

        ciCdPartialMatchConditionBuilder.preparePartialMatchConditions(filter, params, criterias, paramSuffix, CICD_PARTIAL_MATCH_COLUMNS);

        ciCdStageStepConditionBuilder.prepareCiCdStageStepConditions(filter, params, criterias, paramSuffix, company);

        return Map.of(CICD_CONDITIONS, criterias,
                TRIAGE_CONDITIONS, triageCriterias);
    }

    public List<Map<String, Object>> getCicdJobParams(String company, List<String> jobIds) throws SQLException {
        Map<String, Object> params = new HashMap<>();
        String whereCloseCondition = ")";
        if (jobIds != null && jobIds.size() > 0) {
            params.put("cicd_job_ids", jobIds.stream().map(UUID::fromString).collect(Collectors.toList()));
            whereCloseCondition = " WHERE cj.id IN (:cicd_job_ids))";
        }
        String sql = "SELECT DISTINCT name, value FROM " + company + ".cicd_job_run_params WHERE cicd_job_run_id " +
                "IN (SELECT jr.id FROM " + company + ".cicd_job_runs jr INNER JOIN "
                + company + ".cicd_jobs cj ON cj.id = jr.cicd_job_id" + whereCloseCondition;

        log.info("sql = " + sql); // not using "sql = {}" because of log4j does some funny nested substitution with '{}'
        log.info("params = {}", params);

        return template.queryForList(sql, params);
    }

    private boolean isManualJobSelectionInDF(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO != null &&
                CollectionUtils.isNotEmpty(velocityConfigDTO
                        .getDeploymentFrequency()
                        .getVelocityConfigFilters()
                        .getDeploymentFrequency()
                        .getEvent().getValues());
    }

    private boolean isManualJobSelectionInTotalDeployment(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO != null &&
                CollectionUtils.isNotEmpty(velocityConfigDTO
                        .getChangeFailureRate()
                        .getVelocityConfigFilters()
                        .getTotalDeployment()
                        .getEvent().getValues());
    }

    private boolean isManualJobSelectionInFailedDeployment(VelocityConfigDTO velocityConfigDTO) {
        return velocityConfigDTO != null &&
                CollectionUtils.isNotEmpty(velocityConfigDTO
                        .getChangeFailureRate()
                        .getVelocityConfigFilters()
                        .getFailedDeployment()
                        .getEvent().getValues());
    }
}
