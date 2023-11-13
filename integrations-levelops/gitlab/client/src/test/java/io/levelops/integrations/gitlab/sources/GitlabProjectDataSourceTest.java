package io.levelops.integrations.gitlab.sources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.controllers.IntermediateStateUpdater;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.models.JobContext;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabCommit;
import io.levelops.integrations.gitlab.models.GitlabProject;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class GitlabProjectDataSourceTest {

    public static final int PER_PAGE = 20;
    public static final int PAGE = 1;
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY).tenantId(EMPTY).build();
    GitlabProjectDataSource dataSource;
    MockWebServer mockWebServer;
    GitlabClientFactory clientFactory;
    OkHttpClient client;
    ObjectMapper objectMapper;
    IntermediateStateUpdater intermediateStateUpdater;

    @Before
    public void setup() throws GitlabClientException, IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
        GitlabClient gitlabClient = Mockito.mock(GitlabClient.class);
        clientFactory = Mockito.mock(GitlabClientFactory.class);
        dataSource = new GitlabProjectDataSource(clientFactory);
        when(clientFactory.get(TEST_KEY, false)).thenReturn(gitlabClient);
        List<GitlabProject> projects = gitlabClient.getProjects(PAGE, PER_PAGE);
        when(gitlabClient.getProjects(PAGE, PER_PAGE))
                .thenReturn(projects);

        intermediateStateUpdater = Mockito.mock(IntermediateStateUpdater.class);
        client = new OkHttpClient();
        objectMapper = DefaultObjectMapper.get();
    }

    @After
    public void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(GitlabProjectDataSource.GitlabProjectQuery
                .builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        JobContext jobContext = JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .integrationId("1")
                .tenantId("test")
                .attemptCount(0)
                .build();
        List<Data<GitlabProject>> projects = dataSource.fetchMany(jobContext, GitlabProjectDataSource.GitlabProjectQuery.builder()
                                .integrationKey(TEST_KEY)
                                .from(null)
                                .to(null)
                                .build(),
                        intermediateStateUpdater)
                .collect(Collectors.toList());
        assertThat(projects).hasSize(0);
    }

    @Test
    public void fetchManyCommits404() throws FetchException, IOException {
        GitlabProjectDataSource commitDatasource = new GitlabProjectDataSource(clientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.COMMITS));
        GitlabClient gitlabClient = new GitlabClient(client, objectMapper, mockWebServer.url("/").toString(), 10, true, false);
        when(clientFactory.get(any(), eq(false))).thenReturn(gitlabClient);

        GitlabProject gitlabProject = GitlabProject.builder()
                .id("1000")
                .build();


        // projects count api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of(gitlabProject))).setResponseCode(200));
        // projects api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of(gitlabProject))).setResponseCode(200));
        // events api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(200));
        // commits api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(404));
        // next projects api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(200));

        JobContext jobContext = JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .integrationId("1")
                .tenantId("test")
                .attemptCount(0)
                .build();
        List<Data<GitlabProject>> projects = commitDatasource.fetchMany(jobContext, GitlabProjectDataSource.GitlabProjectQuery.builder()
                                .integrationKey(TEST_KEY)
                                .from(new Date())
                                .to(new Date())
                                .build(),
                        intermediateStateUpdater)
                .collect(Collectors.toList());
        assertThat(projects).hasSize(0);
    }

    @Test
    public void fetchManyTags404() throws FetchException, IOException {
        fetchMany404Internal(GitlabProjectDataSource.Enrichment.TAGS);
    }

    @Test
    public void fetchManyMergeRequests404() throws FetchException, IOException {
        fetchMany404Internal(GitlabProjectDataSource.Enrichment.MERGE_REQUESTS);
    }

    @Test
    public void fetchManyBranches404() throws FetchException, IOException {
        fetchMany404Internal(GitlabProjectDataSource.Enrichment.BRANCHES);
    }

    @Test
    public void fetchManyMilestones404() throws FetchException, IOException {
        fetchMany404Internal(GitlabProjectDataSource.Enrichment.MILESTONES);
    }

    public void fetchMany404Internal(GitlabProjectDataSource.Enrichment enrichment) throws FetchException, IOException {
        GitlabProjectDataSource commitDatasource = new GitlabProjectDataSource(clientFactory, EnumSet.of(enrichment));
        GitlabClient gitlabClient = new GitlabClient(client, objectMapper, mockWebServer.url("/").toString(), 10, true, false);
        when(clientFactory.get(any(), eq(false))).thenReturn(gitlabClient);

        GitlabProject gitlabProject = GitlabProject.builder()
                .id("1000")
                .build();

        // project count api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(200));
        // projects api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of(gitlabProject))).setResponseCode(200));
        // enrichment api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(404));
        // next projects api call
        mockWebServer.enqueue(new MockResponse().setBody(objectMapper.writeValueAsString(List.of())).setResponseCode(200));
        JobContext jobContext = JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .integrationId("1")
                .tenantId("test")
                .attemptCount(0)
                .build();

        List<Data<GitlabProject>> projects = commitDatasource.fetchMany(
                        jobContext,
                        GitlabProjectDataSource.GitlabProjectQuery.builder()
                                .integrationKey(TEST_KEY)
                                .from(new Date())
                                .to(new Date())
                                .build(),
                        intermediateStateUpdater
                )
                .collect(Collectors.toList());
        assertThat(projects).hasSize(0);
    }

    private static class TestIntermediateStateUpdater implements IntermediateStateUpdater {
        private final List<Map<String, Object>> history;

        private TestIntermediateStateUpdater() {
            this.history = new ArrayList<>();
        }

        public List<Map<String, Object>> getHistory() {
            return history;
        }

        private Map<String ,Object> getLastState() {
            return history.get(history.size() - 1);
        }

        @Override
        public void updateIntermediateState(Map<String, Object> intermediateState) {
            history.add(intermediateState);
        }

        @Override
        public Map<String, Object> getIntermediateState() {
            if (history.isEmpty())
                return null;
            return getLastState();
        }
    }

    private GitlabProject createProjectWithName(String name) {
        return GitlabProject.builder()
                .id(name)
                .name(name)
                .nameWithNamespace(name)
                .build();
    }

    @Test
    public void testIntermediateState() throws FetchException {
        Date from = new Date(1990, 1, 1);
        Date to = new Date(2023, 10, 19);
        TestIntermediateStateUpdater updater = new TestIntermediateStateUpdater();
        GitlabProjectDataSource commitDatasource = new GitlabProjectDataSource(clientFactory, EnumSet.of(GitlabProjectDataSource.Enrichment.COMMITS));
        GitlabClient gitlabClient = Mockito.mock(GitlabClient.class);
        List<GitlabProject> projectsToReturn = List.of(
                createProjectWithName("repo1"),
                createProjectWithName("repo2"),
                createProjectWithName("repo3"),
                createProjectWithName("repo4")
        );
        when(clientFactory.get(any(), eq(false))).thenReturn(gitlabClient);
        when(gitlabClient.getProjectCount(anyBoolean())).thenReturn(4);
        when(gitlabClient.streamProjects(anyBoolean())).thenAnswer( input ->
            projectsToReturn.stream()
        );
        when(gitlabClient.getPushEvents(any(), any(), any(), anyInt())).thenReturn(List.of());
        when(gitlabClient.streamProjectCommits(any(), any(), any(), anyInt())).thenAnswer(input -> Stream.of(
                GitlabCommit.builder()
                        .id("my-id")
                        .authorEmail("steph-the-goat@warriors.com")
                        .committedDate(new Date(2023, 10, 10))
                        .build()
        ));
        JobContext jobContext = JobContext.builder()
                .jobId(UUID.randomUUID().toString())
                .integrationId("1")
                .tenantId("test")
                .attemptCount(0)
                .build();

        List<Data<GitlabProject>> projects = commitDatasource.fetchMany(
                        jobContext,
                        GitlabProjectDataSource.GitlabProjectQuery.builder()
                                .integrationKey(TEST_KEY)
                                .from(from)
                                .to(to)
                                .build(),
                        updater
                )
                .collect(Collectors.toList());
        assertThat(projects).hasSize(4);
        assertThat(updater.getHistory()).hasSize(4);
        System.out.println(updater.getHistory());
    }
}
