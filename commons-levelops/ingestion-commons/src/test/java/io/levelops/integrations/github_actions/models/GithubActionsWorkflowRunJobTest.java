package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class GithubActionsWorkflowRunJobTest {

    @Test
    public void testDeserializeWorkflowRunJobPaginatedResponse() throws IOException {
        GithubActionsWorkflowRunJobPaginatedResponse response = ResourceUtils.getResourceAsObject("integrations/github_actions/workflow_run_jobs.json", GithubActionsWorkflowRunJobPaginatedResponse.class);

        Assert.assertNotNull(response);
        Assert.assertEquals(1, response.getTotalCount().intValue());
        CollectionUtils.emptyIfNull(response.getJobs()).forEach((job) -> {
            Assert.assertEquals("build", job.getName());
            Assert.assertEquals("CI", job.getWorkflowName());
            Assert.assertEquals("completed", job.getStatus());
            Assert.assertEquals(1, job.getSteps().size());
        });
    }

    @Test
    public void testDeserialize() throws IOException, ParseException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        GithubActionsWorkflowRunJobPaginatedResponse expected = mapper.readValue(
                ResourceUtils.getResourceAsString("integrations/github_actions/workflow_run_jobs.json"),
                GithubActionsWorkflowRunJobPaginatedResponse.class
        );

        GithubActionsWorkflowRunJobPaginatedResponse actual = GithubActionsWorkflowRunJobPaginatedResponse.builder()
                .totalCount(1)
                .jobs(List.of(GithubActionsWorkflowRunJob.builder()
                        .id(15706953121L)
                        .runId(5795418557L)
                        .workflowName("CI")
                        .headBranch("main")
                        .runAttempt(1L)
                        .nodeId("CR_kwDOKDSLxs8AAAADqDUVoQ")
                        .headSha("cde13b300e367e8d93b982f2255c7738f7a68487")
                        .status("completed")
                        .conclusion("success")
                        .createdAt(dateFormat.parse("2023-08-08T09:51:36Z"))
                        .startedAt(dateFormat.parse("2023-08-08T09:51:44Z"))
                        .completedAt(dateFormat.parse("2023-08-08T09:51:47Z"))
                        .name("build")
                        .steps(List.of(GithubActionsWorkflowRunJobStep.builder()
                                .name("Complete job")
                                .status("completed")
                                .conclusion("success")
                                .number(9L)
                                .build()))
                        .labels(List.of("ubuntu-latest"))
                        .runnerId("2")
                        .runnerName("GitHub Actions 1")
                        .runnerGroupId(2L)
                        .runnerGroupName("GitHub Actions")
                        .build()))
                .build();

        Assert.assertEquals(expected, actual);
    }
}
