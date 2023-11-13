package io.levelops.integrations.jira.sources;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryServiceImpl;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.inventory.utils.InventoryHelper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientException;
import io.levelops.integrations.jira.client.JiraClientFactory;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class JiraDataSourceTest {
    private JiraClientFactory jiraClientFactory;
    private OkHttpClient okHttpClient;
    private InventoryServiceImpl inventoryService;

    @Before
    public void setUp() throws Exception {
        okHttpClient = Mockito.mock(OkHttpClient.class);
        inventoryService = Mockito.mock(InventoryServiceImpl.class);
        jiraClientFactory = Mockito.mock(JiraClientFactory.class);

    }

    @Test
    public void issues() throws URISyntaxException, FetchException, IOException, InventoryException, JiraClientException {
        var call = Mockito.mock(Call.class);
        when(okHttpClient.newCall(any(Request.class))).thenReturn(call);
        // var builder = Mockito.mock(Builder.class);
        doCallRealMethod().when(okHttpClient).newBuilder();
        var data = ResourceUtils.getResourceAsString("jira/jira_search_issues.json");
        ResponseBody body = ResponseBody.create(data, MediaType.parse("application/json"));
        var request = new Request.Builder().url("http://localhost").build();
        var response = new Response.Builder().code(200).body(body).request(request).protocol(Protocol.HTTP_1_1).message("ok").build();
        when(call.execute()).thenReturn(response);
        when(inventoryService.getIntegration(any(IntegrationKey.class))).thenReturn(Integration.builder().application("jira").url("https://test.test").build());
        when(inventoryService.listTokens(any(IntegrationKey.class))).thenReturn(List.of(Token.builder().tokenData(ApiKey.builder().apiKey("apiKey").build()).build()));
        

        var jiraClient = JiraClient.builder()
            .objectMapper(DefaultObjectMapper.get())
            .okHttpClient(okHttpClient)
            .jiraUrlSupplier(InventoryHelper.integrationUrlSupplier(inventoryService, IntegrationKey.builder().tenantId("test").integrationId("1").build(), 5, TimeUnit.MINUTES))
            .disableUrlSanitation(true)
            .allowUnsafeSSL(false)
            .sensitiveFields(List.of())
            .build();
        when(jiraClientFactory.get(any(IntegrationKey.class))).thenReturn(jiraClient);
        when(jiraClientFactory.buildFromInventory(any(IntegrationKey.class))).thenReturn(jiraClient);
        JiraIssueDataSource jiraDataSource = new JiraIssueDataSource(jiraClientFactory);
        var results = jiraDataSource.fetchMany(JiraIssueDataSource.JiraIssueQuery.builder()
                .integrationKey(new IntegrationKey("coke", "jira"))
                .jql("updated >= '2019-11-14' and key = 'LEV-63'")
                .limit(1000)
                .build()).collect(Collectors.toList());
        Assertions.assertThat(results).isNotNull();
        Assertions.assertThat(results.size()).isEqualTo(1);
        Assertions.assertThat(results.get(0).getPayload()).isNotNull();
        Assertions.assertThat(results.get(0).getPayload().getFields().getDynamicFields()).isNotEmpty();
    }
}