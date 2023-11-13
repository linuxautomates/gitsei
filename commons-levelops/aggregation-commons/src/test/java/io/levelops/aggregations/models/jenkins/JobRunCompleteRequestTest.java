package io.levelops.aggregations.models.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.levelops.commons.jackson.DefaultObjectMapper;
import io.levelops.commons.utils.ResourceUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class JobRunCompleteRequestTest {
    private final static ObjectMapper MAPPER = DefaultObjectMapper.get();

    @Test
    public void testDeserialize() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("models/jenkins/jenkins_job_run_complete_request.json");
        JobRunCompleteRequest actual = MAPPER.readValue(serialized, JobRunCompleteRequest.class);
        Assert.assertNotNull(actual);
        Assert.assertTrue(CollectionUtils.isNotEmpty(actual.getTriggerChain()));
    }

    @Test
    public void testDeserializeTwo() throws IOException {
        String serialized = ResourceUtils.getResourceAsString("models/jenkins/jenkins_trigger_chain.json");
        JobRunCompleteRequest actual = MAPPER.readValue(serialized, JobRunCompleteRequest.class);
        Assert.assertNotNull(actual);
        Assert.assertTrue(CollectionUtils.isNotEmpty(actual.getTriggerChain()));
    }
}