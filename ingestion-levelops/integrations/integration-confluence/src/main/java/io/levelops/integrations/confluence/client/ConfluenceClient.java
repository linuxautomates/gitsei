package io.levelops.integrations.confluence.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.confluence.models.ConfluenceSearchResponse;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.function.Supplier;

public class ConfluenceClient {

    private final ClientHelper<ConfluenceClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final Supplier<String> confluenceUrl;

    @Builder
    public ConfluenceClient(OkHttpClient okHttpClient, ObjectMapper objectMapper, Supplier<String> confluenceUrl) {
        this.objectMapper = objectMapper;
        this.confluenceUrl = confluenceUrl;
        clientHelper = ClientHelper.<ConfluenceClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(ConfluenceClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(confluenceUrl.get()).newBuilder()
                .addPathSegment("wiki")
                .addPathSegment("rest")
                .addPathSegment("api");
    }

    public ConfluenceSearchResponse search(String cql, Integer start, Integer limit) throws ConfluenceClientException {
        int startParam = MoreObjects.firstNonNull(start, 0);
        int limitParam = MoreObjects.firstNonNull(limit, 25);
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("search")
                .addEncodedQueryParameter("cql", cql)
                .addEncodedQueryParameter("start", Integer.toString(startParam))
                .addEncodedQueryParameter("limit", Integer.toString(limitParam))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        return clientHelper.executeAndParse(request, ConfluenceSearchResponse.class);
    }


}
