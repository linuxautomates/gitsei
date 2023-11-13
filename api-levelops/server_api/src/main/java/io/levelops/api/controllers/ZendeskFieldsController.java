package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.zendesk.DbZendeskField;
import io.levelops.commons.databases.services.ZendeskFieldService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/zendesk_fields")
@SuppressWarnings("unused")
public class ZendeskFieldsController {

    private final ZendeskFieldService zendeskFieldService;

    public ZendeskFieldsController(ZendeskFieldService zendeskFieldService) {
        this.zendeskFieldService = zendeskFieldService;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbZendeskField>>> fieldsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
                    List<String> integrationIds = (List<String>) filter.getFilter().get("integration_ids");
                    String partialTitle = null;
                    String exactTitle = null;
                    if (!filter.getFilter().isEmpty()) {
                        if (filter.getFilter().get("partial") != null)
                            partialTitle = (String) ((Map<String, Object>) filter.getFilter()
                                    .getOrDefault("partial", Collections.emptyMap()))
                                    .getOrDefault("title", null);
                        else
                            exactTitle = (String) filter.getFilter().get("title");
                    }
                    DbListResponse<DbZendeskField> fields = zendeskFieldService.listByFilter(
                            company, integrationIds, partialTitle, exactTitle,
                            null, null, filter.getPage(), filter.getPageSize());
                    return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), fields));
                }
        );
    }
}

