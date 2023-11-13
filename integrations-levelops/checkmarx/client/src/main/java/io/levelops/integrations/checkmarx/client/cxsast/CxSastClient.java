package io.levelops.integrations.checkmarx.client.cxsast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.xml.DefaultXmlMapper;
import io.levelops.integrations.checkmarx.models.*;
import io.levelops.integrations.checkmarx.models.cxsast.CxSastReportAckResponse;
import lombok.Builder;
import okhttp3.*;

import java.util.List;
import java.util.Objects;

public class CxSastClient {

    private static final String PROJECTS_API_PATH = "/cxrestapi/projects";
    private static final String SCANS_API_PATH = "cxrestapi/sast/scans";
    private static final String SCAN_REPORTS_API = "cxrestapi/reports/sastScan";
    private static final String SOURCE_SETTINGS = "sourceCode/remoteSettings";

    private final String resourceUrl;
    private final String organization;
    private final ClientHelper<CxSastClientException> clientHelper;
    private final ObjectMapper objectMapper;

    @Builder
    public CxSastClient(final OkHttpClient okHttpClient,
                        final ObjectMapper objectMapper, String resourceUrl, String organization) {
        this.resourceUrl = resourceUrl;
        this.organization = organization;
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<CxSastClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(CxSastClientException.class)
                .build();
    }

    public List<CxSastProject> getProjects() throws CxSastClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS_API_PATH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxSastProject>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxSastProject.class));
        return response.getBody();
    }

    public List<CxSastScan> getScans() throws CxSastClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SCANS_API_PATH);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxSastScan>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxSastScan.class));
        return response.getBody();
    }

    public CxSastReportAckResponse registerScanReport(String scanId, String reportType) throws CxSastClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SCAN_REPORTS_API);
        RequestBody body = new FormBody.Builder()
                .add("scanId", scanId)
                .add("reportType", reportType)
                .build();
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .post(body)
                .build();
        ClientHelper.BodyAndHeaders<CxSastReportAckResponse> response = clientHelper
                .executeAndParseWithHeaders(request, CxSastReportAckResponse.class);
        return response.getBody();
    }

    public CxSastReportStatus getScanReportStatus(String reportId) throws CxSastClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SCAN_REPORTS_API)
                .addPathSegment(reportId)
                .addPathSegment("status");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<CxSastReportStatus> response = clientHelper
                .executeAndParseWithHeaders(request, CxSastReportStatus.class);
        return response.getBody();
    }

    public CxXmlResults getScanReport(String reportId) throws CxSastClientException, JsonProcessingException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SCAN_REPORTS_API)
                .addPathSegment(String.valueOf(reportId));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<String> stringBodyAndHeaders = clientHelper.executeRequestWithHeaders(request);
        return DefaultXmlMapper.getXmlMapper().readValue(stringBodyAndHeaders.getBody(),
                CxXmlResults.class);
    }

    public VCSSettings getSettings(String projectId, boolean getGitSettings) throws CxSastClientException {
        VCSSettings.VCSSettingsBuilder vcsSettingsBuilder = VCSSettings.builder()
                .excludeSettings(getExcludeSettings(projectId));
        if (getGitSettings) {
            vcsSettingsBuilder = vcsSettingsBuilder
                    .gitSettings(getGitSettings(projectId));
        }
        return vcsSettingsBuilder.build();
    }

    public VCSSettings.ExcludeSettings getExcludeSettings(String projectId) throws CxSastClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS_API_PATH)
                .addPathSegment(projectId)
                .addPathSegment("sourceCode")
                .addPathSegment("excludeSettings");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<VCSSettings.ExcludeSettings> response = clientHelper
                .executeAndParseWithHeaders(request, VCSSettings.ExcludeSettings.class);
        return response.getBody();
    }

    public VCSSettings.GitSettings getGitSettings(String projectId) throws CxSastClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(PROJECTS_API_PATH)
                .addPathSegment(projectId)
                .addPathSegment(SOURCE_SETTINGS)
                .addPathSegment("git");
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
        ClientHelper.BodyAndHeaders<VCSSettings.GitSettings> response = clientHelper
                .executeAndParseWithHeaders(request, VCSSettings.GitSettings.class);
        return response.getBody();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder();
    }
}
