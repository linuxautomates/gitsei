package io.levelops.aggregations.plugins.clients;

import io.levelops.commons.databases.models.database.IntegrationAgg;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

public class IntegrationAggregationsClientIntegrationTest {
    private IntegrationAggregationsClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new IntegrationAggregationsClient(okHttpClient, DefaultObjectMapper.get(), "http://localhost:8080");
    }

    @Test
    public void getById() throws IntegrationAggregationsClient.IntegrationAggregationsClientException {
        String integrationAggResult = client.getById("foo", "1e1b73b8-9e10-4817-815a-cf2500b80488");
        Assert.assertNotNull(integrationAggResult);
        System.out.println(integrationAggResult);
    }

    @Test
    public void list() throws IntegrationAggregationsClient.IntegrationAggregationsClientException {
        PaginatedResponse<IntegrationAgg> r = client.getIntegrationAggregationResultsList("foo", "36", List.of("72"), List.of("SNYK"), 0, 10);
        Assert.assertNotNull(r);
        DefaultObjectMapper.prettyPrint(r);
    }

    @Test
    public void testGetLatestPluginAggregationByToolType() throws IntegrationAggregationsClient.IntegrationAggregationsClientException {
        Optional<String> r = client.getLatestIntegrationAggregationByToolType("foo", "36", List.of("72"), List.of("SNYK"));
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isPresent());
        DefaultObjectMapper.prettyPrint(r);
    }
}