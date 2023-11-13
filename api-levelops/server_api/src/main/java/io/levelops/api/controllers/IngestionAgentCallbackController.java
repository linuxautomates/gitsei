package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.models.ListResponse;
import io.levelops.ingestion.models.AgentHandle;
import io.levelops.ingestion.models.CreateJobRequest;
import io.levelops.ingestion.models.Job;
import io.levelops.services.IngestionAgentControlClient;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.ForbiddenException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/ingestion/agent-callback")
@PreAuthorize("hasAuthority('INGESTION')")
@Log4j2
@SuppressWarnings("unused")
public class IngestionAgentCallbackController {

    private final IngestionAgentControlClient agentControlClient;

    @Autowired
    public IngestionAgentCallbackController(IngestionAgentControlClient agentControlClient) {
        this.agentControlClient = agentControlClient;
    }

    private void validateAgentHandle(@Nonnull String company, @Nullable AgentHandle agentHandle) throws BadRequestException, ForbiddenException {
        BadRequestException.checkNotNull(agentHandle, "agentHandle cannot be null");
        BadRequestException.checkNotNull(agentHandle.getTenantId(), "tenant_id cannot be empty");
        ForbiddenException.check(company.equals(agentHandle.getTenantId()), "Unauthorized to access tenant: " + agentHandle.getTenantId());
    }

    private void validateTenantId(String company, String tenantId) throws ForbiddenException, BadRequestException {
        BadRequestException.checkNotNull(company, "company cannot be null");
        BadRequestException.checkNotNull(tenantId, "tenant_id cannot be null");
        ForbiddenException.check(company.equals(tenantId), "Unauthorized to access tenant: " + tenantId);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PostMapping(value = "/register", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> register(@SessionAttribute("company") String company,
                                                   @RequestBody AgentHandle agentHandle) throws BadRequestException {
        return SpringUtils.deferResponse(() -> {
            validateAgentHandle(company, agentHandle);
            agentControlClient.registerAgent(agentHandle);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping(value = "/heartbeat", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> heartbeat(@SessionAttribute("company") String company,
                                                    @RequestParam("agent_id") String agentId,
                                                    @RequestParam("tenant_id") String tenantId) throws BadRequestException {
        return heartbeat(company, AgentHandle.builder()
                .agentId(agentId)
                .tenantId(tenantId).build());
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(value = "/heartbeat", produces = "application/json")
    public DeferredResult<ResponseEntity<Void>> heartbeat(@SessionAttribute("company") String company,
                                                          @RequestBody AgentHandle agentHandle) throws BadRequestException {
        return SpringUtils.deferResponse(() -> {
            validateTenantId(company, agentHandle.getTenantId());
            agentControlClient.sendHeartbeat(agentHandle);
            return ResponseEntity.ok().build();
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(value = "/jobs/report", produces = "application/json")
    public DeferredResult<ResponseEntity<Map<String, Boolean>>> reportJobs(@SessionAttribute("company") String company,
                                                     @RequestBody ListResponse<Job> jobs,
                                                     @RequestParam("tenant_id") String tenantId) throws BadRequestException {
        return SpringUtils.deferResponse(() -> {
            validateTenantId(company, tenantId);
            Map<String, Boolean> acknowledged = agentControlClient.sendJobReport(jobs.getRecords(), tenantId);
            return ResponseEntity.ok().body(acknowledged);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping("/jobs/requests/list")
    public DeferredResult<ResponseEntity<ListResponse<CreateJobRequest>>> listJobRequests(@SessionAttribute("company") String company,
                                                                                  @RequestBody AgentHandle agentHandle) {
        return SpringUtils.deferResponse(() -> {
            validateAgentHandle(company, agentHandle);
            // only exposing jobs 'reserved' to given tenant to external agents
            List<CreateJobRequest> requests = agentControlClient.listJobRequests(agentHandle, true);
            return ResponseEntity.ok(ListResponse.of(requests));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping("/jobs/requests/accept")
    public DeferredResult<ResponseEntity<CreateJobRequest>> acceptJobRequest(@SessionAttribute("company") String company,
                                                                             @RequestParam("job_id") String jobId,
                                                                             @RequestParam("agent_id") String agentId,
                                                                             @RequestParam("tenant_id") String tenantId) throws ForbiddenException {
        return SpringUtils.deferResponse(() -> {
            validateTenantId(company, tenantId);
            return ResponseEntity.ok(agentControlClient.acceptJobRequest(jobId, agentId, tenantId));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @GetMapping("/jobs/requests/reject")
    public DeferredResult<ResponseEntity<Void>> rejectJobRequest(@SessionAttribute("company") String company,
                                                           @RequestParam("job_id") String jobId,
                                                           @RequestParam("agent_id") String agentId,
                                                           @RequestParam("status") String status,
                                                           @RequestParam("tenant_id") String tenantId) throws ForbiddenException, BadRequestException {
        return SpringUtils.deferResponse(() -> {
            validateTenantId(company, tenantId);
            agentControlClient.rejectJobRequest(jobId, agentId, status, tenantId);
            return ResponseEntity.ok().build();
        });
    }
}
