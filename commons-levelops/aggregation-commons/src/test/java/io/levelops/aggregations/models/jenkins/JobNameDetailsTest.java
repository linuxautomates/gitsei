package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class JobNameDetailsTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void test() throws JsonProcessingException {
        JobNameDetails jobNameDetails = JobNameDetails.builder()
                .jobName("leetcode")
                .jobFullName("Folder1/jobs/Folder2/jobs/BBMaven1New/jobs/leetcode/branches/dev")
                .jobNormalizedFullName("Folder1/Folder2/BBMaven1New/leetcode/dev")
                .branchName("dev")
                .moduleName(null)
                .build();

        String serialized = MAPPER.writeValueAsString(jobNameDetails);
        JobNameDetails deSerialized = MAPPER.readValue(serialized, JobNameDetails.class);
        Assert.assertEquals(jobNameDetails, deSerialized);
    }

}