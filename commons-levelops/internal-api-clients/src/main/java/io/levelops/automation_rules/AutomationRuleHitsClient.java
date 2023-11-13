package io.levelops.automation_rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRuleHit;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class AutomationRuleHitsClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @Builder
    public AutomationRuleHitsClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
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
                .addPathSegment("automation_rule_hits");
    }

    public Id createAutomationRuleHit(String company, AutomationRuleHit ruleHit) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(ruleHit))
                .build();

        String id = clientHelper.executeRequest(request);
        return mapper.readValue(id, Id.class);
    }

    public Id updateAutomationRuleHit(String company, String ruleHitId, AutomationRuleHit ruleHit) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(ruleHitId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(ruleHit))
                .build();

        String id = clientHelper.executeRequest(request);
        return mapper.readValue(id, Id.class);
    }

    public AutomationRuleHit getAutomationRuleHit(String company, String ruleHitId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(ruleHitId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, AutomationRuleHit.class);
    }

    public void deleteAutomationRuleHit(String company, String ruleHitId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(ruleHitId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        clientHelper.executeRequest(request);
        return ;
    }

    public DbListResponse<AutomationRuleHit> listAutomationRuleHits(String company, DefaultListRequest listRequest)
            throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(listRequest))
                .build();

        return clientHelper.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(DbListResponse.class, AutomationRuleHit.class));
    }
}
