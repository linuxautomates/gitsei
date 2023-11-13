package io.levelops.api.controllers;

import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.api.converters.DefaultListRequestUtils;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
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
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN', 'ORG_ADMIN_USER')")
@RequestMapping("/v1/jira_fields")
public class JiraFieldsController {
    private final JiraFieldService jiraFieldService;
    private final OrgUnitHelper orgUnitHelper;

    @Autowired
    public JiraFieldsController(JiraFieldService jiraFieldService, final OrgUnitHelper orgUnitHelper) {
        this.jiraFieldService = jiraFieldService;
        this.orgUnitHelper = orgUnitHelper;
    }

    @SuppressWarnings("unchecked")
    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_INSIGHTS, permission = Permission.INSIGHTS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbJiraField>>> fieldsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {// OU stuff
            var request = originalRequest;
            try {
                var ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.JIRA, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/jira_fields/list' for the request: {}", company, originalRequest, e);
            }
            
            String integrationId = (String) request.getFilter().get("integration_id");
            List<String> integrationIds;
            if (Objects.nonNull(integrationId)) {
                integrationIds = List.of(integrationId);
            } else {
                integrationIds = DefaultListRequestUtils.getListOrDefault(request.getFilter(), "integration_ids");
            }
            String partialName = null;
            String exactName = null;
            if (request.getFilter() != null) {
                if (request.getFilter()
                        .getOrDefault("partial", Collections.emptyMap()) != null)
                    partialName = (String) ((Map<String, Object>) request.getFilter()
                            .getOrDefault("partial", Collections.emptyMap()))
                            .getOrDefault("name", null);
                else
                    exactName = (String) request.getFilter().get("name");
            }
            DbListResponse<DbJiraField> fields = jiraFieldService.listByFilter(
                    company, integrationIds, true, exactName, partialName,
                    null, request.getPage(), request.getPageSize());
            return ResponseEntity.ok(PaginatedResponse.of(request.getPage(), request.getPageSize(), fields));
        });
    }
}
