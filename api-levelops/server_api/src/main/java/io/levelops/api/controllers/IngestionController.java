package io.levelops.api.controllers;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import io.levelops.api.config.IngestionConfig;
import io.levelops.api.model.IngestionStatus;
import io.levelops.api.utils.SelfServeEndpointUtils;
import io.levelops.commons.databases.models.database.ActivityLog;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.services.ActivityLogService;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.exceptions.InventoryException;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.jackson.ParsingUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.ingestion.clients.IngestionClient;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.TriggerType;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.exceptions.NotFoundException;
import io.levelops.web.util.SpringUtils;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableLong;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@Log4j2
@RequestMapping("/v1/ingestion")
@SuppressWarnings("unused")
public class IngestionController {

    private static final long CACHE_TTL_IN_SECONDS = TimeUnit.MINUTES.toSeconds(5);
    private static final int WARNING_ATTEMPTS_THRESHOLD = 3;
    private static final int LOG_HISTORY_IN_DAYS = 7;
    private final ObjectMapper objectMapper;
    private final ControlPlaneService controlPlaneService;
    private final RedisConnectionFactory redisConnectionFactory;
    private final InventoryService inventoryService;
    private final IngestionClient ingestionClient;
    private final ActivityLogService activityLogService;
    private final IngestionConfig.IngestionTriggerSettings triggerSettings;

    private enum INGESTION_UNSUPPORTED_APPS {JENKINS, CUSTOM}


    @Autowired
    public IngestionController(ObjectMapper objectMapper,
                               ControlPlaneService controlPlaneService,
                               RedisConnectionFactory redisConnectionFactory,
                               InventoryService inventoryService,
                               IngestionClient ingestionClient,
                               ActivityLogService activityLogService,
                               IngestionConfig.IngestionTriggerSettings triggerSettings) {
        this.objectMapper = objectMapper;
        this.controlPlaneService = controlPlaneService;
        this.redisConnectionFactory = redisConnectionFactory;
        this.inventoryService = inventoryService;
        this.ingestionClient = ingestionClient;
        this.activityLogService = activityLogService;
        this.triggerSettings = triggerSettings;
    }

    @PutMapping("/{integrationId:[0-9]+}/pause")
    public DeferredResult<ResponseEntity<Map<String, Object>>> pause(@PathVariable("integrationId") String integrationId,
                                                                     @SessionAttribute(name = "company") String company,
                                                                     @SessionAttribute(name = "session_user") String sessionUser) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            DbTrigger trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);

            // if trigger is already paused, ignore
            if (trigger.getFrequency() != null && trigger.getFrequency() == 0) {
                return ResponseEntity.ok(Map.of(
                        "status", "no-op",
                        "frequency", 0));
            }

            controlPlaneService.updateTriggerFrequency(trigger.getId(), 0);
            insertFrequencyActivityLog(company, integrationId, sessionUser, trigger, 0);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "frequency", 0));
        });
    }

    @PutMapping("/{integrationId:[0-9]+}/unpause")
    public DeferredResult<ResponseEntity<Map<String, Object>>> unpause(@PathVariable("integrationId") String integrationId,
                                                                       @SessionAttribute(name = "company") String company,
                                                                       @SessionAttribute(name = "session_user") String sessionUser) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            DbTrigger trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);

            // if trigger is already running, ignore
            if (trigger.getFrequency() != null && trigger.getFrequency() > 0) {
                return ResponseEntity.ok(Map.of(
                        "status", "no-op",
                        "frequency", trigger.getFrequency()));
            }

            int frequency = triggerSettings.getTriggerFrequency(trigger.getType());
            log.info("Starting ingestion for company={} integration={} freq={}", company, integrationId, frequency);
            controlPlaneService.updateTriggerFrequency(trigger.getId(), frequency);

            insertFrequencyActivityLog(company, integrationId, sessionUser, trigger, frequency);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "frequency", frequency));
        });
    }

    @GetMapping("/{integrationId:[0-9]+}/frequency")
    private DeferredResult<ResponseEntity<Map<String, Object>>> getFrequency(@PathVariable("integrationId") String integrationId,
                                                                             @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            DbTrigger trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);

            return ResponseEntity.ok(Map.of(
                    "status", trigger.getFrequency() > 0 ? "running" : "paused",
                    "frequency", trigger.getFrequency()));
        });
    }

    @PutMapping("/{integrationId:[0-9]+}/frequency")
    private DeferredResult<ResponseEntity<Map<String, Object>>> setFrequency(@PathVariable("integrationId") String integrationId,
                                                                             @SessionAttribute(name = "company") String company,
                                                                             @SessionAttribute(name = "session_user") String sessionUser,
                                                                             @RequestParam(value = "frequency") Integer frequency) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            Validate.notNull(frequency, "frequency cannot be null.");
            DbTrigger trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);

            // if trigger is already running with same frequency, ignore
            if (trigger.getFrequency() != null && trigger.getFrequency().equals(frequency)) {
                return ResponseEntity.ok(Map.of(
                        "status", "no-op",
                        "frequency", trigger.getFrequency()));
            }

            controlPlaneService.updateTriggerFrequency(trigger.getId(), frequency);

            insertFrequencyActivityLog(company, integrationId, sessionUser, trigger, frequency);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "frequency", frequency));
        });
    }

    private void insertFrequencyActivityLog(String company, String integrationId, String sessionUser, DbTrigger trigger, int frequency) throws SQLException {
        activityLogService.insert(company, ActivityLog.builder()
                .targetItem(integrationId)
                .email(sessionUser)
                .targetItemType(ActivityLog.TargetItemType.INTEGRATION)
                .body("Changed Ingestion Frequency")
                .details(Map.of(
                        "trigger_id", trigger.getId(),
                        "trigger_type", trigger.getType(),
                        "frequency", frequency))
                .action(ActivityLog.Action.SUCCESS)
                .build());
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
    @PostMapping("/status")
    public DeferredResult<ResponseEntity<PaginatedResponse<IngestionStatus>>> getIngestionStatusBulk(@SessionAttribute(name = "company") String company,
                                                                                                     @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            List<String> integrationIds = request.<String>getFilterValueAsList("integration_ids").orElse(List.of());
            List<IngestionStatus> statusList = integrationIds.stream()
                    .skip((long) request.getPage() * request.getPageSize())
                    .limit(request.getPageSize())
                    .map(RuntimeStreamException.wrap(integrationId -> getStatusCached(company, integrationId)))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), integrationIds.size(), statusList));
        });
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{integrationId:[0-9]+}/status")
    public DeferredResult<ResponseEntity<IngestionStatus>> getIngestionStatus(@PathVariable("integrationId") String integrationId,
                                                                              @SessionAttribute(name = "company") String company,
                                                                              @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(getStatusCached(company, integrationId)));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{integrationId:[0-9]+}/logs")
    public DeferredResult<ResponseEntity<PaginatedResponse<IngestionLogDTO>>> getIngestionLogs(@PathVariable("integrationId") String integrationId,
                                                                                               @RequestParam(name = "include_result_field", required = false, defaultValue = "false") Boolean includeResultField,
                                                                                               @SessionAttribute(name = "company") String company,
                                                                                               @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(ingestionClient.getIngestionLogs(company, integrationId, request, includeResultField)));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @GetMapping("/{integrationId:[0-9]+}/trigger")
    public DeferredResult<ResponseEntity<DbTrigger>> getTriggerForIntegration(@PathVariable("integrationId") String integrationId,
                                                                              @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> {
            DbTrigger trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);
            return ResponseEntity.ok(trigger);
        });
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = ReonboardingRequest.ReonboardingRequestBuilder.class)
    public static class ReonboardingRequest {
        @JsonProperty("historical_span_in_days")
        Integer historicalSpanInDays;
        @JsonProperty("historical_sub_job_span_in_days")
        Integer historicalSubJobSpanInDays;
        @JsonProperty("historical_successive_backward_scan_count")
        Integer historicalSuccessiveBackwardScanCount;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping("/{integrationId:[0-9]+}/trigger/reonboard")
    public DeferredResult<ResponseEntity<DbTrigger>> requestReonboarding(@PathVariable("integrationId") String integrationId,
                                                                         @SessionAttribute(name = "company") String company,
                                                                         @SessionAttribute(name = "session_user") String sessionUser,
                                                                         @RequestBody ReonboardingRequest request) throws ForbiddenException {
        SelfServeEndpointUtils.validateUser(sessionUser);
        return SpringUtils.deferResponse(() -> {
            DbTrigger trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);

            if (TriggerType.GITHUB.getType().equalsIgnoreCase(trigger.getType())) {
                int historicalSpanInDays = MoreObjects.firstNonNull(request.getHistoricalSpanInDays(), 14);
                int subJobSpanInDays = MoreObjects.firstNonNull(request.getHistoricalSubJobSpanInDays(), 5);
                int successiveBackwardScanCount = MoreObjects.firstNonNull(request.getHistoricalSuccessiveBackwardScanCount(), 2);

                Validate.isTrue(historicalSpanInDays >= 1, "historical_span_in_days must be greater or equal to 1");
                Validate.isTrue(historicalSpanInDays <= 730, "historical_span_in_days must be lesser or equal to 730");
                Validate.isTrue(subJobSpanInDays >= 1, "historical_sub_job_span_in_days must be greater or equal to 1");
//                Validate.isTrue(subJobSpanInDays <= historicalSpanInDays, "historical_sub_job_span_in_days must be lesser or equal to historical_span_in_days");
                Validate.isTrue(successiveBackwardScanCount >= 1, "historical_successive_backward_scan_count must be greater or equal to 1");

                int subJobSpanInMin = (int) TimeUnit.DAYS.toMinutes(subJobSpanInDays);
                controlPlaneService.enableHistoricalTrigger(trigger.getId(), historicalSpanInDays, subJobSpanInMin, successiveBackwardScanCount);
                log.info("Triggered reonboarding for tenant={}, integrationId={}, triggerId={}, type={}: historicalSpan={}days, subJobSpan={}days, successiveBackwardScanCount={}",
                        company, integrationId, trigger.getId(), trigger.getType(), historicalSpanInDays, subJobSpanInDays, successiveBackwardScanCount);
            } else {
                controlPlaneService.updateMetadata(trigger.getId(), Map.of());
                log.info("Triggered reonboarding for tenant={}, integrationId={}, triggerId={}, type={}",
                        company, integrationId, trigger.getId(), trigger.getType());
            }

            activityLogService.insert(company, ActivityLog.builder()
                    .targetItem(integrationId)
                    .email(sessionUser)
                    .targetItemType(ActivityLog.TargetItemType.INTEGRATION)
                    .body("Requested Reonboarding")
                    .details(Map.of(
                            "trigger_id", trigger.getId(),
                            "trigger_type", trigger.getType(),
                            "params", ParsingUtils.toJsonObject(DefaultObjectMapper.get(), request)))
                    .action(ActivityLog.Action.SUCCESS)
                    .build());

            // return updated trigger
            trigger = getTrigger(company, integrationId).orElseThrow(NotFoundException::new);
            return ResponseEntity.ok(trigger);
        });
    }

    public Optional<DbTrigger> getTrigger(String company, String integrationId) throws IngestionServiceException {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        PaginatedResponse<DbTrigger> triggers = controlPlaneService.getTriggers(company, 0, integrationId);
        return IterableUtils.getFirst(triggers.getResponse().getRecords());
    }

    public IngestionStatus getStatusCached(String company, String integrationId) throws IOException, IngestionServiceException, InventoryException {
        Optional<IngestionStatus> ingestionStatus = readIngestionStatusFromCache(company, integrationId);
        if (ingestionStatus.isPresent()) {
            return ingestionStatus.get();
        }
        return getStatus(company, integrationId);
    }

    public IngestionStatus getStatus(String company, String integrationId) throws IngestionServiceException, InventoryException {
        Optional<DbTrigger> triggerOpt = getTrigger(company, integrationId);
        if (triggerOpt.isEmpty()) {
            return IngestionStatus.builder()
                    .status("unknown")
                    .build();
        }
        DbTrigger trigger = triggerOpt.get();

        Long before = null;
        Long after = Instant.now().minus(LOG_HISTORY_IN_DAYS, ChronoUnit.DAYS).getEpochSecond();
        Stream<JobDTO> jobs = PaginationUtils.stream(0, 1, RuntimeStreamException.wrap(page ->
                controlPlaneService.getJobs(company, page, 100, trigger.getId(), null, before, after, false, false).getResponse().getRecords()));

        MutableBoolean failed = new MutableBoolean(false);
        MutableBoolean warning = new MutableBoolean(false);
        MutableInt nbSuccessfulJobs = new MutableInt(0);
        MutableLong latestFrom = new MutableLong(0);
        MutableLong latestTo = new MutableLong(0);
        long totalJobs = jobs.peek((JobDTO job) -> {
            if (job.getStatus().equals(JobStatus.SUCCESS)) {
                nbSuccessfulJobs.increment();

                // extract from and to, and keep latest
                ImmutablePair<Long, Long> fromAndTo = IngestionLogDTO.extractFromAndTo(objectMapper, job);
                Long from = fromAndTo.getLeft();
                Long to = fromAndTo.getRight();
                if (to != null && to > latestTo.getValue()) {
                    latestFrom.setValue(MoreObjects.firstNonNull(from, 0));
                    latestTo.setValue(to);
                }
            }
            if (job.getStatus().equals(JobStatus.FAILURE)) {
                failed.setTrue();

                // PROP-2935 this needs to be redesigned, attempt threshold doesn't make sense anymore
                // (GitHub can expect up to 24 retries for example because of rate limits)
                /*
            } else {
                if (job.getAttemptCount() >= WARNING_ATTEMPTS_THRESHOLD) {
                    warning.setTrue();
                }
                */
            }
        }).count();

        Integration integration = inventoryService.getIntegration(company, integrationId);
        String appName = integration.getApplication();

        String status;
        if (totalJobs == 0) {

            if (appName != null && EnumUtils.isValidEnum(INGESTION_UNSUPPORTED_APPS.class, appName.toUpperCase())) {
                status = "unknown";
            } else {
                status = "warning";
            }

        } else if (failed.isTrue()) {
            status = "failed";
        } else if (warning.isTrue()) {
            status = "warning";
        } else if (nbSuccessfulJobs.intValue() == 0) {
            status = "unknown";
        } else {
            status = "healthy";
        }

        Long from = latestFrom.getValue();
        Long to = latestTo.getValue();
        return IngestionStatus.builder()
                .integrationId(integrationId)
                .status(status)
                .lastIngestedActivityFrom(from > 0 ? from : null)
                .lastIngestedActivityTo(to > 0 ? to : null)
                .build();
    }

    private String buildCacheKey(String company, String integrationId) {
        return "cache_ingestion_status_" + String.join("_", company, integrationId);
    }

    public void cacheIngestionStatus(String company, String integrationId, IngestionStatus ingestionStatus) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        Validate.notNull(ingestionStatus, "ingestionStatus cannot be null.");

        String key = buildCacheKey(company, integrationId);
        byte[] payload = objectMapper.writeValueAsBytes(ingestionStatus);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            Boolean success = redis.stringCommands().setEx(key.getBytes(UTF_8), CACHE_TTL_IN_SECONDS, payload);
            if (!Boolean.TRUE.equals(success)) {
                throw new IOException(String.format("Failed to cache ingestion status for company=%s, integrationId=%s", company, integrationId));
            }
        }
    }

    public Optional<IngestionStatus> readIngestionStatusFromCache(String company, String integrationId) throws IOException {
        Validate.notBlank(company, "company cannot be null or empty!");
        Validate.notNull(integrationId, "integrationId cannot be null!");

        String key = buildCacheKey(company, integrationId);
        try (RedisConnection redis = redisConnectionFactory.getConnection()) {
            byte[] data = redis.stringCommands().get(key.getBytes(UTF_8));
            return (data == null) ? Optional.empty() : Optional.of(objectMapper.readValue(data, IngestionStatus.class));
        }
    }

}
