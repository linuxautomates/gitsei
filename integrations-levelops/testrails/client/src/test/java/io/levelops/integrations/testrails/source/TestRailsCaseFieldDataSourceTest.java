package io.levelops.integrations.testrails.source;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.CaseField;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.sources.TestRailsCaseFieldDataSource;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class TestRailsCaseFieldDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TestRailsCaseFieldDataSource dataSource;

    @Before
    public void setup() throws TestRailsClientException {
        TestRailsClient client = Mockito.mock(TestRailsClient.class);
        TestRailsClientFactory clientFactory = Mockito.mock(TestRailsClientFactory.class);

        dataSource = new TestRailsCaseFieldDataSource(clientFactory);
        when(clientFactory.get(ArgumentMatchers.eq(TEST_KEY))).thenReturn(client);
        List<CaseField> caseFields = List.of(CaseField.builder()
                .id(1L)
                .systemName("custom_field")
                .name("Field")
                .label("Field")
                .build());
        when(client.getCaseFields()).thenReturn(caseFields);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TestRailsQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<CaseField>> caseFields = dataSource.fetchMany(TestRailsQuery.builder()
                        .integrationKey(TEST_KEY)
                        .from(null)
                        .shouldFetchUsers(true)
                        .build())
                .collect(Collectors.toList());
        assertThat(caseFields).hasSize(1);
    }
}
