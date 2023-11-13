package io.levelops.io.levelops.scm_repo_mapping.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.io.levelops.scm_repo_mapping.models.ScmRepoMappingResponse;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ScmRepoMappingClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @Builder
    public ScmRepoMappingClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, InternalApiClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("scm_repo_mapping");
    }

    public ScmRepoMappingResponse getScmRepoMapping(String company, String integrationId) throws InternalApiClientException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("repo_mapping")
                .addQueryParameter("integration_id", integrationId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, ScmRepoMappingResponse.class);
    }
}
