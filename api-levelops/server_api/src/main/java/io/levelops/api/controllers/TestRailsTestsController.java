package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsCaseField;
import io.levelops.commons.databases.models.filters.DefaultListRequestUtils;
import io.levelops.commons.databases.models.filters.TestRailsCaseFieldFilter;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.caching.CacheHashUtils;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.testrails.DbTestRailsTest;
import io.levelops.commons.databases.models.filters.TestRailsTestsFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.TestRailsCaseFieldDatabaseService;
import io.levelops.commons.databases.services.TestRailsTestPlanDatabaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.commons.utils.ListUtils;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/testrails_tests")
@SuppressWarnings("unused")
public class TestRailsTestsController {

    private final IntegrationService integService;
    private final IntegrationTrackingService integrationTrackingService;
    private final TestRailsTestPlanDatabaseService planDatabaseService;
    private final TestRailsCaseFieldDatabaseService caseFieldDatabaseService;
    private final ConfigTableHelper configTableHelper;
    private final OrgUnitHelper ouHelper;
    private final AggCacheService cacheService;
    private final ObjectMapper mapper;

    public TestRailsTestsController(IntegrationService integService,
                                    AggCacheService cacheService,
                                    ObjectMapper objectMapper,
                                    IntegrationTrackingService integrationTrackingService,
                                    TestRailsTestPlanDatabaseService planDatabaseService,
                                    TestRailsCaseFieldDatabaseService caseFieldDatabaseService,
                                    ConfigTableHelper configTableHelper, OrgUnitHelper ouHelper) {
        this.integService = integService;
        this.cacheService = cacheService;
        this.mapper = objectMapper;
        this.integrationTrackingService = integrationTrackingService;
        this.planDatabaseService = planDatabaseService;
        this.caseFieldDatabaseService = caseFieldDatabaseService;
        this.configTableHelper = configTableHelper;
        this.ouHelper = ouHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbTestRailsTest>>> getListOfTests(
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var filter = originalRequest;
            OUConfiguration ouConfig;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.TESTRAILS, originalRequest);
                filter = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in /testrails_tests/list for the request: {}", company, originalRequest, e);
            }
            TestRailsTestsFilter testsFilter = getTestRailsFilterBuilder(company, filter)
                    .CALCULATION(TestRailsTestsFilter.CALCULATION.fromDefaultListRequest(filter))
                    .build();
            TestRailsTestsFilter testRailsTestsFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilterValue("config_table_row_id", String.class).orElseThrow();
                testRailsTestsFilter = configTable.getRows().entrySet().stream()
                        .filter(row -> configTableRowId.equals(row.getValue().getId()))
                        .findFirst()
                        .map(row -> updateFilter(testsFilter, configTable.getSchema().getColumns(),
                                row.getValue()))
                        .orElse(testsFilter);
            } else {
                testRailsTestsFilter = testsFilter;
            }
            var page = filter.getPage();
            var pageSize = filter.getPageSize();
            var sort = filter.getSort();
            return ResponseEntity.ok(PaginatedResponse.of(page, pageSize,
                    AggCacheUtils.cacheOrCall(
                            disableCache,
                            company, "testrails_tests/list/pg_" + page + "/sz_" + pageSize,
                            CacheHashUtils.generateCacheHash(forceSource, filter, testRailsTestsFilter),
                            testRailsTestsFilter.getIntegrationIds(),
                            mapper, cacheService,
                            () -> planDatabaseService.list(company, testRailsTestsFilter,
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(sort, Collections.emptyList())),
                                    page, pageSize)
                    )));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var filter = originalRequest;
            OUConfiguration ouConfig;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.TESTRAILS, originalRequest);
                filter = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in /tesrails_test/values for the request: {}", company, originalRequest, e);
            }
            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, planDatabaseService.groupByAndCalculateForValues(company,
                        getTestRailsFilterBuilder(company, filter)
                                .CALCULATION(TestRailsTestsFilter.CALCULATION.fromDefaultListRequest(filter))
                                .DISTINCT(TestRailsTestsFilter.DISTINCT.fromString(field))
                                .build(), List.of()).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/custom_case_fields/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCustomCaseFieldsValuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var filter = originalRequest;
            OUConfiguration ouConfig;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.TESTRAILS, originalRequest);
                filter = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in /tesrails_tests/custom_case_fields/values for the request: {}", company, originalRequest, e);
            }
            List<String> fields = filter.getFields();
            if (CollectionUtils.isEmpty(fields))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
            List<DbTestRailsCaseField> caseFields = caseFieldDatabaseService.listByFilter(company, TestRailsCaseFieldFilter.builder().integrationIds(integrationIds).needAssignedFieldsOnly(true).systemNames(fields).build(), 0, fields.size()).getRecords();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, planDatabaseService.groupByAndCalculateForValues(company,
                        getTestRailsFilterBuilder(company, filter)
                                .CALCULATION(TestRailsTestsFilter.CALCULATION.fromDefaultListRequest(filter))
                                .DISTINCT(TestRailsTestsFilter.DISTINCT.custom_case_field)
                                .customAcross(field)
                                .build(), caseFields).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/tests_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTestsReport(
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, disableCache, forceSource, filter, TestRailsTestsFilter.CALCULATION.fromDefaultListRequest(filter));
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/estimate_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getEstimateReport(
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, disableCache, forceSource, filter, TestRailsTestsFilter.CALCULATION.estimate);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/estimate_forecast_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getEstimateForecastReport(
            @SessionAttribute(name = "company") String company,
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @RequestParam(name = "force_source", required = false) String forceSource,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, disableCache, forceSource, filter, TestRailsTestsFilter.CALCULATION.estimate_forecast);
    }

    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getCustomCaseFieldsList(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var filter = originalRequest;
            OUConfiguration ouConfig;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.TESTRAILS, originalRequest);
                filter = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in /tesrails_tests/custom_case_fields/list for the request: {}", company, originalRequest, e);
            }
            List<String> fields = filter.getFields();
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            if (CollectionUtils.isEmpty(fields))
                return ResponseEntity.ok(PaginatedResponse.of(0, 1000, response));

            List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
            List<DbTestRailsCaseField> caseFields = caseFieldDatabaseService.listByFilter(company, TestRailsCaseFieldFilter.builder().integrationIds(integrationIds).needAssignedFieldsOnly(true).systemNames(fields).build(), 0, fields.size()).getRecords();
            for (String field : filter.getFields()) {
                response.add(Map.of(field, planDatabaseService.groupByAndCalculateForValues(company,
                        getTestRailsFilterBuilder(company, filter)
                                .CALCULATION(TestRailsTestsFilter.CALCULATION.test_count)
                                .DISTINCT(TestRailsTestsFilter.DISTINCT.custom_case_field)
                                .customAcross(field)
                                .build(), caseFields).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, 1000, response));
        });
    }

    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReport(
            String company, Boolean disableCache, String forceSource,
            DefaultListRequest originalRequest, TestRailsTestsFilter.CALCULATION output) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var filter = originalRequest;
            OUConfiguration ouConfig;
            try {
                ouConfig = ouHelper.getOuConfigurationFromRequest(company, IntegrationType.TESTRAILS, originalRequest);
                filter = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in getAggReport for the request: {}", company, originalRequest, e);
            }
            TestRailsTestsFilter testsFilter = getTestRailsFilterBuilder(company, filter)
                    .CALCULATION(output)
                    .DISTINCT(MoreObjects.firstNonNull(TestRailsTestsFilter.DISTINCT.fromString(filter.getAcross()),
                            TestRailsTestsFilter.DISTINCT.assignee))
                    .build();
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                String acrossColumnId = configTableHelper.getColumn(configTable, filter.getAcross()).getId();
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    TestRailsTestsFilter updatedTestsFilter = updateFilter(testsFilter,
                            configTable.getSchema().getColumns(), row.getValue());
                    String rowValue = row.getValue().getValues().get(acrossColumnId);
                    DefaultListRequest finalFilter = filter;
                    List<DbAggregationResult> aggRecords = AggCacheUtils.cacheOrCall(
                            disableCache,
                            company, "testrails_tests/config_table/pg_" + filter.getPage() +"/sz_" + filter.getPageSize(),
                            CacheHashUtils.generateCacheHash(forceSource, finalFilter, output, configTable, testsFilter, rowValue),
                            updatedTestsFilter.getIntegrationIds(),
                            mapper, cacheService,
                            () -> planDatabaseService.groupByAndCalculate(company, updatedTestsFilter,
                                    getStacks(finalFilter.getStacks()), rowValue)
                    ).getRecords();
                    aggregationRecords.addAll(aggRecords);
                }
                return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                DefaultListRequest finalFilter = filter;
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(),
                                AggCacheUtils.cacheOrCall(
                                        disableCache,
                                        company, "testrails_tests/pg_" + filter.getPage() +"/sz_" + filter.getPageSize() ,
                                        CacheHashUtils.generateCacheHash(forceSource, finalFilter, output, testsFilter),
                                        testsFilter.getIntegrationIds(),
                                        mapper, cacheService,
                                        () -> planDatabaseService.groupByAndCalculate(company, testsFilter,
                                                getStacks(finalFilter.getStacks()), null)
                                )));
            }
        });
    }

    private List<TestRailsTestsFilter.DISTINCT> getStacks(List<String> stacks) {
        return CollectionUtils.emptyIfNull(stacks).stream()
                .map(TestRailsTestsFilter.DISTINCT::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private TestRailsTestsFilter updateFilter(TestRailsTestsFilter filter,
                                              Map<String, ConfigTable.Column> columns,
                                              ConfigTable.Row row) {
        TestRailsTestsFilter.TestRailsTestsFilterBuilder filterBuilder = filter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "tr_projects":
                    filterBuilder.projects(getIntersection(getRowValue(row, column.getValue()), filter.getProjects()));
                    break;
                case "tr_milestones":
                    filterBuilder.milestones(getIntersection(getRowValue(row, column.getValue()), filter.getMilestones()));
                    break;
                case "tr_test_plans":
                    filterBuilder.testPlans(getIntersection(getRowValue(row, column.getValue()), filter.getTestPlans()));
                    break;
                case "tr_test_runs":
                    filterBuilder.testRuns(getIntersection(getRowValue(row, column.getValue()), filter.getTestRuns()));
                    break;
                case "tr_assignees":
                    filterBuilder.assignees(getIntersection(getRowValue(row, column.getValue()), filter.getAssignees()));
                    break;
                case "tr_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), filter.getStatuses()));
                    break;
                case "tr_priorities":
                    filterBuilder.priorities(getIntersection(getRowValue(row, column.getValue()), filter.getPriorities()));
                    break;
                case "tr_types":
                    filterBuilder.testTypes(getIntersection(getRowValue(row, column.getValue()), filter.getTestTypes()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    private List<String> getRowValue(ConfigTable.Row row, ConfigTable.Column column) {
        String rowValue = row.getValues().get(column.getId());
        if (column.getMultiValue()) {
            String sanitizedRowValue = rowValue.replaceAll("^\\[|]$", "").replaceAll("\"", "");
            return StringUtils.isEmpty(sanitizedRowValue) ? List.of() : Arrays.asList(sanitizedRowValue.split(","));
        }
        return List.of(rowValue);
    }

    private List<String> getIntersection(List<String> rowValues, List<String> filterValues) {
        if (CollectionUtils.isEmpty(rowValues)) {
            return filterValues;
        } else if (CollectionUtils.isEmpty(filterValues)) {
            return rowValues;
        } else {
            return new ArrayList<>(CollectionUtils.intersection(rowValues, filterValues));
        }
    }

    private TestRailsTestsFilter.TestRailsTestsFilterBuilder getTestRailsFilterBuilder(
            String company, DefaultListRequest filter) throws SQLException, BadRequestException {
        var createdOnTimeRange = DefaultListRequestUtils.getTimeRange(filter, "created_on");
        if (createdOnTimeRange.getLeft() == null && createdOnTimeRange.getRight() == null){
            createdOnTimeRange = DefaultListRequestUtils.getTimeRange(filter, "created_at");
        }
        return TestRailsTestsFilter.builder()
                .acrossLimit(filter.getAcrossLimit())
                .customAcross(filter.getAcross() != null && filter.getAcross().startsWith("custom_") ? filter.getAcross() : null)
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .createdOnTimeRange(createdOnTimeRange)
                .assignees(getListOrDefault(filter.getFilter(), "assignees"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .priorities(getListOrDefault(filter.getFilter(), "priorities"))
                .testRuns(getListOrDefault(filter.getFilter(), "test_runs"))
                .testPlans(getListOrDefault(filter.getFilter(), "test_plans"))
                .testTypes(getListOrDefault(filter.getFilter(), "types"))
                .milestones(getListOrDefault(filter.getFilter(), "milestones"))
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .customStacks(ListUtils.emptyIfNull(filter.getStacks()).stream().filter(stack -> stack.startsWith("custom_")).collect(Collectors.toList()))
                .customCaseFields((Map<String, Object>) MapUtils.emptyIfNull(filter.getFilter()).get("custom_fields"))
                .excludeCustomCaseFields(
                        (Map<String, Object>)
                                (MapUtils.emptyIfNull((Map<String, Object>)MapUtils.emptyIfNull(filter.getFilter()).get("exclude"))
                                        .get("custom_fields")));
    }
}
