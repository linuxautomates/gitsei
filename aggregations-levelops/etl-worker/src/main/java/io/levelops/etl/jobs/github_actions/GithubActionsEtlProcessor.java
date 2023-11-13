package io.levelops.etl.jobs.github_actions;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
public class GithubActionsEtlProcessor extends BaseIngestionResultEtlProcessor<GithubActionsJobState> {

    private final GithubActionsWorkflowRunStage githubActionsWorkflowRunStage;

    @Autowired
    protected GithubActionsEtlProcessor(GithubActionsWorkflowRunStage githubActionsWorkflowRunStage){
        super(GithubActionsJobState.class);
        this.githubActionsWorkflowRunStage = githubActionsWorkflowRunStage;
    }

    @Override
    public List<IngestionResultProcessingStage<?, GithubActionsJobState>> getIngestionProcessingJobStages() {
        return List.of(githubActionsWorkflowRunStage);
    }

    @Override
    public void preProcess(JobContext context, GithubActionsJobState jobState) {

    }

    @Override
    public void postProcess(JobContext context, GithubActionsJobState jobState) {

    }

    @Override
    public GithubActionsJobState createState(JobContext context) {
        return new GithubActionsJobState();
    }
}
