package io.levelops.tags;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Tag;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.tags.models.FindTagsByValueRequest;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;

public class TagsClient {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    @Builder
    public TagsClient(
            OkHttpClient client,
            ObjectMapper objectMapper,
            @Qualifier("internalApiUrl") String internalApiUri) {
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
                .addPathSegment("tags");
    }

    public Tag getById(String company, String id) throws InternalApiClientException {
        var url = baseUrlBuilder(company)
                .addPathSegment(id)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, Tag.class);
    }

    public List<String> findByValue(String company, List<String> values, boolean createIfMissing) throws InternalApiClientException {
        var url = baseUrlBuilder(company)
                .addPathSegment("find")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(FindTagsByValueRequest.builder()
                        .values(values)
                        .createIfMissing(createIfMissing)
                        .build()))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    public PaginatedResponse<Tag> list(String company, DefaultListRequest listRequest) throws InternalApiClientException {
        var url = baseUrlBuilder(company)
                .addPathSegment("list")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(listRequest))
                .build();
        return clientHelper.executeAndParse(request, PaginatedResponse.typeOf(objectMapper, Tag.class));
    }

}
