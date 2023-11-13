package io.levelops.aggregations.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.aggregations.parsers.JobDtoParser;
import io.levelops.aggregations_shared.helpers.GithubAggHelperService;
import io.levelops.aggregations_shared.models.IntegrationWhitelistEntry;
import io.levelops.aggregations_shared.services.AutomationRulesEngine;
import io.levelops.commons.databases.services.GithubAggService;
import io.levelops.commons.databases.services.ScmAggService;
import io.levelops.events.clients.EventsClient;
import io.levelops.ingestion.models.controlplane.MultipleTriggerResults;
import io.levelops.ingestion.models.controlplane.TriggerResults;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.models.GithubRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Log4j2
@Service
@SuppressWarnings("unused")
public class GithubAggHelper {
    private static final String PULL_REQUEST_DATATYPE = "pull_requests";
    private static final String COMMITS_DATATYPE = "commits";
    private static final String ISSUES_DATATYPE = "issues";
    private static final String PROJECTS_DATATYPE = "projects";
    private static final String TAGS_DATATYPE = "tags";
    public static final int TOTAL_LINES_OF_CHANGE = 5000;
    private static final Set<String> RELEVANT_TENANT_IDS = Set.of("broadcom");

    private final JobDtoParser jobDtoParser;
    private final ScmAggService scmAggService;
    private final GithubAggService githubAggService;
    private final EventsClient eventsClient;
    private final ObjectMapper mapper;
    private final AutomationRulesEngine automationRulesEngine;
    private final GithubAggHelperService githubAggHelperService;

    @Autowired
    public GithubAggHelper(JobDtoParser jobDtoParser, ScmAggService aggService, GithubAggService githubAggService,
                           EventsClient eventsClient, ObjectMapper mapper, AutomationRulesEngine automationRulesEngine, GithubAggHelperService githubAggHelperService) {
        this.jobDtoParser = jobDtoParser;
        this.scmAggService = aggService;
        this.githubAggService = githubAggService;
        this.eventsClient = eventsClient;
        this.mapper = mapper;
        this.automationRulesEngine = automationRulesEngine;
        this.githubAggHelperService = githubAggHelperService;
    }

    public boolean processGitCommits(String customer,
                                     String integrationId,
                                     MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                COMMITS_DATATYPE,
                GithubRepository.class,
                results.getTriggerResults().get(0), //theres only one trigger result today.
                repository -> githubAggHelperService.processRepositoryCommits(repository, customer, integrationId),
                List.of()); //do delete of old data in the function?
    }

    public boolean processGitPrs(String customer,
                                 String integrationId,
                                 MultipleTriggerResults results, List<String> productIds) {
        TriggerResults triggerResults = results.getTriggerResults().get(0); //theres only one trigger result today.
        return jobDtoParser.applyToResults(customer,
                PULL_REQUEST_DATATYPE,
                GithubRepository.class,
                triggerResults,
                (repository, jobDTO) -> {
                    githubAggHelperService.processRepositoryPrs(repository, customer, integrationId, jobDTO, productIds);
                },
                List.of());//do delete of old data in the function?
    }

    public boolean updateGitCommitsForDirectMerge(String customer,
                                                  String integrationId,
                                                  MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                COMMITS_DATATYPE,
                GithubRepository.class,
                results.getTriggerResults().get(0),
                repository -> githubAggHelperService.updateRepositoryCommitsForDirectMerge(repository, customer, integrationId),
                List.of());
    }

    public boolean insertGitIssues(String customer,
                                   String integrationId,
                                   MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                ISSUES_DATATYPE,
                GithubRepository.class,
                results.getTriggerResults().get(0), //theres only one trigger result today.
                repository -> githubAggHelperService.insertRepositoryIssues(repository, customer, integrationId),
                List.of());//do delete of old data in the function?
    }

    public boolean insertGitTags(String customer,
                                 String integrationId,
                                 MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                TAGS_DATATYPE,
                GithubRepository.class,
                results.getTriggerResults().get(0),
                repository -> githubAggHelperService.insertRepositoryTags(repository, customer, integrationId),
                List.of());
    }

    public boolean processGitProjects(String customer,
                                      String integrationId,
                                      MultipleTriggerResults results) {
        return jobDtoParser.applyToResults(customer,
                PROJECTS_DATATYPE,
                GithubProject.class,
                results.getTriggerResults().get(0),
                project -> githubAggHelperService.processGitProject(customer, integrationId, project),
                List.of());
    }

    public void linkIssuesAndProjectCards(String customer, String integrationId) {
        githubAggHelperService.linkIssuesAndProjectCards(customer, integrationId);
    }
}
