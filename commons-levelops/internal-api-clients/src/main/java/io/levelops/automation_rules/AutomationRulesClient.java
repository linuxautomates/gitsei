package io.levelops.automation_rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.automation_rules.AutomationRule;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

public class AutomationRulesClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @Builder
    public AutomationRulesClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
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
                .addPathSegment("automation_rules");
    }

    public Id createAutomationRule(String company, AutomationRule rule) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(rule))
                .build();

        String id = clientHelper.executeRequest(request);
        return mapper.readValue(id, Id.class);
    }

    public Id updateAutomationRule(String company, String ruleId, AutomationRule rule) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(ruleId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(rule))
                .build();

        String id = clientHelper.executeRequest(request);
        return mapper.readValue(id, Id.class);
    }

    public AutomationRule getAutomationRule(String company, String ruleId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(ruleId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, AutomationRule.class);
    }

    public DeleteResponse deleteAutomationRule(String company, String ruleId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(ruleId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();
        return clientHelper.executeAndParse(request, DeleteResponse.class);
    }

    public BulkDeleteResponse deleteAutomationRules(String company, List<String> ruleIds) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(ruleIds))
                .build();

       return clientHelper.executeAndParse(request, BulkDeleteResponse.class);
    }

    public PaginatedResponse<AutomationRule> listAutomationRules(String company, DefaultListRequest listRequest)
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
                        .constructParametricType(PaginatedResponse.class, AutomationRule.class));
    }
}
