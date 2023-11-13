package io.levelops.commons.databases.models.database.coverity;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import io.levelops.integrations.coverity.models.Defect;
import io.levelops.integrations.coverity.models.Snapshot;
import io.levelops.integrations.coverity.models.Stream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CoverityModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromDefects() throws IOException {
        String data = ResourceUtils.getResourceAsString("coverity/coverity_defects.json");
        Defect defects = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Defect.class));
        DbCoverityDefect dbCoverityDefect = DbCoverityDefect.fromDefect(defects, "1", "2eaac780-b1fd-4ffa-9023-d2c62db44a3e");
        Assert.assertEquals(java.util.Optional.of(10001).get(), dbCoverityDefect.getCid());
        Assert.assertEquals("NULL_RETURNS", dbCoverityDefect.getCheckerName());
        Assert.assertEquals("Default.Other", dbCoverityDefect.getComponentName());
        Assert.assertEquals(java.util.Optional.of(476).get(), dbCoverityDefect.getCwe());
        Assert.assertEquals(java.util.Optional.of(10006).get(), dbCoverityDefect.getFirstDetectedSnapshotId());
        Assert.assertEquals("STATIC_JAVA", dbCoverityDefect.getDomain());
        Assert.assertEquals("COMMIT", dbCoverityDefect.getFirstDetectedBy());
        Assert.assertEquals("trigger", dbCoverityDefect.getFirstDetectedStream());
        Assert.assertEquals("MyClient.actionPerformed", dbCoverityDefect.getFunctionName());
        Assert.assertEquals("4b4ec5aec5a7494cec5699cca6b1daa6", dbCoverityDefect.getMergeKey());
        Assert.assertEquals("Quality", dbCoverityDefect.getKind());
        Assert.assertEquals(java.util.Optional.of(10006).get(), dbCoverityDefect.getLastDetectedSnapshotId());
        Assert.assertEquals("trigger", dbCoverityDefect.getLastDetectedStream());
    }


    @Test
    public void testFromSnapshots() throws IOException {
        String data = ResourceUtils.getResourceAsString("coverity/coverity_snapshots.json");
        Snapshot snapshot = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Snapshot.class));
        DbCoveritySnapshot dbCoveritySnapshot = DbCoveritySnapshot.fromSnapshot(snapshot, "1", "2eaac780-b1fd-4ffa-9023-d2c62db44a3e");
        Assert.assertEquals(java.util.Optional.of(10005).get(), dbCoveritySnapshot.getSnapshotId());
        Assert.assertEquals("2eaac780-b1fd-4ffa-9023-d2c62db44a3e", dbCoveritySnapshot.getStreamId());
        Assert.assertEquals("synopsis-dev", dbCoveritySnapshot.getAnalysisHost());
        Assert.assertEquals("2021.06", dbCoveritySnapshot.getAnalysisVersion());
        Assert.assertEquals(java.util.Optional.of(14).get(),dbCoveritySnapshot.getTimeTaken());
        Assert.assertEquals(java.util.Optional.of(0).get(),dbCoveritySnapshot.getBuildFailureCount());
        Assert.assertEquals(java.util.Optional.of(0).get(), dbCoveritySnapshot.getBuildSuccessCount());
        Assert.assertEquals("admin", dbCoveritySnapshot.getCommitUser());
        Assert.assertEquals(null, dbCoveritySnapshot.getId());
        Assert.assertEquals(java.util.Optional.of(1).get(), dbCoveritySnapshot.getIntegrationId());
        Assert.assertEquals(null, dbCoveritySnapshot.getCreatedAt());
        Assert.assertEquals(null, dbCoveritySnapshot.getUpdatedAt());
    }

    @Test
    public void testFromStreams() throws IOException {
        String data = ResourceUtils.getResourceAsString("coverity/streams.json");
        Stream stream = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Stream.class));
        DbCoverityStream dbCoverityStream = DbCoverityStream.fromStream(stream, "1");
        Assert.assertEquals(java.util.Optional.of(1).get(), dbCoverityStream.getIntegrationId());
        Assert.assertEquals("trigger", dbCoverityStream.getName());
        Assert.assertEquals("MIXED", dbCoverityStream.getLanguage());
        Assert.assertEquals("New Project 1", dbCoverityStream.getProject());
        Assert.assertEquals(null,dbCoverityStream.getId());
        Assert.assertEquals("Default Triage Store", dbCoverityStream.getTriageStoreId());
        Assert.assertEquals(null, dbCoverityStream.getCreatedAt());
        Assert.assertEquals(null, dbCoverityStream.getUpdatedAt());
    }
}
