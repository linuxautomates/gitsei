package io.levelops.integrations.coverity.client;

import com.coverity.ws.v9.MergedDefectsPageDataObj;
import com.coverity.ws.v9.PageSpecDataObj;
import com.coverity.ws.v9.ProjectIdDataObj;
import com.coverity.ws.v9.SnapshotIdDataObj;
import com.coverity.ws.v9.SnapshotInfoDataObj;
import com.coverity.ws.v9.SnapshotScopeDefectFilterSpecDataObj;
import com.coverity.ws.v9.SnapshotScopeSpecDataObj;
import com.coverity.ws.v9.StreamDataObj;
import com.coverity.ws.v9.StreamIdDataObj;
import io.levelops.commons.inventory.InMemoryInventoryService;
import io.levelops.commons.inventory.InventoryService;
import io.levelops.commons.inventory.keys.IntegrationKey;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class CoverityClientIntegrationTest {

    private static final String TENANT_ID = "test";
    private static final String INTEGRATION_ID = "coverity1";
    private static final String APPLICATION = "coverity";

    private static final String COVERITY_URL = System.getenv("COVERITY_URL");
    private static final String COVERITY_USERNAME = System.getenv("COVERITY_USERNAME");
    private static final String COVERITY_API_KEY = System.getenv("COVERITY_API_KEY");
    private static String STREAM_ID = System.getenv("STREAM_ID");
    private static String PROJECT_ID = System.getenv("PROJECT_ID");
    private static String START_DATE = System.getenv("START_DATE");
    private static String END_DATE = System.getenv("END_DATE");

    private static final IntegrationKey TEST_INTEGRATION_KEY = IntegrationKey.builder()
            .integrationId(INTEGRATION_ID).tenantId(TENANT_ID).build();
    private static long SNAPSHOT_ID = 10001;

    private CoverityClientFactory clientFactory;

    @Before
    public void setup() {
        InventoryService inventoryService = new InMemoryInventoryService(InMemoryInventoryService.Inventory.builder()
                .apiKey(TENANT_ID, INTEGRATION_ID, APPLICATION, COVERITY_URL, Collections.emptyMap(), COVERITY_USERNAME, COVERITY_API_KEY)
                .build());
        clientFactory = CoverityClientFactory.builder()
                .inventoryService(inventoryService)
                .build();
    }

    @Test
    public void streams() throws CoverityClientException {
        List<StreamDataObj> streams = clientFactory.get(TEST_INTEGRATION_KEY).getStreams();
        DefaultObjectMapper.prettyPrint(streams);
        assertThat(streams).isNotNull();
        assertThat(streams.get(0).getId()).isNotNull();
    }

    @Test
    public void snapshotsForStream() throws CoverityClientException, ParseException {
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(STREAM_ID);
        List<SnapshotIdDataObj> snapshots = clientFactory.get(TEST_INTEGRATION_KEY).getSnapshotsForStream(streamIdDataObj,
                new SimpleDateFormat("dd-MM-yyyy").parse(START_DATE), new SimpleDateFormat("dd-MM-yyyy").parse(END_DATE));
        DefaultObjectMapper.prettyPrint(snapshots);
        assertThat(snapshots).isNotNull();
        assertThat(snapshots.get(0).getId()).isNotNull();
    }

    @Test
    public void snapshotInformation() throws CoverityClientException {
        SnapshotIdDataObj snapshotIdDataObj = new SnapshotIdDataObj();
        snapshotIdDataObj.setId(SNAPSHOT_ID);
        List<SnapshotInfoDataObj> snapshots = clientFactory.get(TEST_INTEGRATION_KEY).getSnapshotInformation(List.of(snapshotIdDataObj));
        DefaultObjectMapper.prettyPrint(snapshots);
        assertThat(snapshots).isNotNull();
        assertThat(snapshots.get(0).getSnapshotId()).isNotNull();
    }

    @Test
    public void mergedDefectsForStreams() throws CoverityClientException, ParseException {
        StreamIdDataObj streamIdDataObj = new StreamIdDataObj();
        streamIdDataObj.setName(STREAM_ID);
        MergedDefectsPageDataObj defects = clientFactory.get(TEST_INTEGRATION_KEY).getMergedDefectsForStreams(List.of(streamIdDataObj),
                new SimpleDateFormat("dd-MM-yyyy").parse(START_DATE),
                new SimpleDateFormat("dd-MM-yyyy").parse(END_DATE), 10, 0);
        DefaultObjectMapper.prettyPrint(defects);
        assertThat(defects).isNotNull();
        assertThat(defects.getTotalNumberOfRecords()).isNotNull();
    }

    @Test
    public void mergedDefectsForSnapshotScope() throws CoverityClientException {
        ProjectIdDataObj projectIdDataObj = new ProjectIdDataObj();
        projectIdDataObj.setName(PROJECT_ID);
        MergedDefectsPageDataObj defects = clientFactory.get(TEST_INTEGRATION_KEY).getMergedDefectsForSnapshotScope(projectIdDataObj,
                new SnapshotScopeDefectFilterSpecDataObj(), new PageSpecDataObj(), new SnapshotScopeSpecDataObj());
        DefaultObjectMapper.prettyPrint(defects);
        assertThat(defects).isNotNull();
        assertThat(defects.getTotalNumberOfRecords()).isNotNull();
    }
}
