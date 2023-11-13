package io.levelops.cicd;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.cicd.services.CiCdService;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.databases.models.database.CICDJob;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.CICDJobRunCommits;
import io.levelops.commons.databases.models.database.cicd.JobRunStage;
import io.levelops.commons.databases.models.database.cicd.PathSegment;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.exceptions.InternalApiClientException;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CiCdRESTClient implements CiCdService {

    private final ClientHelper<InternalApiClientException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String internalApiUri;

    public CiCdRESTClient(final String internalApiUri, final OkHttpClient client, final ObjectMapper mapper) {
        this.internalApiUri = internalApiUri;
        this.objectMapper = mapper;
        this.clientHelper = ClientHelper.<InternalApiClientException>builder()
            .objectMapper(mapper)
            .client(client)
            .exception(InternalApiClientException.class).build();
    }

    private HttpUrl.Builder baseUrlBuilder(String company) {
        return HttpUrl.parse(internalApiUri).newBuilder()
                .addPathSegment("internal")
                .addPathSegment("v1")
                .addPathSegment("tenants")
                .addPathSegment(company)
                .addPathSegment("cicd");
    }

    @Override
    public CICDJob getCiCdJob(String company, String jobId)
            throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("jobs")
                .addPathSegment(jobId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, CICDJob.class);
    }

    @Override
    public CICDJobRun getCiCdJobRun(String company, String jobRunId)
            throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("job_runs")
                .addPathSegment(jobRunId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, CICDJobRun.class);
    }

    @Override
    public JobRunStage getJobRunStage(String company, String stageId) throws Exception {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("job_run_stages")
                .addPathSegment(stageId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, JobRunStage.class);
    }

    @Override
    public DbListResponse<JobRunStage> listJobRunStages(String company, String jobRunId, DefaultListRequest defaultListRequest) throws InternalApiClientException {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("job_runs")
                .addPathSegment(jobRunId)
                .addPathSegment("stages")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(defaultListRequest))
                .build();
        return clientHelper.executeAndParse(request, DbListResponse.typeOf(objectMapper, JobRunStage.class));
    }

    @Override
    public Set<PathSegment> getJobRunFullPath(String company, String jobRunId, boolean bottomUp) throws Exception {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("job_runs")
                .addPathSegment(jobRunId)
                .addPathSegment("full_path")
                .addQueryParameter("bottom_up", Boolean.toString(bottomUp))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructCollectionType(Set.class, PathSegment.class));
    }

    @Override
    public Set<PathSegment> getJobStageFullPath(String company, String stageId, boolean bottomUp) throws Exception {
        HttpUrl url = baseUrlBuilder(company)
                .addPathSegment("job_run_stages")
                .addPathSegment(stageId)
                .addPathSegment("full_path")
                .addQueryParameter("bottom_up", Boolean.toString(bottomUp))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, objectMapper.getTypeFactory().constructCollectionType(Set.class, PathSegment.class));

    }

}