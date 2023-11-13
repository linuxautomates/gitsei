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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Log4j2
@RequestMapping("/v1/generic-requests")
@SuppressWarnings("unused")
public class GenericRequestController {
    private final GenericRequestsClient genericRequestsClient;

    @Autowired
    public GenericRequestController(GenericRequestsClient genericRequestsClient){
        this.genericRequestsClient = genericRequestsClient;
    }

    @PostMapping(produces = "application/json")
    public DeferredResult<ResponseEntity<GenericResponse>> createGenericRequest(@SessionAttribute("company") final String company,
                                                                                @RequestBody GenericRequest request) {
        return SpringUtils.deferResponse(() -> {
            GenericResponse genericResponse = genericRequestsClient.create(company, request);
            return ResponseEntity.accepted().body(genericResponse);
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_CREATE)
    @PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
    @PostMapping(path = "/multipart", produces = "application/json")
    public DeferredResult<ResponseEntity<GenericResponse>> createGenericRequestMultiPart(@SessionAttribute(name = "company") String company,
                                                                              @RequestPart("json") MultipartFile jsonFile,
                                                                              @RequestPart(name = "file", required = false) MultipartFile zipFile) {
        return SpringUtils.deferResponse(() -> {
            GenericResponse genericResponse = genericRequestsClient.createMultipart(company, jsonFile, zipFile);
            return ResponseEntity.accepted().body(genericResponse);
        });
    }
}
