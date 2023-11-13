package io.levelops.api.controllers.snyk;

import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.filters.SnykIssuesFilter;
import io.levelops.commons.models.DefaultListRequest;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class SnykFilterParser {

    public SnykIssuesFilter createFilterFromConfig(DefaultListRequest filter,
                                                   ConfigTable.Row row,
                                                   Map<String, ConfigTable.Column> columns,
                                                   SnykIssuesFilter.Calculation calculation,
                                                   SnykIssuesFilter.Distinct distinct,
                                                   boolean withPrefix) {
        SnykIssuesFilter snykIssuesFilter = createFilter(filter, withPrefix, calculation, distinct);
        SnykIssuesFilter.SnykIssuesFilterBuilder filterBuilder = snykIssuesFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column: columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "snyk_orgs":
                    filterBuilder.orgs(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getOrgs()));
                    break;
                case "snyk_projects":
                    filterBuilder.orgs(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getProjects()));
                    break;
                case "snyk_types":
                    filterBuilder.types(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getTypes()));
                    break;
                case "snyk_severities":
                    filterBuilder.severities(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getSeverities()));
                    break;
                case "snyk_languages":
                    filterBuilder.languages(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getLanguages()));
                    break;
                case "snyk_versions":
                    filterBuilder.versions(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getVersions()));
                    break;
                case "package_names":
                    filterBuilder.packageNames(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getPackageNames()));
                    break;
                case "package_managers":
                    filterBuilder.packageManagers(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getPackageManagers()));
                    break;
                case "exploit_maturities":
                    filterBuilder.exploitMaturities(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getExploitMaturities()));
                    break;
                case "cvssv3":
                    filterBuilder.cvssv3(getIntersection(getRowValue(row, column.getValue()), snykIssuesFilter.getCvssv3()));
                    break;
            }
        }
        return filterBuilder.build();
    }

    public SnykIssuesFilter createFilter(DefaultListRequest filter, boolean withPrefix, SnykIssuesFilter.Calculation calculation,
                                         SnykIssuesFilter.Distinct distinct) {
        String prefix = withPrefix ? "snyk_" : "";
        return SnykIssuesFilter.builder()
                .orgs(getListOrDefault(filter, prefix + "orgs"))
                .titles(getListOrDefault(filter, prefix + "titles"))
                .projects(getListOrDefault(filter, prefix + "projects"))
                .types(getListOrDefault(filter, prefix + "types"))
                .severities(getListOrDefault(filter, prefix + "severities"))
                .languages(getListOrDefault(filter, prefix + "languages"))
                .versions(getListOrDefault(filter, prefix + "versions"))
                .packageNames(getListOrDefault(filter, prefix + "package_names"))
                .packageManagers(getListOrDefault(filter, prefix + "package_managers"))
                .exploitMaturities(getListOrDefault(filter, prefix + "exploit_maturities"))
                .cvssv3(getListOrDefault(filter, prefix + "cvssv3"))
                .upgradable(getBooleanOrDefault(filter, prefix + "upgradable"))
                .patchable(getBooleanOrDefault(filter, prefix + "patchable"))
                .pinnable(getBooleanOrDefault(filter, prefix + "pinnable"))
                .ignored(getBooleanOrDefault(filter, prefix + "ignored"))
                .patched(getBooleanOrDefault(filter, prefix + "patched"))
                .integrationIds(getListOrDefault(filter, prefix + "integration_ids"))
                .scoreRange(getScoreRange(filter, prefix + "score_range"))
                .disclosureDateRange(getDateRange(filter, prefix + "disclosure_range"))
                .publicationDateRange(getDateRange(filter, prefix + "publication_range"))
                .calculation(calculation)
                .across(distinct)
                .build();
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

    private List<String> getListOrDefault(DefaultListRequest filter, String key) {
        return filter.<String>getFilterValueAsList(key).orElse(List.of());
    }

    private String getBooleanOrDefault(DefaultListRequest filter, String key) {
        return filter.getFilterValue(key, String.class).orElse(null);
    }

    private Map<String, String> getScoreRange(DefaultListRequest filter, String key) {
        return filter.<String, String>getFilterValueAsMap(key).orElse(Map.of());
    }

    private Map<String, String> getDateRange(DefaultListRequest filter, String key) {
        return filter.<String, String>getFilterValueAsMap(key).orElse(Map.of());
    }
}
