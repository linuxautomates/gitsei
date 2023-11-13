package io.levelops.integrations.azureDevops.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.services.IngestionCachingService;
import io.levelops.integrations.azureDevops.client.AzureDevopsClient;
import io.levelops.integrations.azureDevops.client.AzureDevopsClientException;
import io.levelops.integrations.azureDevops.models.Project;
import io.levelops.integrations.azureDevops.models.ProjectResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AzureDevopsProjectProviderServiceTest {

    IntegrationKey integrationKey = new IntegrationKey("test", "1");
    String currentOrg = "org";

    AzureDevopsProjectProviderService azureDevopsProjectProviderService;
    AzureDevopsClient azureDevopsClient;

    @Before
    public void setUp() throws Exception {
        azureDevopsProjectProviderService = new AzureDevopsProjectProviderService();

        Function<String, Project> projectBuilder = id -> Project.builder().id(id).lastUpdateTime("0").build();
        azureDevopsClient = Mockito.mock(AzureDevopsClient.class);
        when(azureDevopsClient.getProjects(eq("org"), eq(""))).thenReturn(ProjectResponse.builder()
                .continuationToken("1")
                .projects(List.of(projectBuilder.apply("1"), projectBuilder.apply("2")))
                .build());
        when(azureDevopsClient.getProjects(eq("org"), eq("1"))).thenReturn(ProjectResponse.builder()
                .continuationToken("2")
                .projects(List.of(projectBuilder.apply("3"), projectBuilder.apply("4")))
                .build());
        when(azureDevopsClient.getProjects(eq("org"), eq("2"))).thenReturn(ProjectResponse.builder()
                .continuationToken("")
                .projects(List.of())
                .build());
    }

    @Test
    public void getRemainingOrgs() {
        assertThat(AzureDevopsProjectProviderService.getRemainingOrgs(List.of("a", "b", "c", "d"), "b")).containsExactly("c", "d");
        assertThat(AzureDevopsProjectProviderService.getRemainingOrgs(List.of("a", "b", "c", "d"), null)).containsExactly("a", "b", "c", "d");
        assertThat(AzureDevopsProjectProviderService.getRemainingOrgs(List.of("a", "b", "c", "d"), "e")).containsExactly("a", "b", "c", "d");
        assertThat(AzureDevopsProjectProviderService.getRemainingOrgs(List.of("a", "b", "c", "d"), "d")).isEmpty();
    }

    @Test
    public void doGetRemainingProjectsInCurrentOrg() {
        List<Project> projects = List.of(
                Project.builder().name("a").build(),
                Project.builder().name("b").build(),
                Project.builder().name("c").build(),
                Project.builder().name("d").build());

        assertThat(AzureDevopsProjectProviderService.doGetRemainingProjectsInCurrentOrg(projects::stream, null, null)).isEmpty();
        assertThat(AzureDevopsProjectProviderService.doGetRemainingProjectsInCurrentOrg(projects::stream, "org", null).map(Project::getName)).containsExactly("a", "b", "c", "d");
        assertThat(AzureDevopsProjectProviderService.doGetRemainingProjectsInCurrentOrg(projects::stream, "org", "b").map(Project::getName)).containsExactly("b", "c", "d");
        assertThat(AzureDevopsProjectProviderService.doGetRemainingProjectsInCurrentOrg(projects::stream, "org", "d").map(Project::getName)).containsExactly("d");
        assertThat(AzureDevopsProjectProviderService.doGetRemainingProjectsInCurrentOrg(projects::stream, "org", "e").map(Project::getName)).isEmpty();
    }

    @Test
    public void streamAllProjectsInOrgCacheDisabled() throws IOException, AzureDevopsClientException {
        IngestionCachingService ingestionCachingService = Mockito.mock(IngestionCachingService.class);
        when(ingestionCachingService.isEnabled()).thenReturn(false);

        Stream<Project> projectStream = azureDevopsProjectProviderService.streamAllProjectsInOrg(ingestionCachingService, azureDevopsClient, integrationKey, currentOrg);
        assertThat(projectStream.map(Project::getId)).containsExactly("1", "2", "3", "4");

        verify(azureDevopsClient, times(3)).getProjects(eq("org"), anyString());
        verify(ingestionCachingService, times(0)).read(anyString(), anyString(), anyString());
        verify(ingestionCachingService, times(0)).write(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void streamAllProjectsInOrgCacheMissTwice() throws IOException, AzureDevopsClientException {
        IngestionCachingService ingestionCachingService = Mockito.mock(IngestionCachingService.class);
        when(ingestionCachingService.isEnabled()).thenReturn(true);

        Stream<Project> projectStream = azureDevopsProjectProviderService.streamAllProjectsInOrg(ingestionCachingService, azureDevopsClient, integrationKey, currentOrg);
        assertThat(projectStream.map(Project::getId)).containsExactly("1", "2", "3", "4");

        verify(azureDevopsClient, times(6)).getProjects(eq("org"), anyString());
        verify(ingestionCachingService, times(2)).read(anyString(), anyString(), anyString());
        verify(ingestionCachingService, times(5)).write(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    public void streamAllProjectsInOrgCached() throws IOException, AzureDevopsClientException {
        IngestionCachingService ingestionCachingService = Mockito.mock(IngestionCachingService.class);
        when(ingestionCachingService.isEnabled()).thenReturn(true);
        when(ingestionCachingService.read(eq("test"), eq("1"), eq("ado_org_org_project_count"))).thenReturn(Optional.of("3"));
        when(ingestionCachingService.read(eq("test"), eq("1"), eq("ado_org_org_project_nb_0")))
                .thenReturn(Optional.of("{\"id\":\"cached1\", \"lastUpdateTime\":\"0\"}"));
        when(ingestionCachingService.read(eq("test"), eq("1"), eq("ado_org_org_project_nb_1")))
                .thenReturn(Optional.of("{\"id\":\"cached2\", \"lastUpdateTime\":\"0\"}"));
        when(ingestionCachingService.read(eq("test"), eq("1"), eq("ado_org_org_project_nb_2")))
                .thenReturn(Optional.of("{\"id\":\"cached3\", \"lastUpdateTime\":\"0\"}"));

        Stream<Project> projectStream = azureDevopsProjectProviderService.streamAllProjectsInOrg(ingestionCachingService, azureDevopsClient, integrationKey, currentOrg);
        assertThat(projectStream.map(Project::getId)).containsExactly("cached1", "cached2", "cached3");

        verify(azureDevopsClient, times(0)).getProjects(eq("org"), anyString());
        verify(ingestionCachingService, times(4)).read(anyString(), anyString(), anyString());
        verify(ingestionCachingService, times(0)).write(anyString(), anyString(), anyString(), anyString());
    }
}