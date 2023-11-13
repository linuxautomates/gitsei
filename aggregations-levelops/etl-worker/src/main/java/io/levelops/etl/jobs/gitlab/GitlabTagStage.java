package io.levelops.etl.jobs.gitlab;

import io.levelops.aggregations_shared.helpers.GitlabAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.gitlab.models.GitlabProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class GitlabTagStage extends BaseIngestionResultProcessingStage<GitlabProject, GitlabState> {
    private final GitlabAggHelperService gitlabAggHelperService;

    @Autowired
    public GitlabTagStage(GitlabAggHelperService gitlabAggHelperService) {
        this.gitlabAggHelperService = gitlabAggHelperService;
    }

    @Override
    public void process(JobContext context, GitlabState jobState, String ingestionJobId, GitlabProject entity) throws SQLException {
        gitlabAggHelperService.processTags(context.getTenantId(), context.getIntegrationId(), entity);
    }

    @Override
    public String getDataTypeName() {
        return "tags";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "Gitlab Tag Stage";
    }

    @Override
    public void preStage(JobContext context, GitlabState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, GitlabState jobState) throws SQLException {

    }
}
