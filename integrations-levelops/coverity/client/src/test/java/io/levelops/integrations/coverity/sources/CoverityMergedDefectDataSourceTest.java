package io.levelops.integrations.coverity.sources;

import com.coverity.ws.v9.MergedDefectsPageDataObj;
import com.coverity.ws.v9.SnapshotIdDataObj;
import com.coverity.ws.v9.SnapshotInfoDataObj;
import com.coverity.ws.v9.StreamDataObj;
import com.coverity.ws.v9.StreamIdDataObj;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.data.Data;
import io.levelops.ingestion.exceptions.FetchException;
import io.levelops.integrations.coverity.client.CoverityClient;
import io.levelops.integrations.coverity.client.CoverityClientException;
import io.levelops.integrations.coverity.client.CoverityClientFactory;
import io.levelops.integrations.coverity.models.CoverityIterativeScanQuery;
import io.levelops.integrations.coverity.models.EnrichedProjectData;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CoverityMergedDefectDataSourceTest {

    private static final IntegrationKey TEST_KEY = IntegrationKey.builder()
            .integrationId(EMPTY)
            .tenantId(EMPTY)
            .build();

    CoverityMergedDefectDataSource coverityMergedDefectDataSource;
    CoverityClient client;
    CoverityClient clientForStream;
    CoverityClient clientForSnapshot;
    CoverityClient clientForDefect;
    Date from = Date.from(Instant.now().minus(Duration.ofDays(1)));
    Date to = Date.from(Instant.now());

    @Before
    public void setup() throws CoverityClientException {

        StreamDataObj streamDataObj = new StreamDataObj();
        SnapshotInfoDataObj snapshotInfoDataObj = new SnapshotInfoDataObj();
        MergedDefectsPageDataObj mergedDefectsPageDataObj = new MergedDefectsPageDataObj();
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        SnapshotIdDataObj snapshotIdDataObj = new SnapshotIdDataObj();
        SnapshotIdDataObj snapshotIdDataObj1 = new SnapshotIdDataObj();

        client = Mockito.mock(CoverityClient.class);
        clientForStream = Mockito.mock(CoverityClient.class);
        clientForSnapshot = Mockito.mock(CoverityClient.class);
        clientForDefect = Mockito.mock(CoverityClient.class);
        CoverityClientFactory coverityClientFactory = Mockito.mock(CoverityClientFactory.class);
        coverityMergedDefectDataSource = new CoverityMergedDefectDataSource(coverityClientFactory);
        when(coverityClientFactory.get(eq(TEST_KEY))).thenReturn(client);
        when(coverityClientFactory.get(eq(IntegrationKey.builder().integrationId("1").build()))).thenReturn(clientForStream);
        when(coverityClientFactory.get(eq(IntegrationKey.builder().integrationId("2").build()))).thenReturn(clientForSnapshot);
        when(coverityClientFactory.get(eq(IntegrationKey.builder().integrationId("3").build()))).thenReturn(clientForDefect);

        streamIdDataObj.setName("stream1");
        streamDataObj.setId(streamIdDataObj);
        streamDataObj.setDescription("test stream");
        streamDataObj.setOutdated(true);
        streamDataObj.setLanguage("java");
        when(client.getStreams())
                .thenReturn(List.of(streamDataObj));
        when(clientForSnapshot.getStreams())
                .thenReturn(List.of(streamDataObj));
        when(clientForDefect.getStreams())
                .thenReturn(List.of(streamDataObj));
        when(clientForStream.getStreams())
                .thenThrow(new CoverityClientException());

        snapshotIdDataObj.setId(10001);
        snapshotIdDataObj1.setId(10002);
        when(client.getSnapshotsForStream(streamIdDataObj, from, to))
                .thenReturn(List.of(snapshotIdDataObj));
        when(clientForSnapshot.getSnapshotsForStream(streamIdDataObj,from, to))
                .thenThrow(new CoverityClientException());
        when(clientForDefect.getSnapshotsForStream(streamIdDataObj, from, to))
                .thenReturn(List.of(snapshotIdDataObj));

        snapshotInfoDataObj.setSnapshotId(snapshotIdDataObj);
        snapshotInfoDataObj.setDescription("snapshot test");
        when(client.getSnapshotInformation(List.of(snapshotIdDataObj)))
                .thenReturn(List.of(snapshotInfoDataObj));
        when(clientForDefect.getSnapshotInformation(List.of(snapshotIdDataObj)))
                .thenReturn(List.of(snapshotInfoDataObj));

        mergedDefectsPageDataObj.setTotalNumberOfRecords(2);
        when(client.getMergedDefectsForStreams(List.of(streamIdDataObj), from, to, 10, 0))
                .thenReturn(mergedDefectsPageDataObj);
        when(clientForDefect.getMergedDefectsForStreams(List.of(streamIdDataObj), from, to, 10, 0))
                .thenThrow(new CoverityClientException());

    }

    @Test
    public void testFetch() throws FetchException {
        List<Data<EnrichedProjectData>> dataList = coverityMergedDefectDataSource.fetchMany(
                CoverityIterativeScanQuery.builder().integrationKey(TEST_KEY)
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList()
        );
        DefaultObjectMapper.prettyPrint(dataList);
        Assertions.assertThat(dataList.size()).isEqualTo(1);
        List<EnrichedProjectData> enrichedProjectData = dataList.stream().map(Data::getPayload).collect(Collectors.toList());
        Assertions.assertThat(enrichedProjectData.stream().map(item -> item.getStream().getId().getName()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("stream1");
        Assertions.assertThat(enrichedProjectData.stream().map(item -> item.getSnapshot().getSnapshotId().getId()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder(Long.valueOf(10001));
        Assertions.assertThat(enrichedProjectData.stream().map(item -> item.getSnapshot().getDescription()).collect(Collectors.toList()))
                .containsExactlyInAnyOrder("snapshot test");

       List<Data<EnrichedProjectData>> dataList1 = coverityMergedDefectDataSource.fetchMany(
                CoverityIterativeScanQuery.builder().integrationKey(IntegrationKey.builder().integrationId("1").build())
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList()
        );
        DefaultObjectMapper.prettyPrint(dataList1);
        Assertions.assertThat(dataList1.size()).isEqualTo(0);

        List<Data<EnrichedProjectData>> dataList2 = coverityMergedDefectDataSource.fetchMany(
                CoverityIterativeScanQuery.builder().integrationKey(IntegrationKey.builder().integrationId("2").build())
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList()
        );
        DefaultObjectMapper.prettyPrint(dataList2);
        Assertions.assertThat(dataList2.size()).isEqualTo(0);

        List<Data<EnrichedProjectData>> dataList3 = coverityMergedDefectDataSource.fetchMany(
                CoverityIterativeScanQuery.builder().integrationKey(IntegrationKey.builder().integrationId("3").build())
                        .from(from)
                        .to(to)
                        .build()).collect(Collectors.toList()
        );
        DefaultObjectMapper.prettyPrint(dataList3);
        Assertions.assertThat(dataList3.size()).isEqualTo(1);

    }

    @Test
    public void fetchOne() {
        assertThatThrownBy(() -> coverityMergedDefectDataSource.fetchOne(CoverityIterativeScanQuery.builder()
                .integrationKey(TEST_KEY)
                .build()));
    }
}
