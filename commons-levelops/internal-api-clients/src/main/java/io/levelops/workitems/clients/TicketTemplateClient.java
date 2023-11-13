package io.levelops.workitems.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.TicketTemplate;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class TicketTemplateClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @Builder
    public TicketTemplateClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
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
                .addPathSegment("ticket_templates");
    }

    public PaginatedResponse<TicketTemplate> list(String company, DefaultListRequest defaultListRequest) throws InternalApiClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(defaultListRequest))
                .build();
        return clientHelper.executeAndParse(request, mapper.getTypeFactory()
                .constructParametricType(PaginatedResponse.class, TicketTemplate.class));
    }



}
