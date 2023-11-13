package io.levelops.etl.jobs.github;

import io.levelops.aggregations_shared.helpers.GithubAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@Log4j2
@Service
public class GithubTagStage extends BaseIngestionResultProcessingStage<GithubRepository, GithubJobState> {
    private final GithubAggHelperService helper;

    @Autowired
    public GithubTagStage(GithubAggHelperService helper) {
        this.helper = helper;
    }

    @Override
    public String getName() {
        return "Github Tag Stage";
    }

    @Override
    public void preStage(JobContext context, GithubJobState jobState) throws SQLException {
    }

    @Override
    public void process(JobContext context, GithubJobState jobState, String ingestionJobId, GithubRepository entity) throws SQLException {
        helper.insertRepositoryTags(entity, context.getTenantId(), context.getIntegrationId());
    }

    @Override
    public void postStage(JobContext context, GithubJobState jobState) {
    }

    @Override
    public String getDataTypeName() {
        return "tags";
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return false;
    }
}
