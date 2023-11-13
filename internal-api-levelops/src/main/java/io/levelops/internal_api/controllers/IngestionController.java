package io.levelops.internal_api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.functional.IterableUtils;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.controlplane.models.DbTrigger;
import io.levelops.ingestion.exceptions.IngestionServiceException;
import io.levelops.ingestion.models.controlplane.IngestionLogDTO;
import io.levelops.ingestion.models.controlplane.JobDTO;
import io.levelops.ingestion.models.controlplane.JobStatus;
import io.levelops.ingestion.services.ControlPlaneService;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@Log4j2
@RequestMapping("/internal/v1/tenants/{company}/ingestion")
public class IngestionController {

    private static final int LOG_HISTORY_IN_DAYS = 7;

    private final ObjectMapper objectMapper;
    private final ControlPlaneService controlPlaneService;

    @Autowired
    public IngestionController(ObjectMapper objectMapper,
                               ControlPlaneService controlPlaneService,
                               RedisConnectionFactory redisConnectionFactory,
                               InventoryService inventoryService) {
        this.objectMapper = objectMapper;
        this.controlPlaneService = controlPlaneService;
    }


    @PostMapping("/{integrationId}/logs")
    public DeferredResult<ResponseEntity<PaginatedResponse<IngestionLogDTO>>> getIngestionLogs(@PathVariable("company") String company,
                                                                                               @PathVariable("integrationId") String integrationId,
                                                                                               @RequestParam(name = "include_result_field", required = false, defaultValue = "false") Boolean includeResultField,
                                                                                               @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(doGetIngestionLogs(company, request.getPage(), request.getPageSize(),
                integrationId,
                request.<String>getFilterValueAsList("statuses").orElse(null),
                BooleanUtils.isTrue(includeResultField))));
    }

    public PaginatedResponse<IngestionLogDTO> doGetIngestionLogs(String company, Integer page, Integer pageSize, String integrationId, @Nullable List<String> statuses, boolean includeResultField) throws IngestionServiceException {
        Optional<DbTrigger> triggerOpt = getTrigger(company, integrationId);
        if (triggerOpt.isEmpty()) {
            return PaginatedResponse.of(page, pageSize, 0, List.of());
        }
        DbTrigger trigger = triggerOpt.get();

        List<String> statusesFilter = null;
        if (CollectionUtils.isNotEmpty(statuses)) {
            statusesFilter = statuses.stream()
                    .map(IngestionLogDTO::convertDTOStatusToJobStatuses)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .map(JobStatus::toString)
                    .collect(Collectors.toList());
        }

        Long before = null;
        Long after = Instant.now().minus(LOG_HISTORY_IN_DAYS, ChronoUnit.DAYS).getEpochSecond();
        PaginatedResponse<JobDTO> jobs = controlPlaneService.getJobs(company, ObjectUtils.defaultIfNull(page, 0), pageSize, trigger.getId(), statusesFilter, before, after, true, includeResultField);
        List<IngestionLogDTO> jobDTOs = jobs.getResponse().getRecords().stream()
                .map(jobDTO -> IngestionLogDTO.fromJobDTO(objectMapper, jobDTO))
                .collect(Collectors.toList());
        return PaginatedResponse.of(page, pageSize, jobs.getMetadata().getTotalCount(), jobDTOs);
    }

    public Optional<DbTrigger> getTrigger(String company, String integrationId) throws IngestionServiceException {
        Validate.notBlank(integrationId, "integrationId cannot be null or empty.");
        PaginatedResponse<DbTrigger> triggers = controlPlaneService.getTriggers(company, 0, integrationId);
        return IterableUtils.getFirst(triggers.getResponse().getRecords());
    }
}
