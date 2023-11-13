package io.levelops.integrations.circleci.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.ClientHelper.BodyAndHeaders;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.circleci.models.CircleCIBuild;
import io.levelops.integrations.circleci.models.CircleCIProject;
import io.levelops.integrations.circleci.models.CircleCIStepActionLog;
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
 * CircleCI client: It should be used to make any calls to CircleCI
 */

@Log4j2
public class CircleCIClient {

    private static final String BASE_PATH = "api/v1.1";
    private static final String RECENT_BUILDS_PATH = "recent-builds";
    private static final String PROJECT_PATH = "project";

    private static final String PROJECTS_PATH = "projects";
    private static final String LIMIT = "limit";
    private static final int STARTING_OFFSET = 0;
    public static final int MAX_LIMIT = 100;
    private static final String OFFSET = "offset";

    private final ClientHelper<CircleCIClientException> clientHelper;
    private final String resourceUrl;

    @Builder
    public CircleCIClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, String resourceUrl) {
        this.resourceUrl = resourceUrl;
        this.clientHelper = ClientHelper.<CircleCIClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(CircleCIClientException.class)
                .build();
    }

    public Stream<CircleCIBuild> streamBuilds(){
        return PaginationUtils.stream(STARTING_OFFSET, MAX_LIMIT, RuntimeStreamException.wrap(offset -> {
            try {
                return getRecentBuilds(offset);
            } catch (CircleCIClientException e) {
                Throwable rootCause = ExceptionUtils.getRootCause(e);
                if (rootCause instanceof HttpException) {
                    HttpException httpException = (HttpException) rootCause;
                    if (httpException.getCode() != null
                            && httpException.getCode() == 400 &&
                            httpException.getMessage() != null &&
                            httpException.getMessage().contains("limit + offset may not exceed")) {
                        log.warn("CircleCI: can not fetch more builds after offset:{} and limit:{}", offset, (offset + MAX_LIMIT), e);
                        return List.of();
                    }
                }
                throw new RuntimeStreamException("Failed to get builds after offset " + offset, e);
            }
        }));
    }

    public List<CircleCIBuild> getRecentBuilds(int offset) throws CircleCIClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(RECENT_BUILDS_PATH)
                .addQueryParameter(LIMIT, Integer.toString(MAX_LIMIT))
                .addQueryParameter(OFFSET, Integer.toString(offset))
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<CircleCIBuild[]> page = clientHelper.executeAndParseWithHeaders(request, CircleCIBuild[].class);
        return Arrays.asList(page.getBody());
    }

    public CircleCIBuild getBuild(String projectSlug, int buildNumber) throws CircleCIClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(PROJECT_PATH)
                .addPathSegments(projectSlug)
                .addPathSegment(Integer.toString(buildNumber))
                .build();
        Request request = buildRequest(url);
        return clientHelper.executeAndParse(request, CircleCIBuild.class);
    }

    public List<CircleCIStepActionLog> getActionLogs(String url) throws CircleCIClientException {
        BodyAndHeaders<CircleCIStepActionLog[]> page = clientHelper.executeAndParseWithHeaders(new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build(), CircleCIStepActionLog[].class);
        return Arrays.asList(page.getBody());
    }

    public List<CircleCIProject> getProjects() throws CircleCIClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(BASE_PATH)
                .addPathSegment(PROJECTS_PATH)
                .build();
        Request request = buildRequest(url);
        BodyAndHeaders<CircleCIProject[]> page = clientHelper.executeAndParseWithHeaders(request, CircleCIProject[].class);
        return Arrays.asList(page.getBody());
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
