package io.levelops.integrations.harnessng.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.integrations.harnessng.models.HarnessNGAPIResponse;
import io.levelops.integrations.harnessng.models.HarnessNGExecutionInputSet;
import io.levelops.integrations.harnessng.models.HarnessNGListAPIResponse;
import io.levelops.integrations.harnessng.models.HarnessNGPipeline;
import io.levelops.integrations.harnessng.models.HarnessNGPipelineExecution;
import io.levelops.integrations.harnessng.models.HarnessNGProject;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * HarnessNG client: It should be used to make any calls to HarnessNG
 */

@Log4j2
public class HarnessNGClient {

    private final int DEFAULT_PAGE = 0;
    private final int MAX_PAGE_SIZE = 1000;
    private final int PROJECT_MAX_PAGE_SIZE = 50;
    private static final String PROJECTS_PATH = "ng/api/projects";
    private static final String EXECUTIONS_PATH = "pipeline/api/pipelines/execution/summary";
    private static final String EXECUTIONS_DETAILS_PATH ="pipeline/api/pipelines/execution/v2/";
    private static final String EXECUTION_PATH = "pipeline/api/pipelines/execution";
    private static final String INPUT_SET_PATH = "inputsetV2";
    private static final String ACCOUNT_IDENTIFIER = "accountIdentifier";
    private static final String PROJECT_IDENTIFIER = "projectIdentifier";
    private static final String ORG_IDENTIFIER = "orgIdentifier";
    private static final String PAGE = "page";
    private static final String PAGE_INDEX = "pageIndex";
    private static final String PAGE_SIZE = "size";
    private static final String PROJECT_PAGE_SIZE = "pageSize";
    private static final String RENDER_FULL_BOTTOM_GRAPH = "renderFullBottomGraph";
    private static final String RESOLVE_EXPRESSIONS_QUERY_PARAM = "resolveExpressions";
    private final ClientHelper<HarnessNGClientException> clientHelper;
    private final String resourceUrl;

    public String getResourceUrl() {
        return resourceUrl;
    }

    @Builder
    public HarnessNGClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, String resourceUrl) {
        this.resourceUrl = resourceUrl;
        this.clientHelper = ClientHelper.<HarnessNGClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(HarnessNGClientException.class)
                .build();
    }

    public Stream<HarnessNGPipeline> streamExecutions(String accountIdentifier, String projectIdentifier, String orgIdentifier, Long from, Long to) {
        return PaginationUtils.stream(DEFAULT_PAGE, 1, RuntimeStreamException.wrap(page -> {
            try {
                return getExecutions(accountIdentifier, projectIdentifier, orgIdentifier, page, from, to);
            } catch (HarnessNGClientException e) {
                throw new RuntimeStreamException("Failed to get pipelines execution after page " + page, e);
            }
        }));
    }

    public Stream<HarnessNGProject> streamProjects(String accountIdentifier) {
        return PaginationUtils.stream(DEFAULT_PAGE, 1, RuntimeStreamException.wrap(page -> {
            try {
                return getProjects(accountIdentifier, page);
            } catch (HarnessNGClientException e) {
                throw new RuntimeStreamException("Failed to get projects after page " + page, e);
            }
        }));
    }

    public HarnessNGPipelineExecution getPipelineExecutionDetails(String accountIdentifier, String projectIdentifier, String orgIdentifier, String executionId, boolean renderFullBottomGraph) {
        try{
            return getExecutionDetails(accountIdentifier, projectIdentifier, orgIdentifier, executionId, renderFullBottomGraph);
        } catch (HarnessNGClientException e) {
            throw new RuntimeException("Failed to get pipeline executions details for executionId=" + executionId, e);
        }
    }

    public List<HarnessNGProject> getProjects(String accountIdentifier, int pageIndex) throws HarnessNGClientException {
        int pageNumber = (pageIndex < 0) ? DEFAULT_PAGE : pageIndex;
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(PROJECTS_PATH)
                .addQueryParameter(ACCOUNT_IDENTIFIER, accountIdentifier)
                .addQueryParameter(PAGE_INDEX, String.valueOf(pageNumber))
                .addQueryParameter(PROJECT_PAGE_SIZE, String.valueOf(PROJECT_MAX_PAGE_SIZE))
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGProject>>> response = clientHelper.executeAndParseWithHeaders(request, clientHelper.getObjectMapper().constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGProject>>>() {}));
        return response.getBody().getData().getContent();
    }

    public List<HarnessNGPipeline> getExecutions(String accountIdentifier, String projectIdentifier, String orgIdentifier, int page, Long from, Long to) throws HarnessNGClientException {
        int pageNumber = (page < 0) ? DEFAULT_PAGE : page;
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(EXECUTIONS_PATH)
                .addQueryParameter(ACCOUNT_IDENTIFIER, accountIdentifier)
                .addQueryParameter(ORG_IDENTIFIER, orgIdentifier)
                .addQueryParameter("sort", "startTs,DESC")
                .addQueryParameter(PROJECT_IDENTIFIER, projectIdentifier)
                .addQueryParameter(PAGE, String.valueOf(pageNumber))
                .addQueryParameter(PAGE_SIZE, String.valueOf(MAX_PAGE_SIZE))
                .build();
        HashMap<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("filterType", "PipelineExecution");
        if (from != null && to != null) {
            bodyMap.put("timeRange", Map.of("startTime", from, "endTime", to));
        }
        Request request = buildPostRequest(url, clientHelper.createJsonRequestBody(bodyMap));
        ClientHelper.BodyAndHeaders<HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGPipeline>>> response = clientHelper.executeAndParseWithHeaders(request, clientHelper.getObjectMapper().constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGListAPIResponse<HarnessNGPipeline>>>() {}));
        return response.getBody().getData().getContent();
    }

    public HarnessNGPipelineExecution getExecutionDetails(String accountIdentifier, String projectIdentifier, String orgIdentifier, String executionId, boolean renderFullBottomGraph) throws HarnessNGClientException {
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegment(EXECUTIONS_DETAILS_PATH)
                .addPathSegment(executionId)
                .addQueryParameter(ACCOUNT_IDENTIFIER, accountIdentifier)
                .addQueryParameter(ORG_IDENTIFIER, orgIdentifier)
                .addQueryParameter(PROJECT_IDENTIFIER, projectIdentifier)
                .addQueryParameter(RENDER_FULL_BOTTOM_GRAPH, String.valueOf(renderFullBottomGraph))
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<HarnessNGAPIResponse<HarnessNGPipelineExecution>> response = clientHelper.executeAndParseWithHeaders(request, clientHelper.getObjectMapper().constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGPipelineExecution>>() {}));
        return response.getBody().getData();
    }

    public HarnessNGExecutionInputSet getExecutionInputSet(String accountIdentifier, String projectIdentifier, String orgIdentifier, String executionId, boolean resolveExpressions) throws HarnessNGClientException {
        // <url>/pipeline/api/pipelines/execution/<id>/inputsetV2
        var url = Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder()
                .addPathSegments(EXECUTION_PATH)
                .addPathSegment(executionId)
                .addPathSegment(INPUT_SET_PATH)
                .addQueryParameter(ACCOUNT_IDENTIFIER, accountIdentifier)
                .addQueryParameter(ORG_IDENTIFIER, orgIdentifier)
                .addQueryParameter(PROJECT_IDENTIFIER, projectIdentifier)
                .addQueryParameter(RESOLVE_EXPRESSIONS_QUERY_PARAM, String.valueOf(resolveExpressions))
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<HarnessNGAPIResponse<HarnessNGExecutionInputSet>> response = clientHelper.executeAndParseWithHeaders(request,
                clientHelper.getObjectMapper().constructType(new TypeReference<HarnessNGAPIResponse<HarnessNGExecutionInputSet>>() {}));
        return response.getBody().getData();
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }

    @NotNull
    private Request buildPostRequest(HttpUrl url, RequestBody body) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(body)
                .build();
    }
}
