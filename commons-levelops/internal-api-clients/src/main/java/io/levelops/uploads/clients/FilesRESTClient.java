package io.levelops.uploads.clients;

import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.access.RoleType;
import io.levelops.commons.models.ComponentType;
import io.levelops.commons.models.FileUpload;
import io.levelops.exceptions.InternalApiClientException;
import io.levelops.uploads.services.FilesService;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

public class FilesRESTClient implements FilesService {

    private final String apiBaseURL;
    private final ClientHelper<InternalApiClientException> client;

    public FilesRESTClient(final OkHttpClient client, final String apiBaseURL) {
        this.client = ClientHelper.<InternalApiClientException>builder()
            .client(client)
            .exception(InternalApiClientException.class)
            .build();
        this.apiBaseURL = apiBaseURL;
    }

    private HttpUrl.Builder getBaseUrlBuilder(final String company) {
        return HttpUrl.parse(apiBaseURL).newBuilder()
            .addPathSegment("v1")
            .addPathSegment("files")
            .addPathSegment(company);
    }

    @Override
    public UUID uploadForSubComponent(String company, String user, RoleType role, ComponentType component, String componentId,
            String subComponent, String subComponentId, FileUpload fileUpload) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(subComponent)
            .addPathSegment(subComponentId)
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .post(client.createFileUploadRequestBody(fileUpload))
            .build();
        return client.executeAndParse(request, UUID.class);
    }

    @Override
    public UUID uploadForComponent(String company, String user, RoleType role, ComponentType component, String componentId,
            FileUpload fileUpload) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .post(client.createFileUploadRequestBody(fileUpload))
            .build();
        return client.executeAndParse(request, UUID.class);
    }

    @Override
    public FileUpload downloadForSubComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, String subComponent, String subComponentId, UUID fileId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(subComponent)
            .addPathSegment(subComponentId)
            .addPathSegment(fileId.toString())
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        return client.executeRequestForFileUpload(request);
    }

    @Override
    public FileUpload downloadForComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, UUID fileId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(fileId.toString())
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .build();
        return client.executeRequestForFileUpload(request);
    }

    @Override
    public boolean updateForSubComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, String subComponent, String subComponentId, UUID fileId, FileUpload fileUpload) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(subComponent)
            .addPathSegment(subComponentId)
            .addPathSegment(fileId.toString())
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .put(client.createFileUploadRequestBody(fileUpload))
            .build();
        return client.executeAndParse(request, Boolean.class);
    }

    @Override
    public boolean updateForComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, UUID fileId, FileUpload fileUpload) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(fileId.toString())
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .put(client.createFileUploadRequestBody(fileUpload))
            .build();
        return client.executeAndParse(request, Boolean.class);
    }

    @Override
    public boolean upload(String path, String user, RoleType role, FileUpload fileUpload) throws IOException {
        HttpUrl url = HttpUrl.parse(this.apiBaseURL).newBuilder()
            .addPathSegment("v1")
            .addPathSegment("files")
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .put(client.getFileUploadRequestBodyBuilder(fileUpload)
                .addFormDataPart("path", path)
                .build())
            .build();
        return Boolean.TRUE.equals(client.executeAndParse(request, Boolean.class));
    }

    @Override
    public FileUpload download(String path, String user, RoleType role) throws IOException {
        HttpUrl url = HttpUrl.parse(this.apiBaseURL).newBuilder()
            .addPathSegment("v1")
            .addPathSegment("files")
            .addPathSegment("download")
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .post(client.createJsonRequestBody(Map.of("path", path)))
            .build();
        return client.executeRequestForFileUpload(request);
    }

    @Override
    public boolean update(String path, String user, RoleType role, FileUpload fileUpload) throws IOException {
        HttpUrl url = HttpUrl.parse(this.apiBaseURL).newBuilder()
            .addPathSegment("v1")
            .addPathSegment("files")
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .put(client.getFileUploadRequestBodyBuilder(fileUpload)
                .addFormDataPart("path", path)
                .build())
            .build();
        return Boolean.TRUE.equals(client.executeAndParse(request, Boolean.class));
    }

    @Override
    public boolean delete(String path, String user, RoleType role) throws IOException {
        HttpUrl url = HttpUrl.parse(this.apiBaseURL).newBuilder()
            .addPathSegment("v1")
            .addPathSegment("files")
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .delete(client.createJsonRequestBody(Map.of(
                "user", user,
                "role", role.toString(),
                "path", path
                )))
            .build();
        return Boolean.TRUE.equals(client.executeAndParse(request, Boolean.class));
    }

    @Override
    public boolean deleteForSubComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, String subComponent, String subComponentId, UUID fileId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(subComponent)
            .addPathSegment(subComponentId)
            .addPathSegment(fileId.toString())
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        return client.executeAndParse(request, Boolean.class);
    }

    @Override
    public boolean deleteForComponent(String company, String user, RoleType role, ComponentType component,
            String componentId, UUID fileId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(fileId.toString())
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        return client.executeAndParse(request, Boolean.class);
    }

    @Override
    public boolean deleteEverythingUnderSubComponent(String company, String user, RoleType role,
            ComponentType component, String componentId, String subComponent, String subComponentId)
            throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addPathSegment(subComponent)
            .addPathSegment(subComponentId)
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        return client.executeAndParse(request, Boolean.class);
    }

    @Override
    public boolean deleteEverythingUnderComponent(String company, String user, RoleType role,
            ComponentType component, String componentId) throws IOException {
        HttpUrl url = getBaseUrlBuilder(company)
            .addPathSegment(component.toString())
            .addPathSegment(componentId)
            .addEncodedQueryParameter("user", user)
            .addEncodedQueryParameter("role", role.toString())
            .build();
        Request request = new Request.Builder()
            .url(url)
            .delete()
            .build();
        return client.executeAndParse(request, Boolean.class);
    }
}