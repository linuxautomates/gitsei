package io.levelops.integrations.splunk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.levelops.commons.client.ClientHelper;
import lombok.Builder;
import lombok.extern.java.Log;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.FormBody;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.apache.http.HttpHeaders.CONTENT_TYPE;

@Log
public class SplunkClient {

    private static final String MEDIA_TYPE_FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    private static final String OUTPUT_MODE = "output_mode";
    private static final String JSON = "json";
    private static final String SEARCH = "search";

    private final Supplier<String> urlSupplier;
    private final ObjectMapper objectMapper;
    private final ClientHelper<SplunkClientException> clientHelper;
    private final boolean isSplunkCloud;

    @Builder
    public SplunkClient(Supplier<String> urlSupplier, OkHttpClient okHttpClient, ObjectMapper objectMapper,
                        Boolean isSplunkCloud) {
        this.urlSupplier = urlSupplier;
        this.objectMapper = objectMapper;
        this.isSplunkCloud = MoreObjects.firstNonNull(isSplunkCloud, false);
        clientHelper = ClientHelper.<SplunkClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(SplunkClientException.class)
                .build();
    }

    public Stream<String> search(String searchQuery) throws SplunkClientException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(urlSupplier.get()).newBuilder()
                .addPathSegment("services")
                .addPathSegment("search")
                .addPathSegment("jobs")
                .addPathSegment("export");
        HttpUrl url =  urlBuilder.build();
        log.info("Splunk url = " + url);
        log.info("searchQuery = " + searchQuery);

        final RequestBody body = new FormBody.Builder()
                .add(SEARCH, searchQuery)
                .add(OUTPUT_MODE, JSON)
                .build();

        final Request request = new Request.Builder()
                .url(url)
                .post(body)
                .addHeader(CONTENT_TYPE, MEDIA_TYPE_FORM_URL_ENCODED)
                .build();

        return clientHelper.executeStreamingRequest(request);
    }
}
