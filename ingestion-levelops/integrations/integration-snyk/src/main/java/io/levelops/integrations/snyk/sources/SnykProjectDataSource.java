package io.levelops.integrations.snyk.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.snyk.client.SnykClientException;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.models.SnykProject;
import io.levelops.integrations.snyk.models.api.SnykApiListProjectsResponse;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.stream.Stream;

public class SnykProjectDataSource implements DataSource<SnykProject, SnykProjectDataSource.SnykProjectQuery>  {
    private final SnykClientFactory clientFactory;

    public SnykProjectDataSource(SnykClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private SnykProject fetchOneProject(IntegrationKey integrationKey, String orgId) throws SnykClientException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Data<SnykProject> fetchOne(SnykProjectDataSource.SnykProjectQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notNull(query.getOrgId(), "query.getProjectKey() cannot be null.");
        try {
            SnykProject snykProject = fetchOneProject(query.getIntegrationKey(), query.getOrgId());
            return BasicData.of(SnykProject.class, snykProject);
        } catch (SnykClientException e) {
            throw new FetchException("Could not fetch Snyk projects", e);
        }
    }

    @Override
    public Stream<Data<SnykProject>> fetchMany(SnykProjectDataSource.SnykProjectQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        Validate.notNull(query.getOrgId(), "query.getOrgId() cannot be null.");
        String orgId = query.getOrgId();
        try {
            SnykApiListProjectsResponse projects = clientFactory.get(query.getIntegrationKey()).getProjects(orgId);
            return projects.getProjects().stream()
                    .filter(Objects::nonNull)
                    .map(apiOrg -> map(orgId, apiOrg))
                    .map(BasicData.mapper(SnykProject.class));
        } catch (SnykClientException e) {
            throw new FetchException("Could not fetch Snyk projects", e);
        }
    }

    public static SnykProject map(final String orgId, final SnykProject apiProject){
        SnykProject project = apiProject.toBuilder()
                .orgId(orgId).build();
        return project;
    }

    @Value
    @Builder
    @JsonDeserialize(builder = SnykProjectQuery.SnykProjectQueryBuilder.class)
    public static class SnykProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        /**
         * Needed to fetch one specific project. Both Key and Id work.
         */
        @JsonProperty("org_id")
        String orgId;
    }
}
