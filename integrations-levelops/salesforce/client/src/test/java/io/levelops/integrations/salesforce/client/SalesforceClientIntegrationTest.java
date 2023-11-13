package io.levelops.integrations.salesforce.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.salesforce.models.Case;
import io.levelops.integrations.salesforce.models.CaseComment;
import io.levelops.integrations.salesforce.models.CaseHistory;
import io.levelops.integrations.salesforce.models.Contact;
import io.levelops.integrations.salesforce.models.Contract;
import io.levelops.integrations.salesforce.models.SOQLJobResponse;
import io.levelops.integrations.salesforce.models.SalesforcePaginatedResponse;
import io.levelops.integrations.salesforce.models.Solution;
import io.levelops.integrations.salesforce.models.User;
import io.levelops.integrations.salesforce.sources.SOQLHelper;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class SalesforceClientIntegrationTest {
    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "salesforce1";
    private static final String APPLICATION = "salesforce";
    private static final String SALESFORCE_URL = System.getenv("SALESFORCE_URL");
    private static final String SALESFORCE_TOKEN = System.getenv("SALESFORCE_TOKEN");
    private static final String SALESFORCE_REFRESH_TOKEN = System.getenv("SALESFORCE_REFRESH_TOKEN");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();

    private SalesforceClientFactory salesForceClientFactory;

    @Before
    public void setup() {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, SALESFORCE_URL, Collections.emptyMap(), SALESFORCE_TOKEN, SALESFORCE_REFRESH_TOKEN, null)
                .build());
        salesForceClientFactory = SalesforceClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(DefaultObjectMapper.get())
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void getCases() throws InterruptedException, FetchException, IOException {
        SalesforceClient salesForceClient = salesForceClientFactory.get(TEST_INTEGRATION_KEY);
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", Case.SOQL_FIELDS) + ", " +
//                SOQLHelper.constructSOQLFields("Contact.", Contact.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("CreatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("LastModifiedBy.", User.SOQL_FIELDS) +
                " FROM Case";
        SOQLJobResponse soqlQueryJob = salesForceClient.createQueryJob("query", soqlQuery);
        if (!soqlQueryJob.getState().equalsIgnoreCase("JobComplete")) {
            throw new FetchException("Query Job not completed within " +
                    "given time bound. Integration key: " + TEST_INTEGRATION_KEY);
        }

        SalesforcePaginatedResponse<Case> caseResponse =
                salesForceClient.getQueryResults(soqlQueryJob.getId(), null, Case.class);
        assertThat(caseResponse).isNotNull();
        assertThat(caseResponse.getRecords()).isNotNull();
    }

    @Test
    public void getCaseHistories() throws FetchException, InterruptedException, IOException {
        SalesforceClient salesForceClient = salesForceClientFactory.get(TEST_INTEGRATION_KEY);
        String soqlQuery =  "SELECT " + SOQLHelper.constructSOQLFields("", CaseHistory.SOQL_FIELDS) + " FROM CaseHistory";
        SOQLJobResponse soqlQueryJob = salesForceClient.createQueryJob("query", soqlQuery);
        if (!soqlQueryJob.getState().equalsIgnoreCase("JobComplete")) {
            throw new FetchException("Query Job not completed within " +
                    "given time bound. Integration key: " + TEST_INTEGRATION_KEY);
        }
        SalesforcePaginatedResponse<CaseHistory> caseHistoryResponse =
                salesForceClient.getQueryResults(soqlQueryJob.getId(), null, CaseHistory.class);
        assertThat(caseHistoryResponse).isNotNull();
        assertThat(caseHistoryResponse.getRecords()).isNotNull();
    }

    @Test
    public void getCaseComment() throws FetchException, InterruptedException, IOException {
        SalesforceClient salesForceClient = salesForceClientFactory.get(TEST_INTEGRATION_KEY);
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", CaseComment.SOQL_FIELDS) + " FROM CaseComment";
        SOQLJobResponse soqlQueryJob = salesForceClient.createQueryJob("query", soqlQuery);
        if (!soqlQueryJob.getState().equalsIgnoreCase("JobComplete")) {
            throw new FetchException("Query Job not completed within " +
                    "given time bound. Integration key: " + TEST_INTEGRATION_KEY);
        }
        SalesforcePaginatedResponse<CaseComment> caseCommentResponse =
                salesForceClient.getQueryResults(soqlQueryJob.getId(), null, CaseComment.class);
        assertThat(caseCommentResponse).isNotNull();
        assertThat(caseCommentResponse.getRecords()).isNotNull();
    }

    @Test
    public void getCaseContract() throws FetchException, InterruptedException, IOException {
        SalesforceClient salesForceClient = salesForceClientFactory.get(TEST_INTEGRATION_KEY);
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", Contract.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("ActivatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("CreatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("LastModifiedBy.", User.SOQL_FIELDS) + " FROM Contract";
        SOQLJobResponse soqlQueryJob = salesForceClient.createQueryJob("query", soqlQuery);
        if (!soqlQueryJob.getState().equalsIgnoreCase("JobComplete")) {
            throw new FetchException("Query Job not completed within " +
                    "given time bound. Integration key: " + TEST_INTEGRATION_KEY);
        }
        SalesforcePaginatedResponse<Contract> contractResponse =
                salesForceClient.getQueryResults(soqlQueryJob.getId(), null, Contract.class);
        assertThat(contractResponse).isNotNull();
        assertThat(contractResponse.getRecords()).isNotNull();
    }

    @Test
    public void getCaseSolution() throws FetchException, InterruptedException, IOException {
        SalesforceClient salesForceClient = salesForceClientFactory.get(TEST_INTEGRATION_KEY);
        String soqlQuery = "SELECT " + SOQLHelper.constructSOQLFields("", Solution.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("CreatedBy.", User.SOQL_FIELDS) + ", " +
                SOQLHelper.constructSOQLFields("LastModifiedBy.", User.SOQL_FIELDS) + " FROM Solution";
        SOQLJobResponse soqlQueryJob = salesForceClient.createQueryJob("query", soqlQuery);
        if (!soqlQueryJob.getState().equalsIgnoreCase("JobComplete")) {
            throw new FetchException("Query Job not completed within " +
                    "given time bound. Integration key: " + TEST_INTEGRATION_KEY);
        }
        SalesforcePaginatedResponse<Solution> caseSolutionResponse =
                salesForceClient.getQueryResults(soqlQueryJob.getId(), null, Solution.class);
        assertThat(caseSolutionResponse).isNotNull();
        assertThat(caseSolutionResponse.getRecords()).isNotNull();
    }
}
