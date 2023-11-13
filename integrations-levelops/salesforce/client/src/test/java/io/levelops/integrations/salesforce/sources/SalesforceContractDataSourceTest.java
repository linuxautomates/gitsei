package io.levelops.integrations.salesforce.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.salesforce.client.SalesforceClient;
import io.levelops.integrations.salesforce.client.SalesforceClientException;
import io.levelops.integrations.salesforce.client.SalesforceClientFactory;
import io.levelops.integrations.salesforce.models.Contract;
import io.levelops.integrations.salesforce.models.SOQLJobResponse;
import io.levelops.integrations.salesforce.models.SalesforceIngestionQuery;
import io.levelops.integrations.salesforce.models.SalesforcePaginatedResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class SalesforceContractDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    SalesforceContractDataSource dataSource;

    @Before
    public void setup() throws SalesforceClientException, InterruptedException, IOException {
        SalesforceClient salesForceClient = Mockito.mock(SalesforceClient.class);
        SalesforceClientFactory salesForceClientFactory = Mockito.mock(SalesforceClientFactory.class);
        dataSource = new SalesforceContractDataSource(salesForceClientFactory);
        when(salesForceClientFactory.get(TEST_KEY)).thenReturn(salesForceClient);
        when(salesForceClient.createQueryJob(eq("query"), anyString())).thenReturn(SOQLJobResponse.builder()
                .operation("query").id("1").state("JobComplete").build());

        SalesforcePaginatedResponse<Contract> caseResponse = SalesforcePaginatedResponse.<Contract>builder()
                .salesForceLocator("null")
                .records(Arrays.asList(new Contract(), new Contract(), new Contract()))
                .build();

        when(salesForceClient.getQueryResults("1", null, Contract.class)).thenReturn(caseResponse);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(SalesforceIngestionQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<Contract>> assets = dataSource.fetchMany(SalesforceIngestionQuery.builder()
                .partial(true)
                .from(1593436605000L).to(1593436605200L)
                .integrationKey(TEST_KEY).build())
                .collect(Collectors.toList());
        assertThat(assets).hasSize(3);
    }
}
