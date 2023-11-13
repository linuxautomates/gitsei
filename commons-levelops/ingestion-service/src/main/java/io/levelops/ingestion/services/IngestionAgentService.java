package io.levelops.ingestion.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.models.AgentResponse;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.Entity;
import io.levelops.ingestion.models.Job;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.models.ListResponse;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.List;

public class IngestionAgentService {

    private final ClientHelper<IngestionServiceException> clientHelper;
    private final ObjectMapper objectMapper;

    @Builder
    public IngestionAgentService(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;

        clientHelper = ClientHelper.<IngestionServiceException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(IngestionServiceException.class)
                .build();
    }

    // region jobs

    public Job getJob(String agentUri, String jobId) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(agentUri + "/ingestion-agent/v1/jobs/" + jobId))
                .get()
                .build();
        return clientHelper.executeAndParse(request, Job.class);
    }

    public List<Job> getJobs(String agentUri) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(agentUri + "/ingestion-agent/v1/jobs/"))
                .get()
                .build();
        ListResponse<Job> jobs = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(ListResponse.class, Job.class));
        return jobs.getRecords();
    }

    public void deleteJob(String agentUri, String jobId) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(agentUri + "/ingestion-agent/v1/jobs/" + jobId))
                .delete()
                .build();
        clientHelper.executeRequest(request);
    }

    /**
     * Clears all the completed jobs (done=true).
     */
    public void clearJobs(String agentUri) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(agentUri + "/ingestion-agent/v1/jobs/"))
                .delete()
                .build();
        clientHelper.executeRequest(request);
    }


    public Job submitJob(String agentUri, CreateJobRequest createJobRequest) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(agentUri + "/ingestion-agent/v1/jobs/"))
                .post(clientHelper.createJsonRequestBody(createJobRequest))
                .build();
        return clientHelper.executeAndParse(request, Job.class);
    }

    // endregion

    public List<Entity> getEntities(String agentUri, @Nullable String componentType) throws IngestionServiceException {
        return doGetEntities(agentUri, componentType).getResponse().getRecords();
    }

    private AgentResponse<ListResponse<Entity>> doGetEntities(String agentUri, @Nullable String componentType) throws IngestionServiceException {
        HttpUrl.Builder url = HttpUrl.parse(agentUri + "/ingestion-agent/v1/entities/").newBuilder();
        if (StringUtils.isNotEmpty(componentType)) {
            url.addQueryParameter("component_type", componentType);
        }
         Request request = new Request.Builder()
                .url(url.build())
                .get()
                .build();
        AgentResponse<ListResponse<Entity>> response = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(AgentResponse.class,
                        objectMapper.getTypeFactory().constructParametricType(ListResponse.class, Entity.class)));
        return response;
    }
}
