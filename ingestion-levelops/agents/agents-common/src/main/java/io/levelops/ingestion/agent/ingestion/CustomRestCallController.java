package io.levelops.ingestion.agent.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.ingestion.controllers.ControllerIngestionResult;
import io.levelops.ingestion.controllers.DataController;
import io.levelops.ingestion.exceptions.IngestException;
import io.levelops.ingestion.integrations.custom.rest.models.CustomRestCallQuery;
import io.levelops.ingestion.integrations.custom.rest.models.CustomRestCallResult;
import io.levelops.ingestion.models.JobContext;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomRestCallController implements DataController<CustomRestCallQuery> {

    private static final Set<String> METHODS_THAT_REQUIRE_BODY = Set.of("POST", "PUT", "PATCH");

    private final ObjectMapper objectMapper;
    private final ClientHelper<IngestException> clientHelper;

    public CustomRestCallController(ObjectMapper objectMapper, OkHttpClient okHttpClient) {
        this.objectMapper = objectMapper;
        clientHelper = new ClientHelper<>(okHttpClient, objectMapper, IngestException.class);
    }

    @Override
    public ControllerIngestionResult ingest(JobContext jobContext, CustomRestCallQuery customRestCallQuery) throws IngestException {
        String url = customRestCallQuery.getUrl();
        String method = customRestCallQuery.getMethod();
        List<CustomRestCallQuery.Header> headers = customRestCallQuery.getHeaders();
        String contentType = customRestCallQuery.getContentType();
        String body = customRestCallQuery.getBody();

        if (METHODS_THAT_REQUIRE_BODY.contains(method) && body == null) {
            throw new IngestException("Cannot make Rest call: method '" + method + "' requires body");
        }

        RequestBody requestBody = null;
        if (Strings.isNotEmpty(body)) {
            requestBody = RequestBody.create(StringUtils.isNotBlank(contentType) ? MediaType.parse(contentType) : null, body);
        }

        Request.Builder request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .method(method, requestBody);

        if (CollectionUtils.isNotEmpty(headers)) {
            headers.forEach(kv -> request.addHeader(kv.getKey(), kv.getValue()));
        }

        Integer code;
        String responseBody;
        Map<String, Object> jsonBody = Map.of();
        Map<String, List<String>> responseHeaders = Map.of();
        try {
            ClientHelper.BodyAndHeaders<String> response = clientHelper.executeRequestWithHeaders(request.build());
            responseBody = response.getBody();
            var responseContentType = response.getHeader("Content-Type");
            responseHeaders = response.getHeaders();
            if (StringUtils.isBlank(responseContentType)) {
                responseContentType = response.getHeader("content-type");
            }
            if (responseContentType.equalsIgnoreCase("application/json")) {
                jsonBody = ParsingUtils.parseJsonObject(objectMapper, "response", responseBody);
            }
            code = response.getCode();
        } catch (IngestException e) {
            Throwable rootCause = ExceptionUtils.getRootCause(e);
            if (rootCause instanceof HttpException) {
                code = ((HttpException) rootCause).getCode();
                responseBody = ((HttpException) rootCause).getBody();
            } else {
                throw e;
            }
        }

        return CustomRestCallResult.builder()
                .code(code)
                .body(responseBody)
                .jsonBody(jsonBody)
                .headers(responseHeaders)
                .build();
    }

    @Override
    public CustomRestCallQuery parseQuery(Object o) {
        return objectMapper.convertValue(o, CustomRestCallQuery.class);
    }
}
