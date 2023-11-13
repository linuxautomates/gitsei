package io.levelops.api.controllers;


import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.ContentSchema;
import io.levelops.commons.databases.services.ContentSchemaDatabaseService;
import io.levelops.commons.models.ContentType;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.stream.Collectors;


@RestController
@RequestMapping("/v1/content/schema")
public class ContentSchemaController {

    private final ContentSchemaDatabaseService contentSchemaDatabaseService;

    public ContentSchemaController(ContentSchemaDatabaseService contentSchemaDatabaseService) {
        this.contentSchemaDatabaseService = contentSchemaDatabaseService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @PostMapping(path = "/list")
    @PreAuthorize("hasAnyAuthority('ADMIN','AUDITOR','LIMITED_USER','SUPER_ADMIN','ORG_ADMIN_USER')")
    public DeferredResult<ResponseEntity<PaginatedResponse<ContentSchema>>> list(@SessionAttribute("company") String company,
                                                                                 @RequestBody DefaultListRequest listRequest) {
        return SpringUtils.deferResponse(() -> {
            @SuppressWarnings("unchecked")
            List<ContentType> contentTypes = (List<ContentType>) listRequest.getFilterValue("content_types", List.class)
                    .map(list -> list.stream().map(o -> ContentType.fromString((String) o)).collect(Collectors.toList()))
                    .orElse(null);
            var response = contentSchemaDatabaseService.filter(company, listRequest.getPage(), listRequest.getPageSize(), contentTypes);
            return ResponseEntity.ok(PaginatedResponse.of(listRequest.getPage(), listRequest.getPageSize(), response));
        });
    }
}
