package io.levelops.integrations.bitbucket_server.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClient;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientException;
import io.levelops.integrations.bitbucket_server.client.BitbucketServerClientFactory;
import io.levelops.integrations.bitbucket_server.models.BitbucketServerEnrichedProjectData;
import io.levelops.integrations.bitbucket_server.services.BitbucketServerFetchCommitsService;
import io.levelops.integrations.bitbucket_server.services.BitbucketServerFetchPullRequestsService;
import io.levelops.integrations.bitbucket_server.services.BitbucketServerFetchReposService;
import io.levelops.integrations.bitbucket_server.services.BitbucketServerFetchTagsService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Stream;

@Log4j2
public class BitbucketServerProjectDataSource implements DataSource<BitbucketServerEnrichedProjectData, BitbucketServerProjectDataSource.BitbucketServerProjectQuery> {

    private static final int ONBOARDING_IN_DAYS = 30;

    private final BitbucketServerClientFactory clientFactory;
    private final BitbucketServerFetchReposService fetchReposService;
    private final BitbucketServerFetchCommitsService fetchCommitsService;
    private final BitbucketServerFetchPullRequestsService fetchPullRequestsService;
    private final BitbucketServerFetchTagsService fetchTagsService;
    private final EnumSet<Enrichment> enrichments;

    public enum Enrichment {
        COMMITS, PULL_REQUESTS, REPOSITORIES, TAGS;
    }

    public BitbucketServerProjectDataSource(BitbucketServerClientFactory clientFactory, EnumSet<Enrichment> enrichments) {
        this.clientFactory = clientFactory;
        this.enrichments = enrichments;
        fetchCommitsService = new BitbucketServerFetchCommitsService();
        fetchReposService = new BitbucketServerFetchReposService();
        fetchPullRequestsService = new BitbucketServerFetchPullRequestsService();
        fetchTagsService = new BitbucketServerFetchTagsService();
    }

    private BitbucketServerClient getClient(IntegrationKey integrationKey) throws FetchException {
        BitbucketServerClient client = null;
        try {
            client = clientFactory.get(integrationKey, false);
        } catch (BitbucketServerClientException e) {
            throw new FetchException("Could not fetch Bitbucket server client", e);
        }
        return client;
    }

    @Override
    public Data<BitbucketServerEnrichedProjectData> fetchOne(BitbucketServerProjectQuery query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<BitbucketServerEnrichedProjectData>> fetchMany(BitbucketServerProjectQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();

        long from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS))).toEpochMilli();
        long to = DateUtils.toInstant(query.getTo(), Instant.now()).toEpochMilli();

        BitbucketServerClient client = getClient(integrationKey);
        log.info("Fetching {} id={} Bitbucket Server repos between from={} and to={}",
                query.getIntegrationKey().getTenantId(), query.getIntegrationKey().getIntegrationId(), from, to);
        Stream<BitbucketServerEnrichedProjectData> dataStream;
        if (enrichments.contains(Enrichment.COMMITS)) {
            dataStream = fetchCommitsService.fetchCommits(client, from, to, query.getRepos(), query.getProjects(), query.isFetchCommitFiles());
        } else if (enrichments.contains(Enrichment.PULL_REQUESTS)) {
            dataStream = fetchPullRequestsService.fetchPullRequests(client, from, to, query.getRepos(), query.getProjects(), query.isFetchPrReviews(), query.isFetchPRCommits(), query.isFetchCommitFiles());
        } else if (enrichments.contains(Enrichment.TAGS)) {
            dataStream = fetchTagsService.fetchTags(client, query.getRepos());
        } else if (enrichments.contains(Enrichment.REPOSITORIES)) {
            dataStream = fetchReposService.fetchRepos(client, query.getRepos());
        } else {
            dataStream = Stream.empty();
        }
        return dataStream
                .map(BasicData.mapper(BitbucketServerEnrichedProjectData.class));
    }


    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketServerProjectDataSource.BitbucketServerProjectQuery.class)
    public static class BitbucketServerProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("from")
        Date from;
        @JsonProperty("to")
        Date to;

        @JsonProperty("repos")
        List<String> repos; // fetch specific repos (format: "name" NOT "owner/name")

        @JsonProperty("projects")
        List<String> projects;

        // region commit & pull request flags
        @JsonProperty("fetch_commit_files")
        boolean fetchCommitFiles;
        @JsonProperty("fetch_pr_reviews")
        boolean fetchPrReviews;
        @JsonProperty("fetch_pr_commits")
        boolean fetchPRCommits;
        // endregion

    }
}
