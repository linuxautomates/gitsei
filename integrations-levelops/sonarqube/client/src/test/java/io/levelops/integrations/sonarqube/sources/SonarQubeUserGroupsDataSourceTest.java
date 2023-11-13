package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.Group;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import io.levelops.integrations.sonarqube.models.UserGroupResponse;
import lombok.Builder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;


public class SonarQubeUserGroupsDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();

    SonarQubeUserGroupsDataSource dataSource;

    @Before
    public void setup() throws SonarQubeClientException {
        SonarQubeClient sonarQubeClient = Mockito.mock(SonarQubeClient.class);
        SonarQubeClientFactory sonarQubeClientFactory = Mockito.mock(SonarQubeClientFactory.class);
        dataSource = new SonarQubeUserGroupsDataSource(sonarQubeClientFactory);
        when(sonarQubeClientFactory.get(TEST_KEY)).thenReturn(sonarQubeClient);
        List<Group> groups = List.of(
                Group.builder().uuid("1").build(),
                Group.builder().uuid("2").build(),
                Group.builder().uuid("3").build(),
                Group.builder().uuid("4").build(),
                Group.builder().uuid("5").build());
        UserGroupResponse userGroupResponse = UserGroupResponse.builder().groups(groups).build();
        when(sonarQubeClient.getUserGroups(ArgumentMatchers.eq(1))).thenReturn(userGroupResponse);
        when(sonarQubeClient.getUserGroups(ArgumentMatchers.eq(2)))
                .thenReturn(UserGroupResponse.builder().groups(Collections.emptyList()).build());
    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> dataSource.fetchOne(SonarQubeIterativeScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }

    @Test
    public void fetchMany() throws FetchException {
        List<Data<Group>> groups = dataSource.fetchMany(
                SonarQubeIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());
        DefaultObjectMapper.prettyPrint(groups);
        assertThat(groups).hasSize(5);
    }
}