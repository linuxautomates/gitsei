package io.levelops.aggregations.helpers;

import com.cronutils.utils.StringUtils;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.commons.databases.models.database.EventType;
import io.levelops.commons.databases.models.database.IntegrationTracker;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeBranch;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeIssue;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeMeasure;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubeProject;
import io.levelops.commons.databases.models.database.sonarqube.DbSonarQubePullRequest;
import io.levelops.commons.databases.services.IntegrationTrackingService;
import io.levelops.commons.databases.services.SonarQubeIssueService;
import io.levelops.commons.databases.services.SonarQubeProjectService;
import io.levelops.commons.utils.ListUtils;
import io.levelops.events.clients.EventsClient;
import io.levelops.events.models.EventsClientException;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.integrations.sonarqube.models.Issue;
import io.levelops.integrations.sonarqube.models.Project;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Responsible for calling the JobDtoParser instance and passes a list of callables and lambdas that tell function to be
 * performed during Ingestion from GCS. Serves as an intermediary between SonarQubeAggregationsController and JobDtoParser
 */
@Log4j2
@Service
public class SonarQubeAggHelper {

    private static final String ISSUES_DATA_TYPE = "issues";
    private static final String PR_ISSUES_DATA_TYPE = "pr-issues";
    private static final String PR_SCAN = "pr_scan";
    private static final String REPO_SCAN = "repo_scan";

    private final JobDtoParser jobDtoParser;
    private final IntegrationTrackingService trackingService;
    private final SonarQubeProjectService sonarQubeProjectService;
    private final SonarQubeIssueService sonarQubeIssueService;
    private final EventsClient eventsClient;

    @Autowired
    public SonarQubeAggHelper(JobDtoParser jobDtoParser,
                              SonarQubeProjectService sonarQubeProjectService,
                              SonarQubeIssueService sonarQubeIssueService,
                              IntegrationTrackingService trackingService,
                              EventsClient eventsClient) {
        this.jobDtoParser = jobDtoParser;
        this.sonarQubeProjectService = sonarQubeProjectService;
        this.trackingService = trackingService;
        this.eventsClient = eventsClient;
        this.sonarQubeIssueService = sonarQubeIssueService;
    }

    public boolean setupSonarQubeProjectEntities(String customer, String integrationId,
                                                 MultipleTriggerResults results, Date currentTime, String dataType) {
        return jobDtoParser.applyToResults(customer, dataType, Project.class,
                results.getTriggerResults().get(0),
                component -> {
                    try {
                        DbSonarQubeProject dbSonarqubeProject = DbSonarQubeProject.fromComponent(component, integrationId, currentTime);
                        insertProject(customer, dbSonarqubeProject);
                    } catch (Exception e) {
                        log.warn("Could not insert project key='{}'", component.getKey(), e);
                    }
                },
                List.of());
    }

    private void insertProject(String company, DbSonarQubeProject project) throws SQLException {
        String projectId = sonarQubeProjectService.insertProject(company, project);

        List<DbSonarQubeMeasure> measures = new ArrayList<>(DbSonarQubeMeasure.addParentIdToBatch(projectId, project.getMeasures()));

        for (DbSonarQubeBranch branch : ListUtils.emptyIfNull(project.getBranches())) {
            try {
                String branchId = sonarQubeProjectService.insertBranch(company, branch.toBuilder()
                        .projectId(projectId)
                        .build());
                measures.addAll(DbSonarQubeMeasure.addParentIdToBatch(branchId, branch.getMeasures()));
            } catch (Exception e) {
                log.warn("Could not insert branch name='{}' for project key='{}'", branch.getName(), project.getKey(), e);
            }
        }

        for (DbSonarQubePullRequest pr : ListUtils.emptyIfNull(project.getPullRequests())) {
             try {
                 String prId = sonarQubeProjectService.insertPR(company, pr.toBuilder()
                         .projectId(projectId)
                         .build());
                 measures.addAll(DbSonarQubeMeasure.addParentIdToBatch(prId, pr.getMeasures()));
             } catch (Exception e) {
                log.warn("Could not insert PR key='{}' for project key='{}'", pr.getKey(), project.getKey(), e);
             }
        }

        try {
            sonarQubeProjectService.batchInsertAnalyses(company, projectId, project.getAnalyses());
        } catch (Exception e) {
            log.warn("Could not batch insert analyses (count={}) for project key='{}'", CollectionUtils.size(project.getAnalyses()), project.getKey(), e);
        }

        try {
            sonarQubeProjectService.batchInsertMeasures(company, measures);
        } catch (Exception e) {
            log.warn("Could not batch insert measures (count={}) for project key='{}'", CollectionUtils.size(project.getMeasures()), project.getKey(), e);
        }
    }

    public boolean setupSonarQubeIssues(String customer, String integrationId,
                                        MultipleTriggerResults results, Date ingestedAt, boolean isOnboarding) {
        List<DbSonarQubeIssue> newIssues = new ArrayList<>();
        boolean result = jobDtoParser.applyToResults(customer, ISSUES_DATA_TYPE, Issue.class,
                results.getTriggerResults().get(0),
                issue -> {
                    try {
                        DbSonarQubeIssue dbSonarQubeProjectIssue = DbSonarQubeIssue.fromIssue(issue, integrationId, ingestedAt);
                        var id = sonarQubeIssueService.getId(customer, issue.getKey(), issue.getProject(), integrationId);
                        sonarQubeIssueService.insert(customer, dbSonarQubeProjectIssue);
                        if (id.isEmpty()) {
                            newIssues.add(dbSonarQubeProjectIssue);
                        }
                    } catch (Exception e) {
                        log.warn("setupSonarQubeIssues: error inserting issue: " + issue.getKey()
                                + " for integration id: " + integrationId, e);
                    }
                },
                List.of());
        if (result)
            trackingService.upsert(customer,
                    IntegrationTracker.builder()
                            .integrationId(integrationId)
                            .latestIngestedAt(io.levelops.commons.dates.DateUtils.truncate(ingestedAt, Calendar.DATE))
                            .build());
        if (!isOnboarding) {
            newIssues.forEach(issue -> {
                Map<String, Object> eventData = buildEventData(issue, REPO_SCAN);
                try {
                    eventsClient.emitEvent(customer, EventType.SONARQUBE_NEW_ISSUE, eventData);
                } catch (EventsClientException e) {
                    log.error("Error sending event {}, {}, {}", customer, EventType.SONARQUBE_NEW_ISSUE, eventData, e);
                }
            });
        }
        return result;
    }

    public boolean setupSonarQubePRIssues(String customer, String integrationId,
                                          MultipleTriggerResults results, Date ingestedAt, boolean isOnboarding) {
        List<DbSonarQubeIssue> newIssues = new ArrayList<>();
        var result = jobDtoParser.applyToResults(customer, PR_ISSUES_DATA_TYPE, Project.class,
                results.getTriggerResults().get(0),
                project -> {
                    var dbSonarQubePRIssues = CollectionUtils.emptyIfNull(project.getPullRequests()
                            .stream()
                            .flatMap(pullRequest -> CollectionUtils.emptyIfNull(pullRequest.getIssues())
                                    .stream()
                                    .map(issue -> StringUtils.isEmpty(issue.getPullRequest()) ?
                                            issue.toBuilder().pullRequest(pullRequest.getKey()).build() : issue))
                            .map(prIssue -> {
                                DbSonarQubeIssue dbSonarQubeIssue = null;
                                try {
                                    dbSonarQubeIssue = DbSonarQubeIssue.fromIssue(prIssue, integrationId, ingestedAt);
                                } catch (Exception e) {
                                    log.error("Error parsing issue for company " + customer + ", issue " + prIssue.getKey(), e);
                                }
                                return dbSonarQubeIssue;
                            })
                            .collect(Collectors.toList()));
                    dbSonarQubePRIssues.forEach(issue -> {
                        try {
                            var id = sonarQubeIssueService.getId(customer, issue.getKey(), issue.getProject(), integrationId);
                            sonarQubeIssueService.insert(customer, issue);
                            if (id.isEmpty()) {
                                newIssues.add(issue);
                            }
                        } catch (Exception e) {
                            log.warn("setupSonarQubePRIssues: error inserting issue: " + issue.getKey()
                                    + " for integration id: " + issue.getIntegrationId() + " and pull request: "
                                    + issue.getPullRequest(), e);
                        }
                    });
                },
                List.of());
        if (!isOnboarding) {
            newIssues.forEach(issue -> {
                Map<String, Object> eventData = buildEventData(issue, PR_SCAN);
                try {
                    eventsClient.emitEvent(customer, EventType.SONARQUBE_NEW_ISSUE, buildEventData(issue, PR_SCAN));
                } catch (EventsClientException e) {
                    log.error("Error sending event {}, {}, {}", customer, EventType.SONARQUBE_NEW_ISSUE, eventData, e);
                }
            });
        }
        return result;
    }

    public int cleanUpOldData(String company, Long fromTime, Long olderThanSeconds) {
        return sonarQubeProjectService.cleanUpOldData(company, fromTime, olderThanSeconds);
    }

    private Map<String, Object> buildEventData(DbSonarQubeIssue issue, String scanType) {
        return Map.of(
                "id", issue.getKey(),
                "scan_type", scanType,
                "issue_type", issue.getType(),
                "severity", issue.getSeverity(),
                "issue_description", issue.getMessage());
    }

}
