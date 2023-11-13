package io.levelops.integrations.gerrit.sources;

import io.levelops.commons.exceptions.RuntimeStreamException;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.client.GerritClientException;
import io.levelops.integrations.gerrit.client.GerritClientFactory;
import io.levelops.integrations.gerrit.models.AccountInfo;
import io.levelops.integrations.gerrit.models.GerritQuery;
import lombok.extern.log4j.Log4j2;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gerrit's implementation of the {@link DataSource}. This class can be used to fetch data from Gerrit.
 */
@Log4j2
public class GerritAccountsDataSource implements DataSource<AccountInfo, GerritQuery> {

    private final GerritClientFactory gerritClientFactory;
    private final Integer PAGE_LIMIT = 50;

    /**
     * all arg constructor
     *
     * @param gerritClientFactory {@link GerritClientFactory} for fetching the {@link GerritClient}
     */
    public GerritAccountsDataSource(GerritClientFactory gerritClientFactory) {
        this.gerritClientFactory = gerritClientFactory;
    }

    @Override
    public Data<AccountInfo> fetchOne(GerritQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<AccountInfo>> fetchMany(GerritQuery query) throws FetchException {
        GerritClient gerritClient = gerritClientFactory.get(query.getIntegrationKey());
        return PaginationUtils.stream(0, PAGE_LIMIT, offset -> {
            try {
                return getAccounts(gerritClient, offset, PAGE_LIMIT);
            } catch (GerritClientException e) {
                log.error("Encountered gerrit client error for integration key: "
                        + query.getIntegrationKey() + " as : " + e.getMessage(), e);
                throw new RuntimeStreamException("Encountered gerrit client error for integration key: " + query.getIntegrationKey(), e);
            }
        });
    }

    private List<Data<AccountInfo>> getAccounts(GerritClient gerritClient, Integer offset,
                                                Integer limit) throws GerritClientException {
        List<AccountInfo> groupsResponse = gerritClient.getAccounts(offset, limit);
        return groupsResponse.stream()
                .map(BasicData.mapper(AccountInfo.class))
                .collect(Collectors.toList());

    }
}
