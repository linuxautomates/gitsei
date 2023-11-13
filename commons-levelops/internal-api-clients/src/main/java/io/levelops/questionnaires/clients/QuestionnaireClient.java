package io.levelops.questionnaires.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireDTO;
import io.levelops.commons.databases.models.database.questionnaire.QuestionnaireListItemDTO;
import io.levelops.commons.databases.models.filters.QuestionnaireAggFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.DeleteResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class QuestionnaireClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;
    private final String exportServiceUri;

    @Builder
    public QuestionnaireClient(
            OkHttpClient client, 
            ObjectMapper objectMapper, 
            @Qualifier("internalApiUrl") String internalApiUri, 
            @Value("${EXPORT_SERVICE_URL:http://export-service}") String exportServiceUri) {
        this.objectMapper = objectMapper;
        this.internalApiUri = internalApiUri;
        this.exportServiceUri = exportServiceUri;
        clientHelper = new ClientHelper<>(client, objectMapper, InternalApiClientException.class);
    }

    private HttpUrl.Builder baseUrlBuilder(String company) {
        return HttpUrl.parse(internalApiUri).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("questionnaires");
    }

    public Id create(String company, QuestionnaireDTO questionnaire) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company).build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(questionnaire))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public QuestionnaireDTO get(String company, UUID id) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(id.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, QuestionnaireDTO.class);
    }

    public QuestionnaireDTO update(String company, String submitter, UUID id, QuestionnaireDTO questionnaire) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(id.toString())
                .addQueryParameter("submitter", submitter)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(questionnaire))
                .build();

        return clientHelper.executeAndParse(request, QuestionnaireDTO.class);
    }

    public DeleteResponse delete(String company, UUID id) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(id.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        return clientHelper.executeAndParse(request, DeleteResponse.class);
    }

    public BulkDeleteResponse bulkDelete(String company, List<UUID> ids) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(ids))
                .build();
        return clientHelper.executeAndParse(request, BulkDeleteResponse.class);
    }

    public InputStream export(String company, List<Map<String, Object>> requests) throws InternalApiClientException {
        HttpUrl url = HttpUrl.parse(exportServiceUri).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("assessments")
                .addPathSegment("download")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(requests))
                .build();

        return clientHelper.executeRequestForBinary(request);
    }

    public PaginatedResponse<QuestionnaireListItemDTO> list(String company, DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, QuestionnaireListItemDTO.class));
    }

    public PaginatedResponse<DbAggregationResult> aggregate(String company,
                                                            QuestionnaireAggFilter.Calculation calculation,
                                                            DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("aggregate")
                .addQueryParameter("calculation", calculation.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbAggregationResult.class));
    }

    public PaginatedResponse<DbAggregationResult> aggregatePaginated(String company,
                                                                        QuestionnaireAggFilter.Calculation calculation,
                                                                        DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("aggregate_paginated")
                .addQueryParameter("calculation", calculation.toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, DbAggregationResult.class));
    }
}
