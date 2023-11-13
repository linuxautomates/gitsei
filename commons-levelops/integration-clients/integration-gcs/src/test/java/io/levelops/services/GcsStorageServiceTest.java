package io.levelops.services;

import io.levelops.commons.inventory.keys.IntegrationKey;
import org.junit.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

public class GcsStorageServiceTest {

    @Test
    public void testGeneratePath() {
        String output = GcsStorageService.generatePath("prefix/", new IntegrationKey("devtest", "007"), "123", Instant.ofEpochSecond(2000000000), "suffix");
        assertThat(output).isEqualTo("prefix/tenant-devtest/integration-007/2033/05/18/job-123/suffix");
    }

    @Test
    public void testSanitizePathPrefix() {
        assertThat(GcsStorageService.sanitizePathPrefix(null)).isEqualTo("");
        assertThat(GcsStorageService.sanitizePathPrefix("")).isEqualTo("");
        assertThat(GcsStorageService.sanitizePathPrefix("//")).isEqualTo("");
        assertThat(GcsStorageService.sanitizePathPrefix("//////")).isEqualTo("");
        assertThat(GcsStorageService.sanitizePathPrefix("    ")).isEqualTo("    /"); // blank is allowed
        assertThat(GcsStorageService.sanitizePathPrefix("a")).isEqualTo("a/");
        assertThat(GcsStorageService.sanitizePathPrefix("/a")).isEqualTo("a/");
        assertThat(GcsStorageService.sanitizePathPrefix("a/")).isEqualTo("a/");
        assertThat(GcsStorageService.sanitizePathPrefix("/a/b/")).isEqualTo("a/b/");
    }

    @Test
    public void sanitizeFilePath() {
        final GcsStorageService gcsStorageService = new GcsStorageService("bkt", "/tmp/");
        assertThat(gcsStorageService.sanitizeFilePath("test.json")).isEqualTo("tmp/test.json");
    }
}