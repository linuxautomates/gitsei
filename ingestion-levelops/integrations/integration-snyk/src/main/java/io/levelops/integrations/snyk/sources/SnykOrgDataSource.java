package io.levelops.integrations.snyk.sources;

import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.controllers.generic.BaseIntegrationQuery;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.snyk.client.SnykClientException;
import io.levelops.integrations.snyk.client.SnykClientFactory;
import io.levelops.integrations.snyk.models.SnykOrg;
import io.levelops.integrations.snyk.models.api.SnykApiListOrgsResponse;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.Validate;

import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class SnykOrgDataSource implements DataSource<SnykOrg, BaseIntegrationQuery> {
    private final SnykClientFactory clientFactory;

    public SnykOrgDataSource(SnykClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    private SnykOrg fetchOneProject(IntegrationKey integrationKey) throws SnykClientException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Data<SnykOrg> fetchOne(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            SnykOrg snykOrg = fetchOneProject(query.getIntegrationKey());
            return BasicData.of(SnykOrg.class, snykOrg);
        } catch (SnykClientException e) {
            throw new FetchException("Could not fetch Snyk Orgs", e);
        }
    }

    @Override
    public Stream<Data<SnykOrg>> fetchMany(BaseIntegrationQuery query) throws FetchException {
        Validate.notNull(query, "query cannot be null.");
        Validate.notNull(query.getIntegrationKey(), "query.getIntegrationKey() cannot be null.");
        try {
            SnykApiListOrgsResponse orgs = clientFactory.get(query.getIntegrationKey()).getOrgs();
            return orgs.getOrgs().stream()
                    .filter(Objects::nonNull)
                    .map(BasicData.mapper(SnykOrg.class));
        } catch (SnykClientException e) {
            throw new FetchException("Could not fetch Snyk Orgs", e);
        }
    }
}
