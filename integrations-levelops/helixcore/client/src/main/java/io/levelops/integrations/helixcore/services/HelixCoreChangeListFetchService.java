package io.levelops.integrations.helixcore.services;

import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientException;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class HelixCoreChangeListFetchService {

    private static final int LIMIT = 50;
    private static final int OFFSET = 50;

    public Stream<HelixCoreChangeList> fetchChangeLists(HelixCoreClient client, Instant from, Instant to)
            throws HelixCoreClientException {
        int limit = LIMIT;
        List<HelixCoreChangeList> changeLists;
        while (true) {
            changeLists = client.getChangeLists(limit).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(changeLists) || changeLists.size() < limit) {
                break;
            }
            HelixCoreChangeList lastUpdated = changeLists.stream()
                    .min(Comparator.comparing(HelixCoreChangeList::getLastUpdatedAt)).orElse(null);
            if (lastUpdated.getLastUpdatedAt().toInstant().isBefore(from)) {
                break;
            } else {
                limit = limit + OFFSET;
            }
        }
        return changeLists.stream().filter(cl -> !cl.getLastUpdatedAt().before(java.util.Date.from(from))
                && !cl.getLastUpdatedAt().after(java.util.Date.from(to)));
    }
}
