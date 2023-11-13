package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CxSastIssueFilter;
import io.levelops.commons.models.DefaultListRequest;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.levelops.api.utils.MapUtilsForRESTControllers.getListOrDefault;

@Log4j2
@Service
@SuppressWarnings("unused")
public class CxSastIssueFilterParser {

    public CxSastIssueFilter createFilterFromConfig(DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, CxSastIssueFilter.CALCULATION calc, CxSastIssueFilter.DISTINCT across,
                                                    boolean withPrefix) {
        CxSastIssueFilter cxSastIssueFilter = createFilter(filter, calc, across, withPrefix);
        CxSastIssueFilter.CxSastIssueFilterBuilder filterBuilder = cxSastIssueFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "cm_scan_ids":
                    filterBuilder.scanIds(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getScanIds()));
                    break;
                case "cm_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getStatuses()));
                    break;
                case "cm_severities":
                    filterBuilder.severities(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getSeverities()));
                    break;
                case "cm_assignees":
                    filterBuilder.assignees(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getAssignees()));
                    break;
                case "cm_states":
                    filterBuilder.states(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getStates()));
                    break;
                case "cm_projects":
                    filterBuilder.projects(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getProjects()));
                    break;
                case "cm_languages":
                    filterBuilder.languages(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getLanguages()));
                    break;
                case "cm_categories":
                    filterBuilder.categories(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getCategories()));
                    break;
                case "cm_issue_names":
                    filterBuilder.issueNames(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getIssueNames()));
                    break;
                case "cm_issue_groups":
                    filterBuilder.issueGroups(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getIssueGroups()));
                    break;
                case "cm_files":
                    filterBuilder.files(getIntersection(getRowValue(row, column.getValue()),
                            cxSastIssueFilter.getFiles()));
            }
        }
        return filterBuilder.build();
    }

    private List<String> getRowValue(ConfigTable.Row row, ConfigTable.Column column) {
        String rowValue = row.getValues().get(column.getId());
        if (column.getMultiValue()) {
            String sanitizedRowValue = rowValue.replaceAll("^\\[|]$", "")
                    .replaceAll("\"", "");
            return StringUtils.isEmpty(sanitizedRowValue) ? List.of() :
                    Arrays.asList(sanitizedRowValue.split(","));
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

    public CxSastIssueFilter createFilter(DefaultListRequest filter, CxSastIssueFilter.CALCULATION calc,
                                          CxSastIssueFilter.DISTINCT across, boolean withPrefix) {
        String prefix = withPrefix ? "cm_" : "";
        Boolean falsePositive = filter.getFilterValue("false_positive", Boolean.class).orElse(null);
        return CxSastIssueFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .assignees(getListOrDefault(filter.getFilter(), prefix + "assignees"))
                .falsePositive(falsePositive)
                .scanIds(getListOrDefault(filter.getFilter(), prefix + "scan_ids"))
                .severities(getListOrDefault(filter.getFilter(), prefix + "severities"))
                .states(getListOrDefault(filter.getFilter(), prefix + "states"))
                .statuses(getListOrDefault(filter.getFilter(), prefix + "statuses"))
                .projects(getListOrDefault(filter.getFilter(), prefix + "projects"))
                .languages(getListOrDefault(filter.getFilter(), prefix + "languages"))
                .categories(getListOrDefault(filter.getFilter(), prefix + "categories"))
                .issueNames(getListOrDefault(filter.getFilter(), prefix + "issue_names"))
                .issueGroups(getListOrDefault(filter.getFilter(), prefix + "issue_groups"))
                .files(getListOrDefault(filter.getFilter(), prefix + "files"))
                .calculation(calc)
                .across(across)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .acrossLimit(filter.getAcrossLimit())
                .build();
    }
}
