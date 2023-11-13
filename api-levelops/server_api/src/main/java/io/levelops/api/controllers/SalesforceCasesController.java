package io.levelops.api.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import com.google.common.hash.Hashing;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.aggregations_cache.services.AggCacheUtils;
import io.levelops.commons.aggregations_cache.services.AggCacheService;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.salesforce.DbSalesforceCase;
import io.levelops.commons.databases.models.filters.SalesforceCaseFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.ConfigTableDatabaseService;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.SalesforceCaseService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/salesforce_cases")
public class SalesforceCasesController {
    private final IntegrationService integService;
    private final SalesforceCaseService salesforceCaseService;
    private final ConfigTableDatabaseService configTableDatabaseService;
    private final IntegrationTrackingService integrationTrackingService;
    private final AggCacheService aggCacheService;
    private final ObjectMapper objectMapper;
    private final static Pattern UUID_REGEX_PATTERN =
            Pattern.compile("^[{]?[0-9a-fA-F]{8}-([0-9a-fA-F]{4}-){3}[0-9a-fA-F]{12}[}]?$");


    @Autowired
    public SalesforceCasesController(SalesforceCaseService salesforceCaseService,
                                     ConfigTableDatabaseService configTableDatabaseService,
                                     IntegrationTrackingService trackingService,
                                     IntegrationService integrationService,
                                     AggCacheService aggCacheService,
                                     ObjectMapper objectMapper) {
        this.integrationTrackingService = trackingService;
        this.salesforceCaseService = salesforceCaseService;
        this.configTableDatabaseService = configTableDatabaseService;
        this.integService = integrationService;
        this.aggCacheService = aggCacheService;
        this.objectMapper = objectMapper;
    }

    private Long getIngestedAt(String company, DefaultListRequest filter) throws SQLException {
        Integration integ = integService.listByFilter(company, null,
                List.of(IntegrationType.SALESFORCE.toString()),
                null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(NumberUtils::toInt).collect(Collectors.toList()), List.of(),
                0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integ != null)
            ingestedAt = integrationTrackingService.get(company, integ.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbSalesforceCase>>> getListOfCases(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            SalesforceCaseFilter sfCaseFilter = parseSFCaseFilter(company, filter, null, null);
            if (StringUtils.isNotEmpty((String) filter.getFilter().get("config_table_id"))) {
                String configTableId = filter.getFilter().get("config_table_id").toString();
                if(!UUID_REGEX_PATTERN.matcher(configTableId).matches())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid config table key parameter. Required UUID. Provided: " + configTableId);
                ConfigTable configTable = configTableDatabaseService.get(company, configTableId).orElse(null);
                checkRequired(configTable, "Config table not found for given config_table_id");
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                checkRequired(configTableRowId, "Config table not found for given config_table_id");
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    if (configTableRowId.equals(row.getValue().getId())) {
                        sfCaseFilter = updateSFCaseFilter(sfCaseFilter,
                                configTable.getSchema().getColumns(), row.getValue());
                        break;
                    }
                }
            }
            final SalesforceCaseFilter finalFilter = sfCaseFilter;
            String sortHash = Hashing.sha256().hashBytes(objectMapper.writeValueAsString(filter.getSort()).getBytes()).toString();

            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            AggCacheUtils.cacheOrCall(disableCache, company,
                                    "/salesforce_cases/list_" + filter.getPage() + "_" + filter.getPageSize() + "_" + sortHash,
                                    sfCaseFilter.generateCacheHash(),
                                    sfCaseFilter.getIntegrationIds(), objectMapper, aggCacheService,
                                    () -> salesforceCaseService.list(company,
                                            finalFilter,
                                            SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), List.of())),
                                            filter.getPage(),
                                            filter.getPageSize()))));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValues(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {

            if (CollectionUtils.isEmpty(filter.getFields()))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing or empty list of 'fields' provided.");
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String value : filter.getFields()) {
                Map<String, List<DbAggregationResult>> map = new HashMap<>();
                SalesforceCaseFilter sfCaseFilter = SalesforceCaseFilter.builder()
                        .across(SalesforceCaseFilter.DISTINCT.fromString(value))
                        .ingestedAt(getIngestedAt(company, filter))
                        .extraCriteria(MoreObjects.firstNonNull(
                                getListOrDefault(filter.getFilter(), "hygiene_types"),
                                List.of())
                                .stream()
                                .map(String::valueOf)
                                .map(SalesforceCaseFilter.EXTRA_CRITERIA::fromString)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList()))
                        .caseIds(getListOrDefault(filter.getFilter(), "case_ids"))
                        .aggInterval(MoreObjects.firstNonNull(
                                AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                        .caseNumbers(getListOrDefault(filter.getFilter(), "case_numbers"))
                        .priorities(getListOrDefault(filter.getFilter(), "priorities"))
                        .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                        .contacts(getListOrDefault(filter.getFilter(), "contacts"))
                        .types(getListOrDefault(filter.getFilter(), "types"))
                        .age(filter.<String, Object>getFilterValueAsMap("age").orElse(Map.of()))
                        .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                        .accounts(getListOrDefault(filter.getFilter(), "accounts"))
                        .build();
                map.put(value,
                        AggCacheUtils.cacheOrCall(disableCache, company,
                                "/salesforce_cases/values",
                                sfCaseFilter.generateCacheHash(),
                                sfCaseFilter.getIntegrationIds(), objectMapper, aggCacheService,
                                () -> salesforceCaseService.groupByAndCalculate(
                                        company,
                                        sfCaseFilter,
                                        null))
                                .getRecords());
                response.add(map);
            }
            return ResponseEntity.ok().body(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/bounce_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getBounceReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, SalesforceCaseFilter.CALCULATION.bounces, filter);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/hops_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHopsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, SalesforceCaseFilter.CALCULATION.hops, filter);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/cases_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getTicketsReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, SalesforceCaseFilter.CALCULATION.case_count, filter);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/resolution_time_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getResolutionTimeReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, SalesforceCaseFilter.CALCULATION.resolution_time, filter);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/hygiene_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getHygieneReport(
            @RequestParam(name = "there_is_no_cache", required = false, defaultValue = "false") Boolean disableCache,
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(disableCache, company, SalesforceCaseFilter.CALCULATION.case_count, filter);
    }

    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggResult(
            Boolean disableCache,
            String company,
            SalesforceCaseFilter.CALCULATION calc,
            DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            SalesforceCaseFilter.DISTINCT across = MoreObjects.firstNonNull(SalesforceCaseFilter.DISTINCT.fromString(
                    filter.getAcross()), SalesforceCaseFilter.DISTINCT.contact);

            SalesforceCaseFilter sfCaseFilter = parseSFCaseFilter(company, filter, calc, across);
            if (filter.getFilter().get("config_table_id") != null) {
                String configTableId = filter.getFilter().get("config_table_id").toString();
                if(!UUID_REGEX_PATTERN.matcher(configTableId).matches())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Invalid config table key parameter. Required UUID. Provided: " + configTableId);
                ConfigTable configTable = configTableDatabaseService.get(company, configTableId).orElse(null);
                checkRequired(configTable, "No Config table found for given config_table_id");

                ConfigTable.Column acrossColumn = getColumn(configTable, filter.getAcross());
                checkRequired(acrossColumn, "No column found for given across field");

                if (acrossColumn.getMultiValue())
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Across column should be single value");

                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    SalesforceCaseFilter updatedCaseFilter = updateSFCaseFilter(sfCaseFilter,
                            configTable.getSchema().getColumns(), row.getValue());
                    String rowValue = row.getValue().getValues().get(acrossColumn.getId());
                    List<DbAggregationResult> records = AggCacheUtils.cacheOrCall(
                            disableCache, company,
                            "/salesforce_cases/aggs_config",
                            sfCaseFilter.generateCacheHash(),
                            sfCaseFilter.getIntegrationIds(), objectMapper, aggCacheService,
                            () -> salesforceCaseService.groupByAndCalculate(
                                    company, updatedCaseFilter, rowValue))
                            .getRecords();
                    aggregationRecords.addAll(records);
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(),
                                filter.getPageSize(),
                                AggCacheUtils.cacheOrCall(disableCache, company,
                                        "/salesforce_cases/aggs_nonconfig",
                                        sfCaseFilter.generateCacheHash(),
                                        sfCaseFilter.getIntegrationIds(), objectMapper, aggCacheService,
                                        () -> salesforceCaseService.groupByAndCalculate(company, sfCaseFilter, null))));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private SalesforceCaseFilter parseSFCaseFilter(String company, DefaultListRequest filter,
                                                   SalesforceCaseFilter.CALCULATION calc,
                                                   SalesforceCaseFilter.DISTINCT across) throws SQLException {
        Map<String, String> sfCreatedRange = filter.getFilterValue("created_at", Map.class)
                .orElse(Map.of());
        Map<String, String> sfUpdatedRange = filter.getFilterValue("updated_at", Map.class)
                .orElse(Map.of());

        final Long sfCreateStart = sfCreatedRange.get("$gt") != null ? Long.valueOf(sfCreatedRange.get("$gt")) : null;
        final Long sfCreateEnd = sfCreatedRange.get("$lt") != null ? Long.valueOf(sfCreatedRange.get("$lt")) : null;
        final Long sfUpdateStart = sfUpdatedRange.get("$gt") != null ? Long.valueOf(sfUpdatedRange.get("$gt")) : null;
        final Long sfUpdateEnd = sfUpdatedRange.get("$lt") != null ? Long.valueOf(sfUpdatedRange.get("$lt")) : null;

        SalesforceCaseFilter salesforceCaseFilter = SalesforceCaseFilter.builder()
                .ingestedAt(getIngestedAt(company, filter))
                .calculation(calc)
                .across(across)
                .extraCriteria(MoreObjects.firstNonNull(
                        getListOrDefault(filter.getFilter(), "hygiene_types"),
                        List.of())
                        .stream()
                        .map(String::valueOf)
                        .map(SalesforceCaseFilter.EXTRA_CRITERIA::fromString)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .caseIds(getListOrDefault(filter.getFilter(), "case_ids"))
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .caseNumbers(getListOrDefault(filter.getFilter(), "case_numbers"))
                .priorities(getListOrDefault(filter.getFilter(), "priorities"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .contacts(getListOrDefault(filter.getFilter(), "contacts"))
                .types(getListOrDefault(filter.getFilter(), "types"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .accounts(getListOrDefault(filter.getFilter(), "accounts"))
                .age(filter.<String, Object>getFilterValueAsMap("age").orElse(Map.of()))
                .SFCreatedRange(ImmutablePair.of(sfCreateStart, sfCreateEnd))
                .SFUpdatedRange(ImmutablePair.of(sfUpdateStart, sfUpdateEnd))
                .build();
        log.info("salesforceCaseFilter = {}", salesforceCaseFilter);
        return salesforceCaseFilter;
    }

    private SalesforceCaseFilter updateSFCaseFilter(SalesforceCaseFilter sfCaseFilter, Map<String, ConfigTable.Column> columns, ConfigTable.Row row) {
        SalesforceCaseFilter.SalesforceCaseFilterBuilder filterBuilder = sfCaseFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "sf_priorities":
                    filterBuilder.priorities(getIntersection(getRowValue(row, column.getValue()), sfCaseFilter.getPriorities()));
                    break;
                case "sf_contacts":
                    filterBuilder.contacts(getIntersection(getRowValue(row, column.getValue()), sfCaseFilter.getContacts()));
                    break;
                case "sf_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), sfCaseFilter.getStatuses()));
                    break;
                case "sf_types":
                    filterBuilder.types(getIntersection(getRowValue(row, column.getValue()), sfCaseFilter.getTypes()));
                    break;
                case "sf_accounts":
                    filterBuilder.accounts(getIntersection(getRowValue(row, column.getValue()), sfCaseFilter.getAccounts()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    private List<String> getRowValue(ConfigTable.Row row, ConfigTable.Column column) {
        String rowValue = row.getValues().get(column.getId());
        if (column.getMultiValue()) {
            String sanitizedRowValue = rowValue.replaceAll("^\\[|]$", "").replaceAll("\"", "");
            return Arrays.asList(sanitizedRowValue.split(","));
        }
        return Collections.singletonList(rowValue);
    }

    private List<String> getIntersection(List<String> rowValues, List<String> filterValues) {
        if (rowValues.size() == 0) {
            return filterValues;
        } else if (filterValues.size() == 0) {
            return rowValues;
        } else {
            return new ArrayList<>(CollectionUtils.intersection(rowValues, filterValues));
        }
    }

    private ConfigTable.Column getColumn(ConfigTable configTable, String key) {
        return configTable.getSchema().getColumns().values().stream()
                .filter(column -> (column.getKey().equalsIgnoreCase(key)))
                .findAny().orElse(null);
    }

    private void checkRequired(Object required, String message) {
        if (required instanceof String && StringUtils.isEmpty((String) required)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        } else if (required == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }
}
