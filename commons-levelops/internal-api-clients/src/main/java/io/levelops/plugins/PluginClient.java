package io.levelops.plugins;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Plugin;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import org.springframework.beans.factory.annotation.Qualifier;

public class PluginClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    @Builder
    public PluginClient(
            OkHttpClient client,
            ObjectMapper objectMapper,
            @Qualifier("internalApiUrl") String internalApiUri) {
        this.objectMapper = objectMapper;
        this.internalApiUri = internalApiUri;
        clientHelper = new ClientHelper<>(client, objectMapper, InternalApiClientException.class);
    }

    private HttpUrl.Builder baseUrlBuilder(String company) {
        return HttpUrl.parse(internalApiUri).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins");
    }

    public Plugin getByTool(String company, String tool) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("tools")
                .addPathSegment(tool)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, Plugin.class);
    }

}
