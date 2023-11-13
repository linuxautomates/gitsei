package io.levelops.integrations.gitlab.client;

import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.integrations.gitlab.models.*;
import io.levelops.integrations.gitlab.models.GitlabBranch;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GitlabClientIntegrationTest {

    public static final int PAGE = 1;
    public static final int PER_PAGE = 2;
    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "gitlab1";
    private static final String APPLICATION = "gitlab";
    private static final String GITLAB_TOKEN = System.getenv("GITLAB_TOKEN");
    private static final String GITLAB_URL = System.getenv("GITLAB_URL");
    private static final String GITLAB_REFRESH_TOKEN = System.getenv("GITLAB_REFRESH_TOKEN");
    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();
    private static String TEST_TICKET_ID = "1";
    private static String PROJECT_ID = System.getenv("PROJECT_ID");
    private static String PIPELINE_ID = System.getenv("PIPELINE_ID");
    private GitlabClientFactory clientFactory;

    @Before
    public void setup() throws GitlabClientException {
        OkHttpClient client = new OkHttpClient();
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .oauthToken(TENANT_ID, INTEGRATION_ID, APPLICATION, GITLAB_URL, Collections.emptyMap(),
                        GITLAB_TOKEN, GITLAB_REFRESH_TOKEN, null)
                .build());
        clientFactory = GitlabClientFactory.builder()
                .inventoryService(inventoryService)
                .objectMapper(DefaultObjectMapper.get())
                .okHttpClient(client)
                .build();
        Stream<GitlabProject> response = clientFactory.get(TEST_INTEGRATION_KEY, true).streamProjects(true);
    }

    @Test
    public void issues() throws GitlabClientException {
        Date updatedBefore = Date.from(new Date().toInstant());
        Date updatedAfter = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        List<GitlabIssue> response = clientFactory.get(TEST_INTEGRATION_KEY, true).getIssues(updatedBefore,
                updatedAfter, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void commits() throws GitlabClientException {
        Date untilDate = Date.from(new Date().toInstant());
        Date sinceDate = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        List<GitlabCommit> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamProjectCommits(TEST_TICKET_ID, sinceDate, untilDate, PER_PAGE).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void branches() throws GitlabClientException {
        List<GitlabBranch> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getBranches(TEST_TICKET_ID, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void issueStatistics() throws GitlabClientException {
        Date updatedAfter = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        Date updatedBefore = Date.from(new Date().toInstant());
        GitlabStatistics response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getIssueStatistics(updatedAfter, updatedBefore);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void mergeRequests() throws GitlabClientException {
        Date updatedBefore = Date.from(new Date().toInstant());
        Date updatedAfter = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        List<GitlabMergeRequest> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamMergeRequests(TEST_TICKET_ID, updatedAfter, updatedBefore, PER_PAGE)
                .collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void repositories() throws GitlabClientException {
        List<GitlabRepository> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getRepositories(TEST_TICKET_ID, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void projects() throws GitlabClientException {
        List<GitlabProject> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .streamProjects(true).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void users() throws GitlabClientException {
        Date createdAfter = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        Date createdBefore = Date.from(new Date().toInstant());
        List<GitlabUser> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getUsers(TEST_TICKET_ID, createdAfter, createdBefore, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void pipelines() throws GitlabClientException {
        Date updatedAfter = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        Date updatedBefore = Date.from(new Date().toInstant());
        List<GitlabPipeline> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getProjectPipelines(PROJECT_ID, updatedAfter, updatedBefore, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void pipeline() throws GitlabClientException {
        GitlabPipeline response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getProjectPipeline(PROJECT_ID, PIPELINE_ID);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void pipelineVariables() throws GitlabClientException {
        List<GitlabVariable> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getPipelineVariables(PROJECT_ID, PIPELINE_ID);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void pipelineTestReports() throws GitlabClientException {
        GitlabTestReport response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getPipelineTestReport(PROJECT_ID, PIPELINE_ID);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void testGetTags() throws GitlabClientException {
        List<GitlabTag> response = clientFactory.get(TEST_INTEGRATION_KEY, true)
                .getTags(PROJECT_ID, PAGE, PER_PAGE);
        DefaultObjectMapper.prettyPrint(response);
    }

    @Test
    public void testProjectUsers() throws GitlabClientException {
        Date updatedBefore = Date.from(new Date().toInstant());
        Date updatedAfter = Date.from(new Date().toInstant().minus(90, ChronoUnit.DAYS));
        var users = clientFactory.get(TEST_INTEGRATION_KEY, true).streamUsers(PROJECT_ID, updatedBefore, updatedBefore, 10).collect(Collectors.toList());
        System.out.println(users);
    }

    @Test
    public void testUsers() throws GitlabClientException {
        var users = clientFactory.get(TEST_INTEGRATION_KEY, true).streamUsers(null, null, 2).collect(Collectors.toList());
        var usersNow = clientFactory.get(TEST_INTEGRATION_KEY, true).streamUsers(Date.from(Instant.now()), null, 2).collect(Collectors.toList());
        var usersBefore = clientFactory.get(TEST_INTEGRATION_KEY, true).streamUsers(null, Date.from(Instant.now()), 2).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(users);
        DefaultObjectMapper.prettyPrint(usersNow);
        DefaultObjectMapper.prettyPrint(usersBefore);
    }

    @Test
    public void testMRComments() throws GitlabClientException {
        var users = clientFactory.get(TEST_INTEGRATION_KEY, true).streamMRCommentEvents("48383332", "1", 10).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(users);
    }

    @Test
    public void testProjectCount() throws GitlabClientException {
        var count = clientFactory.get(TEST_INTEGRATION_KEY, true).getProjectCount(true);
        System.out.println(count);
    }
}
