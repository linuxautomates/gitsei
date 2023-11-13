package io.levelops.api.controllers.blackduck;

import io.levelops.commons.databases.models.config_tables.ConfigTable;
import io.levelops.commons.databases.models.filters.BlackDuckIssueFilter;
import io.levelops.commons.databases.models.filters.BlackDuckProjectFilter;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static io.levelops.commons.databases.models.filters.DefaultListRequestUtils.getTimeRange;

@Service
public class BlackDuckFilterParser {

    public BlackDuckIssueFilter createFilterFromConfig(DefaultListRequest filter,
                                                       ConfigTable.Row row,
                                                       Map<String, ConfigTable.Column> columns,
                                                       BlackDuckIssueFilter.CALCULATION calculation,
                                                       BlackDuckIssueFilter.DISTINCT distinct,
                                                       boolean withPrefix) throws BadRequestException {
        BlackDuckIssueFilter blackDuckIssueFilter = createIssueFilter(filter, withPrefix, calculation, distinct);
        BlackDuckIssueFilter.BlackDuckIssueFilterBuilder blackDuckIssueFilterBuilder =
                blackDuckIssueFilter.toBuilder();
        for (Map.Entry<String, ConfigTable.Column> column : columns.entrySet()) {
            switch (column.getValue().getKey()) {
                case "blackduck_component_name":
                    blackDuckIssueFilterBuilder.componentNames(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getComponentNames()));
                    break;
                case "blackduck_component_version_name":
                    blackDuckIssueFilterBuilder.componentVersionNames(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getComponentVersionNames()));
                    break;
                case "blackduck_vulnerability_name":
                    blackDuckIssueFilterBuilder.vulnerabilities(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getVulnerabilities()));
                    break;
                case "blackduck_severity":
                    blackDuckIssueFilterBuilder.severities(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getSeverities()));
                    break;
                case "blackduck_source":
                    blackDuckIssueFilterBuilder.sources(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getSources()));
                    break;
                case "snyk_remediation_status":
                    blackDuckIssueFilterBuilder.remediationStatuses(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getRemediationStatuses()));
                    break;
                case "package_cwe_id":
                    blackDuckIssueFilterBuilder.cweIds(getIntersection(getRowValue(row, column.getValue()), blackDuckIssueFilter.getCweIds()));
                    break;
            }
        }
        return blackDuckIssueFilterBuilder.build();
    }

    public BlackDuckIssueFilter createIssueFilter(DefaultListRequest filter, boolean withPrefix,
                                                  BlackDuckIssueFilter.CALCULATION calculation,
                                                  BlackDuckIssueFilter.DISTINCT distinct) throws BadRequestException {
        String prefix = withPrefix ? "bd_" : "";
        return BlackDuckIssueFilter.builder()
                .versionIds(getListOrDefault(filter, prefix + "version_ids"))
                .severities(getListOrDefault(filter, prefix + "severities"))
                .componentNames(getListOrDefault(filter, prefix + "component_names"))
                .componentVersionNames(getListOrDefault(filter, prefix + "component_version_names"))
                .bdsaTags(getListOrDefault(filter, prefix + "bdsa_tags"))
                .cweIds(getListOrDefault(filter, prefix + "cwe_ids"))
                .projects(getListOrDefault(filter,prefix+"projects"))
                .versions(getListOrDefault(filter,prefix+"versions"))
                .phases(getListOrDefault(filter,prefix+"phases"))
                .exploitabilitySubScoreRange(getScoreRange(filter, prefix + "exploitability_range"))
                .baseScoreRange(getScoreRange(filter, prefix + "base_range"))
                .impactSubScoreRange(getScoreRange(filter, prefix + "impact_range"))
                .overallScoreRange(getScoreRange(filter, prefix + "overall_range"))
                .remediationStatuses(getListOrDefault(filter, prefix + "remediation_statuses"))
                .remediationCreatedAtRange(getTimeRange(filter, prefix + "remediation_created_at"))
                .vulnerabilities(getListOrDefault(filter, prefix + "vulnerabilities"))
                .sources(getListOrDefault(filter, prefix + "sources"))
                .remediationUpdatedAtRange(getTimeRange(filter, prefix + "remediation_updated_at"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .calculation(calculation)
                .across(distinct)
                .build();
    }

    public BlackDuckProjectFilter createProjectFilter(DefaultListRequest filter, boolean withPrefix,
                                                      BlackDuckProjectFilter.CALCULATION calculation,
                                                      BlackDuckProjectFilter.DISTINCT distinct) throws BadRequestException {
        String prefix = withPrefix ? "bd_" : "";
        return BlackDuckProjectFilter.builder()
                .projects(getListOrDefault(filter, prefix + "projects"))
                .versions(getListOrDefault(filter, prefix + "versions"))
                .phases(getListOrDefault(filter, prefix + "phases"))
                .sources(getListOrDefault(filter, prefix + "sources"))
                .policyStatuses(getListOrDefault(filter, prefix + "policy_statuses"))
                .licenseRiskProfiles(getListOrDefault(filter, prefix + "license_risks"))
                .securityRiskProfiles(getListOrDefault(filter, prefix + "security_risks"))
                .operationalRiskProfiles(getListOrDefault(filter, prefix + "operational_risks"))
                .integrationIds(getListOrDefault(filter, prefix + "integration_ids"))
                .versionReleasedOnRange(getTimeRange(filter, prefix + "version_released_at"))
                .projectCreatedRange(getTimeRange(filter, prefix + "project_created_at"))
                .versionCreatedRange(getTimeRange(filter, prefix + "version_updated_at"))
                .integrationIds(getListOrDefault(filter, "integration_ids"))
                .calculation(calculation)
                .across(distinct)
                .build();
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

    private List<String> getListOrDefault(DefaultListRequest filter, String key) {
        return filter.<String>getFilterValueAsList(key).orElse(List.of());
    }

    private Map<String, String> getScoreRange(DefaultListRequest filter, String key) {
        return filter.<String, String>getFilterValueAsMap(key).orElse(Map.of());
    }
}
