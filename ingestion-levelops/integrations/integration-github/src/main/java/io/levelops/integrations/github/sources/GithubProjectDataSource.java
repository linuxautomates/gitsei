package io.levelops.integrations.github.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.client.GithubClientException;
import io.levelops.integrations.github.client.GithubClientFactory;
import io.levelops.integrations.github.models.GithubProject;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubProjectService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class GithubProjectDataSource implements DataSource<GithubProject, GithubProjectDataSource.GithubProjectQuery> {

    private final GithubProjectService githubProjectService;
    private final GithubOrganizationService organizationService;

    private static final int ONBOARDING_IN_DAYS = 30;

    public GithubProjectDataSource(GithubClientFactory levelOpsClientFactory,
                                   GithubOrganizationService organizationService) {
        githubProjectService = new GithubProjectService(levelOpsClientFactory);
        this.organizationService = organizationService;
    }

    @Override
    public Data<GithubProject> fetchOne(GithubProjectQuery query) {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<GithubProject>> fetchMany(GithubProjectQuery query) throws FetchException {
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        IntegrationKey integrationKey = query.getIntegrationKey();
        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        Instant to = DateUtils.toInstant(query.getTo(), Instant.now());
        boolean includeArchivedCards = BooleanUtils.isTrue(query.getFetchAllCards());
        List<GithubProject> projects;
        Stream<Data<GithubProject>> stream;
        if (CollectionUtils.isNotEmpty(query.getProjects())) {
            stream = query.getProjects().stream()
                    .map(projectId -> {
                        try {
                            return githubProjectService.getProject(integrationKey, projectId, from, to, includeArchivedCards);
                        } catch (GithubClientException e) {
                            log.warn("Failed to get github project {} for integration {}",
                                    projectId, integrationKey, e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(GithubProject.class));
        } else {
            List<String> organizations = organizationService.getOrganizations(integrationKey);
            projects = new ArrayList<>();
            organizations.forEach(org -> {
                try {
                    projects.addAll(githubProjectService.getProjects(integrationKey, org, from, to, includeArchivedCards));
                } catch (GithubClientException e) {
                    log.warn("fetchMany: Failed to fetch projects of Organisation {} for integration {}",
                            org, integrationKey);
                }
            });
            stream = projects.stream().map(BasicData.mapper(GithubProject.class));
        }
        if (query.getLimit() != null) {
            stream = stream.limit(query.getLimit());
        }
        return stream.filter(Objects::nonNull);
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubProjectQuery.GithubProjectQueryBuilder.class)
    public static class GithubProjectQuery implements IntegrationQuery {
        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("projects")
        List<String> projects; // fetch specific projects

        @JsonProperty("limit")
        Integer limit;

        @JsonProperty("from")
        Date from;

        @JsonProperty("to")
        Date to;

        @JsonProperty("fetch_all_cards")
        Boolean fetchAllCards;
    }

}
