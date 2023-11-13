package io.levelops.integrations.blackduck.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.blackduck.BlackDuckClient;
import io.levelops.integrations.blackduck.BlackDuckClientException;
import io.levelops.integrations.blackduck.BlackDuckClientFactory;
import io.levelops.integrations.blackduck.models.*;
import io.levelops.sources.BlackDuckDataSource;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class BlackDuckDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY)
            .tenantId(EMPTY)
            .build();
    BlackDuckDataSource blackDuckProjectsDataSource;
    BlackDuckClient client;
    Date from = Date.from(Instant.now().minus(Duration.ofDays(2)));
    Date updatedTs = Date.from(Instant.now().minus(Duration.ofDays(1)));
    Date to = Date.from(Instant.now());

    @Before
    public void setup() throws BlackDuckClientException {
        client = Mockito.mock(BlackDuckClient.class);
        BlackDuckClientFactory blackDuckClientFactory = Mockito.mock(BlackDuckClientFactory.class);
        blackDuckProjectsDataSource = new BlackDuckDataSource(blackDuckClientFactory);
        when(blackDuckClientFactory.get(eq(TEST_KEY))).thenReturn(client);
        List<BlackDuckProject> blackDuckProjects = List.of(
                BlackDuckProject.builder()
                .projectName("Sample")
                .blackDuckMetadata(BlackDuckMetadata.builder()
                        .projectHref("https://35.239.185.151/api/projects/d650a186-5b3f-48ef-bbad-2d6fdbcd42d1")
                        .build()).build());
        when(client.getProjects())
                .thenReturn(blackDuckProjects);
        List<BlackDuckVersion> blackDuckVersions = List.of(BlackDuckVersion.builder()
                .source("NVD")
                .distribution("SAMPLE")
                .settingUpdatedAt(updatedTs)
                .phase("SAMPLE")
                .policyStatus("QWERTY")
                .releaseDate(Date.from(Instant.ofEpochMilli(1628658035L)))
                .blackDuckMetadata(BlackDuckMetadata.builder()
                        .projectHref("https://35.239.185.151/api/projects/d650a186-5b3f-48ef-bbad-2d6fdbcd42d1/versions/1b4940fd-c362-4b80-a4db-c0e6d4ab557e")
                        .build()).build());
        when(client.getVersions("d650a186-5b3f-48ef-bbad-2d6fdbcd42d1")).thenReturn(blackDuckVersions);
        List<BlackDuckIssue> blackDuckIssues = List.of(BlackDuckIssue.builder()
                .componentName("Sample component")
                .componentVersionName("0.0.1")
                .componentVersionOriginId("npm")
                .componentVersionOriginName("npm/origin")
                .blackDuckVulnerability(BlackDuckVulnerability.builder()
                        .baseScore("1.0")
                        .impactSubScore("9.0")
                        .severity("SERIOUS")
                        .exploitabilitySubScore("0.8")
                        .build()).build());
        when(client.getIssues("d650a186-5b3f-48ef-bbad-2d6fdbcd42d1", "1b4940fd-c362-4b80-a4db-c0e6d4ab557e", 0))
                .thenReturn(blackDuckIssues);

    }

    @Test
    public void testFetch() throws FetchException {
        List<Data<EnrichedProjectData>> dataList = blackDuckProjectsDataSource.fetchMany(
                BlackDuckIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList()
        );
        DefaultObjectMapper.prettyPrint(dataList);
        Assertions.assertThat(dataList.size()).isEqualTo(1);
        List<EnrichedProjectData> enrichedProjectData = dataList.stream().map(Data::getPayload).collect(Collectors.toList());
        Assertions.assertThat(enrichedProjectData.stream().map(item -> item.getProject().getProjectName()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("Sample");
        Assertions.assertThat(enrichedProjectData.stream().map(item -> item.getVersion().getSource()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("NVD");
        Assertions.assertThat(enrichedProjectData.stream().map( item -> item.getIssues().size()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(1);
        Assertions.assertThat(enrichedProjectData.stream().map( item -> item.getIssues().stream().map(BlackDuckIssue::getComponentName).collect(Collectors.toList())))
                .containsExactlyInAnyOrder(List.of("Sample component"));
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> blackDuckProjectsDataSource.fetchOne(BlackDuckIterativeScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }
}
