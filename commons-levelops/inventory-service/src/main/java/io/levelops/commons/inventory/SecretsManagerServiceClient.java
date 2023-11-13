package io.levelops.commons.inventory;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.retrying.RetryingInterceptor;
import io.levelops.commons.inventory.exceptions.SecretsManagerServiceClientException;
import io.levelops.commons.models.DefaultListRequest;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.TimeUnit;

@Log4j2
public class SecretsManagerServiceClient {

    public static final String DEFAULT_CONFIG_ID = "0";
    private final String secretsManagerServiceUrl;
    private final ClientHelper<SecretsManagerServiceClientException> clientHelper;

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = KeyValue.KeyValueBuilder.class)
    public static class KeyValue {

        @JsonProperty("key")
        String key;
        @JsonProperty("value")
        String value;
    }

    public SecretsManagerServiceClient(String secretsManagerServiceUrl, OkHttpClient client, ObjectMapper objectMapper) {
        this.secretsManagerServiceUrl = secretsManagerServiceUrl;
        this.clientHelper = ClientHelper.<SecretsManagerServiceClientException>builder()
                .client(client.newBuilder()
                        .addInterceptor(RetryingInterceptor.buildHttpCodeRetryer(10, 20, 100, TimeUnit.MILLISECONDS, 500))
                        .build())
                .objectMapper(objectMapper)
                .exception(SecretsManagerServiceClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(secretsManagerServiceUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("secrets");
    }

    public KeyValue getKeyValue(String tenant, String configId, String secretId) throws SecretsManagerServiceClientException {
        // GET /v1/secrets/{tenant}/configs/{configId}/keys/{key}
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(tenant)
                .addPathSegment("configs")
                .addPathSegment(configId)
                .addPathSegment("keys")
                .addPathSegment(secretId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, KeyValue.class);
    }

    public void storeKeyValue(String tenant, String configId, KeyValue kv) throws SecretsManagerServiceClientException {
        // POST /v1/secrets/{tenant}/configs/{configId}/keys
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(tenant)
                .addPathSegment("configs")
                .addPathSegment(configId)
                .addPathSegment("keys")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(kv))
                .build();
        clientHelper.executeRequest(request);
    }

    public void deleteKeyValue(String tenant, String configId, String secretId) throws SecretsManagerServiceClientException {
        // DELETE /v1/secrets/{tenant}/configs/{configId}/keys/{key}
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(tenant)
                .addPathSegment("configs")
                .addPathSegment(configId)
                .addPathSegment("keys")
                .addPathSegment(secretId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        clientHelper.executeRequest(request);
    }

    public void listKeys(String tenant, String configId, DefaultListRequest defaultListRequest) throws SecretsManagerServiceClientException {
        // POST /v1/secrets/{tenant}/configs/{configId}/keys/list
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(tenant)
                .addPathSegment("configs")
                .addPathSegment(configId)
                .addPathSegment("keys")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(defaultListRequest))
                .build();
        clientHelper.executeRequest(request);
    }

}
