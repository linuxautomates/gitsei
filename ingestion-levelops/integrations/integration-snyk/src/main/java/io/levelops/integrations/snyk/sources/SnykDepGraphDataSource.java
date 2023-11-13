package io.levelops.integrations.snyk.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.snyk.client.SnykClient;
import io.levelops.integrations.snyk.client.SnykClientException;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.models.SnykDepGraph;
import io.levelops.integrations.snyk.models.SnykDepGraphWrapper;
import io.levelops.integrations.snyk.models.SnykIssues;
import io.levelops.integrations.snyk.models.SnykOrg;
import io.levelops.integrations.snyk.models.SnykProject;
import io.levelops.integrations.snyk.models.api.SnykApiListOrgsResponse;
import io.levelops.integrations.snyk.models.api.SnykApiListProjectsResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.snyk.sources.SnykIssueDataSource.*;

@Log4j2
public class SnykDepGraphDataSource implements DataSource<SnykDepGraph, BaseIntegrationQuery> {
    private final SnykClientFactory clientFactory;

    public SnykDepGraphDataSource(SnykClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private SnykDepGraph fetchOneProject(IntegrationKey integrationKey) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Data<SnykDepGraph> fetchOne(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        SnykDepGraph snykDepGraph = fetchOneProject(query.getIntegrationKey());
        return BasicData.of(SnykDepGraph.class, snykDepGraph);
    }

    @Override
    public Stream<Data<SnykDepGraph>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");

        try {
            SnykClient client = clientFactory.get(query.getIntegrationKey());

            // -- loading list of projects by org (so that we don't hold the connection open for too long)
            List<ProjectMetadata> projectMetadataList = ListUtils.emptyIfNull(client.getOrgs().getOrgs()).stream()
                    .flatMap(currentOrg -> {
                        try {
                            SnykApiListProjectsResponse projectsResponse = client.getProjects(currentOrg.getId());
                            return ListUtils.emptyIfNull(projectsResponse.getProjects()).stream()
                                    .map(currentProject -> ProjectMetadata.builder()
                                            .orgId(currentOrg.getId())
                                            .orgName(currentOrg.getName())
                                            .projectId(currentProject.getId())
                                            .projectRemoteRepoUrl(currentProject.getRemoteRepoUrl())
                                            .projectName(currentProject.getName())
                                            .build());
                        } catch (SnykClientException e) {
                            log.warn("Failed to get Snyk projects for orgId={}", currentOrg.getId());
                            return Stream.empty();
                        }
                    })
                    .collect(Collectors.toList());

            // -- lazily stream lists of depGraphs for each project
            return projectMetadataList.stream()
                    .map(projectMetadata -> {
                        SnykDepGraphWrapper wrapper;
                        try {
                            wrapper = client.getDependencyGraph(projectMetadata.getOrgId(), projectMetadata.getProjectId());
                        } catch (SnykClientException e) {
                            log.warn("Failed to get Snyk depGraph for orgId={}, projectId={}", projectMetadata.getOrgId(), projectMetadata.getProjectId());
                            return null;
                        }
                        if (wrapper == null || wrapper.getDepGraph() == null) {
                            return null;
                        }
                        return parseSnykDepGraph(projectMetadata.getOrgId(), projectMetadata.getProjectId(), wrapper.getDepGraph());
                    })
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(SnykDepGraph.class));
        } catch (SnykClientException e) {
            throw new FetchException("Could not fetch Snyk Dep Graph", e);
        }
    }

    public static SnykDepGraph parseSnykDepGraph(final String orgId, final String projectId, final SnykDepGraph apiDepGraph){
        return apiDepGraph.toBuilder()
                .orgId(orgId)
                .projectId(projectId)
                .build();
    }

}
