package io.levelops.integrations.github.services;

import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.model.GithubApiCommit;
import io.levelops.integrations.github.model.GithubConverters;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubReview;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Log4j2
public class GithubPullRequestService {

    public List<GithubPullRequest> getPullRequests(GithubClient client, String repoId, Instant from, Instant to, boolean fetchPrCommits, boolean fetchPrReviews, boolean fetchPrPatches) {

        MutableInt nbPullRequests = new MutableInt(0);
        MutableInt nbReviews = new MutableInt(0);
        return StreamSupport.stream(client.streamPullRequests(repoId).spliterator(), false)
                .filter(pr -> pr.getUpdatedAt() != null && pr.getUpdatedAt().toInstant().isBefore(to))
                .takeWhile(pr -> pr.getUpdatedAt() != null && pr.getUpdatedAt().toInstant().isAfter(from))
                .map(pr -> enrichPullRequest(client, repoId, pr, fetchPrCommits, fetchPrReviews, fetchPrPatches))
                .filter(Objects::nonNull)
                .peek(pr -> {
                    nbPullRequests.increment();
                    nbReviews.add(CollectionUtils.size(pr.getReviews()));
                    if (nbPullRequests.getValue() % 30 == 0) {
                        log.info("Processed PRs for repo={}: nb_prs={} nb_reviews={} (ts={})", repoId, nbPullRequests.getValue(), nbReviews.getValue(), pr.getCreatedAt());
                    }
                })
                .collect(Collectors.toList());
    }

    private GithubPullRequest enrichPullRequest(GithubClient client, String repoId, GithubPullRequest pr, boolean fetchPrCommits, boolean fetchPrReviews, boolean fetchPrPatches) {
        if (pr == null || pr.getNumber() == null) {
            return pr;
        }
        List<GithubReview> reviews = Collections.emptyList();
        if (fetchPrReviews) {
            var prReviews = client.streamReviews(repoId, pr.getNumber());
            var issueComments = client.streamIssueComments(repoId, pr.getNumber());
            reviews = Stream.concat(prReviews, issueComments).collect(Collectors.toList());
        }
        List<GithubCommit> commits = fetchPrCommits ? client.streamPullRequestCommits(repoId, pr.getNumber())
                .map(GithubConverters::parseGithubApiCommit)
                .collect(Collectors.toList())
                : Collections.emptyList();

        List<String> patches = null;
        GithubApiCommit mergeCommit = null;
        if (fetchPrPatches && StringUtils.isNotBlank(pr.getMergeCommitSha())) {
            try {
                mergeCommit = client.getPullRequestMergeCommit(repoId, pr.getMergeCommitSha());
                patches = CollectionUtils.emptyIfNull(mergeCommit.getFiles()).stream().map(x -> x.getPatch()).collect(Collectors.toList());
            } catch (GithubClientException e) {
                log.info("Failed to get pr merge commit, ");
            }
        }
        GithubPullRequest.GithubPullRequestBuilder bldr = pr.toBuilder()
                .reviews(reviews)
                .commits(commits);
        if (CollectionUtils.isNotEmpty(patches)) {
            bldr.patches(patches);
        }
        if (mergeCommit != null) {
            log.info("Merge commit processing for sha={}", pr.getMergeCommitSha());
            bldr.mergeCommit(GithubConverters.parseGithubApiCommit(mergeCommit));
        }
        return bldr.build();
    }
}
