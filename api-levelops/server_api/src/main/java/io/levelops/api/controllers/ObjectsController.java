package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.automation_rules.ObjectType;
import io.levelops.commons.databases.models.database.automation_rules.ObjectTypeDTO;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.objects.ObjectsClient;
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
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;

@RestController
@RequestMapping("/v1/objects")
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@Log4j2
@SuppressWarnings("unused")
public class ObjectsController {
    private ObjectsClient objectsClient;

    @Autowired
    public ObjectsController(ObjectsClient objectsClient) {
        this.objectsClient = objectsClient;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<String>>> objectsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        objectsClient.listObjectTypes(company, filter))));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{type}/fields", produces = "application/json")
    public DeferredResult<ResponseEntity<List<String>>> getFieldsForObjectType(@PathVariable("type") String type,
                                                                     @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(objectsClient.getFieldsForObjectType(company, ObjectType.fromString(type)))
        );
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list_details", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ObjectTypeDTO>>> objectDetailsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                        objectsClient.listObjectTypeDetails(company, filter))));
    }
}
