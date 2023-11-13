package io.levelops.integrations.checkmarx.sources.cxsast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClient;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientException;
import io.levelops.integrations.checkmarx.client.cxsast.CxSastClientFactory;
import io.levelops.integrations.checkmarx.models.CxSastProject;
import io.levelops.integrations.checkmarx.models.Link;
import io.levelops.integrations.checkmarx.models.VCSSettings;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class CxSastProjectDataSource implements DataSource<CxSastProject, CxSastProjectDataSource.CxSastProjectQuery> {

    private final CxSastClientFactory clientFactory;

    public CxSastProjectDataSource(CxSastClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    @Override
    public Data<CxSastProject> fetchOne(CxSastProjectQuery cxSastProjectQuery) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<CxSastProject>> fetchMany(CxSastProjectQuery cxSastProjectQuery) throws FetchException {
        CxSastClient client = clientFactory.get(cxSastProjectQuery.getIntegrationKey());
        List<CxSastProject> projects = client.getProjects();
        Stream<Data<CxSastProject>> stream = projects.stream()
                .map(cxSastProject -> {
                    try {
                        return parseAndEnrichProject(client, cxSastProject);
                    } catch (CxSastClientException e) {
                        log.error("Encountered CxSast client error for integration key: "
                                + cxSastProjectQuery.getIntegrationKey() + " as : " + e.getMessage(), e);
                        throw new RuntimeStreamException("Encountered CxSast client error for integration key: " +
                                cxSastProjectQuery.getIntegrationKey(), e);
                    }
                })
                .map(BasicData.mapper(CxSastProject.class));
        return stream.filter(Objects::nonNull);
    }

    private CxSastProject parseAndEnrichProject(CxSastClient client,
                                                CxSastProject project) throws CxSastClientException {
        boolean gitSettings = project.getLinks()
                .stream()
                .map(Link::getType)
                .anyMatch("git"::equals);
        VCSSettings settings = client.getSettings(project.getId(), gitSettings);
        return project.toBuilder()
                .vcsSettings(settings)
                .build();
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = CxSastProjectQuery.CxSastProjectQueryBuilder.class)
    public static class CxSastProjectQuery implements IntegrationQuery {

        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("team_id")
        String teamId;

        @JsonProperty("project_name")
        String projectName;

    }
}
