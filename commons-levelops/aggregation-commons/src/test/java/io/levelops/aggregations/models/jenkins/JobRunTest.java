package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class JobRunTest {
    private final ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeSerialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("models/jenkins/jenkins_job_run.json");
        JobRun actual = MAPPER.readValue(serialized, JobRun.class);
        Assert.assertTrue(CollectionUtils.isNotEmpty(actual.getStages()));
        Assert.assertNotNull(actual);
    }
}