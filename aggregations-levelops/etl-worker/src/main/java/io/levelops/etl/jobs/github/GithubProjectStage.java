package io.levelops.etl.jobs.github;

import io.levelops.aggregations_shared.helpers.GithubAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.github.models.GithubProject;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Log4j2
@Service
public class GithubProjectStage extends BaseIngestionResultProcessingStage<GithubProject, GithubJobState> {
    private final GithubAggHelperService helper;

    public GithubProjectStage(GithubAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public String getName() {
        return "Github Project Stage";
    }

    @Override
    public void preStage(JobContext context, GithubJobState jobState) throws SQLException {
    }

    @Override
    public void process(JobContext context, GithubJobState jobState, String ingestionJobId, GithubProject entity) throws SQLException {
        helper.processGitProject(context.getTenantId(), context.getIntegrationId(), entity);
        helper.linkIssuesAndProjectCards(context.getTenantId(), context.getIntegrationId());
    }

    @Override
    public void postStage(JobContext context, GithubJobState jobState) {
    }

    @Override
    public String getDataTypeName() {
        return "projects";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }
}
