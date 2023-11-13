package io.levelops.integrations.sonarqube.services;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;

import io.levelops.integrations.sonarqube.models.Metric;
import io.levelops.integrations.sonarqube.models.Project;
import io.levelops.integrations.sonarqube.models.PullRequest;
import io.levelops.integrations.sonarqube.models.PullRequestResponse;
import io.levelops.integrations.sonarqube.models.Issue;
import io.levelops.integrations.sonarqube.models.IssueResponse;
import io.levelops.integrations.sonarqube.models.Analyse;
import io.levelops.integrations.sonarqube.models.ProjectAnalysesResponse;
import io.levelops.integrations.sonarqube.models.ProjectBranchResponse;
import io.levelops.integrations.sonarqube.models.Branch;
import io.levelops.integrations.sonarqube.models.Measure;
import io.levelops.integrations.sonarqube.models.ComponentWithMeasures;
import io.levelops.integrations.sonarqube.sources.SonarQubeProjectDataSource;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sonarqube ProjectEnrichmentService class which should be used for enriching project data with project analyses,project branch,
 * pull requests and issues.
 */
@Log4j2
public class SonarQubeProjectEnrichmentService {

    public List<PullRequest> enrichPullRequest(SonarQubeClient sonarQubeClient,
                                               SonarQubeProjectDataSource.SonarQubeProjectQuery query,
                                               Map<String, Metric> metrics,
                                               Project project) throws SonarQubeClientException {
        PullRequestResponse pullRequestResponse = sonarQubeClient.getPullRequests(project.getKey());
        return pullRequestResponse.getPullRequests().parallelStream()
                .filter(pullRequest -> pullRequest.getAnalysisDate() != null && pullRequest.getAnalysisDate().after(query.getFrom()))
                .map(pullRequest -> {
                    try {
                        return pullRequest.toBuilder()
                                .measures(getMeasuresWithType(sonarQubeClient.getMeasuresForPullRequest(
                                        project.getKey(), pullRequest.getKey(), metrics.keySet()), metrics))
                                .build();
                    } catch (SonarQubeClientException e) {
                        log.warn("enrichPullRequest: failed to fetch measures for pull request: "
                                + pullRequest.getKey() + " in project: " + project.getKey(), e);
                        return pullRequest;
                    }
                })
                .map(pullRequest -> {
                    List<Issue> issues = PaginationUtils.stream(1, 1, offset -> {
                        try {
                            IssueResponse issueResponse = sonarQubeClient
                                    .getIssues(project.getKey(), pullRequest.getKey(), query.getFrom(), query.getTo(), null, null, null, null, offset);
                            return issueResponse.getIssues();
                        } catch (SonarQubeClientException e) {
                            log.warn("enrichPullRequest: failed to fetch issues for pull request: "
                                    + pullRequest.getKey() + " in project: " + project.getKey(), e);
                            return List.of();
                        }
                    }).collect(Collectors.toList());
                    return pullRequest.toBuilder()
                            .issues(issues)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<Branch> enrichProjectBranches(SonarQubeClient sonarQubeClient,
                                              SonarQubeProjectDataSource.SonarQubeProjectQuery query,
                                              Project project,
                                              Map<String, Metric> metrics) throws SonarQubeClientException {
        ProjectBranchResponse projectBranchResponse = sonarQubeClient.getProjectBranches(project.getKey());
        return projectBranchResponse.getBranches()
                .parallelStream()
                .filter(branch -> branch.getAnalysisDate() != null && branch.getAnalysisDate().after(query.getFrom()))
                .map(branch -> {
                    try {
                        return branch.toBuilder()
                                .measures(getMeasuresWithType(sonarQubeClient.getMeasuresForBranch(
                                        project.getKey(), branch.getName(), metrics.keySet()), metrics))
                                .build();
                    } catch (SonarQubeClientException e) {
                        log.error("enrichProjectBranches: failed to fetch measures for branch: "
                                + branch.getName() + " in project: " + project.getKey(), e);
                        return branch;
                    }
                })
                .collect(Collectors.toList());
    }

    public List<Analyse> enrichProjectAnalyses(SonarQubeClient sonarQubeClient,
                                               SonarQubeProjectDataSource.SonarQubeProjectQuery query,
                                               Project project) {
        return PaginationUtils.stream(1, 1, offset -> {
            try {
                ProjectAnalysesResponse analysesResponse = sonarQubeClient
                        .getProjectAnalyses(project.getKey(), query.getFrom(), query.getTo(), offset);
                return analysesResponse.getAnalyses();
            } catch (SonarQubeClientException e) {
                log.warn("Failed to get project analyses after page {}", offset, e);
                throw new RuntimeStreamException("Failed to get project analyses after page=" + offset, e);
            }
        }).collect(Collectors.toList());
    }

    public List<Measure> getMeasures(SonarQubeClient sonarQubeClient, String projectKey,
                                     Map<String, Metric> metrics) throws SonarQubeClientException {
        var numericMetricKeys = metrics.values().stream().filter(m -> !m.getType().equalsIgnoreCase("string")).map(m -> m.getKey()).collect(Collectors.toSet());
        ComponentWithMeasures measures = sonarQubeClient.getMeasures(projectKey, numericMetricKeys);
        return getMeasuresWithType(measures, metrics);
    }

    private List<Measure> getMeasuresWithType(ComponentWithMeasures measures, Map<String, Metric> metrics) {
        return ListUtils.emptyIfNull(measures.getMeasures())
                .stream()
                .map(measure -> measure.toBuilder()
                        .dataType(Optional.ofNullable(metrics.get(measure.getMetric()))
                                .map(Metric::getType)
                                .orElse(null))
                        .build())
                .collect(Collectors.toList());
    }
}
