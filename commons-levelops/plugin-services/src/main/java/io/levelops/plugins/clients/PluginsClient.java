package io.levelops.plugins.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.plugins.models.PluginTrigger;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.IOException;

public class PluginsClient {

    private final String pluginTriggerApiUrl;
    private final ClientHelper<IOException> clientHelper;

    public PluginsClient(OkHttpClient client, ObjectMapper objectMapper, @Qualifier("pluginTriggerUrl") String pluginTriggerApiUrl) {
        this.pluginTriggerApiUrl = pluginTriggerApiUrl;
        this.clientHelper = new ClientHelper<>(client, objectMapper, IOException.class);
    }

    public String triggerPlugin(final PluginTrigger trigger) throws Exception {
        HttpUrl url = HttpUrl.parse(this.pluginTriggerApiUrl).newBuilder()
            .addPathSegment("internal")
            .addPathSegment("v1")
            .addPathSegments("plugins")
            .addPathSegment("trigger")
            .build();
        Request request = new Request.Builder()
            .url(url)
            .post(clientHelper.createJsonRequestBody(trigger))
            .build();
        return clientHelper.executeRequest(request);
    }
}