package io.levelops.api.controllers;

import com.google.common.base.MoreObjects;
import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.filters.AGG_INTERVAL;
import io.levelops.commons.databases.models.filters.CxSastScanFilter;
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
public class CxSastScanFilterParser {

    public CxSastScanFilter createFilterFromConfig(DefaultListRequest filter, ConfigTable.Row row, Map<String,
            ConfigTable.Column> columns, CxSastScanFilter.CALCULATION calc, CxSastScanFilter.DISTINCT across,
                                                   boolean withPrefix) {
        CxSastScanFilter cxSastScanFilter = createFilter(filter, calc, across, withPrefix);
        CxSastScanFilter.CxSastScanFilterBuilder filterBuilder = cxSastScanFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "cm_scan_ids":
                    filterBuilder.scanIds(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getScanIds()));
                    break;
                case "cm_scan_types":
                    filterBuilder.scanTypes(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getScanTypes()));
                    break;
                case "cm_scan_paths":
                    filterBuilder.scanPaths(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getScanPaths()));
                    break;
                case "cm_languages":
                    filterBuilder.languages(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getLanguages()));
                    break;
                case "cm_owners":
                    filterBuilder.owners(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getOwners()));
                    break;
                case "cm_initiator_names":
                    filterBuilder.initiatorNames(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getInitiatorNames()));
                    break;
                case "cm_project_names":
                    filterBuilder.projectNames(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getProjectNames()));
                    break;
                case "cm_statuses":
                    filterBuilder.statuses(getIntersection(getRowValue(row, column.getValue()),
                            cxSastScanFilter.getStatuses()));
                    break;
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

    public CxSastScanFilter createFilter(DefaultListRequest filter, CxSastScanFilter.CALCULATION calc,
                                         CxSastScanFilter.DISTINCT across, boolean withPrefix) {
        String prefix = withPrefix ? "cm_" : "";
        Boolean isPublic = filter.getFilterValue("public", Boolean.class).orElse(null);
        return CxSastScanFilter.builder()
                .integrationIds(getListOrDefault(filter.getFilter(), "integration_ids"))
                .initiatorNames(getListOrDefault(filter.getFilter(), prefix + "initiator_names"))
                .isPublic(isPublic)
                .scanIds(getListOrDefault(filter.getFilter(), prefix + "scan_ids"))
                .languages(getListOrDefault(filter.getFilter(), prefix + "languages"))
                .owners(getListOrDefault(filter.getFilter(), prefix + "owners"))
                .projectNames(getListOrDefault(filter.getFilter(), prefix + "project_names"))
                .scanPaths(getListOrDefault(filter.getFilter(), prefix + "scan_paths"))
                .scanTypes(getListOrDefault(filter.getFilter(), prefix + "scan_types"))
                .statuses(getListOrDefault(filter.getFilter(), prefix + "statuses"))
                .calculation(calc)
                .across(across)
                .aggInterval(MoreObjects.firstNonNull(
                        AGG_INTERVAL.fromString(filter.getAggInterval()), AGG_INTERVAL.day))
                .acrossLimit(filter.getAcrossLimit())
                .build();
    }
}
