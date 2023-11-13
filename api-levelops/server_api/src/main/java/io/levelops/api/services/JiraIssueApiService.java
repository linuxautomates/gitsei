package io.levelops.api.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.levelops.commons.databases.models.database.SortingOrder;
import io.levelops.commons.databases.models.database.jira.DbJiraIssue;
import io.levelops.commons.databases.models.database.jira.JiraReleaseResponse;
import io.levelops.commons.databases.models.database.velocity.VelocityConfigDTO;
import io.levelops.commons.databases.models.filters.JiraIssuesFilter;
import io.levelops.commons.databases.models.filters.JiraSprintFilter;
import io.levelops.commons.databases.models.organization.OUConfiguration;
import io.levelops.commons.databases.services.jira.JiraIssueReleaseService;
import io.levelops.commons.databases.services.jira.JiraIssueReleaseWidgetService;
import io.levelops.commons.models.DbListResponse;
import io.levelops.commons.models.DefaultListRequest;
import io.levelops.web.exceptions.BadRequestException;
import io.levelops.web.exceptions.NotFoundException;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.levelops.commons.databases.services.jira.conditions.JiraConditionUtils.getStageStatusesMap;
import static io.levelops.commons.databases.services.jira.utils.JiraIssueReadUtils.isReleaseStageExcluded;

@Log4j2
@Service
public class JiraIssueApiService {
    private final JiraIssueReleaseService jiraIssueReleaseService;
    private final JiraIssueReleaseWidgetService jiraIssueReleaseWidgetService;

    public JiraIssueApiService(JiraIssueReleaseService jiraIssueReleaseService, JiraIssueReleaseWidgetService jiraIssueReleaseWidgetService) {
        this.jiraIssueReleaseService = jiraIssueReleaseService;
        this.jiraIssueReleaseWidgetService = jiraIssueReleaseWidgetService;
    }

    public DbListResponse<JiraReleaseResponse> getListOfRelease(String company,
                                                                DefaultListRequest request,
                                                                JiraIssuesFilter finalIssueFilter,
                                                                VelocityConfigDTO velocityConfigDTO,
                                                                Boolean disablePrecalculatedResult) throws SQLException, BadRequestException, NotFoundException, JsonProcessingException {
        verifyReleaseStagePresent(finalIssueFilter, velocityConfigDTO);

        return jiraIssueReleaseWidgetService.jiraReleaseTableReport(
                company, request, disablePrecalculatedResult
        );
    }

    public DbListResponse<DbJiraIssue> getListOfReleaseForDrillDown(String company,
                                                                    DefaultListRequest request,
                                                                    JiraIssuesFilter finalIssueFilter,
                                                                    VelocityConfigDTO velocityConfigDTO,
                                                                    Boolean disablePrecalculatedResult) throws SQLException, NotFoundException, BadRequestException, JsonProcessingException {
        verifyReleaseStagePresent(finalIssueFilter, velocityConfigDTO);

        return jiraIssueReleaseWidgetService.drilldownListReport(
                company, request, disablePrecalculatedResult
        );
    }

    private static void verifyReleaseStagePresent(JiraIssuesFilter finalIssueFilter, VelocityConfigDTO velocityConfigDTO) {
        List<VelocityConfigDTO.Stage> developmentStages = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPreDevelopmentCustomStages())) {
            List<VelocityConfigDTO.Stage> preDevSortedStages = velocityConfigDTO.getPreDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            developmentStages.addAll(preDevSortedStages);
        }
        if (CollectionUtils.isNotEmpty(velocityConfigDTO.getPostDevelopmentCustomStages())) {
            List<VelocityConfigDTO.Stage> postDevSortedStages = velocityConfigDTO.getPostDevelopmentCustomStages()
                    .stream()
                    .sorted(Comparator.comparing(VelocityConfigDTO.Stage::getOrder))
                    .collect(Collectors.toList());
            developmentStages.addAll(postDevSortedStages);
        }

        developmentStages = developmentStages.stream()
                .filter(stage ->
                        stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_STATUS)
                                || stage.getEvent().getType().equals(VelocityConfigDTO.EventType.JIRA_RELEASE)
                )
                .collect(Collectors.toList());

        Map<String, List<String>> velocityStageStatusesMap = getStageStatusesMap(developmentStages);
        if (!isReleaseStageExcluded(velocityStageStatusesMap, finalIssueFilter)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Release Stage require for this widget.");
        }
    }
}
