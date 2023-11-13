package io.levelops.integrations.helixcore.client;

import com.google.common.base.Stopwatch;
import com.perforce.p4java.core.ChangelistStatus;
import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.IChangelistSummary;
import com.perforce.p4java.exception.AccessException;
import com.perforce.p4java.exception.ConnectionException;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.exception.RequestException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetChangelistsOptions;
import com.perforce.p4java.option.server.LoginOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.IServerInfo;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import io.levelops.integrations.helixcore.models.HelixCoreChangeListUtils;
import io.levelops.integrations.helixcore.models.HelixCoreDepot;
import lombok.Builder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class HelixCoreClient {

    private static final String INVALID_PASSWORD = "Perforce password (P4PASSWD) invalid or unset.\n";

    private final boolean enrichmentEnabled;
    private final IOptionsServer server;
    private final String password;

    /**
     * @param server            object to be used for fetching information from helix core server
     * @param enrichmentEnabled true if the responses need to be enriched, otherwise false
     */
    @Builder
    public HelixCoreClient(IOptionsServer server, boolean enrichmentEnabled, String password) {
        this.enrichmentEnabled = enrichmentEnabled;
        this.server = server;
        this.password = password;
    }

    public Stream<HelixCoreDepot> getDepots() throws HelixCoreClientException {
        try {
            return ListUtils.emptyIfNull(server.getDepots()).stream().map(HelixCoreDepot::fromDepot);
        } catch (ConnectionException | RequestException | AccessException e) {
            throw new HelixCoreClientException(e.getMessage());
        }
    }

    public Stream<HelixCoreChangeList> getChangeLists(LocalDate specFrom, LocalDate specTo, Instant filterFrom, Instant filterTo, int maxFileSize)
            throws HelixCoreClientException {
        String spec = getSpec(specFrom, specTo);
        return getChangeLists(spec, filterFrom, filterTo, maxFileSize, null);
    }

    public Stream<HelixCoreChangeList> getChangeLists(String spec, Instant filterFrom, Instant filterTo, int maxFileSize, Integer limit) throws HelixCoreClientException {
        try {
            if (INVALID_PASSWORD.equalsIgnoreCase(server.getLoginStatus())) {
                refreshTicket();
            }

            GetChangelistsOptions getChangelistsOptions = new GetChangelistsOptions();
            if (limit != null) {
                getChangelistsOptions.setMaxMostRecent(limit);
            }
            log.info("Listing changelists for spec='{}'...", spec);
            Stopwatch st = Stopwatch.createStarted();
            List<IChangelistSummary> changelists = server.getChangelists(List.of(new FileSpec(spec)), getChangelistsOptions);
            Set<Integer> changeListIds = CollectionUtils.emptyIfNull(changelists).stream().filter(Objects::nonNull).map(IChangelistSummary::getId).collect(Collectors.toSet());
            log.info("Listing changelists for spec='{}' took {} ms, fetched changelist ids {}", spec, st.elapsed(TimeUnit.MILLISECONDS), changeListIds);

            return ListUtils.emptyIfNull(changelists).stream()
                    .filter(Objects::nonNull)
                    .peek(clSummary -> log.debug("From is {}, to is {} and changelist last updated" +
                            " at is {}", filterFrom, filterTo, clSummary.getDate()))
                    .filter(clSummary -> {
                        //If cl summary date is null, use it
                        if(clSummary.getDate() == null) {
                            return true;
                        }
                        //If from is specified & clSummary date is before from, filter it out
                        if(filterFrom != null && clSummary.getDate().toInstant().isBefore(filterFrom)) {
                            return false;
                        }
                        //If to is specified & clSummary date is before from, filter it out
                        if(filterTo != null && clSummary.getDate().toInstant().isAfter(filterTo)) {
                            return false;
                        }
                        return true;
                    })
                    .filter(clSummary -> {
                        //For fetch single commit both from & to will be null - do NOT filter cl
                        if (filterFrom == null && filterTo == null) {
                            return true;
                        }
                        //For fetch all commits, get only submitted cls
                        return ChangelistStatus.SUBMITTED == clSummary.getStatus();
                    })
                    .map(iChangelistSummary -> {
                        try {
                            IChangelist changelist = server.getChangelist(iChangelistSummary.getId());
                            // NOTE: SDK has a bug in changelist.getDate() but iChangelistSummary.getDate() is correct
                            changelist.setDate(iChangelistSummary.getDate());
                            return changelist;
                        } catch (ConnectionException | RequestException | AccessException e) {
                            log.warn("failed fetch changelist for id " + iChangelistSummary.getId(), e);
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .map(changelist -> {
                        try {
                            return HelixCoreChangeListUtils.getHelixCoreChangeList(changelist, server, maxFileSize);
                        } catch (Exception ex) {
                            log.warn("failed to convert changelist with id as " + changelist.getId() + " to Perforce Changelist", ex);
                            return null;
                        }
                    }).filter(Objects::nonNull);
        } catch (P4JavaException e) {
            throw new HelixCoreClientException(e.getMessage());
        }
    }

    private void refreshTicket() throws P4JavaException {
        String ticket = server.getAuthTicket();
        server.login(password, new StringBuffer().append(ticket),
                new LoginOptions(true, true));
    }

    private String getSpec(LocalDate from, LocalDate to) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        return "@" + formatter.format(from) + ",@" + formatter.format(to);
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
