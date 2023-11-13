package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/jiraprojects")
@SuppressWarnings("unused")
public class JiraProjectsController {
    private ObjectMapper objectMapper;
    private JiraProjectService jiraProjectService;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public JiraProjectsController(ObjectMapper objectMapper, JiraProjectService jiraProjectService, final OrgUnitHelper orgUnitHelper) {
        this.objectMapper = objectMapper;
        this.jiraProjectService = jiraProjectService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.GET, value = "/{projectid}", produces = "application/json")
    public DeferredResult<ResponseEntity<DbJiraProject>> getProject(@PathVariable("projectid") String projectId,
                                                                    @SessionAttribute(name = "company") String company) {
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(jiraProjectService.get(company, projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Project id not found."))));
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbJiraProject>>> projectsList(@SessionAttribute(name = "company") String company,
                                                                                         @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jiraprojects/list' for the request: {}", company, originalRequest, e);
            }

            String integrationId = (String) request.getFilter().get("integration_id");
            List<String> ids = (List<String>) request.getFilter().get("ids");
            DbListResponse<DbJiraProject> projects = jiraProjectService.listByFilter(
                    company, ids, integrationId, request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), projects));
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> projectsValues(@SessionAttribute(name = "company") String company,
                                                                                                                    @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {// OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jiraprojects/values' for the request: {}", company, originalRequest, e);
            }

            if (CollectionUtils.isEmpty(request.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<String> integrationIds = (List) request.getFilter().get("integration_ids");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : request.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                map.put(value,
                        jiraProjectService.groupByAndCalculateProject(
                                company, integrationIds, value).getRecords());
                response.add(map);
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

}