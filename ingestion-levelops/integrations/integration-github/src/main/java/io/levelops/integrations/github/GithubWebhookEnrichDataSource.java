package io.levelops.integrations.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.model.GithubApiCommit;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubCommitFile;
import io.levelops.integrations.github.models.GithubIssueEvent;
import io.levelops.integrations.github.models.GithubProjectCard;
import io.levelops.integrations.github.models.GithubProjectColumn;
import io.levelops.integrations.github.models.GithubReview;
import io.levelops.integrations.github.models.GithubWebhookEnrichResponse;
import io.levelops.integrations.github.models.GithubWebhookEnrichResponses;
import io.levelops.integrations.github.models.GithubWebhookEnrichmentRequests;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.egit.github.core.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubWebhookEnrichDataSource implements DataSource<GithubWebhookEnrichResponse, GithubWebhookEnrichDataSource.GithubWebhookEnrichQuery> {

    private final GithubClientFactory levelOpsClientFactory;

    public GithubWebhookEnrichDataSource(GithubClientFactory levelOpsClientFactory) {
        this.levelOpsClientFactory = levelOpsClientFactory;
    }

    @Override
    public Stream<Data<GithubWebhookEnrichResponse>> fetchMany(GithubWebhookEnrichQuery query) throws FetchException {
        IntegrationKey integrationKey = query.getIntegrationKey();
        GithubClient levelopsClient = getLevelopsClient(integrationKey);
        List<GithubWebhookEnrichResponse> results = new ArrayList<>();
        if (query.getEnrichmentRequests() != null && CollectionUtils.isNotEmpty(query.getEnrichmentRequests().getRequests())) {
            query.getEnrichmentRequests().getRequests().forEach(enrichmentRequest -> {
                if (enrichmentRequest instanceof GithubWebhookEnrichmentRequests.IssueEventRequest) {
                    enrichIssueEvent(levelopsClient, results, (GithubWebhookEnrichmentRequests.IssueEventRequest) enrichmentRequest);
                } else if (enrichmentRequest instanceof GithubWebhookEnrichmentRequests.PREventRequest) {
                    enrichPREvent(levelopsClient, results, (GithubWebhookEnrichmentRequests.PREventRequest) enrichmentRequest);
                } else if (enrichmentRequest instanceof GithubWebhookEnrichmentRequests.ProjectColumnEventRequest) {
                    enrichProjectColumnEvent(levelopsClient, results, (GithubWebhookEnrichmentRequests.ProjectColumnEventRequest) enrichmentRequest);
                } else if (enrichmentRequest instanceof GithubWebhookEnrichmentRequests.ProjectEventRequest) {
                    enrichProjectEvent(levelopsClient, results, (GithubWebhookEnrichmentRequests.ProjectEventRequest) enrichmentRequest);
                } else if (enrichmentRequest instanceof GithubWebhookEnrichmentRequests.PushEventRequest) {
                    getPushEvent(levelopsClient, results, (GithubWebhookEnrichmentRequests.PushEventRequest) enrichmentRequest);
                } else if (enrichmentRequest instanceof GithubWebhookEnrichmentRequests.ProjectCardEventRequest) {
                    processProjectCardEvent(results, (GithubWebhookEnrichmentRequests.ProjectCardEventRequest) enrichmentRequest);
                } else {
                    throw new IllegalArgumentException("The Request object is invalid.");
                }
            });
        }
        return results.stream()
                .filter(Objects::nonNull)
                .map(BasicData.mapper(GithubWebhookEnrichResponse.class));
    }

    private void processProjectCardEvent(List<GithubWebhookEnrichResponse> results,
                                         GithubWebhookEnrichmentRequests.ProjectCardEventRequest request) {
        try {
            results.add(GithubWebhookEnrichResponses.ProjectCardEventResponse.builder()
                    .webhookId(request.getWebhookId())
                    .build());
        } catch (Exception e) {
            log.error("processProjectCardEvent: Failed to process github card for GITHUB_PROJECT_CARD event with webhookId:{}",
                    request.getWebhookId(), e);
            results.add(GithubWebhookEnrichResponses.ProjectCardEventResponse.builder()
                    .webhookId(request.getWebhookId())
                    .error(e.getMessage())
                    .build());
        }
    }

    private void enrichIssueEvent(GithubClient client,
                                  List<GithubWebhookEnrichResponse> results,
                                  GithubWebhookEnrichmentRequests.IssueEventRequest request) {
        try {
            List<GithubIssueEvent> events = client.streamIssueTimelineEvents(request.getRepoId(), request.getIssueNumber())
                    .map(GithubConverters::parseGithubIssueTimelineEvent)
                    .collect(Collectors.toList());
            results.add(GithubWebhookEnrichResponses.IssueEventResponse.builder()
                    .events(events)
                    .issueNumber(request.getIssueNumber())
                    .repoId(request.getRepoId())
                    .webhookId(request.getWebhookId())
                    .build());
        } catch (Exception e) {
            log.error("enrichIssueEvent: Failed to enrich issue with issueNumber: {} and repo: {} for GITHUB_ISSUE event with webhookId:{}",
                    request.getIssueNumber(), request.getRepoId(), request.getWebhookId(), e);
            results.add(GithubWebhookEnrichResponses.IssueEventResponse.builder()
                    .error(e.getMessage())
                    .webhookId(request.getWebhookId())
                    .build());
        }
    }

    private void enrichPREvent(GithubClient client,
                               List<GithubWebhookEnrichResponse> results,
                               GithubWebhookEnrichmentRequests.PREventRequest request) {
        try {
            List<GithubCommit> commits = client.streamPullRequestCommits(request.getRepoId(), request.getPrNumber())
                    .map(GithubConverters::parseGithubApiCommit)
                    .collect(Collectors.toList());
            List<GithubReview> reviews = client.streamReviews(request.getRepoId(), request.getPrNumber())
                    .collect(Collectors.toList());
            List<String> patches = List.of();
            if (StringUtils.isNotBlank(request.getPrMergeCommitSha())) {
                GithubApiCommit mergeCommit;
                try {
                    mergeCommit = client.getPullRequestMergeCommit(request.getRepoId(), request.getPrMergeCommitSha());
                    patches = CollectionUtils.emptyIfNull(mergeCommit.getFiles()).stream()
                            .map(GithubCommitFile::getPatch)
                            .collect(Collectors.toList());
                } catch (GithubClientException e) {
                    log.warn("GithubClientException: Could not fetch PR merge commit", e);
                }
            }
            results.add(GithubWebhookEnrichResponses.PREventResponse.builder()
                    .prNumber(request.getPrNumber())
                    .reviews(reviews)
                    .commits(commits)
                    .patches(patches)
                    .repoId(request.getRepoId())
                    .repoOwner(request.getRepoOwner())
                    .webhookId(request.getWebhookId())
                    .build());
        } catch (Exception e) {
            log.error("enrichPREvent: Failed to enrich pr with prNumber: {} and repo: {} for GITHUB_PR event with webhookId:{}",
                    request.getPrNumber(), request.getRepoId(), request.getWebhookId(), e);
            results.add(GithubWebhookEnrichResponses.PREventResponse.builder()
                    .error(e.getMessage())
                    .webhookId(request.getWebhookId())
                    .build());
        }
    }

    private void enrichProjectColumnEvent(GithubClient client,
                                          List<GithubWebhookEnrichResponse> results,
                                          GithubWebhookEnrichmentRequests.ProjectColumnEventRequest request) {
        try {
            List<GithubProjectCard> projectCards = client.streamProjectColumnCards(request.getColumnId(), request.getIncludeArchived())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            results.add(GithubWebhookEnrichResponses.ProjectColumnEventResponse.builder()
                    .columnId(request.getColumnId())
                    .projectCards(projectCards)
                    .webhookId(request.getWebhookId())
                    .build());
        } catch (Exception e) {
            log.error("enrichProjectColumnEvent: Failed to enrich column with columnId: {} for GITHUB_PROJECT_COLUMN event with webhookId:{}",
                    request.getColumnId(), request.getWebhookId(), e);
            results.add(GithubWebhookEnrichResponses.ProjectColumnEventResponse.builder()
                    .error(e.getMessage())
                    .webhookId(request.getWebhookId())
                    .build());
        }
    }

    private void enrichProjectEvent(GithubClient client,
                                    List<GithubWebhookEnrichResponse> results,
                                    GithubWebhookEnrichmentRequests.ProjectEventRequest request) {
        try {
            List<GithubProjectColumn> githubProjectColumns = client.streamProjectColumns(request.getProjectId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            List<GithubProjectColumn> projectColumns = githubProjectColumns.stream()
                    .map(column -> enrichProjectColumn(client, column, request.getIncludeArchived()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            results.add(GithubWebhookEnrichResponses.ProjectEventResponse.builder()
                    .projectId(request.getProjectId())
                    .githubProjectColumns(projectColumns)
                    .webhookId(request.getWebhookId())
                    .build());
        } catch (Exception e) {
            log.error("enrichProjectEvent: Failed to enrich column with projectId: {} for GITHUB_PROJECT event with webhookId:{}",
                    request.getProjectId(), request.getWebhookId(), e);
            results.add(GithubWebhookEnrichResponses.ProjectEventResponse.builder()
                    .error(e.getMessage())
                    .webhookId(request.getWebhookId())
                    .build());
        }
    }

    private GithubProjectColumn enrichProjectColumn(GithubClient client,
                                                    GithubProjectColumn column,
                                                    Boolean includeArchived) {
        if (column == null || column.getId() == null) {
            return null;
        }
        List<GithubProjectCard> cards = client.streamProjectColumnCards(column.getId(), includeArchived)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return column.toBuilder()
                .cards(cards)
                .build();
    }

    private void getPushEvent(GithubClient levelopsClient,
                              List<GithubWebhookEnrichResponse> results,
                              GithubWebhookEnrichmentRequests.PushEventRequest request) {
        try {
            Repository repository = request.getRepository();
            List<GithubCommit> githubCommits = request.getCommitShas().stream()
                    .map(sha -> {
                        try {
                            return levelopsClient.getCommit(repository.generateId(), sha);
                        } catch (GithubClientException e) {
                            log.warn("Failed to get commit for repo=" + repository.generateId() + " and sha=" + sha, e);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            results.add(GithubWebhookEnrichResponses.PushEventResponse.builder()
                    .commits(githubCommits)
                    .repoId(repository.generateId())
                    .eventTime(request.getEventTime())
                    .webhookId(request.getWebhookId())
                    .build());
        } catch (Exception e) {
            log.error("getPushEvent: Failed to fetch commits with repoId: {} for GITHUB_PUSH event with webhookId:{}",
                    request.getRepository().generateId(), request.getWebhookId(), e);
            results.add(GithubWebhookEnrichResponses.PushEventResponse.builder()
                    .error(e.getMessage())
                    .webhookId(request.getWebhookId())
                    .build());
        }
    }

    @Override
    public Data<GithubWebhookEnrichResponse> fetchOne(GithubWebhookEnrichQuery query) {
        return null;
    }

    private GithubClient getLevelopsClient(IntegrationKey key) throws FetchException {
        try {
            return levelOpsClientFactory.get(key, false);
        } catch (GithubClientException e) {
            throw new FetchException("Failed to get client", e);
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubWebhookEnrichQuery.GithubWebhookEnrichQueryBuilder.class)
    public static class GithubWebhookEnrichQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("requests")
        GithubWebhookEnrichmentRequests enrichmentRequests;
    }
}
