package io.levelops.integrations.jira.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.jira.client.JiraClient;
import io.levelops.integrations.jira.client.JiraClientFactory;
import io.levelops.integrations.jira.models.JiraPriority;
import io.levelops.integrations.jira.models.JiraPriorityScheme;
import io.levelops.integrations.jira.models.JiraProject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class JiraProjectDataSourceTest {

    IntegrationKey KEY = IntegrationKey.builder().integrationId("").build();

    JiraProjectDataSource jiraProjectDataSource;

    @Before
    public void setUp() throws Exception {
        JiraClient jiraClient = Mockito.mock(JiraClient.class);
        JiraClientFactory factory = Mockito.mock(JiraClientFactory.class);
        when(factory.get(eq(KEY))).thenReturn(jiraClient);
        jiraProjectDataSource = new JiraProjectDataSource(factory);

        when(jiraClient.getProjects()).thenReturn(List.of(JiraProject.builder()
                .key("LEV")
                .build()));
        when(jiraClient.getProject(eq("LEV"))).thenReturn(JiraProject.builder()
                .key("LEV")
                .description("salut")
                .build());
        when(jiraClient.getPriorities()).thenReturn(getPriorities());
        when(jiraClient.getPrioritySchemes("LEV")).thenReturn(JiraPriorityScheme.builder()
                .name("Priority Scheme")
                .priorities(getPriorities()).build());
    }

    @Test
    public void fetchOne() throws FetchException {
        Data<JiraProject> projectData = jiraProjectDataSource.fetchOne(JiraProjectDataSource.JiraProjectQuery.builder()
                .integrationKey(KEY)
                .projectKey("LEV")
                .build());
        assertThat(projectData.getPayload().getDescription()).isEqualTo("salut");
        testPriorityAndSchemes(projectData);
    }

    private void testPriorityAndSchemes(Data<JiraProject> projectData) {
        assertThat(projectData.getPayload().getDefaultPriorities()).isEqualTo(List.of(JiraPriority.builder()
                        .name("HIGH")
                        .description("Important")
                        .priorityOrder(1)
                        .build(),
                JiraPriority.builder()
                        .name("LOW")
                        .description("Might be important")
                        .priorityOrder(2)
                        .build()));
        assertThat(projectData.getPayload().getPriorityScheme().getName()).isEqualTo("Priority Scheme");
        List<JiraPriority> priorities = projectData.getPayload().getPriorityScheme().getPriorities();
        assertThat(projectData.getPayload().getDefaultPriorities()).isEqualTo(priorities);
        int i = 1;
        for (JiraPriority priority : priorities) {
            assertThat(priority.getPriorityOrder()).isEqualTo(i++);
        }
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<JiraProject>> projectData = jiraProjectDataSource.fetchMany(JiraProjectDataSource.JiraProjectQuery.builder()
                .integrationKey(KEY)
                .build())
                .collect(Collectors.toList());
        assertThat(projectData).hasSize(1);
        assertThat(projectData.get(0).getPayload().getDescription()).isEqualTo("salut");
    }

    public List<JiraPriority> getPriorities() {
        return List.of(
                JiraPriority.builder()
                        .name("HIGH")
                        .description("Important")
                        .priorityOrder(1)
                        .build(),
                JiraPriority.builder()
                        .name("LOW")
                        .description("Might be important")
                        .priorityOrder(2)
                        .build());
    }
}