package io.levelops.api.controllers.bullseye;

import com.google.common.base.MoreObjects;
import io.levelops.api.controllers.ConfigTableHelper;
import io.levelops.commons.databases.models.filters.util.SortingConverter;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.database.bullseye.BullseyeSourceFile;
import io.levelops.commons.databases.models.database.bullseye.DbBullseyeBuild;
import io.levelops.commons.databases.models.filters.BullseyeFileFilter;
import io.levelops.commons.databases.models.filters.BullseyeBuildFilter;
import io.levelops.commons.databases.models.response.DbAggregationResult;
import io.levelops.commons.databases.services.bullseye.BullseyeDatabaseService;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.commons.models.PaginatedResponse;
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

@RestController
@Log4j2
@PreAuthorize("hasAnyAuthority('ADMIN','SUPER_ADMIN')")
@RequestMapping("/v1/bullseye_builds")
@SuppressWarnings("unused")
public class BullseyeProjectController {

    private final ConfigTableHelper configTableHelper;
    private final BullseyeDatabaseService bullseyeDatabaseService;
    private final BullseyeFilterParser bullseyeFilterParser;

    public BullseyeProjectController(BullseyeDatabaseService bullseyeDatabaseService, ConfigTableHelper configTableHelper,
                                     BullseyeFilterParser bullseyeFilterParser) {
        this.bullseyeDatabaseService = bullseyeDatabaseService;
        this.configTableHelper = configTableHelper;
        this.bullseyeFilterParser = bullseyeFilterParser;
    }

    @RequestMapping(method = RequestMethod.POST, value = "/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbBullseyeBuild>>> getListBuilds(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            BullseyeBuildFilter projectFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                projectFilter = bullseyeFilterParser.createProjectFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), true, null, null);
            } else {
                projectFilter = bullseyeFilterParser.createProjectFilter(filter, false, null, null);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            bullseyeDatabaseService.listProjects(company, projectFilter,
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), Collections.emptyList())),
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
                response.add(Map.of(field, bullseyeDatabaseService.groupByAndCalculateProjects(
                        company,
                        bullseyeFilterParser.createProjectFilter(filter,
                                false,
                                BullseyeBuildFilter.Distinct.fromString(field),
                                BullseyeBuildFilter.Calculation.total_coverage), null)
                        .getRecords()));
            }
            return ResponseEntity.ok(PaginatedResponse.of(0, response.size(), response));
        });
    }

    @RequestMapping(method = RequestMethod.POST, value = "/files/list", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<BullseyeSourceFile>>> getListFiles(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return SpringUtils.deferResponse(() -> {
            BullseyeFileFilter fileFilter;
            BullseyeBuildFilter projectFilter;
            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForList(company, filter);
                String configTableRowId = filter.getFilter().get("config_table_row_id").toString();
                fileFilter = bullseyeFilterParser.createFileFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), true);
                projectFilter = bullseyeFilterParser.createProjectFilterFromConfig(filter, configTable.getRows().get(configTableRowId),
                        configTable.getSchema().getColumns(), true, null, null);
            } else {
                fileFilter = bullseyeFilterParser.createFileFilter(filter, true);
                projectFilter = bullseyeFilterParser.createProjectFilter(filter, true, null, null);
            }
            return ResponseEntity.ok(
                    PaginatedResponse.of(filter.getPage(),
                            filter.getPageSize(),
                            bullseyeDatabaseService.listFiles(company, projectFilter, fileFilter,
                                    SortingConverter.fromFilter(MoreObjects.firstNonNull(filter.getSort(), Collections.emptyList())),
                                    filter.getPage(),
                                    filter.getPageSize())));
        });
    }


    @RequestMapping(method = RequestMethod.POST, value = "/coverage_report", produces = "application/json")
    public DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getCoverageReport(
            @SessionAttribute(name = "company") String company,
            @RequestBody DefaultListRequest filter) {
        return getAggReport(company, filter, "total_coverage");
    }


    private DeferredResult<ResponseEntity<PaginatedResponse<DbAggregationResult>>> getAggReport(String company, DefaultListRequest filter,
                                                                                                String calculation) {

        return SpringUtils.deferResponse(() -> {
            BullseyeBuildFilter.Calculation calc = BullseyeBuildFilter.Calculation.fromString(calculation);
            BullseyeBuildFilter.Distinct across = BullseyeBuildFilter.Distinct.fromString(filter.getAcross());
            BullseyeBuildFilter projectFilter;

            if (configTableHelper.isConfigBasedAggregation(filter)) {
                ConfigTable configTable = configTableHelper.validateAndReturnTableForReport(company, filter);
                List<DbAggregationResult> aggregationRecords = new ArrayList<>();
                for (Map.Entry<String, ConfigTable.Row> row : configTable.getRows().entrySet()) {
                    projectFilter = bullseyeFilterParser.createProjectFilterFromConfig(filter, row.getValue(),
                            configTable.getSchema().getColumns(), true, across, calc);
                    String rowValue = row.getValue().getValues()
                            .get(configTableHelper.getColumn(configTable, filter.getAcross()).getId());

                    List<DbAggregationResult> records = bullseyeDatabaseService.groupByAndCalculateProjects(
                            company, projectFilter, rowValue).getRecords();
                    aggregationRecords.addAll(records);
                }
                return ResponseEntity.ok(PaginatedResponse.of(filter.getPage(), filter.getPageSize(), aggregationRecords));
            } else {
                projectFilter = bullseyeFilterParser.createProjectFilter(filter, false, across, calc);
                return ResponseEntity.ok(
                        PaginatedResponse.of(filter.getPage(),
                                filter.getPageSize(),
                                bullseyeDatabaseService.groupByAndCalculateProjects(company, projectFilter, null)));
            }
        });
    }
}
