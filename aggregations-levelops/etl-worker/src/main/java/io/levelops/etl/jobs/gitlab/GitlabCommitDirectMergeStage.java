package io.levelops.etl.jobs.gitlab;

import io.levelops.aggregations_shared.helpers.GitlabAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.gitlab.models.GitlabProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Service
public class GitlabCommitDirectMergeStage extends BaseIngestionResultProcessingStage<GitlabProject, GitlabState> {
    private final GitlabAggHelperService gitlabAggHelperService;

    @Autowired
    public GitlabCommitDirectMergeStage(GitlabAggHelperService gitlabAggHelperService) {
        this.gitlabAggHelperService = gitlabAggHelperService;
    }


    @Override
    public void process(JobContext context, GitlabState jobState, String ingestionJobId, GitlabProject entity) throws SQLException {
        gitlabAggHelperService.processCommitsForDirectMerge(context.getTenantId(), context.getIntegrationId(), entity);
    }

    @Override
    public String getDataTypeName() {
        return "commits";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }

    @Override
    public String getName() {
        return "Gitlab Commit Direct Merge Stage";
    }

    @Override
    public void preStage(JobContext context, GitlabState jobState) throws SQLException {

    }

    @Override
    public void postStage(JobContext context, GitlabState jobState) throws SQLException {

    }
}
