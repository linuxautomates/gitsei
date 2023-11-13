package io.levelops.io.levelops.scm_repo_mapping.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.ingestion.models.ScmRepoMappingResult;
import org.junit.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ScmRepoMappingResponseTest {
    @Test
    public void testSerialization() throws JsonProcessingException {
        ObjectMapper objectMapper = DefaultObjectMapper.get();
        Instant fetchedAt = Instant.ofEpochMilli(1678324853423L);
        ScmRepoMappingResponse response = ScmRepoMappingResponse.builder()
                .jobId("jobId-1")
                .result(ScmRepoMappingResult.builder()
                        .fetchedAt(fetchedAt)
                        .mappedRepos(List.of("repo-1", "repo-2"))
                        .build())
                .build();
        var serialized = objectMapper.writeValueAsString(response);
        assertThat(serialized).isEqualTo("{\"job_id\":\"jobId-1\",\"result\":{\"fetched_at\":1678324853423,\"mapped_repos\":[\"repo-1\",\"repo-2\"]}}");
        ScmRepoMappingResponse deserialized = objectMapper.readValue(serialized, ScmRepoMappingResponse.class);
        assertThat(deserialized.getJobId()).isEqualTo("jobId-1");
        assertThat(deserialized.getResult().getFetchedAt()).isEqualTo(fetchedAt);
    }
}