package io.levelops.api.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;

@RestController
@Log4j2
@RequestMapping("/v1/notifications")
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@SuppressWarnings("unused")
public class NotificationsController {
    private ObjectMapper objectMapper;

    @Autowired
    public NotificationsController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list")
    public ResponseEntity<String> getNotificationsList(@RequestBody DefaultListRequest defaultListRequest)
            throws JsonProcessingException {
        return ResponseEntity.ok().body(objectMapper.writeValueAsString(PaginatedResponse.of(
                0, 0, Collections.emptyList())));
    }
}
