package io.levelops.integrations.gerrit.services;

import io.levelops.integrations.gerrit.client.GerritClient;
import io.levelops.integrations.gerrit.models.ChangeInfo;
import io.levelops.integrations.gerrit.models.GerritQuery;

import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

public class GerritFetchChangesService {

    public Stream<ChangeInfo> fetchChanges(GerritClient client, Instant from) {
        return client.streamChanges(GerritQuery.builder().after(Date.from(from)).build());
    }
}
