package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.services.ScmActivityService;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.services.dev_productivity.models.ScmActivities;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/scm/activity")
@Log4j2
public class ScmActivityController {
    private final ObjectMapper mapper;
    private final ScmActivityService scmActivityService;

    @Autowired
    public ScmActivityController(ObjectMapper mapper,ScmActivityService scmActivityService) {
        this.mapper = mapper;
        this.scmActivityService=scmActivityService;

    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<ScmActivities>>> getScmActivity(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @SessionAttribute(name = "session_user") String requestorEmail,
            @SessionAttribute(name = "scopes") Map<String, List<String>> scopes,
            @RequestBody DefaultListRequest request) {
        return SpringUtils.deferResponse(() -> {
            boolean valueOnly = true;
            log.info("getScmActivity company = {}, requestorEmail = {}, scopes = {}", company, requestorEmail, scopes);
            DbListResponse<ScmActivities> response = scmActivityService.getScmData(disableCache, requestorEmail, scopes, company, request, valueOnly);
            return ResponseEntity.ok().body(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            response
                    ));
        });
    }
}
