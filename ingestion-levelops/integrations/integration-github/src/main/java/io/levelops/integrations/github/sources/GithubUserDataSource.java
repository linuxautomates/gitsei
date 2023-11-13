package io.levelops.integrations.github.sources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.IntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.github.models.GithubRepository;
import io.levelops.integrations.github.models.GithubUser;
import io.levelops.integrations.github.services.GithubOrganizationService;
import io.levelops.integrations.github.services.GithubUserService;
import lombok.Builder;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.stream.Stream;

@Log4j2
public class GithubUserDataSource implements DataSource<GithubUser, GithubUserDataSource.GithubUserQuery> {
    private final GithubOrganizationService organizationService;
    private final GithubUserService githubUserService;

    public GithubUserDataSource(
            GithubOrganizationService organizationService,
            GithubUserService githubUserService) {
        this.organizationService = organizationService;
        this.githubUserService = githubUserService;
    }


    @Override
    public Data<GithubUser> fetchOne(GithubUserQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<GithubUser>> fetchMany(GithubUserQuery query) throws FetchException {
        log.info("Fetching users for integration: {}", query.getIntegrationKey());
        List<String> organizations;
        if (CollectionUtils.isNotEmpty(query.getOrganizations())) {
            organizations = query.getOrganizations();
        } else {
            try {
                organizations = organizationService.getOrganizations(query.getIntegrationKey());
            } catch (Exception e) {
                log.error("Error fetching organizations for integration: {}. Skipping fetching users", query.getIntegrationKey(), e);
                organizations = List.of();
            }
        }
        log.info("Fetching users for organizations: {}", organizations);
        return organizations.stream()
                .flatMap(org -> {
                    try {
                        return githubUserService.streamUsers(query.getIntegrationKey(), org);
                    } catch (Exception e) {
                        log.error("Error fetching users for org: {}", org, e);
                        return Stream.<GithubUser>empty();
                    }
                })
                .map(user -> BasicData.of(GithubUser.class, user));
    }

    @Value
    @Builder(toBuilder = true)
    @JsonDeserialize(builder = GithubProjectDataSource.GithubProjectQuery.GithubProjectQueryBuilder.class)
    public static class GithubUserQuery implements IntegrationQuery {
        @JsonProperty("integration_key")
        IntegrationKey integrationKey;

        @JsonProperty("organizations")
        List<String> organizations; // fetch specific organization's users only
    }
}
