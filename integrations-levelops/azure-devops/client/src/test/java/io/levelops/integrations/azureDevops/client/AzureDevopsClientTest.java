package io.levelops.integrations.azureDevops.client;

import io.levelops.commons.databases.models.database.Integration;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureDevopsClientTest {

    @Test
    public void name() {
        Integration integ = Integration.builder()
                .metadata(Map.of(
                        "organization", "a",
                        "organizations", "b"))
                .build();
        assertThat(AzureDevopsClient.getOrganizations(integ.getMetadata())).containsExactlyInAnyOrder("a", "b");

        integ = Integration.builder()
                .metadata(Map.of(
                        "organization", List.of("a", "b"),
                        "organizations", List.of("c", "d")))
                .build();
        assertThat(AzureDevopsClient.getOrganizations(integ.getMetadata())).containsExactlyInAnyOrder("a", "b", "c", "d");

        integ = Integration.builder()
                .metadata(Map.of(
                        "organization", "a",
                        "organizations", " b "))
                .build();
        assertThat(AzureDevopsClient.getOrganizations(integ.getMetadata())).containsExactlyInAnyOrder("a", "b");
        integ = Integration.builder()
                .metadata(Map.of(
                        "organization", "a",
                        "organizations", " b , c    ,d"))
                .build();
        assertThat(AzureDevopsClient.getOrganizations(integ.getMetadata())).containsExactlyInAnyOrder("a", "b", "c", "d");

        integ = Integration.builder()
                .metadata(Map.of(
                        "organization", "",
                        "organizations", "        "))
                .build();
        assertThat(AzureDevopsClient.getOrganizations(integ.getMetadata())).isEmpty();
    }

    @Test
    public void testProjectsMetadata() throws IOException {
        Integration integ = Integration.builder()
                .metadata(Map.of(
                        "projects", " org2/proj2 , org2/proj3    ,org4/proj4",
                        "organizations", "cgn-test"))
                .build();
        DefaultObjectMapper.prettyPrint(integ);
        assertThat(AzureDevopsClient.getQualifiedProjects(integ.getMetadata())).containsExactlyInAnyOrder("org2/proj2", "org2/proj3", "org4/proj4");

        integ = Integration.builder()
                .metadata(Map.of(
                        "project", "org1/proj1      ",
                        "projects", "        org2/proj2 ,       org2/proj3    ,org4/proj4"))
                .build();
        assertThat(AzureDevopsClient.getQualifiedProjects(integ.getMetadata())).containsExactlyInAnyOrder("org2/proj2", "org2/proj3", "org4/proj4");


        integ = Integration.builder()
                .metadata(Map.of(
                        "project", "",
                        "projects", "         "))
                .build();
        assertThat(AzureDevopsClient.getQualifiedProjects(integ.getMetadata())).isEmpty();
    }

}