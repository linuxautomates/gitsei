package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JobAllRunsTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testSerialization() throws JsonProcessingException {
        JobAllRuns jobAllRuns = JobAllRuns.builder()
                .jobName("pipeline-1")
                .jobFullName("pipeline-1/branches/master")
                .jobNormalizedFullName("pipeline-1/master")
                .scmUrl("scm-url")
                .scmUserId("scm-user-id")
                .build();
        String serialization = MAPPER.writeValueAsString(jobAllRuns);
        Assert.assertEquals(serialization, "{\"job_name\":\"pipeline-1\",\"job_full_name\":\"pipeline-1/branches/master\",\"job_normalized_full_name\":\"pipeline-1/master\",\"scm_url\":\"scm-url\",\"scm_user_id\":\"scm-user-id\",\"runs\":[]}");
    }
}