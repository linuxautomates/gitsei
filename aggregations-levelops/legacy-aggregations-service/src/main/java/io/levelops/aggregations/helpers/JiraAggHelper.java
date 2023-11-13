package io.levelops.aggregations.helpers;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations_shared.helpers.JiraAggHelperService;
import io.levelops.commons.databases.models.database.jira.DbJiraSprint;
import io.levelops.commons.databases.models.database.jira.parsers.JiraIssueParser;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.JiraIssueService;
import io.levelops.commons.dates.DateUtils;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.jira.models.JiraIssue;
import io.levelops.integrations.jira.models.JiraSprint;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Log4j2
@Service
public class JiraAggHelper {
    private static final String DEFAULT_ISSUES_DATATYPE = "issues";
    private static final String DEFAULT_SPRINTS_DATATYPE = "sprints";

    private final JobDtoParser jobDtoParser;
    private final JiraIssueService jiraIssueService;
    private final IntegrationTrackingService trackingService;
    private final JiraAggHelperService jiraAggHelperService;

    @Autowired
    public JiraAggHelper(JiraAggHelperService jiraAggHelperService,
                         JobDtoParser jobDtoParser,
                         JiraIssueService jiraIssueService,
                         IntegrationTrackingService trackingService) {
        this.jiraAggHelperService = jiraAggHelperService;
        this.jobDtoParser = jobDtoParser;
        this.trackingService = trackingService;
        this.jiraIssueService = jiraIssueService;
    }

    public boolean setupJiraSprints(String customer,
                                    String integrationId,
                                    MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(
                customer,
                DEFAULT_SPRINTS_DATATYPE,
                JiraSprint.class,
                results.getTriggerResults().get(0),
                (sprint, jobDTO) -> jiraAggHelperService.processJiraSprint(customer, integrationId, sprint, jobDTO.getCreatedAt()),
                List.of());
    }

    public boolean setupJiraIssues(String customer,
                                   Date currentTime,
                                   String integrationId,
                                   JiraIssueParser.JiraParserConfig parserConfig,
                                   MultipleTriggerResults results,
                                   List<String> productIds,
                                   Boolean isEnableToMaskUser) {
        log.info("currentTime {}", currentTime);
        Map<Integer, Long> sprintIdCache = new HashMap<>();
        LoadingCache<String, Optional<String>> statusIdToStatusCategoryCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(statusId -> jiraAggHelperService.findStatusCategoryByStatusId(customer, integrationId, statusId)));
        LoadingCache<String, Optional<DbJiraSprint>> dbSprintLoadingCache = CacheBuilder.from("maximumSize=250")
                .build(CacheLoader.from(sprintId -> jiraAggHelperService.findDbSprintById(customer, integrationId, sprintId)));
        LoadingCache<String, Optional<String>> userIdByDisplayNameCache = CacheBuilder.from("maximumSize=100000")
                .build(CacheLoader.from(displayName -> jiraAggHelperService.getUserIdByDisplayName(customer, integrationId, displayName)));
        LoadingCache<String, Optional<String>> userIdByCloudIdCache = CacheBuilder.from("maximumSize=100000")
                .build(CacheLoader.from(cloudId -> jiraAggHelperService.getUserIdByCloudId(customer, integrationId, cloudId)));
        boolean result = jobDtoParser.applyToResults(
                customer,
                DEFAULT_ISSUES_DATATYPE,
                JiraIssue.class,
                results.getTriggerResults().get(0),
                (issue, jobDTO) -> jiraAggHelperService.processJiraIssue(customer,
                        integrationId,
                        jobDTO,
                        issue,
                        currentTime,
                        parserConfig,
                        null,
                        false,
                        productIds,
                        sprintIdCache,
                        dbSprintLoadingCache,
                        statusIdToStatusCategoryCache,
                        userIdByDisplayNameCache,
                        userIdByCloudIdCache,
                        isEnableToMaskUser),
                List.of());
        if (!result) {
            return false;
        }

        // -- story points
        try {
            jiraIssueService.bulkUpdateEpicStoryPoints(customer, integrationId,
                    DateUtils.truncate(currentTime, Calendar.DATE));
        } catch (SQLException e) {
            log.warn("Failed to update story points for parents.", e);
        }

        // -- update the integration tracker
        Long ingestedAt = DateUtils.truncate(currentTime, Calendar.DATE);
        try {
            trackingService.upsertJiraWIDBAggregatedAt(customer, Integer.parseInt(integrationId), ingestedAt);
        } catch (SQLException e) {
            log.error("Error upserting latest_aggregated_at!, company {}, integrationId {}, ingestedAt {}", customer, integrationId, ingestedAt, e);
        }
        return true;
    }
}
