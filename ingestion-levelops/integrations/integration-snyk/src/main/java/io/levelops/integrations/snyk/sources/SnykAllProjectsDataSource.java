package io.levelops.integrations.snyk.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.snyk.client.SnykClient;
import io.levelops.integrations.snyk.client.SnykClientException;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.models.SnykOrg;
import io.levelops.integrations.snyk.models.SnykProject;
import io.levelops.integrations.snyk.models.api.SnykApiListProjectsResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class SnykAllProjectsDataSource implements DataSource<SnykProject, BaseIntegrationQuery> {
    private final SnykClientFactory clientFactory;

    public SnykAllProjectsDataSource(SnykClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<SnykProject> fetchOne(BaseIntegrationQuery query) throws FetchException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<Data<SnykProject>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            SnykClient client = clientFactory.get(query.getIntegrationKey());
            return ListUtils.emptyIfNull(client.getOrgs().getOrgs()).stream()
                    .map(SnykOrg::getId)
                    .filter(StringUtils::isNotBlank)
                    .flatMap(orgId -> {
                        try {
                            SnykApiListProjectsResponse projects = client.getProjects(orgId);
                            return ListUtils.emptyIfNull(projects.getProjects()).stream()
                                    .filter(Objects::nonNull)
                                    .map(project -> parseProject(orgId, project));
                        } catch (SnykClientException e) {
                            throw new RuntimeStreamException(e);
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(SnykProject.class));
        } catch (RuntimeStreamException | SnykClientException e) {
            throw new FetchException("Could not fetch Snyk projects", e);
        }
    }

    public static SnykProject parseProject(final String orgId, final SnykProject apiProject) {
        return apiProject.toBuilder()
                .orgId(orgId)
                .build();
    }

}
