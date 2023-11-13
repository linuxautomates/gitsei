package io.levelops.integrations.bitbucket.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.bitbucket.client.BitbucketClient;
import io.levelops.integrations.bitbucket.client.BitbucketClientException;
import io.levelops.integrations.bitbucket.client.BitbucketClientFactory;
import io.levelops.integrations.bitbucket.models.BitbucketRepository;
import io.levelops.integrations.bitbucket.services.BitbucketFetchCommitsService;
import io.levelops.integrations.bitbucket.services.BitbucketFetchPullRequestsService;
import io.levelops.integrations.bitbucket.services.BitbucketFetchReposService;
import io.levelops.integrations.bitbucket.services.BitbucketFetchTagsService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class BitbucketRepositoryDataSource implements DataSource<BitbucketRepository, BitbucketRepositoryDataSource.BitbucketRepositoryQuery> {

    private static final int ONBOARDING_IN_DAYS = 30;

    private final BitbucketClientFactory clientFactory;
    private final BitbucketFetchReposService fetchReposService;
    private final BitbucketFetchCommitsService fetchCommitsService;
    private final BitbucketFetchTagsService fetchTagsService;
    private final BitbucketFetchPullRequestsService fetchPullRequestsService;
    private final EnumSet<Enrichment> enrichments;

    public enum Enrichment {
        COMMITS, PULL_REQUESTS, TAGS
    }

    public BitbucketRepositoryDataSource(BitbucketClientFactory clientFactory) {
        this(clientFactory, EnumSet.noneOf(Enrichment.class));
    }

    public BitbucketRepositoryDataSource(BitbucketClientFactory clientFactory,
                                         EnumSet<Enrichment> enrichments) {
        this.clientFactory = clientFactory;
        this.enrichments = enrichments;
        fetchCommitsService = new BitbucketFetchCommitsService();
        fetchTagsService = new BitbucketFetchTagsService();
        fetchReposService = new BitbucketFetchReposService();
        fetchPullRequestsService = new BitbucketFetchPullRequestsService();
    }

    private BitbucketClient getClient(IntegrationKey integrationKey) throws FetchException {
        BitbucketClient client = null;
        try {
            client = clientFactory.get(integrationKey, false);
        } catch (BitbucketClientException e) {
            throw new FetchException("Could not fetch Bitbucket client", e);
        }
        return client;
    }

    @Override
    public Data<BitbucketRepository> fetchOne(BitbucketRepositoryQuery query) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<BitbucketRepository>> fetchMany(BitbucketRepositoryQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();

        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        Instant to = DateUtils.toInstant(query.getTo(), Instant.now());
        BitbucketClient client = getClient(integrationKey);

        log.info("Fetching {} id={} Bitbucket repos between from={} and to={} (enrichments={})...", query.getIntegrationKey().getTenantId(), query.getIntegrationKey().getIntegrationId(), from, to, enrichments);

        Stream<Data<BitbucketRepository>> stream = fetchReposService.fetchRepos(client)
                .filter(repository -> {
                    if (CollectionUtils.isNotEmpty(query.getRepos())) {
                        return query.getRepos().contains(repository.getName());
                    } else {
                        return true;
                    }
                })
                .peek(repo -> log.info("Processing {} id={} Bitbucket repo {}/{} (enrichments={})", query.getIntegrationKey().getTenantId(), query.getIntegrationKey().getIntegrationId(), repo.getWorkspaceSlug(), repo.getName(), enrichments))
                .flatMap(repository -> parseAndEnrichRepository(client, repository, from, to, query.isFetchCommitFiles(), query.isFetchPrReviews()))
                .map(BasicData.mapper(BitbucketRepository.class));
        return stream.filter(Objects::nonNull);
    }

    private Stream<BitbucketRepository> parseAndEnrichRepository(BitbucketClient client,
                                                                 BitbucketRepository repository,
                                                                 Instant from,
                                                                 Instant to,
                                                                 boolean fetchCommitFiles,
                                                                 boolean fetchPrReviews) {
        if (enrichments.isEmpty()) {
            return Stream.of(repository);
        }
        Stream<BitbucketRepository> enrichedRepoStream = Stream.empty();
        if (enrichments.contains(Enrichment.COMMITS)) {
            Stream<BitbucketRepository> commits = fetchCommitsService.getRepoCommits(client, repository, from, to, fetchCommitFiles);
            enrichedRepoStream = Stream.concat(enrichedRepoStream, commits);
        }
        if (enrichments.contains(Enrichment.TAGS)) {
            Stream<BitbucketRepository> tags = fetchTagsService.getRepoTags(client, repository, from, to);
            enrichedRepoStream = Stream.concat(enrichedRepoStream, tags);
        }
        if (enrichments.contains(Enrichment.PULL_REQUESTS)) {
            Stream<BitbucketRepository> prs = fetchPullRequestsService.getRepoPrs(client, repository, from, to, fetchPrReviews);
            enrichedRepoStream = Stream.concat(enrichedRepoStream, prs);
        }
        return enrichedRepoStream;
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = BitbucketRepositoryQuery.BitbucketRepositoryQueryBuilder.class)
    public static class BitbucketRepositoryQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("from")
        Date from;
        @JsonProperty("to")
        Date to;

        @JsonProperty("repos")
        List<String> repos; // fetch specific repos (format: "owner/name")

        @JsonProperty("limit")
        Integer limit;

        // region commit & pull request flags
        @JsonProperty("fetch_commit_files")
        boolean fetchCommitFiles;
        @JsonProperty("fetch_pr_reviews")
        boolean fetchPrReviews;
        // endregion

    }
}
