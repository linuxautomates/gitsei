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
import io.levelops.integrations.sonarqube.models.Group;
import io.levelops.integrations.sonarqube.models.SonarQubeIterativeScanQuery;
import io.levelops.integrations.sonarqube.models.UserGroupResponse;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Sonarqube's implementation of the {@link DataSource}. This class can be used to fetch User-groups data from Sonarqube.
 */
@Log4j2
public class SonarQubeUserGroupsDataSource implements DataSource<Group, SonarQubeIterativeScanQuery> {

    private final SonarQubeClientFactory sonarQubeClientFactory;

    public SonarQubeUserGroupsDataSource(SonarQubeClientFactory sonarQubeClientFactory) {
        this.sonarQubeClientFactory = sonarQubeClientFactory;
    }

    @Override
    public Data<Group> fetchOne(SonarQubeIterativeScanQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<Group>> fetchMany(SonarQubeIterativeScanQuery query) throws FetchException {
        SonarQubeClient sonarQubeClient = sonarQubeClientFactory.get(query.getIntegrationKey());
        int STARTING_PAGE = 1;
        return PaginationUtils.stream(STARTING_PAGE, 1, offset -> getPagedData(sonarQubeClient, offset));
    }

    private List<Data<Group>> getPagedData(SonarQubeClient sonarQubeClient, int offset) {
        try {
            UserGroupResponse userGroupResponse = sonarQubeClient.getUserGroups(offset);
            log.info("UserGroupResponse is : {} ",userGroupResponse);
            return userGroupResponse.getGroups().stream()
                    .map(BasicData.mapper(Group.class))
                    .collect(Collectors.toList());
        } catch (SonarQubeClientException e) {
            log.warn("Failed to get issues after page {}", offset, e);
            throw new RuntimeStreamException("Failed to get issues after page=" + offset, e);
        }
    }
}