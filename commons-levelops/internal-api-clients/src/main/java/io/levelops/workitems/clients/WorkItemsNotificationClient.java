package io.levelops.workitems.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.WorkItemNotificationRequest;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import lombok.Value;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

import java.util.UUID;

public class WorkItemsNotificationClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @Builder
    public WorkItemsNotificationClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, InternalApiClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company){
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("workitems_notifications");
    }

    public QueueResponse queueRequest(String company, WorkItemNotificationRequest workItemNotificationRequest) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(workItemNotificationRequest))
                .build();
        return clientHelper.executeAndParse(request, QueueResponse.class);
    }

    public Id rebuildTextAttachmentCache(String company, UUID workItemId, UUID uploadId) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment("slack")
                .addPathSegment("rebuild_text_attachments_cache")
                .addPathSegment(workItemId.toString())
                .addPathSegment(uploadId.toString())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(null, new byte[]{}))
                .build();
        return clientHelper.executeAndParse(request, Id.class);
    }

    @Value
    @Builder
    @JsonDeserialize(builder = QueueResponse.QueueResponseBuilder.class)
    public static class QueueResponse{
        @JsonProperty("id")
        private final String id;
    }
}
