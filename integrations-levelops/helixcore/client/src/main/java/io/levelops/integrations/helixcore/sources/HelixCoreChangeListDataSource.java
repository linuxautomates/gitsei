package io.levelops.integrations.helixcore.sources;

import io.levelops.commons.dates.DateUtils;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.ingestion.data.BasicData;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.ingestion.sources.DataSource;
import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientFactory;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreIterativeQuery;
import io.levelops.integrations.helixcore.services.HelixCoreChangeListFetchService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;

import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HelixCoreChangeListDataSource implements DataSource<HelixCoreChangeList, HelixCoreIterativeQuery> {

    public static final String UNKNOWN = "_UNKNOWN_";
    private static final int ONBOARDING_IN_DAYS = 50;
    private static final Pattern DEPOT_PATH = Pattern.compile("//(.*)/(.*)/.*", Pattern.CASE_INSENSITIVE);

    private final HelixCoreClientFactory helixcoreClientFactory;
    private final HelixCoreChangeListFetchService helixCoreChangeListFetchService;

    public HelixCoreChangeListDataSource(HelixCoreClientFactory helixcoreClientFactory,
                                         HelixCoreChangeListFetchService helixCoreChangeListFetchService) {
        this.helixcoreClientFactory = helixcoreClientFactory;
        this.helixCoreChangeListFetchService = helixCoreChangeListFetchService;
    }

    @Override
    public Data<HelixCoreChangeList> fetchOne(HelixCoreIterativeQuery query) throws FetchException {
        throw new UnsupportedOperationException("FetchOne not supported");
    }

    @Override
    public Stream<Data<HelixCoreChangeList>> fetchMany(HelixCoreIterativeQuery query) throws FetchException {
        IntegrationKey integrationKey = query.getIntegrationKey();
        Validate.notNull(integrationKey, "query.getIntegrationKey() cannot be null.");
        Instant from = DateUtils.toInstant(query.getFrom(),
                Instant.now().minus(Duration.ofDays(ONBOARDING_IN_DAYS)));
        Instant to = DateUtils.toInstant(query.getTo());
        HelixCoreClient client = helixcoreClientFactory.get(integrationKey);
        return helixCoreChangeListFetchService.fetchChangeLists(client, from, to).map(cl -> {
            if (client.isEnrichmentEnabled()) {
                return enrichPerforceChangeList(cl);
            } else {
                return cl;
            }
        }).map(BasicData.mapper(HelixCoreChangeList.class));
    }

    private HelixCoreChangeList enrichPerforceChangeList(HelixCoreChangeList changeList) {
        String depotName = UNKNOWN;
        String streamName = UNKNOWN;
        if (changeList.getFilesCount() != 0) {
            String fileDepotName = changeList.getFiles().get(0).getDepotName();
            String depotPathString = changeList.getFiles().get(0).getDepotPathString();
            String pathDepotName = null;
            if (depotPathString != null) {
                Matcher depotMatcher = DEPOT_PATH.matcher(depotPathString);
                if (depotMatcher.find()) {
                    pathDepotName = depotMatcher.group(1);
                    streamName = depotMatcher.group(2);
                }
            }
            depotName = ObjectUtils.firstNonNull(fileDepotName, pathDepotName, UNKNOWN);
        }
        return changeList.toBuilder()
                .depotName(depotName)
                .streamName(streamName)
                .build();
    }

}
