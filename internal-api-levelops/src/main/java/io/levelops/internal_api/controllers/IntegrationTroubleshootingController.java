package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.internal_api.models.troubleshooting.IngestionError;
import io.levelops.internal_api.models.troubleshooting.TimeRange;
import io.levelops.web.util.SpringUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{tenant_id}/integrations/troubleshooting")
public class IntegrationTroubleshootingController {
    private final ControlPlaneService controlPlaneService;

    @Autowired
    public IntegrationTroubleshootingController(ControlPlaneService controlPlaneService) {
        this.controlPlaneService = controlPlaneService;
    }

    @RequestMapping(method = RequestMethod.GET, value = "/{integrationid:[0-9]+}", produces = "application/json")
    public DeferredResult<ResponseEntity<IngestionTroubleshootingSummary>> troubleshootIntegration(@PathVariable("tenant_id") String company,
                                                                                                   @PathVariable("integrationid") String integrationId) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok()
                .body(troubleshootIntegrationInternal(company, integrationId)));
    }

    public IngestionTroubleshootingSummary troubleshootIntegrationInternal(String tenantId, String integrationId) throws IngestionServiceException {
        TriggerResults result = controlPlaneService.getAllTriggerResults(
                IntegrationKey.builder()
                        .tenantId(tenantId).integrationId(integrationId).build(),
                false,
                true,
                false
        ).getTriggerResults().get(0);
        IngestionFailuresSummary failuresSummary = checkForIngestionJobFailures(result);
        var successTimeline = getIngestionTimeline(result);
        return IngestionTroubleshootingSummary.builder()
                .ingestionFailuresSummary(failuresSummary)
                .successTimeline(successTimeline)
                .build();
        // TODO: Add ETL monitoring after exposing the ETL data structures in commons
    }

    private IngestionJobSummary getIngestionJobSummary(JobDTO jobDto) throws ParseException {
        var querySummary = new HashMap<String, Object>();
        Instant from = null, to = null;
        if (jobDto.getQuery() instanceof Map) {
            Map query = (Map) jobDto.getQuery();
            if (query.containsKey("from")) {
                if (query.get("from") instanceof Long) {
                    from = Instant.ofEpochMilli((Long) query.get("from"));
                } else if (query.get("from") instanceof String) {
                    from = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse((String) query.get("from")).toInstant();
                }
            }
            if (query.containsKey("to")) {
                if (query.get("to") instanceof Long){
                    to = Instant.ofEpochMilli((Long) query.get("to"));
                } else if (query.get("to") instanceof String) {
                    to = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").parse((String) query.get("to")).toInstant();
                }
            }
        }
        return IngestionJobSummary.builder()
                .ingestionJobId(jobDto.getId())
                .timeRange(TimeRange.builder()
                        .from(from)
                        .to(to).build())
                .build();
    }

    private IngestionFailuresSummary checkForIngestionJobFailures(TriggerResults results) {
        var failedJobs = results.getJobs().stream()
                .filter(jobDto -> jobDto.getStatus() == JobStatus.FAILURE);

        var failedJobsWithError = failedJobs
                .map(RuntimeStreamException.wrap(jobDto -> {
                    var errorString = jobDto.getError().toString();
                    IngestionError error = IngestionError.detectError(errorString);

                    return IngestionJobFailureInfo.builder()
                            .jobSummary(getIngestionJobSummary(jobDto))
                            .errorMessage(error.getMessage())
                            .build();
                }))
                .collect(Collectors.toList());
        return IngestionFailuresSummary.builder()
                .totalJobsCount(results.getJobs().size())
                .successfulJobsCount((int) results.getJobs().stream().filter(jobDto -> jobDto.getStatus() == JobStatus.SUCCESS).count())
                .failedJobsCount(failedJobsWithError.size())
                .failedJobsInfo(failedJobsWithError)
                .build();
    }

    private List<TimeRange> getIngestionTimeline(TriggerResults results) {
        var successfulJobTimeRanges = results.getJobs().stream()
                .filter(jobDto -> jobDto.getStatus() == JobStatus.SUCCESS)
                .map(RuntimeStreamException.wrap(this::getIngestionJobSummary))
                .map(IngestionJobSummary::getTimeRange)
                .collect(Collectors.toList());

        return TimeRange.createTimeline(successfulJobTimeRanges);
    }

    @Value
    @Builder
    public static class IngestionTroubleshootingSummary {
        @JsonProperty("ingestion_failure_summary")
        IngestionFailuresSummary ingestionFailuresSummary;

        @JsonProperty("success_timeline")
        List<TimeRange> successTimeline;
    }

    @Value
    @Builder
    public static class IngestionFailuresSummary {
        @JsonProperty("total_jobs_count")
        int totalJobsCount;

        @JsonProperty("successful_jobs_count")
        int successfulJobsCount;

        @JsonProperty("failed_jobs_count")
        int failedJobsCount;

        @JsonProperty("failed_jobs_info_list")
        List<IngestionJobFailureInfo> failedJobsInfo;
    }

    @Value
    @Builder
    public static class IngestionJobSummary {
        @JsonProperty("ingestion_job_id")
        String ingestionJobId;

        @JsonProperty("time_range")
        TimeRange timeRange;
    }


    @Value
    @Builder
    public static class IngestionJobFailureInfo {
        @JsonProperty("ingestion_job_summary")
        IngestionJobSummary jobSummary;

        @JsonProperty("message")
        String errorMessage;
    }
}
