package io.levelops.integrations.blackduck;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.blackduck.models.*;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.BooleanUtils;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

@Log4j2
public class BlackDuckClient {

    public static final String LIST_BLACKDUCK_PROJECTS_API_PATH = "projects";
    public static final String LIST_PROJECT_VERSIONS_API_PATH = "versions";
    public static final String LIST_PROJECT_VULNERABILITIES_API_PATH = "vulnerable-bom-components";
    private static final String PAGE_SIZE_QUERY_PARAM = "limit";
    private static final String PAGE_OFFSET_QUERY_PARAM = "offset";

    private final ClientHelper<BlackDuckClientException> clientHelper;
    private final String resourceUrl;
    private final ObjectMapper objectMapper;


    private final int pageSize;

    @Builder
    public BlackDuckClient(OkHttpClient okHttpClient, ObjectMapper objectMapper,
                           String resourceUrl, int pageSize, boolean allowUnsafeSSL) {
        this.resourceUrl = resourceUrl;
        this.objectMapper = objectMapper;
        if (BooleanUtils.isTrue(allowUnsafeSSL)) {
            try {
                okHttpClient = ClientHelper.configureToIgnoreCertificate(okHttpClient.newBuilder()).build();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                log.warn("Failed to configure BlackDuck client to ignore SSL certificate validation", e);
            }
        }
        this.pageSize = pageSize != 0 ? pageSize : BlackDuckClientFactory.DEFAULT_PAGE_SIZE;
        this.clientHelper = ClientHelper.<BlackDuckClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(BlackDuckClientException.class)
                .build();
    }

    public List<BlackDuckProject> getProjects() throws BlackDuckClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(LIST_BLACKDUCK_PROJECTS_API_PATH)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        BlackDuckProjectsListResponse projectsListResponse = clientHelper
                .executeAndParse(request, BlackDuckProjectsListResponse.class);
        return projectsListResponse.getBlackDuckProjects();
    }

    public List<BlackDuckVersion> getVersions(String projectId) throws BlackDuckClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(LIST_BLACKDUCK_PROJECTS_API_PATH)
                .addPathSegment(projectId)
                .addPathSegment(LIST_PROJECT_VERSIONS_API_PATH)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        BlackDuckVersionsListResponse versionsListResponse = clientHelper
                .executeAndParse(request, BlackDuckVersionsListResponse.class);
        return versionsListResponse.getBlackDuckVersions();
    }

    public List<BlackDuckIssue> getIssues(String projectId, String versionId, int offset) throws BlackDuckClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment(LIST_BLACKDUCK_PROJECTS_API_PATH)
                .addPathSegment(projectId)
                .addPathSegment(LIST_PROJECT_VERSIONS_API_PATH)
                .addPathSegment(versionId)
                .addPathSegment(LIST_PROJECT_VULNERABILITIES_API_PATH)
                .addQueryParameter(PAGE_OFFSET_QUERY_PARAM, String.valueOf(offset))
                .addQueryParameter(PAGE_SIZE_QUERY_PARAM, String.valueOf(pageSize))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        BlackDuckIssuesListResponse issuesListResponse = clientHelper
                .executeAndParse(request, BlackDuckIssuesListResponse.class);
        return issuesListResponse.getBlackDuckIssues();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return Objects.requireNonNull(HttpUrl.parse(resourceUrl))
                .newBuilder()
                .addPathSegment("api");
    }
}
