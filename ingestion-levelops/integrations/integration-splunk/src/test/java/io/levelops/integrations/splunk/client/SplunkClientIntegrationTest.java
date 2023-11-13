package io.levelops.integrations.splunk.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.Token;
import io.levelops.commons.databases.models.database.tokens.ApiKey;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public class SplunkClientIntegrationTest {
    private final String QUERY = "search=search index=main host=splunk-client-enterprise-test-1";
    private final String splunkOAuthToken = System.getenv("SPLUNK-OAUTH-TOKEN");
    private final long currentTime = System.currentTimeMillis();
    private final OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder().readTimeout(3, TimeUnit.MINUTES);
    private final OkHttpClient client;
    private ObjectMapper mapper = DefaultObjectMapper.get();
    private InventoryService inventoryService = Mockito.mock(InventoryService.class);
    private final SplunkClientFactory clientFactory;
    private final IntegrationKey key = IntegrationKey.builder().tenantId("coke").integrationId("splunk1").build();
    private final Integration integration = Integration.builder().id(key.getIntegrationId()).application("splunk").createdAt(currentTime).url("https://localhost:8089").build();
    private final ApiKey tokenData = ApiKey.builder()
            .apiKey(splunkOAuthToken).createdAt(currentTime).build();
    private final Token token = Token.builder()
            .id("1234").integrationId(key.getIntegrationId()).tokenData(tokenData).createdAt(currentTime).build();

    public SplunkClientIntegrationTest() throws KeyManagementException, NoSuchAlgorithmException {
        client = ClientHelper.configureToIgnoreCertificate(clientBuilder).build();
        clientFactory = SplunkClientFactory.builder()
                .okHttpClient(client)
                .objectMapper(mapper)
                .inventoryService(inventoryService)
                .build();
    }

    @Before
    public void setup() throws InventoryException {
        Mockito.when(inventoryService.listTokens(key)).thenReturn(Arrays.asList(token));
        Mockito.when(inventoryService.getIntegration(key)).thenReturn(integration);
        Mockito.when(inventoryService.getToken(key.getTenantId(), key.getIntegrationId(), token.getId())).thenReturn(token);
    }

    @Test
    public void testSearch() throws SplunkClientException {
        MutableInt i = new MutableInt(1);
        try(Stream<String> results = clientFactory.get(key).search(QUERY)){
            results.forEach(l -> {
                System.out.println("-------" + i + "------");
                System.out.println(l);
                i.add(1);
            });
        }
    }
}