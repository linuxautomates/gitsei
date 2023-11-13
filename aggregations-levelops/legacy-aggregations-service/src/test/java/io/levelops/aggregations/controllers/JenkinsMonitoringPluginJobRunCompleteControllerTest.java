package io.levelops.aggregations.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import io.levelops.aggregations.models.jenkins.JobRunCompleteRequest;
import io.levelops.bullseye_converter_clients.BullseyeConverterClient;
import io.levelops.commons.databases.models.database.CICDJobRun;
import io.levelops.commons.databases.models.database.cicd.CiCdJobRunArtifact;
import io.levelops.commons.databases.services.CiCdInstancesDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunArtifactsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunStageStepsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunTestDatabaseService;
import io.levelops.commons.databases.services.CiCdJobRunsDatabaseService;
import io.levelops.commons.databases.services.CiCdJobsDatabaseService;
import io.levelops.commons.databases.services.bullseye.BullseyeDatabaseService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JenkinsMonitoringPluginJobRunCompleteControllerTest {
    private final Blob blob = mock(Blob.class);
    @Mock
    ObjectMapper objectMapper;
    @Mock
    CiCdInstancesDatabaseService ciCdInstancesDatabaseService;
    @Mock
    CiCdJobsDatabaseService ciCdJobsDatabaseService;
    @Mock
    CiCdJobRunsDatabaseService ciCdJobRunsDatabaseService;
    @Mock
    CiCdJobRunStageDatabaseService ciCdJobRunStageDatabaseService;
    @Mock
    CiCdJobRunStageStepsDatabaseService ciCdJobRunStageStepsDatabaseService;
    @Mock
    CiCdJobRunTestDatabaseService ciCdJobRunTestDatabaseService;
    @Mock
    BullseyeDatabaseService bullseyeDatabaseService;
    @Mock
    Storage storage;
    @Mock
    RedisConnectionFactory redisConnectionFactory;
    @Mock
    BullseyeConverterClient bullseyeConverterClient;

    @Mock
    CiCdJobRunArtifactsDatabaseService ciCdJobRunArtifactsDatabaseService;

    @Captor
    ArgumentCaptor<List<CiCdJobRunArtifact>> artifactsCaptor;

    @Ignore
    @Test
    public void testStartTimeDeserialize() {
        String startTime = "2020-08-26T02:50:37.323+0000";
        Instant instant = Instant.parse(startTime);
        Assert.assertNotNull(instant);
    }

    @Test
    public void testZipFilesInDataDirectory() throws IOException {
        MockitoAnnotations.initMocks(this);
        JenkinsMonitoringPluginJobRunCompleteController jenkinsMonitoringPluginJobRunCompleteController =
                mock(JenkinsMonitoringPluginJobRunCompleteController.class);
        when(jenkinsMonitoringPluginJobRunCompleteController.getPostBuildPublishedZipFile(any())).thenCallRealMethod();
        when(jenkinsMonitoringPluginJobRunCompleteController.getProjectFromPostBuildPublishedZipFileName(any())).thenCallRealMethod();
        when(jenkinsMonitoringPluginJobRunCompleteController.getDirectoryNameFromZipFileName(any())).thenCallRealMethod();
        String expectedProjectName = "test_project";
        String expectedDirectoryName = "levelops_code_coverage_" + expectedProjectName + "_xml";
        String expectedZipFileName = "levelops_code_coverage_" + expectedProjectName + "_xml.zip";

        File unzipFolder = null, zipFile = null;
        try {
            unzipFolder = Files.createTempDirectory("job-run-unzip-test").toFile();
            zipFile = new File(unzipFolder, expectedZipFileName);
            zipFile.createNewFile();
            String actualZipFileName = jenkinsMonitoringPluginJobRunCompleteController.getPostBuildPublishedZipFile(unzipFolder);
            String actualProjectName = jenkinsMonitoringPluginJobRunCompleteController.getProjectFromPostBuildPublishedZipFileName(actualZipFileName);
            String actualDirectoryName = jenkinsMonitoringPluginJobRunCompleteController.getDirectoryNameFromZipFileName(actualZipFileName);
            Assertions.assertEquals(expectedZipFileName, actualZipFileName);
            Assertions.assertEquals(expectedProjectName, actualProjectName);
            Assertions.assertEquals(expectedDirectoryName, actualDirectoryName);
        } finally {
            if (unzipFolder != null && unzipFolder.exists()) {
                unzipFolder.delete();
            }
            if (zipFile != null && zipFile.exists()) {
                zipFile.delete();
            }
        }

    }

    @Test
    public void testExtractPathAndFileNamesSimple() {
        JenkinsMonitoringPluginJobRunCompleteController jenkinsMonitoringPluginJobRunCompleteController =
                mock(JenkinsMonitoringPluginJobRunCompleteController.class);
        when(jenkinsMonitoringPluginJobRunCompleteController.extractPathAndFileName(any(), any())).thenCallRealMethod();

        ImmutablePair<String, String> pair = jenkinsMonitoringPluginJobRunCompleteController
                .extractPathAndFileName(new File("/tmp/"), new File("/tmp/a/b/c_A1234.cov"));
        assertThat(pair.left).isEqualTo("a/b/");
        assertThat(pair.right).isEqualTo("c.cov");

        pair = jenkinsMonitoringPluginJobRunCompleteController
                .extractPathAndFileName(new File("/tmp/"), new File("/tmp/c_A1234.cov"));
        assertThat(pair.left).isEqualTo("");
        assertThat(pair.right).isEqualTo("c.cov");

        pair = jenkinsMonitoringPluginJobRunCompleteController
                .extractPathAndFileName(new File("/tmp/"), new File("/tmp/c.cov"));
        assertThat(pair.left).isEqualTo("");
        assertThat(pair.right).isEqualTo("c.cov");
    }

    @Test
    public void testInsertArtifacts() throws IOException, SQLException {

        JenkinsMonitoringPluginJobRunCompleteController jenkinsMonitoringPluginJobRunCompleteController =
                mock(JenkinsMonitoringPluginJobRunCompleteController.class);
        ReflectionTestUtils.setField(jenkinsMonitoringPluginJobRunCompleteController, "ciCdJobRunArtifactsDatabaseService", ciCdJobRunArtifactsDatabaseService, CiCdJobRunArtifactsDatabaseService.class);

        Mockito.doCallRealMethod().when(jenkinsMonitoringPluginJobRunCompleteController).insertArtifacts(anyString(), any(),any());
        UUID jobRunId = UUID.randomUUID();
        CICDJobRun jobRun = CICDJobRun.builder()
                .id(jobRunId)
                .build();
        JobRunCompleteRequest jobRunCompleteRequest = JobRunCompleteRequest.builder().artifacts(
                List.of(CiCdJobRunArtifact.builder()
                        .name("artifact1")
                        .qualifier("tag-15")
                        .hash("sha256:c2de85514da49daba8d2f32ae4be4d504b2c4ebd2b76c39501056158a39f56fc")
                        .location("https://hub.docker.com/layers/juhiagr/sei-repo/16/images/sha256-c2de85514da49daba8d2f32ae4be4d504b2c4ebd2b76c39501056158a39f56fc/")
                        .type("container")
                        .input(false)
                        .output(true)
                        .build(),
                CiCdJobRunArtifact.builder()
                        .name("artifact2")
                        .qualifier("tag-16")
                        .hash("sha256:2cb4530b722b07bb14d716bf82627a6cce36efeecb3e1f80bc0d32ad6a9f5233")
                        .location("https://hub.docker.com/layers/juhiagr/sei-repo/17/images/sha256-2cb4530b722b07bb14d716bf82627a6cce36efeecb3e1f80bc0d32ad6a9f5233/")
                        .type("container")
                        .input(true)
                        .output(false)
                        .build()))
                .build();

        jenkinsMonitoringPluginJobRunCompleteController.insertArtifacts("test", jobRun, jobRunCompleteRequest);
        verify(ciCdJobRunArtifactsDatabaseService, times(1)).replace(anyString(), anyString(), any());
        then(ciCdJobRunArtifactsDatabaseService).should().replace(eq("test"), eq(jobRun.getId().toString()), artifactsCaptor.capture());
        Assert.assertEquals(jobRunId, artifactsCaptor.getValue().stream().map(a -> a.getCicdJobRunId()).collect(Collectors.toList()).get(0));
    }
}
