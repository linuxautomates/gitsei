package io.levelops.integrations.testrails.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.testrails.client.TestRailsClient;
import io.levelops.integrations.testrails.client.TestRailsClientException;
import io.levelops.integrations.testrails.client.TestRailsClientFactory;
import io.levelops.integrations.testrails.models.Project;
import io.levelops.integrations.testrails.models.TestRailsQuery;
import io.levelops.integrations.testrails.models.User;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * TestRails's implementation of the {@link DataSource}. This class can be used to fetch user data
 * from TestRails.
 */
@Log4j2
public class TestRailsUserDataSource implements DataSource<User, TestRailsQuery> {

    private final TestRailsClientFactory clientFactory;

    /**
     * all arg constructor
     *
     * @param clientFactory {@link TestRailsClientFactory} for fetching the {@link TestRailsClient}
     */
    public TestRailsUserDataSource(TestRailsClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<User> fetchOne(TestRailsQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    /**
     * Fetches the users from the TestRails
     * It makes calls to Zendesk using the {@link TestRailsClient}.
     *
     * @param query {@link TestRailsQuery} used to fetch the users
     * @return {@link Stream<Data<User>>} containing all the fetched users
     * @throws FetchException If any error occurs while fetching the users
     */
    @Override
    public Stream<Data<User>> fetchMany(TestRailsQuery query) throws FetchException {
        TestRailsClient testRailsClient = clientFactory.get(query.getIntegrationKey());
        List<Project> projects = testRailsClient.getProjects()
                .collect(Collectors.toList());
        List<User> users = new ArrayList<>(List.of());
        projects.forEach(project -> {
            try {
                users.addAll(testRailsClient.getUsersByProjectId(project.getId()));
            } catch (TestRailsClientException e) {
                log.warn("Encountered testrails client error during fetching users of project " + project.getId() +
                        " for integration key: " + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                throw new RuntimeStreamException("Encountered testrails client error for integration key: "
                        + query.getIntegrationKey(), e);
            }
        });
        return users.stream().map(BasicData.mapper(User.class));
    }
}
