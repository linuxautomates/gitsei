package io.levelops.etl.jobs.github;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.helpers.GithubAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.integrations.github.models.GithubIterativeScanQuery;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.services.ControlPlaneService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;

@Log4j2
@Service
public class GithubEtlProcessor extends BaseIngestionResultEtlProcessor<GithubJobState> {
    private final GithubCommitStage githubCommitStage;
    private final GithubPrStage githubPrStage;
    private final GithubCommitDirectMergeStage githubCommitDirectMergeStage;
    private final GithubIssuesStage githubIssuesStage;
    private final GithubProjectStage githubProjectStage;
    private final GithubTagStage githubTagStage;
    private final GithubRepositoryStage githubRepositoryStage;
    private final GithubUsersStage githubUsersStage;

    private final GithubAggHelperService githubAggHelperService;

    private final ControlPlaneService controlPlaneService;

    private final ObjectMapper mapper;

    @Autowired
    protected GithubEtlProcessor(
            GithubCommitStage githubCommitStage,
            GithubPrStage githubPrStage,
            GithubCommitDirectMergeStage githubCommitDirectMergeStage,
            GithubIssuesStage githubIssuesStage,
            GithubProjectStage githubProjectStage,
            GithubTagStage githubTagStage,
            GithubRepositoryStage githubRepositoryStage,
            GithubUsersStage githubUsersStage,
            GithubAggHelperService githubAggHelperService,
            ControlPlaneService controlPlaneService,
            ObjectMapper mapper) {
        super(GithubJobState.class);
        this.githubCommitStage = githubCommitStage;
        this.githubPrStage = githubPrStage;
        this.githubCommitDirectMergeStage = githubCommitDirectMergeStage;
        this.githubIssuesStage = githubIssuesStage;
        this.githubProjectStage = githubProjectStage;
        this.githubTagStage = githubTagStage;
        this.githubRepositoryStage = githubRepositoryStage;
        this.githubUsersStage = githubUsersStage;
        this.githubAggHelperService = githubAggHelperService;
        this.controlPlaneService = controlPlaneService;
        this.mapper = mapper;
    }

    @Override
    public void preProcess(JobContext context, GithubJobState jobState) {
    }

    @Override
    public void postProcess(JobContext context, GithubJobState jobState) {
        emitCicdMappingEvent(context);
    }

    private void emitCicdMappingEvent(JobContext context) {

        OptionalLong oldestIngestedAt = getOldestIngestionTime(context);
        long startTime = Instant.now().minus(1l, ChronoUnit.DAYS).toEpochMilli();
        if(oldestIngestedAt.isPresent()) {
            startTime = Math.min(oldestIngestedAt.getAsLong(), startTime);
        }


        githubAggHelperService.emitEvent(context.getTenantId(), context.getIntegrationId(), startTime);
    }

    private OptionalLong getOldestIngestionTime(JobContext context) {

         return context.getGcsRecords().stream().
                map(gcs -> {
                    try {
                        JobDTO jobDto = context.getIngestionJobDto(gcs.getIngestionJobId(), controlPlaneService);
                        GithubIterativeScanQuery query = mapper.convertValue(jobDto.getQuery(), GithubIterativeScanQuery.class);
                        if(query != null){
                            return query.getFrom();
                        }
                        return null;
                    } catch (IngestionServiceException e) {
                        throw new RuntimeException(e);
                    }
                }).filter(Objects::nonNull)
                 .mapToLong(d -> d.toInstant().toEpochMilli())
                 .min();
    }

    @Override
    public List<IngestionResultProcessingStage<?, GithubJobState>> getIngestionProcessingJobStages() {
        return List.of(
                githubUsersStage,
                githubRepositoryStage,
                githubCommitStage,
                githubPrStage,
                githubCommitDirectMergeStage,
                githubIssuesStage,
                githubProjectStage,
                githubTagStage
        );
    }

    @Override
    public GithubJobState createState(JobContext context) {
        return new GithubJobState();
    }
}
