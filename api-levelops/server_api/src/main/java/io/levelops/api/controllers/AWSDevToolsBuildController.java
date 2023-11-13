package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsBuild;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsTestcase;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildsFilter;
import io.levelops.commons.databases.models.filters.AWSDevToolsTestcasesFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.AWSDevToolsBuildDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;
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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD', 'SUPER_ADMIN')")
@RequestMapping("/v1/awsdevtools_builds")
@SuppressWarnings("unused")
public class AWSDevToolsBuildController {

    private final ConfigTableHelper configTableHelper;
    private final AWSDevToolsFilterParser filterParser;
    private final AWSDevToolsBuildDatabaseService buildDatabaseService;

    public AWSDevToolsBuildController(ConfigTableHelper configTableHelper,
                                      AWSDevToolsFilterParser filterParser,
                                      AWSDevToolsBuildDatabaseService buildDatabaseService) {
        this.configTableHelper = configTableHelper;
        this.filterParser = filterParser;
        this.buildDatabaseService = buildDatabaseService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAWSDevToolsBuild>>> getListOfBuilds(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            AWSDevToolsBuildsFilter buildsFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                buildsFilter = filterParser.createBuildFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null, false);
            } else {
                buildsFilter = filterParser.createBuildFilter(filter, null, null, false);
            }

            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    buildDatabaseService.listBuilds(company, buildsFilter,
                            SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), Collections.emptyList())),
                            filter.getPage(), filter.getPageSize())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, buildDatabaseService.groupByAndCalculateBuild(company,
                        filterParser.getAWSDevToolsBuildsFilterBuilder(filter)
                                .calculation(AWSDevToolsBuildsFilter.CALCULATION.build_count)
                                .across(AWSDevToolsBuildsFilter.DISTINCT.fromString(field))
                                .build(), null).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getBuildsReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws SQLException {
        return getAggReportForBuild(company, filter, AWSDevToolsBuildsFilter.CALCULATION.build_count);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/duration_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getDurationReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) throws SQLException {
        return getAggReportForBuild(company, filter, AWSDevToolsBuildsFilter.CALCULATION.duration);
    }

    @NotNull
    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReportForBuild(
            String company, DefaultListRequest filter, AWSDevToolsBuildsFilter.CALCULATION output) throws SQLException {
        return SpringUtils.deferResponse(() -> {
            AWSDevToolsBuildsFilter.DISTINCT across = AWSDevToolsBuildsFilter.DISTINCT.fromString(filter.getAcross());
            if (across == null) {
                across = AWSDevToolsBuildsFilter.DISTINCT.project_name;
            }
            List<AWSDevToolsBuildsFilter.DISTINCT> stacks = null;
            if (CollectionUtils.isNotEmpty(filter.getStacks()))
                stacks = filter.getStacks().stream()
                        .map(AWSDevToolsBuildsFilter.DISTINCT::valueOf)
                        .collect(Collectors.toList());
            AWSDevToolsBuildsFilter buildsFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    buildsFilter = filterParser.createBuildFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), output, across, false);
                    String rowValue = row.getValue().getValues()
                            .get(configTableHelper.getColumn(configTable, filter.getAcross()).getId());
                    List<DbAggregationResult> records =
                            buildDatabaseService.buildStackedGroupBy(company, buildsFilter, stacks, rowValue).getRecords();
                    aggregationRecords.addAll(records);
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                buildsFilter = filterParser.createBuildFilter(filter, output, across, false);
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(),
                                filter.getPageSize(),
                                buildDatabaseService.buildStackedGroupBy(company, buildsFilter, stacks, null)));
            }
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/testcases/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAWSDevToolsTestcase>>> getListOfTestcases(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            AWSDevToolsTestcasesFilter testcasesFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                testcasesFilter = filterParser.createTestcaseFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null, false);
            } else {
                testcasesFilter = filterParser.createTestcaseFilter(filter, null, null, false);
            }
            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    buildDatabaseService.listTestcases(company, testcasesFilter,
                            filter.getPage(), filter.getPageSize())));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/testcases/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReportForTestcase(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, buildDatabaseService.groupByAndCalculateTestcase(company,
                        filterParser.getAWSDevToolsTestcasesFilterBuilder(filter)
                                .calculation(AWSDevToolsTestcasesFilter.CALCULATION.testcase_count)
                                .across(AWSDevToolsTestcasesFilter.DISTINCT.fromString(field))
                                .build(), null).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/testcases/report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTestcasesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReportForTestcase(company, filter, AWSDevToolsTestcasesFilter.CALCULATION.testcase_count);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/testcases/duration_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getDurationReportFotTestcase(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReportForTestcase(company, filter, AWSDevToolsTestcasesFilter.CALCULATION.duration);
    }

    @NotNull
    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReportForTestcase(
            String company, DefaultListRequest filter, AWSDevToolsTestcasesFilter.CALCULATION output) {
        return SpringUtils.deferResponse(() ->
        {
            AWSDevToolsTestcasesFilter.DISTINCT across = AWSDevToolsTestcasesFilter.DISTINCT.fromString(filter.getAcross());
            if (across == null) {
                across = AWSDevToolsTestcasesFilter.DISTINCT.report_arn;
            }
            List<AWSDevToolsTestcasesFilter.DISTINCT> stacks = null;
            if (CollectionUtils.isNotEmpty(filter.getStacks()))
                stacks = filter.getStacks().stream()
                        .map(AWSDevToolsTestcasesFilter.DISTINCT::valueOf)
                        .collect(Collectors.toList());
            AWSDevToolsTestcasesFilter testcasesFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    testcasesFilter = filterParser.createTestcaseFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), output, across, false);
                    String rowValue = row.getValue().getValues()
                            .get(configTableHelper.getColumn(configTable, filter.getAcross()).getId());
                    List<DbAggregationResult> records =
                            buildDatabaseService.testcaseStackedGroupBy(company, testcasesFilter, stacks, rowValue).getRecords();
                    aggregationRecords.addAll(records);
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                testcasesFilter = filterParser.createTestcaseFilter(filter, output, across, false);
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(),
                                filter.getPageSize(),
                                buildDatabaseService.testcaseStackedGroupBy(company, testcasesFilter, stacks, null)));
            }
        });
    }
}
