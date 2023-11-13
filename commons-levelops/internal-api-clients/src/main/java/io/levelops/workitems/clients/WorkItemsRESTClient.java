package io.levelops.workitems.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.WorkItem;
import io.levelops.commons.databases.models.database.workitems.CreateSnippetWorkitemRequestWithText;
import io.levelops.commons.databases.models.filters.WorkItemFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.HttpUrl.Builder;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorkItemsRESTClient implements WorkItemsClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @lombok.Builder
    public WorkItemsRESTClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, InternalApiClientException.class);
    }

    private Builder getBaseUrlBuilder(final String company){
        return HttpUrl.parse(apiBaseUrl).newBuilder()
            .addPathSegment("internal")
            .addPathSegment("v1")
            .addPathSegment("tenants")
            .addPathSegment(company)
            .addPathSegment("workitems");
    }

    @Override
    public WorkItem create(String company, WorkItem workItem) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .post(clientHelper.createJsonRequestBody(workItem))
            .build();
        return clientHelper.executeAndParse(request, WorkItem.class);
    }

    @Override
    public WorkItem createSnippetWorkItemMultipart(String company, MultipartFile createSnippetWorkItemRequest, MultipartFile snippetFile) throws InternalApiClientException, IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("snippet_workitems")
                .addPathSegment("multipart")
                .build();

        MultipartBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("json", createSnippetWorkItemRequest.getOriginalFilename(), okhttp3.RequestBody.create(createSnippetWorkItemRequest.getBytes()))
                .addFormDataPart("file", snippetFile.getOriginalFilename(), okhttp3.RequestBody.create(snippetFile.getBytes()))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        return clientHelper.executeAndParse(request, WorkItem.class);
    }

    @Override
    public WorkItem createSnippetWorkItem(String company, CreateSnippetWorkitemRequestWithText createSnippetWorkitemRequest) throws InternalApiClientException, IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("snippet_workitems")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(createSnippetWorkitemRequest))
                .build();

        return clientHelper.executeAndParse(request, WorkItem.class);
    }

    @Override
    public WorkItem getById(String company, UUID workItemId) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .addPathSegment(workItemId.toString())
            .build();

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        return clientHelper.executeAndParse(request, WorkItem.class);
    }

    @Override
    public WorkItem getByVanityId(String company, String vanityId) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .addPathSegment("vanity-id")
            .addPathSegment(vanityId)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        return clientHelper.executeAndParse(request, WorkItem.class);
    }

    @Override
    public Id update(String company, String submitter, WorkItem workItem) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .addPathSegment(workItem.getId())
            .addQueryParameter("submitter", submitter)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .put(clientHelper.createJsonRequestBody(workItem))
            .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    @Override
    public Id changeProductId(String company, UUID workItemId, String productId) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .addPathSegment(workItemId.toString())
            .addPathSegment("product")
            .addPathSegment(productId)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .patch(clientHelper.createJsonRequestBody(Map.of()))
            .build();
        return clientHelper.executeAndParse(request, Id.class);
    }

    @Override
    public Id changeParentId(String company, UUID workItemId, String parentWorkItemId) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .addPathSegment(workItemId.toString())
            .addPathSegment("parent")
            .addPathSegment(parentWorkItemId)
            .build();

        Request request = new Request.Builder()
            .url(url)
            .patch(clientHelper.createJsonRequestBody(Map.of()))
            .build();
        return clientHelper.executeAndParse(request, Id.class);
    }

    @Override
    public Id changeState(String company, UUID workItemId, String newState) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(workItemId.toString())
                .addPathSegment("state")
                .addPathSegment(newState)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .patch(clientHelper.createJsonRequestBody(Map.of()))
                .build();
        return clientHelper.executeAndParse(request, Id.class);
    }

    @Override
    public DeleteResponse delete(String company, UUID workItemId) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment(workItemId.toString())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        return clientHelper.executeAndParse(request, DeleteResponse.class);
    }

    @Override
    public BulkDeleteResponse bulkDelete(String company, List<UUID> ids) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(ids))
                .build();
        return clientHelper.executeAndParse(request, BulkDeleteResponse.class);
    }

    @Override
    public PaginatedResponse<WorkItem> list(String company, DefaultListRequest search) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
            .addPathSegment("list")
            .build();

        Request request = new Request.Builder()
            .url(url)
            .post(clientHelper.createJsonRequestBody(search))
            .build();
        return clientHelper.executeAndParse(request, mapper.getTypeFactory().constructParametricType(PaginatedResponse.class, WorkItem.class));
    }

    @Override
    public DbListResponse<DbAggregationResult> aggregate(String company, WorkItemFilter.Calculation calculation, DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("aggregate")
                .addQueryParameter("calculation", calculation.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                mapper.getTypeFactory().constructParametricType(DbListResponse.class, DbAggregationResult.class));
    }
}