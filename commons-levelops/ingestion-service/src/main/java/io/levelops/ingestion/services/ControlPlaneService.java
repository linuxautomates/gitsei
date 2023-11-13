package io.levelops.ingestion.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.client.ClientHelper;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.ListResponse;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.controlplane.models.RegisteredAgent;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.SubmitJobResponse;
import io.levelops.ingestion.models.TriggerType;
import io.levelops.ingestion.models.controlplane.CreateTriggerRequest;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import lombok.Builder;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ControlPlaneService {

    private static final int OPTIMIZE_GET_ALL_TRIGGER_RESULTS_PAGE_SIZE = 10;
    private final ClientHelper<IngestionServiceException> clientHelper;
    private final ObjectMapper objectMapper;
    private final String controlPlaneUrl;
    private final Boolean optimizeGetAllTriggerResults;

    @Builder
    public ControlPlaneService(OkHttpClient okHttpClient,
                               ObjectMapper objectMapper,
                               String controlPlaneUrl,
                               Boolean optimizeGetAllTriggerResults) {
        this.objectMapper = objectMapper;
        this.controlPlaneUrl = StringUtils.stripEnd(controlPlaneUrl, "/");
        this.optimizeGetAllTriggerResults = BooleanUtils.isTrue(optimizeGetAllTriggerResults);
        clientHelper = ClientHelper.<IngestionServiceException>builder()
                .client(okHttpClient)
                .objectMapper(objectMapper)
                .exception(IngestionServiceException.class)
                .build();
    }

    // region --- Triggers ---

    /**
     * Create Trigger and return its generated id.
     */
    public String createTrigger(CreateTriggerRequest createTriggerRequest) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers"))
                .post(clientHelper.createJsonRequestBody(createTriggerRequest))
                .build();
        return (String) clientHelper.executeAndParse(request, Map.class).get("id");
    }

    public void updateTriggerFrequency(String triggerId, int frequency) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("frequency")
                .addQueryParameter("frequency", String.valueOf(frequency))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create("", null))
                .build();
        clientHelper.executeRequest(request);
    }

    public void enableHistoricalTrigger(String triggerId, int spanInDays, int subJobSpanInMinutes, int successiveBackwardScanCount) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("enableHistoricalTrigger")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(Map.of(
                        "historical_span_in_days", spanInDays,
                    "historical_sub_job_span_in_min", subJobSpanInMinutes,
                    "historical_successive_backward_scan_count", successiveBackwardScanCount
                )))
                .build();
        clientHelper.executeRequest(request);
    }

    public void updateMetadata(String triggerId, Map<String, Object> metadata) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("metadata")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .put(clientHelper.createJsonRequestBody(metadata))
                .build();
        clientHelper.executeRequest(request);
    }

    public PaginatedResponse<DbTrigger> getTriggers(String company, int page, @Nullable String integrationId) throws IngestionServiceException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers").newBuilder()
                .addQueryParameter("tenant_id", company)
                .addQueryParameter("page", String.valueOf(page));
        if (StringUtils.isNotBlank(integrationId)) {
            urlBuilder.addQueryParameter("integration_id", integrationId);
        }

        //http://localhost:8081/control-plane/v1/triggers?tenant_id=nutanix
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        return clientHelper.executeAndParse(request, PaginatedResponse.typeOf(objectMapper, DbTrigger.class));
    }

    public PaginatedResponse<DbTrigger> getTriggers(@Nullable String company, int page, @Nullable String integrationId, @Nullable TriggerType triggerType) throws IngestionServiceException {
        HttpUrl.Builder urlBuilder = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers").newBuilder()
                .addQueryParameter("page", String.valueOf(page));

        if (StringUtils.isNotBlank(company)) {
            urlBuilder.addQueryParameter("tenant_id", company);
        }
        if (Objects.nonNull(triggerType)) {
            urlBuilder.addQueryParameter("trigger_type", triggerType.getType());
        }
        if (StringUtils.isNotBlank(integrationId)) {
            urlBuilder.addQueryParameter("integration_id", integrationId);
        }

        //http://localhost:8081/control-plane/v1/triggers?tenant_id=nutanix
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        return clientHelper.executeAndParse(request, PaginatedResponse.typeOf(objectMapper, DbTrigger.class));
    }

    public List<String> getTriggerIds(String company, String integrationId) throws IngestionServiceException {
        try {
            return streamTriggers(company, integrationId)
                    .map(DbTrigger::getId)
                    .collect(Collectors.toList());
        } catch (RuntimeStreamException e) {
            if (e.getCause() instanceof IngestionServiceException) {
                throw (IngestionServiceException) e.getCause();
            }
            throw e;
        }
    }

    public Stream<DbTrigger> streamTriggers(String company, String integrationId) throws RuntimeStreamException {
        return PaginationUtils.streamThrowingRuntime(0, 1, page -> getTriggers(company, page, integrationId).getResponse().getRecords());
    }

    public Stream<DbTrigger> streamTriggers(@Nullable String company, @Nullable String integrationId, @Nullable TriggerType triggerType) throws RuntimeStreamException {
        return PaginationUtils.streamThrowingRuntime(0, 1, page -> getTriggers(company, page, integrationId, triggerType).getResponse().getRecords());
    }

    public boolean deleleTrigger(String triggerId) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers").newBuilder()
                        .addPathSegment(triggerId)
                        .build())
                .delete()
                .build();
        return (Boolean) clientHelper.executeAndParse(request, Map.class).get("deleted");
    }

    public int deleleTriggersByIntegrationKey(IntegrationKey integrationKey) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers").newBuilder()
                        .addQueryParameter("tenant_id", integrationKey.getTenantId())
                        .addQueryParameter("integration_id", integrationKey.getIntegrationId())
                        .build())
                .delete()
                .build();
        return (Integer) clientHelper.executeAndParse(request, Map.class).get("deleted");
    }

    /**
     * Get results for a single iteration
     */
    public TriggerResults getTriggerResultsAtIteration(String triggerId, String iterationId) throws IngestionServiceException {
        // get /v1/triggers/{triggerId}/iterations/{iterationId}/results
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("iterations")
                .addPathSegment(iterationId)
                .addPathSegment("results")
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, TriggerResults.class);
    }

    /**
     * Retrieve the results of all the jobs created by that trigger.
     *
     * @param partial               Boolean:
     *                              - If True, then returns the latest iteration of the jobs only.
     *                              - If False, then returns the results of all the iterations of the jobs until a full iteration is found.
     * @param allowEmptyResults     If True, jobs with *explicitly* empty results will be omitted (default: false)
     * @param onlySuccessfulResults If True, only successful jobs will be returned (default: true)
     */
    public TriggerResults getTriggerResults(String triggerId,
                                            boolean partial,
                                            boolean allowEmptyResults,
                                            boolean onlySuccessfulResults) throws IngestionServiceException {
        return getTriggerResults(triggerId, partial, allowEmptyResults, onlySuccessfulResults, true);
    }

    public TriggerResults getTriggerResults(String triggerId, boolean partial, boolean allowEmptyResults, boolean onlySuccessfulResults, boolean includeResultField) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("results")
                .addPathSegment("latest")
                .addQueryParameter("partial", Boolean.toString(partial))
                .addQueryParameter("allow_empty_results", Boolean.toString(allowEmptyResults))
                .addQueryParameter("only_successful_results", Boolean.toString(onlySuccessfulResults))
                .addQueryParameter("include_job_result_field", Boolean.toString(includeResultField))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, TriggerResults.class);
    }

    // paginated
    public TriggerResults getTriggerResults(int page, int pageSize,
                                            String triggerId,
                                            boolean partial,
                                            boolean allowEmptyResults,
                                            boolean onlySuccessfulResults) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("results")
                .addPathSegment("latest")
                .addPathSegment("list")
                .addQueryParameter("partial", Boolean.toString(partial))
                .addQueryParameter("allow_empty_results", Boolean.toString(allowEmptyResults))
                .addQueryParameter("only_successful_results", Boolean.toString(onlySuccessfulResults))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .post(clientHelper.createJsonRequestBody(DefaultListRequest.builder()
                        .page(page)
                        .pageSize(pageSize)
                        .build()))
                .build();
        return clientHelper.executeAndParse(request, TriggerResults.class);
    }

    public Stream<JobDTO> streamTriggerResults(int pageSize,
                                               String triggerId,
                                               boolean partial,
                                               boolean allowEmptyResults,
                                               boolean onlySuccessfulResults) throws RuntimeStreamException {
        MutableInt page = new MutableInt(0);
        return PaginationUtils.stream(() -> {
                    try {
                        return getTriggerResults(page.getAndIncrement(), pageSize, triggerId, partial, allowEmptyResults, onlySuccessfulResults);
                    } catch (IngestionServiceException e) {
                        throw new RuntimeStreamException(e);
                    }
                }, triggerResults -> BooleanUtils.isTrue(triggerResults.getHasNext()) || CollectionUtils.isNotEmpty(triggerResults.getJobs()))
                .filter(Objects::nonNull)
                .flatMap(triggerResults -> triggerResults.getJobs().stream())
                .filter(Objects::nonNull);
    }


    /**
     * Similar to {@link ControlPlaneService#getTriggerResults(java.lang.String, boolean, boolean, boolean)},
     * but only return results between the last full scan (if not partial) and the given iterationId, instead of
     * the latest results.
     */
    public TriggerResults getTriggerResults(String triggerId,
                                            String iterationId,
                                            boolean partial,
                                            boolean allowEmptyResults,
                                            boolean onlySuccessfulResults) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("results")
                .addQueryParameter("iteration_id", iterationId)
                .addQueryParameter("partial", Boolean.toString(partial))
                .addQueryParameter("allow_empty_results", Boolean.toString(allowEmptyResults))
                .addQueryParameter("only_successful_results", Boolean.toString(onlySuccessfulResults))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, TriggerResults.class);
    }

    /**
     * Similar to {@link ControlPlaneService#getTriggerResults(java.lang.String, boolean, boolean, boolean)},
     * but only return results between the last full scan (if not partial) and the given iterationId, instead of
     * the latest results.
     */
    public TriggerResults getTriggerResults(String triggerId,
                                            int lastNJobs,
                                            boolean partial,
                                            boolean allowEmptyResults,
                                            boolean onlySuccessfulResults,
                                            long before) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/").newBuilder()
                .addPathSegment(triggerId)
                .addPathSegment("results")
                .addQueryParameter("last_n_jobs", Integer.toString(lastNJobs))
                .addQueryParameter("partial", Boolean.toString(partial))
                .addQueryParameter("allow_empty_results", Boolean.toString(allowEmptyResults))
                .addQueryParameter("only_successful_results", Boolean.toString(onlySuccessfulResults))
                .addQueryParameter("before", Long.toString(before))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, TriggerResults.class);
    }


    /**
     * Get all results from all triggers of given integration.
     * See {@link ControlPlaneService#getTriggerResults(String, boolean, boolean, boolean)} for documentation on parameters.
     */
    public MultipleTriggerResults getAllTriggerResults(IntegrationKey integrationKey,
                                                       boolean partial,
                                                       boolean allowEmptyResults,
                                                       boolean onlySuccessfulResults) throws IngestionServiceException {
        String tenantId = integrationKey.getTenantId();
        String integrationId = integrationKey.getIntegrationId();
        Validate.notBlank(tenantId, "integrationKey.getTenantId() cannot be null or empty.");
        Validate.notBlank(integrationId, "integrationKey.getIntegrationId() cannot be null or empty.");
        if (optimizeGetAllTriggerResults) {
            try {
                Stream<DbTrigger> triggers = streamTriggers(tenantId, integrationId);
                return MultipleTriggerResults.builder()
                        .integrationId(integrationId)
                        .tenantId(tenantId)
                        .partial(Boolean.TRUE.equals(partial))
                        .triggerResults(triggers
                                .map(trigger -> TriggerResults.builder()
                                        .triggerId(trigger.getId())
                                        .triggerType(trigger.getType())
                                        .integrationId(integrationId)
                                        .iterationId(trigger.getIterationId())
                                        .tenantId(tenantId)
                                        .partial(partial)
                                        .jobs(streamTriggerResults(OPTIMIZE_GET_ALL_TRIGGER_RESULTS_PAGE_SIZE, trigger.getId(),
                                                partial, allowEmptyResults, onlySuccessfulResults)
                                                .collect(Collectors.toList()))
                                        .build())
                                .collect(Collectors.toList()))
                        .build();
            } catch (RuntimeStreamException e) {
                if (e.getCause() instanceof IngestionServiceException) {
                    throw (IngestionServiceException) e.getCause();
                }
                throw e;
            }
        } else {
            return doGetAllTriggerResults(integrationKey, partial, allowEmptyResults, onlySuccessfulResults);
        }
    }

    private MultipleTriggerResults doGetAllTriggerResults(IntegrationKey integrationKey,
                                                          boolean partial,
                                                          boolean allowEmptyResults,
                                                          boolean onlySuccessfulResults) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/triggers/all/results/latest").newBuilder()
                .addQueryParameter("integration_id", integrationKey.getIntegrationId())
                .addQueryParameter("tenant_id", integrationKey.getTenantId())
                .addQueryParameter("partial", Boolean.toString(partial))
                .addQueryParameter("allow_empty_results", Boolean.toString(allowEmptyResults))
                .addQueryParameter("only_successful_results", Boolean.toString(onlySuccessfulResults))
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, MultipleTriggerResults.class);
    }

    // endregion

    // region --- Jobs ---

    /**
     * Poll Job information by jobId
     */
    public JobDTO getJob(String jobId) throws IngestionServiceException {
        HttpUrl url = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/jobs/").newBuilder()
                .addPathSegment(jobId)
                .build();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();
        return clientHelper.executeAndParse(request, JobDTO.class);
    }

    public PaginatedResponse<JobDTO> getJobs(String company, int page, @Nullable Integer pageSize,
                                             @Nullable String triggerId,
                                             @Nullable List<String> statuses,
                                             @Nullable Long before,
                                             @Nullable Long after,
                                             @Nullable Boolean returnTotalCount,
                                             @Nullable Boolean includeResultField) throws IngestionServiceException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/jobs").newBuilder()
                .addQueryParameter("tenant_id", company)
                .addQueryParameter("page", String.valueOf(page));
        if (pageSize != null) {
            urlBuilder.addQueryParameter("page_size", String.valueOf(pageSize));
        }
        if (StringUtils.isNotBlank(triggerId)) {
            urlBuilder.addQueryParameter("trigger_id", triggerId);
        }
        if (CollectionUtils.isNotEmpty(statuses)) {
            urlBuilder.addQueryParameter("statuses", String.join(",", statuses));
        }
        if (before != null) {
            urlBuilder.addQueryParameter("before", String.valueOf(before));
        }
        if (after != null) {
            urlBuilder.addQueryParameter("after", String.valueOf(after));
        }
        if (returnTotalCount != null) {
            urlBuilder.addQueryParameter("return_total_count", String.valueOf(returnTotalCount));
        }
        if (includeResultField != null) {
            urlBuilder.addQueryParameter("include_job_result_field", String.valueOf(includeResultField));
        }
        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .get()
                .build();
        return clientHelper.executeAndParse(request, PaginatedResponse.typeOf(objectMapper, JobDTO.class));
    }

    public SubmitJobResponse submitJob(CreateJobRequest createJobRequest) throws IngestionServiceException {
        Request request = new Request.Builder()
                .url(HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/jobs/"))
                .post(clientHelper.createJsonRequestBody(createJobRequest))
                .build();
        return clientHelper.executeAndParse(request, SubmitJobResponse.class);
    }

    // endregion

    public ListResponse<RegisteredAgent> getRegisteredAgents() throws IngestionServiceException {
        // TODO add pagination to this endpoint
        // http://localhost:8081/control-plane/v1/agents
        Request request = new Request.Builder()
                .url(HttpUrl.parse(controlPlaneUrl + "/control-plane/v1/agents"))
                .get()
                .build();
        return clientHelper.executeAndParse(request, ListResponse.typeOf(objectMapper, RegisteredAgent.class));
    }
}
