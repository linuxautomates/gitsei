package io.levelops.etl.jobs.jira;

import com.google.common.collect.Lists;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.models.database.jira.DbJiraProject;
import io.levelops.commons.databases.models.database.jira.DbJiraVersion;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.jira.JiraIssueVersionService;
import io.levelops.etl.job_framework.BaseIngestionResultProcessingStage;
import io.levelops.integrations.jira.models.JiraProject;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Log4j2
@Service
public class JiraProjectsStage extends BaseIngestionResultProcessingStage<JiraProject, JiraJobState> {
    private final JiraProjectService jiraProjectService;
    private final JiraIssueVersionService jiraIssueVersionService;

    protected JiraProjectsStage(JiraProjectService jiraProjectService,
                                JiraIssueVersionService jiraIssueVersionService) {
        this.jiraProjectService = jiraProjectService;
        this.jiraIssueVersionService = jiraIssueVersionService;
    }

    @Override
    public String getName() {
        return "Jira Project Stage";
    }

    @Override
    public void preStage(JobContext context, JiraJobState jobState) throws SQLException {
        jobState.setJiraProjects(new ArrayList<>());
    }

    @Override
    public void process(JobContext context, JiraJobState jobState, String ingestionJobId, JiraProject entity) throws SQLException {
        DbJiraProject jiraProject = DbJiraProject.fromJiraProject(entity, context.getIntegrationId());
        jobState.getJiraProjects().add(jiraProject);
    }

    @Override
    public void postStage(JobContext context, JiraJobState jobState) {
        List<List<DbJiraProject>> batches = Lists.partition(jobState.getJiraProjects(), 500);
        log.info("{} batches of projects to be upserted", batches.size());
        AtomicInteger index = new AtomicInteger();
        batches.forEach(projects -> {
            try {
                jiraProjectService.batchUpsert(context.getTenantId(), projects);
                projects.forEach(project -> {
                    DbJiraVersion.fromJiraProject(project, context.getIntegrationId())
                            .forEach(version -> {
                                Optional<DbJiraVersion> dbJiraVersion = jiraIssueVersionService.getJiraVersion(context.getTenantId(), Integer.parseInt(context.getIntegrationId()), version.getVersionId());
                                if (!dbJiraVersion.isPresent() || dbJiraVersion.get().getFixVersionUpdatedAt() == null || dbJiraVersion.get().getFixVersionUpdatedAt() < version.getFixVersionUpdatedAt()) {
                                    jiraIssueVersionService.insertJiraVersion(context.getTenantId(), version);
                                }
                            });
                });
                log.info("Persisted batch {} of jira projects", index);
                index.getAndIncrement();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
        log.info("Handled {} Jira projects for tenant {}", jobState.getJiraProjects().size(), context.getTenantId());
    }

    @Override
    public String getDataTypeName() {
        return "projects";
    }

    @Override
    public boolean shouldCheckpointIndividualFiles() {
        return false;
    }

    @Override
    public boolean onlyProcessLatestIngestionJob() {
        return true;
    }
}
