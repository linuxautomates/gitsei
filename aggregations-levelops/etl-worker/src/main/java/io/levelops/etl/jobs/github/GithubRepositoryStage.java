package io.levelops.etl.jobs.github;

import com.google.common.collect.Lists;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.repo.DbRepository;
import io.levelops.commons.databases.services.GitRepositoryService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Service
public class GithubRepositoryStage  extends BaseIngestionResultProcessingStage<GithubRepository, GithubJobState> {
    private final GitRepositoryService gitRepositoryService;

    public GithubRepositoryStage(GitRepositoryService gitRepositoryService) {
        this.gitRepositoryService = gitRepositoryService;
    }

    @Override
    public String getName() {
        return "Github Repository Stage";
    }

    @Override
    public void preStage(JobContext context, GithubJobState jobState) throws SQLException {
        jobState.setRepositoryList(new ArrayList<>());
    }

    @Override
    public void process(JobContext context, GithubJobState jobState, String ingestionJobId, GithubRepository entity) throws SQLException {
        DbRepository dbRepository = DbRepository.fromGithubRepository(entity, context.getIntegrationId());
        if (dbRepository != null) {
            jobState.getRepositoryList().add(dbRepository);
        }
    }

    @Override
    public void postStage(JobContext context, GithubJobState jobState) {
        List<List<DbRepository>> batches = Lists.partition(jobState.getRepositoryList(), 500);
        log.info("{} batches of repositories to be upserted", batches.size());
        AtomicInteger index = new AtomicInteger();
        batches.forEach(projects -> {
            try {
                gitRepositoryService.batchUpsert(context.getTenantId(), projects);
                log.info("Persisted batch {} of github repos", index);
                index.getAndIncrement();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("Handled {} Github repos for tenant {}", jobState.getRepositoryList().size(), context.getTenantId());
    }

    @Override
    public String getDataTypeName() {
        return "repositories";
    }

    @Override
    public boolean shouldCheckpointIndividualFiles() {
        return false;
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return true;
    }
}
