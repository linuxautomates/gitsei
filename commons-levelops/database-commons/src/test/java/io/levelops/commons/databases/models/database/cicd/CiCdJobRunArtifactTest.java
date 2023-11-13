package io.levelops.commons.databases.models.database.cicd;

import org.junit.Test;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class CiCdJobRunArtifactTest {

    @Test
    public void testCompareTo() {
        CiCdJobRunArtifact a = CiCdJobRunArtifact.builder()
                .id("123")
                .cicdJobRunId(UUID.randomUUID())
                .input(true)
                .output(false)
                .type("t1")
                .location("l1")
                .name("a1")
                .qualifier("v1")
                .hash("h1")
                .metadata(Map.of("a", "b"))
                .createdAt(Instant.ofEpochSecond(10))
                .build();

        assertThat(a.compareTo(a)).isTrue();

        assertThat(a.toBuilder()
                .id("456")
                .build().compareTo(a)).isTrue();

        assertThat(a.toBuilder()
                .createdAt(Instant.ofEpochSecond(20))
                .build().compareTo(a)).isTrue();

        assertThat(a.toBuilder()
                .cicdJobRunId(UUID.randomUUID())
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .input(false)
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .output(true)
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .type("t2")
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .location("l2")
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .name("a2")
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .qualifier("v2")
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .hash("h2")
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .metadata(Map.of("c", "d"))
                .build().compareTo(a)).isFalse();
        assertThat(a.toBuilder()
                .metadata(Map.of("a", "b2"))
                .build().compareTo(a)).isFalse();
    }
}