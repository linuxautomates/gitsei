package io.levelops.commons.etl;

import io.levelops.commons.jackson.DefaultObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.Test;

import java.util.List;

public class EtlMonitoringClientIntegrationTest {
    @Test
    public void testSummary() throws EtlMonitoringClient.EtlMonitoringClientException {
        OkHttpClient okHttpClient = new OkHttpClient();
        EtlMonitoringClient etlMonitoringClient = new EtlMonitoringClient(
            okHttpClient, DefaultObjectMapper.get(), "http://127.0.0.1:8082"
        );
        var result = etlMonitoringClient.getJobDefinitionSummary(null, "qaaggstest", "4");
        System.out.println(result);
    }

    @Test
    public void testGetJobInstances() throws EtlMonitoringClient.EtlMonitoringClientException {
        OkHttpClient okHttpClient = new OkHttpClient();
        EtlMonitoringClient etlMonitoringClient = new EtlMonitoringClient(
                okHttpClient, DefaultObjectMapper.get(), "http://127.0.0.1:8082"
        );
        var result = etlMonitoringClient.getJobInstances("foo", "4323", List.of("ba280e35-8981-434c-a380-a2a69f787f7e"));
        System.out.println(result);
    }
}