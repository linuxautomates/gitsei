package io.levelops.api.controllers.bullseye;

import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.filters.BullseyeFileFilter;
import io.levelops.commons.databases.models.filters.BullseyeBuildFilter;
import io.levelops.commons.models.DefaultListRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class BullseyeFilterParser {

    public BullseyeBuildFilter createProjectFilterFromConfig(DefaultListRequest filter,
                                                             ConfigTable.Row row,
                                                             Map<String, ConfigTable.Column> columns,
                                                             boolean withPrefix,
                                                             BullseyeBuildFilter.Distinct across,
                                                             BullseyeBuildFilter.Calculation calculation) {
        BullseyeBuildFilter bullseyeBuildFilter = createProjectFilter(filter, withPrefix, across, calculation);
        BullseyeBuildFilter.BullseyeBuildFilterBuilder filterBuilder = bullseyeBuildFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "be_build_names":
                    filterBuilder.names(getIntersection(getRowValue(row, column.getValue()), bullseyeBuildFilter.getNames()));
                    break;
                case "be_build_directories":
                    filterBuilder.directories(getIntersection(getRowValue(row, column.getValue()), bullseyeBuildFilter.getDirectories()));
                    break;
                case "be_build_projects":
                    filterBuilder.projects(getIntersection(getRowValue(row, column.getValue()), bullseyeBuildFilter.getProjects()));
                    break;
                case "be_build_job_names":
                    filterBuilder.jobNames(getIntersection(getRowValue(row, column.getValue()), bullseyeBuildFilter.getJobNames()));
                    break;
                case "be_build_job_full_names":
                    filterBuilder.jobFullNames(getIntersection(getRowValue(row, column.getValue()), bullseyeBuildFilter.getJobFullNames()));
                    break;
                case "be_build_job_normalized_full_names":
                    filterBuilder.jobNormalizedFullNames(getIntersection(getRowValue(row, column.getValue()), bullseyeBuildFilter.getJobNormalizedFullNames()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    @SuppressWarnings("unchecked")
    public BullseyeBuildFilter createProjectFilter(DefaultListRequest filter, boolean withPrefix, BullseyeBuildFilter.Distinct across,
                                                   BullseyeBuildFilter.Calculation calculation) {
        Map<String, Map<String, String>> partialMatchMap =
                MapUtils.emptyIfNull((Map<String, Map<String, String>>) filter.getFilter().get("partial_match"));
        String prefix = getProjectPrefix(withPrefix);
        return BullseyeBuildFilter.builder()
                .cicdJobRunIds(getListOrDefault(filter, prefix + "cicd_job_run_ids"))
                .names(getListOrDefault(filter, prefix + "names"))
                .directories(getListOrDefault(filter, prefix + "directories"))
                .projects(getListOrDefault(filter, prefix + "projects"))
                .functionsCovered(getRange(filter, prefix + "functions_covered"))
                .totalFunctions(getRange(filter, prefix + "total_functions"))
                .decisionsCovered(getRange(filter, prefix + "decisions_covered"))
                .totalDecisions(getRange(filter, prefix + "total_decisions"))
                .conditionsCovered(getRange(filter, prefix + "conditions_covered"))
                .totalConditions(getRange(filter, prefix + "total_conditions"))
                .builtAtRange(getRange(filter, prefix + "built_at_range"))
                .buildIds(getListOrDefault(filter, prefix + "build_ids"))
                .jobNames(getListOrDefault(filter, prefix + "job_names"))
                .jobFullNames(getListOrDefault(filter, prefix + "job_full_names"))
                .jobNormalizedFullNames(getListOrDefault(filter, prefix + "job_normalized_full_names"))
                .partialMatch(partialMatchMap)
                .across(across)
                .calculation(calculation)
                .build();
    }

    public BullseyeFileFilter createFileFilterFromConfig(DefaultListRequest filter,
                                                         ConfigTable.Row row,
                                                         Map<String, ConfigTable.Column> columns,
                                                         boolean withPrefix) {
        BullseyeFileFilter bullseyeFileFilter = createFileFilter(filter, withPrefix);
        BullseyeFileFilter.BullseyeFileFilterBuilder filterBuilder = bullseyeFileFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            if (column.getValue().getKey().equals("be_file_names"))
                filterBuilder.names(getIntersection(getRowValue(row, column.getValue()), bullseyeFileFilter.getNames()));
        }
        return filterBuilder.build();
    }

    public BullseyeFileFilter createFileFilter(DefaultListRequest filter, boolean withPrefix) {
        String prefix = getFilePrefix(withPrefix);
        return BullseyeFileFilter.builder()
                .names(getListOrDefault(filter, prefix + "names"))
                .functionsCovered(getRange(filter, prefix + "functions_covered"))
                .totalFunctions(getRange(filter, prefix + "total_functions"))
                .decisionsCovered(getRange(filter, prefix + "decisions_covered"))
                .totalDecisions(getRange(filter, prefix + "total_decisions"))
                .conditionsCovered(getRange(filter, prefix + "conditions_covered"))
                .totalConditions(getRange(filter, prefix + "total_conditions"))
                .modificationTimeRange(getRange(filter, prefix + "modification_time_range"))
                .build();
    }

    public String getProjectPrefix(boolean withPrefix) {
        return withPrefix ? "be_build_" : "";
    }

    public String getFilePrefix(boolean withPrefix) {
        return withPrefix ? "be_file_" : "";
    }

    private List<String> getListOrDefault(DefaultListRequest filter, String key) {
        return filter.<String>getFilterValueAsList(key).orElse(List.of());
    }

    private Map<String, String> getRange(DefaultListRequest filter, String key) {
        return filter.<String, String>getFilterValueAsMap(key).orElse(Map.of());
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
            return StringUtils.isEmpty(sanitizedRowValue) ? List.of() : Arrays.asList(sanitizedRowValue.split(","));
        }
        return Collections.singletonList(rowValue);
    }
}
