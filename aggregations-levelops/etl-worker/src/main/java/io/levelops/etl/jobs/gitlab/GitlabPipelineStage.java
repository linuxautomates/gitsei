package io.levelops.etl.jobs.gitlab;

import io.levelops.aggregations_shared.helpers.GitlabAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.gitlab.models.GitlabProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class GitlabPipelineStage extends BaseIngestionResultProcessingStage<GitlabProject, GitlabState> {
    private final GitlabAggHelperService gitlabAggHelperService;

    @Autowired
    public GitlabPipelineStage(GitlabAggHelperService gitlabAggHelperService) {
        this.gitlabAggHelperService = gitlabAggHelperService;
    }

    @Override
    public void process(JobContext context, GitlabState jobState, String ingestionJobId, GitlabProject entity) throws SQLException {
        gitlabAggHelperService.processPipelines(context.getTenantId(), context.getIntegrationId(), entity);
    }

    @Override
    public String getDataTypeName() {
        return "pipelines";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "Gitlab Pipeline Stage";
    }

    @Override
    public void preStage(JobContext context, GitlabState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, GitlabState jobState) throws SQLException {

    }
}
