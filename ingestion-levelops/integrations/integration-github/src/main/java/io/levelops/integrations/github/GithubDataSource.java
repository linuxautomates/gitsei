package io.levelops.integrations.github;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.DataQuery;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.client.GithubClient;
import io.levelops.integrations.github.model.GithubData;
import io.levelops.integrations.github.models.GithubPullRequest;
import io.levelops.integrations.github.models.GithubTag;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class GithubDataSource implements DataSource<GithubData, GithubDataSource.GithubQuery> {

    private final GithubClientFactory levelOpsClientFactory;
    private final FetchDataType fetchDataType;

    public enum FetchDataType {
        PULL_REQUESTS, TAGS
    }

    public GithubDataSource(GithubClientFactory levelOpsClientFactory,
                            FetchDataType fetchDataType) {
        this.levelOpsClientFactory = levelOpsClientFactory;
        this.fetchDataType = fetchDataType;
    }

    @Override
    public Data<GithubData> fetchOne(GithubDataSource.GithubQuery query) throws FetchException {
        IntegrationKey integrationKey = query.getIntegrationKey();
        GithubClient levelopsClient = getLevelopsClient(integrationKey);
        int DEFAULT_PER_PAGE = 100;
        if (fetchDataType.name().equalsIgnoreCase(FetchDataType.PULL_REQUESTS.name())) {
            if (StringUtils.isNotEmpty(query.getPrNumber())) {
                try {
                    GithubPullRequest pullRequest = levelopsClient.getPullRequest(query.getRepoOwner(), query.getRepoName(), query.getPrNumber());
                    GithubData githubData = GithubData.builder().pullRequest(pullRequest).build();
                    return BasicData.of(GithubData.class, githubData);
                } catch (GithubClientException e) {
                    log.error("Unable to fetch pull request with pr number=" + query.getPrNumber(), e);
                    throw new FetchException("Failed to get pull request with PR number=" + query.getPrNumber(), e);
                }
            } else {
                throw new IllegalArgumentException("The Pull Request number is missing.");
            }
        } else if(fetchDataType.name().equalsIgnoreCase(FetchDataType.TAGS.name())) {
            if (StringUtils.isNotEmpty(query.getRepoName())) {
                    List<GithubTag> githubTags = levelopsClient.streamTags(query.getRepoOwner(),
                            query.getRepoName(), DEFAULT_PER_PAGE).collect(Collectors.toList());
                    GithubData githubData = GithubData.builder().githubTags(githubTags).build();
                    return BasicData.of(GithubData.class, githubData);
            } else {
                throw new IllegalArgumentException("The repo name is missing.");
            }
        }
        else {
            throw new IllegalArgumentException("Invalid data types: " + fetchDataType.name());
        }
    }

    @Override
    public Stream<Data<GithubData>> fetchMany(GithubDataSource.GithubQuery query) {
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
    @JsonDeserialize(builder = GithubQuery.GithubQueryBuilder.class)
    public static class GithubQuery implements DataQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        // Pull request related fields
        @JsonProperty("repo_name")
        String repoName;
        @JsonProperty("repo_owner")
        String repoOwner;
        @JsonProperty("pr_number")
        String prNumber;
        // end region
    }
}
