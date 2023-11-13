package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeIssue;
import io.levelops.commons.databases.models.filters.SonarQubeIssueFilter;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.IntegrationService;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.SonarQubeIssueService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
import io.levelops.ingestion.models.IntegrationType;
import io.levelops.web.util.SpringUtils;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

/**
 * REST API Controller for Sonarqube Aggregation Service
 */
@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN','ORG_ADMIN_USER')")
@RequestMapping("/v1/sonarqube_issues")
@SuppressWarnings("unused")
public class SonarQubeIssuesController {

    private final IntegrationService integrationService;
    private final SonarQubeIssueService issueService;
    private final IntegrationTrackingService trackingService;
    private final ConfigTableHelper configTableHelper;
    private final OrgUnitHelper orgUnitHelper;

    public SonarQubeIssuesController(SonarQubeIssueService issueService, IntegrationTrackingService trackingService,
                                     IntegrationService integrationService,
                                     ConfigTableHelper configTableHelper,
                                     final OrgUnitHelper orgUnitHelper) {
        this.integrationService = integrationService;
        this.issueService = issueService;
        this.trackingService = trackingService;
        this.configTableHelper = configTableHelper;
        this.orgUnitHelper = orgUnitHelper;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbSonarQubeIssue>>> getListOfIssues(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig  = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_issues/list' for the request: {}", company, originalRequest, e);
            }

            SonarQubeIssueFilter issueFilter = getSQIssueFilterBuilder(company, request).build();
            SonarQubeIssueFilter sonarQubeIssueFilter;
            if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, request);
                String configTableRowId = request.getFilterValue("config_table_row_id", String.class).orElseThrow();
                sonarQubeIssueFilter = configTable.getRows().entrySet().stream()
                        .filter(row -> configTableRowId.equals(row.getValue().getId()))
                        .findFirst()
                        .map(row -> updateSonarQubeFilter(issueFilter, configTable.getSchema().getColumns(),
                                row.getValue()))
                        .orElse(issueFilter);
            } else {
                sonarQubeIssueFilter = issueFilter;
            }
            final var page = request.getPage();
            final var pageSize = request.getPageSize();
            final var finalOuConfig = ouConfig;
            return ResponseEntity.ok(PaginatedResponse.of(
                    request.getPage(),
                    request.getPageSize(),
                    issueService.list(company, sonarQubeIssueFilter,
                            SortingConverter.fromFilter(MoreObjects.firstNonNull(request.getSort(), Collections.emptyList())),
                            request.getPage(), request.getPageSize(), finalOuConfig)));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/values", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<Map<String, List<DbAggregationResult>>>>> getValuesReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest originalRequest) throws SQLException {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_issues/values' for the request: {}", company, originalRequest, e);
            }
            final var finalOuConfig = ouConfig;
            List<Map<String, List<DbAggregationResult>>> response = new ArrayList<>();
            for (String field : request.getFields()) {
                response.add(Map.of(field, issueService.groupByAndCalculateForValues(company,
                        getSQIssueFilterBuilder(company, request)
                                .distinct(SonarQubeIssueFilter.DISTINCT.fromString(field))
                                .calculation(SonarQubeIssueFilter.CALCULATION.issue_count)
                                .build(), null, finalOuConfig).getRecords()));
            }

            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/issue_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getIssueReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(company, filter, SonarQubeIssueFilter.CALCULATION.issue_count);
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_COLLECTIONS, permission = Permission.COLLECTIONS_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/effort_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getEffortReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggResult(company, filter, SonarQubeIssueFilter.CALCULATION.effort);
    }

    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggResult(
            String company,
            DefaultListRequest originalRequest,
            SonarQubeIssueFilter.CALCULATION calculation) {
        return SpringUtils.deferResponse(() -> {
            // OU stuff
            var request = originalRequest;
            OUConfiguration ouConfig = null;
            try {
                ouConfig = orgUnitHelper.getOuConfigurationFromRequest(company, IntegrationType.SONARQUBE, originalRequest);
                request = ouConfig.getRequest();
            } catch (SQLException e) {
                log.error("[{}] Unable to process the OU config in '/sonarqube_issues/..getAggResult()' for the request: {}", company, originalRequest, e);
            }

            SonarQubeIssueFilter issueFilter = getSQIssueFilterBuilder(company, request)
                    .distinct(SonarQubeIssueFilter.DISTINCT.fromString(request.getAcross()))
                    .calculation(calculation)
                    .build();
            final var finalOuConfig = ouConfig;
            if (configTableHelper.isConfigBasedAggregation(request)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, request);
                String acrossColumnId = configTableHelper.getColumn(configTable, request.getAcross()).getId();
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    SonarQubeIssueFilter updatedIssueFilter = updateSonarQubeFilter(issueFilter,
                            configTable.getSchema().getColumns(), row.getValue());
                    String rowValue = row.getValue().getValues().get(acrossColumnId);
                    aggregationRecords.addAll(issueService.stackedGroupBy(company, updatedIssueFilter,
                            getStacks(request.getStacks()), rowValue, finalOuConfig).getRecords());
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(request.getPage(), request.getPageSize(), aggregationRecords));
            } else {
                return ResponseEntity.ok(PaginatedResponse.of(
                        request.getPage(),
                        request.getPageSize(),
                        issueService.stackedGroupBy(company, issueFilter, getStacks(request.getStacks()), null, finalOuConfig)));
            }
        });
    }

    private SonarQubeIssueFilter.SonarQubeIssueFilterBuilder getSQIssueFilterBuilder(
            String company,
            DefaultListRequest filter) throws SQLException {
        return SonarQubeIssueFilter.builder()
                .projects(getListOrDefault(filter.getFilter(), "projects"))
                .types(getListOrDefault(filter.getFilter(), "types"))
                .severities(getListOrDefault(filter.getFilter(), "severities"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .organizations(getListOrDefault(filter.getFilter(), "organizations"))
                .authors(getListOrDefault(filter.getFilter(), "authors"))
                .components(getListOrDefault(filter.getFilter(), "components"))
                .tags(getListOrDefault(filter.getFilter(), "tags"))
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .ingestedAt(getIngestedAt(company, filter))
                .partialMatch(getPartialMatch(filter));
    }

    private static Map<String, Map<String, String>> getPartialMatch(DefaultListRequest filter) {
        return filter.getFilter() == null ? Collections.emptyMap() :
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
    }

    private Long getIngestedAt(String company, DefaultListRequest filter) throws SQLException {
        Integration integration = integrationService.listByFilter(company, null,
                List.of(IntegrationType.SONARQUBE.toString()), null,
                getListOrDefault(filter.getFilter(), "integration_ids").stream()
                        .map(NumberUtils::toInt).collect(Collectors.toList()), List.of(),
                0, 1).getRecords().stream().findFirst().orElse(null);
        Long ingestedAt = DateUtils.truncate(new Date(), Calendar.DATE);
        if (integration != null)
            ingestedAt = trackingService.get(company, integration.getId())
                    .orElse(IntegrationTracker.builder().latestIngestedAt(ingestedAt).build())
                    .getLatestIngestedAt();
        return ingestedAt;
    }

    private List<SonarQubeIssueFilter.DISTINCT> getStacks(List<String> stacks) {
        return CollectionUtils.emptyIfNull(stacks).stream()
                .map(SonarQubeIssueFilter.DISTINCT::fromString)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void checkRequired(Object required, String message) {
        if (required instanceof String && StringUtils.isEmpty((String) required)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        } else if (required == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private SonarQubeIssueFilter updateSonarQubeFilter(SonarQubeIssueFilter sonarQubeIssueFilter,
                                                       Map<String, ConfigTable.Column> columns,
                                                       ConfigTable.Row row) {
        SonarQubeIssueFilter.SonarQubeIssueFilterBuilder sonarQubeIssueFilterBuilder = sonarQubeIssueFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "sq_severities":
                    sonarQubeIssueFilterBuilder.severities(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getSeverities()));
                    break;
                case "sq_types":
                    sonarQubeIssueFilterBuilder.types(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getTypes()));
                    break;
                case "sq_statuses":
                    sonarQubeIssueFilterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getStatuses()));
                    break;
                case "sq_projects":
                    sonarQubeIssueFilterBuilder.projects(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getProjects()));
                    break;
                case "sq_organizations":
                    sonarQubeIssueFilterBuilder.organizations(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getOrganizations()));
                    break;
                case "sq_authors":
                    sonarQubeIssueFilterBuilder.authors(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getAuthors()));
                    break;
                case "sq_components":
                    sonarQubeIssueFilterBuilder.components(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getComponents()));
                    break;
                case "sq_tags":
                    sonarQubeIssueFilterBuilder.tags(getIntersection(getRowValue(row, column.getValue()), sonarQubeIssueFilter.getTags()));
                    break;
            }
        }
        return sonarQubeIssueFilterBuilder.build();
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

    private ConfigTable.Column getColumn(ConfigTable configTable, String key) {
        return configTable.getSchema().getColumns().values().stream()
                .filter(column -> (column.getKey().equalsIgnoreCase(key)))
                .findAny().orElse(null);
    }
}
