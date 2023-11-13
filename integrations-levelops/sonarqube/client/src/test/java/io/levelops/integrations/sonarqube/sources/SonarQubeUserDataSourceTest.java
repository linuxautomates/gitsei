package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

public class SonarQubeUserDataSourceTest {
    private static final IntegrationKey TEST_KEY = IntegrationKey.builder().integrationId(EMPTY).tenantId(EMPTY).build();
    private static final String SONARQUBE_USER_GROUP_OWNER = System.getenv("SONARQUBE_USER_GROUP_OWNER");

    SonarQubeUserDataSource dataSource;

    @Before
    public void setup() throws SonarQubeClientException {
        SonarQubeClient sonarQubeClient = Mockito.mock(SonarQubeClient.class);
        SonarQubeClientFactory sonarQubeClientFactory = Mockito.mock(SonarQubeClientFactory.class);
        dataSource = new SonarQubeUserDataSource(sonarQubeClientFactory);
        when(sonarQubeClientFactory.get(TEST_KEY)).thenReturn(sonarQubeClient);
        List<User> users = List.of(
                User.builder().login("test5").lastConnectionDate(Date.from(Instant.now().minus(Duration.ofDays(100)))).build());
        UserResponse userResponse = UserResponse.builder().users(users).build();
        when(sonarQubeClient.getUsers(ArgumentMatchers.eq(SONARQUBE_USER_GROUP_OWNER),ArgumentMatchers.eq(1))).thenReturn(userResponse);
        when(sonarQubeClient.getUsers(ArgumentMatchers.eq(SONARQUBE_USER_GROUP_OWNER),ArgumentMatchers.eq(2)))
                .thenReturn(UserResponse.builder().users(Collections.emptyList()).build());
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
        List<Data<User>> issues = dataSource.fetchMany(
                SonarQubeIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .build()).collect(Collectors.toList());

        assertThat(issues).hasSize(1);
    }
}