package io.levelops.integrations.helixcore.client;

import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.Changelist;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.server.IOptionsServer;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreDepot;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class HelixCoreClient {

    private final boolean enrichmentEnabled;
    private final IOptionsServer server;

    /**
     * @param server            object to be used for fetching information from helix core server
     * @param enrichmentEnabled true if the responses need to be enriched, otherwise false
     */
    @Builder
    public HelixCoreClient(IOptionsServer server, boolean enrichmentEnabled) {
        this.enrichmentEnabled = enrichmentEnabled;
        this.server = server;
    }

    public Stream<HelixCoreDepot> getDepots() throws HelixCoreClientException {
        try {
            return ListUtils.emptyIfNull(server.getDepots()).stream().map(HelixCoreDepot::fromDepot);
        } catch (ConnectionException | RequestException | AccessException e) {
            throw new HelixCoreClientException(e.getMessage());
        }
    }

    public Stream<HelixCoreChangeList> getChangeLists(int maxMostRecent) throws HelixCoreClientException {
        try {
            return ListUtils.emptyIfNull(server.getChangelists(null, new GetChangelistsOptions().setLongDesc(true)
                    .setMaxMostRecent(maxMostRecent))).stream()
                    .map(iChangelistSummary -> {
                        try {
                            return server.getChangelist(iChangelistSummary.getId());
                        } catch (ConnectionException | RequestException | AccessException e) {
                            log.warn("failed fetch changelist for id {}", iChangelistSummary.getId(), e);
                            return new Changelist();
                        }
                    })
                    .filter(cl -> {
                        try {
                            return CollectionUtils.isNotEmpty(cl.getFiles(true));
                        } catch (ConnectionException | RequestException | AccessException e) {
                            log.warn("failed fetch changelist files for id {}", cl.getId(), e);
                            return false;
                        }
                    })
                    .map(changelist -> {
                        try {
                            return HelixCoreChangeList.fromIChangeList(changelist);
                        } catch (ConnectionException | AccessException | RequestException ex) {
                            log.warn("failed to convert changelist with id as {} to Perforce Changelist", changelist.getId(), ex);
                            return null;
                        }
                    }).filter(Objects::nonNull);
        } catch (P4JavaException e) {
            throw new HelixCoreClientException(e.getMessage());
        }
    }

    /**
     * returns true if the projects need to be enriched otherwise false
     *
     * @return boolean value telling if the Projects/Depots need to be enriched
     */
    public boolean isEnrichmentEnabled() {
        return enrichmentEnabled;
    }
}
