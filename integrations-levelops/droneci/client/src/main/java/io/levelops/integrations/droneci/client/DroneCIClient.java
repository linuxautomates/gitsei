package io.levelops.integrations.droneci.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.ClientHelper.BodyAndHeaders;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.droneci.models.DroneCIBuild;
import io.levelops.integrations.droneci.models.DroneCIBuildStepLog;
import io.levelops.integrations.droneci.models.DroneCIEnrichRepoData;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * DroneCI client: It should be used to make any calls to DroneCI
 */

@Log4j2
public class DroneCIClient {

    private final int DEFAULT_PAGE = 1;
    private final int DEFAULT_PAGE_SIZE = 100;
    private static final String BASE_PATH = "api";
    private static final String REPOS_PATH = "repos";
    private static final String BUILDS_PATH = "builds";
    private static final String LOGS_PATH = "logs";
    private static final String PAGE = "page";
    private static final String PER_PAGE = "per_page";

    private final ClientHelper<DroneCIClientException> clientHelper;
    private final String resourceUrl;

    public String getResourceUrl() {
        return resourceUrl;
    }

    @Builder
    public DroneCIClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, String resourceUrl) {
        this.resourceUrl = resourceUrl;
        this.clientHelper = ClientHelper.<DroneCIClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(DroneCIClientException.class)
                .build();
    }

    public Stream<DroneCIEnrichRepoData> streamRepositories() {
        return PaginationUtils.stream(1, 1, RuntimeStreamException.wrap(page -> {
            try {
                return getRepositories(page, 1000);
            } catch (DroneCIClientException e) {
                throw new RuntimeStreamException("Failed to get repositories after page " + page, e);
            }
        }));
    }

    public Stream<DroneCIBuild> streamRepoBuilds(String owner, String repoName) {
        return PaginationUtils.stream(1, 1, RuntimeStreamException.wrap(page -> {
            try {
                return getRepoBuilds(owner, repoName, page, DEFAULT_PAGE_SIZE);
            } catch (DroneCIClientException e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause instanceof HttpException) {
                    HttpException httpException = (HttpException) rootCause;
                    if (httpException.getCode() != null && httpException.getCode() == 404) {
                        log.warn("Can not fetch builds of repository: {} reason: {}", (owner + "/" + repoName), httpException.getMessage(), e);
                        return List.of();
                    }
                }
                throw new RuntimeStreamException("Failed to get builds after page " + page, e);
            }
        }));
    }

    public List<DroneCIBuildStepLog> buildStepLogs(String owner, String repoName, long buildNumber, long stageNumber, long stepNumber) {
        try {
            return getBuildStepLogs(owner, repoName, buildNumber, stageNumber, stepNumber);
        } catch (Exception e) {
            log.warn("buildStepLogs : Failed to list all step logs for : owner: "
                    + owner + " RepoName: " + repoName + " BuildNumber: "
                    + buildNumber + " Stage: " + stageNumber + " Step: " + stepNumber);
            return List.of();
        }
    }

    public List<DroneCIEnrichRepoData> getRepositories(int page, int pageSize) throws DroneCIClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(REPOS_PATH)
                .addQueryParameter(PAGE, String.valueOf(pageNumber))
                .addQueryParameter(PER_PAGE, String.valueOf(countPerPage))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<DroneCIEnrichRepoData[]> response = clientHelper.executeAndParseWithHeaders(request, DroneCIEnrichRepoData[].class);
        return Arrays.asList(response.getBody());
    }

    public List<DroneCIBuild> getRepoBuilds(String owner, String repoName, int page, int pageSize) throws DroneCIClientException {
        int pageNumber = (page == 0) ? DEFAULT_PAGE : page;
        int countPerPage = (pageSize == 0) ? DEFAULT_PAGE_SIZE : pageSize;
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(REPOS_PATH)
                .addPathSegment(owner)
                .addPathSegment(repoName)
                .addPathSegment(BUILDS_PATH)
                .addQueryParameter(PAGE, String.valueOf(pageNumber))
                .addQueryParameter(PER_PAGE, String.valueOf(countPerPage))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<DroneCIBuild[]> response = clientHelper.executeAndParseWithHeaders(request, DroneCIBuild[].class);
        return Arrays.asList(response.getBody());
    }

    public DroneCIBuild getBuildInfo(String owner, String repoName, long buildNumber) throws DroneCIClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(REPOS_PATH)
                .addPathSegment(owner)
                .addPathSegment(repoName)
                .addPathSegment(BUILDS_PATH)
                .addPathSegment(String.valueOf(buildNumber))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<DroneCIBuild> response = clientHelper.executeAndParseWithHeaders(request, DroneCIBuild.class);
        return response.getBody();
    }

    public List<DroneCIBuildStepLog> getBuildStepLogs(String owner, String repoName, long buildNumber, long stageNumber, long stepNumber) throws DroneCIClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(REPOS_PATH)
                .addPathSegment(owner)
                .addPathSegment(repoName)
                .addPathSegment(BUILDS_PATH)
                .addPathSegment(String.valueOf(buildNumber))
                .addPathSegment(LOGS_PATH)
                .addPathSegment(String.valueOf(stageNumber))
                .addPathSegment(String.valueOf(stepNumber))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<DroneCIBuildStepLog[]> response = clientHelper.executeAndParseWithHeaders(request, DroneCIBuildStepLog[].class);
        return Arrays.asList(response.getBody());
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }
}
