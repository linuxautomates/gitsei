package io.levelops.bullseye_converter_clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.bullseye_converter_commons.models.ConversionRequest;
import io.levelops.bullseye_converter_commons.models.Result;
import io.levelops.commons.client.ClientHelper;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class BullseyeConverterClient {
    private final ClientHelper<BullseyeConverterClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String bullseyeConverterServiceUrl;

    @lombok.Builder
    public BullseyeConverterClient(final OkHttpClient client, final ObjectMapper mapper, final String bullseyeConverterServiceUrl) {
        this.mapper = mapper;
        this.bullseyeConverterServiceUrl = bullseyeConverterServiceUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, BullseyeConverterClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(){
        return HttpUrl.parse(bullseyeConverterServiceUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("convert");
    }

    private MultipartBody createMultipartBody(ConversionRequest conversionRequest, File covFile) throws IOException {
        String serializedRequest = mapper.writeValueAsString(conversionRequest);
        MultipartBody.Builder requestBodyBldr = null;
        requestBodyBldr = new MultipartBody.Builder()
                .addFormDataPart("json", "json", okhttp3.RequestBody.create(serializedRequest.getBytes(StandardCharsets.UTF_8)))
                .addFormDataPart("file", covFile.getName(), okhttp3.RequestBody.create(Files.readAllBytes(covFile.toPath())));
        return requestBodyBldr.build();
    }
    public Result convertCovToXml(ConversionRequest conversionRequest, File covFile) throws BullseyeConverterClientException {
        var url = getBaseUrlBuilder().build();

        MultipartBody multipartBody = null;
        try {
            multipartBody = createMultipartBody(conversionRequest, covFile);
        } catch (IOException e) {
            throw new BullseyeConverterClientException("Error creating multipart body!", e);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();
        return clientHelper.executeAndParse(request, Result.class);
    }

    public static class BullseyeConverterClientException extends Exception {
        public BullseyeConverterClientException() {
        }

        public BullseyeConverterClientException(String message) {
            super(message);
        }

        public BullseyeConverterClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public BullseyeConverterClientException(Throwable cause) {
            super(cause);
        }
    }
}
