package io.levelops.integrations.testrails.source;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.models.User;
import io.levelops.integrations.testrails.sources.TestRailsUserDataSource;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@Ignore
public class TestRailsUserDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    TestRailsUserDataSource dataSource;

    @Before
    public void setup() throws TestRailsClientException {
        TestRailsClient client = Mockito.mock(TestRailsClient.class);
        TestRailsClientFactory clientFactory = Mockito.mock(TestRailsClientFactory.class);

        dataSource = new TestRailsUserDataSource(clientFactory);
        when(clientFactory.get(TEST_KEY)).thenReturn(client);
        when(client.getProjects())
                .thenReturn(Stream.of(Project.builder()
                        .id(1)
                        .milestones(Collections.emptyList())
                        .build()));
        List<User> users = List.of(User.builder().id(1).build());
        when(client.getUsersByProjectId(anyInt())).thenReturn(users);
    }

    @Ignore
    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(TestRailsQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Ignore
    @Test
    public void fetchMany() throws FetchException {
        List<Data<User>> users = dataSource.fetchMany(TestRailsQuery.builder()
                .integrationKey(TEST_KEY)
                .build())
                .collect(Collectors.toList());
        assertThat(users).hasSize(1);
    }
}
