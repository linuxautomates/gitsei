package io.levelops.commons.databases.models.database.cicd;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;

import org.junit.Assert;
import org.junit.Test;

import java.util.UUID;

public class JobRunStageStepTest {
    private static final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws JsonProcessingException {
        JobRunStageStep expected = JobRunStageStep.builder()
                .id(UUID.randomUUID())
                .cicdJobRunStageId(UUID.randomUUID())
                .stepId("7")
                .displayName("Check out from version control")
                .displayDescription(null)
                .duration(1234)
                .result("FAILURE")
                .state("FINISHED")
                //.startTime(Instant.now())
                .gcsPath("cicd-job-run-stage-logs/tenant-foo/2020/08/27/0b35560f-4bb9-4aad-a4e8-dbe4aabd3336")
                .build();
        String serialized = MAPPER.writeValueAsString(expected);
        JobRunStageStep actual = MAPPER.readValue(serialized, JobRunStageStep.class);
        Assert.assertEquals(expected, actual);
    }
}