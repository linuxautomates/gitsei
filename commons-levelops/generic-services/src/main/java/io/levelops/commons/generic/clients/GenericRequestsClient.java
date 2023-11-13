package io.levelops.commons.generic.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public class GenericRequestsClient {
    private final ClientHelper<GenericRequestsClientException> clientHelper;
    private final ObjectMapper mapper;
    private final String apiBaseUrl;

    @lombok.Builder
    public GenericRequestsClient(final OkHttpClient client, final ObjectMapper mapper, final String apiBaseUrl) {
        this.mapper = mapper;
        this.apiBaseUrl = apiBaseUrl;
        this.clientHelper = new ClientHelper<>(client, mapper, GenericRequestsClientException.class);
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company){
        return HttpUrl.parse(apiBaseUrl).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("generic-requests");
    }

    public GenericResponse create(String company, GenericRequest genericRequest) throws GenericRequestsClientException {
        var url = getBaseUrlBuilder(company)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(genericRequest))
                .build();
        return clientHelper.executeAndParse(request, GenericResponse.class);
    }

    private MultipartBody createMultipartBody(MultipartFile jsonFile, MultipartFile zipFile) throws IOException {
        MultipartBody.Builder requestBodyBldr = null;
        requestBodyBldr = new MultipartBody.Builder()
                .addFormDataPart("json", jsonFile.getOriginalFilename(), okhttp3.RequestBody.create(jsonFile.getBytes()));
        if(zipFile != null) {
            requestBodyBldr.addFormDataPart("file", zipFile.getOriginalFilename(), okhttp3.RequestBody.create(zipFile.getBytes()));
        }
        return requestBodyBldr.build();
    }
    public GenericResponse createMultipart(String company, MultipartFile jsonFile, MultipartFile zipFile) throws GenericRequestsClientException {
        var url = getBaseUrlBuilder(company)
                .addPathSegment("multipart")
                .build();

        MultipartBody multipartBody = null;
        try {
            multipartBody = createMultipartBody(jsonFile, zipFile);
        } catch (IOException e) {
            throw new GenericRequestsClientException("Error creating multipart body!", e);
        }

        Request request = new Request.Builder()
                .url(url)
                .post(multipartBody)
                .build();
        return clientHelper.executeAndParse(request, GenericResponse.class);
    }

    public static class GenericRequestsClientException extends Exception {
        public GenericRequestsClientException() {
        }

        public GenericRequestsClientException(String message) {
            super(message);
        }

        public GenericRequestsClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public GenericRequestsClientException(Throwable cause) {
            super(cause);
        }
    }
}
