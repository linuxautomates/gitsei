package io.levelops.integrations.salesforce.client;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import io.levelops.commons.client.ClientConstants;
import io.levelops.commons.client.ClientHelper;
import io.levelops.integrations.salesforce.models.SOQLJobRequest;
import io.levelops.integrations.salesforce.models.SOQLJobResponse;
import io.levelops.integrations.salesforce.models.SalesforceEntity;
import io.levelops.integrations.salesforce.models.SalesforcePaginatedResponse;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * SalesForce client which should be used for making calls to SalesForce API
 */
@Log4j2
public class SalesforceClient {

    private static final String CREATE_JOB = "services/data/v48.0/jobs/query";
    private static final String RESULTS = "results";
    private static final String LOCATOR = "locator";
    private static final String MAX_RECORDS = "maxRecords";
    private static final int QUERY_JOB_POLL_MAX_ATTEMPT = 10;

    private final ClientHelper<SalesforceClientException> clientHelper;
    private final CsvMapper csvMapper;
    private final int pageSize;
    private final String resourceUrl;

    /**
     * @param okHttpClient {@link OkHttpClient} object to be used for making http calls
     * @param objectMapper {@link ObjectMapper} for deserializing the responses
     * @param pageSize     response page size
     * @param resourceUrl  salesforce service url
     */
    @Builder
    public SalesforceClient(final OkHttpClient okHttpClient, final ObjectMapper objectMapper, int pageSize, String resourceUrl) {
        this.pageSize = pageSize != 0 ? pageSize : 1000;
        this.resourceUrl = resourceUrl;
        this.csvMapper = new CsvMapper();
        this.clientHelper = ClientHelper.<SalesforceClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(SalesforceClientException.class)
                .build();
    }

    /**
     * Creates job request for fetching salesforce entities. Entities are fetched based on the provided
     * SOQL statement in the query param. Function will not return until the job is completed.
     *
     * @param operationName type of the query. Possible values are query and queryAll
     * @param query         SOQL query to be performed.
     * @return Job response of the submitted job request
     * @throws SalesforceClientException when the client encounters an exception while making the call
     */
    public SOQLJobResponse createQueryJob(String operationName, String query) throws SalesforceClientException, InterruptedException {
        SOQLJobRequest soqlJobRequest = SOQLJobRequest.builder()
                .operation(operationName)
                .query(query)
                .build();
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(CREATE_JOB)
                .build();
        Request createJobRequest = buildPostRequest(url, clientHelper.createJsonRequestBody(soqlJobRequest),
                ClientConstants.APPLICATION_JSON.toString(), ClientConstants.APPLICATION_JSON.toString());
        ClientHelper.BodyAndHeaders<SOQLJobResponse> createJobResponse =
                clientHelper.executeAndParseWithHeaders(createJobRequest, SOQLJobResponse.class);
        ClientHelper.BodyAndHeaders<SOQLJobResponse> jobStatusResponse;
        int currentAttempt = -1;
        do {
            Thread.sleep(10000);
            var getJobInfo = HttpUrl.parse(resourceUrl).newBuilder()
                    .addPathSegments(CREATE_JOB)
                    .addPathSegment(createJobResponse.getBody().getId())
                    .build();
            Request jobStatusRequest = buildRequest(getJobInfo, ClientConstants.APPLICATION_JSON.toString(),
                    ClientConstants.APPLICATION_JSON.toString());
            jobStatusResponse =
                    clientHelper.executeAndParseWithHeaders(jobStatusRequest, SOQLJobResponse.class);
            currentAttempt++;
        } while (!jobStatusResponse.getBody().getState().equalsIgnoreCase("JobComplete")
                && currentAttempt < QUERY_JOB_POLL_MAX_ATTEMPT);
        return jobStatusResponse.getBody();
    }

    /**
     * Submits the request for fetching the result for a query job.
     *
     * @param jobId   the Id of the query job
     * @param locator get results starting from the specific location
     * @param type    target type of the requested entity
     * @return result if the job's query
     * @throws SalesforceClientException when the client encounters an exception while making the call
     */
    public <D extends SalesforceEntity> SalesforcePaginatedResponse<D> getQueryResults(
            String jobId, String locator, Class<D> type) throws SalesforceClientException, IOException {
        var url = HttpUrl.parse(resourceUrl).newBuilder()
                .addPathSegments(CREATE_JOB)
                .addPathSegment(jobId)
                .addPathSegment(RESULTS)
                .build();
        Request request = buildRequest(url, ClientConstants.TEXT_CSV.toString(), ClientConstants.APPLICATION_JSON.toString());
        Request.Builder requestBuilder = request.newBuilder();
        if (locator != null) {
            requestBuilder.header(LOCATOR, locator);
        }
        request = requestBuilder.header(MAX_RECORDS, String.valueOf(pageSize)).build();
        ClientHelper.BodyAndHeaders<String> response = clientHelper.executeRequestWithHeaders(request);
        String salesForceLocator = response.getHeader("Sforce-Locator");
        List<D> records = deserialize(response.getBody(), type);
        return SalesforcePaginatedResponse.<D>builder()
                .records(records)
                .salesForceLocator(salesForceLocator)
                .build();
    }

    /**
     * Deserializes the CSV string into object
     *
     * @param csv  CSV string containing the result.
     * @param type target type of the entity
     * @return List of target type entity
     */
    public <D> List<D> deserialize(final String csv, final Class<D> type) throws IOException {
        CsvSchema csvSchema = csvMapper.schemaFor(type).withHeader();
        MappingIterator<D> mappingIterator = csvMapper.reader(csvSchema)
                .forType(type)
                .readValues(csv);
        List<D> objects = new ArrayList<>();
        while (mappingIterator.hasNext()) {
            objects.add(mappingIterator.next());
        }
        return objects;
    }

    private Request buildPostRequest(HttpUrl url, RequestBody requestBody, String acceptHeader, String contentTypeHeader) throws SalesforceClientException {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, acceptHeader)
                .header(ClientConstants.CONTENT_TYPE, contentTypeHeader)
                .post(requestBody)
                .build();
    }

    @NotNull
    private Request buildRequest(HttpUrl url, String acceptType, String contentType) {
        return new Request.Builder()
                .url(url)
                .header(ClientConstants.ACCEPT, acceptType)
                .header(ClientConstants.CONTENT_TYPE, contentType)
                .get()
                .build();
    }
}
