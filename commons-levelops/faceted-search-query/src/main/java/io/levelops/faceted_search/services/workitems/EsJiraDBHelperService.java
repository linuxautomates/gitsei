package io.levelops.faceted_search.services.workitems;

import io.levelops.commons.databases.models.database.jira.DbJiraField;
import io.levelops.commons.databases.models.database.jira.DbJiraStatusMetadata;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.JiraFieldService;
import io.levelops.commons.databases.services.JiraStatusMetadataDatabaseService;
import io.levelops.commons.databases.services.organization.OrgUsersDatabaseService;
import io.levelops.commons.helper.organization.OrgUnitHelper;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
public class EsJiraDBHelperService {
    private final JiraFieldService jiraFieldService;
    private final OrgUsersDatabaseService orgUsersDatabaseService;
    private final JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService;

    @Autowired
    public EsJiraDBHelperService(JiraFieldService jiraFieldService, OrgUsersDatabaseService orgUsersDatabaseService, JiraStatusMetadataDatabaseService jiraStatusMetadataDatabaseService) {
        this.jiraFieldService = jiraFieldService;
        this.orgUsersDatabaseService = orgUsersDatabaseService;
        this.jiraStatusMetadataDatabaseService = jiraStatusMetadataDatabaseService;
    }

    public List<DbJiraField> getDbJiraFields(String company, Long ingestedAt, List<String> integrationIds) {
        List<DbJiraField> dbJiraFields = new ArrayList<>();
        try {
            dbJiraFields = jiraFieldService.listByFilter(company,
                    integrationIds, true,
                    null, null, null, 0, Integer.MAX_VALUE).getRecords();
        } catch (SQLException e) {
            log.error("Error for company: " + company + " , ingestedAt : " + ingestedAt +
                    "while getting jiraFields : " + e.getMessage());
        }
        return dbJiraFields;
    }

    public JiraIssuesFilter getIntegrationUserIds(String company, OUConfiguration ouConfig, Boolean ignoreOU, JiraIssuesFilter jiraIssuesFilter) {
        if (OrgUnitHelper.isOuConfigActive(ouConfig) && BooleanUtils.isFalse(ignoreOU)) {
            log.info("getIntegrationUserIds fetching ou users");
            boolean reportersNeeded = ouConfig.getJiraFields().contains("reporter") && OrgUnitHelper.doesOUConfigHaveJiraReporters(ouConfig);
            boolean assigneesNeeded = ouConfig.getJiraFields().contains("assignee") && OrgUnitHelper.doesOUConfigHaveJiraAssignees(ouConfig);
            boolean firstAssigneesNeeded = ouConfig.getJiraFields().contains("first_assignee") && OrgUnitHelper.doesOUConfigHaveJiraFirstAssignees(ouConfig);
            log.info("getIntegrationUserIds  reportersNeeded = {}, assigneesNeeded = {}, firstAssigneesNeeded = {}", reportersNeeded, assigneesNeeded, firstAssigneesNeeded);
            List<String> users = null;
            if (reportersNeeded || assigneesNeeded || firstAssigneesNeeded) {
                users = orgUsersDatabaseService.getOuUsersDisplayNames(company, ouConfig, IntegrationType.JIRA);
            }
            log.info("getIntegrationUserIds users.size() = {}", CollectionUtils.size(users));

            if (reportersNeeded) {
                jiraIssuesFilter = jiraIssuesFilter.toBuilder()
                        .reporterDisplayNames(users)
                        .build();
            }
            if (assigneesNeeded) {
                jiraIssuesFilter = jiraIssuesFilter.toBuilder()
                        .assigneeDisplayNames(users)
                        .build();
            }
            if (firstAssigneesNeeded) {
                jiraIssuesFilter = jiraIssuesFilter.toBuilder()
                        .firstAssigneeDisplayNames(users)
                        .build();
            }
        }
        return jiraIssuesFilter;
    }

    public List<DbJiraStatusMetadata.IntegStatusCategoryMetadata> getIntegStatusCategoryMetadata(String company, JiraIssuesFilter filter) {
        return jiraStatusMetadataDatabaseService.getIntegStatusCategoryMetadata(company, filter.getIntegrationIds());
    }

}
