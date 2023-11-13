package io.levelops.integrations.tenable.client;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.tenable.models.Asset;
import io.levelops.integrations.tenable.models.AssetsExportRequest;
import io.levelops.integrations.tenable.models.ExportResponse;
import io.levelops.integrations.tenable.models.ExportStatusResponse;
import io.levelops.integrations.tenable.models.NetworkResponse;
import io.levelops.integrations.tenable.models.ScannerPoolResponse;
import io.levelops.integrations.tenable.models.ScannerResponse;
import io.levelops.integrations.tenable.models.TenableScanQuery;
import io.levelops.integrations.tenable.models.VulnerabilitiesExportRequest;
import io.levelops.integrations.tenable.models.Vulnerability;
import io.levelops.integrations.tenable.models.WASResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Tenable client which should be used for making calls to Tenable APIs.
 */
@Log4j2
public class TenableClient {

    private final String resourceUrl = "https://cloud.tenable.com";
    private final String ASSETS_EXPORT_API_PATH = "assets/export";
    private final String VULNERABILITIES_EXPORT_API_PATH = "vulns/export";
    private static final String WAS_V2_VULN_PATH = "was/v2/vulnerabilities";
    private final String STATUS = "status";
    private final String CHUNKS = "chunks";
    public static final String NETWORKS = "networks";
    public static final String SCANNERS = "scanners";
    public static final String SCANNER_GROUPS = "scanner-groups";
    private static final String OFFSET = "offset";
    private static final String LIMIT = "limit";
    private static final String PAGE = "page";
    private static final String SIZE = "size";
    private static final String ORDERING = "ordering";

    private final ClientHelper<TenableClientException> clientHelper;
    private final int pageSize;

    /**
     * @param okHttpClient {@link OkHttpClient} object to be used for making http calls
     * @param objectMapper {@link ObjectMapper} for deserializing the responses
     * @param pageSize response page size
     */
    @Builder
    public TenableClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, int pageSize) {
        this.pageSize = pageSize != 0 ? pageSize : TenableClientFactory.DEFAULT_PAGE_SIZE;
        this.clientHelper = ClientHelper.<TenableClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(TenableClientException.class)
                .build();
    }

    /**
     * Submits request for exporting assets information. The assets information are fetched based on the
     * {@link AssetsExportRequest.Filter#getCreatedAt()} or {@link AssetsExportRequest.Filter#getUpdatedAt()}
     * criteria specified in the query parameter
     *
     * @param query {@link TenableScanQuery} based on which the assets are fetched
     * @return {@link ExportResponse} contains the UUID for the submitted request
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public ExportResponse exportAssets(TenableScanQuery query) throws TenableClientException {
        query = query.toBuilder().chunkSize(pageSize).build();
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(ASSETS_EXPORT_API_PATH)
                .build();
        AssetsExportRequest assetsExportRequest = getAssetsExportRequest(query);
        Request request = buildPostRequest(url, clientHelper.createJsonRequestBody(assetsExportRequest));
        log.debug("Submitting assets export request with the query criteria: {}", assetsExportRequest);
        ClientHelper.BodyAndHeaders<ExportResponse> page =
                clientHelper.executeAndParseWithHeaders(request, ExportResponse.class);
        return page.getBody();
    }

    /**
     * Submits request for exporting vulnerabilities information. The vulnerabilities information are fetched based on the
     * {@link VulnerabilitiesExportRequest.Filter#getSince()} criteria specified in the query parameter
     *
     * @param query {@link TenableScanQuery} based on which the vulnerabilities are fetched
     * @return {@link ExportResponse} contains the UUID for the submitted request
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public ExportResponse exportVulnerabilities(TenableScanQuery query) throws TenableClientException {
        query = query.toBuilder().chunkSize(pageSize).build();
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(VULNERABILITIES_EXPORT_API_PATH)
                .build();
        VulnerabilitiesExportRequest vulnerabilitiesExportRequest = getVulnerabilitiesExportRequest(query);
        Request request = buildPostRequest(url, clientHelper.createJsonRequestBody(vulnerabilitiesExportRequest));
        log.debug("Submitting vulnerabilities export request with the query criteria: {}", vulnerabilitiesExportRequest);
        ClientHelper.BodyAndHeaders<ExportResponse> page =
                clientHelper.executeAndParseWithHeaders(request, ExportResponse.class);
        return page.getBody();
    }

    /**
     * Creates {@link AssetsExportRequest} from the {@link TenableScanQuery} query
     *
     * @param query {@link TenableScanQuery} based on which assets are fetched
     * @return {@link AssetsExportRequest} query criteria for fetching assets
     */
    private AssetsExportRequest getAssetsExportRequest(TenableScanQuery query) {
        AssetsExportRequest.Filter assetExportRequestFilter;
        if (query.getPartial() == null || query.getPartial()) {
            assetExportRequestFilter = AssetsExportRequest.Filter.builder()
                    .updatedAt(query.getSince())
                    .build();
        } else {
            assetExportRequestFilter = AssetsExportRequest.Filter.builder()
                    .createdAt(query.getSince())
                    .build();
        }
        return AssetsExportRequest.builder()
                .chunkSize(query.getChunkSize())
                .filters(assetExportRequestFilter)
                .build();
    }

    /**
     * Creates {@link VulnerabilitiesExportRequest} from the {@link TenableScanQuery} query
     *
     * @param query {@link TenableScanQuery} based on which vulnerabilities are fetched
     * @return {@link VulnerabilitiesExportRequest} query criteria for fetching vulnerabilities
     */
    private VulnerabilitiesExportRequest getVulnerabilitiesExportRequest(TenableScanQuery query) {
        VulnerabilitiesExportRequest.Filter vulnerabilitiesExportRequestFilter = VulnerabilitiesExportRequest.Filter.builder()
                .since(query.getSince())
                .build();
        return VulnerabilitiesExportRequest.builder()
                .numAssets(query.getChunkSize())
                .filters(vulnerabilitiesExportRequestFilter)
                .build();
    }

    /**
     * Fetches response regarding the submitted export asset request. Response contains chunks available for
     * processing
     *
     * @param exportUUID uuid of the asset export job
     * @return {@link ExportStatusResponse} response containing the uuid and chunks available for download
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public ExportStatusResponse getAssetsExportStatus(String exportUUID) throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(ASSETS_EXPORT_API_PATH)
                .addPathSegment(exportUUID)
                .addPathSegment(STATUS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<ExportStatusResponse> statusResponse =
                clientHelper.executeAndParseWithHeaders(request, ExportStatusResponse.class);
        return statusResponse.getBody();
    }

    /**
     * Fetches response regarding the submitted export vulnerability request. Response contains chunks available for
     * processing
     *
     * @param exportUUID uuid of the vulnerability export job
     * @return {@link ExportStatusResponse} response containing the uuid and chunks available for download
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public ExportStatusResponse getVulnerabilitiesExportStatus(String exportUUID) throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(VULNERABILITIES_EXPORT_API_PATH)
                .addPathSegment(exportUUID)
                .addPathSegment(STATUS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<ExportStatusResponse> statusResponse =
                clientHelper.executeAndParseWithHeaders(request, ExportStatusResponse.class);
        return statusResponse.getBody();
    }

    /**
     * Downloads the available asset chunk.
     *
     * @param exportUUID uuid of the asset export job
     * @param chunkId the id of the asset chunk to download
     * @return {@link List<Asset>} list of asset for the given chunk id
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public List<Asset> downloadAssetChunk(String exportUUID, Integer chunkId) throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(ASSETS_EXPORT_API_PATH)
                .addPathSegment(exportUUID)
                .addPathSegment(CHUNKS)
                .addPathSegment(String.valueOf(chunkId))
                .build();
        Request request = buildRequest(url);
        JavaType assetList = clientHelper.getObjectMapper()
                .getTypeFactory()
                .constructCollectionType(List.class, Asset.class);
        ClientHelper.BodyAndHeaders<List<Asset>> page = clientHelper.executeAndParseWithHeaders(request, assetList);
        return page.getBody();
    }

    /**
     * Downloads the available vulnerability chunk.
     *
     * @param exportUUID uuid of the vulnerability export job
     * @param chunkId the id of the vulnerability chunk to download
     * @return {@link List<Vulnerability>} list of vulnerability data for the given chunk id
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public List<Vulnerability> downloadVulnerabilityChunk(String exportUUID, Integer chunkId) throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(VULNERABILITIES_EXPORT_API_PATH)
                .addPathSegment(exportUUID)
                .addPathSegment(CHUNKS)
                .addPathSegment(String.valueOf(chunkId))
                .build();
        Request request = buildRequest(url);
        JavaType vulnerabilityList = clientHelper.getObjectMapper()
                .getTypeFactory()
                .constructCollectionType(List.class, Vulnerability.class);
        ClientHelper.BodyAndHeaders<List<Vulnerability>> page =
                clientHelper.executeAndParseWithHeaders(request, vulnerabilityList);
        return page.getBody();
    }

    /**
     * Fetches the detail about networks
     * @param offset offset to start fetching data from
     * @param limit limit of the query result
     * @return {@link NetworkResponse} containing the detail about networks and pagination
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public NetworkResponse getNetworks(Integer offset, Integer limit) throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(NETWORKS)
                .addQueryParameter(OFFSET, String.valueOf(offset))
                .addQueryParameter(LIMIT, String.valueOf(limit))
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<NetworkResponse> networkResponse =
                clientHelper.executeAndParseWithHeaders(request, NetworkResponse.class);
        return networkResponse.getBody();
    }

    /**
     * Fetches the detail about scanners
     *
     * @return {@link ScannerResponse} list of scanners
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public ScannerResponse getScanners() throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(SCANNERS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<ScannerResponse> scannerResponse =
                clientHelper.executeAndParseWithHeaders(request, ScannerResponse.class);
        return scannerResponse.getBody();
    }

    /**
     * Fetches the detail about scanner pools
     *
     * @return {@link ScannerPoolResponse} list of scanner pools
     * @throws TenableClientException when the client encounters an exception while making the call
     */
    public ScannerPoolResponse getScannerPools() throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(SCANNER_GROUPS)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<ScannerPoolResponse> scannerPoolResponse =
                clientHelper.executeAndParseWithHeaders(request, ScannerPoolResponse.class);
        return scannerPoolResponse.getBody();
    }

    /**
     * Fetches detail about web application vulnerability scanning.
     * @param page page number to retrieve
     * @param size page size of the query results
     * @param ordering the sort order applied when sorting via the order_by parameter.
     *                Default value for order_by parameter is created_at field
     * @return {@link WASResponse} list of vulnerability data along with pagination detail.
     * @throws TenableClientException
     */
    public WASResponse getWasResponse(Integer page, Integer size, String ordering) throws TenableClientException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(WAS_V2_VULN_PATH)
                .addQueryParameter(PAGE, String.valueOf(page))
                .addQueryParameter(SIZE, String.valueOf(size))
                .addQueryParameter(ORDERING, ordering)
                .build();
        Request request = buildRequest(url);
        ClientHelper.BodyAndHeaders<WASResponse> networkResponse =
                clientHelper.executeAndParseWithHeaders(request, WASResponse.class);
        return networkResponse.getBody();
    }

    @NotNull
    private Request buildRequest(HttpUrl url) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .get()
                .build();
    }

    @NotNull
    private Request buildPostRequest(HttpUrl url, RequestBody requestBody) throws TenableClientException {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, ClientConstants.APPLICATION_JSON.toString())
                .header(ClientConstants.CONTENT_TYPE, ClientConstants.APPLICATION_JSON.toString())
                .post(requestBody)
                .build();
    }
}
