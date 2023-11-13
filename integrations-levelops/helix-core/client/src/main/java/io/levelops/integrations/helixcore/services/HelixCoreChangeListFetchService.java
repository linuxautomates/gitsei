package io.levelops.integrations.helixcore.services;

import io.levelops.integrations.helixcore.client.HelixCoreClient;
import io.levelops.integrations.helixcore.client.HelixCoreClientException;
import io.levelops.integrations.helixcore.models.HelixCoreChangeList;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class HelixCoreChangeListFetchService {

    public Stream<HelixCoreChangeList> fetchChangeLists(HelixCoreClient client, Instant from, Instant to,
                                                        int pageSizeInDays, int maxFileSize, ZoneId zoneId) {
        List<ImmutablePair<LocalDate, LocalDate>> localDateIntervals = getIntervals(from, to, zoneId, pageSizeInDays);
        return CollectionUtils.emptyIfNull(localDateIntervals).stream()
                .map(pair -> {
                    try {
                        List<HelixCoreChangeList> changeListBatch = client.getChangeLists(pair.getLeft(), pair.getRight(), from, to, maxFileSize)
                                .peek(cl -> log.debug("Spec from = {}, Spec To = {}, From = {}, To = {} and changelist last updated at is {}", pair.getLeft(), pair.getRight(), from, to, cl.getLastUpdatedAt()))
                                .collect(Collectors.toList());
                        log.debug("Got {} changelists updated between {} and {}", changeListBatch.size(), from, to);
                        return changeListBatch;
                    } catch (HelixCoreClientException e) {
                        log.error("Failed to get change list for helix core " + e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                })
                .flatMap(Collection::stream);
    }

    public static List<ImmutablePair<LocalDate, LocalDate>> getIntervals(Instant from, Instant to, ZoneId zoneId, int pageSizeInDays) {
        ZonedDateTime fromDt = from.atZone(zoneId);
        ZonedDateTime toDt = to.atZone(zoneId);

        long numOfDays = ChronoUnit.DAYS.between(fromDt.toLocalDate(), toDt.toLocalDate()) + 1;
        int limit = (int) Math.ceil(1.0 * numOfDays / pageSizeInDays);

        LocalDate intervalStart = fromDt.toLocalDate();
        List<ImmutablePair<LocalDate, LocalDate>> localDateIntervals = new ArrayList<>();
        for(int i=0; i< limit; i++) {
            LocalDate intervalEnd = intervalStart.plus(pageSizeInDays, ChronoUnit.DAYS);
            localDateIntervals.add(ImmutablePair.of(intervalStart, intervalEnd));
            intervalStart = intervalEnd;
        }
        log.info("getIntervals, zoneId = {}, from = {}, to = {}, localDateIntervals = {}", zoneId, from, to, localDateIntervals);
        return localDateIntervals;
    }
}
