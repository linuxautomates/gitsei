package io.levelops.integrations.gitlab.sources;

import io.levelops.commons.functional.IngestionResult;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.gitlab.client.GitlabClient;
import io.levelops.integrations.gitlab.client.GitlabClientException;
import io.levelops.integrations.gitlab.client.GitlabClientFactory;
import io.levelops.integrations.gitlab.models.GitlabGroup;
import io.levelops.integrations.gitlab.models.GitlabQuery;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class GitlabGroupDataSourceTest {
    public static final int PER_PAGE = 20;
    public static final int PAGE = 1;
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY).tenantId(EMPTY).build();
    GitlabGroupDataSource dataSource;

    @Before
    public void setup() throws GitlabClientException {
        GitlabClient client = Mockito.mock(GitlabClient.class);
        GitlabClientFactory clientFactory = Mockito.mock(GitlabClientFactory.class);
        dataSource = new GitlabGroupDataSource(clientFactory);
        when(clientFactory.get(TEST_KEY, false)).thenReturn(client);
        IngestionResult<GitlabGroup> groups = client.getGroups(0, 0);
        when(client.getGroups(PAGE, PER_PAGE))
                .thenReturn(groups);
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(GitlabQuery.builder().integrationKey(TEST_KEY).build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<GitlabGroup>> groups = dataSource.fetchMany(GitlabQuery.builder()
                .integrationKey(TEST_KEY)
                .from(null)
                .to(null)
                .build())
                .collect(Collectors.toList());
        assertThat(groups).hasSize(0);
    }
}
