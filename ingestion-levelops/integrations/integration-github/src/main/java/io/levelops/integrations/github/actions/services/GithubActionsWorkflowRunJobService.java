package io.levelops.integrations.github.actions.services;

import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRunJob;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.stream.Stream;

@Log4j2
public class GithubActionsWorkflowRunJobService {

    private static final int DEFAULT_PER_PAGE = 100;

    public Stream<GithubActionsWorkflowRunJob> getWorkflowRunJobs(GithubClient client, String repoFullName, GithubActionsWorkflowRun workflowRun, Instant from, Instant to) {
        log.debug("Fetching workflow run jobs for repository = {} and workflow_run_id = {}", repoFullName, workflowRun.getId());

        return client.streamWorkflowRunJobs(repoFullName, workflowRun.getId(), DEFAULT_PER_PAGE);
    }
}