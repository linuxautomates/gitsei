package io.levelops.aggregations_shared.helpers.harnessng;

import com.google.common.annotations.VisibleForTesting;
import io.levelops.aggregations_shared.models.HarnessNGAggPipelineStageStep;
import io.levelops.commons.databases.models.database.CICDInstance;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.JobRunStageStep;
import io.levelops.commons.databases.models.filters.CICDInstanceFilter;
import io.levelops.commons.databases.models.filters.CICD_TYPE;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.CicdJobRunArtifactCorrelationService;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineStageStep;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.levelops.aggregations_shared.helpers.harnessng.HarnessNGConverters.parseExecutionMetadataFields;
import static io.levelops.aggregations_shared.helpers.harnessng.HarnessNGConverters.parseExecutionParams;

@Log4j2
@Service
public class HarnessNGAggHelperService {

    private final CiCdJobsDatabaseService ciCdJobsDatabaseService;
    private final CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    private final CiCdJobRunStageDatabaseService jobRunStageDatabaseService;
    private final CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    private final CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;
    private final CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    private final CicdJobRunArtifactCorrelationService cicdJobRunArtifactCorrelationService;

    @Autowired
    public HarnessNGAggHelperService(CiCdInstancesDatabaseService ciCdInstancesDatabaseService,
                                     CiCdJobsDatabaseService ciCdJobsDatabaseService,
                                     CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService,
                                     CiCdJobRunStageDatabaseService jobRunStageDatabaseService,
                                     CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService,
                                     CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService,
                                     CicdJobRunArtifactCorrelationService cicdJobRunArtifactCorrelationService) {
        this.ciCdInstancesDatabaseService = ciCdInstancesDatabaseService;
        this.ciCdJobsDatabaseService = ciCdJobsDatabaseService;
        this.ciCdJobRunsDatabaseService = ciCdJobRunsDatabaseService;
        this.jobRunStageDatabaseService = jobRunStageDatabaseService;
        this.ciCdJobRunStageStepsDatabaseService = ciCdJobRunStageStepsDatabaseService;
        this.ciCdJobRunArtifactsDatabaseService = ciCdJobRunArtifactsDatabaseService;
        this.cicdJobRunArtifactCorrelationService = cicdJobRunArtifactCorrelationService;
    }

    public String processPipeline(String company, String integrationId, HarnessNGPipelineExecution pipelineExecution) throws SQLException {
        UUID instanceId = getCiCdInstanceId(company, integrationId);
        HarnessNGPipeline pipeline = pipelineExecution.getPipeline();
        if (pipeline == null || pipeline.getIdentifier() == null) {
            log.warn("Skipping malformed pipeline without identifier: {}", pipeline);
            return null;
        }
        try {
            String ciCdJobId = insertIntoCiCdJob(company, instanceId, pipeline);
            log.debug("inserted into cicd jobs for job name {} ", pipeline.getName());

            processPipelineExecution(company, ciCdJobId, pipelineExecution);

            return ciCdJobId;
        } catch (SQLException throwable) {
            log.error("Failed to insert to cicd jobs with job name " + pipeline.getName() + " and execution id " + pipeline.getExecutionId(), throwable);
            return null;
        }
    }

    private void processPipelineExecution(String company, String ciCdJobId, HarnessNGPipelineExecution execution) {
        HarnessNGPipeline pipeline = execution.getPipeline();
        HarnessNGPipelineExecution.ExecutionGraph executionGraph = execution.getExecutionGraph();
        try {
            // -- job run
            Optional<CICDJobRun> ciCdJobRunOpt = insertIntoCiCdJobRun(company, execution, ciCdJobId);
            if (ciCdJobRunOpt.isEmpty()) {
                return;
            }
            log.debug("inserted into cicd job runs for pipeline id {} for execution id {} ", pipeline.getIdentifier(), pipeline.getExecutionId());

            // -- stages & steps
            if (executionGraph != null) {
                List<HarnessNGAggPipelineStageStep> stages = getPipelineStages(execution.getExecutionGraph().getRootNodeId(), execution.getExecutionGraph());
                insertStagesAndSteps(company, pipeline, ciCdJobRunOpt.get(), stages);
            }
            List<String> artifactIds = List.of();
            // -- artifacts
            if (executionGraph != null) {
                artifactIds = insertArtifacts(company, ciCdJobRunOpt.get(), executionGraph);
            }

            try {
                cicdJobRunArtifactCorrelationService.mapCicdJob(company, ciCdJobRunOpt.get(), artifactIds);
            } catch (Exception e) {
                log.error("Failed to map cicd job run to artifacts", e);
            }

        } catch (SQLException throwable) {
            log.error("Failed to insert to cicd jobs with job run number " + pipeline.getRunSequence() + " and execution id " + pipeline.getExecutionId(), throwable);
        }
    }

    private String insertIntoCiCdJob(String company, UUID instanceId, HarnessNGPipeline pipeline) throws SQLException {
        String orgProject = pipeline.getOrgIdentifier() + "/" + pipeline.getProjectIdentifier();
        String pipelineName = pipeline.getName();
        String jobFullName = orgProject + "/" + pipelineName;


        Optional<CICDJob> dbJobOpt = IterableUtils.getFirst(ciCdJobsDatabaseService.listByFilter(company, 0, 1, null, null, null, List.of(jobFullName), List.of(instanceId)).getRecords());
        if (dbJobOpt.isPresent()) {
            String dbJobId = dbJobOpt.get().getId().toString();
            log.debug("Skipping job insert, already in db: id={}", dbJobId);
            return dbJobId;
        }

        CICDJob cicdJob = CICDJob.builder()
                .cicdInstanceId(instanceId)
                .branchName(Optional.ofNullable(pipeline.getModuleInfo())
                        .map(HarnessNGPipeline.ModuleInfo::getCiModule)
                        .map(HarnessNGPipeline.ModuleInfo.CIModuleInfo::getBranch)
                        .orElse(null))
                .projectName(orgProject)
                .jobName(pipelineName)
                .jobFullName(jobFullName)
                .jobNormalizedFullName(jobFullName)
                .scmUrl(Optional.ofNullable(pipeline.getModuleInfo())
                        .map(HarnessNGPipeline.ModuleInfo::getCiModule)
                        .map(ciModule -> IterableUtils.getFirst(ciModule.getScmDetailsList())
                                .map(HarnessNGPipeline.ScmDetails::getScmUrl)
                                .orElse(null))
                        .orElse(null))
                .build();
        return ciCdJobsDatabaseService.insert(company, cicdJob);
    }

    @VisibleForTesting
    public Optional<CICDJobRun> insertIntoCiCdJobRun(String company, HarnessNGPipelineExecution execution, String ciCdJobId) throws SQLException {
        HarnessNGPipeline pipeline = execution.getPipeline();

        Long jobRunNumber = pipeline.getRunSequence();
        if (jobRunNumber == null) {
            log.warn("Skipping job run without number: executionId={}", execution.getPipeline().getExecutionId());
            return Optional.empty();
        }

        long finished = pipeline.getEndTs() != null ? pipeline.getEndTs() : 0;
        long started = pipeline.getStartTs() != null ? pipeline.getStartTs() : finished;
        started = TimeUnit.MILLISECONDS.toSeconds(started);
        finished = TimeUnit.MILLISECONDS.toSeconds(finished);
        Instant startTime = Instant.ofEpochSecond(started);
        Instant endTime = Instant.ofEpochSecond(finished);

        UUID cicdJobUuid = UUID.fromString(ciCdJobId);
        Optional<CICDJobRun> dbJobRun = IterableUtils.getFirst(ciCdJobRunsDatabaseService.listByFilter(company, 0, 1, null, List.of(cicdJobUuid), List.of(jobRunNumber)).getRecords());
        Instant dbEndTime = dbJobRun.map(CICDJobRun::getEndTime).orElse(null);
        log.debug("executionId={}, dbEndTime={}, endTime={}", pipeline.getExecutionId(), dbEndTime, endTime);
        if (dbEndTime != null && (endTime.isBefore(dbEndTime) || endTime.equals(dbEndTime))) {
            // exists in the db and already up to date
            log.debug("Skipping executionId={}, already in DB: dbEndTime={} endTime={}", pipeline.getExecutionId(), dbEndTime, endTime);
            return Optional.empty();
        }

        boolean isCI = Optional.ofNullable(pipeline.getModuleInfo()).map(HarnessNGPipeline.ModuleInfo::getCiModule).isPresent();
        boolean isCD = Optional.ofNullable(pipeline.getModuleInfo()).map(HarnessNGPipeline.ModuleInfo::getCdModule).isPresent();
        Map<String, Object> metadata = parseExecutionMetadataFields(execution);
        List<CICDJobRun.JobRunParam> params = parseExecutionParams(execution);
        CICDJobRun cicdJobRun = CICDJobRun.builder()
                .cicdJobId(cicdJobUuid)
                .jobRunNumber(jobRunNumber)
                .status(pipeline.getStatus())
                .startTime(startTime)
                .duration((Long.valueOf(finished - started).intValue()))
                .endTime(endTime)
                .cicdUserId(pipeline.getExecutionTriggerInfo() != null ? pipeline.getExecutionTriggerInfo().getTriggeredByUser().getIdentifier() : null)
                .scmCommitIds(Optional.ofNullable(pipeline.getModuleInfo())
                        .map(HarnessNGPipeline.ModuleInfo::getCiModule)
                        .map(HarnessNGPipeline.ModuleInfo.CIModuleInfo::getExecutionInfoDTO)
                        .map(HarnessNGPipeline.ExecutionInfoDTO::getBranch)
                        .map(branch -> branch.getCommits().stream().map(HarnessNGPipeline.Branch.Commit::getId).collect(Collectors.toList()))
                        .orElse(List.of()))
                .params(params)
                .ci(isCI)
                .cd(isCD)
                .metadata(metadata)
                .build();

        // -- insert if the job run was not found in the db; otherwise update it
        String jobRunId;
        if (dbJobRun.isEmpty()) {
            jobRunId = ciCdJobRunsDatabaseService.insert(company, cicdJobRun);
        } else {
            jobRunId = dbJobRun.get().getId().toString();
        }
        cicdJobRun = cicdJobRun.toBuilder()
                .id(UUID.fromString(jobRunId))
                .build();
        if (dbJobRun.isPresent()) {
            ciCdJobRunsDatabaseService.update(company, cicdJobRun);
        }

        log.debug("Inserted HarnessNG - Pipeline = {}, cicdJobRun = {}", pipeline.getName(), cicdJobRun.getJobRunNumber());
        return Optional.of(cicdJobRun);
    }

    private void insertStagesAndSteps(String company, HarnessNGPipeline pipeline, CICDJobRun ciCdJobRun, List<HarnessNGAggPipelineStageStep> stages) {
        ListUtils.emptyIfNull(stages).forEach(pipelineStage -> {
            try {
                String optStageId = insertJobRunStage(company, ciCdJobRun, pipeline, pipelineStage);
                ListUtils.emptyIfNull(pipelineStage.getSteps()).forEach(pipelineStageStep -> insertJobRunStageStep(company, optStageId, pipelineStageStep));
                log.debug("inserted into cicd stages and steps with job run id {} ", ciCdJobRun.getId());
            } catch (SQLException throwable) {
                throw new RuntimeException("Failed to insert job run stage for cicdJobRunId " + ciCdJobRun.getId(), throwable);
            }
        });
    }

    private String insertJobRunStage(String company, CICDJobRun ciCdJobRun, HarnessNGPipeline pipeline, HarnessNGAggPipelineStageStep pipelineStage) throws SQLException {
        long stopped = pipelineStage.getStageInfo().getEndTs() != null ? pipelineStage.getStageInfo().getEndTs() : 0;
        long started = pipelineStage.getStageInfo().getStartTs() != null ? pipelineStage.getStageInfo().getStartTs() : stopped;
        started = started / 1000;
        stopped = stopped / 1000;
        JobRunStage jobRunStage = JobRunStage.builder()
                .ciCdJobRunId(ciCdJobRun.getId())
                .stageId(pipelineStage.getStageInfo().getIdentifier())
                .state(pipelineStage.getStageInfo().getStatus())
                .childJobRuns(Set.of())
                .name(pipelineStage.getStageInfo().getName())
                .description(StringUtils.EMPTY)
                .result(pipelineStage.getStageInfo().getStatus())
                .duration(Long.valueOf(stopped - started).intValue())
                .logs(StringUtils.EMPTY)
                .startTime(Instant.ofEpochSecond(started))
                .fullPath(Set.of())
                .build();

        return jobRunStageDatabaseService.insert(company, jobRunStage);
    }

    private void insertJobRunStageStep(String company, String stageId, HarnessNGPipelineStageStep pipelineStageStep) {
        try {
            Long stopped = pipelineStageStep.getEndTs() != null ? pipelineStageStep.getEndTs() : 0;
            Long started = pipelineStageStep.getStartTs() != null ? pipelineStageStep.getStartTs() : stopped;
            started = started / 1000;
            stopped = stopped / 1000;
            JobRunStageStep jobRunStageStep = JobRunStageStep.builder()
                    .cicdJobRunStageId(UUID.fromString(stageId))
                    .stepId(pipelineStageStep.getIdentifier())
                    .displayName(pipelineStageStep.getName())
                    .displayDescription(StringUtils.EMPTY)
                    .startTime((started != 0) ? Instant.ofEpochSecond(started) : null)
                    .result(pipelineStageStep.getStatus())
                    .state(pipelineStageStep.getStatus())
                    .duration(Long.valueOf(stopped - started).intValue())
                    .build();
            ciCdJobRunStageStepsDatabaseService.insert(company, jobRunStageStep);
        } catch (SQLException throwable) {
            throw new RuntimeException("Failed to insert job run stage step for stage id " + stageId, throwable);
        }
    }

    private List<String> insertArtifacts(String company, CICDJobRun cicdJobRun, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        try {
            List<CiCdJobRunArtifact> artifacts = HarnessNGConverters.parseArtifacts(cicdJobRun, executionGraph);
            return ciCdJobRunArtifactsDatabaseService.replace(company, cicdJobRun.getId().toString(), artifacts);
        } catch (SQLException e) {
            log.error("Failed to insert artifacts for cicdJobRunId={}", cicdJobRun.getId(), e);
        } catch (Exception e) {
            log.error("Failed to parse artifacts for cicdJobRunId={}", cicdJobRun.getId(), e);
        }
        return List.of();
    }

    public List<HarnessNGAggPipelineStageStep> getPipelineStages(String rootNodeId,
                                                                 HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        List<HarnessNGAggPipelineStageStep> stages = new ArrayList<>();
        if (ObjectUtils.anyNull(rootNodeId, executionGraph.getNodeMap(), executionGraph.getNodeAdjacencyListMap()) ||
                rootNodeId.isEmpty() ||
                executionGraph.getNodeMap().isEmpty() ||
                executionGraph.getNodeAdjacencyListMap().isEmpty()) {
            return stages;
        }
        List<HarnessNGPipelineStageStep> steps;
        Stack<String> stack = new Stack<>();
        stack.push(rootNodeId);
        while (!stack.empty()) {
            HarnessNGAggPipelineStageStep stage = HarnessNGAggPipelineStageStep.builder().stageInfo(executionGraph.getNodeMap().get(stack.peek())).build();
            String top = stack.peek();
            stack.pop();
            if ((stage.getStageInfo().getBaseFqn()).equals("pipeline.stages." + stage.getStageInfo().getIdentifier())) {

                // Once stage is found, then its children are considered as steps of that stage
                steps = getPipelineSteps(stage.getStageInfo().getUuid(), executionGraph);
                stage = stage.toBuilder().steps(steps).build();
                stages.add(stage);
                getNextIdsAndPush(top, stack, executionGraph);
            } else {
                getNextIdsAndPush(top, stack, executionGraph);
                getChildrenAndPush(top, stack, executionGraph);
            }
        }
        return stages;
    }

    private List<HarnessNGPipelineStageStep> getPipelineSteps(String stageId,
                                                              HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        List<HarnessNGPipelineStageStep> steps = new ArrayList<>();
        Stack<String> stack = new Stack<>();

        getChildrenAndPush(stageId, stack, executionGraph);
        while (!stack.empty()) {
            String top = stack.peek();
            stack.pop();

            steps.add(executionGraph.getNodeMap().get(top));

            getNextIdsAndPush(top, stack, executionGraph);
            getChildrenAndPush(top, stack, executionGraph);
        }
        return steps;
    }

    private void getChildrenAndPush(String nodeId, Stack<String> stack, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        HarnessNGPipelineExecution.ExecutionGraph.NodeAdjacency adjacency = executionGraph.getNodeAdjacencyListMap().get(nodeId);
        List<String> children = adjacency.getChildren();
        Collections.reverse(children);

        if (!ListUtils.isEmpty(children)) {
            for (String child : children) {
                stack.push(child);
            }
        }
    }

    private void getNextIdsAndPush(String nodeId, Stack<String> stack, HarnessNGPipelineExecution.ExecutionGraph executionGraph) {
        HarnessNGPipelineExecution.ExecutionGraph.NodeAdjacency adjacency = executionGraph.getNodeAdjacencyListMap().get(nodeId);
        List<String> nextIds = adjacency.getNextIds();

        if (!ListUtils.isEmpty(nextIds)) {
            stack.push(nextIds.get(0));
        }
    }

    private UUID getCiCdInstanceId(String company, String integrationId) throws SQLException {
        DbListResponse<CICDInstance> dbListResponse = ciCdInstancesDatabaseService
                .list(company,
                        CICDInstanceFilter.builder()
                                .integrationIds(List.of(integrationId))
                                .types(List.of(CICD_TYPE.harnessng))
                                .build(), null, null, null);
        if (CollectionUtils.isNotEmpty(dbListResponse.getRecords())) {
            return dbListResponse.getRecords().get(0).getId();
        } else {
            log.warn("CiCd instance response is empty for company " + company + "and integration id" + integrationId);
            throw new RuntimeException("Error listing the cicd instances for integration id " + integrationId + " type "
                    + CICD_TYPE.harnessng);
        }
    }

    public Long getOldestJobRunStartTime(String company, String integrationId) {
        int integration = Integer.parseInt(integrationId);
        Long oldestJobRunStartTime = ciCdJobRunsDatabaseService.getOldestJobRunStartTimeInMillis(company,integration);
        return oldestJobRunStartTime != null ? oldestJobRunStartTime : Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli();
    }
}
