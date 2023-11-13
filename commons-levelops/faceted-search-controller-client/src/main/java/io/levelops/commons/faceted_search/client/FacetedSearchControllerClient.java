package io.levelops.commons.faceted_search.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.faceted_search.models.IndexType;
import io.levelops.commons.models.ListResponse;
import io.levelops.commons.tenant_management.models.JobResult;
import io.levelops.commons.tenant_management.models.JobStatus;
import io.levelops.commons.tenant_management.models.Offsets;
import io.levelops.commons.tenant_management.models.TenantIndexSnapshot;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FacetedSearchControllerClient {
    private final ClientHelper<FacetedSearchControllerClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String facetedSearchControllerServiceUri;

    @Builder
    public FacetedSearchControllerClient(final OkHttpClient client, final ObjectMapper objectMapper, final String facetedSearchControllerServiceUri) {
        this.objectMapper = objectMapper;
        this.facetedSearchControllerServiceUri = facetedSearchControllerServiceUri;
        this.clientHelper = new ClientHelper<>(client, objectMapper, FacetedSearchControllerClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder() {
        return HttpUrl.parse(facetedSearchControllerServiceUri).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("worker-callback")
                .addPathSegment("jobs");
    }

    public List<TenantIndexSnapshot> getJobs(int count) throws FacetedSearchControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment("request")
                .addQueryParameter("count", String.valueOf(count));

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        ListResponse<TenantIndexSnapshot> response = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(ListResponse.class, TenantIndexSnapshot.class));
        return response.getRecords();
    }

    public Id updateJobStatus(UUID id, JobStatus status) throws FacetedSearchControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment(id.toString())
                .addPathSegment("status")
                .addPathSegment(status.toString());

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .patch(clientHelper.createJsonRequestBody(Map.of()))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public Id updateJobStatus(UUID id, JobResult result) throws FacetedSearchControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment(id.toString())
                .addPathSegment("status");

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .patch(clientHelper.createJsonRequestBody(result))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public Id updateJobLatestOffset(UUID id, Offsets latestOffsets) throws FacetedSearchControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment(id.toString())
                .addPathSegment("latest-offsets");

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .patch(clientHelper.createJsonRequestBody(latestOffsets))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public List<TenantIndexSnapshot> getDeleteIndexJobs(int count) throws FacetedSearchControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment("delete-index")
                .addPathSegment("request")
                .addQueryParameter("count", String.valueOf(count));

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        ListResponse<TenantIndexSnapshot> response = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(ListResponse.class, TenantIndexSnapshot.class));
        return response.getRecords();
    }

    public Id updateDeleteIndexJobStatus(UUID id, JobStatus status) throws FacetedSearchControllerClientException {
        HttpUrl.Builder urlBuilder = getBaseUrlBuilder()
                .addPathSegment("delete-index")
                .addPathSegment(id.toString())
                .addPathSegment("status")
                .addPathSegment(status.toString());

        HttpUrl url = urlBuilder.build();
        Request request = new Request.Builder()
                .url(url)
                .patch(clientHelper.createJsonRequestBody(Map.of()))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public static class FacetedSearchControllerClientException extends Exception {
        public FacetedSearchControllerClientException() {
        }

        public FacetedSearchControllerClientException(String message) {
            super(message);
        }

        public FacetedSearchControllerClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public FacetedSearchControllerClientException(Throwable cause) {
            super(cause);
        }
    }
}
