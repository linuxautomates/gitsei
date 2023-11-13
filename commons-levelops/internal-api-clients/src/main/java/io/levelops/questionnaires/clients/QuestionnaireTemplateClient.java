package io.levelops.questionnaires.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Id;
import io.levelops.commons.databases.models.database.QuestionnaireTemplate;
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

import java.util.List;

public class QuestionnaireTemplateClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    @Builder
    public QuestionnaireTemplateClient(OkHttpClient client, ObjectMapper objectMapper, @Qualifier("internalApiUrl") String internalApiUri) {
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
                .addPathSegment("qtemplates");
    }

    public Id create(String company, QuestionnaireTemplate questionnaire) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company).build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(questionnaire))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }

    public QuestionnaireTemplate get(String company, String id) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(id)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, QuestionnaireTemplate.class);
    }

    public Id update(String company, String id, QuestionnaireTemplate questionnaire) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(id)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(questionnaire))
                .build();

        return clientHelper.executeAndParse(request, Id.class);
    }


    public DeleteResponse delete(String company, String id) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(id)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete()
                .build();

        return clientHelper.executeAndParse(request, DeleteResponse.class);
    }

    public BulkDeleteResponse bulkDelete(String company, List<String> ids) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(ids))
                .build();
        return clientHelper.executeAndParse(request, BulkDeleteResponse.class);
    }

    public PaginatedResponse<QuestionnaireTemplate> list(String company, DefaultListRequest filter) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, QuestionnaireTemplate.class));
    }

}
