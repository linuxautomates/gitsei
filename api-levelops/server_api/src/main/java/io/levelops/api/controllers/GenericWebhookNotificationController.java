package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.generic.clients.GenericRequestsClient;
import io.levelops.commons.generic.models.GenericRequest;
import io.levelops.commons.generic.models.GenericResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

@RestController
@Log4j2
@RequestMapping("/v1/generic-requests/hooks")
@SuppressWarnings("unused")
public class GenericWebhookNotificationController {

    private final GenericRequestsClient genericRequestsClient;


    @Autowired
    public GenericWebhookNotificationController(GenericRequestsClient genericRequestsClient) {
        this.genericRequestsClient = genericRequestsClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PreAuthorize("hasAuthority('TRIGGERS')")
    @PostMapping(produces = "application/json", path = "/splunk")
    public DeferredResult<ResponseEntity<GenericResponse>> splunkNotificationHandler(
            @SessionAttribute("company") final String company,
            @RequestBody String payload,
            @PathVariable("runbookId") String runbookId) {
        log.info("createRunbookRun: {}, {}", payload, company);
        return SpringUtils.deferResponse(() -> {
            GenericResponse genericResponse = genericRequestsClient.create(company, GenericRequest.builder()
                    .requestType("SplunkSearchJobRunbookRunRequest")
                    .payload(payload)
                    .build());
            return ResponseEntity.ok(GenericResponse.builder().build());
        });
    }
}
