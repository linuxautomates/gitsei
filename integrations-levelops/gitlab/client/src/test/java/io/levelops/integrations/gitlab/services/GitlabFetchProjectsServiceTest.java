package io.levelops.integrations.gitlab.services;

import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.models.GitlabProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class GitlabFetchProjectsServiceTest {

    @Mock
    GitlabClient gitlabClient;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    private GitlabProject createProjectWithName(String name) {
        return GitlabProject.builder()
                .id(name)
                .name(name)
                .nameWithNamespace(name)
                .build();
    }

    @Test
    public void testResumeFromRepo() {
        GitlabFetchProjectsService gitlabFetchProjectsService = new GitlabFetchProjectsService();
        List<GitlabProject> projectsToReturn = List.of(
                createProjectWithName("repo1"),
                createProjectWithName("repo2"),
                createProjectWithName("repo3"),
                createProjectWithName("repo4"),
                createProjectWithName("repo5"),
                createProjectWithName("repo6")
        );
        when(gitlabClient.streamProjects(false)).thenAnswer(input -> projectsToReturn.stream());
        Stream<GitlabProject> stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, null, null, false, "repo3");
        assertThat(stream).containsExactlyElementsOf(projectsToReturn.stream().skip(2).collect(Collectors.toList()));

        stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, null, null, false, "repo7");
        assertThat(stream).isEmpty();

        stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, null, null, false, null);
        assertThat(stream).containsExactlyElementsOf(projectsToReturn);

        stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, null, null, false, "");
        assertThat(stream).containsExactlyElementsOf(projectsToReturn);
    }

    @Test
    public void testIncludeRepos() {
        GitlabFetchProjectsService gitlabFetchProjectsService = new GitlabFetchProjectsService();
        List<GitlabProject> projectsToReturn = List.of(
                createProjectWithName("repo1"),
                createProjectWithName("repo2"),
                createProjectWithName("repo3"),
                createProjectWithName("repo4"),
                createProjectWithName("repo5"),
                createProjectWithName("repo6")
        );
        when(gitlabClient.streamProjects(false)).thenAnswer(input -> projectsToReturn.stream());
        Stream<GitlabProject> stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, List.of("repo1","repo6","repo1234"), null, false, null);
        assertThat(stream).containsExactly(projectsToReturn.get(0), projectsToReturn.get(5));

        stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, null, null, false, null);
        assertThat(stream).containsExactlyElementsOf(projectsToReturn);
    }

    @Test
    public void testExcludeRepos() {
        GitlabFetchProjectsService gitlabFetchProjectsService = new GitlabFetchProjectsService();
        List<GitlabProject> projectsToReturn = List.of(
                createProjectWithName("repo1"), // 0
                createProjectWithName("repo2"), // 1
                createProjectWithName("repo3"), // 2
                createProjectWithName("repo4"), // 3
                createProjectWithName("repo5"), // 4
                createProjectWithName("repo6")  // 5
        );
        when(gitlabClient.streamProjects(false)).thenAnswer(input -> projectsToReturn.stream());
        Stream<GitlabProject> stream = gitlabFetchProjectsService.getProjectStream(gitlabClient, null, List.of("repo1","repo6","repo1234"), false, null);
        assertThat(stream).containsExactly(projectsToReturn.get(1), projectsToReturn.get(2), projectsToReturn.get(3), projectsToReturn.get(4));
    }

}