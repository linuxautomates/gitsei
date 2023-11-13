package io.levelops.integrations.github_actions.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.github.models.GithubCommit;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

public class GithubActionsWorkflowRunTest {

    @Test
    public void testDeserializeWorkflowRunPaginatedResponse() throws IOException {
        GithubActionsWorkflowRunPaginatedResponse response = ResourceUtils.getResourceAsObject("integrations/github_actions/workflow_runs.json", GithubActionsWorkflowRunPaginatedResponse.class);

        Assert.assertNotNull(response);
        Assert.assertEquals(5, response.getTotalCount().intValue());
        CollectionUtils.emptyIfNull(response.getWorkflowRuns()).forEach((workflowRun) -> {
            Assert.assertTrue(List.of("CI", "Build and Deploy to GKE", "Link Checker: All English").contains(workflowRun.getName()));
            Assert.assertEquals("krina85", workflowRun.getTriggeringActor().getLogin());
            Assert.assertEquals("Krina-Test-Org/Github-Actions-K", workflowRun.getRepository().getFullName());
            Assert.assertTrue(workflowRun.getId() != null && workflowRun.getRunNumber() != null && workflowRun.getStatus() != null);
        });
    }

    @Test
    public void testDeserialize() throws IOException, ParseException {
        ObjectMapper mapper = DefaultObjectMapper.get();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        GithubActionsWorkflowRunPaginatedResponse expected = mapper.readValue(
                ResourceUtils.getResourceAsString("integrations/github_actions/workflow_runs.json"),
                GithubActionsWorkflowRunPaginatedResponse.class
        );

        GithubActionsWorkflowRunPaginatedResponse actual = GithubActionsWorkflowRunPaginatedResponse.builder()
                .totalCount(5)
                .workflowRuns(List.of(GithubActionsWorkflowRun.builder()
                        .id(5974903536L)
                        .name("CI")
                        .nodeId("WFR_kwLOKDSLxs8AAAABZCHK8A")
                        .headBranch("main")
                        .headSha("cde13b300e367e8d93b982f2255c7738f7a68487")
                        .path(".github/workflows/blank.yml")
                        .displayTitle("CI")
                        .runNumber(3L)
                        .event("workflow_dispatch")
                        .status("completed")
                        .conclusion("success")
                        .workflowId(65452674L)
                        .checkSuiteId(15476540392L)
                        .checkSuiteNodeId("CS_kwDOKDSLxs8AAAADmnlD6A")
                        .pullRequests(List.of())
                        .createdAt(dateFormat.parse("2023-08-25T10:36:48Z"))
                        .updatedAt(dateFormat.parse("2023-08-25T10:37:01Z"))
                        .actor(GithubUser.builder()
                                .login("krina85")
                                .build())
                        .runAttempt(1L)
                        .referencedWorkflows(List.of())
                        .runStartedAt(dateFormat.parse("2023-08-25T10:36:48Z"))
                        .triggeringActor(GithubUser.builder()
                                .login("krina85")
                                .build())
                        .headCommit(GithubCommit.builder()
                                .message("Create Deploygoogle.yml")
                                .timestamp(dateFormat.parse("2023-08-08T09:51:33Z"))
                                .author(GithubUser.builder()
                                        .name("krina85").build())
                                .committer(GithubUser.builder()
                                        .name("GitHub").build())
                                .build())
                        .repository(GithubRepository.builder()
                                .name("Github-Actions-K")
                                .fullName("Krina-Test-Org/Github-Actions-K")
                                .build())
                        .headRepository(GithubRepository.builder()
                                .name("Github-Actions-K")
                                .fullName("Krina-Test-Org/Github-Actions-K")
                                .build())
                        .build()))
                .build();

        Assert.assertEquals(expected, actual);
    }
}
