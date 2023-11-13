package io.levelops.integrations.coverity.models;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class CoverityModelsTest {

    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testFromDefects() throws IOException {
        String data = ResourceUtils.getResourceAsString("integrations/coverity/coverity_defects.json");
        Defect defects = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Defect.class));
        Assert.assertEquals(java.util.Optional.of(10001).get(), defects.getCid());
        Assert.assertEquals("NULL_RETURNS", defects.getCheckerName());
        Assert.assertEquals("Default.Other", defects.getComponentName());
        Assert.assertEquals(java.util.Optional.of(476).get(), defects.getCwe());
        Assert.assertEquals(java.util.Optional.of(10006).get(), defects.getFirstDetectedSnapshotId());
        Assert.assertEquals("STATIC_JAVA", defects.getDomain());
        Assert.assertEquals("COMMIT", defects.getFirstDetectedBy());
        Assert.assertEquals("trigger", defects.getFirstDetectedStream());
        Assert.assertEquals("MyClient.actionPerformed", defects.getFunctionName());
        Assert.assertEquals("4b4ec5aec5a7494cec5699cca6b1daa6", defects.getMergeKey());
        Assert.assertEquals("Quality", defects.getDisplayIssueKind());
        Assert.assertEquals(java.util.Optional.of(10006L).get(), defects.getLastDetectedSnapshotId());
        Assert.assertEquals("trigger", defects.getLastDetectedStream());
    }


    @Test
    public void testFromSnapshots() throws IOException {
        String data = ResourceUtils.getResourceAsString("integrations/coverity/coverity_snapshots.json");
        Snapshot snapshot = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Snapshot.class));
        Assert.assertEquals(java.util.Optional.of(10005).get(), snapshot.getSnapshotId().get("id"));
        Assert.assertEquals("synopsis-dev", snapshot.getAnalysisHost());
        Assert.assertEquals("2021.06", snapshot.getAnalysisVersion());
        Assert.assertEquals(java.util.Optional.of(14).get(),snapshot.getAnalysisTime());
        Assert.assertEquals(java.util.Optional.of(0).get(),snapshot.getBuildFailureCount());
        Assert.assertEquals(java.util.Optional.of(0).get(), snapshot.getBuildSuccessCount());
        Assert.assertEquals("admin", snapshot.getCommitUser());

    }

    @Test
    public void testFromStreams() throws IOException {
        String data = ResourceUtils.getResourceAsString("integrations/coverity/streams.json");
        Stream stream = MAPPER.readValue(data, MAPPER.getTypeFactory().constructType(Stream.class));
        Assert.assertEquals("trigger", stream.getId().get("name"));
        Assert.assertEquals("MIXED", stream.getLanguage());
        Assert.assertEquals("New Project 1", stream.getPrimaryProjectId().get("name"));
        Assert.assertEquals("Default Triage Store", stream.getTriageStoreId().get("name"));

    }
}
