package io.levelops.commons.databases.services.dev_productivity.handlers;

import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.TenantSCMSettings;
import io.levelops.commons.databases.models.database.dev_productivity.BreakDownType;
import io.levelops.commons.databases.models.database.dev_productivity.DevProductivityProfile;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureBreakDown;
import io.levelops.commons.databases.models.database.dev_productivity.FeatureResponse;
import io.levelops.commons.databases.models.database.dev_productivity.IntegrationUserDetails;
import io.levelops.commons.databases.models.database.dev_productivity.OrgUserDetails;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.filters.DevProductivityFilter;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.commons.databases.services.WorkItemsService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.utils.MapUtils;
import io.levelops.ingestion.models.IntegrationType;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@SuppressWarnings("unused")
@Component
public class BugsPerNLOCHandler implements DevProductivityFeatureHandler {

    private final JiraIssueService jiraIssueService;
    private final ScmAggService scmAggService;
    private final WorkItemsService workItemsService;
    private String linesOfCode = "100";
    private static final List<String> HEADERS = List.of("Key", "Assignee", "StoryPoints", "Status");

    @Autowired
    public BugsPerNLOCHandler(JiraIssueService jiraIssueService, ScmAggService scmAggService, WorkItemsService workItemsService) {
        this.jiraIssueService = jiraIssueService;
        this.scmAggService = scmAggService;
        this.workItemsService = workItemsService;
    }

    @Override
    public Set<DevProductivityProfile.FeatureType> getSupportedFeatureTypes() {
        return Set.of(DevProductivityProfile.FeatureType.BUGS_PER_HUNDRED_LINES_OF_CODE);
    }

    @Override
    public FeatureResponse calculateFeature(final String company, final Integer sectionOrder, DevProductivityProfile.Feature feature,
                                            Map<String, Object> profileSettings, DevProductivityFilter devProductivityFilter,
                                            OrgUserDetails orgUserDetails, final Map<String, Long> latestIngestedAtByIntegrationId, final TenantSCMSettings tenantSCMSettings) throws SQLException, IOException {

        //Step 2 - Using DevProductivityFilter & OrgUserDetails - create one or more filters for Jira Aggs, WI Aggs, SCM Aggs
        JiraIssuesFilter jiraIssuesFilter = getJiraFilter(feature, orgUserDetails);

        //Step 3 - Do Query
        DbListResponse<DbJiraIssue> aggregationResult = jiraIssueService.list(company, null, jiraIssuesFilter, null, Optional.empty(), null, 0, 100);

        Double mean = aggregationResult.getCount().doubleValue();

        FeatureResponse result = FeatureResponse.constructBuilder(sectionOrder, feature, mean.longValue())
                .count(mean.longValue())
                .mean(mean)
                .build();

        return result;
    }

    @Override
    public FeatureBreakDown getBreakDown(String company, DevProductivityProfile.Feature feature, Map<String, Object> profileSettings,
                                         DevProductivityFilter devProductivityFilter, OrgUserDetails orgUserDetails, Map<String, Long> latestIngestedAtByIntegrationId,
                                         TenantSCMSettings tenantSCMSettings, Map<String, SortingOrder> sortBy, Integer pageNumber, Integer pageSize) throws SQLException, IOException {

        JiraIssuesFilter jiraIssuesFilter = getJiraFilter(feature, orgUserDetails);

        List<DbJiraIssue> dbJiraList = jiraIssueService.list(company, JiraSprintFilter.builder().build(),jiraIssuesFilter,null,sortBy,pageNumber,pageSize).getRecords();

        return FeatureBreakDown.builder()
                .orgUserId(orgUserDetails.getOrgUserId())
                .email(orgUserDetails.getEmail())
                .fullName(orgUserDetails.getFullName())
                .name(feature.getName())
                .description(feature.getDescription())
                .breakDownType(BreakDownType.JIRA_ISSUES)
                .records(dbJiraList)
                .build();
    }

    private JiraIssuesFilter getJiraFilter( DevProductivityProfile.Feature feature, OrgUserDetails orgUserDetails) {

        //Step 1 - Parse feature params
        Map<String, List<String>> params = MapUtils.emptyIfNull(feature.getParams());
        linesOfCode = "100";
        if (params.containsKey("lines_of_code")) {
            linesOfCode = params.get("lines_of_code").get(0);
        }

        return JiraIssuesFilter.builder()
                .assignees(orgUserDetails.getIntegrationUserDetailsList().stream().filter(i -> IntegrationType.JIRA.equals(i.getIntegrationType())).map(IntegrationUserDetails::getIntegrationUserId)
                        .filter(Objects::nonNull).map(UUID::toString).collect(Collectors.toList()))
                .integrationIds(orgUserDetails.getIntegrationUserDetailsList().stream().filter(i -> IntegrationType.JIRA.equals(i.getIntegrationType())).map(i -> i.getIntegrationId()).map(String::valueOf).collect(Collectors.toList()))
                .sprintStates(List.of(linesOfCode)) // using data from params
                .build();
    }
}
