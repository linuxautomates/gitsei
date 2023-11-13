package io.levelops.plugins.services;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class JenkinsPluginJobRunCompleteStorageServiceTest {
    private static final String BUCKET_NAME = "bucket_name";
    private final Instant now = Instant.now();
    private final String dateComponent = DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.US).format(now.atZone(ZoneOffset.UTC));

    private Storage storage = Mockito.mock(Storage.class);
    private Blob blob = Mockito.mock(Blob.class);

    @Before
    public void setup(){
        Mockito.when(storage.create(Mockito.any(BlobInfo.class), Mockito.any())).thenReturn(blob);
    }

    @Test
    public void testUploadJsonFile() {
        JenkinsPluginJobRunCompleteStorageService service = new JenkinsPluginJobRunCompleteStorageService(storage, BUCKET_NAME);
        String gcsPath = service.uploadJsonFile("tname",  "r_id", "json", "Hello".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("pre-results/tenant-tname/" + dateComponent + "/r_id-json-file.json", gcsPath);
    }

    @Test
    public void testUploadResultZipFile() {
        JenkinsPluginJobRunCompleteStorageService service = new JenkinsPluginJobRunCompleteStorageService(storage, BUCKET_NAME);
        String gcsPath = service.uploadResultsZipFile("tname",  "r_id", "json", "Hello".getBytes(StandardCharsets.UTF_8));
        Assert.assertEquals("pre-results/tenant-tname/" + dateComponent + "/r_id-zip-file.zip", gcsPath);
    }

}