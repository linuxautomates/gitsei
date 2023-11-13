package io.levelops.api.controllers.blackduck;

import io.levelops.api.controllers.ConfigTableHelper;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckIssue;
import io.levelops.commons.databases.models.database.blackduck.DbBlackDuckProjectVersion;
import io.levelops.commons.databases.models.filters.BlackDuckIssueFilter;
import io.levelops.commons.databases.models.filters.BlackDuckProjectFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.blackduck.BlackDuckDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'SUPER_ADMIN')")
@RequestMapping("/v1/blackduck")
@SuppressWarnings("unused")
public class BlackDuckController {

    private BlackDuckDatabaseService databaseService;
    private final ConfigTableHelper configTableHelper;
    private final BlackDuckFilterParser filterParser;

    public BlackDuckController(BlackDuckDatabaseService databaseService,
                               ConfigTableHelper configTableHelper,
                               BlackDuckFilterParser filterParser) {
        this.databaseService = databaseService;
        this.configTableHelper = configTableHelper;
        this.filterParser = filterParser;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/projects/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbBlackDuckProjectVersion>>> projectsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            BlackDuckProjectFilter issueFilter = filterParser.createProjectFilter(filter, false, null, null);
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            databaseService.listProjects(company, issueFilter,
                                    filter.getPage(),
                                    filter.getPageSize())));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/issues/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbBlackDuckIssue>>> issuesList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            BlackDuckIssueFilter issueFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                issueFilter = filterParser.createFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null, false);
            } else {
                issueFilter = filterParser.createIssueFilter(filter, false, null, null);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            databaseService.listIssues(company, issueFilter,
                                    filter.getPage(),
                                    filter.getPageSize())));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new LinkedList<>();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, databaseService.groupByAndCalculateIssues(
                        company,
                        filterParser.createIssueFilter(filter,
                                false,
                                BlackDuckIssueFilter.CALCULATION.total_issues,
                                BlackDuckIssueFilter.DISTINCT.fromString(field)), null)
                        .getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/issues_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<List<DbAggregationResult>>>> getIssuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            List<List<DbAggregationResult>> response = new LinkedList<>();
            String across = filter.getAcross();
            BlackDuckIssueFilter issueFilter = filterParser.createIssueFilter(filter,
                    true,
                    BlackDuckIssueFilter.CALCULATION.total_issues,
                    BlackDuckIssueFilter.DISTINCT.fromString(across));
            List<DbAggregationResult> records = databaseService.groupByAndCalculateIssues(
                    company, issueFilter, null).getRecords();
            response.add(records);
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

}
