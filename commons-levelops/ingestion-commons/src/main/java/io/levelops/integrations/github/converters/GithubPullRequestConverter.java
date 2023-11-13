package io.levelops.integrations.github.converters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.github.models.NormalizedGithubPullRequest;
import io.levelops.normalization.Normalizer;
import lombok.extern.log4j.Log4j2;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Log4j2
public class GithubPullRequestConverter {

    private static final String DEFAULT_VALUE = "";

    @Normalizer(contentType = "integration/github/pull_request")
    public static NormalizedGithubPullRequest convert(ObjectMapper objectMapper, GithubPullRequest pullRequest) {
        Optional<GithubPullRequest> pullRequestOpt = Optional.ofNullable(pullRequest);
        String id = pullRequestOpt.map(GithubPullRequest::getId).orElse(DEFAULT_VALUE);
        String title = pullRequestOpt.map(GithubPullRequest::getTitle).orElse(DEFAULT_VALUE);
        String body = pullRequestOpt.map(GithubPullRequest::getBody).orElse(DEFAULT_VALUE);
        String state = pullRequestOpt.map(GithubPullRequest::getState).orElse(DEFAULT_VALUE);
        Integer prNumber = pullRequestOpt.map(GithubPullRequest::getNumber).orElse(0);

        String assignee = pullRequestOpt.map(GithubPullRequest::getAssignee)
                .map(GithubUser::getLogin)
                .orElse(DEFAULT_VALUE);

        List<String> prLabels = pullRequestOpt.map(GithubPullRequest::getLabels)
                .orElse(Collections.emptyList())
                .stream()
                .map(GithubPullRequest.Label::getName)
                .collect(Collectors.toList());

        List<String> prAssignees = pullRequestOpt.map(GithubPullRequest::getAssignees)
                .orElse(Collections.emptyList())
                .stream()
                .map(GithubUser::getLogin)
                .collect(Collectors.toList());

        List<String> prReviewers = pullRequestOpt.map(GithubPullRequest::getRequestedReviewers)
                .orElse(Collections.emptyList()).stream()
                .map(GithubUser::getLogin)
                .collect(Collectors.toList());

        String milestone = pullRequestOpt.map(GithubPullRequest::getMilestone)
                .map(GithubPullRequest.Milestone::getId)
                .orElse(DEFAULT_VALUE);

        return NormalizedGithubPullRequest.builder()
                .id(id)
                .number(prNumber)
                .title(title)
                .body(body)
                .state(state)
                .labels(prLabels)
                .assignee(assignee)
                .assignees(prAssignees)
                .reviewers(prReviewers)
                .milestone(milestone)
                .locked(pullRequest.getLocked())
                .createdAt(pullRequest.getCreatedAt())
                .updatedAt(pullRequest.getUpdatedAt())
                .mergedAt(pullRequest.getMergedAt())
                .closedAt(pullRequest.getClosedAt())
                .build();
    }
}
