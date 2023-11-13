package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.harness.authz.acl.model.Permission;
import io.harness.authz.acl.model.ResourceType;
import io.levelops.auth.authz.HarnessAccessControlCheck;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.awsdevtools.DbAWSDevToolsProject;
import io.levelops.commons.databases.models.filters.AWSDevToolsProjectsFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.AWSDevToolsProjectDatabaseService;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN', 'PUBLIC_DASHBOARD','SUPER_ADMIN')")
@RequestMapping("/v1/awsdevtools_project")
@SuppressWarnings("unused")
public class AWSDevToolsProjectController {

    private final ConfigTableHelper configTableHelper;
    private final AWSDevToolsFilterParser filterParser;
    private final AWSDevToolsProjectDatabaseService projectDatabaseService;

    public AWSDevToolsProjectController(ConfigTableHelper configTableHelper,
                                        AWSDevToolsFilterParser filterParser,
                                        AWSDevToolsProjectDatabaseService projectDatabaseService) {
        this.configTableHelper = configTableHelper;
        this.filterParser = filterParser;
        this.projectDatabaseService = projectDatabaseService;
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS, permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAWSDevToolsProject>>> getListOfProjects(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            AWSDevToolsProjectsFilter projectsFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                projectsFilter = filterParser.createProjectFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), null, null, false);
            } else {
                projectsFilter = filterParser.createProjectFilter(filter, null, null, false);
            }
            return ResponseEntity.ok(PaginatedResponse.of(
                    filter.getPage(),
                    filter.getPageSize(),
                    projectDatabaseService.list(company, projectsFilter,
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
                response.add(Map.of(field, projectDatabaseService.groupByAndCalculate(company,
                        filterParser.getAWSDevToolsProjectsFilterBuilder(filter)
                                .calculation(AWSDevToolsProjectsFilter.CALCULATION.project_count)
                                .across(AWSDevToolsProjectsFilter.DISTINCT.fromString(field))
                                .build(), null).getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @HarnessAccessControlCheck(resourceType = ResourceType.SEI_CONFIGURATION_SETTINGS,permission = Permission.ACCOUNT_CONFIG_VIEW)
    @RequestMapping(method = RequestMethod.POST, value = "/report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getProjectsReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, filter, AWSDevToolsProjectsFilter.CALCULATION.project_count);
    }

    @NotNull
    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReport(
            String company, DefaultListRequest filter, AWSDevToolsProjectsFilter.CALCULATION output) {
        return SpringUtils.deferResponse(() -> {
            AWSDevToolsProjectsFilter.DISTINCT across = AWSDevToolsProjectsFilter.DISTINCT.fromString(filter.getAcross());
            if (across == null) {
                across = AWSDevToolsProjectsFilter.DISTINCT.source_type;
            }
            List<AWSDevToolsProjectsFilter.DISTINCT> stacks = null;
            if (CollectionUtils.isNotEmpty(filter.getStacks()))
                stacks = filter.getStacks().stream()
                        .map(AWSDevToolsProjectsFilter.DISTINCT::valueOf)
                        .collect(Collectors.toList());
            AWSDevToolsProjectsFilter projectsFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    projectsFilter = filterParser.createProjectFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), output, across, false);
                    String rowValue = row.getValue().getValues()
                            .get(configTableHelper.getColumn(configTable, filter.getAcross()).getId());
                    List<DbAggregationResult> records =
                            projectDatabaseService.stackedGroupBy(company, projectsFilter, stacks, rowValue).getRecords();
                    aggregationRecords.addAll(records);
                }
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                projectsFilter = filterParser.createProjectFilter(filter, output, across, false);
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(),
                                filter.getPageSize(),
                                projectDatabaseService.stackedGroupBy(company, projectsFilter, stacks, null)));
            }
        });
    }
}
