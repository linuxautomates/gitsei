package io.levelops.commons.databases.services.velocity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.models.filters.CiCdJobQualifiedName;
import io.levelops.commons.databases.models.filters.CiCdJobRunParameter;
import io.levelops.commons.databases.models.filters.CiCdJobRunsFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.ScmCommitFilter;
import io.levelops.commons.databases.models.filters.ScmPrFilter;
import io.levelops.commons.databases.models.filters.VelocityFilter;
import io.levelops.commons.databases.models.filters.WorkItemsFilter;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.query_criteria.WorkItemMilestoneQueryCriteria;
import io.levelops.commons.databases.query_criteria.WorkItemQueryCriteria;
import io.levelops.commons.databases.services.CiCdMetadataConditionBuilder;
import io.levelops.commons.databases.services.CiCdPartialMatchConditionBuilder;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.jira.conditions.JiraConditionsBuilder;
import io.levelops.commons.databases.utils.TeamUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.utils.ListUtils;
import io.levelops.commons.utils.NumberUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringSubstitutor;

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.commons.databases.services.CiCdAggsService.CICD_APPLICATIONS;
import static io.levelops.commons.databases.services.CiCdAggsService.CICD_CONDITIONS;
import static io.levelops.commons.databases.services.CiCdAggsService.TRIAGE_CONDITIONS;
import static io.levelops.commons.databases.services.JiraIssueService.ISSUES_TABLE;
import static io.levelops.commons.databases.services.JiraIssueService.JIRA_ISSUE_SPRINTS;
import static io.levelops.commons.databases.services.ScmAggService.COMMITS_TABLE;
import static io.levelops.commons.databases.services.ScmAggService.PRS_TABLE;

@Log4j2
@Value
public class VelocityAggsQueryBuilder {
    public static final Set<String> CICD_PARTIAL_MATCH_COLUMNS = Set.of("job_normalized_full_name");
    private static final Pattern JIRA_IM_DOT_PREFIX = Pattern.compile("^im\\.");
    private static final boolean FOR_COUNT_SQL = true;
    private static final boolean NOT_FOR_COUNT_SQL = false;
    private static final String PR_JOIN_CONDITION = "PR_JOIN_CONDITION";
    public static final Set<String> TOTAL_LEAD_TIME_COLUMN_KEYS = Set.of("data", "total"); //remove data in future

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    List<String> innerSelects = new ArrayList<>();

    List<String> baseSelects = new ArrayList<>();
    List<String> baseGroupBys = new ArrayList<>();

    List<String> reduceSelects = new ArrayList<>();
    List<String> enrichSelects = new ArrayList<>();
    List<String> enrichConditions = new ArrayList<>();
    List<String> enrichSortBy = new ArrayList<>();
    StringBuilder enrichLimitOffset = new StringBuilder();

    List<String> medianSelects = new ArrayList<>();
    List<String> medianGroupBys = new ArrayList<>();
    List<String> medianOrderBys = new ArrayList<>();

    StringBuilder joins = new StringBuilder();
    AtomicInteger offset = new AtomicInteger();
    Map<String, Object> params = new HashMap<>();
    Map<Integer, VelocityConfigDTO.Stage> offsetStageMap = new HashMap<>();
    AtomicInteger lastCicdEventStageOffset = new AtomicInteger(-1);
    List<String> whereConditions = new ArrayList<>();
    List<String> ratingConditions = new ArrayList<>();

    private final JiraConditionsBuilder jiraConditionsBuilder;
    private final ScmAggService scmAggService;
    private final CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder;
    private final CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder;
    private final boolean disablePrJiraCorrelationForPrVelocity;

    public VelocityAggsQueryBuilder(JiraConditionsBuilder jiraConditionsBuilder, ScmAggService scmAggService, boolean disablePrJiraCorrelationForPrVelocity, CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder, CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder) {
        this.jiraConditionsBuilder = jiraConditionsBuilder;
        this.scmAggService = scmAggService;
        this.disablePrJiraCorrelationForPrVelocity = disablePrJiraCorrelationForPrVelocity;
        this.ciCdMetadataConditionBuilder = ciCdMetadataConditionBuilder;
        this.ciCdPartialMatchConditionBuilder = ciCdPartialMatchConditionBuilder;
    }

    public VelocityAggsQueryBuilder(JiraConditionsBuilder jiraConditionsBuilder, ScmAggService scmAggService, CiCdMetadataConditionBuilder ciCdMetadataConditionBuilder, CiCdPartialMatchConditionBuilder ciCdPartialMatchConditionBuilder) {
        this(jiraConditionsBuilder, scmAggService, false, ciCdMetadataConditionBuilder, ciCdPartialMatchConditionBuilder);
    }

    private Integer parseHistogramStageName(final VelocityFilter velocityFilter, final String velocityConfigName) throws BadRequestException {
        if (velocityFilter.getAcross() == VelocityFilter.DISTINCT.trend && velocityFilter.getAcross() == VelocityFilter.DISTINCT.velocity) {
            throw new BadRequestException(String.format("Across %s is neither histogram nor rating, parsing histogram stage is not supported!", velocityFilter.getAcross()));
        }
        if (StringUtils.isBlank(velocityFilter.getHistogramStageName())) {
            throw new BadRequestException("Histogram stage name cannot be null or empty!");
        }
        Map<String, Integer> stageNameTOOffsetMap = new HashMap<>();
        for (Map.Entry<Integer, VelocityConfigDTO.Stage> e : offsetStageMap.entrySet()) {
            stageNameTOOffsetMap.put(e.getValue().getName(), e.getKey());
        }
        if (!stageNameTOOffsetMap.containsKey(velocityFilter.getHistogramStageName())) {
            throw new BadRequestException(String.format("Velocity config %s, does not contain histogram stage %s", velocityConfigName, velocityFilter.getHistogramStageName()));
        }
        return stageNameTOOffsetMap.get(velocityFilter.getHistogramStageName());
    }

    private void processSorts(final VelocityFilter velocityFilter) {
        if (velocityFilter.getAcross() != VelocityFilter.DISTINCT.values) {
            log.info("across is not values, sorting not needed!");
            return;
        }
        Map<String, Integer> stageNameTOOffsetMap = new HashMap<>();
        for (Map.Entry<Integer, VelocityConfigDTO.Stage> e : offsetStageMap.entrySet()) {
            stageNameTOOffsetMap.put(e.getValue().getName(), e.getKey());
        }
        if (MapUtils.isEmpty(velocityFilter.getSort())) {
            log.error("velocityFilter.getSort() is null or empty!");
            enrichSortBy.add(String.format("t %s NULLS LAST", SortingOrder.DESC));
            enrichSortBy.add(String.format("u_id %s", SortingOrder.DESC));
            return;
        }
        for (Map.Entry<String, SortingOrder> e : velocityFilter.getSort().entrySet()) {
            String sortingKeyStageName = e.getKey();
            SortingOrder sortingOrder = e.getValue();
            if (stageNameTOOffsetMap.containsKey(sortingKeyStageName)) {
                Integer stageOffSet = stageNameTOOffsetMap.get(sortingKeyStageName);
                enrichSortBy.add(String.format("s%d %s NULLS LAST", stageOffSet, sortingOrder));
                continue;
            }
            if (TOTAL_LEAD_TIME_COLUMN_KEYS.contains(sortingKeyStageName)) {
                log.info("for stagename or sort key {} is total!", sortingKeyStageName);
                enrichSortBy.add(String.format("t %s NULLS LAST", sortingOrder));
                enrichSortBy.add(String.format("u_id %s", SortingOrder.DESC));
            } else {
                log.error("for stagename or sort key {} offset not found, ignoring it!", sortingKeyStageName);
            }
        }
        log.info("enrichSortBy = {}", enrichSortBy);
    }

    static String generateCoalescedValuesForPrevOffset(Integer currentOffset) {
        List<String> descendingEvents = new ArrayList<>();
        for (int i = currentOffset; i >= 0; i--) {
            descendingEvents.add(String.format("e%d", i));
        }
        descendingEvents.add("estart");
        String coalescedValues = "COALESCE(" + String.join(",", descendingEvents) + ")";
        log.debug("coalescedValues = {}", coalescedValues);
        return coalescedValues;
    }

    private void processStage(VelocityConfigDTO.Stage stage, final VelocityFilter velocityFilter, final WorkItemsType workItemsType) {
        if (stage.getEvent() == null) {
            return;
        }
        int currentOffset = offset.getAndIncrement();
        VelocityConfigDTO.Event event = stage.getEvent();
        VelocityConfigDTO.EventType eventType = stage.getEvent().getType();
        VelocityFilter.CALCULATION calculation = velocityFilter.getCalculation();
        boolean updatePRJoinCondition = calculation != VelocityFilter.CALCULATION.pr_velocity;
        String aggregateFunction = "MIN";
        if ((eventType == VelocityConfigDTO.EventType.JIRA_STATUS) || (eventType == VelocityConfigDTO.EventType.WORKITEM_STATUS)) {
            String placeholderParam = String.format("s%d_value", currentOffset);
            if (workItemsType == WorkItemsType.WORK_ITEM) {
                innerSelects.add(String.format("CASE WHEN (ims.field_type = 'status' AND ims.field_value in (:%s) ) THEN CASE WHEN ims.start_date IS NOT NULL THEN COALESCE(ims.end_date,now()) ELSE ims.end_date END ELSE NULL END as e%d", placeholderParam, currentOffset));
            } else {
                innerSelects.add(String.format("CASE WHEN (ims.status in (:%s) ) THEN to_timestamp(ims.end_time) ELSE NULL END as e%d", placeholderParam, currentOffset));
            }
            params.put(placeholderParam, event.getValues());
            //reduceSelects.add(String.format("MIN(e%d) as e%d", currentOffset, currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.SCM_COMMIT_CREATED) {
            innerSelects.add(String.format("c.committed_at as e%d", currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.SCM_PR_CREATED) {
            int index = 0;
            if (MapUtils.isNotEmpty(event.getParams())) {
                List<String> sourceBranches = event.getParams().get("source_branches");
                if (sourceBranches != null) {
                    String placeholderParam = String.format("s%d_value_%d", currentOffset, index++);
                    String condition = String.format("pr.source_branch in (:%s) \n", placeholderParam);
                    updatePRCondition(condition, updatePRJoinCondition);
                    params.put(placeholderParam, sourceBranches);
                }
                List<String> targetBranches = event.getParams().get("target_branches");
                if (targetBranches != null) {
                    String placeholderParam = String.format("s%d_value_%d", currentOffset, index++);
                    String condition = String.format("pr.target_branch in (:%s) \n", placeholderParam);
                    updatePRCondition(condition, updatePRJoinCondition);
                    params.put(placeholderParam, targetBranches);
                }
            }
            innerSelects.add(String.format("pr.pr_created_at as e%d", currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.SCM_PR_LABEL_ADDED) {
            String placeholderParam = String.format("s%d_value", currentOffset);
            if (event.isAnyLabelAdded()) {
                joins.append(String.format("               left join ${company}.scm_pullrequest_labels as s%d on s%d.scm_pullrequest_id = pr.id \n",
                        currentOffset, currentOffset, currentOffset, currentOffset, placeholderParam));
            } else {
                joins.append(String.format("               left join ${company}.scm_pullrequest_labels as s%d on s%d.scm_pullrequest_id = pr.id AND s%d.name in (:%s) \n",
                        currentOffset, currentOffset, currentOffset, placeholderParam));
                params.put(placeholderParam, event.getValues());
            }
            innerSelects.add(String.format("s%d.label_added_at as e%d", currentOffset, currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.SCM_PR_REVIEW_STARTED) {
            innerSelects.add(String.format("case when (prr.state IN ( 'COMMENTED', 'CHANGES_REQUESTED' ) ) then prr.reviewed_at else null end as e%d", currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.SCM_PR_APPROVED) {
            aggregateFunction = getAggregationFunction(stage);
            innerSelects.add(String.format("case when (prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) ) then prr.reviewed_at else null end as e%d", currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.SCM_PR_MERGED) {
            aggregateFunction = "MAX";
            int index = 0;
            if (MapUtils.isNotEmpty(event.getParams())) {
                List<String> sourceBranches = event.getParams().get("source_branches");
                if (CollectionUtils.isNotEmpty(sourceBranches)) {
                    String placeholderParam = String.format("s%d_value_%d", currentOffset, index++);
                    String condition = String.format("pr.source_branch in (:%s) \n", placeholderParam);
                    updatePRCondition(condition, updatePRJoinCondition);
                    params.put(placeholderParam, sourceBranches);
                }
                List<String> targetBranches = event.getParams().get("target_branches");
                if (CollectionUtils.isNotEmpty(targetBranches)) {
                    String placeholderParam = String.format("s%d_value_%d", currentOffset, index++);
                    String condition = String.format("pr.target_branch in (:%s) \n", placeholderParam);
                    updatePRCondition(condition, updatePRJoinCondition);
                    params.put(placeholderParam, targetBranches);
                }
                List<String> sourceBranchesRegex = event.getParams().get("source_branches_regex");
                if (CollectionUtils.isNotEmpty(sourceBranchesRegex)) {
                    String placeholderParam = String.format("s%d_value_%d", currentOffset, index++);
                    String condition = String.format("pr.source_branch ~ :%s \n", placeholderParam);
                    updatePRCondition(condition, updatePRJoinCondition);
                    params.put(placeholderParam, sourceBranchesRegex.get(0));
                }
                List<String> targetBranchesRegex = event.getParams().get("target_branches_regex");
                if (CollectionUtils.isNotEmpty(targetBranchesRegex)) {
                    String placeholderParam = String.format("s%d_value_%d", currentOffset, index++);
                    String condition = String.format("pr.target_branch ~ :%s \n", placeholderParam);
                    updatePRCondition(condition, updatePRJoinCondition);
                    params.put(placeholderParam, targetBranchesRegex.get(0));
                }
            }
            innerSelects.add(String.format("pr.pr_merged_at as e%d", currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.DEPLOYMENT_JOB_RUN) {
            lastCicdEventStageOffset.set(currentOffset);
            List<String> cicdCritereas = new ArrayList<>();
            List<String> cicdBuildJobCritereas = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(event.getValues())) {
                String placeholderParam = String.format("s%d_value", currentOffset);
                cicdCritereas.add(String.format("jr.cicd_job_id in (:%s)", placeholderParam));
                params.put(placeholderParam, parseUuidList(event.getValues()));
            }
            String deployJobMarker = "";
            List<String> buildJobIds = new ArrayList<>();
            if (MapUtils.isNotEmpty(event.getParams())) {
                List<String> parametersCriterea = new ArrayList<>();
                int i = 0;
                for (Map.Entry<String, List<String>> entry : event.getParams().entrySet()) {
                    if (entry.getKey().equalsIgnoreCase("deploy_job_marker")) {
                        deployJobMarker = entry.getValue().get(0);
                        continue;
                    }

                    if (entry.getKey().equalsIgnoreCase("build_job_ids")) {
                        buildJobIds = entry.getValue();
                        continue;
                    }
                    String nameKey = "s" + currentOffset + "_name_" + i;
                    String valueKey = "s" + currentOffset + "_value_" + i;
                    parametersCriterea.add("(jrp.name = :" + nameKey + " AND jrp.value IN (:" + valueKey + "))");
                    params.put(nameKey, entry.getKey());
                    params.put(valueKey, entry.getValue());
                    i++;
                }
                if (CollectionUtils.isNotEmpty(parametersCriterea)) {
                    cicdCritereas.add("( " + String.join(" OR ", parametersCriterea) + " )");
                }
            }
            if (CollectionUtils.isNotEmpty(buildJobIds)) {
                String placeholderParam = String.format("s%d_build_jobs", currentOffset);
                cicdBuildJobCritereas.add(String.format("jr.cicd_job_id in (:%s)", placeholderParam));
                params.put(placeholderParam, parseUuidList(event.getValues()));
            }
            cicdCritereas.add("jrp.name = :marker_name");
            params.put("marker_name", deployJobMarker);
            String cicdCritereasString = String.join(" AND ", cicdCritereas);
            String cicdBuildJobCriteriaStr = String.join(" AND ", cicdBuildJobCritereas);

            joins.append(String.format("                   left join ( " +
                    " select build_run.id, deploy_run.end_time from " +
                    " ( select jr.id, jr.end_time, " +
                    "jrp.value from ${company}.cicd_job_runs as jr " +
                    " join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where %s ) deploy_run " +
                    " inner join " +
                    " ( select jr.id, jr.end_time, jrp.value from ${company}.cicd_job_runs as jr " +
                    " join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id where %s ) build_run " +
                    " on deploy_run.value = build_run.id" +
                    " ) as s%d on s%d.id = csm.cicd_job_run_id \n", cicdCritereasString, cicdBuildJobCriteriaStr, currentOffset, currentOffset));
            innerSelects.add(String.format("s%s.end_time AS e%d", currentOffset, currentOffset));
        } else if (eventType == VelocityConfigDTO.EventType.CICD_JOB_RUN || eventType == VelocityConfigDTO.EventType.HARNESSCI_JOB_RUN
                || eventType == VelocityConfigDTO.EventType.HARNESSCD_JOB_RUN || eventType == VelocityConfigDTO.EventType.GITHUB_ACTIONS_JOB_RUN) {
            lastCicdEventStageOffset.set(currentOffset);
            List<String> cicdCritereas = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(event.getValues())) {
                String placeholderParam = String.format("s%d_value", currentOffset);
                cicdCritereas.add(String.format("jr.cicd_job_id in (:%s)", placeholderParam));
                params.put(placeholderParam, parseUuidList(event.getValues()));
            }
            boolean isHarnessCI = eventType == VelocityConfigDTO.EventType.HARNESSCI_JOB_RUN;
            boolean isHarnessCD = eventType == VelocityConfigDTO.EventType.HARNESSCD_JOB_RUN;
            DefaultListRequest request = DefaultListRequest.builder().filter(stage.getFilter() != null ? stage.getFilter() : new HashMap<>()).build();
            try {
                CiCdJobRunsFilter filter = CiCdJobRunsFilter.parseCiCdJobRunsFilter(request, MAPPER);
                if(isHarnessCI){
                    filter = filter.toBuilder().isCiJob(true).build();
                }
                else if(isHarnessCD) {
                    filter = filter.toBuilder().isCdJob(true).build();
                }
                Map<String, List<String>> conditions = createWhereClauseAndUpdateParamsJobRuns("${company}", filter, params, "_" + lastCicdEventStageOffset.get(), false, null);
                if (conditions.get(CICD_CONDITIONS).size() > 0) {
                    cicdCritereas.addAll(conditions.get(CICD_CONDITIONS));
                }
            } catch (BadRequestException e) {
                log.error("Unable to parse the filter ", e);
            }
            if (MapUtils.isNotEmpty(event.getParams())) {
                List<String> parametersCriterea = new ArrayList<>();
                int i = 0;
                for (Map.Entry<String, List<String>> entry : event.getParams().entrySet()) {
                    String nameKey = "s" + currentOffset + "_name_" + i;
                    String valueKey = "s" + currentOffset + "_value_" + i;
                    parametersCriterea.add("(jrp.name = :" + nameKey + " AND jrp.value IN (:" + valueKey + "))");
                    params.put(nameKey, entry.getKey());
                    params.put(valueKey, entry.getValue());
                    i++;
                }
                cicdCritereas.add("( " + String.join(" OR ", parametersCriterea) + " )");
            }
            String cicdCritereasString = cicdCritereas.isEmpty() ? "" : "where " + String.join(" AND ", cicdCritereas) + " ";
            if (isHarnessCD) {
                joins.append(String.format("               left join ( " +
                        "select jr.id,jr.end_time,jram.cicd_job_run_id1 as ci_job_run_id from ${company}.cicd_job_runs as jr " +
                        (MapUtils.isNotEmpty(event.getParams()) ? "join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id " : StringUtils.EMPTY) +
                        "join ${company}.cicd_job_run_artifact_mappings as jram on jram.cicd_job_run_id2 = jr.id " +
                        cicdCritereasString +
                        ") as s%d on s%d.ci_job_run_id = s%d.id \n", currentOffset, currentOffset, (lastCicdEventStageOffset.get() - 1)));
            } else if(isHarnessCI) {
                joins.append(String.format("               left join ( " +
                        "select jr.id,jr.end_time from ${company}.cicd_job_runs as jr " +
                        (MapUtils.isNotEmpty(event.getParams()) ? "join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id " : StringUtils.EMPTY) +
                        cicdCritereasString +
                        ") as s%d on s%d.id = csm.cicd_job_run_id \n", currentOffset, currentOffset));
            } else {
                joins.append(String.format("               left join ( " +
                        "select jr.id,jr.end_time from ${company}.cicd_job_runs as jr " +
                        "join ${company}.cicd_jobs as j on j.id = jr.cicd_job_id " +
                        "join ${company}.cicd_instances as i on i.id = j.cicd_instance_id " +
                        (MapUtils.isNotEmpty(event.getParams()) ? "join ${company}.cicd_job_run_params as jrp on jrp.cicd_job_run_id = jr.id " : StringUtils.EMPTY) +
                        cicdCritereasString +
                        ") as s%d on s%d.id = csm.cicd_job_run_id \n", currentOffset, currentOffset));
            }

            innerSelects.add(String.format("s%s.end_time AS e%d", currentOffset, currentOffset));
        }
        baseSelects.add(String.format("%s(e%d) as e%d", aggregateFunction, currentOffset, currentOffset));

        int prevOffset = currentOffset - 1;
        if (currentOffset == 0) {
            reduceSelects.add(String.format("ABS(EXTRACT(EPOCH FROM (e%d - estart))) as s%d", currentOffset, currentOffset));
        } else {
            reduceSelects.add(String.format("EXTRACT(EPOCH FROM (e%d - %s)) as s%d", currentOffset, generateCoalescedValuesForPrevOffset(prevOffset), currentOffset));
        }
        /*
        Per, LEV-3271, the option limit_to_only_applicable_data, applies only to aggs, for values we need to always return null for not applicable stages.
        Across values, limit_to_only_applicable_data = true -> enrich phase treat not applicable as null
        Across values, limit_to_only_applicable_data = false -> enrich phase treat not applicable as null
        Across NOT values, limit_to_only_applicable_data = true -> enrich phase treat not applicable as null
        Across NOT values, limit_to_only_applicable_data = false -> enrich phase treat not applicable as 0
         */
        if ((velocityFilter.getAcross() == VelocityFilter.DISTINCT.values) || (velocityFilter.getAcross() == VelocityFilter.DISTINCT.histogram) || (velocityFilter.getAcross() == VelocityFilter.DISTINCT.rating) || (Boolean.TRUE.equals(velocityFilter.getLimitToOnlyApplicableData()))) {
            //use only tickets/prs relevant for that stage i.e. convert only negatives to 0, keep null as-is to exclude from avg
            enrichSelects.add(String.format("greatest((s%d + 0) - 0, (s%d + 0) - s%d) as s%d", currentOffset, currentOffset, currentOffset, currentOffset));
        } else {
            //default use all tickets/prs i.e. convert all nulls & negatives to 0
            enrichSelects.add(String.format("greatest(s%d,0) as s%d", currentOffset, currentOffset, currentOffset, currentOffset));
        }

        medianSelects.add(String.format("PERCENTILE_CONT(0.5) WITHIN GROUP(ORDER BY s%d) as s%d_median", currentOffset, currentOffset));
        medianSelects.add(String.format("PERCENTILE_CONT(0.9) WITHIN GROUP(ORDER BY s%d) as s%d_p90", currentOffset, currentOffset));
        medianSelects.add(String.format("PERCENTILE_CONT(0.95) WITHIN GROUP(ORDER BY s%d) as s%d_p95", currentOffset, currentOffset));
        medianSelects.add(String.format("AVG(s%d) as s%d_mean", currentOffset, currentOffset));
        medianSelects.add(String.format("count(s%d) as s%d_count", currentOffset, currentOffset));
        offsetStageMap.put(currentOffset, stage);
    }

    public Map<String, List<String>> createWhereClauseAndUpdateParamsJobRuns(String company, CiCdJobRunsFilter filter,
                                                                             Map<String, Object> params, String suffix, Boolean isList,
                                                                             OUConfiguration ouConfig) {
        List<String> criterias = new ArrayList<>();
        List<String> triageCriterias = new ArrayList<>();
        String paramSuffix = suffix == null ? "" : "_" + suffix;
        if (CollectionUtils.isNotEmpty(filter.getIntegrationIds())) {
            criterias.add("i.integration_id IN (:integration_ids" + paramSuffix + ")");
            params.put("integration_ids" + paramSuffix, filter.getIntegrationIds().stream()
                    .map(NumberUtils::toInteger).collect(Collectors.toList()));
        }
        if (CollectionUtils.isNotEmpty(filter.getCicdUserIds()) || OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)) { // OU: user
            var columnName = "jr.cicd_user_id" + paramSuffix;
            var columnNameParam = columnName + paramSuffix;
            if(OrgUnitHelper.doesOuConfigHaveCiCdUsers(ouConfig)){
                var usersSelect = OrgUnitHelper.getSelectForCloudIdsByOuConfig(company, ouConfig, params,
                        IntegrationType.getCICDIntegrationTypes());
                if (StringUtils.isNotBlank(usersSelect)) {
                    criterias.add(MessageFormat.format("{0} IN (SELECT cloud_id FROM ({1}) l)", "jr.cicd_user_id", usersSelect));
                }
            }
            else if(CollectionUtils.isNotEmpty(filter.getCicdUserIds())){
                TeamUtils.addUsersCondition(company, criterias, params, "jr.cicd_user_id", columnNameParam, false, filter.getCicdUserIds(), CICD_APPLICATIONS);
            }
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeCiCdUserIds())) {
            String columnNameParam = "jr.cicd_user_id" + paramSuffix;
            TeamUtils.addUsersCondition(company, criterias, params, "jr.cicd_user_id",
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
                criterias.add("jr.start_time >= TO_TIMESTAMP(:start_time_start" + paramSuffix + ")");
                params.put("start_time_start" + paramSuffix, startTimeRange.getLeft());
            }
            if (startTimeRange.getRight() != null) {
                criterias.add("jr.start_time <= TO_TIMESTAMP(:start_time_end" + paramSuffix + ")");
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
            criterias.add("jr.status IN (:job_statuses" + paramSuffix + ")");
            params.put("job_statuses" + paramSuffix, filter.getJobStatuses());
        }
        if (CollectionUtils.isNotEmpty(filter.getExcludeJobStatuses())) {
            criterias.add("jr.status NOT IN (:excl_job_statuses" + paramSuffix + ")");
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
            criterias.add("(jr.cd = :is_cd" + paramSuffix + " OR r.ci = :is_ci" + paramSuffix + ")");
            params.put("is_cd" + paramSuffix, filter.getIsCdJob());
            params.put("is_ci" + paramSuffix, filter.getIsCiJob());
        } else if (filter.getIsCiJob() != null){
            criterias.add("jr.ci = :is_ci" + paramSuffix);
            params.put("is_ci" + paramSuffix, filter.getIsCiJob());
        } else if (filter.getIsCdJob() != null) {
            criterias.add("jr.cd = :is_cd" + paramSuffix);
            params.put("is_cd" + paramSuffix, filter.getIsCdJob());
        }
        if (filter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = filter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                criterias.add("jr.end_time >= to_timestamp(:end_time_start" + paramSuffix + ")");
                params.put("end_time_start" + paramSuffix, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                criterias.add("jr.end_time <= to_timestamp(:end_time_end" + paramSuffix + ")");
                params.put("end_time_end" + paramSuffix, endTimeRange.getRight());
            }
        }
        int acrossCount = (filter.getAcrossCount() != null) ? filter.getAcrossCount() : 90;
        if (isList && (CiCdJobRunsFilter.DISTINCT.trend == filter.getAcross() ||
                CollectionUtils.isNotEmpty(filter.getStacks())
                        && filter.getStacks().get(0).equals(CiCdJobRunsFilter.DISTINCT.trend))
                && filter.getEndTimeRange() !=  null && filter.getStartTimeRange() != null) {
            criterias.add("start_time >= to_timestamp(:start_time_from" + paramSuffix + ")");
            params.put("start_time_from" + paramSuffix, Instant.now().minus(acrossCount, ChronoUnit.DAYS).getEpochSecond());
        }
        if (CiCdJobRunsFilter.DISTINCT.instance_name == filter.getAcross()) {
            criterias.add("i.name IS NOT NULL");
        }
        if (CiCdJobRunsFilter.DISTINCT.job_normalized_full_name == filter.getAcross()) {
            criterias.add("j.job_normalized_full_name IS NOT NULL");
        }
        if(CiCdJobRunsFilter.DISTINCT.triage_rule == filter.getAcross() ||
                (CollectionUtils.isNotEmpty(filter.getStacks()) && filter.getStacks().contains(CiCdJobRunsFilter.DISTINCT.triage_rule))){
            criterias.add("triage_rule IS NOT NULL");
        }

        ciCdMetadataConditionBuilder.prepareMetadataConditions(filter, params, lastCicdEventStageOffset.get() + "_", criterias);

        ciCdPartialMatchConditionBuilder.preparePartialMatchConditions(filter, params, criterias, paramSuffix, CICD_PARTIAL_MATCH_COLUMNS);

        return Map.of(CICD_CONDITIONS, criterias,
                TRIAGE_CONDITIONS, triageCriterias);
    }

    private void updatePRCondition(String condition, boolean updatePRJoinCondition) {
        if (updatePRJoinCondition) {
            params.merge(PR_JOIN_CONDITION, condition, (a, b) -> a + " AND " + b);
        } else {
            whereConditions.add(condition);
        }
    }

    private void processStages(final List<VelocityConfigDTO.Stage> stages, final VelocityFilter velocityFilter, final WorkItemsType workItemsType) {
        if (CollectionUtils.isEmpty(stages)) {
            return;
        }
        final List<VelocityConfigDTO.Stage> sortedStages = new ArrayList<>(stages);
        Collections.sort(sortedStages, (a, b) -> a.getOrder() - b.getOrder());
        for (VelocityConfigDTO.Stage stage : stages) {
            processStage(stage, velocityFilter, workItemsType);
        }
    }

    private void processTotalLeadTimeForEachTicketOrPr() {
        /*
        In enrich selects we have so,s1,s2.. we also need (s0+s1+s2...) as t which is the total lead time for that Ticket or PR
        This total will be used for sorting & pagination
         */
        if (offset.get() == 0) {
            log.warn("offset is 0, enrich total is not needed");
            return;
        }
        List<String> enrichedCalculations = new ArrayList<>();
        for (int i = 0; i < offset.get(); i++) {
            enrichedCalculations.add(String.format("greatest(s%d,0)", i));
        }
        String enrichTotal = "(" + String.join("+", enrichedCalculations) + ") as t";
        enrichSelects.add(enrichTotal);
    }

    private boolean isIssueStatusJoinNeeded(VelocityConfigDTO velocityConfigDTO) {
        Optional<VelocityConfigDTO.Stage> ticketStatusStage = Stream.of(CollectionUtils.emptyIfNull(velocityConfigDTO.getPreDevelopmentCustomStages()).stream(),
                        CollectionUtils.emptyIfNull(velocityConfigDTO.getFixedStages()).stream(),
                        CollectionUtils.emptyIfNull(velocityConfigDTO.getPostDevelopmentCustomStages()).stream())
                .flatMap(stream -> stream.map(v -> v))
                .filter(s -> Objects.nonNull(s.getEvent()))
                .filter(s -> s.getEvent().getType() == VelocityConfigDTO.EventType.JIRA_STATUS || s.getEvent().getType() == VelocityConfigDTO.EventType.WORKITEM_STATUS)
                .findFirst();
        return ticketStatusStage.isPresent();
    }

    private void parseCalculation(VelocityConfigDTO velocityConfigDTO, VelocityFilter velocityFilter, final WorkItemsType workItemsType, final boolean sprintJoinNeeded, final String sprintJoinWhere) throws BadRequestException {
        VelocityFilter.DISTINCT across = velocityFilter.getAcross();
        VelocityFilter.CALCULATION calculation = velocityFilter.getCalculation();
        boolean addPrLink = VelocityFilter.CALCULATION.pr_velocity.equals(calculation);
        switch (calculation) {
            case ticket_velocity:
                innerSelects.addAll(List.of("im.id as u_id", "im.integration_id as integration_id"));
                if (workItemsType == WorkItemsType.WORK_ITEM) {
                    innerSelects.addAll(List.of("im.workitem_id as key", "im.summary as title", "im.attributes ->> 'organization' as org", "im.project as project", "NULL as repo_id"));
                    if (BooleanUtils.isTrue(velocityConfigDTO.getStartingEventIsGenericEvent())) {
                        innerSelects.add("COALESCE(ge.event_time, im.workitem_created_at) as estart");
                    } else {
                        innerSelects.add("im.workitem_created_at as estart");
                    }
                    joins.append("${company}.issue_mgmt_workitems as im \n")
                            .append("               left join (select m.workitem_id, c.id, c.committed_at, c.integration_id, c.commit_sha, c.repo_id, c.committer_id, c.author_id from ${company}.scm_commit_workitem_mappings m \n" +
                                    "               inner join ${company}.scm_commits c on c.commit_sha = m.commit_sha and  c.integration_id = m.scm_integration_id \n" +
                                    "               and c.integration_id IN (:integration_ids) ) AS c on c.workitem_id = im.workitem_id \n");
                    //Add ingested_at filter only for calculation ticket_velocity for non trend
                    if ((across == VelocityFilter.DISTINCT.velocity) || (((across == VelocityFilter.DISTINCT.values) || (across == VelocityFilter.DISTINCT.histogram) || (across == VelocityFilter.DISTINCT.rating)) && (CollectionUtils.isEmpty(velocityFilter.getValueTrendKeys())))) {
                        joins.append("               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n");
                    }
                    if (BooleanUtils.isTrue(velocityConfigDTO.getStartingEventIsGenericEvent())) {
                        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getStartingGenericEventTypes())) {
                            joins.append("               left join ${company}.generic_events as ge on ge.component = 'work_item' and ge.key = im.workitem_id and ge.event_type in (:starting_generic_event_types) \n");
                            params.put("starting_generic_event_types", velocityConfigDTO.getStartingGenericEventTypes());
                        } else {
                            joins.append("               left join ${company}.generic_events as ge on ge.component = 'work_item' and ge.key = im.workitem_id \n");
                        }
                    }

                    if (sprintJoinNeeded) {
                        joins.append("    INNER JOIN(SELECT timelines.integration_id as timeline_integration_id , timelines.workitem_id as timeline_workitem_id , milestone_field_value , milestone_parent_field_value , milestone_name  , milestone_integration_id  , latest_ingested_at FROM ${company}.issue_mgmt_workitems_timeline timelines  \n" +
                                "   INNER JOIN ${company}.integration_tracker it ON timelines.integration_id = it.integration_id\n" +
                                "   INNER JOIN (SELECT field_value as milestone_field_value , parent_field_value as milestone_parent_field_value , name as milestone_name , integration_id as milestone_integration_id  FROM ${company}.issue_mgmt_milestones " + sprintJoinWhere + ") as milestones ON milestones.milestone_parent_field_value || '\\' || milestones.milestone_name = timelines.field_value AND milestones.milestone_integration_id = timelines.integration_id \n" +
                                "   AND timelines.start_date <= to_timestamp(latest_ingested_at) AND timelines.end_date >= to_timestamp(latest_ingested_at)) smj  \n" +
                                "                    ON         im.integration_id = smj.timeline_integration_id\n" +
                                "                    AND        im.workitem_id = smj.timeline_workitem_id \n");
                    }


                    joins.append("               left join ${company}.scm_pullrequests_workitem_mappings as prjm on prjm.workitem_id = im.workitem_id and prjm.project = im.project \n");

                    //The params integration_ids are already put by the createPrWhereClauseAndUpdateParams
                    whereConditions.add("im.integration_id IN (:integration_ids)");
                } else {
                    innerSelects.addAll(List.of("im.key as key", "im.summary as title", "NULL as org", "NULL as project", "NULL as repo_id"));
                    if (BooleanUtils.isTrue(velocityConfigDTO.getStartingEventIsGenericEvent())) {
                        innerSelects.add("COALESCE(ge.event_time, to_timestamp(im.issue_created_at)) as estart");
                    } else {
                        innerSelects.add("to_timestamp(im.issue_created_at) as estart");
                    }
                    joins.append("${company}.jira_issues as im \n")
                            .append("               left join ${company}.scm_commit_jira_mappings as m on m.issue_key = im.key \n")
                            .append("               left join ${company}.scm_commits as c on c.commit_sha = m.commit_sha AND c.integration_id = m.scm_integ_id \n");
                    //Add jira_ingested_at filter only for calculation ticket_velocity for non trend
                    if ((across == VelocityFilter.DISTINCT.velocity) || (((across == VelocityFilter.DISTINCT.values) || (across == VelocityFilter.DISTINCT.histogram) || (across == VelocityFilter.DISTINCT.rating)) && (CollectionUtils.isEmpty(velocityFilter.getValueTrendKeys())))) {
                        joins.append("               join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at \n");
                    }
                    if (BooleanUtils.isTrue(velocityConfigDTO.getStartingEventIsGenericEvent())) {
                        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getStartingGenericEventTypes())) {
                            joins.append("               left join ${company}.generic_events as ge on ge.component = 'jira' and ge.key = im.key and ge.event_type in (:starting_generic_event_types) \n");
                            params.put("starting_generic_event_types", velocityConfigDTO.getStartingGenericEventTypes());
                        } else {
                            joins.append("               left join ${company}.generic_events as ge on ge.component = 'jira' and ge.key = im.key \n");
                        }
                    }
                    if (sprintJoinNeeded) {
                        joins.append("               INNER JOIN (select integration_id, sprint_id FROM ${company}.jira_issue_sprints " + sprintJoinWhere + " ) as sprints ON sprints.integration_id=im.integration_id AND sprints.sprint_id=ANY(im.sprint_ids) \n");
                    }

                    joins.append("               left join ${company}.scm_pullrequests_jira_mappings as prjm on prjm.issue_key = im.key \n");

                    if (workItemsType != WorkItemsType.NONE) {
                        //The params integration_ids & jira_integration_ids are already put by the createPrWhereClauseAndUpdateParams
                        whereConditions.add("im.integration_id IN (:jira_integration_ids)");
                    }
                }
                joins.append("               left join ${company}.scm_commit_pullrequest_mappings as cprm on cprm.scm_commit_id = c.id \n");
                joins.append("               left join ${company}.scm_pullrequests as pr on ${PR_JOIN_CONDITION} \n");
                params.put(PR_JOIN_CONDITION,
                        "((pr.id = cprm.scm_pullrequest_id) OR (pr.id=prjm.pr_uuid))");
                baseSelects.addAll(List.of("u_id", "integration_id", "key", "title", "org", "project", "repo_id", "MIN(estart) as estart"));
                //The params integration_ids is already put by the createPrWhereClauseAndUpdateParams
                whereConditions.add("( (pr.integration_id IN (:integration_ids)) OR (pr.integration_id IS NULL))");
                whereConditions.add("( (c.integration_id IN (:integration_ids)) OR (c.integration_id IS NULL))");
                break;

            case pr_velocity:
                innerSelects.addAll(List.of("pr.id AS u_id", "pr.integration_id as integration_id", "pr.number AS key", "pr.title AS title", "NULL as org", "pr.project as project", "pr.repo_id AS repo_id", "pr.metadata->>'pr_link' AS pr_link"));
                if (BooleanUtils.isTrue(velocityConfigDTO.getStartingEventIsCommitCreated())) {
                    innerSelects.add("c.committed_at as estart");
                } else {
                    if (workItemsType == WorkItemsType.WORK_ITEM) {
                        innerSelects.add("im.workitem_created_at as estart");
                    } else {
                        innerSelects.add("to_timestamp(im.issue_created_at) as estart");
                    }
                }

                //The params integration_ids is already put by the createPrWhereClauseAndUpdateParams
                whereConditions.add("pr.integration_id IN (:integration_ids)");

                joins.append("${company}.scm_pullrequests AS pr \n")
                        .append("               left join ${company}.scm_commit_pullrequest_mappings AS cprm ON cprm.scm_pullrequest_id = pr.id \n")
                        .append("               left join ${company}.scm_commits AS c ON c.id = cprm.scm_commit_id \n");

                if (workItemsType == WorkItemsType.WORK_ITEM) {
                    if (!disablePrJiraCorrelationForPrVelocity) {
                        joins.append("               left join ${company}.scm_pullrequests_workitem_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n");
                    }
                    joins.append("               left join ${company}.scm_commit_workitem_mappings AS m ON m.commit_sha = c.commit_sha AND m.scm_integration_id = c.integration_id \n");
                    joins.append("               left join ( select im.* from ${company}.issue_mgmt_workitems AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im");
                    if (disablePrJiraCorrelationForPrVelocity) {
                        joins.append(" ON (im.workitem_id = m.workitem_id) \n");
                    } else {
                        joins.append(" ON (im.workitem_id = m.workitem_id) OR (prjm.workitem_id = im.workitem_id) \n");
                    }

                    //The params integration_ids is already put by the createPrWhereClauseAndUpdateParams
                    whereConditions.add("( (im.integration_id IN (:integration_ids)) OR (im.integration_id IS NULL))");
                } else {
                    if (!disablePrJiraCorrelationForPrVelocity) {
                        joins.append("               left join ${company}.scm_pullrequests_jira_mappings AS prjm ON (pr.id=prjm.pr_uuid) \n");
                    }
                    joins.append("               left join ${company}.scm_commit_jira_mappings AS m ON m.commit_sha = c.commit_sha \n");
                    joins.append("               left join ( select im.* from ${company}.jira_issues AS im join ${company}.integration_tracker as it on it.integration_id = im.integration_id and it.latest_ingested_at = im.ingested_at ) as im");
                    if (disablePrJiraCorrelationForPrVelocity) {
                        joins.append(" ON (im.KEY = m.issue_key) \n");
                    } else {
                        joins.append(" ON (im.KEY = m.issue_key) OR (prjm.issue_key = im.key) \n");
                    }

                    if (workItemsType != WorkItemsType.NONE) {
                        //If workItemsType == WorkItemsType.NONE, then jira filter is null, so jira_integration_ids has not been put here
                        //ToDo: VA - Add unit test for this case to prevent regression
                        //The params jira_integration_ids are already put by the createPrWhereClauseAndUpdateParams
                        whereConditions.add("( (im.integration_id IN (:jira_integration_ids)) OR (im.integration_id IS NULL))");
                    }
                }
                baseSelects.addAll(List.of("u_id", "integration_id", "key", "title", "org", "project", "repo_id", "pr_link", "MIN(estart) as estart"));
                break;
            default:
                throw new BadRequestException(String.format("Calculation %s is not supported!", velocityFilter.getCalculation()));
        }

        baseGroupBys.addAll(List.of("u_id", "integration_id", "key", "title", "org", "project", "repo_id" ));
        reduceSelects.addAll(List.of("u_id", "integration_id", "key", "title", "org", "project", "repo_id"));
        enrichSelects.addAll(List.of("u_id", "integration_id", "key", "title", "org", "project", "repo_id"));

        if (addPrLink) {
            baseGroupBys.add("pr_link");
            reduceSelects.add("pr_link");
            enrichSelects.add("pr_link");
        }
        joins.append("               left join ${company}.scm_pullrequest_reviews as prr on prr.pr_id = pr.id \n")
                .append("               left join ${company}.cicd_scm_mapping as csm on csm.commit_id = c.id \n");
        boolean isIssueStatusJoinNeeded = isIssueStatusJoinNeeded(velocityConfigDTO);
        if (isIssueStatusJoinNeeded) {
            if (workItemsType == WorkItemsType.WORK_ITEM) {
                joins.append("               left join ${company}.issue_mgmt_workitems_timeline as ims on ims.integration_id = im.integration_id AND ims.workitem_id=im.workitem_id \n");
            } else {
                joins.append("               left join ${company}.jira_issue_statuses as ims on ims.integration_id = im.integration_id AND ims.issue_key=im.key \n");
            }
        }
    }

    private void parseAcross(VelocityFilter velocityFilter, final WorkItemsType workItemsType) throws BadRequestException {
        VelocityFilter.DISTINCT across = velocityFilter.getAcross();
        VelocityFilter.CALCULATION calculation = velocityFilter.getCalculation();

        switch (across) {
            case trend:
                switch (calculation) {
                    case ticket_velocity:
                        innerSelects.add("im.ingested_at as trend");
                        break;
                    case pr_velocity:
                        innerSelects.add("EXTRACT(EPOCH FROM (pr.pr_created_at::date)) as trend");
                        break;
                    default:
                        throw new BadRequestException(String.format("For Across %s Calculation %s is not supported!", across, calculation));
                }
                baseSelects.add("trend");
                baseGroupBys.add("trend");

                reduceSelects.add("trend");
                enrichSelects.add("trend");

                medianSelects.add("trend");
                medianGroupBys.add("trend");
                medianOrderBys.add("trend");
                break;
            case velocity:
                break;
            case values:
            case histogram:
            case rating:
                if (CollectionUtils.isNotEmpty(velocityFilter.getValueTrendKeys())) {
                    switch (calculation) {
                        case ticket_velocity:
                            whereConditions.add("im.ingested_at IN (:value_trend_keys)");
                            break;
                        case pr_velocity:
                            whereConditions.add("EXTRACT(EPOCH FROM (pr.pr_created_at::date)) IN (:value_trend_keys)");
                            break;
                        default:
                            throw new BadRequestException(String.format("For Across %s Calculation %s is not supported!", across, calculation));
                    }
                    params.put("value_trend_keys", velocityFilter.getValueTrendKeys());
                }
                if (across == VelocityFilter.DISTINCT.values) {
                    enrichLimitOffset.append(String.format("LIMIT %s OFFSET %s ", velocityFilter.getPageSize(), (velocityFilter.getPage() * velocityFilter.getPageSize())));
                }
                break;
            default:
                throw new BadRequestException(String.format("Across %s is not supported!", across));
        }
    }

    private void parseStackValues(VelocityFilter velocityFilter, final WorkItemsType workItemsType) throws BadRequestException {
        if (!velocityFilter.getAcross().isSupportsStackValues()) {
            log.info("Across %s does not support Stack values!", velocityFilter.getAcross());
            return;
        }
        List<String> valueStacks = velocityFilter.getValueStacks();
        if (CollectionUtils.isEmpty(valueStacks)) {
            log.info("Across is %s and value stack is null or empty!", velocityFilter.getAcross());
            return;
        }
        if (CollectionUtils.isEmpty(velocityFilter.getStacks())) {
            log.info("Across is %s, value stack is present and stacks is null or empty!", velocityFilter.getAcross());
            return;
        }
        VelocityFilter.STACK stack = velocityFilter.getStacks().get(0);
        switch (stack) {
            case issue_type:
                if (workItemsType == WorkItemsType.WORK_ITEM) {
                    whereConditions.add("im.workitem_type IN (:value_stacks)");
                } else {
                    whereConditions.add("im.issue_type IN (:value_stacks)");
                }
                break;
            case issue_priority:
                whereConditions.add("im.priority IN (:value_stacks)");
                break;
            case issue_component:
                whereConditions.add("im.components && :value_stacks");
                break;
            case issue_project:
                whereConditions.add("im.project IN (:value_stacks)");
                break;
            case issue_label:
                whereConditions.add("im.labels && :value_stacks");
                break;
            case issue_epic:
                whereConditions.add("im.epic IN (:value_stacks)");
                break;
            default:
                throw new BadRequestException(String.format("Stack %s is not supported for stack values!", stack));
        }
        params.put("value_stacks", valueStacks);
    }

    private void parseStack(VelocityFilter.DISTINCT across, VelocityFilter.STACK stack, final WorkItemsType workItemsType) throws BadRequestException {
        if (!stack.getSupportedAcross().contains(across)) {
            //log
            return;
        }
        switch (stack) {
            case issue_type:
                if (workItemsType == WorkItemsType.WORK_ITEM) {
                    innerSelects.add("im.workitem_type as stack");
                } else {
                    innerSelects.add("im.issue_type as stack");
                }
                break;
            case issue_priority:
                innerSelects.add("im.priority as stack");
                break;
            case issue_component:
                innerSelects.add("UNNEST(im.components) as stack");
                break;
            case issue_project:
                innerSelects.add("im.project as stack");
                break;
            case issue_label:
                innerSelects.add("UNNEST(im.labels) as stack");
                break;
            case issue_epic:
                innerSelects.add("im.epic as stack");
                break;
            default:
                throw new BadRequestException(String.format("Stack %s is not supported!", stack));
        }
        baseSelects.add("stack");
        baseGroupBys.add("stack");

        reduceSelects.add("stack");
        enrichSelects.add("stack");

        medianSelects.add("stack");
        medianGroupBys.add("stack");
    }

    private void parseStacks(VelocityFilter velocityFilter, final WorkItemsType workItemsType) throws BadRequestException {
        if (CollectionUtils.isEmpty(velocityFilter.getStacks())) {
            return;
        }
        for (VelocityFilter.STACK stack : velocityFilter.getStacks()) {
            parseStack(velocityFilter.getAcross(), stack, workItemsType);
        }
    }

    private void validateVelocityFilter(VelocityFilter velocityFilter) throws BadRequestException {
        if ((CollectionUtils.isNotEmpty(velocityFilter.getStacks())) && (!velocityFilter.getAcross().isSupportsStacks()) && (!velocityFilter.getAcross().isSupportsStackValues())) {
            throw new BadRequestException(String.format("Across %s does not support stacks or stack values and stacks was specified!", velocityFilter.getAcross()));
        }
        if (CollectionUtils.isNotEmpty(velocityFilter.getStacks())) {
            for (VelocityFilter.STACK stack : velocityFilter.getStacks()) {
                if (CollectionUtils.isNotEmpty(velocityFilter.getValueStacks())) {
                    if (!stack.getSupportedAcrossForValues().contains(velocityFilter.getAcross())) {
                        throw new BadRequestException(String.format("Stack %s does not support across %s", stack, velocityFilter.getAcross()));
                    }
                } else {
                    if (!stack.getSupportedAcross().contains(velocityFilter.getAcross())) {
                        throw new BadRequestException(String.format("Stack %s does not support across %s", stack, velocityFilter.getAcross()));
                    }
                }
            }
        }
    }

    private void processCiCdJobRunFilter(VelocityConfigDTO velocityConfigDTO, CiCdJobRunsFilter ciCdJobRunsFilter) {
        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            stages.addAll(velocityConfigDTO.getPreDevelopmentCustomStages());
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            stages.addAll(velocityConfigDTO.getPostDevelopmentCustomStages());
        }
        List<String> cicdEventStageNames = stages.stream()
                .filter(s -> s.getEvent().getType() == VelocityConfigDTO.EventType.CICD_JOB_RUN)
                .map(VelocityConfigDTO.Stage::getName)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(cicdEventStageNames)) {
            return;
        }
        if ((lastCicdEventStageOffset == null) || (lastCicdEventStageOffset.get() == -1)) {
            return;
        }

        if (ciCdJobRunsFilter.getEndTimeRange() != null) {
            ImmutablePair<Long, Long> endTimeRange = ciCdJobRunsFilter.getEndTimeRange();
            if (endTimeRange.getLeft() != null) {
                String valueKey = "s" + lastCicdEventStageOffset.get() + "_job_run_end_start";
                whereConditions.add(String.format("s%d.end_time > to_timestamp(:%s)", lastCicdEventStageOffset.get(), valueKey));
                params.put(valueKey, endTimeRange.getLeft());
            }
            if (endTimeRange.getRight() != null) {
                String valueKey = "s" + lastCicdEventStageOffset.get() + "_job_run_end_end";
                whereConditions.add(String.format("s%d.end_time < to_timestamp(:%s)", lastCicdEventStageOffset.get(), valueKey));
                params.put(valueKey, endTimeRange.getRight());
            }
        }
    }

    public Query buildQuery(String company,
                            VelocityConfigDTO velocityConfigDTO,
                            VelocityFilter velocityFilter,
                            WorkItemsType workItemsType,
                            JiraIssuesFilter jiraIssuesFilter,
                            WorkItemsFilter workItemsFilter,
                            WorkItemsMilestoneFilter workItemsMilestoneFilter,
                            ScmPrFilter scmPrFilter,
                            ScmCommitFilter scmCommitFilter,
                            CiCdJobRunsFilter ciCdJobRunsFilter,
                            List<DbWorkItemField> workItemCustomFields,
                            OUConfiguration ouConfig) throws BadRequestException {
        validateVelocityFilter(velocityFilter);
        boolean sprintJoinNeeded = false;
        String sprintJoinWhere = "";
        if (workItemsType == WorkItemsType.WORK_ITEM) {
            /*
            List<DbWorkItemField> workItemCustomFields = null;
            if(MapUtils.isNotEmpty(workItemsFilter.getCustomFields())) {
                try {
                    workItemCustomFields = workItemFieldsMetaService.listByFilter(company, workItemsFilter.getIntegrationIds(), true,
                            null, null, null, null, null, 0,
                            1000).getRecords();
                } catch (SQLException e) {
                    log.error("Error while querying workitem field meta table. Reason: " + e.getMessage());
                }
            }
             */
            io.levelops.commons.models.Query.QueryConditions queryConditions =
                    WorkItemQueryCriteria.getSelectionCriteria(company, workItemsFilter, workItemCustomFields, "im.", "im_", ouConfig).getCriteria();
            CollectionUtils.emptyIfNull(queryConditions.getConditions()).stream()
                    .filter(StringUtils::isNotBlank)
                    .filter(c -> !"im.integration_id IN (:im_integs)".equals(c))
                    .filter(c -> !"im.ingested_at = (:im_ingested_at)".equals(c))
                    .forEach(whereConditions::add);
            if (MapUtils.isNotEmpty(queryConditions.getQueryParams())) {
                params.putAll(queryConditions.getQueryParams());
                params.remove("im_integs");
                params.remove("im_ingested_at");
            }

            sprintJoinNeeded = CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getStates()) || CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getFullNames()) || (workItemsMilestoneFilter.getPartialMatch() != null ? (CollectionUtils.isNotEmpty(workItemsMilestoneFilter.getPartialMatch().entrySet()) && workItemsMilestoneFilter.getPartialMatch().get("workitem_sprint_full_names") != null) : false);
            io.levelops.commons.models.Query.QueryConditions sprintQueryConditions = WorkItemMilestoneQueryCriteria.getSelectionCriteria(workItemsMilestoneFilter, "sprint_").getCriteria();
            List<String> sprintJoinWhereList = CollectionUtils.emptyIfNull(sprintQueryConditions.getConditions()).stream()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(sprintJoinWhereList)) {
                sprintJoinWhere = " WHERE " + String.join(" AND ", sprintJoinWhereList);
            }
            params.putAll(sprintQueryConditions.getQueryParams());


        } else {
            Map<String, List<String>> jiraConditions = (jiraIssuesFilter != null) ? jiraConditionsBuilder.createWhereClauseAndUpdateParams(company, params, null, jiraIssuesFilter, null, null, "im.", ouConfig) : Map.of();
            try {
                log.info("jiraConditions = {}", MAPPER.writeValueAsString(jiraConditions));
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException!", e);
            }
            if (jiraConditions.containsKey(ISSUES_TABLE)) {
                CollectionUtils.emptyIfNull(jiraConditions.get(ISSUES_TABLE)).stream()
                        .filter(StringUtils::isNotBlank)
                        .filter(s -> !s.startsWith("im.integration_id"))
                        .filter(s -> !s.startsWith("((im.ingested_at ="))
                        .forEach(whereConditions::add);
            }

            sprintJoinNeeded = jiraIssuesFilter != null ? CollectionUtils.isNotEmpty(jiraIssuesFilter.getSprintStates()) : false;
            List<String> sprintJoinWhereList = CollectionUtils.emptyIfNull(jiraConditions.get(JIRA_ISSUE_SPRINTS)).stream()
                    .filter(StringUtils::isNotBlank)
                    .collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(sprintJoinWhereList)) {
                sprintJoinWhere = " WHERE " + String.join(" AND ", sprintJoinWhereList);
            }
        }

        log.info("whereConditions = {}", whereConditions);
        log.info("params = {}", params);

        Map<String, List<String>> scmPrConditions = scmAggService.createPrWhereClauseAndUpdateParams(company, params, scmPrFilter, null, "pr.", ouConfig);
        updateScmPrFilters(scmPrFilter, scmPrConditions);
        try {
            log.info("scmPrConditions = {}", MAPPER.writeValueAsString(scmPrConditions));
        } catch (JsonProcessingException e) {
            log.error("JsonProcessingException!", e);
        }
        if (scmPrConditions.containsKey(PRS_TABLE)) {
            CollectionUtils.emptyIfNull(scmPrConditions.get(PRS_TABLE)).stream()
                    .filter(StringUtils::isNotBlank)
                    .filter(s -> !s.startsWith("pr.integration_id"))
                    .forEach(whereConditions::add);
        }
        if (scmCommitFilter != null) {
            Map<String, List<String>> scmCommitConditions = scmAggService.createCommitsWhereClauseAndUpdateParams(company, params, scmCommitFilter, null, "c.", false, ouConfig);
            updateScmCommitFilters(scmCommitFilter, scmCommitConditions);
            try {
                log.info("scmCommitConditions = {}", MAPPER.writeValueAsString(scmCommitConditions));
            } catch (JsonProcessingException e) {
                log.error("JsonProcessingException!", e);
            }
            if (scmCommitConditions.containsKey(COMMITS_TABLE)) {
                CollectionUtils.emptyIfNull(scmCommitConditions.get(COMMITS_TABLE)).stream()
                        .filter(StringUtils::isNotBlank)
                        .filter(s -> !s.startsWith("c.integration_id"))
                        .forEach(whereConditions::add);
            }
            log.info("whereConditions = {}", whereConditions);
            log.info("params = {}", params);
        }

        parseCalculation(velocityConfigDTO, velocityFilter, workItemsType, sprintJoinNeeded, sprintJoinWhere);
        parseAcross(velocityFilter, workItemsType);
        parseStacks(velocityFilter, workItemsType);
        parseStackValues(velocityFilter, workItemsType);

        processStages(velocityConfigDTO.getPreDevelopmentCustomStages(), velocityFilter, workItemsType);
        processStages(velocityConfigDTO.getFixedStages(), velocityFilter, workItemsType);
        processStages(velocityConfigDTO.getPostDevelopmentCustomStages(), velocityFilter, workItemsType);
        processTotalLeadTimeForEachTicketOrPr();

        processSorts(velocityFilter);

        processCiCdJobRunFilter(velocityConfigDTO, ciCdJobRunsFilter);
        log.info("whereConditions = {}", whereConditions);
        log.info("params = {}", params);

        String joinsTemplateString = joins.toString();
        HashMap<String, String> joinsSubstituteMap = new HashMap<>();
        joinsSubstituteMap.put("customer", company);
        if (params.containsKey(PR_JOIN_CONDITION)) {
            joinsSubstituteMap.put(PR_JOIN_CONDITION, params.get(PR_JOIN_CONDITION).toString());
            params.remove(PR_JOIN_CONDITION);
        }
        StringSubstitutor joinsSubstitutor = new StringSubstitutor(joinsSubstituteMap);
        String joinsString = joinsSubstitutor.replace(joinsTemplateString);

        String whereConditionsString = String.join(" AND ", whereConditions);
        String whereClause = (StringUtils.isBlank(whereConditionsString)) ? " " : "where " + whereConditionsString;

        String enrichConditionsString = String.join(" AND ", enrichConditions);
        String enrichWhereClause = (StringUtils.isBlank(enrichConditionsString)) ? " " : "where " + enrichConditionsString;

        String enrichSortByString = String.join(",", enrichSortBy);
        String enrichSortByClause = (StringUtils.isBlank(enrichSortByString)) ? " " : "ORDER BY " + enrichSortByString;

        String medianGroupByString = String.join(",", medianGroupBys);
        String medianGroupByClause = (StringUtils.isBlank(medianGroupByString)) ? " " : "group by " + medianGroupByString;

        String medianOrderByString = String.join(",", medianOrderBys);
        String medianOrderByClause = (StringUtils.isBlank(medianOrderByString)) ? " " : "order by " + medianOrderByString;

        String histogramStageOffset = null;
        String stageTimeUpper = null;
        String stageTimeLower = null;
        String histogramBucketsCount = String.valueOf(velocityFilter.getHistogramBucketsCount());
        String ratingConditionsStr = null;

        String sqlTemplateWithoutLimit = "  select ${enrichSelects} from ( \n" +
                "       select ${reduceSelects} from ( \n" +
                "           select ${baseSelects} from ( \n" +
                "               select ${innerSelects} \n" +
                "               from ${joins} \n" +
                "               ${whereClause} \n" +
                "           ) as base \n" +
                "           group by ${baseGroupBys} \n" +
                "       ) as reduce \n" +
                "   ) as enrich \n" +
                "   ${enrichWhereClause} \n" +
                "   ${enrichSortBys} \n";
        String sqlTemplate = sqlTemplateWithoutLimit +
                "   ${enrichLimitOffset} \n";
        if ((velocityFilter.getAcross() == VelocityFilter.DISTINCT.velocity) || (velocityFilter.getAcross() == VelocityFilter.DISTINCT.trend)) {
            sqlTemplate = "select ${medianSelects} from ( \n" +
                    sqlTemplate +
                    ") as median \n" +
                    "${medianGroupBys} \n" +
                    "${medianOrderBys} \n";
        } else if (velocityFilter.getAcross() == VelocityFilter.DISTINCT.histogram) {
            sqlTemplate = "SELECT MIN(s${histogramStageOffset}) AS min, MAX(s${histogramStageOffset}) AS max, WIDTH_BUCKET(s${histogramStageOffset}, 0, ${stageTimeUpper}, ${histogramBucketsCount}) AS bucket, COUNT(*) AS cnt FROM (\n" +
                    sqlTemplate +
                    ") as histo\n" +
                    "GROUP BY bucket\n" +
                    "ORDER BY bucket;";
            Integer histogramStageOffsetInt = parseHistogramStageName(velocityFilter, velocityConfigDTO.getName());
            histogramStageOffset = String.valueOf(histogramStageOffsetInt);
            VelocityConfigDTO.Stage stage = offsetStageMap.getOrDefault(histogramStageOffsetInt, null);
            if (stage != null) {
                stageTimeUpper = String.valueOf(stage.getUpperLimitUnit().toSeconds(stage.getUpperLimitValue()) + 1);
            }
        } else if (velocityFilter.getAcross() == VelocityFilter.DISTINCT.rating) {
            sqlTemplate = "SELECT case WHEN s${histogramStageOffset} IS NULL OR s${histogramStageOffset} = 0 THEN 'missing' WHEN s${histogramStageOffset} > 0 AND s${histogramStageOffset} <= ${stageTimeLower} THEN 'good' WHEN s${histogramStageOffset} > ${stageTimeLower} AND s${histogramStageOffset} <= ${stageTimeUpper} THEN 'Needs_Attention' ELSE 'slow' END AS rating, COUNT(*) AS cnt FROM (\n" +
                    sqlTemplate +
                    ") as histo\n" +
                    "GROUP BY rating;";
            Integer histogramStageOffsetInt = parseHistogramStageName(velocityFilter, velocityConfigDTO.getName());
            histogramStageOffset = String.valueOf(histogramStageOffsetInt);
            VelocityConfigDTO.Stage stage = offsetStageMap.getOrDefault(histogramStageOffsetInt, null);
            if (stage != null) {
                stageTimeUpper = String.valueOf(stage.getUpperLimitUnit().toSeconds(stage.getUpperLimitValue()) + 1);
                stageTimeLower = String.valueOf(stage.getLowerLimitUnit().toSeconds(stage.getLowerLimitValue()) - 1);
            }
        } else if (filterByRatingRequired(velocityFilter)) {
            //create ratingConditions based on whether a single stage is requested or all stages
            if (StringUtils.isNotEmpty(velocityFilter.getHistogramStageName())) {
                Integer histogramStageOffsetInt = parseHistogramStageName(velocityFilter, velocityConfigDTO.getName());
                String histogramOffsetIndexStr = "s" + histogramStageOffsetInt;
                VelocityConfigDTO.Stage stage = offsetStageMap.getOrDefault(histogramStageOffsetInt, null);
                if (stage != null) {
                    stageTimeUpper = String.valueOf(stage.getUpperLimitUnit().toSeconds(stage.getUpperLimitValue()) + 1);
                    stageTimeLower = String.valueOf(stage.getLowerLimitUnit().toSeconds(stage.getLowerLimitValue()) - 1);
                }
                String finalStageTimeLower = stageTimeLower;
                String finalStageTimeUpper = stageTimeUpper;
                velocityFilter.getRatings().forEach(rating -> createRatingCondition(histogramOffsetIndexStr, rating, finalStageTimeLower, finalStageTimeUpper));
            } else {
                List<VelocityConfigDTO.Stage> allStages = getAllStages(velocityConfigDTO);
                for (int stageIndex = 0; stageIndex < allStages.size(); stageIndex++) {
                    VelocityConfigDTO.Stage stage = allStages.get(stageIndex);
                    if (stage != null) {
                        stageTimeUpper = String.valueOf(stage.getUpperLimitUnit().toSeconds(stage.getUpperLimitValue()) + 1);
                        stageTimeLower = String.valueOf(stage.getLowerLimitUnit().toSeconds(stage.getLowerLimitValue()) - 1);
                    }
                    String stageIndexStr = "s" + stageIndex;
                    String finalStageTimeLower = stageTimeLower;
                    String finalStageTimeUpper = stageTimeUpper;
                    velocityFilter.getRatings().forEach(rating -> createRatingCondition(stageIndexStr, rating, finalStageTimeLower, finalStageTimeUpper));
                }
            }
            sqlTemplate = "SELECT * FROM (\n" +
                    sqlTemplateWithoutLimit +
                    ") as histo\n" +
                    "where ${ratingConditions}\n" +
                    "   ${enrichLimitOffset}";
            ratingConditionsStr = String.join(" OR ", ratingConditions);
        }

        StringSubstitutor sqlSubstitutor = new StringSubstitutor(generateStringSubstitutorMap(NOT_FOR_COUNT_SQL, joinsString, whereClause, enrichWhereClause, enrichSortByClause, medianGroupByClause, medianOrderByClause, histogramStageOffset, stageTimeLower, stageTimeUpper, histogramBucketsCount, ratingConditionsStr));
        String sql = sqlSubstitutor.replace(sqlTemplate);
        StringSubstitutor countSqlSubstitutor = new StringSubstitutor(generateStringSubstitutorMap(FOR_COUNT_SQL, joinsString, whereClause, enrichWhereClause, enrichSortByClause, medianGroupByClause, medianOrderByClause, histogramStageOffset, stageTimeLower, stageTimeUpper, histogramBucketsCount, ratingConditionsStr));
        String countSql = countSqlSubstitutor.replace("SELECT COUNT(*) FROM ( " + sqlTemplate + " ) AS counted");

        return Query.builder()
                .sql(sql).countSql(countSql).params(params).offsetStageMap(offsetStageMap)
                .build();
    }

    private void createRatingCondition(String stageIndex, VelocityConfigDTO.Rating rating, String stageTimeLower, String stageTimeUpper) {
        switch (rating) {
            case MISSING:
                ratingConditions.add("(" + stageIndex + " IS NULL" + ")");
                break;
            case GOOD:
                ratingConditions.add("(" + stageIndex + " <= " + stageTimeLower + ")");
                break;
            case NEEDS_ATTENTION:
                ratingConditions.add("(" + stageIndex + " > " + stageTimeLower + " AND " + stageIndex + " <= " + stageTimeUpper + ")");
                break;
            case SLOW:
                ratingConditions.add("(" + stageIndex + " > " + stageTimeUpper + ")");
        }
    }

    private List<VelocityConfigDTO.Stage> getAllStages(final VelocityConfigDTO velocityConfigDTO) {
        List<VelocityConfigDTO.Stage> stages = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            List<VelocityConfigDTO.Stage> preDevStages = new ArrayList<>(velocityConfigDTO.getPreDevelopmentCustomStages());
            Collections.sort(preDevStages, Comparator.comparingInt(VelocityConfigDTO.Stage::getOrder));
            stages.addAll(preDevStages);
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getFixedStages())) {
            List<VelocityConfigDTO.Stage> fixedStages = new ArrayList<>(velocityConfigDTO.getFixedStages());
            Collections.sort(fixedStages, Comparator.comparingInt(VelocityConfigDTO.Stage::getOrder));
            stages.addAll(fixedStages);
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            List<VelocityConfigDTO.Stage> postDevStages = new ArrayList<>(velocityConfigDTO.getPostDevelopmentCustomStages());
            Collections.sort(postDevStages, Comparator.comparingInt(VelocityConfigDTO.Stage::getOrder));
            stages.addAll(postDevStages);
        }
        return stages;
    }

    private Boolean filterByRatingRequired(VelocityFilter velocityFilter) {
        return velocityFilter.getAcross() == VelocityFilter.DISTINCT.values && CollectionUtils.isNotEmpty(velocityFilter.getRatings());
    }

    private void updateScmPrFilters(ScmPrFilter scmPrFilter, Map<String, List<String>> scmPrConditions) {
        List<String> pullrequestsCondition = scmPrConditions.get("scm_pullrequests");
        if (params.containsKey("pr.creator_id")) {
            if(scmPrFilter.getIsApplyOuOnVelocityReport() == null || scmPrFilter.getIsApplyOuOnVelocityReport()){
            pullrequestsCondition.remove("pr.creator_id IN (:pr.creator_id)");
            pullrequestsCondition.add("(pr.id is NULL OR pr.creator_id IN (:pr.creator_id))");
            }
            params.put("pr.creator_id", parseUuidList(scmPrFilter.getCreators()));
        }
        if (params.containsKey("pr.exclude_creator_id")) {
            params.put("pr.exclude_creator_id", parseUuidList(scmPrFilter.getExcludeCreators()));
        }
        if (params.containsKey("pr.reviewer_id")) {
            pullrequestsCondition.remove("pr.reviewer_id && ARRAY[ :pr.reviewer_id ]::varchar[] ");
            if(scmPrFilter.getIsApplyOuOnVelocityReport() == null || scmPrFilter.getIsApplyOuOnVelocityReport()) {
                pullrequestsCondition.add("(prr.id is NULL OR prr.reviewer_id IN (:pr.reviewer_id))");
            }else {
                pullrequestsCondition.add("prr.reviewer_id IN (:pr.reviewer_id)");
            }
            params.put("pr.reviewer_id", parseUuidList(scmPrFilter.getReviewers()));
        }
        if (params.containsKey("pr.exclude_reviewer_id")) {
            pullrequestsCondition.remove(" NOT reviewer_id && ARRAY[ :pr.exclude_reviewer_id ]::varchar[] ");
            pullrequestsCondition.add("prr.reviewer_id NOT IN (:pr.exclude_reviewer_id)");
            params.put("pr.exclude_reviewer_id", parseUuidList(scmPrFilter.getExcludeReviewers()));
        }
        if (params.containsKey("pr.approver_id")) {
            pullrequestsCondition.remove("pr.approver_id && ARRAY[ :pr.approver_id ]::varchar[] ");
            if(scmPrFilter.getIsApplyOuOnVelocityReport() == null || scmPrFilter.getIsApplyOuOnVelocityReport()) {
                pullrequestsCondition.add("(prr.id is NULL OR prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' ) )");
                pullrequestsCondition.add("(prr.id is NULL OR prr.reviewer_id IN (:pr.approver_id) )");
            }else {
                pullrequestsCondition.add("prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' )");
                pullrequestsCondition.add("prr.reviewer_id IN (:pr.approver_id)");
            }
            params.put("pr.approver_id", parseUuidList(scmPrFilter.getApprovers()));
        }
        if (params.containsKey("pr.exclude_approver_id")) {
            pullrequestsCondition.remove(" NOT approver_id && ARRAY[ :pr.exclude_approver_id ]::varchar[] ");
            pullrequestsCondition.add("prr.state IN ( 'APPROVED', 'merged', 'approved','approved with suggestions' )");
            pullrequestsCondition.add("prr.reviewer_id NOT IN (:pr.exclude_approver_id)");
            params.put("pr.exclude_approver_id", parseUuidList(scmPrFilter.getExcludeApprovers()));
        }
        if (params.containsKey("pr.assignee_ids")) {
            pullrequestsCondition.remove("pr.assignee_ids && ARRAY[ :pr.assignee_ids ]::varchar[] ");
            if(scmPrFilter.getIsApplyOuOnVelocityReport() == null || scmPrFilter.getIsApplyOuOnVelocityReport()) {
                pullrequestsCondition.add("(pr.id is NULL OR pr.assignee_ids::uuid[] && ARRAY[ :pr.assignee_ids ] )");
            }else {
                pullrequestsCondition.add("pr.assignee_ids::uuid[] && ARRAY[ :pr.assignee_ids ]");
            }
            params.put("pr.assignee_ids", parseUuidList(scmPrFilter.getAssignees()));
        }
        if (params.containsKey("pr.exclude_assignee_ids")) {
            pullrequestsCondition.remove(" NOT assignee_ids && ARRAY[ :pr.exclude_assignee_ids ]::varchar[] ");
            pullrequestsCondition.add(" NOT assignee_ids::uuid[] && ARRAY[ :pr.exclude_assignee_ids ] ");
            params.put("pr.exclude_assignee_ids", parseUuidList(scmPrFilter.getExcludeAssignees()));
        }
    }

    private void updateScmCommitFilters(ScmCommitFilter scmCommitFilter,Map<String, List<String>> scmCommitConditions) {
        List<String> scmCommitConditionList=scmCommitConditions.get("scm_commits");
        if (params.containsKey("c.author_id")) {
            if(scmCommitFilter.getIsApplyOuOnVelocityReport() == null || scmCommitFilter.getIsApplyOuOnVelocityReport()){
                scmCommitConditionList.remove("c.author_id IN (:c.author_id)");
                scmCommitConditionList.add("(c.id is NULL OR c.author_id IN (:c.author_id))");
            }
            params.put("c.author_id", parseUuidList(scmCommitFilter.getAuthors()));
        }
        if (params.containsKey("c.exclude_author_id")) {
            params.put("c.exclude_author_id", parseUuidList(scmCommitFilter.getExcludeAuthors()));
        }
        if (params.containsKey("c.committer_id")) {
            if(scmCommitFilter.getIsApplyOuOnVelocityReport() == null || scmCommitFilter.getIsApplyOuOnVelocityReport()){
                scmCommitConditionList.remove("c.committer_id IN (:c.committer_id)");
                scmCommitConditionList.add("(c.id is NULL OR c.committer_id IN (:c.committer_id))");
            }
            params.put("c.committer_id", parseUuidList(scmCommitFilter.getCommitters()));
        }
        if (params.containsKey("c.exclude_committer_id")) {
            params.put("c.exclude_committer_id", parseUuidList(scmCommitFilter.getExcludeCommitters()));
        }
    }

    public static List<UUID> parseUuidList(List<String> list) {
        return ListUtils.emptyIfNull(list).stream()
                .filter(StringUtils::isNotBlank)
                .filter(str -> !str.equalsIgnoreCase("NONE"))
                .map(UUID::fromString)
                .collect(Collectors.toList());
    }

    Map<String, String> generateStringSubstitutorMap(boolean forCountSql, final String joinsString, final String whereClause, final String enrichWhereClause, final String enrichSortByClause, final String medianGroupByClause, final String medianOrderByClause,
                                                     final String histogramStageOffset, final String stageTimeLower, final String stageTimeUpper, final String histogramBucketsCount, String ratingConditionsStr) {
        Map<String, String> stringSubstitutorMap = new HashMap<>();
        stringSubstitutorMap.put("medianSelects", String.join(",", medianSelects));
        stringSubstitutorMap.put("enrichSelects", String.join(",", enrichSelects));
        stringSubstitutorMap.put("reduceSelects", String.join(",", reduceSelects));
        stringSubstitutorMap.put("baseSelects", String.join(",", baseSelects));
        stringSubstitutorMap.put("innerSelects", String.join(",", innerSelects));
        stringSubstitutorMap.put("joins", joinsString);
        stringSubstitutorMap.put("whereClause", whereClause);
        stringSubstitutorMap.put("baseGroupBys", String.join(",", baseGroupBys));

        stringSubstitutorMap.put("enrichWhereClause", enrichWhereClause);
        stringSubstitutorMap.put("enrichSortBys", enrichSortByClause);
        stringSubstitutorMap.put("enrichLimitOffset", (forCountSql) ? "" : enrichLimitOffset.toString());
        stringSubstitutorMap.put("medianGroupBys", medianGroupByClause);
        stringSubstitutorMap.put("medianOrderBys", medianOrderByClause);
        stringSubstitutorMap.put("histogramStageOffset", histogramStageOffset);
        stringSubstitutorMap.put("stageTimeLower", stageTimeLower);
        stringSubstitutorMap.put("stageTimeUpper", stageTimeUpper);
        stringSubstitutorMap.put("histogramBucketsCount", histogramBucketsCount);
        stringSubstitutorMap.put("ratingConditions", ratingConditionsStr);
        return stringSubstitutorMap;
    }

    private String getAggregationFunction(VelocityConfigDTO.Stage stage) {

        if(stage.getEvent() != null
                && stage.getEvent().getParams() != null
                && CollectionUtils.isNotEmpty(stage.getEvent().getParams().get("approval"))
                && "MAX".equalsIgnoreCase(stage.getEvent().getParams().get("approval").get(0)))
                    return "MAX";
        else
            return "MIN";
    }

    @Value
    @Builder
    public static class Query {
        String sql;
        String countSql;
        Map<String, Object> params;
        Map<Integer, VelocityConfigDTO.Stage> offsetStageMap;
    }
}
