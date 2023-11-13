package io.levelops.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.AgentHandle;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.Job;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Client for the agent callback endpoints of the control-plane.
 */
public class IngestionAgentControlClient {

    private final ClientHelper<ControlException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String controlPlaneUrl;

    public static class ControlException extends Exception {
        public ControlException() {
        }

        public ControlException(String message) {
            super(message);
        }

        public ControlException(String message, Throwable cause) {
            super(message, cause);
        }

        public ControlException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * @param controlPlaneUrl url of control plane or levelops ui including needed base path
     */
    @Builder
    public IngestionAgentControlClient(OkHttpClient okHttpClient,
                                       ObjectMapper objectMapper,
                                       String controlPlaneUrl) {
        this.objectMapper = objectMapper;
        this.controlPlaneUrl = controlPlaneUrl;
        clientHelper = ClientHelper.<ControlException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(ControlException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(controlPlaneUrl).newBuilder()
                .addPathSegment("agent-callback");
    }

    public void registerAgent(AgentHandle agentHandle) throws ControlException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("register")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(agentHandle))
                .build();
        clientHelper.executeRequest(request);
    }

    /**
     * @deprecated use {@link io.levelops.services.IngestionAgentControlClient#sendHeartbeat(io.levelops.ingestion.models.AgentHandle)}
     */
    public void sendHeartbeat(String agentId, @Nullable String tenantId) throws ControlException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("heartbeat")
                .addQueryParameter("agent_id", agentId)
                .addQueryParameter("tenant_id", tenantId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        clientHelper.executeRequest(request);
    }

    public void sendHeartbeat(AgentHandle agentHandle) throws ControlException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("heartbeat")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(agentHandle))
                .build();
        clientHelper.executeRequest(request);
    }

    public Map<String, Boolean> sendJobReport(List<Job> jobs, @Nullable String tenantId) throws ControlException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("jobs")
                .addPathSegment("report")
                .addQueryParameter("tenant_id", tenantId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(ListResponse.of(jobs)))
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory()
                .constructMapType(Map.class, String.class, Boolean.class));
    }

    /**
     * List available job requests
     *
     * @return returns 'lite' requests
     */
    public List<CreateJobRequest> listJobRequests(AgentHandle agentHandle, @Nullable Boolean reserved) throws ControlException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("jobs")
                .addPathSegment("requests")
                .addPathSegment("list")
                .addQueryParameter("reserved", reserved != null ? Boolean.toString(reserved) : null)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(agentHandle))
                .build();
        ListResponse<CreateJobRequest> response = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(ListResponse.class, CreateJobRequest.class));
        return response.getRecords();
    }

    public CreateJobRequest acceptJobRequest(String jobId, String agentId, @Nullable String tenantId) throws ControlException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("jobs")
                .addPathSegment("requests")
                .addPathSegment("accept")
                .addQueryParameter("job_id", jobId)
                .addQueryParameter("agent_id", agentId)
                .addQueryParameter("tenant_id", tenantId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, CreateJobRequest.class);
    }

    public void rejectJobRequest(String jobId, String agentId, String status, @Nullable String tenantId) throws ControlException {
        // TODO use proper enum or use another parameter type (current reusing job status)
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("jobs")
                .addPathSegment("requests")
                .addPathSegment("reject")
                .addQueryParameter("job_id", jobId)
                .addQueryParameter("agent_id", agentId)
                .addQueryParameter("status", status)
                .addQueryParameter("tenant_id", tenantId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        clientHelper.executeRequest(request);
    }
}
