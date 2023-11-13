package io.levelops.integrations.checkmarx.client.cxsca;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.checkmarx.models.*;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.List;
import java.util.Objects;

public class CxScaClient {
    public static final String SEARCH_PROJECTS = "/risk-management/projects";
    public static final String SEARCH_SCANS = "/risk-management/scans";
    public static final String GET_RISK_REPORTS = "/risk-management/risk-reports";
    public static final String SCAN_STATUS = "status";
    public static final String RISK_REPORT_VULNERABILITIES = "vulnerabilities";
    public static final String RISK_REPORT_PACKAGES = "packages";
    public static final String RISK_REPORT_LICENSES = "licenses";
    public static final String PROJECT_NAME = "name";
    public static final String PROJECT_ID = "projectId";
    public static final String SIZE = "size";

    private final String resourceUrl;
    private final String organization;
    private final ClientHelper<CxScaClientException> clientHelper;
    private final ObjectMapper objectMapper;

    @Builder
    public CxScaClient(final OkHttpClient okHttpClient,
                       final ObjectMapper objectMapper, String resourceUrl, String organization) {
        this.resourceUrl = resourceUrl;
        this.organization = organization;
        this.objectMapper = objectMapper;
        this.clientHelper = ClientHelper.<CxScaClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(CxScaClientException.class)
                .build();
    }

    public List<CxScaProject> getProjects(String projectName) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SEARCH_PROJECTS)
                .addQueryParameter(PROJECT_NAME, projectName);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxScaProject>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxScaProject.class));
        return response.getBody();
    }

    public CxScaProject getProjectById(String projectId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SEARCH_PROJECTS)
                .addPathSegment(projectId);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<CxScaProject> response = clientHelper
                .executeAndParseWithHeaders(request, CxScaProject.class);
        return response.getBody();
    }

    public List<CxScaScan> getScans(String projectId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SEARCH_SCANS)
                .addQueryParameter(PROJECT_ID, projectId);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxScaScan>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxScaScan.class));
        return response.getBody();
    }

    public CxScaScanStatus getScanStatus(String scanId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(SEARCH_SCANS)
                .addPathSegment(scanId)
                .addPathSegment(SCAN_STATUS);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<CxScaScanStatus> response = clientHelper
                .executeAndParseWithHeaders(request, CxScaScanStatus.class);
        return response.getBody();
    }

    public CxScaRiskReport getRiskReportSummaries(String projectId, Integer size) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(GET_RISK_REPORTS)
                .addQueryParameter(PROJECT_ID, projectId)
                .addQueryParameter(SIZE, String.valueOf(size));
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<CxScaRiskReport> response = clientHelper
                .executeAndParseWithHeaders(request, CxScaRiskReport.class);
        return response.getBody();
    }

    public CxScaScan getScanById(String scanId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(GET_RISK_REPORTS)
                .addPathSegment(scanId)
                .addPathSegment(RISK_REPORT_PACKAGES);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<CxScaScan> response = clientHelper
                .executeAndParseWithHeaders(request, CxScaScan.class);
        return response.getBody();
    }

    public List<CxScaRiskReportPackage> getPackagesForRiskReport(String scanId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(GET_RISK_REPORTS)
                .addPathSegment(scanId)
                .addPathSegment(RISK_REPORT_PACKAGES);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxScaRiskReportPackage>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxScaRiskReportPackage.class));
        return response.getBody();
    }

    public List<CxScaRiskReportVulnerability> getVulnerabilitiesForRiskReport(String scanId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(GET_RISK_REPORTS)
                .addPathSegment(scanId)
                .addPathSegment(RISK_REPORT_VULNERABILITIES);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxScaRiskReportVulnerability>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxScaRiskReportVulnerability.class));
        return response.getBody();
    }

    public List<CxScaRiskReportLicense> getLicenseForRiskReport(String scanId) throws CxScaClientException {
        HttpUrl.Builder urlBuilder = baseUrlBuilder()
                .addPathSegment(GET_RISK_REPORTS)
                .addPathSegment(scanId)
                .addPathSegment(RISK_REPORT_LICENSES);
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.AUTHORIZATION, ClientConstants.BEARER_)
                .get()
                .build();
        ClientHelper.BodyAndHeaders<List<CxScaRiskReportLicense>> response = clientHelper
                .executeAndParseWithHeaders(request, objectMapper.getTypeFactory()
                        .constructCollectionType(List.class, CxScaRiskReportLicense.class));
        return response.getBody();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return Objects.requireNonNull(HttpUrl.parse(resourceUrl)).newBuilder();
    }

}
