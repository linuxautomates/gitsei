package io.levelops.integrations.github.actions.services;

import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflow;
import io.levelops.integrations.github_actions.models.GithubActionsWorkflowRun;
import lombok.extern.log4j.Log4j2;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j2
public class GithubActionsWorkflowRunService {

    private static final int DEFAULT_PER_PAGE = 100;

    public Stream<GithubActionsWorkflowRun> getWorkflowRuns(GithubClient client, String repoFullName, Instant from, Instant to) {
        log.debug("Fetching workflow runs for repository {} from {} to {}", repoFullName, from, to);
        Map<Long, String> workflowNames = getWorkflowNames(client, repoFullName);
        return StreamSupport.stream(client.streamWorkflowRuns(repoFullName, DEFAULT_PER_PAGE).spliterator(), false)
                .filter(workflowRun -> workflowRun.getUpdatedAt() != null && workflowRun.getUpdatedAt().toInstant().isBefore(to) && workflowRun.getUpdatedAt().toInstant().isAfter(from))
                .map(workflowRun -> enrichWorkflowRun(workflowRun, workflowNames));
    }

    private Map<Long, String> getWorkflowNames(GithubClient client, String repoFullName) {
        return client.streamWorkflows(repoFullName, DEFAULT_PER_PAGE)
                .collect(Collectors.toConcurrentMap(GithubActionsWorkflow::getId, GithubActionsWorkflow::getName));
    }

    private GithubActionsWorkflowRun enrichWorkflowRun(GithubActionsWorkflowRun workflowRun, Map<Long, String> workflowNames) {
        return workflowRun.toBuilder()
                .workflowName(workflowNames.getOrDefault(workflowRun.getWorkflowId(), workflowRun.getName()))
                .build();
    }
}