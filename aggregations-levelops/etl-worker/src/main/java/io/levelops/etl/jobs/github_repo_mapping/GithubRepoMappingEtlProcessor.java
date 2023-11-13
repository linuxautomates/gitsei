package io.levelops.etl.jobs.github_repo_mapping;

import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseGenericEtlProcessor;
import io.levelops.etl.job_framework.GenericJobProcessingStage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
public class GithubRepoMappingEtlProcessor extends BaseGenericEtlProcessor<RepoMappingJobState> {
    private final RepoMappingStage repoMappingStage;

    protected GithubRepoMappingEtlProcessor(
            RepoMappingStage repoMappingStage
    ) {
        super(RepoMappingJobState.class);
        this.repoMappingStage = repoMappingStage;
    }

    @Override
    public List<GenericJobProcessingStage<RepoMappingJobState>> getGenericJobProcessingStages() {
        return List.of(repoMappingStage);
    }

    @Override
    public void preProcess(JobContext context, RepoMappingJobState jobState) {

    }

    @Override
    public void postProcess(JobContext context, RepoMappingJobState jobState) {

    }

    @Override
    public RepoMappingJobState createState(JobContext context) {
        return new RepoMappingJobState();
    }
}
