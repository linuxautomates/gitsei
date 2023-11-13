package io.levelops.integrations.coverity.services;

import com.coverity.ws.v9.MergedDefectDataObj;
import com.coverity.ws.v9.MergedDefectsPageDataObj;
import com.coverity.ws.v9.SnapshotIdDataObj;
import com.coverity.ws.v9.SnapshotInfoDataObj;
import com.coverity.ws.v9.StreamDataObj;
import com.google.common.collect.Iterators;
import io.levelops.commons.functional.PaginationUtils;
import io.levelops.commons.functional.StreamUtils;
import io.levelops.integrations.coverity.client.CoverityClient;
import io.levelops.integrations.coverity.client.CoverityClientException;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import io.levelops.integrations.coverity.models.EnrichedProjectData;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.levelops.integrations.coverity.utils.CoverityUtils.extractIdsFromStreams;

@Log4j2
public class CoverityDefectsFetchService {

    private static final int BATCH_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 10;

    public Stream<EnrichedProjectData> fetch(CoverityClient client, CoverityIterativeScanQuery query) {
        var entryStream =
                Objects.requireNonNull(streamCoverityStreams(client))
                        .flatMap(stream -> {
                            Stream<ImmutablePair<StreamDataObj, SnapshotInfoDataObj>> snapshotStream =
                                    streamSnapshots(client, stream, query.getFrom(), query.getTo())
                                            .map(snapshot -> ImmutablePair.of(stream, snapshot));
                            return snapshotStream;
                        })
                        .flatMap(pair -> {
                            Stream<MergedDefectDataObj> mergedDefectsStream = streamDefects(client, List.of(pair.getLeft()),
                                    query.getFrom(), query.getTo());
                            return Stream.of(ImmutablePair.of(pair, mergedDefectsStream.collect(Collectors.toList())));
                        });
        return StreamUtils.partition(entryStream, BATCH_SIZE)
                .flatMap(pairs -> {
                    Stream<EnrichedProjectData> enrichedProjectDataStream = pairs.stream().map(pair -> EnrichedProjectData.builder()
                            .stream(pair.getKey().getLeft())
                            .snapshot(pair.getKey().getRight())
                            .defects(pair.getRight())
                            .build());
                    return enrichedProjectDataStream;
                });
    }

    private Stream<StreamDataObj> streamCoverityStreams(CoverityClient client) {
        try {
            return client.getStreams().stream();
        } catch (CoverityClientException e) {
            log.error("streamProjects: Encountered Coverity client error " +
                    "while fetching streams", e);
            return Stream.empty();
        }
    }

    private Stream<SnapshotInfoDataObj> streamSnapshots(CoverityClient client,
                                                        StreamDataObj stream,
                                                        Date startDate,
                                                        Date endDate) {
        try {
            List<SnapshotIdDataObj> snapshots = client.getSnapshotsForStream(stream.getId(), startDate, endDate);
            return client.getSnapshotInformation(snapshots).stream();
        } catch (CoverityClientException e) {
            log.error("streamVersions: Encountered Coverity client error " +
                    "while fetching snapshots for streams " + stream.getId(), e);
            return Stream.empty();
        }
    }

    private Stream<MergedDefectDataObj> streamDefects(CoverityClient client,
                                                      List<StreamDataObj> streams,
                                                      Date startDate,
                                                      Date endDate) {
        return PaginationUtils.stream(0, DEFAULT_PAGE_SIZE, (offset) -> {
            try {
                MergedDefectsPageDataObj mergedDefectsForStreams = client.getMergedDefectsForStreams(extractIdsFromStreams(streams),
                        startDate, endDate, DEFAULT_PAGE_SIZE, offset);
                return mergedDefectsForStreams.getMergedDefects();
            } catch (CoverityClientException e) {
                log.error("streamDefects: Encountered Coverity client error " +
                        "while fetching defects for streams ", e);
                return List.of();
            }
        });
    }
}
