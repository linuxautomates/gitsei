package io.levelops.api.controllers.issue_mgmt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.hash.Hashing;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.database.azuredevops.DbIssueSprint;
import io.levelops.commons.databases.models.filters.WorkItemsMilestoneFilter;
import io.levelops.commons.databases.services.IssuesMilestoneService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import java.sql.SQLException;
import java.util.List;

@RestController("issueMgmtWorkItemsMilestonesController")
@Log4j2
@RequestMapping("/v1/issue_mgmt")
public class WorkItemsMilestonesController {

    private final IssuesMilestoneService issuesMilestoneService;
    private final AggCacheService cacheService;
    private final ObjectMapper mapper;
    private final OrgUnitHelper ouHelper;

    public WorkItemsMilestonesController(AggCacheService cacheService, ObjectMapper mapper,
                                         IssuesMilestoneService issuesMilestoneService,
                                         final OrgUnitHelper ouHelper) {
        this.cacheService = cacheService;
        this.mapper = mapper;
        this.issuesMilestoneService = issuesMilestoneService;
        this.ouHelper = ouHelper;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/sprints/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbIssueSprint>>> getListOfSprintsWithFilters(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company, @RequestBody DefaultListRequest originalRequest) {
        log.info("getListOfSprintsWithFilters: API being hit");
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            try {
                var ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.AZURE_DEVOPS, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/issue_mgmt/workitems/' for the request: {}", company, originalRequest, e);
            }
            String sortHash = Hashing.sha256().hashBytes(mapper.writeValueAsString(request.getSort()).getBytes()).toString();
            WorkItemsMilestoneFilter sprintFilter = WorkItemsMilestoneFilter.fromSprintRequest(request, "");
            sprintFilter.toBuilder().fieldTypes(List.of("sprint"));
            var page = request.getPage();
            var pageSize = request.getPageSize();
            return ResponseEntity.ok().body(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    AggCacheUtils.cacheOrCall(disableCache, company,
                            "/sprintsList/pg_" + request.getPage() + "_sz_" + request.getPageSize() + "_list_" + sortHash,
                            sprintFilter.generateCacheHash() + sprintFilter.generateCacheHash(), sprintFilter.getIntegrationIds(), mapper, cacheService,
                            () -> issuesMilestoneService.listByFilter(company, sprintFilter, page, pageSize))
            ));
        });
    }
}
