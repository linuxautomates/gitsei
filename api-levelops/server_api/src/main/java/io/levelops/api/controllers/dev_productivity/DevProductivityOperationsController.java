package io.levelops.api.controllers.dev_productivity;

import io.levelops.api.services.dev_productivity.DevProductivityOpsService;
import io.levelops.commons.databases.models.database.User;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/dev_productivity/ops")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class DevProductivityOperationsController {
    private final DevProductivityOpsService devProductivityOpsService;

    @Autowired
    public DevProductivityOperationsController(DevProductivityOpsService devProductivityOpsService) {
        this.devProductivityOpsService = devProductivityOpsService;
    }

    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @RequestMapping(method = RequestMethod.POST, value = "/assign-scopes", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<User>>> assignScopesForUser(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("assignScopesForUser company = {}, requestorEmail = {}, scopes = {}, request = {}", company, requestorEmail, scopes, request);
            List<User> updatedUsers = devProductivityOpsService.assignDevProdScope(company, requestorEmail, scopes, request);
            return ResponseEntity.ok().body(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    updatedUsers
            ));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/reschedule-report-current-tenant", produces = "application/json")
    public DeferredResult<ResponseEntity<Boolean>> reScheduleReportSingleTenant(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("reScheduleReportSingleTenant company = {}, requestorEmail = {}, scopes = {}, request = {}", company, requestorEmail, scopes, request);
            Boolean success = devProductivityOpsService.reScheduleReportSingleTenant(company, requestorEmail, scopes, request);
            return ResponseEntity.ok().body(success);
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/reschedule-report", produces = "application/json")
    public DeferredResult<ResponseEntity<List<String>>> reScheduleReportAllTenants(
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            log.info("reScheduleReportTenants company = {}, requestorEmail = {}, scopes = {}, request = {}", company, requestorEmail, scopes, request);
            List<String> tenantIds = devProductivityOpsService.reScheduleReportAllTenants(company, requestorEmail, scopes, request);
            return ResponseEntity.ok().body(tenantIds);
        });
    }

}
