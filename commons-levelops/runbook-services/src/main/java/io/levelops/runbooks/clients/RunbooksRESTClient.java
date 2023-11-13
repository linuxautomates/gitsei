package io.levelops.runbooks.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.runbooks.Runbook;
import io.levelops.commons.databases.models.database.runbooks.RunbookRun;
import io.levelops.commons.databases.models.database.runbooks.RunbookRunningNode;
import io.levelops.commons.databases.models.database.runbooks.RunbookVariable;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.runbooks.models.EvaluateNodeRequest;
import io.levelops.runbooks.models.EvaluateNodeResponse;
import io.levelops.runbooks.models.RunbookClientException;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunbooksRESTClient implements RunbookClient {
    private final ClientHelper<RunbookClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String apiBaseUri;

    public RunbooksRESTClient(final OkHttpClient client, final ObjectMapper objectMapper, final String apiBaseUrl) {
        this.objectMapper = objectMapper;
        this.apiBaseUri = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, objectMapper, RunbookClientException.class);
    }

    private Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUri).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("runbooks");
    }

    @Override
    public Map<String, String> createRun(final String company, final String runbookId, final String triggerType, final List<RunbookVariable> runbookData)
            throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(runbookId)
                .addPathSegment("runs")
                .addQueryParameter("trigger_type", triggerType)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(runbookData))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, String.class));
    }

    @Override
    public DbListResponse<RunbookRun> listRuns(String company, String runbookId, DefaultListRequest search)
            throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(runbookId)
                .addPathSegment("runs")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(search))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, RunbookRun.class));
    }

    @Override
    public Map<String, String> createRunbook(String company, Runbook runbook) throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company).build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(runbook))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructMapLikeType(HashMap.class, String.class, String.class));
    }

    @Override
    public DbListResponse<Runbook> listRunbooks(final String company, final DefaultListRequest search)
            throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(search))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, Runbook.class));
    }

    @Override
    public Boolean updateRunbookMetadata(String company, Runbook runbook) throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company).build();
        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(runbook))
                .build();
        clientHelper.executeRequest(request);
        return true;
    }

    @Override
    public DbListResponse<RunbookRunningNode> listRunningNodes(final String company, String runbookId, String runId, final DefaultListRequest search)
            throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(runbookId)
                .addPathSegment("runs")
                .addPathSegment(runId)
                .addPathSegment("nodes")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(search))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructParametricType(DbListResponse.class, RunbookRunningNode.class));
    }

    @Override
    public EvaluateNodeResponse evaluateNode(String company, EvaluateNodeRequest evaluateNodeRequest) throws RunbookClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("nodes")
                .addPathSegment("evaluate")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(evaluateNodeRequest))
                .build();
        return clientHelper.executeAndParse(request, EvaluateNodeResponse.class);
    }

}