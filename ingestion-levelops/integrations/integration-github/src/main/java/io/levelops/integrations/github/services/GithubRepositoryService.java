package io.levelops.integrations.github.services;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubRepositoryService {

    private final boolean enableCaching;
    private final GithubClientFactory clientFactory;

    @Value
    @Builder(toBuilder = true)
    protected static class GetRepoKey {
        IntegrationKey integrationKey;
        String repositoryId;
    }

    private final LoadingCache<GetRepoKey, GithubRepository> getRepoCache;
    private final LoadingCache<GetRepoKey, Map<String, Long>> languagesCache;


    @Value
    @Builder(toBuilder = true)
    protected static class StreamReposKey {
        IntegrationKey integrationKey;
        String org;
        boolean internalRepos;
    }

    private final LoadingCache<StreamReposKey, List<GithubRepository>> streamReposCache;

    private final LoadingCache<IntegrationKey, List<GithubRepository>> streamInstallationReposCache;

    public GithubRepositoryService(boolean enableCaching,
                                   long cacheMaxSize,
                                   long cacheExpiryInHours,
                                   GithubClientFactory clientFactory) {
        this.enableCaching = enableCaching;
        this.clientFactory = clientFactory;
        this.streamReposCache = initStreamReposCache(cacheMaxSize, cacheExpiryInHours);
        this.streamInstallationReposCache = initStreamInstallationReposCache(cacheMaxSize, cacheExpiryInHours);
        this.getRepoCache = initGetRepoCache(cacheMaxSize, cacheExpiryInHours);
        this.languagesCache = initLanguagesCache(cacheMaxSize, cacheExpiryInHours);
    }

    private LoadingCache<GetRepoKey, GithubRepository> initGetRepoCache(long cacheMaxSize,
                                                                        long cacheExpiryInHours) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize) // a repo is about .5kb on average
                .expireAfterWrite(cacheExpiryInHours, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public GithubRepository load(@NotNull GetRepoKey key) throws Exception {
                        return getRepositoryLive(key.getIntegrationKey(), key.getRepositoryId());
                    }
                });
    }

    private LoadingCache<GetRepoKey, Map<String, Long>> initLanguagesCache(long cacheMaxSize,
                                                                           long cacheExpiryInHours) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(cacheExpiryInHours, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public Map<String, Long> load(@NotNull GetRepoKey key) throws Exception {
                        return getLanguagesLive(key.getIntegrationKey(), key.getRepositoryId());
                    }
                });
    }

    private LoadingCache<StreamReposKey, List<GithubRepository>> initStreamReposCache(long cacheMaxSize,
                                                                                      long cacheExpiryInHours) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize) // a repo is about .5kb on average
                .expireAfterWrite(cacheExpiryInHours, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public List<GithubRepository> load(@NotNull StreamReposKey key) throws Exception {
                        List<GithubRepository> repos = streamRepositoriesLive(key.getIntegrationKey(), key.getOrg(), key.isInternalRepos())
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                        log.info("Loaded {} repositories for cache key {}, TTL {} hours", repos.size(), key, cacheExpiryInHours);
                        log.debug("All repos: {}", repos);
                        return repos;
                    }
                });
    }

    private LoadingCache<IntegrationKey, List<GithubRepository>> initStreamInstallationReposCache(long cacheMaxSize,
                                                                                                  long cacheExpiryInHours) {
        return CacheBuilder.newBuilder()
                .maximumSize(cacheMaxSize) // a repo is about .5kb on average
                .expireAfterWrite(cacheExpiryInHours, TimeUnit.HOURS)
                .build(new CacheLoader<>() {
                    @NotNull
                    @Override
                    public List<GithubRepository> load(@NotNull IntegrationKey key) throws Exception {
                        return streamInstallationRepositoriesLive(key)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());
                    }
                });
    }

    // region get repo

    public GithubRepository getRepository(IntegrationKey integrationKey, String repoId) throws FetchException {
        log.info("Getting repo id={} for {}", repoId, integrationKey);
        if (enableCaching) {
            return getRepositoryCache(integrationKey, repoId);
        }
        return getRepositoryLive(integrationKey, repoId);
    }

    private GithubRepository getRepositoryCache(IntegrationKey integrationKey, String repoId) throws FetchException {
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notBlank(integrationKey.getTenantId(), "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(integrationKey.getIntegrationId(), "integrationKey.getIntegrationId() cannot be null or empty.");
        Validate.notNull(repoId, "repoId cannot be null.");
        try {
            return getRepoCache.get(new GetRepoKey(integrationKey, repoId));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FetchException) {
                throw (FetchException) e.getCause();
            }
            throw new FetchException("Failed to get repo id=" + repoId + " for " + integrationKey, e);
        }
    }

    private GithubRepository getRepositoryLive(IntegrationKey integrationKey, String repoId) throws FetchException {
        log.debug("Fetching repo from cloud for id={}, {}", repoId, integrationKey);
        try {
            GithubClient githubClient = clientFactory.get(integrationKey, false);
            return githubClient.getRepository(repoId.toString());
        } catch (GithubClientException e) {
            throw new FetchException(e);
        }
    }

    // endregion

    // region stream repos

    // TODO: Move this to commons streamutils if the fix works
    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    public Stream<GithubRepository> streamAllRepositories(IntegrationKey integrationKey, String org) throws FetchException {
        var allRepos = Stream.concat(
                        streamRepositories(integrationKey, org, false),
                        streamRepositories(integrationKey, org, true))
                .sorted(Comparator.comparing(GithubRepository::getName))
                .filter(distinctByKey(GithubRepository::getFullName))
                .collect(Collectors.toList());
        log.info("{} repos found for {} {}", allRepos.size(), integrationKey, org);
        log.debug("All repos found for {} {}: {}", integrationKey, org, allRepos);
        return allRepos.stream();
    }

    public Stream<GithubRepository> streamRepositories(IntegrationKey integrationKey, String org, boolean internalRepos) throws FetchException {
        log.debug("Streaming repos for {}, org={}, internal={}", integrationKey, org, internalRepos);
        if (enableCaching) {
            return streamRepositoriesCached(integrationKey, org, internalRepos);
        }
        return streamRepositoriesLive(integrationKey, org, internalRepos);
    }

    private Stream<GithubRepository> streamRepositoriesCached(IntegrationKey integrationKey, String org, boolean internalRepos) throws FetchException {
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notBlank(integrationKey.getTenantId(), "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(integrationKey.getIntegrationId(), "integrationKey.getIntegrationId() cannot be null or empty.");
        Validate.notBlank(org, "org cannot be null or empty.");
        try {
            return streamReposCache.get(new StreamReposKey(integrationKey, org, internalRepos)).stream();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FetchException) {
                throw (FetchException) e.getCause();
            }
            throw new FetchException("Failed to stream " + (internalRepos ? "internal " : "") + "repos for org=" + org + " for " + integrationKey, e);
        }
    }


    private Stream<GithubRepository> streamRepositoriesLive(IntegrationKey integrationKey, String org, boolean internalRepos) throws FetchException {
        log.debug("Fetching repos from cloud for {} org={} internal={}", integrationKey, org, internalRepos);
        try {
            GithubClient githubClient = clientFactory.get(integrationKey, false);
            return githubClient.streamRepositories(org, internalRepos).sorted(Comparator.comparing(GithubRepository::getName));
        } catch (GithubClientException e) {
            throw new FetchException("Failed to stream repositories for " + integrationKey, e);
        }
    }

    // endregion

    // region installation repos

    public Stream<GithubRepository> streamInstallationRepositories(IntegrationKey integrationKey) throws FetchException {
        log.debug("Streaming installation repos for {}", integrationKey);
        if (enableCaching) {
            return streamInstallationRepositoriesCached(integrationKey);
        }
        return streamInstallationRepositoriesLive(integrationKey);
    }

    private Stream<GithubRepository> streamInstallationRepositoriesCached(IntegrationKey integrationKey) throws FetchException {
        Validate.notNull(integrationKey, "integrationKey cannot be null.");
        Validate.notBlank(integrationKey.getTenantId(), "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(integrationKey.getIntegrationId(), "integrationKey.getIntegrationId() cannot be null or empty.");
        try {
            return streamInstallationReposCache.get(integrationKey).stream();
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FetchException) {
                throw (FetchException) e.getCause();
            }
            throw new FetchException("Failed to stream installation repos for " + integrationKey, e);
        }
    }


    private Stream<GithubRepository> streamInstallationRepositoriesLive(IntegrationKey integrationKey) throws FetchException {
        log.debug("Fetching installation repos from cloud for {}", integrationKey);
        try {
            GithubClient githubClient = clientFactory.get(integrationKey, false);
            return githubClient.streamInstallationRepositories().sorted(Comparator.comparing(GithubRepository::getName));
        } catch (GithubClientException e) {
            throw new FetchException("Failed to stream installation repositories for " + integrationKey, e);
        }
    }
    // end region

    // region languages

    public Map<String, Long> getLanguages(IntegrationKey integrationKey, String repoId) throws FetchException {
        log.debug("Getting languages for id={}, {}", repoId, integrationKey);
        if (enableCaching) {
            return getLanguagesCached(integrationKey, repoId);
        }
        return getLanguagesLive(integrationKey, repoId);
    }

    private Map<String, Long> getLanguagesCached(IntegrationKey key, String repoId) throws FetchException {
        Validate.notNull(key, "integrationKey cannot be null.");
        Validate.notBlank(key.getTenantId(), "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(key.getIntegrationId(), "integrationKey.getIntegrationId() cannot be null or empty.");
        Validate.notNull(repoId, "repoId cannot be null.");
        try {
            return languagesCache.get(new GetRepoKey(key, repoId));
        } catch (ExecutionException e) {
            if (e.getCause() instanceof FetchException) {
                throw (FetchException) e.getCause();
            }
            throw new FetchException("Failed to fetch languages of Github repository for integrationId=" + key + " and repo=" + repoId, e);
        }
    }

    private Map<String, Long> getLanguagesLive(IntegrationKey key, String repoId) throws FetchException {
        log.debug("Fetching languages from cloud for id={}, {}", repoId, key);
        try {
            GithubClient githubClient = clientFactory.get(key, false);
            return githubClient.getRepositoryLanguages(repoId.toString());
        } catch (GithubClientException e) {
            log.warn("Failed to fetch languages of Github repository for integrationId={} and repo={}", key, repoId.toString(), e);
        }
        return Map.of();
    }

    // endregion
}
