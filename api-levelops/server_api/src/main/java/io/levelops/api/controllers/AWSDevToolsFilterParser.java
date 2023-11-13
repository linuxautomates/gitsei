package io.levelops.api.controllers;

import io.levelops.api.utils.MapUtilsForRESTControllers;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildBatchesFilter;
import io.levelops.commons.databases.models.filters.AWSDevToolsBuildsFilter;
import io.levelops.commons.databases.models.filters.AWSDevToolsProjectsFilter;
import io.levelops.commons.databases.models.filters.AWSDevToolsTestcasesFilter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@Log4j2
@Service
@SuppressWarnings("unused")
public class AWSDevToolsFilterParser {

    @Autowired
    public AWSDevToolsFilterParser() {
    }

    public AWSDevToolsProjectsFilter createProjectFilter(DefaultListRequest filter, AWSDevToolsProjectsFilter.CALCULATION calc,
                                                         AWSDevToolsProjectsFilter.DISTINCT across, boolean withPrefix) {
        String prefix = withPrefix ? "aws_cb_project_" : "";
        List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
        return AWSDevToolsProjectsFilter.builder()
                .calculation(calc)
                .across(across)
                .sourceTypes(getListOrDefault(filter.getFilter(), prefix + "source_types"))
                .regions(getListOrDefault(filter.getFilter(), prefix + "regions"))
                .integrationIds(integrationIds)
                .build();
    }

    public AWSDevToolsProjectsFilter createProjectFilterFromConfig(DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, AWSDevToolsProjectsFilter.CALCULATION calc, AWSDevToolsProjectsFilter.DISTINCT across,
                                                                   boolean withPrefix) {
        AWSDevToolsProjectsFilter projectsFilter = createProjectFilter(filter, calc, across, withPrefix);
        AWSDevToolsProjectsFilter.AWSDevToolsProjectsFilterBuilder filterBuilder = projectsFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "aws_cb_project_source_types":
                    filterBuilder.sourceTypes(getIntersection(getRowValue(row, column.getValue()), projectsFilter.getSourceTypes()));
                    break;
                case "aws_cb_project_regions":
                    filterBuilder.regions(getIntersection(getRowValue(row, column.getValue()), projectsFilter.getRegions()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    public AWSDevToolsBuildBatchesFilter createBuildBatchFilter(DefaultListRequest filter, AWSDevToolsBuildBatchesFilter.CALCULATION calc,
                                                                AWSDevToolsBuildBatchesFilter.DISTINCT across, boolean withPrefix) {
        String prefix = withPrefix ? "aws_cb_build_batch_" : "";
        List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
        return AWSDevToolsBuildBatchesFilter.builder()
                .calculation(calc)
                .across(across)
                .projectNames(getListOrDefault(filter.getFilter(), prefix + "project_names"))
                .sourceTypes(getListOrDefault(filter.getFilter(), prefix + "source_types"))
                .statuses(getListOrDefault(filter.getFilter(), prefix + "statuses"))
                .lastPhases(getListOrDefault(filter.getFilter(), prefix + "last_phases"))
                .lastPhaseStatuses(getListOrDefault(filter.getFilter(), prefix + "last_phase_statuses"))
                .initiators(getListOrDefault(filter.getFilter(), prefix + "initiators"))
                .regions(getListOrDefault(filter.getFilter(), prefix + "regions"))
                .integrationIds(integrationIds)
                .build();
    }

    public AWSDevToolsBuildBatchesFilter createBuildBatchFilterFromConfig(DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, AWSDevToolsBuildBatchesFilter.CALCULATION calc, AWSDevToolsBuildBatchesFilter.DISTINCT across,
                                                                          boolean withPrefix) {
        AWSDevToolsBuildBatchesFilter buildBatchesFilter = createBuildBatchFilter(filter, calc, across, withPrefix);
        AWSDevToolsBuildBatchesFilter.AWSDevToolsBuildBatchesFilterBuilder filterBuilder = buildBatchesFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "aws_cb_build_batch_project_names":
                    filterBuilder.projectNames(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getProjectNames()));
                    break;
                case "aws_cb_build_batch_source_types":
                    filterBuilder.sourceTypes(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getSourceTypes()));
                    break;
                case "aws_cb_build_batch_last_phases":
                    filterBuilder.lastPhases(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getLastPhases()));
                    break;
                case "aws_cb_build_batch_last_phase_statuses":
                    filterBuilder.lastPhaseStatuses(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getLastPhaseStatuses()));
                    break;
                case "aws_cb_build_batch_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getStatuses()));
                    break;
                case "aws_cb_build_batch_initiators":
                    filterBuilder.initiators(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getInitiators()));
                    break;
                case "aws_cb_build_batch_regions":
                    filterBuilder.regions(getIntersection(getRowValue(row, column.getValue()), buildBatchesFilter.getRegions()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    public AWSDevToolsBuildsFilter createBuildFilter(DefaultListRequest filter, AWSDevToolsBuildsFilter.CALCULATION calc,
                                                     AWSDevToolsBuildsFilter.DISTINCT across, boolean withPrefix) {
        String prefix = withPrefix ? "aws_cb_build_" : "";
        List<String> integrationIds = getListOrDefault(filter.getFilter(), "integration_ids");
        return AWSDevToolsBuildsFilter.builder()
                .calculation(calc)
                .across(across)
                .projectNames(getListOrDefault(filter.getFilter(), prefix + "project_names"))
                .sourceTypes(getListOrDefault(filter.getFilter(), prefix + "source_types"))
                .statuses(getListOrDefault(filter.getFilter(), prefix + "statuses"))
                .lastPhases(getListOrDefault(filter.getFilter(), prefix + "last_phases"))
                .lastPhaseStatuses(getListOrDefault(filter.getFilter(), prefix + "last_phase_statuses"))
                .initiators(getListOrDefault(filter.getFilter(), prefix + "initiators"))
                .buildBatchArns(getListOrDefault(filter.getFilter(), prefix + "build_batch_arns"))
                .regions(getListOrDefault(filter.getFilter(), prefix + "regions"))
                .integrationIds(integrationIds)
                .build();
    }

    public AWSDevToolsBuildsFilter createBuildFilterFromConfig(DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, AWSDevToolsBuildsFilter.CALCULATION calc, AWSDevToolsBuildsFilter.DISTINCT across,
                                                               boolean withPrefix) {
        AWSDevToolsBuildsFilter buildsFilter = createBuildFilter(filter, calc, across, withPrefix);
        AWSDevToolsBuildsFilter.AWSDevToolsBuildsFilterBuilder filterBuilder = buildsFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "aws_cb_build_project_names":
                    filterBuilder.projectNames(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getProjectNames()));
                    break;
                case "aws_cb_build_source_types":
                    filterBuilder.sourceTypes(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getSourceTypes()));
                    break;
                case "aws_cb_build_last_phases":
                    filterBuilder.lastPhases(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getLastPhases()));
                    break;
                case "aws_cb_build_last_phase_statuses":
                    filterBuilder.lastPhaseStatuses(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getLastPhaseStatuses()));
                    break;
                case "aws_cb_build_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getStatuses()));
                    break;
                case "build_initiators":
                    filterBuilder.initiators(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getInitiators()));
                    break;
                case "aws_cb_build_build_batch_arns":
                    filterBuilder.buildBatchArns(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getBuildBatchArns()));
                    break;
                case "aws_cb_build_regions":
                    filterBuilder.regions(getIntersection(getRowValue(row, column.getValue()), buildsFilter.getRegions()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    public AWSDevToolsTestcasesFilter createTestcaseFilter(DefaultListRequest filter, AWSDevToolsTestcasesFilter.CALCULATION calc,
                                                           AWSDevToolsTestcasesFilter.DISTINCT across, boolean withPrefix) {
        String prefix = withPrefix ? "aws_cb_testcase_" : "";
        return AWSDevToolsTestcasesFilter.builder()
                .calculation(calc)
                .across(across)
                .reportArns(getListOrDefault(filter.getFilter(), prefix + "report_arns"))
                .statuses(getListOrDefault(filter.getFilter(), prefix + "statuses"))
                .regions(getListOrDefault(filter.getFilter(), prefix + "regions"))
                .projectNames(getListOrDefault(filter.getFilter(), prefix + "project_names"))
                .sourceTypes(getListOrDefault(filter.getFilter(), prefix + "source_types"))
                .initiators(getListOrDefault(filter.getFilter(), prefix + "initiators"))
                .buildBatchArns(getListOrDefault(filter.getFilter(), prefix + "build_batch_arns"))
                .build();
    }

    public AWSDevToolsTestcasesFilter createTestcaseFilterFromConfig(DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, AWSDevToolsTestcasesFilter.CALCULATION calc, AWSDevToolsTestcasesFilter.DISTINCT across,
                                                                     boolean withPrefix) {
        AWSDevToolsTestcasesFilter testcasesFilter = createTestcaseFilter(filter, calc, across, withPrefix);
        AWSDevToolsTestcasesFilter.AWSDevToolsTestcasesFilterBuilder filterBuilder = testcasesFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "aws_cb_testcases_report_arns":
                    filterBuilder.reportArns(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getReportArns()));
                    break;
                case "aws_cb_testcases_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getStatuses()));
                    break;
                case "aws_cb_testcases_regions":
                    filterBuilder.regions(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getRegions()));
                    break;
                case "aws_cb_testcases_project_names":
                    filterBuilder.projectNames(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getProjectNames()));
                    break;
                case "aws_cb_testcases_source_types":
                    filterBuilder.sourceTypes(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getSourceTypes()));
                    break;
                case "aws_cb_testcases_initiators":
                    filterBuilder.initiators(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getInitiators()));
                    break;
                case "aws_cb_testcases_build_batch_arns":
                    filterBuilder.buildBatchArns(getIntersection(getRowValue(row, column.getValue()), testcasesFilter.getBuildBatchArns()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    protected AWSDevToolsProjectsFilter.AWSDevToolsProjectsFilterBuilder getAWSDevToolsProjectsFilterBuilder(
            DefaultListRequest filter) {
        return AWSDevToolsProjectsFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .sourceTypes(getListOrDefault(filter.getFilter(), "source_types"))
                .regions(getListOrDefault(filter.getFilter(), "regions"));
    }

    protected AWSDevToolsBuildBatchesFilter.AWSDevToolsBuildBatchesFilterBuilder getAWSDevToolsBuildBatchesFilterBuilder(
            DefaultListRequest filter) {
        return AWSDevToolsBuildBatchesFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .lastPhases(getListOrDefault(filter.getFilter(), "last_phases"))
                .lastPhaseStatuses(getListOrDefault(filter.getFilter(), "last_phase_statuses"))
                .projectNames(getListOrDefault(filter.getFilter(), "project_names"))
                .sourceTypes(getListOrDefault(filter.getFilter(), "source_types"))
                .initiators(getListOrDefault(filter.getFilter(), "initiators"))
                .regions(getListOrDefault(filter.getFilter(), "regions"));
    }

    protected AWSDevToolsBuildsFilter.AWSDevToolsBuildsFilterBuilder getAWSDevToolsBuildsFilterBuilder(
            DefaultListRequest filter) {
        return AWSDevToolsBuildsFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .lastPhases(getListOrDefault(filter.getFilter(), "last_phases"))
                .lastPhaseStatuses(getListOrDefault(filter.getFilter(), "last_phase_statuses"))
                .projectNames(getListOrDefault(filter.getFilter(), "project_names"))
                .sourceTypes(getListOrDefault(filter.getFilter(), "source_types"))
                .initiators(getListOrDefault(filter.getFilter(), "initiators"))
                .buildBatchArns(getListOrDefault(filter.getFilter(), "build_batch_arns"))
                .regions(getListOrDefault(filter.getFilter(), "regions"));
    }

    protected AWSDevToolsTestcasesFilter.AWSDevToolsTestcasesFilterBuilder getAWSDevToolsTestcasesFilterBuilder(
            DefaultListRequest filter) {
        return AWSDevToolsTestcasesFilter.builder()
                .statuses(getListOrDefault(filter.getFilter(), "statuses"))
                .reportArns(getListOrDefault(filter.getFilter(), "report_ids"))
                .regions(getListOrDefault(filter.getFilter(), "regions"))
                .projectNames(getListOrDefault(filter.getFilter(), "project_names"))
                .sourceTypes(getListOrDefault(filter.getFilter(), "source_types"))
                .initiators(getListOrDefault(filter.getFilter(), "initiators"))
                .buildBatchArns(getListOrDefault(filter.getFilter(), "build_batch_arns"));
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

    private List<String> getRowValue(ConfigTable.Row row, ConfigTable.Column column) {
        String rowValue = row.getValues().get(column.getId());
        if (column.getMultiValue()) {
            String sanitizedRowValue = rowValue.replaceAll("^\\[|]$", "").replaceAll("\"", "");
            return Arrays.asList(sanitizedRowValue.split(","));
        }
        return Collections.singletonList(rowValue);
    }
}
