package io.levelops.integrations.gerrit.sources;

import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.client.GerritClientException;
import io.levelops.integrations.gerrit.client.GerritClientFactory;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.GerritQuery;
import io.levelops.integrations.gerrit.models.ProjectInfo;
import io.levelops.integrations.gerrit.models.ReviewerInfo;
import io.levelops.integrations.gerrit.models.RevisionInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class GerritRepositoryDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    GerritRepositoryDataSource reposDataSource;
    GerritRepositoryDataSource reposDataSource2;


    @Before
    public void setup() throws GerritClientException {
        Date after = new Date();
        GerritClient gerritClient = Mockito.mock(GerritClient.class);
        GerritClientFactory gerritClientFactory = Mockito.mock(GerritClientFactory.class);
        reposDataSource = new GerritRepositoryDataSource(gerritClientFactory);
        reposDataSource2 = new GerritRepositoryDataSource(gerritClientFactory, EnumSet.of(GerritRepositoryDataSource.Enrichment.PULL_REQUESTS,
                GerritRepositoryDataSource.Enrichment.REVIEWERS));
        when(gerritClientFactory.get(TEST_KEY)).thenReturn(gerritClient);
        List<ProjectInfo> repos = List.of(
                ProjectInfo.builder().id("project1").build(),
                ProjectInfo.builder().id("project2").build(),
                ProjectInfo.builder().id("project3").build(),
                ProjectInfo.builder().id("project4").build()
        );
        List<ChangeInfo> changes = List.of(
                ChangeInfo.builder().id("change1").currentRevision("1").revisions(Map.of("sampleRevision1",
                        RevisionInfo.builder()
                                .ref("1").build())).build(),
                ChangeInfo.builder().id("change2").currentRevision("2").revisions(Map.of("sampleRevision2",
                        RevisionInfo.builder()
                                .ref("2").build())).build(),
                ChangeInfo.builder().id("change3").currentRevision("3").revisions(Map.of("sampleRevision3",
                        RevisionInfo.builder()
                                .ref("3").build())).build(),
                ChangeInfo.builder().id("change4").currentRevision("4").revisions(Map.of("sampleRevision4",
                        RevisionInfo.builder()
                                .ref("4").build())).build());
        List<ReviewerInfo> reviewerInfos = List.of(ReviewerInfo.builder().accountId("123").displayName("sample").build(),
                ReviewerInfo.builder().accountId("345").displayName("sample2").build());

        when(gerritClient.getChanges(eq(0), eq(50), eq(GerritQuery.builder().after(after).build()))).thenReturn(changes);
        when(gerritClient.getRevisionReviewers(eq("change1"), eq("1"))).thenReturn(reviewerInfos);
        when(gerritClient.getProjects(eq(0), eq(50))).thenReturn(repos);
        when(gerritClient.getAccounts(eq(50), eq(50))).thenReturn(new ArrayList<>());
        when(gerritClient.streamProjects()).thenReturn(
                PaginationUtils.stream(0, 50, offset -> {
                    try {
                        return gerritClient.getProjects(offset, 50);
                    } catch (GerritClientException e) {
                        return null;
                    }
                }));
        when(gerritClient.streamChanges(eq(GerritQuery.builder().after(after).build())))
                .thenReturn(
                        PaginationUtils.stream(0, 50, offset -> {
                            try {
                                return gerritClient.getChanges(0, 50, GerritQuery.builder().after(after).build());
                            } catch (GerritClientException e) {
                                return null;
                            }
                        }));
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> reposDataSource.fetchOne(GerritRepositoryDataSource.GerritRepositoryQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<ProjectInfo>> projects = reposDataSource.fetchMany(
                GerritRepositoryDataSource.GerritRepositoryQuery.builder()
                        .integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());
        assertThat(projects).hasSize(4);
        projects = reposDataSource2.fetchMany(
                GerritRepositoryDataSource.GerritRepositoryQuery.builder()
                        .integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());
        assertThat(projects).hasSize(0);
    }
}
