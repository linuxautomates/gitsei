package io.levelops.api.controllers.snyk;

import io.levelops.api.controllers.ConfigTableHelper;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.snyk.DbSnykIssue;
import io.levelops.commons.databases.models.filters.SnykIssuesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.snyk.SnykDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.integrations.snyk.models.SnykVulnerability;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/snyk_issues")
@SuppressWarnings("unused")
public class SnykIssuesController {

    private final SnykDatabaseService snykDatabaseService;
    private final ConfigTableHelper configTableHelper;
    private final SnykFilterParser snykFilterParser;

    public SnykIssuesController(SnykDatabaseService snykDatabaseService, ConfigTableHelper configTableHelper,
                                SnykFilterParser snykFilterParser) {
        this.snykDatabaseService = snykDatabaseService;
        this.configTableHelper = configTableHelper;
        this.snykFilterParser = snykFilterParser;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbSnykIssue>>> getListIssues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            SnykIssuesFilter issueFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                issueFilter = snykFilterParser.createFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null,  false);
            } else {
                issueFilter = snykFilterParser.createFilter(filter, false,null, null);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            snykDatabaseService.listIssues(company, issueFilter,
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
            for (String field: filter.getFields()) {
                response.add(Map.of(field, snykDatabaseService.groupByAndCalculateIssues(
                        company,
                        snykFilterParser.createFilter(filter,
                                false,
                                SnykIssuesFilter.Calculation.total_issues,
                                SnykIssuesFilter.Distinct.fromString(field)), null)
                        .getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/issues_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getIssuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, filter, "total_issues");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/score_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getScoreReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, filter, "scores");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/patches_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getPatchesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, filter, "total_patches");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/trends_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTrendsReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, filter, "total_issues");
    }

    @RequestMapping(method = RequestMethod.POST, value = "/patches/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<SnykVulnerability.Patch>>> getListPatches(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            SnykIssuesFilter issueFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                issueFilter = snykFilterParser.createFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null,  false);
            } else {
                issueFilter = snykFilterParser.createFilter(filter, false,null, null);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            snykDatabaseService.listPatches(company, issueFilter,
                                    filter.getPage(),
                                    filter.getPageSize())));
        });
    }

    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReport(String company, DefaultListRequest filter,
                                                                                                String calculation) {

        return SpringUtils.deferResponse(() -> {
            SnykIssuesFilter.Calculation calc = SnykIssuesFilter.Calculation.fromString(calculation);
            SnykIssuesFilter.Distinct across = SnykIssuesFilter.Distinct.fromString(filter.getAcross());
            SnykIssuesFilter issueFilter;

            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    issueFilter = snykFilterParser.createFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), calc, across, false);
                    String rowValue = row.getValue().getValues()
                            .get(configTableHelper.getColumn(configTable, filter.getAcross()).getId());
                    List<DbAggregationResult> records =
                            snykDatabaseService.stackedGroupByIssues(company, issueFilter, getStacks(filter.getStacks()), rowValue).getRecords();
                    aggregationRecords.addAll(records);
                }
                return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            }
            else {
                issueFilter = snykFilterParser.createFilter(filter, false, calc, across);
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(),
                                filter.getPageSize(),
                                snykDatabaseService.stackedGroupByIssues(company, issueFilter, getStacks(filter.getStacks()), null)));
            }
        });
    }

    private List<SnykIssuesFilter.Distinct> getStacks(List<String> stacks) {
        return CollectionUtils.emptyIfNull(stacks).stream()
                .map(SnykIssuesFilter.Distinct::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
