package io.levelops.etl.jobs.gitlab;

import io.levelops.aggregations_shared.helpers.GitlabAggHelperService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.scm.DbScmUser;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.etl.job_framework.BaseIngestionResultEtlProcessor;
import io.levelops.etl.job_framework.IngestionResultProcessingStage;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Log4j2
@Service
public class GitlabEtlProcessor extends BaseIngestionResultEtlProcessor<GitlabState> {
    private final GitlabCommitStage gitlabCommitStage;
    private final GitlabMergeRequestStage gitlabMergeRequestStage;
    private final GitlabCommitDirectMergeStage gitlabCommitDirectMergeStage;
    private final GitlabIssueStage gitlabIssueStage;
    private final GitlabPipelineStage gitlabPipelineStage;
    private final GitlabTagStage gitlabTagStage;
    private final GitlabUserStage gitlabUserStage;

    private final GitlabAggHelperService gitlabAggHelperService;
    private final EventsClient eventsClient;

    @Autowired
    protected GitlabEtlProcessor(
            GitlabCommitStage gitlabCommitStage,
            GitlabMergeRequestStage gitlabMergeRequestStage,
            GitlabCommitDirectMergeStage gitlabCommitDirectMergeStage,
            GitlabIssueStage gitlabIssueStage,
            GitlabPipelineStage gitlabPipelineStage,
            GitlabTagStage gitlabTagStage,
            GitlabUserStage gitlabUserStage,
            GitlabAggHelperService gitlabAggHelperService,
            EventsClient eventsClient
    ) {
        super(GitlabState.class);
        this.gitlabCommitStage = gitlabCommitStage;
        this.gitlabMergeRequestStage = gitlabMergeRequestStage;
        this.gitlabCommitDirectMergeStage = gitlabCommitDirectMergeStage;
        this.gitlabIssueStage = gitlabIssueStage;
        this.gitlabPipelineStage = gitlabPipelineStage;
        this.gitlabTagStage = gitlabTagStage;
        this.gitlabUserStage = gitlabUserStage;
        this.gitlabAggHelperService = gitlabAggHelperService;
        this.eventsClient = eventsClient;
    }

    @Override
    public List<IngestionResultProcessingStage<?, GitlabState>> getIngestionProcessingJobStages() {
        return List.of(
                gitlabCommitStage,
                gitlabMergeRequestStage,
                gitlabCommitDirectMergeStage,
                gitlabIssueStage,
                gitlabPipelineStage,
                gitlabTagStage,
                gitlabUserStage
        );
    }

    @Override
    public void preProcess(JobContext context, GitlabState jobState) {

    }

    @Override
    public void postProcess(JobContext context, GitlabState jobState) {
        IntegrationKey integrationKey = IntegrationKey.builder()
                .tenantId(context.getTenantId())
                .integrationId(context.getIntegrationId())
                .build();

        Long startTime = gitlabAggHelperService.getOldestJobRunStartTime(context.getTenantId(), context.getIntegrationId());

        try {
            eventsClient.emitEvent(context.getTenantId(), EventType.GITLAB_NEW_AGGREGATION, Map.of("integration_key", integrationKey, "start_time", startTime));
        } catch (EventsClientException e) {
            log.error("Failed to emit event for gitlab ETL job {}", context.getJobInstanceId(), e);
        }
    }

    @Override
    public GitlabState createState(JobContext context) {
        return new GitlabState(new ArrayList<DbScmUser>());
    }
}
