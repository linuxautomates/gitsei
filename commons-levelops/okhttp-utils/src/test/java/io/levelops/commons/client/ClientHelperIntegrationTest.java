package io.levelops.commons.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Test;

import java.util.stream.Stream;

public class ClientHelperIntegrationTest {
    private final String URL = "https://localhost:8089";
    private static final String MEDIA_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    private final String QUERY = "search=search index=main host=splunk-client-enterprise-test-1&output_mode=json";
    private final String splunkOAuthToken = System.getenv("SPLUNK-OAUTH-TOKEN");
    private final ObjectMapper objectMapper = DefaultObjectMapper.get()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    @Test
    public void testExecuteStreamingRequest() throws Exception {
        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder = ClientHelper.configureToIgnoreCertificate(builder);
        OkHttpClient client = builder.build();
        ClientHelper<Exception> clientHelper = ClientHelper.<Exception>builder()
                .client(client)
                .objectMapper(objectMapper)
                .exception(Exception.class)
                .build();

        MediaType mediaType = MediaType.parse(MEDIA_TYPE_FORM_URL_ENCODED);
        RequestBody body = RequestBody.create(mediaType, QUERY);
        Request request = new Request.Builder()
                .url("https://localhost:8089/services/search/jobs/export")
                .method("POST", body)
                .addHeader("Authorization", "Bearer " + splunkOAuthToken)
                .addHeader("Content-Type", MEDIA_TYPE_FORM_URL_ENCODED)
                .build();
        final MutableInt i = new MutableInt(1);
        try(Stream<String> result = clientHelper.executeStreamingRequest(request)){
            result.forEach(l -> {
                System.out.println("-------" + i + "------");
                System.out.println(l);
                i.add(1);
            });
        }
    }
}