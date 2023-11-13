package io.levelops.objects;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.database.automation_rules.ObjectTypeDTO;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.exceptions.InternalApiClientException;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.List;

public class ObjectsClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @Builder
    public ObjectsClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
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
                .addPathSegment("objects");
    }

    public DbListResponse<String> listObjectTypes(String company, DefaultListRequest listRequest)
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
                        .constructParametricType(DbListResponse.class, String.class));
    }


    public List<String> getFieldsForObjectType(String company, ObjectType objectType) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment(objectType.toString())
                .addPathSegment("fields")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(List.class, String.class));
    }

    public DbListResponse<ObjectTypeDTO> listObjectTypeDetails(String company, DefaultListRequest listRequest)
            throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
                .addPathSegment("list_details")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(listRequest))
                .build();

        return clientHelper.executeAndParse(request,
                mapper.getTypeFactory()
                        .constructParametricType(DbListResponse.class, ObjectTypeDTO.class));
    }
}
