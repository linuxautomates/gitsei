package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.DocumentationService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.integrations.gcs.models.Category;
import io.levelops.integrations.gcs.models.ReportDocumentation;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.io.IOException;

@RestController
@RequestMapping("/v1/docs/reports")
@PreAuthorize("hasAnyAuthority('PUBLIC_DASHBOARD','ADMIN','SUPER_ADMIN','ORG_ADMIN_USER')")
@Log4j2
public class DocumentationController {

    private final DocumentationService documentationService;

    @Autowired
    public DocumentationController(DocumentationService documentationService) {
        this.documentationService = documentationService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/categories/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<Category>>> getCategoriesList(@RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok().body(documentationService.getCategoriesList(filter)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<DbListResponse<ReportDocumentation>>> getReportsList(@RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok().body(documentationService.getReportDocumentsList(filter)));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{report_name}", produces = "application/json")
    public DeferredResult<ResponseEntity<ReportDocumentation>> getDoc(@PathVariable("report_name") String reportName) throws IOException {
        ReportDocumentation reportDocumentation = documentationService.getReportDoc(reportName);
        return SpringUtils.deferResponse(() -> {
            if (reportDocumentation != null)
                return ResponseEntity.ok().body(reportDocumentation);
            else {
                log.warn("ReportDoc not found for reportName {}", reportName);
                return ResponseEntity.notFound().build();
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/images/{image_name}", produces = "image/png")
    public DeferredResult<ResponseEntity<byte[]>> getImage(@PathVariable("image_name") String imageName) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok().body(documentationService.getImage(imageName)));
    }
}

