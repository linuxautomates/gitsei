package io.levelops.triage.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.TriageRule;
import io.levelops.commons.databases.models.database.TriageRuleHit;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.triage.services.TriageService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

public class TriageRESTClient implements TriageService {

    private final String apiBaseUrl;
    private final ObjectMapper mapper;
    private final ClientHelper<InternalApiClientException> client;

    public TriageRESTClient(final OkHttpClient client,
                            final ObjectMapper mapper,
                            final String apiBaseURL) {
        this.client = ClientHelper.<InternalApiClientException>builder()
                .client(client)
                .objectMapper(mapper)
                .exception(InternalApiClientException.class)
                .build();
        this.apiBaseUrl = apiBaseURL;
        this.mapper = mapper;
    }

    private HttpUrl.Builder getBaseTriageRuleUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("triage_rules");
    }

    private HttpUrl.Builder getBaseTriageRuleResultUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("triage_rule_results");
    }

    @Override
    public String createTriageRule(String company, TriageRule rule) throws IOException {
        HttpUrl url = getBaseTriageRuleUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(rule))
                .build();

        return client.executeRequest(request);
    }

    @Override
    public String updateTriageRule(String company, String ruleId, TriageRule rule) throws IOException {
        HttpUrl url = getBaseTriageRuleUrlBuilder(company)
                .addPathSegment(ruleId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(client.createJsonRequestBody(rule))
                .build();

        return client.executeRequest(request);
    }

    @Override
    public TriageRule getTriageRule(String company, String ruleId) throws IOException {
        HttpUrl url = getBaseTriageRuleUrlBuilder(company)
                .addPathSegment(ruleId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return client.executeAndParse(request, TriageRule.class);
    }

    @Override
    public DeleteResponse deleteTriageRule(String company, String ruleId) throws IOException {
        HttpUrl url = getBaseTriageRuleUrlBuilder(company)
                .addPathSegment(ruleId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        return client.executeAndParse(request, DeleteResponse.class);
    }

    @Override
    public BulkDeleteResponse bulkDeleteTriageRules(String company, List<String> ids) throws IOException {
        HttpUrl url = getBaseTriageRuleUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .delete(client.createJsonRequestBody(ids))
                .build();
        return client.executeAndParse(request, BulkDeleteResponse.class);
    }

    @Override
    public DbListResponse<TriageRule> listTriageRules(String company, DefaultListRequest listRequest)
            throws IOException {
        HttpUrl url = getBaseTriageRuleUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(listRequest))
                .build();

        return client.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(DbListResponse.class, TriageRule.class));
    }

    @Override
    public DbListResponse<TriageRuleHit> listTriageRuleResults(String company, DefaultListRequest listRequest)
            throws IOException {
        HttpUrl url = getBaseTriageRuleResultUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(client.createJsonRequestBody(listRequest))
                .build();

        return client.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(DbListResponse.class, TriageRuleHit.class));
    }
}
