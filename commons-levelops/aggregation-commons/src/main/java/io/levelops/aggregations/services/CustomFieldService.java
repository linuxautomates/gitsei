package io.levelops.aggregations.services;

import io.levelops.aggregations.models.GenericIssueManagementField;
import io.levelops.commons.databases.issue_management.DbWorkItemField;
import io.levelops.commons.databases.models.database.IntegrationConfig;
import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.services.IntegrationService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
@Service
public class CustomFieldService {
    public static final String AGGS_CUSTOM_FIELDS_KEY = "agg_custom_fields";
    IntegrationService integrationService;
    private final Set<String> mostPopularFields = Set.of(
            "Story Points", "Sprint", "Acceptance Criteria",
            "Epic Link", "Team", "Story point estimate", "Impact",
            "Customer", "Customer Name", "Epic Name", "Category",
            "Organizations", "Severity", "T-Shirt Size", "Flagged",
            "Release (Fix Version)", "Release", "Portfolio Ask", "Estimate",
            "Planned Start Date", "Planned End Date", "Effort", "Value Area",
            "Target Date", "Start Date", "Due Date");

    @Autowired
    public CustomFieldService(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    public void insertPopularJiraFieldsToIntegrationConfig(
            List<DbJiraField> fields, String company, String integrationId) throws SQLException {
        List<GenericIssueManagementField> genericFields = fields.stream()
                .map(GenericIssueManagementField::fromJiraField)
                .collect(Collectors.toList());
        insertPopularFieldsToIntegrationConfig(genericFields, company, integrationId);
    }

    public void insertPopularWorkitemFieldsToIntegrationConfig(
            List<DbWorkItemField> fields, String company, String integrationId) throws SQLException {
        List<GenericIssueManagementField> genericFields = fields.stream()
                .map(GenericIssueManagementField::fromWorkItemField)
                .collect(Collectors.toList());
        insertPopularFieldsToIntegrationConfig(genericFields, company, integrationId);
    }

    /**
     * This function inserts popular issue management fields into the integration config agg_custom_fields map
     *
     * @throws SQLException
     */
    public void insertPopularFieldsToIntegrationConfig(
            List<GenericIssueManagementField> fields, String company, String integrationId) throws SQLException {
        Set<String> popularFieldNames = getMostPopularFields();

        // Check if any of the jiraFields received are popular
        // We could do some kind of fuzzy match here but then we'd pick up on fields with
        // spelling errors that were created by mistake. Keeping this to a case-insensitive exact match
        // for now.
        List<GenericIssueManagementField> filteredPopularFields = fields.stream()
                .filter(field -> popularFieldNames.contains(field.getName().toLowerCase()))
                .collect(Collectors.toList());

        if (!filteredPopularFields.isEmpty()) {
            var integrationConfigResponse = integrationService.listConfigs(company, List.of(integrationId), 0, 1);
            IntegrationConfig integrationConfig = null;
            if (integrationConfigResponse.getCount() > 0) {
                integrationConfig = integrationConfigResponse.getRecords().get(0);
            } else {
                integrationConfig = IntegrationConfig.builder()
                        .config(Map.of())
                        .integrationId(integrationId)
                        .build();
            }
            var config = new HashMap<>(MapUtils.emptyIfNull(integrationConfig.getConfig()));
            var configEntries = new ArrayList<>(config.getOrDefault(AGGS_CUSTOM_FIELDS_KEY, List.of()));
            Set<String> configKeys = configEntries.stream().map(IntegrationConfig.ConfigEntry::getKey).collect(Collectors.toSet());
            List<IntegrationConfig.ConfigEntry> newConfigs = filteredPopularFields.stream()
                    .filter(field -> !configKeys.contains(field.getKey()))
                    .map(field -> IntegrationConfig.ConfigEntry.builder()
                            .key(field.getKey())
                            .name(field.getName())
                            .build())
                    .collect(Collectors.toList());
            log.info("Found {} new popular fields for integration: {}, tenant: {}", newConfigs.size(), integrationId, company);
            configEntries.addAll(newConfigs);
            config.put("agg_custom_fields", configEntries);
            integrationService.insertConfig(company, integrationConfig.toBuilder().config(config).build());
        }
    }


    public final Set<String> getMostPopularFields() {
        return mostPopularFields.stream()
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }
}
