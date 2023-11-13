package io.levelops.aggregations.plugins.clients;


import io.levelops.aggregations.models.JenkinsMonitoringAggDataDTO;
import io.levelops.commons.databases.models.database.AggregationRecord;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.models.PaginatedResponse;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

public class PluginResultAggregationsClientIntegrationTest {
    private PluginResultAggregationsClient client;

    @Before
    public void setUp() throws Exception {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.level(HttpLoggingInterceptor.Level.BODY);
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addNetworkInterceptor(logging).build();
        client = new PluginResultAggregationsClient(okHttpClient, DefaultObjectMapper.get(), "http://localhost:8080");
    }

    @Test
    public void getById() throws PluginResultAggregationsClient.PluginResultAggregationsClientException {
        JenkinsMonitoringAggDataDTO jenkinsMonitoringAggDataDTO = client.getById("foo", "9d3230cc-dfde-4ba3-93e5-79015f87416e");
        Assert.assertNotNull(jenkinsMonitoringAggDataDTO);
        System.out.println(jenkinsMonitoringAggDataDTO);
    }

    @Test
    public void list() throws PluginResultAggregationsClient.PluginResultAggregationsClientException {
        PaginatedResponse<AggregationRecord> r = client.getPluginAggregationResultsList("foo", null, null);
        Assert.assertNotNull(r);
        DefaultObjectMapper.prettyPrint(r);
    }

    @Test
    public void testGetLatestPluginAggregationByToolType() throws PluginResultAggregationsClient.PluginResultAggregationsClientException {
        Optional<JenkinsMonitoringAggDataDTO> r = client.getLatestPluginAggregationByToolType("foo", null, "jenkins_config");
        Assert.assertNotNull(r);
        Assert.assertTrue(r.isPresent());
        DefaultObjectMapper.prettyPrint(r);
    }

}