package io.levelops.commons.etl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.etl.models.DbJobInstance;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.List;
import java.util.Map;

public class EtlMonitoringClient {
    private final ClientHelper<EtlMonitoringClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String etlSchedulerUrl;

    @Builder
    public EtlMonitoringClient(final OkHttpClient client, final ObjectMapper objectMapper, final String facetedSearchControllerServiceUri) {
        this.objectMapper = objectMapper;
        this.etlSchedulerUrl = facetedSearchControllerServiceUri;
        this.clientHelper = new ClientHelper<>(client, objectMapper, EtlMonitoringClientException.class);

    }

    private HttpUrl.Builder getBaseUrlBuilder() {
        return HttpUrl.parse(etlSchedulerUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("monitoring");
    }

    public Map<String, Object> getJobDefinitionSummary(
            String jobDefinitionId,
            String tenantId,
            String integrationId
    ) throws EtlMonitoringClientException {
        HttpUrl url = getBaseUrlBuilder()
                .addPathSegment("job_summary")
                .addQueryParameter("job_definition_id", jobDefinitionId)
                .addQueryParameter("tenant_id", tenantId)
                .addQueryParameter("integration_id", integrationId)
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, Map.class);
    }

    public  Map<String, DbJobInstance> getJobInstances(
            String tenantId,
            String integrationId,
            List<String> ingestionJobIds
    ) throws EtlMonitoringClientException {
        ///ingestion_processing/job_instances
        HttpUrl url = getBaseUrlBuilder()
                .addPathSegment("ingestion_processing")
                .addPathSegment("job_instances")
                .addQueryParameter("tenant_id", tenantId)
                .addQueryParameter("integration_id", integrationId)
                .addQueryParameter("ingestion_job_ids", String.join(",", ingestionJobIds))
                .build();

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(
                request,
                objectMapper.getTypeFactory().constructParametricType(Map.class, String.class, DbJobInstance.class));
    }

    public static class EtlMonitoringClientException extends Exception {
        public EtlMonitoringClientException() {
        }

        public EtlMonitoringClientException(String message) {
            super(message);
        }

        public EtlMonitoringClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public EtlMonitoringClientException(Throwable cause) {
            super(cause);
        }
    }
}
