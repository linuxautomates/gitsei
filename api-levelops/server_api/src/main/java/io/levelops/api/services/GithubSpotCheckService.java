package io.levelops.api.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.ExceptionPrintout;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.model.GithubApiCommit;
import io.levelops.integrations.github.models.GithubPullRequestSearchResult;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class GithubSpotCheckService {

    private final GithubClientFactory clientFactory;

    @Autowired
    public GithubSpotCheckService(InventoryService inventoryService, ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.clientFactory = new GithubClientFactory(inventoryService, objectMapper, okHttpClient, 0);
    }

    public GithubSpotCheckUserData fetchUserData(IntegrationKey integrationKey, String user, String from, String to, @Nullable Integer limit) {
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notBlank(integrationKey.getIntegrationId(), "integrationKey.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(integrationKey.getTenantId(), "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(user, "user cannot be null or empty.");
        Validate.notBlank(from, "from cannot be null or empty.");
        Validate.notBlank(to, "to cannot be null or empty.");

        try {
            return GithubSpotCheckUserData.builder()
                    .user(user)
                    .limit(limit)
                    .pullRequests(fetchPullRequests(integrationKey, user, from, to, limit))
                    .commitsAsAuthor(fetchCommits(integrationKey, user, "author", from, to, limit))
                    .commitsAsCommitter(fetchCommits(integrationKey, user, "committer", from, to, limit))
                    .build();
        } catch (GithubClientException e) {
            return GithubSpotCheckUserData.builder()
                    .error(ExceptionPrintout.fromThrowable(e))
                    .build();
        }
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubSpotCheckUserData.GithubSpotCheckUserDataBuilder.class)
    public static class GithubSpotCheckUserData {
        @JsonProperty("user")
        String user;

        @JsonProperty("limit")
        Integer limit;

        @JsonProperty("pull_requests")
        List<PullRequest> pullRequests;

        @JsonProperty("commits_as_author")
        List<Commit> commitsAsAuthor;

        @JsonProperty("commits_as_committer")
        List<Commit> commitsAsCommitter;

        @JsonProperty("error")
        ExceptionPrintout error;

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = PullRequest.PullRequestBuilder.class)
        public static class PullRequest {
            @JsonProperty("url")
            String url;
            @JsonProperty("title")
            String title;
            @JsonProperty("state")
            String state;
            @JsonProperty("created_at")
            String createdAt;
            @JsonProperty("updated_at")
            String updatedAt;
            @JsonProperty("closed_at")
            String closedAt;
            @JsonProperty("merged_at")
            String mergedAt;

            public static PullRequest fromGithubPullRequestSearchResult(GithubPullRequestSearchResult result) {
                return PullRequest.builder()
                        .url(StringUtils.firstNonBlank(result.getHtmlUrl(), result.getUrl()))
                        .title(result.getTitle())
                        .state(result.getState())
                        .createdAt(DateUtils.toString(result.getCreatedAt()))
                        .updatedAt(DateUtils.toString(result.getUpdatedAt()))
                        .closedAt(DateUtils.toString(result.getClosedAt()))
                        .mergedAt(DateUtils.toString(result.getPullRequest() != null ? result.getPullRequest().getMergedAt() : null))
                        .build();
            }
        }

        @Value
        @Builder(toBuilder = true)
        @JsonDeserialize(builder = Commit.CommitBuilder.class)
        public static class Commit {
            @JsonProperty("sha")
            String sha;
            @JsonProperty("repo")
            String repo;
            @JsonProperty("message")
            String message;
            @JsonProperty("authored_at")
            String authoredAt;
            @JsonProperty("committed_at")
            String committedAt;

            public static Commit fromApiCommit(GithubApiCommit result) {
                GithubApiCommit.Commit commit = result.getCommit();
                String message = null;
                String authoredAt = null;
                String committedAt = null;
                if (commit != null) {
                    message = commit.getMessage();
                    authoredAt = DateUtils.toString(commit.getAuthor() != null ? commit.getAuthor().getDate() : null);
                    committedAt = DateUtils.toString(commit.getCommitter() != null ? commit.getCommitter().getDate() : null);
                }
                return Commit.builder()
                        .sha(result.getSha())
                        .repo(result.getRepository() != null ? result.getRepository().getFullName() : null)
                        .message(message)
                        .authoredAt(authoredAt)
                        .committedAt(committedAt)
                        .build();
            }
        }
    }

    /**
     * Return PRs for a given user created between from and to
     *
     * @param user github login
     * @param from YYYY-MM-DD
     * @param to   YYYY-MM-DD
     */
    private List<GithubSpotCheckUserData.PullRequest> fetchPullRequests(IntegrationKey integrationKey, String user, String from, String to, @Nullable Integer limit) throws GithubClientException {
        GithubClient client = clientFactory.get(integrationKey, true);

        String query = String.format("is:pr author:%s created:%s..%s sort:created-desc", user, from, to);
        Stream<GithubSpotCheckUserData.PullRequest> stream = client.searchPullRequestsAndStreamResults(query, null, null)
                .map(GithubSpotCheckUserData.PullRequest::fromGithubPullRequestSearchResult);
        if (limit != null) {
            stream = stream.limit(limit);
        }
        return stream.collect(Collectors.toList());
    }

    /**
     * @param userCriterion "author" or "committer"
     */
    private List<GithubSpotCheckUserData.Commit> fetchCommits(IntegrationKey integrationKey, String user, String userCriterion, String from, String to, @Nullable Integer limit) throws GithubClientException {
        GithubClient client = clientFactory.get(integrationKey, true);

        String query = String.format("%s:%s %s-date:%s..%s", userCriterion, user, userCriterion, from, to);
        Stream<GithubSpotCheckUserData.Commit> stream = client.searchCommitAndStreamResults(query, userCriterion + "-date", "desc")
                .map(GithubSpotCheckUserData.Commit::fromApiCommit);
        if (limit != null) {
            stream = stream.limit(limit);
        }
        return stream.collect(Collectors.toList());
    }


}
