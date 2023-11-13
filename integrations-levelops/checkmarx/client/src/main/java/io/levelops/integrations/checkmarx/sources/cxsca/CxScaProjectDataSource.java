package io.levelops.integrations.checkmarx.sources.cxsca;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClient;
import io.levelops.integrations.checkmarx.client.cxsca.CxScaClientFactory;
import io.levelops.integrations.checkmarx.models.CxScaProject;
import io.levelops.integrations.checkmarx.services.cxsca.CxScaProjectEnrichmentService;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class CxScaProjectDataSource implements DataSource<CxScaProject, CxScaProjectDataSource.CxScaProjectQuery> {

    private final CxScaClientFactory clientFactory;
    private final CxScaProjectEnrichmentService enrichmentService;

    public CxScaProjectDataSource(CxScaClientFactory factory, CxScaProjectEnrichmentService enrichmentService) {
        this.clientFactory = factory;
        this.enrichmentService = enrichmentService;
    }

    @Override
    public Data<CxScaProject> fetchOne(CxScaProjectQuery cxScaProjectQuery) throws FetchException {
        throw new UnsupportedOperationException("Fetch one is not supported");
    }

    @Override
    public Stream<Data<CxScaProject>> fetchMany(CxScaProjectQuery cxScaProjectQuery) throws FetchException {
        CxScaClient client = clientFactory.get(cxScaProjectQuery.getIntegrationKey());
        List<CxScaProject> projects = client.getProjects(cxScaProjectQuery.getProjectName());
        projects = enrichProjects(client, projects);
        Stream<Data<CxScaProject>> stream = projects.stream().map(BasicData.mapper(CxScaProject.class));
        return stream.filter(Objects::nonNull);
    }

    public List<CxScaProject> enrichProjects(CxScaClient client, List<CxScaProject> projects) {
        return enrichmentService.enrichProjects(client, projects);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CxScaProjectDataSource.CxScaProjectQuery.CxScaProjectQueryBuilder.class)
    public static class CxScaProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("project_name")
        String projectName;

    }
}
