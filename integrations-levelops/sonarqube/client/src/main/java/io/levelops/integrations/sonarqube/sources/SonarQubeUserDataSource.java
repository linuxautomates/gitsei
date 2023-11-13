package io.levelops.integrations.sonarqube.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.sonarqube.client.SonarQubeClient;
import io.levelops.integrations.sonarqube.client.SonarQubeClientException;
import io.levelops.integrations.sonarqube.client.SonarQubeClientFactory;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import io.levelops.integrations.sonarqube.models.User;
import io.levelops.integrations.sonarqube.models.UserResponse;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sonarqube's implementation of the {@link DataSource}. This class can be used to fetch Users data from Sonarqube.
 */
@Log4j2
public class SonarQubeUserDataSource implements DataSource<User, SonarQubeIterativeScanQuery> {

    private final SonarQubeClientFactory sonarQubeClientFactory;
    private final SonarQubeUserGroupsDataSource sonarQubeUserGroupDataSource;

    public SonarQubeUserDataSource(SonarQubeClientFactory sonarQubeClientFactory) {
        this.sonarQubeClientFactory = sonarQubeClientFactory;
        this.sonarQubeUserGroupDataSource = new SonarQubeUserGroupsDataSource(sonarQubeClientFactory);
    }

    @Override
    public Data<User> fetchOne(SonarQubeIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<User>> fetchMany(SonarQubeIterativeScanQuery query) throws FetchException {
        SonarQubeClient sonarQubeClient = sonarQubeClientFactory.get(query.getIntegrationKey());
        int startPage = 1;
        return sonarQubeUserGroupDataSource.fetchMany(query)
                .filter(Data::isPresent)
                .map(Data::getPayload)
                .flatMap(group -> PaginationUtils.stream(startPage, 1, offset -> getPagedData(sonarQubeClient, group.getName(), offset)))
                .distinct();
    }

    private List<Data<User>> getPagedData(SonarQubeClient sonarQubeClient, String groupName, int offset) {
        try {
            UserResponse userResponse = sonarQubeClient.getUsers(groupName, offset);
            log.info("UserResponse is : {} ", userResponse);
            return userResponse.getUsers().stream()
                    .map(BasicData.mapper(User.class))
                    .collect(Collectors.toList());
        } catch (SonarQubeClientException e) {
            log.warn("Failed to get users after page {}", offset, e);
            throw new RuntimeStreamException("Failed to get users after page=" + offset, e);
        }
    }
}