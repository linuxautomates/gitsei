package io.levelops.plugins.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.PluginResultDTO;
import io.levelops.commons.models.BulkDeleteResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.models.JsonDiff;
import io.levelops.plugins.models.StoredPluginResultDTO;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Log4j2
public class PluginResultsClient {

    private final ClientHelper<PluginResultsClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    public PluginResultsClient(OkHttpClient client, ObjectMapper objectMapper, @Qualifier("internalApiUrl") String internalApiUri) {
        this.objectMapper = objectMapper;
        this.internalApiUri = internalApiUri;
        clientHelper = new ClientHelper<>(client, objectMapper, PluginResultsClientException.class);
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(internalApiUri).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1");
    }

    public PaginatedResponse<String> listLabelKeys(String company, DefaultListRequest filter) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("labels")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, String.class));
    }

    public PaginatedResponse<String> listLabelValues(String company, DefaultListRequest filter) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("labels")
                .addPathSegment("values")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, String.class));
    }

    public PaginatedResponse<String> getLabelKeys(String company, DefaultListRequest filter) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("labels")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, String.class));
    }

    public PaginatedResponse<String> getLabelValues(String company, String key, DefaultListRequest filter) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("labels")
                .addPathSegment(key)
                .addPathSegment("values")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, String.class));
    }

    public PluginResultDTO getById(String company, String id) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment(id)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, PluginResultDTO.class);
    }

    public Map<String, Object> getOldestJobRunStartTimeById(String company, String id) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment(id)
                .addPathSegment("oldest_job_run_start_time")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory()
                .constructMapLikeType(Map.class, String.class, Object.class));
    }

    public PaginatedResponse<PluginResultDTO> list(String company, DefaultListRequest filter) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("list")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(filter))
                .build();

        return clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(PaginatedResponse.class, PluginResultDTO.class));
    }

    public BulkDeleteResponse deleteBulkPluginResult(String company, List<String> ids) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .delete(clientHelper.createJsonRequestBody(ids))
                .build();
        return clientHelper.executeAndParse(request, BulkDeleteResponse.class);
    }

    public Map<String, JsonDiff> diff(String company, String beforeId, String afterId) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("diff")
                .addQueryParameter("before_id", beforeId)
                .addQueryParameter("after_id", afterId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory()
                .constructMapLikeType(Map.class, String.class, JsonDiff.class));
    }

    public String createPluginResult(String company, PluginResultDTO pluginResultDTO) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(pluginResultDTO))
                .build();

        return clientHelper.executeRequest(request);
    }

    public String createPluginResultMultipart(String company, MultipartFile pluginResultDTOStr, MultipartFile resultFile) throws PluginResultsClientException, IOException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("multipart")
                .build();

        MultipartBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("json", pluginResultDTOStr.getOriginalFilename(), okhttp3.RequestBody.create(pluginResultDTOStr.getBytes()))
                .addFormDataPart("result", resultFile.getOriginalFilename(), okhttp3.RequestBody.create(resultFile.getBytes()))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        return clientHelper.executeRequest(request);
    }

    public String createStoredPluginResult(String company, StoredPluginResultDTO storedPluginResultDTO) throws PluginResultsClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("stored")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(storedPluginResultDTO))
                .build();

        return clientHelper.executeRequest(request);
    }

    public String submitPluginResultWithPreProcessing(String company, MultipartFile jsonFile, MultipartFile resultFile) throws PluginResultsClientException, IOException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment("multipart")
                .addPathSegment("pre-process")
                .build();

        MultipartBody requestBody = new MultipartBody.Builder()
                .addFormDataPart("json", jsonFile.getOriginalFilename(), okhttp3.RequestBody.create(jsonFile.getBytes()))
                .addFormDataPart("result", resultFile.getOriginalFilename(), okhttp3.RequestBody.create(resultFile.getBytes()))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();

        return clientHelper.executeRequest(request);
    }

    public String updatePluginResult(String company, PluginResultDTO pluginResultDTO) throws PluginResultsClientException, IOException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("plugins")
                .addPathSegment("results")
                .addPathSegment(pluginResultDTO.getId())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(pluginResultDTO))
                .build();

        return clientHelper.executeRequest(request);
    }

    public static class PluginResultsClientException extends Exception {
        public PluginResultsClientException() {
        }

        public PluginResultsClientException(String message) {
            super(message);
        }

        public PluginResultsClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public PluginResultsClientException(Throwable cause) {
            super(cause);
        }
    }
}
