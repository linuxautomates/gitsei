package io.levelops.api.controllers.issue_mgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.databases.issue_management.DbWorkItemPrioritySLA;
import io.levelops.commons.databases.models.filters.WorkItemsPrioritySLAFilter;
import io.levelops.commons.databases.services.WorkItemsPrioritySLAService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController("issueMgmtWorkItemsPrioritySLAController")
@Log4j2
@RequestMapping("/v1/issue_mgmt/priorities")
@SuppressWarnings("unused")
public class WorkItemsPrioritySLAController {
    private final ObjectMapper mapper;
    private final WorkItemsPrioritySLAService workItemsPrioritySLAService;
    private final OrgUnitHelper ouHelper;

    public WorkItemsPrioritySLAController(ObjectMapper mapper, WorkItemsPrioritySLAService workItemsPrioritySLAService, final OrgUnitHelper ouHelper) {
        this.mapper = mapper;
        this.workItemsPrioritySLAService = workItemsPrioritySLAService;
        this.ouHelper = ouHelper;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbWorkItemPrioritySLA>>> getListOfPriorities(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/' for the request: {}", company, originalRequest, e);
            }
            WorkItemsPrioritySLAFilter workItemsPrioritySLAFilter = WorkItemsPrioritySLAFilter.fromDefaultListRequest(request);
            return ResponseEntity.ok(
                    PaginatedResponse.of(
                            request.getPage(),
                            request.getPageSize(),
                            workItemsPrioritySLAService.listPrioritiesSla(company, workItemsPrioritySLAFilter.getIntegrationIds(),
                                    workItemsPrioritySLAFilter.getProjects(), workItemsPrioritySLAFilter.getWorkitemTypes(),
                                    workItemsPrioritySLAFilter.getPriorities(), request.getPage(), request.getPageSize()
                            )));
        });
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/{id}", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> updatePriority(
            @PathVariable("id") final UUID id,
            @SessionAttribute(name = "company") String company,
            @RequestBody DbWorkItemPrioritySLA dbWorkItemPrioritySLA) {
        return SpringUtils.deferResponse(() ->
                ResponseEntity.ok("{\"ok\":\"" + workItemsPrioritySLAService.updatePrioritySla(company,
                        dbWorkItemPrioritySLA.toBuilder().id(id.toString()).build()) + "\"}"));
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/bulk", produces = "application/json")
    public DeferredResult<ResponseEntity<String>> bulkUpdatePriorities(
            @SessionAttribute(name = "company") String company,
            @RequestBody Map<String, Object> requestBody) {
        DefaultListRequest filter = mapper.convertValue(requestBody, DefaultListRequest.class);
        DbWorkItemPrioritySLA slaObj = mapper.convertValue(requestBody.get("update"), DbWorkItemPrioritySLA.class);
        return SpringUtils.deferResponse(() -> ResponseEntity.ok(mapper.writeValueAsString(
                Map.of("update_count",
                        workItemsPrioritySLAService.bulkUpdatePrioritySla(
                                company,
                                null,
                                getListOrDefault(filter.getFilter(), "integration_ids"),
                                getListOrDefault(filter.getFilter(), "projects"),
                                getListOrDefault(filter.getFilter(), "workitem_types"),
                                getListOrDefault(filter.getFilter(), "priorities"),
                                slaObj.getRespSla(),
                                slaObj.getSolveSla()
                        )))));
    }
}
