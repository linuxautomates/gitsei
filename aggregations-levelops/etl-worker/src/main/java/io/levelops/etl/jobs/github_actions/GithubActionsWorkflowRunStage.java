package io.levelops.etl.jobs.github_actions;

import io.levelops.aggregations_shared.helpers.GithubActionsAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.github_actions.models.GithubActionsEnrichedWorkflowRun;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Log4j2
public class GithubActionsWorkflowRunStage extends BaseIngestionResultProcessingStage<GithubActionsEnrichedWorkflowRun, GithubActionsJobState> {

    GithubActionsAggHelperService helper;

    @Autowired
    public GithubActionsWorkflowRunStage(GithubActionsAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public void process(JobContext context, GithubActionsJobState jobState, String ingestionJobId, GithubActionsEnrichedWorkflowRun entity) throws SQLException {
        List<String> artifactIds = new ArrayList<>();
        List<String> paramIds = new ArrayList<>();
        helper.processGithubActionsWorkflowRun(entity, context.getTenantId(), context.getIntegrationId(), artifactIds, paramIds);
        int cleanUpArtifactsCount = helper.cleanUpPushedArtifactData(context.getTenantId(), artifactIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        log.debug("cleaning up data: github actions pushed artifacts - {}", cleanUpArtifactsCount);
        int cleanUpParamsCount = helper.cleanUpPushedParamsData(context.getTenantId(), paramIds.stream().map(UUID::fromString).collect(Collectors.toList()));
        log.debug("cleaning up data: github actions pushed params - {}", cleanUpParamsCount);
    }

    @Override
    public String getDataTypeName() {
        return "workflow_run";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "Github Actions WorkflowRun Stage";
    }

    @Override
    public void preStage(JobContext context, GithubActionsJobState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, GithubActionsJobState jobState) throws SQLException {

    }
}
