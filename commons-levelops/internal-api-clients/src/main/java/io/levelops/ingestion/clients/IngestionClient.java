package io.levelops.ingestion.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Qualifier;


public class IngestionClient {
    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    @Builder
    public IngestionClient(OkHttpClient client, ObjectMapper objectMapper, @Qualifier("internalApiUrl") String internalApiUri) {
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
                .addPathSegment("ingestion");
    }

    public PaginatedResponse<IngestionLogDTO> getIngestionLogs(
            String company,
            String integrationId,
            DefaultListRequest listRequest,
            Boolean includeResultField) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment(integrationId)
                .addPathSegment("logs")
                .addQueryParameter("include_result_field", Boolean.valueOf(BooleanUtils.isTrue(includeResultField)).toString())
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(listRequest))
                .build();
        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory()
                        .constructParametricType(PaginatedResponse.class, IngestionLogDTO.class));
    }
}
