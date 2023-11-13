package io.levelops.etl.jobs.jira;

import io.levelops.aggregations_shared.database.JobInstanceDatabaseService;
import io.levelops.aggregations_shared.models.JobContext;
import io.levelops.commons.databases.services.JiraProjectService;
import io.levelops.commons.databases.services.jira.JiraIssueVersionService;
import io.levelops.commons.etl.models.JobInstanceId;
import io.levelops.commons.etl.models.JobType;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.jira.models.JiraProject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JiraProjectStageTest {

    @Mock
    JiraProjectService jiraProjectService;
    @Mock
    JiraIssueVersionService jiraIssueVersionService;
    @Mock
    JobInstanceDatabaseService jobInstanceDatabaseService;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testJiraVersionAggregation() throws IOException, SQLException {
        JiraProjectsStage jiraProjectsStage = new JiraProjectsStage(jiraProjectService, jiraIssueVersionService);
        JobInstanceId jobInstanceId = JobInstanceId.builder()
                .jobDefinitionId(UUID.randomUUID())
                .instanceId(1)
                .build();
        JobContext jobContext = JobContext.builder()
                .jobInstanceId(jobInstanceId)
                .tenantId("test")
                .integrationId("1")
                .integrationType("jira")
                .jobScheduledStartTime(new Date())
                .etlProcessorName("-")
                .jobType(JobType.INGESTION_RESULT_PROCESSING_JOB)
                .build();
        jobContext = jobContext.withMetadataAccessors(jobInstanceDatabaseService);
        JiraJobState jobState = new JiraJobState();
        jobState.setJiraProjects(new ArrayList<>());
        JiraProject jiraProject = ResourceUtils.getResourceAsObject("jira_project.json", JiraProject.class);
        jiraProjectsStage.process(jobContext, jobState, "", jiraProject);

        jiraProjectsStage.postStage(jobContext, jobState);
        Assert.assertNotNull(jiraProject.getJiraProjectsIngestedAt());
        verify(jiraIssueVersionService, times(3)).insertJiraVersion(eq(jobContext.getTenantId()), any());
        verify(jiraIssueVersionService, times(3)).getJiraVersion(eq(jobContext.getTenantId()), eq(Integer.parseInt(jobContext.getIntegrationId())), any());
    }
}
