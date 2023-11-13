package io.levelops.etl.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.models.ListResponse;
import lombok.extern.log4j.Log4j2;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Log4j2
@Service
public class EtlSchedulerClient {
    private final ClientHelper<SchedulerClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String etlSchedulerUrl;

    public EtlSchedulerClient(
            OkHttpClient okHttpClient,
            ObjectMapper objectMapper,
            @Qualifier("etlSchedulerUrl") String etlSchedulerUrl) {
        this.objectMapper = objectMapper;
        this.etlSchedulerUrl = etlSchedulerUrl;
        this.clientHelper = ClientHelper.<SchedulerClientException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(SchedulerClientException.class)
                .build();
    }

    private HttpUrl.Builder baseUrlBuilder() {
        return HttpUrl.parse(etlSchedulerUrl).newBuilder()
                .addPathSegment("v1")
                .addPathSegment("scheduler");
    }

    public List<JobContext> getJobsToRun() throws SchedulerClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("jobs_to_run")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        ListResponse<JobContext> response = clientHelper.executeAndParse(request,
                objectMapper.getTypeFactory().constructParametricType(ListResponse.class, JobContext.class));
        return response.getRecords();
    }

    public boolean claimJob(JobInstanceId jobInstanceId, String workerId) throws SchedulerClientException {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("claim_job")
                .addQueryParameter("job_instance_id", jobInstanceId.toString())
                .addQueryParameter("worker_id", workerId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .patch(RequestBody.create("", null))
                .build();
        try {
            clientHelper.executeRequest(request);
        } catch (Exception e) {
            log.warn("Failed to claim job {}", jobInstanceId, e);
            return false;
        }
        return true;
    }

    public boolean unclaimJob(JobInstanceId jobInstanceId, String workerId) {
        HttpUrl url = baseUrlBuilder()
                .addPathSegment("unclaim_job")
                .addQueryParameter("job_instance_id", jobInstanceId.toString())
                .addQueryParameter("worker_id", workerId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .patch(RequestBody.create("", null))
                .build();
        try {
            clientHelper.executeRequest(request);
        } catch (Exception e) {
            log.error("Failed to unclaim job {}", jobInstanceId, e);
            return false;
        }
        return true;
    }

    public static class SchedulerClientException extends Exception {
        public SchedulerClientException() {
        }

        public SchedulerClientException(String message) {
            super(message);
        }

        public SchedulerClientException(String message, Throwable cause) {
            super(message, cause);
        }

        public SchedulerClientException(Throwable cause) {
            super(cause);
        }
    }
}
