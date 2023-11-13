package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.checkmarx.DbCxSastIssue;
import io.levelops.commons.databases.models.filters.CxSastIssueFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.CxSastAggService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/cxsast_issues")
@SuppressWarnings("unused")
public class CxSastIssuesController {
    private final CxSastAggService aggService;
    private final ConfigTableHelper configTableHelper;
    private final CxSastIssueFilterParser filterParser;

    @Autowired
    public CxSastIssuesController(CxSastAggService aggService, ConfigTableHelper configTableHelper,
                                  CxSastIssueFilterParser filterParser) {
        this.aggService = aggService;
        this.configTableHelper = configTableHelper;
        this.filterParser = filterParser;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbCxSastIssue>>> issueList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            CxSastIssueFilter cxSastIssueFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                cxSastIssueFilter = filterParser.createFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null, false);
            } else {
                cxSastIssueFilter = filterParser.createFilter(filter, null, null, false);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            aggService.listIssues(company,
                                    cxSastIssueFilter,
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())),
                                    filter.getPage(),
                                    filter.getPageSize())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getIssueValues
            (
                    @SessionAttribute(name = "company") String company,
                    @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : filter.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                CxSastIssueFilter cxSastIssueFilter = filterParser.createFilter(filter, null,
                        CxSastIssueFilter.DISTINCT.fromString(value), false);
                map.put(value,
                        aggService.groupByAndCalculateIssue(
                                company,
                                cxSastIssueFilter,
                                null)
                                .getRecords());
                response.add(map);
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/issues_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getIssueReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {

            CxSastIssueFilter issueFilter;

            CxSastIssueFilter.DISTINCT across = CxSastIssueFilter.DISTINCT.fromString(filter.getAcross());
            if (across == null) {
                across = CxSastIssueFilter.DISTINCT.status;
            }

            CxSastIssueFilter.CALCULATION calc = CxSastIssueFilter.CALCULATION.count;

            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                String acrossColumnId = configTableHelper.getColumn(configTable, filter.getAcross()).getId();
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    issueFilter = filterParser.createFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), calc, across, false);
                    String rowValue = row.getValue().getValues().get(acrossColumnId);
                    aggregationRecords.addAll(aggService.stackedGroupByIssue(company,
                            issueFilter,
                            getStacks(filter.getStacks()), rowValue).getRecords());
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                issueFilter = filterParser.createFilter(filter, calc, across, false);
                return ResponseEntity.ok().body(
                        PaginatedResponse.of(
                                filter.getPage(),
                                filter.getPageSize(),
                                aggService.stackedGroupByIssue(
                                        company,
                                        issueFilter,
                                        getStacks(filter.getStacks()), null).getRecords()));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/files_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getFilesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {

            CxSastIssueFilter issueFilter;
            CxSastIssueFilter.DISTINCT across = CxSastIssueFilter.DISTINCT.fromString(filter.getAcross());
            if (across == null) {
                across = CxSastIssueFilter.DISTINCT.status;
            }
            CxSastIssueFilter.CALCULATION calc = CxSastIssueFilter.CALCULATION.count;

            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                String acrossColumnId = configTableHelper.getColumn(configTable, filter.getAcross()).getId();
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    issueFilter = filterParser.createFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), calc, across, false);
                    String rowValue = row.getValue().getValues().get(acrossColumnId);
                    aggregationRecords.addAll(aggService.stackedGroupByIssue(company,
                            issueFilter,
                            getStacks(filter.getStacks()), rowValue).getRecords());
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                issueFilter = filterParser.createFilter(filter, calc, across, false);
                return ResponseEntity.ok().body(
                        PaginatedResponse.of(
                                filter.getPage(),
                                filter.getPageSize(),
                                aggService.stackedGroupByIssue(
                                        company,
                                        issueFilter,
                                        getStacks(filter.getStacks()), null).getRecords()));
            }
        });
    }

    @NotNull
    private List<CxSastIssueFilter.DISTINCT> getStacks(List<String> stacks) {
        return CollectionUtils.emptyIfNull(stacks).stream()
                .map(CxSastIssueFilter.DISTINCT::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}



