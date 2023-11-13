package io.levelops.integrations.snyk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.client.exceptions.HttpException;
import io.levelops.integrations.snyk.models.SnykDepGraphWrapper;
import io.levelops.integrations.snyk.models.SnykIssueRevised;
import io.levelops.integrations.snyk.models.SnykUser;
import io.levelops.integrations.snyk.models.api.SnykApiListOrgsResponse;
import io.levelops.integrations.snyk.models.api.SnykApiListProjectsResponse;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.collections4.ListUtils;
import org.apache.http.HttpStatus;

import java.util.Collections;

public class SnykClient {
    private static final String BASE_URL_FORMAT = "https://snyk.io/api/v1/";
    private final ObjectMapper objectMapper;
    private final ClientHelper<SnykClientException> clientHelper;

    @Builder
    public SnykClient(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        clientHelper = ClientHelper.<SnykClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(SnykClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(BASE_URL_FORMAT).newBuilder();
    }

    public SnykApiListOrgsResponse getOrgs() throws SnykClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("user")
                .addPathSegment("me")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            SnykUser user =  clientHelper.executeAndParse(request, SnykUser.class);
            return SnykApiListOrgsResponse.builder()
                    .orgs(ListUtils.emptyIfNull(user.getOrgs()))
                    .build();
        } catch (SnykClientException e){
            if(e.getCause() instanceof HttpException){
                HttpException ex = (HttpException) e.getCause();
                if(ex.getCode() == HttpStatus.SC_NOT_FOUND){
                    return SnykApiListOrgsResponse.builder().orgs(Collections.emptyList()).build();
                }
            }
            throw e;
        }
    }

    public SnykApiListProjectsResponse getProjects(final String orgId) throws SnykClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("org")
                .addPathSegment(orgId)
                .addPathSegment("projects")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();

        try {
            return clientHelper.executeAndParse(request, SnykApiListProjectsResponse.class);
        } catch (SnykClientException e){
            if(e.getCause() instanceof HttpException){
                HttpException ex = (HttpException) e.getCause();
                if(ex.getCode() == HttpStatus.SC_NOT_FOUND){
                    return SnykApiListProjectsResponse.builder().projects(Collections.emptyList()).build();
                }
            }
            throw e;
        }
    }

    public SnykIssueRevised getIssues(final String orgId, final String projectId) throws SnykClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("org")
                .addPathSegment(orgId)
                .addPathSegment("project")
                .addPathSegment(projectId)
                .addPathSegment("aggregated-issues")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(RequestBody.create(new byte[]{}, null))
                .build();
        try {
            return clientHelper.executeAndParse(request, SnykIssueRevised.class);
        } catch (SnykClientException e){
            if(e.getCause() instanceof HttpException){
                HttpException ex = (HttpException) e.getCause();
                if(ex.getCode() == HttpStatus.SC_NOT_FOUND){
                    return null;
                }
            }
            throw e;
        }
    }

    public SnykDepGraphWrapper getDependencyGraph(final String orgId, final String projectId) throws SnykClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("org")
                .addPathSegment(orgId)
                .addPathSegment("project")
                .addPathSegment(projectId)
                .addPathSegment("dep-graph")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        try {
            return clientHelper.executeAndParse(request, SnykDepGraphWrapper.class);
        } catch (SnykClientException e){
            if(e.getCause() instanceof HttpException){
                HttpException ex = (HttpException) e.getCause();
                if(ex.getCode() == HttpStatus.SC_NOT_FOUND){
                    return null;
                }
            }
            throw e;
        }
    }
}
